package service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

import data.Booking;
import data.Event;
import data.Refund;
import data.Ticket;
import data.TicketOption;
import user.Customer;
import user.EventManager;
import user.TicketingOfficer;
import user.User;


public class DatabaseService {
    private String host;
    private String name;
    private String username;
    private String password;
    private Connection connection;

    // Constructor
    public DatabaseService(String host, String name, String username, String password) throws SQLException {
        this.host = host;
        this.name = name;
        this.username = username;
        this.password = password;
        this.connect();
    }


    // Method to establish a database connection
    public void connect() throws SQLException {
        String url = "jdbc:mysql://" + this.host + "/" + this.name;
        this.connection = DriverManager.getConnection(url, this.username, this.password);
    }

    // Method to close the database connection
    public void disconnect() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
    }

    public Customer getCustomer(User user){
        if (user instanceof Customer) {
            return (Customer) user;
        }
        else {
            throw new RuntimeException("Error getting customer: user is not a customer");
        }
    }

    public EventManager getEventManager(User user){
        if (user instanceof EventManager) {
            return (EventManager) user;
        }
        else {
            throw new RuntimeException("Error getting event manager: user is not an event manager");
        }
    }

    public TicketingOfficer getTicketingOfficer(User user){
        if (user instanceof TicketingOfficer) {
            return (TicketingOfficer) user;
        }
        else {
            throw new RuntimeException("Error getting ticketing officer: user is not a ticketing officer");
        }
    }

    public User getUser(int userID) throws SQLException{
        String query = "SELECT * FROM User WHERE userID = ?";
        try (Connection conn = getConnection(); // Assuming you have a method getConnection() to get a DB connection
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String type = rs.getString("type");
                switch (type) {
                    case "Customer":
                        return new Customer(userID, email, password, name, type, accountBalance);
                    case "EventManager":
                        return new EventManager(userID, email, password, name, type);
                    case "TicketingOfficer":
                        return new TicketingOfficer(userID, email, password, name, type);
                    default:
                        return null; // Or throw an exception if appropriate
                }
            }
        }
        return null; // User not found
    }

    // -----------------------------------------------------------
    // ACCOUNT DATABASE

    public boolean authenticateUser(String email, String password) {
        String sql = "SELECT * FROM User WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql))
        {

            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next())
            {
                int userID = rs.getInt("userID");
                String name = rs.getString("name");
                String type = rs.getString("type");

                if ("Customer".equals(type)) {
                    double accountBalance = rs.getDouble("accountBalance");
                    return new Customer(userID, email, password, name, type, accountBalance);
                } else if ("EventManager".equals(type)) {
                    return new EventManager(userID, email, password, name, type);
                } else if ("TicketingOfficer".equals(type)) {
                    return new TicketingOfficer(userID, email, password, name, type);
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createUser(String email, String name, String password, String type) {
        // Implement user creation logic and store user information in the database
        // Return the created User object

        String query = "INSERT INTO users (email, password, name, type) VALUES (?, ?, ?, ?)";
        int userID = 0;

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.setString(3, name);
            pstmt.setString(4, type);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userID = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
            if ("Customer".equals(type)) {
                String insertCustomerSQL = "INSERT INTO Customer (userID, accountBalance) VALUES (?, ?)";
                try (PreparedStatement pstmtCustomer = conn.prepareStatement(insertCustomerSQL)) {
                    pstmtCustomer.setInt(1, userID);
                    pstmtCustomer.setDouble(2, accountBalance == null ? 0.0 : accountBalance);
                    pstmtCustomer.executeUpdate();
                }
                conn.commit();
                return true;
            } else if ("EventManager".equals(type)) {
                String insertManagerSQL = "INSERT INTO EventManager (userID) VALUES (?)";
                try (PreparedStatement pstmtManager = conn.prepareStatement(insertManagerSQL)) {
                    pstmtManager.setInt(1, userID);
                    pstmtManager.executeUpdate();
                }
                conn.commit();
                return true;
            } else if ("TicketingOfficer".equals(type)) {
                String insertOfficerSQL = "INSERT INTO TicketingOfficer (userID) VALUES (?)";
                try (PreparedStatement pstmtOfficer = conn.prepareStatement(insertOfficerSQL)) {
                    pstmtOfficer.setInt(1, userID);
                    pstmtOfficer.executeUpdate();
                }
                conn.commit();
                return true;
            } else {
                conn.rollback();
                throw new IllegalArgumentException("Invalid user type provided");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<Integer, Boolean> addAuthorisedOfficer(int eventManagerID, int eventID, List<Integer> userIDs) {

        HashMap<Integer, Boolean> results = new HashMap<>();

        // Check if the event manager is associated with the event
        if (getManagedEvents(eventManager.getID()).contains(getEvent(eventID))) {
            for (Integer userID : userIDs) {
                boolean success = false;
                // Check if the user is a ticketing officer before trying to add
                TicketingOfficer officer = getUser(userID);
                if (officer != null) {
                    String query = "INSERT INTO AuthorisedOfficers (eventID, ticketingOfficerID, timeStamp) VALUES (?, ?, NOW())";

                    try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
                        pstmt.setInt(1, eventID);
                        pstmt.setInt(2, userID);

                        int rowsAffected = pstmt.executeUpdate();
                        success = rowsAffected > 0;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        // Depending on how you want to handle exceptions, you might want to log this error or handle it differently
                        // For simplicity, I'm just printing the stack trace here
                    }
                }
                results.put(userID, success);
            }

            return results;
        }
        else {
            throw new RuntimeException("Error adding officer to event: event manager not associated with event");
        }
    }

    // Methods for Event Management

    public List<Ticket> getTicketsByEventID(int eventID) {
        // Retrieve a list of tickets associated with the specified event from the database
        List<Ticket> tickets = new ArrayList<Ticket>();
        String query1 = "SELECT * FROM bookings WHERE event_id = ?";
        String query2 = "SELECT * FROM tickets WHERE booking_id = ?";

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt1 = this.connection.prepareStatement(query1)) {
            pstmt1.setInt(1, eventID);
            ResultSet bookings_results = pstmt1.executeQuery();

            while (bookings_results.next()) {
                try (PreparedStatement pstmt2 = this.connection.prepareStatement(query2)) {
                    pstmt2.setInt(1, bookings_results.getInt("bookingID"));
                    ResultSet tickets_results = pstmt2.executeQuery();

                    while (tickets_results.next()) {
                        Ticket ticket = new Ticket(
                                tickets_results.getInt("ticketID"),
                                tickets_results.getBoolean("isGuest"),
                                tickets_results.getBoolean("attended")
                        );
                        tickets.add(ticket);
                    }
                }
            }
            return tickets;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // -----------------------------------------------------------
    // EVENT DATABASE

    public Event createEvent(int eventManagerID, Map<String, Object> details) {
        // Query to insert a new event
        String query = "INSERT INTO Event (eventManagerID, basePrice, eventName, eventDesc, venue, startTime, duration, revenue, currSlots, totalSlots, numTicketsAvailable, isCanclled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (getEventManager(accountService.getCurrentUser()) == null) {
            throw new RuntimeException("Error creating event: event manager not found");
        }

        try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            // Set values for the event insert query
            pstmt.setInt(1, eventManagerID);
            pstmt.setFloat(2, (Float) details.get("basePrice"));
            pstmt.setString(3, (String) details.get("eventName"));
            pstmt.setString(4, (String) details.get("eventDesc"));
            pstmt.setString(5, (String) details.get("venue"));
            pstmt.setTimestamp(6, Timestamp.valueOf((String) details.get("startTime")));
            pstmt.setInt(7, (Integer) details.get("duration"));
            pstmt.setInt(8, 0); // No revenue at the start
            pstmt.setInt(9, (Integer) details.get("totalSlots")); // Set current slots as the same as total slots
            pstmt.setInt(10, (Integer) details.get("totalSlots"));
            pstmt.setFloat(11, (Float) details.get("ticketCancellationFee"));
            pstmt.setInt(12, 1); // Event created won't be cancelled at the start, 1 is false for TinyInt

            int success = pstmt.executeUpdate();

            if (success == 1 && pstmt.getGeneratedKeys().next()) {
                int eventID = pstmt.getGeneratedKeys().getInt(1);

                // Directly handle ticket options here
                String ticketOptionQuery = "INSERT INTO TicketOption (eventID, optionName, priceMultiplier, totalAvailable) VALUES (?, ?, ?, ?)";
                List<Map<String, Object>> ticketOptions = (List<Map<String, Object>>) details.get("ticketOptions");

                for (Map<String, Object> option : ticketOptions) {
                    try (PreparedStatement pstmtOption = this.connection.prepareStatement(ticketOptionQuery)) {
                        pstmtOption.setInt(1, eventID);
                        pstmtOption.setString(2, (String) option.get("optionName"));
                        pstmtOption.setDouble(3, (Double) option.get("priceMultiplier"));
                        pstmtOption.setInt(4, (Integer) option.get("totalAvailable"));
                        pstmtOption.executeUpdate();
                    }
                }

                // Assuming a method getEvent(int eventID) that retrieves the created Event object
                return getEvent(eventID);
            } else {
                throw new RuntimeException("Creating event failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    public Event getEvent(int eventID) {
        String query = "SELECT * FROM Event WHERE eventID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            ResultSet events_result = pstmt.executeQuery();

            // Parse the results and create Event objects
            while (events_result.next()) {

                Event event = new Event(
                        events_result.getInt("eventID"),
                        events_result.getInt("eventManagerID"),
                        events_result.getDouble("basePrice"),
                        events_result.getString("eventName"),
                        events_result.getString("eventDesc"),
                        events_result.getString("venue"),
                        events_result.getTimestamp("startTime").toLocalDateTime(),
                        events_result.getInt("duration"),
                        events_result.getDouble("revenue"),
                        events_result.getInt("currSlots"),
                        events_result.getInt("totalSlots"),
                        events_result.getDouble("ticketCancellationFee"),
                        events_result.getInt("isCancelled") != 0
                );
                return event;
            }
            throw new RuntimeException("Error getting event: event not found");
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Event> getAllEvents() {
        String query = "SELECT * FROM Event";
        List<Event> events = new ArrayList<Event>();

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            ResultSet events_results = pstmt.executeQuery();

            while (events_results.next()) {
                Event event = new Event(
                        events_result.getInt("eventID"),
                        events_result.getInt("eventManagerID"),
                        events_result.getDouble("basePrice"),
                        events_result.getString("eventName"),
                        events_result.getString("eventDesc"),
                        events_result.getString("venue"),
                        events_result.getTimestamp("startTime").toLocalDateTime(),
                        events_result.getInt("duration"),
                        events_result.getDouble("revenue"),
                        events_result.getInt("currSlots"),
                        events_result.getInt("totalSlots"),
                        events_result.getDouble("ticketCancellationFee"),
                        events_result.getInt("isCancelled") != 0
                );
                events.add(event);
            }
            return events;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // Event Manager
    public List<Event> getManagedEvents(int eventManagerID) {

        String query = "SELECT * FROM Event WHERE eventManagerID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventManagerID);
            ResultSet events_results = pstmt.executeQuery();
            List<Event> events = new ArrayList<Event>();

            // Parse the results and create Event objects
            while (events_results.next()) {
                Event event = new Event(
                        events_result.getInt("eventID"),
                        events_result.getInt("eventManagerID"),
                        events_result.getDouble("basePrice"),
                        events_result.getString("eventName"),
                        events_result.getString("eventDesc"),
                        events_result.getString("venue"),
                        events_result.getTimestamp("startTime").toLocalDateTime(),
                        events_result.getInt("duration"),
                        events_result.getDouble("revenue"),
                        events_result.getInt("currSlots"),
                        events_result.getInt("totalSlots"),
                        events_result.getDouble("ticketCancellationFee"),
                        events_result.getInt("isCancelled") != 0
                );
                events.add(event);
            }
            return events;

        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // Ticketing Officer
    public List<Event> getAuthorisedEvents(int ticketingOfficerID)
    {
        List<Event> events = new ArrayList<>();
        String query = "SELECT e.* FROM Event e JOIN AuthorisedOfficers ao ON e.eventID = ao.eventID WHERE ao.ticketingOfficerID = ?";
        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, ticketingOfficerID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int eventID = rs.getInt("eventID");
                int eventManagerID = rs.getInt("eventManagerID");
                double basePrice = rs.getDouble("basePrice");
                String eventName = rs.getString("eventName");
                String eventDesc = rs.getString("eventDesc");
                String venue = rs.getString("venue");
                LocalDateTime startTime = rs.getTimestamp("startTime").toLocalDateTime();
                int duration = rs.getInt("duration");
                double revenue = rs.getDouble("revenue");
                int currSlots = rs.getInt("currSlots");
                int totalSlots = rs.getInt("totalSlots");
                double ticketCancellationFee = rs.getDouble("ticketCancellationFee");
                boolean isCancelled = rs.getInt("isCancelled") != 0;

                Event event = new Event(
                        eventID,
                        eventManagerID,
                        basePrice,
                        eventName,
                        eventDesc,
                        venue,
                        startTime,
                        duration,
                        revenue,
                        currSlots,
                        totalSlots,
                        ticketCancellationFee,
                        isCancelled
                );
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    public Event updateEvent(int eventID, int eventManagerID, Map<String, Object> details) {

        String query = "UPDATE Event SET basePrice = ?, eventName = ?, eventDesc = ?, venue = ?, startTime = ?, duration = ?, revenue = ?, currSlots = ?, totalSlots = ?, ticketCancellationFee = ? WHERE eventID = ?";

        // Update the details of the specified event in the database
        int eventID = event.getEventID();

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setFloat(1, (Float) details.get("basePrice"));
            pstmt.setString(2, (String) details.get("eventName"));
            pstmt.setString(3, (String) details.get("eventDesc"));
            pstmt.setString(4, (String) details.get("venue"));
            pstmt.setTimestamp(5, Timestamp.valueOf((String) details.get("startTime")));
            pstmt.setInt(6, (Integer) details.get("duration"));
            pstmt.setInt(7, (Float) details.get("revenue"));
            pstmt.setInt(8, (Integer) details.get("currSlots"));
            pstmt.setInt(9, (Integer) details.get("totalSlots"));
            pstmt.setFloat(10, (Float) details.get("ticketCancellationFee"));

            int success = pstmt.executeUpdate();

            if (success > 0) {
                return getEvent(eventID);
            }
            else {
                throw new RuntimeException("Error updating event: event not updated");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    public boolean cancelEvent(int eventManagerID, int eventID) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection(); // Assuming you have a method to get the DB connection
            conn.setAutoCommit(false); // Start transaction

            // Step 1: Get all bookingIDs tagged with this eventID
            List<Integer> bookingIDs = new ArrayList<>();
            String getBookingsQuery = "SELECT bookingID, customerID, amountPaid FROM Booking WHERE eventID = ?";
            pstmt = conn.prepareStatement(getBookingsQuery);
            pstmt.setInt(1, eventID);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                bookingIDs.add(rs.getInt("bookingID"));
                int customerID = rs.getInt("customerID");
                double amountPaid = rs.getDouble("amountPaid");

                // Step 2: For each booking, create a Refund record
                String insertRefundQuery = "INSERT INTO Refund (bookingID, refundDate, refundStatus) VALUES (?, NOW(), 'Processed')";
                try (PreparedStatement refundPstmt = conn.prepareStatement(insertRefundQuery)) {
                    refundPstmt.setInt(1, rs.getInt("bookingID"));
                    refundPstmt.executeUpdate();
                }

                // Step 3: Update the customer's account balance
                String updateBalanceQuery = "UPDATE Customer SET accountBalance = accountBalance + ? WHERE userID = ?";
                try (PreparedStatement balancePstmt = conn.prepareStatement(updateBalanceQuery)) {
                    balancePstmt.setDouble(1, amountPaid);
                    balancePstmt.setInt(2, customerID);
                    balancePstmt.executeUpdate();
                }

                // Step 4: Decrease the event's revenue by the amount paid for each booking
                // This step will be done after all bookings are processed to ensure only one update operation
            }

            // Perform the revenue update outside of the loop
            String updateEventRevenueQuery = "UPDATE Event SET revenue = revenue - (SELECT SUM(amountPaid) FROM Booking WHERE eventID = ?), isCancelled = 1 WHERE eventID = ?";
            try (PreparedStatement revenuePstmt = conn.prepareStatement(updateEventRevenueQuery)) {
                revenuePstmt.setInt(1, eventID);
                revenuePstmt.setInt(2, eventID);
                revenuePstmt.executeUpdate();
            }

            conn.commit(); // Commit the transaction
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback in case of any failure
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            // Clean up resources
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // -----------------------------------------------------------
    // TICKET DATABASE

    // Methods for Ticket and Booking Operations

    //region Booking & Ticket
    public HashMap<Integer,Boolean> createTicket(int bookingID, HashMap<String,Integer> ticketMap) {

        int guest = ticketMap.get("guest");
        int total = ticketMap.get("total");

        // TicketID, success
        HashMap<Integer,Boolean> results = new HashMap<Integer,Boolean>();


        for (int i = 0; i < total; i++) {
            String query = "INSERT INTO tickets (bookingID, isGuest) VALUES (?, ?)";

            // Try-with-resources to ensure that resources are freed properly
            try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, bookingID);
                pstmt.setBoolean(2, i < guest);

                int success = pstmt.executeUpdate();

                if (success > 0) {
                    // Retrieve the generated keys
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            results.put(generatedKeys.getInt(1), true);
                        } else {
                            results.put(i, false);
                        }
                    }
                } else {
                    results.put(i, false);
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }

        // Create tickets for the specified event and quantity in the database
        return results;
    }


    public Ticket getTicket(int ticketID) {
        // Retrieve a ticket from the database based on the ticket ID
        String query = "SELECT * FROM tickets WHERE ticketID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, ticketID);
            ResultSet ticket = pstmt.executeQuery();

            if (ticket.next()) {
                return new Ticket(
                        ticket.getInt("ticketID"),
                        ticket.getBoolean("isGuest"),
                        ticket.getBoolean("attended")
                );
            }
            else {
                throw new RuntimeException("Error getting ticket: ticket not found");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public boolean checkTicketAvailability(int eventID, int quantity) {

        String query = "SELECT * FROM events WHERE eventID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            ResultSet event = pstmt.executeQuery();

            if (event.next()) {
                return event.getInt("numTicketsAvailable") >= quantity;
            }
            else {
                throw new RuntimeException("Error checking ticket availability: event not found");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    public boolean verifyTicket(int ticketID) {

        // Check if the specified ticket exist and has not been verified
        String query = "SELECT * FROM tickets WHERE ticketID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, ticketID);
            ResultSet ticket = pstmt.executeQuery();

            if (ticket.next()) {
                // Update the ticket to mark it as verified
                String query2 = "UPDATE tickets SET attended = ? WHERE ticketID = ?";
                try (PreparedStatement pstmt2 = this.connection.prepareStatement(query2)) {
                    pstmt2.setBoolean(1, true);
                    pstmt2.setInt(2, ticketID);

                    int success = pstmt2.executeUpdate();

                    return success > 0;
                }
                catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
            else {
                throw new RuntimeException("Error verifying ticket: ticket not found");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    public List<Ticket> getTicketsByBookingID(int bookingID) {
        // Retrieve a list of tickets associated with the specified booking from the database
        String query = "SELECT * FROM tickets WHERE bookingID = ?";
        List<Ticket> tickets = new ArrayList<Ticket>();

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, bookingID);
            ResultSet tickets_results = pstmt.executeQuery();

            while (tickets_results.next()) {
                Ticket ticket = new Ticket(
                        tickets_results.getInt("ticketID"),
                        tickets_results.getBoolean("isGuest"),
                        tickets_results.getBoolean("attended")
                );
                tickets.add(ticket);
            }
            return tickets;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public TicketOption getTicketOptionByBookingID(int bookingID) {
        String query = "SELECT * FROM ticket_options WHERE bookingID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, bookingID);
            ResultSet ticketOption = pstmt.executeQuery();

            if (ticketOption.next()) {
                return new TicketOption(
                        ticketOption.getInt("ticketOptionID"),
                        ticketOption.getInt("totalAvailable"),
                        ticketOption.getInt("priceMultipler"),
                        ticketOption.getString("name")
                );
            }
            else {
                throw new RuntimeException("Error getting ticket option: ticket option not found");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Integer> getTicketOptionsByEventID(Integer eventID){
        String query = "SELECT * FROM event_ticket_options WHERE event_id = ?";
        List<Integer> ticketOptions = new ArrayList<Integer>();

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            ResultSet ticketOptions_results = pstmt.executeQuery();

            while (ticketOptions_results.next()) {
                ticketOptions.add(ticketOptions_results.getInt("ticketOptionID"));
            }
            return ticketOptions;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Ticket> getTickets(int userID) {

        try{
            // Get bookings for the user
            List<Booking> bookings = getBookings(userID);

            // Get tickets for each booking
            List<Ticket> tickets = new ArrayList<Ticket>();
            for (Booking booking : bookings) {
                tickets.addAll(booking.getTickets());
            }

            return tickets;

        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public TicketOption createTicketOption(int eventID, String name, int totalAvailable, double priceMultiplier) {
        String query = "INSERT INTO ticket_options (eventID, name, totalAvailable, priceMultiplier) VALUES (?, ?, ?, ?)";

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, eventID);
            pstmt.setString(2, name);
            pstmt.setInt(3, totalAvailable);
            pstmt.setDouble(4, priceMultiplier);

            int success = pstmt.executeUpdate();

            if (success > 0) {
                // Retrieve the generated keys
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return new TicketOption(
                                generatedKeys.getInt(1),
                                totalAvailable,
                                priceMultiplier,
                                name
                        );
                    } else {
                        throw new SQLException("Creating ticket option failed, no ticket option ID obtained.");
                    }
                }
            } else {
                throw new SQLException("Creating ticket option failed, no rows affected.");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // -----------------------------------------------------------
    // BOOKING DATABASE


    public Booking createBooking(int eventID, int ticketOptionID, int customerID, int ticketingOfficerID, int numOfTickets, float amountPaid) {
        if (numOfTickets < 1 || numOfTickets > 5) {
            throw new IllegalArgumentException("Number of tickets must be between 1 and 5.");
        }

        connect(); // Ensure connection is open

        try {
            this.connection.setAutoCommit(false); // Start transaction

            // Check current slots and calculate amount paid to add into event's revenue
            double amountPaid = calculateAmountPaidAndUpdateSlots(eventID, ticketOptionID, numOfTickets, amountPaid);
            if (amountPaid == -1) {
                return null; // Requirements not met or insufficient slots
            }

            if (!updateCustomerBalance(customerID, amountPaid)) {
                throw new SQLException("Insufficient funds for the customer."); // Requirement 4
            }

            int bookingID = createBookingRecord(eventID, ticketOptionID, customerID, ticketingOfficerID, numOfTickets, amountPaid);

            // Handles ticket creation and links them to the booking (Requirement 3)
            List<Ticket> tickets = createTicket(bookingID, false, numOfTickets);

            this.connection.commit(); // Commit transaction

            // Requirement 7: Return the Booking class
            return new Booking(bookingID, eventID, customerID, ticketingOfficerID, ticketOptionID, amountPaid, tickets, LocalDateTime.now()));
        } catch (SQLException e) {
            this.connection.rollback(); // Rollback in case of any error
            throw e; // Rethrow the exception to be handled or logged by the caller
        } finally {
            disconnect(); // Ensure connection is closed
        }
    }

    // Check if event's current slot is more than number of tickets, add revenue with amount paid based on basePrice and priceMultiplier, and update current slots to minus off number of tickets
    private double calculateAmountPaidAndUpdateSlots(int eventID, int ticketOptionID, int numOfTickets) throws SQLException {
        double amountPaid = -1;
        String eventQuery = "SELECT basePrice, currSlots FROM Event WHERE eventID = ? FOR UPDATE";
        String ticketOptionQuery = "SELECT priceMultiplier, totalAvailable FROM TicketOption WHERE ticketOptionID = ?";

        try (Connection conn = getConnection();
             PreparedStatement eventStmt = conn.prepareStatement(eventQuery);
             PreparedStatement ticketOptionStmt = conn.prepareStatement(ticketOptionQuery)) {

            eventStmt.setInt(1, eventID);
            ResultSet eventRs = eventStmt.executeQuery();

            if (eventRs.next()) {
                double basePrice = eventRs.getDouble("basePrice");
                int currSlots = eventRs.getInt("currSlots");

                if (currSlots >= numOfTickets) {
                    ticketOptionStmt.setInt(1, ticketOptionID);
                    ResultSet ticketOptionRs = ticketOptionStmt.executeQuery();

                    if (ticketOptionRs.next()) {
                        float priceMultiplier = ticketOptionRs.getFloat("priceMultiplier");
                        int totalAvailable = ticketOptionRs.getInt("totalAvailable");

                        amountPaid = basePrice * priceMultiplier * numOfTickets;

                        // Update currSlots in Event
                        String updateEventSlotsQuery = "UPDATE Event SET currSlots = currSlots - ?, revenue + ? WHERE eventID = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateEventSlotsQuery)) {
                            updateStmt.setInt(1, numOfTickets);
                            updateStmt.setFloat(2, amountPaid);
                            updateStmt.setInt(3, eventID);
                            updateStmt.executeUpdate();
                        }
                    }
                }
            }
        }
        return amountPaid;
    }

    // Update account balance only if it's more than amount paid
    private boolean updateCustomerBalance(int customerID, double amountPaid) throws SQLException {
        String updateCustomerBalanceQuery = "UPDATE Customer SET accountBalance = accountBalance - ? WHERE userID = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateCustomerBalanceQuery)) {

            if(accountBalance >= amountPaid)
            {
                pstmt.setDouble(1, amountPaid);
                pstmt.setInt(2, customerID);
                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0;
            }
        }
    }

    // Create given number of tickets into database
    private List<Ticket> createTicket(int bookingID, boolean isGuest, int numOfTickets) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String insertTicketQuery = "INSERT INTO Ticket (bookingID, isGuest, attended) VALUES (?, ?, 0)";
        String fetchLastInsertedId = "SELECT LAST_INSERT_ID()";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertTicketQuery, Statement.RETURN_GENERATED_KEYS);
             Statement stmt = conn.createStatement()) {

            for (int i = 0; i < numOfTickets; i++) {
                pstmt.setInt(1, bookingID);
                pstmt.setBoolean(2, isGuest);
                pstmt.executeUpdate();

                // Retrieve the generated ticket ID for each ticket
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int ticketID = generatedKeys.getInt(1);
                        // Assuming attended is false initially
                        tickets.add(new Ticket(ticketID, bookingID, isGuest, false));
                    }
                }
            }
        }
        return tickets;
    }

    private int createBookingRecord(int eventID, int ticketOptionID, int customerID, int ticketingOfficerID, int numOfTickets, double amountPaid) throws SQLException {
        String insertBookingQuery = "INSERT INTO Booking (eventID, ticketOptionID, customerID, ticketingOfficerID, numOfTickets, amountPaid, bookedTime, bookingStatus) VALUES (?, ?, ?, ?, ?, ?, NOW(), 'Booked')";
        int bookingID = -1;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertBookingQuery, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, eventID);
            pstmt.setInt(2, ticketOptionID);
            pstmt.setInt(3, customerID);
            pstmt.setInt(4, ticketingOfficerID);
            pstmt.setInt(5, numOfTickets);
            pstmt.setDouble(6, amountPaid);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bookingID = generatedKeys.getInt(1);
                }
            }
        }
        return bookingID;
    }

    public List<Booking> getBookings(int userID) {
        String query = "SELECT * FROM bookings WHERE customerID = ?";
        List<Booking> bookings = new ArrayList<>();

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, userID);
            ResultSet bookingsResults = pstmt.executeQuery();

            while (bookingsResults.next()) {
                // Assuming you have methods to fetch the related objects based on ID
                List<Ticket> tickets = getTicketsByBookingID(bookingsResults.getInt("bookingID"));

                Booking booking = new Booking(
                        bookingsResults.getInt("bookingID"),
                        bookingsResults.getInt("eventID"),
                        bookingsResults.getInt("customerID"),
                        bookingsResults.getInt("ticketingOfficerID"),
                        bookingsResults.getInt("ticketOptionID"),
                        bookingsResults.getDouble("amountPaid"),
                        tickets,
                        bookingsResults.getTimestamp("bookedTime").toLocalDateTime(),
                        bookingsResults.getString("bookingStatus")
                );

                bookings.add(booking);
            }
            return bookings;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error retrieving bookings: " + e.getMessage());
        }
    }

    public List<Booking> getBookingsByEvent(int eventID) {
        String query = "SELECT * FROM bookings WHERE eventID = ?";
        List<Booking> bookings = new ArrayList<>();

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            ResultSet bookingsResults = pstmt.executeQuery();

            while (bookingsResults.next()) {
                int bookingID = bookingsResults.getInt("bookingID");
                int customerID = bookingsResults.getInt("customerID");
                int ticketingOfficerID = bookingsResults.getInt("ticketingOfficerID");
                int ticketOptionID = bookingsResults.getInt("ticketOptionID");
                double amountPaid = bookingsResults.getDouble("amountPaid");
                LocalDateTime bookedTime = bookingsResults.getTimestamp("bookedTime").toLocalDateTime();

                List<Ticket> tickets = getTicketsByBookingID(bookingID);

                // Create a new Booking object with the retrieved data
                Booking booking = new Booking(
                        bookingID,
                        eventID,
                        customerID,
                        ticketingOfficerID,
                        ticketOptionID,
                        amountPaid,
                        tickets,
                        bookedTime,
                        bookingStatus
                );

                bookings.add(booking);
            }
            return bookings;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error retrieving bookings for event: " + e.getMessage());
        }
    }

    public boolean cancelBooking(int bookingID, int userID) {
        Connection conn = this.connection;

        try {
            // Start transaction
            conn.setAutoCommit(false);

            // Step 1: Retrieve eventID and ticketOptionID from booking to get cancellation fee and number of tickets
            String bookingQuery = "SELECT eventID, ticketOptionID, numOfTickets FROM Booking WHERE bookingID = ? AND customerID = ?";
            int eventID, ticketOptionID, numOfTickets;
            double cancellationFee;
            try (PreparedStatement pstmtBooking = conn.prepareStatement(bookingQuery)) {
                pstmtBooking.setInt(1, bookingID);
                pstmtBooking.setInt(2, userID);
                ResultSet rsBooking = pstmtBooking.executeQuery();
                if (rsBooking.next()) {
                    eventID = rsBooking.getInt("eventID");
                    ticketOptionID = rsBooking.getInt("ticketOptionID");
                    numOfTickets = rsBooking.getInt("numOfTickets");
                } else {
                    conn.rollback();
                    return false; // Booking not found or does not belong to the user
                }
            }

            // Step 2: Retrieve cancellation fee from Event and calculate refund amount
            String eventQuery = "SELECT ticketCancellationFee FROM Event WHERE eventID = ?";
            try (PreparedStatement pstmtEvent = conn.prepareStatement(eventQuery)) {
                pstmtEvent.setInt(1, eventID);
                ResultSet rsEvent = pstmtEvent.executeQuery();
                if (rsEvent.next()) {
                    cancellationFee = rsEvent.getDouble("ticketCancellationFee");
                } else {
                    conn.rollback();
                    return false; // Event not found
                }
            }

            double refundAmount = cancellationFee * numOfTickets;

            // Step 3: Update customer's account balance
            String updateCustomerQuery = "UPDATE Customer SET accountBalance = accountBalance + ? WHERE userID = ?";
            try (PreparedStatement pstmtUpdateCustomer = conn.prepareStatement(updateCustomerQuery)) {
                pstmtUpdateCustomer.setDouble(1, refundAmount);
                pstmtUpdateCustomer.setInt(2, userID);
                pstmtUpdateCustomer.executeUpdate();
            }

            // Step 4: Add back current slots in the event
            String updateEventSlotsQuery = "UPDATE Event SET currSlots = currSlots + ? WHERE eventID = ?";
            try (PreparedStatement pstmtUpdateEventSlots = conn.prepareStatement(updateEventSlotsQuery)) {
                pstmtUpdateEventSlots.setInt(1, numOfTickets);
                pstmtUpdateEventSlots.setInt(2, eventID);
                pstmtUpdateEventSlots.executeUpdate();
            }

            // Step 5: Create Refund entry
            String insertRefundQuery = "INSERT INTO Refund (bookingID, refundDate, refundStatus) VALUES (?, NOW(), 'Processed')";
            try (PreparedStatement pstmtInsertRefund = conn.prepareStatement(insertRefundQuery)) {
                pstmtInsertRefund.setInt(1, bookingID);
                pstmtInsertRefund.executeUpdate();
            }

            // Step 6: Decrease event revenue
            String updateEventRevenueQuery = "UPDATE Event SET revenue = revenue - ? WHERE eventID = ?";
            try (PreparedStatement pstmtUpdateEventRevenue = conn.prepareStatement(updateEventRevenueQuery)) {
                pstmtUpdateEventRevenue.setDouble(1, refundAmount);
                pstmtUpdateEventRevenue.setInt(2, eventID);
                pstmtUpdateEventRevenue.executeUpdate();
            }

            // Step 7: Mark booking as cancelled
            String cancelBookingQuery = "UPDATE Booking SET bookingStatus = 'Cancelled' WHERE bookingID = ?";
            try (PreparedStatement pstmtCancelBooking = conn.prepareStatement(cancelBookingQuery)) {
                pstmtCancelBooking.setInt(1, bookingID);
                pstmtCancelBooking.executeUpdate();
            }

            // Commit transaction
            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                // Rollback in case of any error
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------
    // REFUND DATABASE

    public Refund createRefund(int bookingID, int userID) {
        String query = "INSERT INTO refunds (bookingID, customerID, refundTime) VALUES (?, ?, ?)";
        Event event = getEvent(getBooking(bookingID).getEventID());
        double cancelationFee = event.getTicketCancellationFee();
        double multiplier = (double) getBooking(bookingID).getTicketOption().getPriceMultiplier();
        int numberOfTickets = getBooking(bookingID).getTickets().size();

        // Calculate the refund amount
        double refundAmount = (multiplier * event.getBasePrice() * numberOfTickets) - cancelationFee;

        // Set refund date as current date
        LocalDateTime refundDate = java.time.LocalDateTime.now();

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, bookingID);
            pstmt.setInt(2, userID);
            pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

            int success = pstmt.executeUpdate();

            if (success > 0) {
                // Retrieve the generated keys
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int refundID = generatedKeys.getInt(1);
                        return new Refund(
                                refundID,
                                getBooking(bookingID).getBookingId(),
                                refundAmount,
                                refundDate,
                                "pending"
                        );
                    } else {
                        throw new SQLException("Creating refund failed, no refund ID obtained.");
                    }
                }
            } else {
                throw new SQLException("Creating refund failed, no rows affected.");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    //endregion
}
