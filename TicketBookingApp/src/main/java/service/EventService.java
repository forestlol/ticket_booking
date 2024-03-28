package service;

import user.EventManager;
import data.Event;
import java.util.List;
import java.util.Map;

public class EventService {
    private DatabaseService databaseService;
    private BookingService bookingService;

    public EventService(DatabaseService databaseService, BookingService bookingService) {
        this.databaseService = databaseService;
        this.bookingService = bookingService;
    }

    // Get all events method
    public List<Event> getEvents() {
        return databaseService.getAllEvents();
    }

    // Get events managed by a specific EventManager
    public List<Event> getMyManagedEvents(EventManager eventManager) {
        return databaseService.getEventsManagedBy(eventManager);
    }

    // Cancel an event method
    public String cancelEvent(EventManager eventManager, Event event) {
        if (eventManagerCanManageEvent(eventManager, event)) {
            // Cancel the event and handle related operations
            return "Event canceled successfully.";
        } else {
            return "Permission denied. Event not canceled.";
        }
    }

    // Update event details method
    public Event updateEvent(Event event, EventManager eventManager, Map<String, Object> details) {
        if (eventManagerCanManageEvent(eventManager, event)) {
            // Update event details and handle related operations
            return databaseService.updateEvent(event, details);
        } else {
            return null; // Permission denied. Event not updated.
        }
    }

    // Create a new event method
    public Event createEvent(EventManager eventManager, Map<String, Object> details) {
        // Assume event creation involves handling details and permissions
//        if (eventManagerCanCreateEvent(eventManager)) {
        Event newEvent = databaseService.createEvent(details);
        return newEvent;
//        } else {
//            return null; // Permission denied. Event not created.
//        }
    }

    // Add officers to an event method
    public Map<String, Boolean> addOfficer(EventManager eventManager, List<String> userIds) {
        // Assume adding officers involves permissions and handling related operations
        return databaseService.addOfficersToEvent(eventManager, userIds);
    }

    // Helper method to check if EventManager can manage the event
    private boolean eventManagerCanManageEvent(EventManager eventManager, Event event) {
        // Implementation logic to check if the EventManager has the necessary permissions
        // You may customize this based on your application's requirements
        return event.getManager().equals(eventManager);
    }

    // Helper method to check if EventManager can create a new event
//    private boolean eventManagerCanCreateEvent(EventManager eventManager) {
//        // Implementation logic to check if the EventManager has the necessary permissions
//        // You may customize this based on your application's requirements
//        return true; // For simplicity, assuming all EventManagers can create events
//    }
}
