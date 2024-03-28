package user;

import data.Event;
import java.util.ArrayList;
import java.util.List;

public class TicketingOfficer extends User {
    private List<Event> ticketingEvents;

    public TicketingOfficer(int id, String email, String password, String name, String type, List<Event> ticketingEvents) {
        super(id, email, password, name, type);
        this.ticketingEvents = ticketingEvents;
    }

    public TicketingOfficer(int id, String email, String password, String name, String type) {
        super(id, email, password, name, type);
        this.ticketingEvents = new ArrayList<>();
    }

    public List<Event> getTicketingEvents() {
        return ticketingEvents;
    }
}
