package service;

import data.TicketOption;
import user.User;
import data.Ticket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public int getTicketOptionIDByName(int eventID, String categoryName) {
        List<TicketOption> ticketOptions = getTicketOptionsByEvent(eventID);
        for (TicketOption option : ticketOptions) {
            if (option.getCategoryName().equals(categoryName)) {
                return option.getTicketOptionID(); // Assuming TicketOption has a method to get its ID
            }
        }
        return -1; // Indicate not found or handle this case as needed
    }

    // Verify a ticket with current date being the same as event date
    public boolean verifyTicket(int ticketID) {
        return this.databaseService.verifyTicket(ticketID);
    }

    // This method retrieves a mapping of ticket categories to their available counts
    public Map<String, Integer> getAvailableSeatsByCategory(int eventID) {
        List<TicketOption> ticketOptions = getTicketOptionsByEvent(eventID);
        Map<String, Integer> availableSeats = new HashMap<>();

        for (TicketOption option : ticketOptions) {
            availableSeats.put(option.getCategoryName(), option.getTotalAvailable());
            // Debug print to check what's being put in the map
            // System.out.println("Category: " + option.getCategoryName() + ", Available: " + option.getTotalAvailable());
        }

        return availableSeats;
    }
}
