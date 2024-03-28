package data;

public class TicketOption {
    private int ticketOptionID;
    private int eventID;
    private String optionName;
    private int priceMultiplier;
    private int totalAvailable;

    public TicketOption(int ticketOptionID, int eventID, String optionName, int priceMultiplier, int totalAvailable) {
        this.ticketOptionID = ticketOptionID;
        this.eventID = eventID;
        this.optionName = optionName;
        this.priceMultipler = priceMultipler;
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

    public int getPriceMultipler() {
        return this.priceMultipler;
    }

    public int getTotalAvailable() {
        return this.totalAvailable;
    }
}
