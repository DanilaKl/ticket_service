package hometask.ticket_service.repository;

import hometask.ticket_service.jooq.tables.records.EventsRecord;
import hometask.ticket_service.util.TimeConverter;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import static hometask.ticket_service.jooq.tables.Events.EVENTS;


@Repository
public class EventRepository {
    public List<EventsRecord> getAllEvents(DSLContext dslContext) {
        return dslContext.selectFrom(EVENTS)
                .fetchInto(EventsRecord.class);
    }

    public long insertEvent(DSLContext dslContext, String name, OffsetDateTime startsAt) {
        return dslContext.insertInto(EVENTS)
                .set(EVENTS.NAME, name)
                .set(EVENTS.STARTS_AT, TimeConverter.toInnerLocalDatetime(startsAt))
                .returningResult(EVENTS.ID)
                .fetchOne()
                .into(Long.class);
    }
}
