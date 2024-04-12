package user;

import data.Event;
import java.util.ArrayList;
import java.util.List;

public class EventManager extends User{
    private List<Event> managedEvents;

    public EventManager(int id, String email, String password, String name, String type, List<Event> managedEvents) {
        super(id, email, password, name, type);
        this.managedEvents = managedEvents;
    }

    // Creating new EventManager
    public EventManager(int id, String email, String password, String name, String type) {
        super(id, email, password, name, type);
        this.managedEvents = new ArrayList<>(); // Starts with an empty list
    }
    public List<Event> getManagedEvents() {
        return managedEvents;
    }
    
}
