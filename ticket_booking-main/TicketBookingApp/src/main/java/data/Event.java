package data;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Event {
    private int eventID;
    private int eventManagerID;
    private double basePrice;
    private String eventName;
    private String eventDesc;
    private String venue;
    private LocalDateTime startTime;
    private int duration;
    private double revenue;
    private int currSlots;
    private int totalSlots;
    private double ticketCancellationFee;
    private List<Ticket> attendance;
    private boolean isCancelled;

    private final SimpleIntegerProperty eventId = new SimpleIntegerProperty();



    public Event(
        int eventID,
        int eventManagerID,
        double basePrice,
        String eventName,
        String eventDesc,
        String venue,
        LocalDateTime startTime,
        int duration,
        double revenue,
        int currSlots,
        int totalSlots,
        double ticketCancellationFee,
        boolean isCancelled) {
            this.eventID = eventID;
            this.eventManagerID = eventManagerID;
            this.basePrice = basePrice;
            this.eventName = eventName;
            this.eventDesc = eventDesc;
            this.venue = venue;
            this.startTime = startTime;
            this.duration = duration;
            this.revenue = revenue;
            this.currSlots = currSlots;
            this.totalSlots = totalSlots;
            this.ticketCancellationFee = ticketCancellationFee;
            this.isCancelled = isCancelled;
    }

    @Override
    public String toString() {
        // Return a string representation of the event that you'd like to display in the ListView
        return eventName + "\n" + eventDesc;
    }

    // Getters
    public int getEventID() {
        return this.eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;;
    }
    public int getEventManagerID() {
        return this.eventManagerID;
    }

    public double getBasePrice() {
        return this.basePrice;
    }

    public String getEventName() {
        return this.eventName;
    }

    public String getEventDesc() {
        return this.eventDesc;
    }

    public String getVenue() {
        return this.venue;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public int getDuration() {
        return this.duration;
    }

    public LocalDateTime getEndTime() {
        return (this.startTime.plusMinutes(this.duration));
    }

    public double getRevenue() {
        return this.revenue;
    }

    public int getCurrSlots() {
        return this.currSlots;
    }

    public int getTotalSlots() {
        return this.totalSlots;
    }

    public double getTicketCancellationFee() {
        return this.ticketCancellationFee;
    }

    public boolean getIsCancelled() { return this.isCancelled; }


    // Setters
    public void setIsCancelled(Boolean isCancelled) { this.isCancelled = isCancelled; }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventDesc(String eventDesc) {
        this.eventDesc = eventDesc;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setCurrSlotsAvailable(int currSlots) {
        this.currSlots = currSlots;
    }

    public void setTicketCancellationFee(double ticketCancellationFee) {
        this.ticketCancellationFee = ticketCancellationFee;
    }
    // Other methods
    public Map<String, Object> getEventDetails() {
        Map<String, Object> details = new HashMap<>();

        details.put("eventID", this.eventID);
        details.put("eventName", this.eventName);
        details.put("eventDesc", this.eventDesc);
        details.put("basePrice", this.basePrice);
        details.put("venue", this.venue);
        details.put("startTime", this.startTime.toString());
        details.put("duration", this.duration);
        details.put("revenue", this.revenue);
        details.put("currSlots", this.currSlots);
        details.put("totalSlots", this.totalSlots);
        details.put("isCancelled", this.isCancelled);

        return details;
    }

    // Convert all relevant event data to a map.
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", this.eventName);
        map.put("basePrice", this.basePrice);
        // Add other properties to the map
        return map;
    }
}
