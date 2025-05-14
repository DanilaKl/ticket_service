package hometask.ticket_service.services;

import hometask.ticket_service.AppConfiguration;
import hometask.ticket_service.jooq.tables.records.TicketsRecord;
import hometask.ticket_service.repository.EventRepository;
import hometask.ticket_service.repository.TicketRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@GrpcService
public class TicketServiceImpl extends TicketServiceGrpc.TicketServiceImplBase {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final EventValidator eventValidator;
    private final TicketValidator ticketValidator;
    private final long timeout;

    @Autowired
    public TicketServiceImpl(AppConfiguration appConfiguration,
                             EventRepository eventRepository,
                             TicketRepository ticketRepository,
                             EventValidator eventValidator,
                             TicketValidator ticketValidator) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.eventValidator = eventValidator;
        this.ticketValidator = ticketValidator;
        this.timeout = appConfiguration.getConfirmationTimeoutMinutes();
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

        long event_id = eventRepository.insertEvent(
                request.getName(),
                LocalDateTime.parse(request.getDateTime())
        );
        ticketRepository.insertTickets(request.getTicketNumbersList(), event_id);

        CreateEventResponse response = CreateEventResponse.newBuilder()
                .setEventId(event_id)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getEvents(GetEventsRequest request,
                          StreamObserver<GetEventsResponse> responseObserver) {
        var eventsRecords = eventRepository.getAllEvents();
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
            ticketsRecords = ticketRepository.getEventTickets(request.getEventId(), false);
        } else {
            ticketsRecords = ticketRepository.getAllEventTickets(request.getEventId());
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
    public void getUserTickets(TicketServiceOuterClass.GetUserTicketsRequest request, StreamObserver<GetTicketsResponse> responseObserver) {
        List<TicketsRecord> ticketsRecords = ticketRepository.getUserTickets(request.getUserId());

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
        TicketsRecord ticketsRecord = ticketRepository.getTicket(request.getTicketNumber(), request.getEventId());

        try {
            ticketValidator.validateTicketReserve(ticketsRecord);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        String ticketNumber = ticketRepository.updateEventTicket(
                request.getTicketNumber(),
                request.getEventId(),
                request.getUserId(),
                true,
                false,
                LocalDateTime.now().plusMinutes(timeout)
        );

        var response = TicketActionResponse.newBuilder().setTicketNumber(ticketNumber).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void confirmReservation(TicketActionRequest request,
                                   StreamObserver<TicketActionResponse> responseObserver) {
        TicketsRecord ticketsRecord = ticketRepository.getTicket(request.getTicketNumber(), request.getEventId());

        try {
            ticketValidator.validateTicketConfirmCancel(request, ticketsRecord);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        String ticketNumber = ticketRepository.updateUserTicket(
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
        TicketsRecord ticketsRecord = ticketRepository.getTicket(request.getTicketNumber(), request.getEventId());

        try {
            ticketValidator.validateTicketConfirmCancel(request, ticketsRecord);
        } catch (RuntimeException exception) {
            handleException(exception, responseObserver);
            return;
        }

        String ticketNumber = ticketRepository.updateUserTicket(
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

        responseObserver.onError(status.asException());
    }

    private Iterable<? extends TicketServiceOuterClass.Ticket> mapTicketRecordsToTicketResponse(
            List<TicketsRecord> ticketsRecords
    ) {
        return () -> ticketsRecords.stream()
                .map(ticket -> TicketServiceOuterClass.Ticket.newBuilder()
                        .setNumber(ticket.getNumber())
                        .setEventId(ticket.getEventId())
                        .setIsReserved(ticket.getIsReserved())
                        .setIsConfirmed(ticket.getIsConfirmed())
                        .build()
                ).iterator();
    }
}
