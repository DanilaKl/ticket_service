package hometask.ticket_service.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app")
public class AppConfiguration {
    private long confirmationTimeoutMinutes;

    public long getConfirmationTimeoutMinutes() {
        return confirmationTimeoutMinutes;
    }

    public void setConfirmationTimeoutMinutes(long confirmationTimeoutMinutes) {
        this.confirmationTimeoutMinutes = confirmationTimeoutMinutes;
    }
}
