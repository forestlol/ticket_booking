package data;

import java.util.Map;
import java.util.HashMap;

public class Ticket {
    private int ticketID;
    private int bookingID;
    private boolean isGuest;
    private boolean attended;

    public Ticket(int ticketID, int bookingID, boolean isGuest, boolean attended) {
        this.ticketID = ticketID;
        this.bookingID = bookingID;
        this.isGuest = isGuest;
        this.attended = attended;
    }

    // Getters
    public int getTicketID() {
        return this.ticketID;
    }

     public int getBookingID() {
         return this.bookingID;
     }

    public boolean getIsGuest() {
        return isGuest;
    }

    public boolean getAttended() {
        return attended;
    }

    // Setters
    public void setAttended(boolean attended) {
        this.attended = attended;
    }

    // Other methods
    public Map<String, Object> getTicketDetails() {
        Map<String, Object> ticketDetails = new HashMap<>();

        ticketDetails.put("ticketID", this.ticketID);
        ticketDetails.put("bookingID", this.bookingID);
        ticketDetails.put("isGuest", this.isGuest);
        ticketDetails.put("attended", this.attended);

        return ticketDetails;
    }

}
