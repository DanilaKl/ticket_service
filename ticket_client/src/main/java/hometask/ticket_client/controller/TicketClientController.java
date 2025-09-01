package hometask.ticket_client.controller;


import hometask.ticket_client.dto.EventFormDto;
import hometask.ticket_client.dto.TicketActionFormDto;
import hometask.ticket_client.dto.TicketFormDto;
import hometask.ticket_client.dto.UserTicketFormDto;
import hometask.ticket_client.service.TemplateFillerService;
import hometask.ticket_client.service.TicketServiceClient;
import hometask.ticket_client.service.UserService;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

@Controller
@ControllerAdvice
@RequestMapping("${api.version}")
public class TicketClientController {

    @Value("${api.version}")
    private String apiVersion;

    private final TicketServiceClient ticketServiceClient;
    private final TemplateFillerService templateFillerService;
    private final UserService userService;

    public  TicketClientController(
            TicketServiceClient ticketServiceClient, TemplateFillerService templateFillerService, UserService userService
    ) {
        this.ticketServiceClient = ticketServiceClient;
        this.templateFillerService = templateFillerService;
        this.userService = userService;
    }

    @GetMapping("home")
    public String homePage(Model model) {
        return "home";
    }

    @GetMapping("get-events-request/perform")
    public String getEvents(Model model) {
        var events = ticketServiceClient.getAllEvents();
        templateFillerService.insertListOfItemsResponse(
                model,
                "response_forms/event_description",
                events
        );

        return "home";
    }

    @GetMapping("create-event-request")
    public String createEventFrom(Model model) {
        templateFillerService.insertForm(model, "request_forms/create_event_form", new EventFormDto());

        return "home";
    }

    @PostMapping("create-event-request/perform")
    public String createEvent(Model model, @ModelAttribute("dto") EventFormDto eventForm) {
        try {
            var tickets = Arrays.asList(eventForm.getTickets().split(", "));
            long eventId = ticketServiceClient.createEvent(
                    eventForm.getName(),
                    eventForm.getDateTime(),
                    tickets
            );
            templateFillerService.insertSingleItemResponse(
                    model,
                    "response_forms/id_description",
                    eventId);
        } catch (StatusRuntimeException e) {
            handleGrpcExceptions(model, e);
        }
        templateFillerService.insertForm(model, "request_forms/create_event_form", eventForm);

        return "home";
    }

    @GetMapping("get-tickets-request")
    public String getTicketsForm(Model model) {
        templateFillerService.insertForm(model, "request_forms/get_tickets_form", new TicketFormDto());

        return "home";
    }

    @PostMapping("get-tickets-request/perform")
    public String getTickets(Model model, @ModelAttribute("dto") TicketFormDto ticketForm) {
        try {
            var tickets = ticketServiceClient.getEventTickets(ticketForm.getEventId(), ticketForm.isAvailableOnly());
            templateFillerService.insertListOfItemsResponse(
                    model,
                    "response_forms/ticket_description",
                    tickets
            );
        } catch (StatusRuntimeException e) {
            handleGrpcExceptions(model, e);
        }
        templateFillerService.insertForm(model, "request_forms/get_tickets_form", ticketForm);

        return "home";
    }

    @GetMapping("get-user-tickets-request")
    public String getUserTicketsForm(HttpSession session, Model model) {
        templateFillerService.insertForm(
                model,
                "request_forms/get_user_tickets_form",
                new UserTicketFormDto()
        );
        model.addAttribute("users", userService.getUsers(session));

        return "home";
    }

    @PostMapping("get-user-tickets-request/perform")
    public String getUserTickets(
            HttpSession session, Model model,
            @ModelAttribute("dto") UserTicketFormDto userTicketForm
    ) {
        try {
            var tickets = ticketServiceClient.getUserTickets(userTicketForm.getUserId());
            templateFillerService.insertListOfItemsResponse(
                    model,
                    "response_forms/ticket_description",
                    tickets
            );
            userService.addUser(session, userTicketForm.getUserId());
        } catch (StatusRuntimeException e) {
            handleGrpcExceptions(model, e);
        }
        templateFillerService.insertForm(model, "request_forms/get_user_tickets_form", userTicketForm);
        model.addAttribute("users", userService.getUsers(session));


        return "home";
    }

    @GetMapping("ticket-{action}-request")
    public String makeTicketActionForm(HttpSession session, Model model,  @PathVariable String action) {
        templateFillerService.insertForm(
                model,
                "request_forms/ticket_action_form",
                new TicketActionFormDto()
        );
        model.addAttribute("users", userService.getUsers(session));
        model.addAttribute("action", action);

        return "home";
    }

    @PostMapping("ticket-{action}-request/perform")
    public String makeTicketAction(
            HttpSession session, Model model, @PathVariable String action,
            @ModelAttribute("dto") TicketActionFormDto ticketActionForm
    ) {
        try {
            String ticketNumber = performTicketAction(action, ticketActionForm);
            templateFillerService.insertSingleItemResponse(
                    model,
                    "response_forms/id_description",
                    ticketNumber
            );
            userService.addUser(session, ticketActionForm.getUserId());
        } catch (StatusRuntimeException e) {
            handleGrpcExceptions(model, e);
        }
        templateFillerService.insertForm(
                model,
                "request_forms/ticket_action_form",
                ticketActionForm
        );
        model.addAttribute("users", userService.getUsers(session));
        model.addAttribute("action", action);

        return "home";
    }

    @ModelAttribute
    public void addAttributes(Model model) {
        model.addAttribute("apiVersion", apiVersion);
    }

    private String performTicketAction(
            String action, TicketActionFormDto ticketActionForm
    ) {
        return switch (action) {
            case "reserve" -> ticketServiceClient.reserveTicket(
                    ticketActionForm.getUserId(), ticketActionForm.getEventId(), ticketActionForm.getTicketNumber()
            );
            case "confirm" -> ticketServiceClient.confirmTicket(
                    ticketActionForm.getUserId(), ticketActionForm.getEventId(), ticketActionForm.getTicketNumber()
            );
            case "cancel" -> ticketServiceClient.cancelTicketReservation(
                    ticketActionForm.getUserId(), ticketActionForm.getEventId(), ticketActionForm.getTicketNumber()
            );
            default -> "";
        };
    }

    private void handleGrpcExceptions(Model model, Exception exception) {
        templateFillerService.insertSingleItemResponse(
                model,
                "response_forms/error_description",
                exception.getMessage()
        );
    }
}
