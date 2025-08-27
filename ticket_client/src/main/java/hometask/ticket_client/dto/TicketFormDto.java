package hometask.ticket_client.dto;

public class TicketFormDto {
    long eventId;
    boolean availableOnly;

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public boolean isAvailableOnly() {
        return availableOnly;
    }

    public void setAvailableOnly(boolean availableOnly) {
        this.availableOnly = availableOnly;
    }
}
