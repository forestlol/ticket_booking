package service;

import data.TicketOption;
import user.User;
import data.Ticket;

import java.util.ArrayList;
import java.util.List;

public class TicketService {
    private List<Ticket> tickets;

    private DatabaseService databaseService;

    public TicketService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    // Get all tickets from everywhere
    public List<Ticket> getTickets() {
        return this.databaseService.getTickets(AccountService.getCurrentUser().getID());
    }

    // Get all tickets from chosen event
    public List<Ticket> getTicketsByEvent(int eventID)
    {
        return this.databaseService.getTicketsByEvent(eventID);
    }

    // Get all tickets from chosen booking
    public List<Ticket> getTicketsByBooking(int bookingID)
    {
        return this.databaseService.getTicketsByBooking(bookingID);
    }

    // Get all ticket options from chosen event (Can retrive totalAvailable)
    public List<TicketOption> getTicketOptionsByEvent(int eventID) {
        return this.databaseService.getTicketOptionsByEvent(eventID);
    }

    // Verify a ticket with current date being the same as event date
    public boolean verifyTicket(int ticketID) {
        return this.databaseService.verifyTicket(ticketID);
    }
}
