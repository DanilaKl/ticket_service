package hometask.ticket_service;

import hometask.ticket_service.configuration.AppConfiguration;
import hometask.ticket_service.util.TimeConverter;
import hometask.ticketservice.TicketServiceGrpc;
import hometask.ticketservice.TicketServiceOuterClass.CreateEventRequest;
import hometask.ticketservice.TicketServiceOuterClass.CreateEventResponse;
import hometask.ticketservice.TicketServiceOuterClass.GetEventsRequest;
import hometask.ticketservice.TicketServiceOuterClass.GetEventsResponse;
import hometask.ticketservice.TicketServiceOuterClass.GetTicketsRequest;
import hometask.ticketservice.TicketServiceOuterClass.GetTicketsResponse;
import hometask.ticketservice.TicketServiceOuterClass.TicketActionRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import liquibase.exception.LiquibaseException;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static hometask.ticket_service.jooq.tables.Events.EVENTS;
import static hometask.ticket_service.jooq.tables.Tickets.TICKETS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TicketServiceApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
			.withDatabaseName("tickets")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	private static TicketServiceGrpc.TicketServiceBlockingStub stub;

	@Autowired
	private DSLContext dsl;

	@Autowired
	AppConfiguration appConfig;

	@BeforeAll
	public static void setup() throws SQLException, LiquibaseException {
		ManagedChannel channel = ManagedChannelBuilder
				.forAddress("localhost", 9090)
				.usePlaintext()
				.build();

		stub = TicketServiceGrpc.newBlockingStub(channel);
	}

	@BeforeEach
	void resetDatabase() {
		dsl.truncate(EVENTS).cascade().execute();
		dsl.truncate(TICKETS).execute();
	}

	@Test
	void testCreateAndGetEvent() {
		List<String> ticketNumbers = List.of("A1", "A2", "A3");
		CreateEventRequest request = CreateEventRequest.newBuilder()
				.setName("Concert")
				.setDateTime(OffsetDateTime.now().plusDays(20).toString())
				.addAllTicketNumbers(ticketNumbers)
				.build();

		CreateEventResponse createResponse = stub.createEvent(request);

        var tickets = dsl.selectFrom(TICKETS)
				.where(TICKETS.EVENT_ID.eq(createResponse.getEventId()))
				.fetch();

		assertEquals(ticketNumbers.size(), tickets.size());

		GetEventsResponse getResponse = stub.getEvents(GetEventsRequest.newBuilder().build());

		assertEquals(1, getResponse.getEventsList().size());
	}

	@Test
	void testGetEventsAndTickets() {
		long eventId = createTestEvent("Football", List.of("B1", "B2"), OffsetDateTime.now().plusDays(20));

		GetEventsResponse eventsResponse = stub.getEvents(GetEventsRequest.newBuilder().build());
		assertEquals(1, eventsResponse.getEventsList().size());

		GetTicketsResponse ticketsResponse = stub.getTickets(GetTicketsRequest.newBuilder()
				.setEventId(eventId)
				.setAvailableOnly(false)
				.build());

		assertEquals(2, ticketsResponse.getTicketsList().size());
	}

	@Test
	void testSuccessfulReserveAndConfirm() {
		String ticketNumber = "C1";
		long eventId = createTestEvent("Movie", List.of(ticketNumber), OffsetDateTime.now().plusDays(20));
		String userId = UUID.randomUUID().toString();

		var reserveResp = stub.reserveTicket(TicketActionRequest.newBuilder()
				.setUserId(userId)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		assertEquals(ticketNumber, reserveResp.getTicketNumber());

		var confirmResp = stub.confirmReservation(TicketActionRequest.newBuilder()
				.setUserId(userId)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		assertEquals(ticketNumber, confirmResp.getTicketNumber());
	}

	@Test
	void testSuccessfulReserveAndCancel() {
		String ticketNumber = "C1";
		long eventId = createTestEvent("Movie", List.of(ticketNumber), OffsetDateTime.now().plusDays(20));
		String userId = UUID.randomUUID().toString();

		var reserveResp = stub.reserveTicket(TicketActionRequest.newBuilder()
				.setUserId(userId)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		assertEquals(ticketNumber, reserveResp.getTicketNumber());

		var cancelResp = stub.cancelReservation(TicketActionRequest.newBuilder()
				.setUserId(userId)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		assertEquals(ticketNumber, cancelResp.getTicketNumber());
	}

	@Test
	void testFailureCancelAfterReserve() {
		String ticketNumber = "C1";
		long eventId = createTestEvent("Movie", List.of(ticketNumber), OffsetDateTime.now().plusDays(20));
		String userId = UUID.randomUUID().toString();

		var reserveResp = stub.reserveTicket(TicketActionRequest.newBuilder()
				.setUserId(userId)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		assertEquals(ticketNumber, reserveResp.getTicketNumber());

		var confirmResp = stub.confirmReservation(TicketActionRequest.newBuilder()
				.setUserId(userId)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		assertEquals(ticketNumber, confirmResp.getTicketNumber());

		var cancelRequest = TicketActionRequest.newBuilder()
				.setUserId(userId)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build();

		assertThrows(StatusRuntimeException.class,
				() -> stub.cancelReservation(cancelRequest),
				"INVALID_ARGUMENT: You can't cancel a confirmed ticket");
	}

	@Test
	void testReReservationAfterExpiration() {
		String ticketNumber = "D1";
		long eventId = createTestEvent("Seminar", List.of(ticketNumber), OffsetDateTime.now().plusDays(20));
		String userId1 = UUID.randomUUID().toString();
		String userId2 = UUID.randomUUID().toString();

		stub.reserveTicket(TicketActionRequest.newBuilder()
				.setUserId(userId1)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		dsl.update(TICKETS)
				.set(TICKETS.RESERVATION_DATE,
						TimeConverter.toInnerLocalDatetime(OffsetDateTime.now().minusMinutes(1))
				)
				.where(TICKETS.NUMBER.eq(ticketNumber))
				.execute();

		var response = stub.reserveTicket(TicketActionRequest.newBuilder()
				.setUserId(userId2)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		assertEquals(ticketNumber, response.getTicketNumber());
	}

	@Test
	void testReservationConflict() {
		String ticketNumber = "E1";
		long eventId = createTestEvent("Talk Show", List.of(ticketNumber), OffsetDateTime.now().plusDays(20));
		String user1 = UUID.randomUUID().toString();
		String user2 = UUID.randomUUID().toString();

		stub.reserveTicket(TicketActionRequest.newBuilder()
				.setUserId(user1)
				.setTicketNumber(ticketNumber)
				.setEventId(eventId)
				.build());

		var reservationRequest = TicketActionRequest.newBuilder()
				.setUserId(user2)
				.setTicketNumber("E1")
				.setEventId(eventId)
				.build();

		assertThrows(StatusRuntimeException.class,
				() -> stub.reserveTicket(reservationRequest),
				"INVALID_ARGUMENT: You don't own the ticket");
	}

	private long createTestEvent(String name, List<String> tickets, OffsetDateTime dateTime) {
		CreateEventRequest request = CreateEventRequest.newBuilder()
				.setName(name)
				.setDateTime(dateTime.toString())
				.addAllTicketNumbers(tickets)
				.build();

		return stub.createEvent(request).getEventId();
	}
}
