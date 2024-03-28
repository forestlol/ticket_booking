package service;

import user.EventManager;
import data.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EventService {
    private AccountService accountService;
    private DatabaseService databaseService;
    private BookingService bookingService;

    public EventService(DatabaseService databaseService, BookingService bookingService) {
        this.databaseService = databaseService;
        this.bookingService = bookingService;
    }

//    NEW CODE
    /*
    [CRUD]
    createEvent(int eventManagerID, Map<String, Object> details)
        - details = Event Details
    getAllEvents()
    updateEvent(int eventID, int eventManagerID, Map<String, Object> details)
        - details = Event Details
    cancelEvent(int eventManagerID, int eventID)
        1. Get all bookingID that are tagged with this eventID
        2. Create Refund class, add in bookingID, timestamp, and status
        3. All user accountBalance will add amountPaid retrieved from bookingID
        4. Remove all affected bookings in database
        5. Remove this event in database
    getManagedEvents(int eventManagerID)
        - You get your own events
    */

    public Event createEvent(Map<String, Object> details) {
        return databaseService.createEvent(accountService.getCurrentUser().getID(), Map<String, Object > details);
    }

    public Event getEvent(int eventID)
    {
        return databaseService.getEvent(eventID);
    }

    public List<Event> getAllEvents() {
        return databaseService.getAllEvents();
    }

    // For Event Managers
    public List<Event> getManagedEvents() {
        return databaseService.getManagedEvents(accountService.getCurrentUser().getID());
    }

    // For Ticketing Officers
    public List<Event> getAuthorisedEvents() {
        return databaseService.getAuthorisedEvents(accountService.getCurrentUser().getID());
    }

    public Event updateEvent(int eventID, Map<String, Object> details) {
        return databaseService.updateEvent( int eventID, accountService.getCurrentUser().getID(), Map<String, Object > details)
    }

    public boolean cancelEvent(int eventID) {
        return databaseService.cancelEvent(accountService.getCurrentUser().getID(), eventID);
    }
}