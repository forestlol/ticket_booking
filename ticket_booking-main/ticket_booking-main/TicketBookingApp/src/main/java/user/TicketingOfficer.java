package user;

import data.Event;
import java.util.ArrayList;
import java.util.List;

public class TicketingOfficer extends User {
    private List<Event> ticketingEvents;

    // Constructor that initializes TicketingOfficer with events
    public TicketingOfficer(int id, String email, String password, String name, String type, List<Event> ticketingEvents) {
        super(id, email, password, name, type);
        this.ticketingEvents = ticketingEvents;
    }

    // Constructor for TicketingOfficer without events (empty event list)
    public TicketingOfficer(int id, String email, String password, String name, String type) {
        super(id, email, password, name, type);
        this.ticketingEvents = new ArrayList<>();
    }

    // Getter for ticketing events
    public List<Event> getTicketingEvents() {
        return ticketingEvents;
    }

    // Overrides the getUserId method to return the ID from the User class
    public int getUserID() {
        return getID(); // Assumes getId() is the method in the User class.
    }
}
