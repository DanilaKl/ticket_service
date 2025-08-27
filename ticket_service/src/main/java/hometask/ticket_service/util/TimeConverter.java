package hometask.ticket_service.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class TimeConverter {
    private static final ZoneOffset zoneOffset = ZoneOffset.UTC;

    public static LocalDateTime toInnerLocalDatetime(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(zoneOffset).toLocalDateTime();
    }

    public static OffsetDateTime fromInnerLocalDatetime(LocalDateTime dateTime) {
        return dateTime.atOffset(zoneOffset);
    }
}
