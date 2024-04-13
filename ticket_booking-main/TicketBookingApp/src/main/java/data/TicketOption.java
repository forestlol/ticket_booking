package data;

public class TicketOption {
    private int ticketOptionID;
    private int eventID;
    private String optionName;
    private double priceMultiplier;
    private int totalAvailable;

    public TicketOption(int ticketOptionID, int eventID, String optionName, double priceMultiplier, int totalAvailable) {
        this.ticketOptionID = ticketOptionID;
        this.eventID = eventID;
        this.optionName = optionName;
        this.priceMultiplier = priceMultiplier;
        this.totalAvailable = totalAvailable;
    }

    public int getTicketOptionID() {
        return this.ticketOptionID;
    }

    public int getEventID() {
        return this.eventID;
    }

    public String getOptionName() {
        return this.optionName;
    }

    public double getPriceMultiplier() {
        return this.priceMultiplier;
    }

    public int getTotalAvailable() {
        return this.totalAvailable;
    }

    public String getCategoryName() {
        return this.optionName;
    }
}
