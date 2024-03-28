package service;

import java.util.HashMap;
import java.util.Map;

public class StatisticsService {
    private BookingService bookingService;

    public StatisticsService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Generate a report for a specific type of statistics related to an event
    public Map<String, Double> generateReport(String type, Event event) {
        Map<String, Double> report = new HashMap<>();

        switch (type.toLowerCase()) {
            case "attendance":
                int totalBookings = bookingService.getTotalBookingsForEvent(event);
                int totalGuests = bookingService.getTotalGuestsForEvent(event);
                double attendancePercentage = calculateAttendancePercentage(totalBookings, totalGuests);
                report.put("Attendance Percentage", attendancePercentage);
                break;

            // Add more cases for other types of statistics as needed

            default:
                // Handle unknown statistic type
                System.out.println("Unknown statistic type");
        }

        return report;
    }

    // Export data for a specific type related to an event
    public boolean exportData(String type, Event event) {
        // Assume exporting data involves writing to a file or external system
        // You may implement this based on your application's requirements
        // Return true if the export is successful, false otherwise
        return true;
    }

    // Helper method to calculate attendance percentage
    private double calculateAttendancePercentage(int totalBookings, int totalGuests) {
        if (totalBookings == 0) {
            return 0.0; // Avoid division by zero
        }
        return (double) totalGuests / totalBookings * 100;
    }
}
