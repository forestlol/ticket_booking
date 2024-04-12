package service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.sql.SQLException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private DatabaseService databaseService;

    public StatisticsService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Map<Integer, Integer> getTotalTicketsSold(List<Integer> eventIDs) throws SQLException {
        return this.databaseService.getTotalTicketsSold(eventIDs);
    }

    public Map<Integer, Double> getRevenue(List<Integer> eventIDs) throws SQLException {
        return this.databaseService.getRevenue(eventIDs);
    }

    public Map<Integer, Double> getAttendanceRate(List<Integer> eventIDs) throws SQLException {
        return this.databaseService.getAttendanceRate(eventIDs);
    }

    public Map<Integer, Map<String, Integer>> getTicketTypeBreakdown(List<Integer> eventIDs) throws SQLException {
        return this.databaseService.getTicketTypeBreakdown(eventIDs);
    }

    public void exportDataToCSV(List<Integer> eventIDs, String outputPath) throws IOException, SQLException {
        Map<Integer, Integer> ticketsSold = getTotalTicketsSold(eventIDs);
        Map<Integer, Double> revenue = getRevenue(eventIDs);
        Map<Integer, Double> attendanceRates = getAttendanceRate(eventIDs);
        Map<Integer, Map<String, Integer>> ticketTypeBreakdown = getTicketTypeBreakdown(eventIDs);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Event ID", "Tickets Sold", "Revenue", "Attendance Rate", "Ticket Type Breakdown"))) {

            for (Integer eventID : eventIDs) {
                int tickets = ticketsSold.getOrDefault(eventID, 0);
                double rev = revenue.getOrDefault(eventID, 0.0);
                double attendanceRate = attendanceRates.getOrDefault(eventID, 0.0);
                Map<String, Integer> typeBreakdown = ticketTypeBreakdown.get(eventID);
                String typeBreakdownStr = (typeBreakdown != null) ? typeBreakdown.entrySet().stream()
                        .map(e -> e.getKey() + ": " + e.getValue())
                        .collect(Collectors.joining(", ")) : "No data";

                // Print each record to CSV
                csvPrinter.printRecord(eventID, tickets, rev, String.format("%.2f%%", attendanceRate * 100), typeBreakdownStr);
            }

            csvPrinter.flush();
        }
    }

}
