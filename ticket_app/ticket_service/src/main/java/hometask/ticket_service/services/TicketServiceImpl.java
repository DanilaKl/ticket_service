package hometask.ticket_service.services;

import hometask.ticket_service.configuration.AppConfiguration;
import hometask.ticket_service.jooq.tables.records.TicketsRecord;
import hometask.ticket_service.repository.EventRepository;
import hometask.ticket_service.repository.TicketRepository;
import hometask.ticket_service.util.TimeConverter;
import hometask.ticket_service.validators.EventValidator;
import hometask.ticket_service.validators.TicketValidator;
import hometask.ticketservice.TicketServiceGrpc;
import hometask.ticketservice.TicketServiceOuterClass;
import hometask.ticketservice.TicketServiceOuterClass.CreateEventRequest;
import hometask.ticketservice.TicketServiceOuterClass.CreateEventResponse;
import hometask.ticketservice.TicketServiceOuterClass.GetEventsRequest;
import hometask.ticketservice.TicketServiceOuterClass.GetEventsResponse;
import hometask.ticketservice.TicketServiceOuterClass.GetTicketsRequest;
import hometask.ticketservice.TicketServiceOuterClass.GetTicketsResponse;
import hometask.ticketservice.TicketServiceOuterClass.TicketActionRequest;
import hometask.ticketservice.TicketServiceOuterClass.TicketActionResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.grpc.server.service.GrpcService;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@GrpcService
public class TicketServiceImpl extends TicketServiceGrpc.TicketServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final EventValidator eventValidator;
    private final TicketValidator ticketValidator;
    private final DSLContext dslContext;
    AppConfiguration config;

    @Autowired
    public TicketServiceImpl(AppConfiguration appConfiguration,
                             EventRepository eventRepository,
                             TicketRepository ticketRepository,
                             EventValidator eventValidator,
                             TicketValidator ticketValidator,
                             DSLContext dslContext) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.eventValidator = eventValidator;
        this.ticketValidator = ticketValidator;
        this.dslContext = dslContext;
        this.config = appConfiguration;
    }

    @Override
    public void createEvent(CreateEventRequest request,
                            StreamObserver<CreateEventResponse> responseObserver) {
        try {
            eventValidator.validateEvent(request);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        dslContext.transaction((Configuration trx) -> {
            long event_id = eventRepository.insertEvent(
                    trx.dsl(),
                    request.getName(),
                    OffsetDateTime.parse(request.getDateTime())
            );
            ticketRepository.insertTickets(trx.dsl(), request.getTicketNumbersList(), event_id);

            CreateEventResponse response = CreateEventResponse.newBuilder()
                    .setEventId(event_id)
                    .build();

            responseObserver.onNext(response);
        });

        responseObserver.onCompleted();
    }

    @Override
    public void getEvents(GetEventsRequest request,
                          StreamObserver<GetEventsResponse> responseObserver) {
        var eventsRecords = eventRepository.getAllEvents(dslContext);
        Iterator<TicketServiceOuterClass.Event> eventsResponses = eventsRecords.stream()
                .map(event -> TicketServiceOuterClass.Event.newBuilder()
                                        .setId(event.getId())
                                        .setName(event.getName())
                                        .setDateTime(event.getStartsAt().toString())
                                        .build()
                ).iterator();

        GetEventsResponse response = GetEventsResponse.newBuilder()
                .addAllEvents(() -> eventsResponses)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTickets(GetTicketsRequest request,
                           StreamObserver<GetTicketsResponse> responseObserver) {
        List<TicketsRecord> ticketsRecords;
        if (request.getAvailableOnly()) {
            ticketsRecords = ticketRepository.getEventTickets(dslContext, request.getEventId(), false);
        } else {
            ticketsRecords = ticketRepository.getAllEventTickets(dslContext, request.getEventId());
        }

        try {
            ticketValidator.validateTicketObjectNotNull(ticketsRecords);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        var response = GetTicketsResponse.newBuilder()
                .addAllTickets(mapTicketRecordsToTicketResponse(ticketsRecords))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getUserTickets(TicketServiceOuterClass.GetUserTicketsRequest request,
                               StreamObserver<GetTicketsResponse> responseObserver) {
        List<TicketsRecord> ticketsRecords = ticketRepository.getUserTickets(dslContext, request.getUserId());

        try {
            ticketValidator.validateTicketObjectNotNull(ticketsRecords);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        var response = GetTicketsResponse.newBuilder()
                .addAllTickets(mapTicketRecordsToTicketResponse(ticketsRecords))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void reserveTicket(TicketActionRequest request,
                              StreamObserver<TicketActionResponse> responseObserver) {
        TicketsRecord ticketsRecord = ticketRepository.getTicket(
                dslContext, request.getTicketNumber(), request.getEventId()
        );

        try {
            ticketValidator.validateTicketReserve(ticketsRecord);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        String ticketNumber = ticketRepository.updateEventTicket(
                dslContext,
                request.getTicketNumber(),
                request.getEventId(),
                request.getUserId(),
                true,
                false,
                OffsetDateTime.now().plusMinutes(config.getConfirmationTimeoutMinutes())
        );

        var response = TicketActionResponse.newBuilder().setTicketNumber(ticketNumber).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void confirmReservation(TicketActionRequest request,
                                   StreamObserver<TicketActionResponse> responseObserver) {
        TicketsRecord ticketsRecord = ticketRepository.getTicket(
                dslContext, request.getTicketNumber(), request.getEventId()
        );

        try {
            ticketValidator.validateTicketConfirm(request, ticketsRecord);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        String ticketNumber = ticketRepository.updateUserTicket(
                dslContext,
                request.getTicketNumber(),
                request.getEventId(),
                request.getUserId(),
                true,
                true
        );

        var response = TicketActionResponse.newBuilder().setTicketNumber(ticketNumber).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void cancelReservation(TicketActionRequest request,
                                  StreamObserver<TicketActionResponse> responseObserver) {
        TicketsRecord ticketsRecord = ticketRepository.getTicket(
                dslContext, request.getTicketNumber(), request.getEventId()
        );

        try {
            ticketValidator.validateTicketCancel(request, ticketsRecord);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        String ticketNumber = ticketRepository.updateUserTicket(
                dslContext,
                request.getTicketNumber(),
                request.getEventId(),
                request.getUserId(),
                false,
                false
        );

        var response = TicketActionResponse.newBuilder().setTicketNumber(ticketNumber).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void handleException(Exception exception, StreamObserver<?> responseObserver) {
        Status status = Status.UNKNOWN;
        if (exception instanceof NullPointerException) {
            status = Status.NOT_FOUND.augmentDescription(exception.getMessage());
        }
        else if (exception instanceof IllegalStateException) {
            status = Status.INVALID_ARGUMENT.augmentDescription(exception.getMessage());
        }
        else if (exception instanceof OptimisticLockingFailureException) {
            status = Status.INTERNAL.augmentDescription("Retry you request");
        }

        responseObserver.onError(status.asException());
    }

    private Iterable<? extends TicketServiceOuterClass.Ticket> mapTicketRecordsToTicketResponse(
            List<TicketsRecord> ticketsRecords
    ) {
        return () -> ticketsRecords.stream()
                .map(ticket -> TicketServiceOuterClass.Ticket.newBuilder()
                        .setNumber(ticket.getNumber())
                        .setEventId(ticket.getEventId())
                        .setIsReserved(
                                ticket.getIsReserved()
                                && OffsetDateTime.now().isBefore(
                                        TimeConverter.fromInnerLocalDatetime(ticket.getReservationDate())
                                )
                        )
                        .setIsConfirmed(ticket.getIsConfirmed())
                        .build()
                ).iterator();
    }
}
