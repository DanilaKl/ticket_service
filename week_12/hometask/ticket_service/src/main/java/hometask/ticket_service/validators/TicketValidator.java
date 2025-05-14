package hometask.ticket_service.validators;

import hometask.ticket_service.jooq.tables.records.TicketsRecord;
import hometask.ticketservice.TicketServiceOuterClass.TicketActionRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class TicketValidator {

    public void validateTicketReserve(TicketsRecord ticketRecord) {
        validateTicketObjectNotNull(ticketRecord);

        if ((ticketRecord.getReservationDate() != null
                    && LocalDateTime.now().isBefore(ticketRecord.getReservationDate()))
                || ticketRecord.getIsReserved()) {
            throw new IllegalStateException("Ticket already reserved");
        }
    }

    public void validateTicketConfirmCancel(TicketActionRequest ticketRequest, TicketsRecord ticketRecord) {
        validateTicketObjectNotNull(ticketRecord);

        if (!UUID.fromString(ticketRequest.getUserId()).equals(ticketRecord.getUserId())
                || !ticketRecord.getIsReserved()
        ) {
            throw new IllegalStateException("Ticket already reserved");
        }
    }

    public void validateTicketObjectNotNull(Object ticketObject) {
        if (ticketObject == null) {
            throw new NullPointerException("No ticket with given parameters");
        }
    }
}
