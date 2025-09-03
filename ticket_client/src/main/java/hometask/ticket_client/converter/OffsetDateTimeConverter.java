package hometask.ticket_client.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class OffsetDateTimeConverter implements Converter<String, OffsetDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");

    @Override
    public OffsetDateTime convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        LocalDateTime localDateTime = LocalDateTime.parse(source, FORMATTER);
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
