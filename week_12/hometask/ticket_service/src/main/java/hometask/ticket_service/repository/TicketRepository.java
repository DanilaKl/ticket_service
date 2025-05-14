package hometask.ticket_service.repository;

import hometask.ticket_service.jooq.tables.records.TicketsRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static hometask.ticket_service.jooq.tables.Tickets.TICKETS;

@Repository
public class TicketRepository {
    private final DSLContext dslContext;

    @Autowired
    public TicketRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public List<TicketsRecord> getAllEventTickets(Long eventId) {
        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.EVENT_ID.eq(eventId))
                .fetchInto(TicketsRecord.class);
    }

    public List<TicketsRecord> getEventTickets(Long eventId, boolean isReserved) {
        var reservationCondition = TICKETS.IS_RESERVED.eq(isReserved);
        if (!isReserved) {
            reservationCondition.or(TICKETS.RESERVATION_DATE.lt(LocalDateTime.now()));
        } else {
            reservationCondition.and(TICKETS.RESERVATION_DATE.ge(LocalDateTime.now()));
        }

        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.EVENT_ID.eq(eventId).and(reservationCondition))
                .fetchInto(TicketsRecord.class);
    }

    public List<TicketsRecord> getUserTickets(String user) {
        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.USER_ID.eq(UUID.fromString(user)))
                .fetchInto(TicketsRecord.class);
    }

    public TicketsRecord getTicket(String ticketNumber, Long eventId) {
        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.NUMBER.eq(ticketNumber).and(TICKETS.EVENT_ID.eq(eventId)))
                .fetchOne();
    }

    public void insertTickets(List<String> ticketNumbers, Long eventId) {
        var insertionStmnt = dslContext.insertInto(TICKETS, TICKETS.NUMBER, TICKETS.EVENT_ID);
        for (var ticketNumber : ticketNumbers) {
            insertionStmnt = insertionStmnt.values(ticketNumber, eventId);
        }
        insertionStmnt.execute();
    }

    public String updateEventTicket(String ticketNumber, Long eventId, String user,
                                    Boolean idReserved, Boolean isConfirmed, LocalDateTime reservationTime) {
        return dslContext.update(TICKETS)
                .set(TICKETS.USER_ID, UUID.fromString(user))
                .set(TICKETS.IS_RESERVED, idReserved)
                .set(TICKETS.IS_CONFIRMED, isConfirmed)
                .set(TICKETS.RESERVATION_DATE, reservationTime)
                .where(TICKETS.NUMBER.eq(ticketNumber)
                        .and(TICKETS.EVENT_ID.eq(eventId))
                ).returningResult(TICKETS.NUMBER)
                .fetchOne().getValue(TICKETS.NUMBER);
    }

    public String updateUserTicket(String ticketNumber, Long eventId, String user,
                                 Boolean idReserved, Boolean isConfirmed) {
        return dslContext.update(TICKETS)
                .set(TICKETS.IS_CONFIRMED, true)
                .set(TICKETS.IS_RESERVED, idReserved)
                .set(TICKETS.IS_CONFIRMED, isConfirmed)
                .where(TICKETS.NUMBER.eq(ticketNumber)
                        .and(TICKETS.EVENT_ID.eq(eventId))
                        .and(TICKETS.USER_ID.eq(UUID.fromString(user))))
                .returningResult(TICKETS.NUMBER)
                .fetchOne().getValue(TICKETS.NUMBER);
    }


}
