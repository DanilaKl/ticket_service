package hometask.ticket_service.services;

import hometask.ticketservice.TicketServiceGrpc;
import hometask.ticketservice.TicketServiceOuterClass;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class TicketServiceImpl extends TicketServiceGrpc.TicketServiceImplBase {
    @Override
    public void createEvent(TicketServiceOuterClass.CreateEventRequest request,
                            StreamObserver<TicketServiceOuterClass.CreateEventResponse> responseObserver) {
        super.createEvent(request, responseObserver);
    }

    @Override
    public void getEvents(TicketServiceOuterClass.GetEventsRequest request,
                          StreamObserver<TicketServiceOuterClass.GetEventsResponse> responseObserver) {
        super.getEvents(request, responseObserver);
    }

    @Override
    public void getTickets(TicketServiceOuterClass.GetTicketsRequest request,
                           StreamObserver<TicketServiceOuterClass.GetTicketsResponse> responseObserver) {
        super.getTickets(request, responseObserver);
    }

    @Override
    public void reserveTicket(TicketServiceOuterClass.ReserveTicketRequest request,
                              StreamObserver<TicketServiceOuterClass.ReserveTicketResponse> responseObserver) {
        super.reserveTicket(request, responseObserver);
    }

    @Override
    public void confirmReservation(TicketServiceOuterClass.ConfirmReservationRequest request,
                                   StreamObserver<TicketServiceOuterClass.ConfirmReservationResponse> responseObserver) {
        super.confirmReservation(request, responseObserver);
    }

    @Override
    public void cancelReservation(TicketServiceOuterClass.CancelReservationRequest request,
                                  StreamObserver<TicketServiceOuterClass.CancelReservationResponse> responseObserver) {
        super.cancelReservation(request, responseObserver);
    }
}
