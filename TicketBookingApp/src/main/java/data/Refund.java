package data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

public class Refund {
    private int refundID;
    private int bookingID;
    private LocalDateTime refundDate;
    private String refundStatus;

    public Refund(int refundID, int bookingID, LocalDateTime refundDate, String refundStatus) {
        this.refundID = refundID;
        this.bookingID = bookingID;
        this.refundDate = refundDate;
        this.refundStatus = refundStatus;
    }

    // Getters
    public int getRefundID() {
        return this.refundID;
    }

    public int getBookingID() {
        return this.bookingID;
    }

    public LocalDateTime getRefundDate() {
        return this.refundDate;
    }

    public String getRefundStatus() {
        return this.refundStatus;
    }

    // Setters
    public void setRefundDate(LocalDateTime refundDate) {
        this.refundDate = refundDate;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    // Other methods
    public Map<String, Object> getRefundDetails() {
        Map<String, Object> refundDetails = new HashMap<>();
        refundDetails.put("refundID", refundID);
        refundDetails.put("bookingID", bookingID);
        refundDetails.put("refundDate", refundDate);
        refundDetails.put("refundStatus", refundStatus);

        return refundDetails;
    }
}
