package hometask.ticket_client.dto;

import java.util.UUID;

public class UserTicketFormDto {
    UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
