package data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import user.*;

public class Booking {
    private int bookingID;
    private int eventID;
    private int customerID;
    private int ticketOfficerID;
    private int ticketOptionID;
    private float amountPaid;
    private List<Ticket> tickets;
    private LocalDateTime bookedTime;

    public Booking(int bookingID, int eventID, int customerID, int ticketOfficerID, int ticketOptionID, float amountPaid, List<Ticket> tickets, LocalDateTime bookedTime) {
        this.bookingID = bookingID;
        this.eventID = eventID;
        this.customerID = customerID;
        this.ticketOfficerID = ticketOfficerID;
        this.ticketOptionID = ticketOptionID;
        this.amountPaid = amountPaid;
        this.tickets = tickets;
        this.bookingTime = bookingTime;
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

    public int getTicketOfficerID() {
        return this.ticketOfficerID;
    }

    public int getTicketOptionID() {
        return this.ticketOptionID;
    }

    public float getAmountPaid() {
        return this.amountPaid;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public LocalDateTime getBookingTime() {
        return this.bookedTime;
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
        details.put("ticketOfficerID", this.ticketOfficerID);
        details.put("ticketOptionID", this.ticketOptionID);
        details.put("amountPaid", this.amountPaid);
        details.put("bookedTime", this.bookedTime);
        return details;
    }
}
