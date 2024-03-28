package service;

import java.util.List;

public class RefundService {
    private DatabaseService databaseService;
    private BookingService bookingService;

    public RefundService(DatabaseService databaseService, BookingService bookingService) {
        this.databaseService = databaseService;
        this.bookingService = bookingService;
    }

    // Process refund for a single booking
    public Refund processRefund(String bookingID) {
        // Retrieve booking information from the database
        Booking booking = databaseService.getBookingByID(bookingID);

        if (booking != null) {
            // Process the refund using the BookingService
            Refund refund = bookingService.cancelBooking(booking);
            // Save the refund information to the database or perform other operations if needed
            databaseService.saveRefund(refund);

            return refund;
        } else {
            return null; // Handle case where booking is not found
        }
    }

    // Process refunds for a list of bookings
    public List<Refund> processRefund(List<String> bookingIDs) {
        List<Refund> refunds = bookingIDs.stream()
                .map(this::processRefund)
                .toList();

        // Save the refund information to the database or perform other operations if needed
        databaseService.saveRefunds(refunds);

        return refunds;
    }
}
