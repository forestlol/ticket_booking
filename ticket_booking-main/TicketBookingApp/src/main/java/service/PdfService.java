package service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import data.Ticket;

public class PdfService {

    public void createEventDetailsPDF(String outputPath, String eventName, String eventDescription, String venue,
                                      LocalDateTime startTime, String ticketOptionName, double amountPaid, List<Ticket> tickets) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(100, 750);
                contentStream.showText("Event Details");
                contentStream.endText();

                // Event Name
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(100, 730);
                contentStream.showText("Event Name: " + eventName);
                contentStream.endText();

                // Event Description
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 715);
                contentStream.showText("Description: " + eventDescription);
                contentStream.endText();

                // Venue
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Venue: " + venue);
                contentStream.endText();

                // Start Time
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 685);
                contentStream.showText("Start Time: " + startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                contentStream.endText();

                // Ticket Option
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 670);
                contentStream.showText("Ticket Type: " + ticketOptionName);
                contentStream.endText();

                // Amount Paid
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 655);
                contentStream.showText("Amount Paid: " + amountPaid);
                contentStream.endText();

                // Tickets List Header
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 640);
                contentStream.showText("Tickets:");
                contentStream.endText();

                // List each ticket
                int yOffset = 625;
                for (Ticket ticket : tickets) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, yOffset);
                    contentStream.showText("- Ticket ID: " + ticket.getTicketID() + " | Guest: " + (ticket.getIsGuest() ? "Yes" : "No"));
                    contentStream.endText();
                    yOffset -= 15;
                }

            }

            document.save(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
