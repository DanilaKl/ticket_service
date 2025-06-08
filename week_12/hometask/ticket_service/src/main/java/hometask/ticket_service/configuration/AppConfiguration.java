package hometask.ticket_service.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
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
