package service;

import data.Booking;

import java.sql.SQLException;
import java.util.List;

public class BookingService {
    private DatabaseService databaseService;

    public BookingService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Booking createBooking(int eventID, int ticketOptionID, int ticketingOfficerID, int numOfTickets) throws SQLException {
        return this.databaseService.createBooking(eventID, ticketOptionID, AccountService.getCurrentUser().getID(), ticketingOfficerID, numOfTickets);
    }

    public List<Booking> getBookings(){
        return this.databaseService.getBookings(AccountService.getCurrentUser().getID());
    }

    public List<Booking> getBookingsByEvent(int eventID){
        return this.databaseService.getBookingsByEvent(eventID);
    }

    public boolean cancelBooking (int bookingID) {
        return this.databaseService.cancelBooking (AccountService.getCurrentUser().getID(), bookingID);
    }
}
