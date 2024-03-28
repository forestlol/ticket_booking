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
    createBooking(eventID, ticketOptionID, customerID, ticketingOfficerID, numOfTickets)
        1. Check if numOfTickets is less than or equals to 5, and if the event's currSlot > numOfTickets
            Yes, then add inside database
        2. Minus totalAvailable under TicketOption using numOfTickets and update in TicketOption
        3. Call TicketService's createTicket(int bookingID, isGuest)
        4. customerID pay for (basePrice * ticketOption) * numOfTickets which update in accountBalance
        5. amountPaid add basePrice * ticketOption
        6. Add to eventID's revenue and currSlots under Event
        7. Return Booking class
    getBookings(int userID)
    getBookingsByEvent(int eventID)
    cancelBooking (userID, bookingID)
    */

    public Booking createBooking(int eventID, int ticketOptionID, int ticketingOfficerID, int numOfTickets)
    {
        return databaseService.createBooking(eventID, ticketOptionID, accountService.getCurrentUser().getID(), ticketingOfficerID, numOfTickets);
    }

    public List<Booking> getBookings(){
        return databaseService.getBookings(accountService.getCurrentUser().getID());
    }

    public List<Booking> getBookingsByEvent(int eventID){
        return databaseService.getBookingsByEvent(eventID);
    }

    public boolean cancelBooking (int bookingID) {
        databaseService.cancelBooking (accountService.getCurrentUser().getID(), bookingID);
    }
}
