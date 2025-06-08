package hometask.ticket_service.validators;

import hometask.ticket_service.jooq.tables.records.TicketsRecord;
import hometask.ticket_service.util.TimeConverter;
import hometask.ticketservice.TicketServiceOuterClass.TicketActionRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class TicketValidator {

    public void validateTicketReserve(TicketsRecord ticketRecord) {
        validateTicketObjectNotNull(ticketRecord);

        if (ticketRecord.getIsReserved()
                && OffsetDateTime.now().isBefore(
                        TimeConverter.fromInnerLocalDatetime(ticketRecord.getReservationDate())
                )
        ) {
            throw new IllegalStateException("Ticket already reserved");
        }
    }

    public void validateTicketConfirm(TicketActionRequest ticketRequest, TicketsRecord ticketRecord) {
        validateTicketObjectNotNull(ticketRecord);

        if (!UUID.fromString(ticketRequest.getUserId()).equals(ticketRecord.getUserId())
                || !ticketRecord.getIsReserved()
        ) {
            throw new IllegalStateException("Ticket already reserved");
        }
    }

    public void validateTicketCancel(TicketActionRequest ticketRequest, TicketsRecord ticketRecord) {
        validateTicketObjectNotNull(ticketRecord);

        if (!UUID.fromString(ticketRequest.getUserId()).equals(ticketRecord.getUserId())
        ) {
            throw new IllegalStateException("You don't own the ticket");
        }

        if (!ticketRecord.getIsReserved()) {
            throw new IllegalStateException("The ticket is not reserved");
        }

        if (ticketRecord.getIsConfirmed()) {
            throw new IllegalStateException("You can't cancel a confirmed ticket.");
        }
    }

    public void validateTicketObjectNotNull(Object ticketObject) {
        if (ticketObject == null) {
            throw new NullPointerException("No ticket with given parameters");
        }
    }
}
