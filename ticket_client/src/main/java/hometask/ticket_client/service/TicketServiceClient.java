package hometask.ticket_client.service;

import com.google.common.base.Function;
import hometask.ticketservice.TicketServiceGrpc;
import hometask.ticketservice.TicketServiceOuterClass;
import io.grpc.ManagedChannel;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceClient {
    private final TicketServiceGrpc.TicketServiceBlockingStub ticketServiceStub;

    public TicketServiceClient(ManagedChannel channel) {
        this.ticketServiceStub = TicketServiceGrpc.newBlockingStub(channel);
    }

    public List<TicketServiceOuterClass.Event> getAllEvents() {
        TicketServiceOuterClass.GetEventsRequest request = TicketServiceOuterClass.GetEventsRequest.newBuilder().build();
        TicketServiceOuterClass.GetEventsResponse events = ticketServiceStub.getEvents(request);

        return events.getEventsList();
    }

    public long createEvent(String name, OffsetDateTime dateTime, List<String> ticketNumbers) {
        TicketServiceOuterClass.CreateEventRequest request = TicketServiceOuterClass.CreateEventRequest
                .newBuilder()
                .setName(name)
                .setDateTime(dateTime.toString())
                .addAllTicketNumbers(ticketNumbers)
                .build();
        TicketServiceOuterClass.CreateEventResponse eventResponse = ticketServiceStub.createEvent(request);

        return  eventResponse.getEventId();
    }

    public List<TicketServiceOuterClass.Ticket> getEventTickets(long eventId, boolean availableOnly) {
        System.out.println(availableOnly);
        TicketServiceOuterClass.GetTicketsRequest request = TicketServiceOuterClass.GetTicketsRequest
                .newBuilder()
                .setEventId(eventId)
                .setAvailableOnly(availableOnly)
                .build();
        TicketServiceOuterClass.GetTicketsResponse ticketsResponse = ticketServiceStub.getTickets(request);

        return  ticketsResponse.getTicketsList();
    }

    public List<TicketServiceOuterClass.Ticket> getUserTickets(UUID userId) {
        TicketServiceOuterClass.GetUserTicketsRequest request = TicketServiceOuterClass.GetUserTicketsRequest
                .newBuilder()
                .setUserId(userId.toString())
                .build();
        TicketServiceOuterClass.GetTicketsResponse ticketsResponse = ticketServiceStub.getUserTickets(request);

        return ticketsResponse.getTicketsList();
    }

    public String reserveTicket(UUID userId, long eventId, String ticketNumber) {
        return makeTicketAction(userId, eventId, ticketNumber, ticketServiceStub::reserveTicket);
    }

    public String confirmTicket(UUID userId, long eventId, String ticketNumber) {
        return makeTicketAction(userId, eventId, ticketNumber, ticketServiceStub::confirmReservation);
    }

    public String cancelTicketReservation(UUID userId, long eventId, String ticketNumber) {
        return makeTicketAction(userId, eventId, ticketNumber, ticketServiceStub::cancelReservation);
    }

    private String makeTicketAction(
            UUID userId, long eventId, String ticketNumber,
            Function<TicketServiceOuterClass.TicketActionRequest, TicketServiceOuterClass.TicketActionResponse> action
    ) {
        TicketServiceOuterClass.TicketActionRequest request = TicketServiceOuterClass.TicketActionRequest
                .newBuilder()
                .setUserId(userId.toString())
                .setTicketNumber(ticketNumber)
                .setEventId(eventId)
                .build();
        TicketServiceOuterClass.TicketActionResponse ticketActionResponse = action.apply(request);

        return ticketActionResponse.getTicketNumber();
    }
}
