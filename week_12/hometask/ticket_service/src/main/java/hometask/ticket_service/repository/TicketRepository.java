package hometask.ticket_service.repository;

import hometask.ticket_service.jooq.tables.records.TicketsRecord;
import hometask.ticket_service.util.TimeConverter;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static hometask.ticket_service.jooq.tables.Tickets.TICKETS;

@Repository
public class TicketRepository {
    public List<TicketsRecord> getAllEventTickets(DSLContext dslContext, long eventId) {
        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.EVENT_ID.eq(eventId))
                .fetchInto(TicketsRecord.class);
    }

    public List<TicketsRecord> getEventTickets(DSLContext dslContext, long eventId, boolean isReserved) {
        LocalDateTime now = TimeConverter.toInnerLocalDatetime(OffsetDateTime.now());
        var reservationCondition = TICKETS.IS_RESERVED.eq(isReserved);
        if (!isReserved) {
            reservationCondition.or(TICKETS.RESERVATION_DATE.lt(now));
        } else {
            reservationCondition.and(TICKETS.RESERVATION_DATE.ge(now));
        }

        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.EVENT_ID.eq(eventId).and(reservationCondition))
                .fetchInto(TicketsRecord.class);
    }

    public List<TicketsRecord> getUserTickets(DSLContext dslContext, String user) {
        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.USER_ID.eq(UUID.fromString(user)))
                .fetchInto(TicketsRecord.class);
    }

    public TicketsRecord getTicket(DSLContext dslContext, String ticketNumber, long eventId) {
        return dslContext.selectFrom(TICKETS)
                .where(TICKETS.NUMBER.eq(ticketNumber).and(TICKETS.EVENT_ID.eq(eventId)))
                .fetchOne();
    }

    public void insertTickets(DSLContext dslContext, List<String> ticketNumbers, long eventId) {
        var insertionStmnt = dslContext.insertInto(TICKETS, TICKETS.NUMBER, TICKETS.EVENT_ID);
        for (var ticketNumber : ticketNumbers) {
            insertionStmnt = insertionStmnt.values(ticketNumber, eventId);
        }
        insertionStmnt.execute();
    }

    public String updateEventTicket(DSLContext dslContext,
                                    String ticketNumber, long eventId, String user,
                                    boolean isReserved, boolean isConfirmed, OffsetDateTime reservationTime) {
        TicketsRecord updateTicket = dslContext.fetchOne(TICKETS,
                TICKETS.NUMBER.eq(ticketNumber).and(TICKETS.EVENT_ID.eq(eventId)));

        if (updateTicket != null) {
            String updatedNumber = dslContext.update(TICKETS)
                    .set(TICKETS.USER_ID, UUID.fromString(user))
                    .set(TICKETS.IS_RESERVED, isReserved)
                    .set(TICKETS.IS_CONFIRMED, isConfirmed)
                    .set(TICKETS.RESERVATION_DATE, TimeConverter.toInnerLocalDatetime(reservationTime))
                    .set(TICKETS.VERSION, updateTicket.getVersion() + 1)
                    .where(TICKETS.NUMBER.eq(ticketNumber)
                            .and(TICKETS.EVENT_ID.eq(eventId))
                            .and(TICKETS.VERSION.eq(updateTicket.getVersion()))
                    ).returningResult(TICKETS.NUMBER)
                    .fetchOne(Records.mapping(value -> value));

            if (updatedNumber == null) {
                throw new OptimisticLockingFailureException("Record was modified by another transaction");
            }

            return updatedNumber;
        }

        return null;
    }

    public String updateUserTicket(DSLContext dslContext,
                                String ticketNumber, Long eventId, String user,
                                Boolean isReserved, Boolean isConfirmed) {
        TicketsRecord updateTicket = dslContext.fetchOne(TICKETS,
                TICKETS.NUMBER.eq(ticketNumber)
                        .and(TICKETS.EVENT_ID.eq(eventId))
                        .and(TICKETS.USER_ID.eq(UUID.fromString(user))));

        if (updateTicket != null) {
            String updatedNumber = dslContext.update(TICKETS)
                    .set(TICKETS.IS_RESERVED, isReserved)
                    .set(TICKETS.IS_CONFIRMED, isConfirmed)
                    .set(TICKETS.VERSION, updateTicket.getVersion() + 1)
                    .where(TICKETS.NUMBER.eq(ticketNumber)
                            .and(TICKETS.EVENT_ID.eq(eventId))
                            .and(TICKETS.USER_ID.eq(UUID.fromString(user)))
                            .and(TICKETS.VERSION.eq(updateTicket.getVersion()))
                    ).returningResult(TICKETS.NUMBER)
                    .fetchOne(Records.mapping(value -> value));

            if (updatedNumber == null) {
                throw new OptimisticLockingFailureException("Record was modified by another transaction");
            }

            return updatedNumber;
        }

        return null;
    }


}
