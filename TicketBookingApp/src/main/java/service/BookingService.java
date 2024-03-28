package service;

import user.User;
import user.TicketingOfficer;
import user.EventManager;

import data.Booking;
import data.Event;
import data.Refund;

import java.util.List;

public class BookingService {
    private DatabaseService databaseService;
    private RefundService refundService;

    public BookingService(DatabaseService databaseService, RefundService refundService) {
        this.databaseService = databaseService;
        this.refundService = refundService;
    }

//    NEW CODE
    /*
    createBooking(eventID, ticketOptionID, customerID, ticketOfficerID, numOfTickets)
	    1. Check if numOfTickets is less than or equals to 5, and if the event's currSlot > numOfTickets
            2. Yes, then add inside database
            2. Call TicketService's createTicket(int bookingID, isGuest)
            3. customerID pay for (basePrice * ticketOption) * numOfTickets which update in accountBalance
            4. amountPaid add basePrice * ticketOption
            5. Add to eventID's revenue
            6. Return Booking class
    getBookings(int userID)
    getBookingsByEvent(int eventID)
    cancelBooking (userID, bookingID)
    */

    public Booking createBooking(int eventID, int ticketOptionID, int customerID, int ticketOfficerID, int numOfTickets)
    {
        return databaseService.createBooking(eventID, ticketOptionID, customerID, ticketOfficerID, numOfTickets);
    }


    public List<Booking> getBookings(int userID){
        return databaseService.getBookings(userID);
    }

    public List<Biooking> getBookingsByEvent(int eventID){
        return databaseService.getBookingsByEvent(eventID);
    }

    public void cancelBooking (int userID, int bookingID) {
        databaseService.cancelBooking (userID, bookingID);
    }



// OLD CODE

    // Get all bookings for a user method
    public List<Booking> getBookings(int userID) {
        return databaseService.getBookings(userID);
    }

    // Get all bookings for an event method
    public List<Booking> getBookingsByEvent(Event event) {
        return databaseService.getBookingsByEvent(event);
    }

    // Create a new booking for a user method
    public Booking createBooking(int eventID, int ticketOptionID, int userID, int ticketOfficerID, ) {
        return databaseService.createBooking(userID, numTickets, eventID, numGuests);
    }

    // Create a new booking for a user by a ticket officer method
    public Booking createBookingFor(TicketingOfficer ticketOfficer, int numTickets, String eventID, int numGuests, String userID) {
        return databaseService.createBookingFor(ticketOfficer, numTickets, eventID, numGuests, userID);
    }

    // Cancel a booking for a user and process refund method
    public Refund cancelBooking(User user, String bookingID) {
        // Get the booking associated with the provided ID
        Booking booking = databaseService.getBookingByID(bookingID);

        if (booking != null && booking.getUser().equals(user)) {
            // Process refund and return the Refund object
            return refundService.processRefund(bookingID);
        } else {
            return null; // Permission denied or booking not found
        }
    }

    // Cancel all bookings for an event and process refunds method
    public List<Refund> cancelBooking(EventManager eventManager, Event event) {
        // Check if the event manager has the necessary permissions
        if (eventManagerCanManageEvent(eventManager, event)) {
            // Get all bookings for the event
            List<Booking> bookings = databaseService.getBookingsByEvent(event);

            // Process refunds for each booking and return the list of Refund objects
            return refundService.processRefund(bookings);
        } else {
            return null; // Permission denied
        }
    }

    // Helper method to check if EventManager can manage the event
    private boolean eventManagerCanManageEvent(EventManager eventManager, Event event) {
        // Implementation logic to check if the EventManager has the necessary permissions
        // You may customize this based on your application's requirements
        return event.getManager().equals(eventManager);
    }
}
