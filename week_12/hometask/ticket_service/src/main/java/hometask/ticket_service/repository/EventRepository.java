package hometask.ticket_service.repository;

import hometask.ticket_service.jooq.tables.records.EventsRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static hometask.ticket_service.jooq.tables.Events.EVENTS;


@Repository
public class EventRepository {
    private final DSLContext dslContext;

    @Autowired
    public EventRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public List<EventsRecord> getAllEvents() {
        return dslContext.selectFrom(EVENTS)
                .fetchInto(EventsRecord.class);
    }

    public Long insertEvent(String name, LocalDateTime startsAt) {
        return dslContext.insertInto(EVENTS)
                .set(EVENTS.NAME, name)
                .set(EVENTS.STARTS_AT, startsAt)
                .returningResult(EVENTS.ID)
                .fetchOne()
                .into(Long.class);
    }
}
