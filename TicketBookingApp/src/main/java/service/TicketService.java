package service;

import user.User;
import data.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TicketService {
    private List<Ticket> tickets;

    public TicketService() {
        this.tickets = new ArrayList<>();
    }

    // Get all tickets for a user method
    public List<Ticket> getTickets(User user) {
        List<Ticket> userTickets = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getOwner().equals(user)) {
                userTickets.add(ticket);
            }
        }
        return userTickets;
    }

    // Verify a ticket method
    public boolean verifyTicket(String ticketID) {
        for (Ticket ticket : tickets) {
            if (ticket.getTicketID().equals(ticketID)) {
                return ticket.isVerified();
            }
        }
        return false;
    }

    // Create a ticket method
    public boolean createTicket(String bookingID, boolean isGuest, int copies) {
        // Assume bookingID is valid and exists in the system
        BookingService bookingService = new BookingService();  // You may want to inject this dependency
        Booking booking = bookingService.getBookingByID(bookingID);

        if (booking != null) {
            for (int i = 0; i < copies; i++) {
                Ticket ticket = new Ticket(booking, isGuest);
                tickets.add(ticket);
            }
            return true;
        }
        return false;
    }

    private List<Integer> getTicketOptionsByEvent(int eventID) {
        List<Integer> ticketOptionIDs = new ArrayList<>();
        String query = "SELECT ticketOptionID FROM TicketOption WHERE eventID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ticketOptionIDs.add(rs.getInt("ticketOptionID"));
                }
            }
        }
        return ticketOptionIDs;
    }
}
