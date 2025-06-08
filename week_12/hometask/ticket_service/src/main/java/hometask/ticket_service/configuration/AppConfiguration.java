package hometask.ticket_service.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app")
public class AppConfiguration {
    private int confirmationTimeoutMinutes;

    public int getConfirmationTimeoutMinutes() {
        return confirmationTimeoutMinutes;
    }

    public void setConfirmationTimeoutMinutes(int confirmationTimeoutMinutes) {
        this.confirmationTimeoutMinutes = confirmationTimeoutMinutes;
    }
}
