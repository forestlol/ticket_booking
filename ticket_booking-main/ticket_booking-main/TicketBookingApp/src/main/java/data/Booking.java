package data;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Booking {
    private int bookingID;
    private int eventID;
    private int customerID;
    private int ticketingOfficerID;
    private int ticketOptionID;
    private double amountPaid;
    private List<Ticket> tickets;
    private LocalDateTime bookedTime;
    private String bookingStatus;

    public Booking(int bookingID, int eventID, int customerID, int ticketingOfficerID, int ticketOptionID, double amountPaid, List<Ticket> tickets, LocalDateTime bookedTime, String bookingStatus) {
        this.bookingID = bookingID;
        this.eventID = eventID;
        this.customerID = customerID;
        this.ticketingOfficerID = ticketingOfficerID;
        this.ticketOptionID = ticketOptionID;
        this.amountPaid = amountPaid;
        this.tickets = tickets;
        this.bookedTime = bookedTime;
        this.bookingStatus = bookingStatus;
    }


    // Getters
    public int getBookingID() {
        return this.bookingID;
    }

    public int getEventID() {
        return this.eventID;
    }

    public int getCustomerID() {
        return this.customerID;
    }

    public int getTicketingOfficerID() {
        return this.ticketingOfficerID;
    }

    public int getTicketOptionID() {
        return this.ticketOptionID;
    }

    public double getAmountPaid() {
        return this.amountPaid;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public LocalDateTime getBookingTime() {
        return this.bookedTime;
    }

    public String getBookingStatus() {
        return this.bookingStatus;
    }

    // Setters
    public void setBookingTime(LocalDateTime bookedTime) {
        this.bookedTime = bookedTime;
    }

    // Other Methods
    public Map<String, Object> getBookingDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("bookingID", this.bookingID);
        details.put("eventID", this.eventID);
        details.put("customerID", this.customerID);
        details.put("ticketingOfficerID", this.ticketingOfficerID);
        details.put("ticketOptionID", this.ticketOptionID);
        details.put("amountPaid", this.amountPaid);
        details.put("bookedTime", this.bookedTime);
        details.put("bookingStatus", this.bookingStatus);
        return details;
    }
}
