package hometask.ticket_client.configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfiguration {
    @Bean
    public ManagedChannel ticketServiceChannel() {
        return ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();
    }
}
