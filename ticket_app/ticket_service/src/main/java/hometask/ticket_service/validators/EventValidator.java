package hometask.ticket_service.validators;

import hometask.ticketservice.TicketServiceOuterClass.CreateEventRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class EventValidator {
    public void validateEvent(CreateEventRequest eventRequest) {
        if (eventRequest.getName().isBlank()) {
            throw new IllegalStateException("Name could not be blank");
        }

        if (OffsetDateTime.parse(eventRequest.getDateTime()).isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Date could not be in past");
        }

        int ticketNumberCount = eventRequest.getTicketNumbersCount();
        Set<String> usedTicketNumber = new HashSet<>();
        for (int i = 0; i < ticketNumberCount; i++) {
            usedTicketNumber.add(eventRequest.getTicketNumbers(i));
        }

        if (ticketNumberCount > usedTicketNumber.size()) {
            throw new IllegalStateException("Ticket numbers should be unique");
        }
    }
}
