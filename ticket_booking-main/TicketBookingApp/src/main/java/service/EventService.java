package service;

import data.Event;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class EventService {
    private DatabaseService databaseService;

    public EventService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Event createEvent(Map<String, Object> details) throws SQLException {
        return this.databaseService.createEvent(AccountService.getCurrentUser().getID(), details);
    }

    public Event getEvent(int eventID)
    {
        return this.databaseService.getEvent(eventID);
    }

    public List<Event> getAllEvents() {
        return this.databaseService.getAllEvents();
    }

    // For Event Managers
    public List<Event> getManagedEvents() {
        return this.databaseService.getManagedEvents(AccountService.getCurrentUser().getID());
    }

    // For Ticketing Officers
    public List<Event> getAuthorisedEvents() {
        return databaseService.getAuthorisedEvents(AccountService.getCurrentUser().getID());
    }

    public Event updateEvent(int eventID, Map<String, Object> details) {
        return databaseService.updateEvent(eventID, AccountService.getCurrentUser().getID(), details);
    }

    public boolean cancelEvent(int eventID) {
        return databaseService.cancelEvent(AccountService.getCurrentUser().getID(), eventID);
    }
}