package service;

import java.sql.*;
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
        if(this.connection == null || this.connection.isClosed()) {
            String url = "jdbc:mysql://" + this.host + "/" + this.name;
            this.connection = DriverManager.getConnection(url, this.username, this.password);
        }
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

    public User getUser(int userID) {
        String query = "SELECT * FROM User WHERE userID = ?";
        try (Connection conn = this.connection;
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                String password = rs.getString("password");
                String name = rs.getString("name");
                String type = rs.getString("type");

                Double accountBalance = type.equals("Customer") ? rs.getDouble("accountBalance") : null;
                if (accountBalance != null && rs.wasNull()) {
                    accountBalance = 0.0; // Handle null value for account balance.
                }

                switch (type) {
                    case "Customer":
                        return new Customer(userID, email, password, name, type, accountBalance);
                    case "EventManager":
                        return new EventManager(userID, email, password, name, type);
                    case "TicketingOfficer":
                        return new TicketingOfficer(userID, email, password, name, type);
                    default:
                        return null; // Or throw an exception if the type is unrecognized.
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return null; // User not found.
    }

    // -----------------------------------------------------------
    // ACCOUNT DATABASE

    public User authenticateUser(String email, String password) {
        String sql = "SELECT u.*, c.accountBalance FROM user u LEFT JOIN customer c ON u.userID = c.userID WHERE u.email = ? AND u.password = ?";
        try (PreparedStatement stmt = this.connection.prepareStatement(sql))
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

    public boolean createUser(String email, String password, String name , String type) {
        // Implement user creation logic and store user information in the database
        // Return the created User object

        String query = "INSERT INTO User (email, password, name, type) VALUES (?, ?, ?, ?)";
        int userID = 0;
        try {
            this.connection.setAutoCommit(false);

            // Try-with-resources to ensure that resources are freed properly
            try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, email);
                pstmt.setString(2, password);
                pstmt.setString(3, name);
                pstmt.setString(4, type);
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        userID = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
                if ("Customer".equals(type)) {
                    String insertCustomerSQL = "INSERT INTO Customer (userID, accountBalance) VALUES (?, ?)";
                    try (PreparedStatement pstmtCustomer = this.connection.prepareStatement(insertCustomerSQL)) {
                        pstmtCustomer.setInt(1, userID);
                        pstmtCustomer.setDouble(2, 1000); // Default value of $1000
                        pstmtCustomer.executeUpdate();
                    }
                    this.connection.commit();
                    return true;
                } else if ("EventManager".equals(type)) {
                    String insertManagerSQL = "INSERT INTO EventManager (userID) VALUES (?)";
                    try (PreparedStatement pstmtManager = this.connection.prepareStatement(insertManagerSQL)) {
                        pstmtManager.setInt(1, userID);
                        pstmtManager.executeUpdate();
                    }
                    this.connection.commit();
                    return true;
                } else if ("TicketingOfficer".equals(type)) {
                    String insertOfficerSQL = "INSERT INTO TicketingOfficer (userID) VALUES (?)";
                    try (PreparedStatement pstmtOfficer = this.connection.prepareStatement(insertOfficerSQL)) {
                        pstmtOfficer.setInt(1, userID);
                        pstmtOfficer.executeUpdate();
                    }
                    this.connection.commit();
                    return true;
                } else {
                    this.connection.rollback();
                    throw new IllegalArgumentException("Invalid user type provided");
                }
            }
            catch (SQLException e) {
                this.connection.rollback();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<Integer, Boolean> addAuthorisedOfficer(int eventManagerID, int eventID, List<Integer> userIDs) throws SQLException {

        HashMap<Integer, Boolean> results = new HashMap<>();

        boolean originalAutoCommit = this.connection.getAutoCommit();
        try {
            if (originalAutoCommit) {
                this.connection.setAutoCommit(false);
            }

            List<Event> managedEvents = getManagedEvents(eventManagerID);
            boolean isEventManagedByEventManager = managedEvents.stream()
                    .anyMatch(event -> event.getEventID() == eventID);

            // Check if the event manager is associated with the event
            if (isEventManagedByEventManager) {
                for (Integer userID : userIDs) {
                    boolean success = false;
                    // Check if the user is a ticketing officer before trying to add
                    TicketingOfficer officer = (TicketingOfficer) getUser(userID);
                    if (officer != null) {
                        if (this.connection == null || this.connection.isClosed()) {
                            this.connect();
                        }
                        String query = "INSERT INTO AuthorisedOfficers (eventID, ticketingOfficerID, timeStamp) VALUES (?, ?, NOW())";

                        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
                            pstmt.setInt(1, eventID);
                            pstmt.setInt(2, userID);

                            int rowsAffected = pstmt.executeUpdate();
                            success = rowsAffected > 0;
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    results.put(userID, success);
                }
                originalAutoCommit = this.connection.getAutoCommit();
                if (!originalAutoCommit) {
                    this.connection.commit();
                }
            } else {
                throw new RuntimeException("Error adding officer to event: event manager not associated with event");
            }
        } catch (SQLException e) {
            if (!originalAutoCommit) {
                try {
                    this.connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace(); // Handle rollback exception
                }
            }
            e.printStackTrace();
        } finally {
            if (!originalAutoCommit) {
                try {
                    this.connection.setAutoCommit(true); // Restore original auto-commit state
                } catch (SQLException e) {
                    e.printStackTrace(); // Handle exception on resetting auto-commit
                }
            }
        }
        return results;
    }

    // -----------------------------------------------------------
    // EVENT DATABASE

    public Event createEvent(int eventManagerID, Map<String, Object> details) {
        // Query to insert a new event
        String query = "INSERT INTO Event (eventManagerID, basePrice, eventName, eventDesc, venue, startTime, duration, revenue, currSlots, totalSlots, numTicketsAvailable, isCancelled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (getEventManager(AccountService.getCurrentUser()) == null) {
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
            pstmt.setDouble(11, (Float) details.get("ticketCancellationFee"));
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
            return null;
        }
    }

    public List<Event> getAllEvents() {
        String query = "SELECT * FROM Event";
        List<Event> events = new ArrayList<Event>();

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            ResultSet events_result = pstmt.executeQuery();

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
            ResultSet events_result = pstmt.executeQuery();
            List<Event> events = new ArrayList<Event>();

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
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
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
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setDouble(1, (Float) details.get("basePrice"));
            pstmt.setString(2, (String) details.get("eventName"));
            pstmt.setString(3, (String) details.get("eventDesc"));
            pstmt.setString(4, (String) details.get("venue"));
            pstmt.setTimestamp(5, Timestamp.valueOf((String) details.get("startTime")));
            pstmt.setInt(6, (Integer) details.get("duration"));
            pstmt.setDouble(7, (Float) details.get("revenue"));
            pstmt.setInt(8, (Integer) details.get("currSlots"));
            pstmt.setInt(9, (Integer) details.get("totalSlots"));
            pstmt.setDouble(10, (Float) details.get("ticketCancellationFee"));

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

        try {
            this.connection.setAutoCommit(false); // Start transaction

            // Step 1: Get all bookingIDs tagged with this eventID
            String getBookingsQuery = "SELECT bookingID, customerID, amountPaid FROM Booking WHERE eventID = ?";
            try (PreparedStatement pstmt = this.connection.prepareStatement(getBookingsQuery)) {
                pstmt.setInt(1, eventID);
                try (ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        int bookingID = rs.getInt("bookingID");
                        int customerID = rs.getInt("customerID");
                        double amountPaid = rs.getDouble("amountPaid");

                        // Step 2: For each booking, create a Refund record
                        String insertRefundQuery = "INSERT INTO Refund (bookingID, refundDate, refundStatus) VALUES (?, NOW(), 'Processed')";
                        try (PreparedStatement refundPstmt = this.connection.prepareStatement(insertRefundQuery)) {
                            refundPstmt.setInt(1, rs.getInt("bookingID"));
                            refundPstmt.executeUpdate();
                        }

                        // Step 3: Update the customer's account balance
                        String updateBalanceQuery = "UPDATE Customer SET accountBalance = accountBalance + ? WHERE userID = ?";
                        try (PreparedStatement balancePstmt = this.connection.prepareStatement(updateBalanceQuery)) {
                            balancePstmt.setDouble(1, amountPaid);
                            balancePstmt.setInt(2, customerID);
                            balancePstmt.executeUpdate();
                        }

                    }
                }
            }

            // Perform the revenue update outside of the loop and change isCancelled to true
            String updateEventRevenueQuery = "UPDATE Event SET revenue = revenue - (SELECT SUM(amountPaid) FROM Booking WHERE eventID = ?), isCancelled = 1 WHERE eventID = ?";
            try (PreparedStatement revenuePstmt = this.connection.prepareStatement(updateEventRevenueQuery)) {
                revenuePstmt.setInt(1, eventID);
                revenuePstmt.setInt(2, eventID);
                revenuePstmt.executeUpdate();
            }

            this.connection.commit(); // Commit the transaction
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (this.connection != null) {
                try {
                    this.connection.rollback(); // Rollback in case of any failure
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        }
    }

    // -----------------------------------------------------------
    // TICKET DATABASE

    // Create given number of tickets into database
    private List<Ticket> createTicket(int bookingID, boolean isGuest, int numOfTickets) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        String insertTicketQuery = "INSERT INTO Ticket (bookingID, isGuest, attended) VALUES (?, ?, 0)";

        try (PreparedStatement pstmt = this.connection.prepareStatement(insertTicketQuery, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < numOfTickets; i++) {
                pstmt.setInt(1, bookingID);
                pstmt.setBoolean(2, isGuest);
                pstmt.executeUpdate();
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int ticketID = generatedKeys.getInt(1);
                        tickets.add(new Ticket(ticketID, bookingID, isGuest, false));
                    }
                }
            }
        }
        return tickets;
    }


    public List<Ticket> getTickets(int userID) {
        List<Ticket> tickets = new ArrayList<>();
        String query = "SELECT t.* FROM Ticket t " +
                "INNER JOIN Booking b ON t.bookingID = b.bookingID " +
                "WHERE b.customerID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tickets.add(new Ticket(
                        rs.getInt("ticketID"),
                        rs.getInt("bookingID"),
                        rs.getInt("isGuest") == 1,
                        rs.getInt("attended") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public List<Ticket> getTicketsByEvent(int eventID) {
        List<Ticket> tickets = new ArrayList<>();
        String query = "SELECT t.* FROM Ticket t " +
                "INNER JOIN Booking b ON t.bookingID = b.bookingID " +
                "WHERE b.eventID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tickets.add(new Ticket(
                        rs.getInt("ticketID"),
                        rs.getInt("bookingID"),
                        rs.getInt("isGuest") == 1,
                        rs.getInt("attended") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public List<Ticket> getTicketsByBooking(int bookingID) {
        List<Ticket> tickets = new ArrayList<>();
        String query = "SELECT * FROM Ticket WHERE bookingID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, bookingID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                tickets.add(new Ticket(
                        rs.getInt("ticketID"),
                        bookingID,
                        rs.getInt("isGuest") == 1,
                        rs.getInt("attended") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get tickets by booking: " + e.getMessage());
        }
        return tickets;
    }

    public TicketOption getTicketOptionByEvent(int eventID, int ticketOptionID) throws SQLException {
        // SQL query to retrieve ticket option details for a specific event
        String query = "SELECT * FROM TicketOption WHERE eventID = ? AND ticketOptionID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            // Set parameters
            pstmt.setInt(1, eventID);
            pstmt.setInt(2, ticketOptionID);

            ResultSet rs = pstmt.executeQuery();

            // Check if ticket option exists
            if (rs.next()) {
                // Extract details from the ResultSet
                String optionName = rs.getString("optionName");
                double priceMultiplier = rs.getDouble("priceMultiplier");
                int totalAvailable = rs.getInt("totalAvailable");

                // Return a new TicketOption object with the retrieved data
                return new TicketOption(ticketOptionID, eventID, optionName, priceMultiplier, totalAvailable);
            } else {
                // Ticket option not found
                throw new SQLException("Ticket Option not found for eventID: " + eventID + " and ticketOptionID: " + ticketOptionID);
            }
        }
        // Note: Catching SQLException here. Depending on your error handling, you might want to rethrow or handle it differently.
    }

    public List<TicketOption> getTicketOptionsByEvent(int eventID){

        List<TicketOption> ticketOptions = new ArrayList<>();
        String query = "SELECT * FROM TicketOption WHERE eventID = ?";

        // Try-with-resources to ensure that resources are freed properly
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ticketOptions.add(new TicketOption(
                        rs.getInt("ticketOptionID"),
                        eventID,
                        rs.getString("optionName"),
                        rs.getFloat("priceMultiplier"),
                        rs.getInt("totalAvailable")
                ));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get ticket options by event: " + e.getMessage());
        }
        return ticketOptions;
    }

    public boolean verifyTicket(int ticketID) {
        String query = "SELECT t.ticketID, e.startTime FROM Ticket t " +
                "INNER JOIN Booking b ON t.bookingID = b.bookingID " +
                "INNER JOIN Event e ON b.eventID = e.eventID " +
                "WHERE t.ticketID = ? AND DATE(e.startTime) = CURDATE() AND t.attended = 0";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, ticketID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String updateQuery = "UPDATE Ticket SET attended = 1 WHERE ticketID = ?";
                try (PreparedStatement updatePstmt = this.connection.prepareStatement(updateQuery)) {
                    updatePstmt.setInt(1, ticketID);
                    int updatedRows = updatePstmt.executeUpdate();
                    return updatedRows > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // -----------------------------------------------------------
    // BOOKING DATABASE


    public Booking createBooking(int eventID, int ticketOptionID, int customerID, int ticketingOfficerID, int numOfTickets) throws SQLException {
        if (numOfTickets < 1 || numOfTickets > 5) {
            throw new IllegalArgumentException("Number of tickets must be between 1 and 5.");
        }

        try {
            this.connection.setAutoCommit(false); // Start transaction

            // Check current slots and calculate amount paid to add into event's revenue
            double amountPaid = calculateAmountPaidAndUpdateSlots(eventID, ticketOptionID, numOfTickets);
            if (amountPaid == -1) {
                return null; // Requirements not met or insufficient slots
            }
            Event event = getEvent(eventID); // Implement this method to fetch event details based on eventID
            TicketOption ticketOption = getTicketOptionByEvent(eventID, ticketOptionID);

            if (!updateCustomerBalance(customerID, amountPaid)) {
                throw new SQLException("Insufficient funds for the customer."); // Requirement 4
            }

            int bookingID = createBookingRecord(eventID, ticketOptionID, customerID, ticketingOfficerID, numOfTickets, amountPaid);

            // Handles ticket creation and links them to the booking (Requirement 3)
            List<Ticket> tickets = createTicket(bookingID, false, numOfTickets);

            PdfService pdfService = new PdfService();
            String basePath = System.getProperty("user.dir"); // Get the user working directory
            String pdfFilePath = basePath + "/image/ticket.pdf"; // Path to the PDF file
            pdfService.createEventDetailsPDF(pdfFilePath, event.getEventName(), event.getEventDesc(), event.getVenue(), event.getStartTime(), ticketOption.getOptionName(), amountPaid, tickets);

            EmailService emailService = new EmailService();
            String htmlContent = buildEmailContent(bookingID, tickets, amountPaid); // Implement this method based on your requirements
            String toEmail = AccountService.getCurrentUser().getEmail();
            String subject = "Booking Confirmation";
            // Assuming you have a method or service to send emails
            boolean emailSent = emailService.sendEmail(toEmail, subject, htmlContent, pdfFilePath, numOfTickets);

            if (!emailSent) {
                throw new RuntimeException("Failed to send booking confirmation email.");
            }

            // Commit transaction
            this.connection.commit();

            // Return booking details
            return new Booking(bookingID, eventID, customerID, ticketingOfficerID, ticketOptionID, amountPaid, tickets, LocalDateTime.now(), "Booked");
        } catch (SQLException e) {
            this.connection.rollback(); // Rollback in case of any error
        }
        return null;
    }

    // Check if event's current slot is more than number of tickets, add revenue with amount paid based on basePrice and priceMultiplier, and update current slots to minus off number of tickets
    private double calculateAmountPaidAndUpdateSlots(int eventID, int ticketOptionID, int numOfTickets) throws SQLException {
        double amountPaid = -1;
        String eventQuery = "SELECT basePrice, currSlots FROM Event WHERE eventID = ? FOR UPDATE";
        String ticketOptionQuery = "SELECT priceMultiplier, totalAvailable FROM TicketOption WHERE ticketOptionID = ?";

        try (PreparedStatement eventStmt = this.connection.prepareStatement(eventQuery);
             PreparedStatement ticketOptionStmt = this.connection.prepareStatement(ticketOptionQuery)) {

            this.connection.setAutoCommit(false);

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
                        try (PreparedStatement updateStmt = this.connection.prepareStatement(updateEventSlotsQuery)) {
                            updateStmt.setInt(1, numOfTickets);
                            updateStmt.setDouble(2, amountPaid);
                            updateStmt.setInt(3, eventID);
                            updateStmt.executeUpdate();
                        }
                    }
                }
            }
            this.connection.commit();
        }
        return amountPaid;
    }

    // Update account balance only if it's more than amount paid
    private boolean updateCustomerBalance(int customerID, double amountPaid) throws SQLException {
        String updateCustomerBalanceQuery = "UPDATE Customer SET accountBalance = accountBalance - ? WHERE userID = ? AND accountBalance >= ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(updateCustomerBalanceQuery)) {
            pstmt.setDouble(1, amountPaid);
            pstmt.setInt(2, customerID);
            pstmt.setDouble(3, amountPaid); // Ensure sufficient balance
            return pstmt.executeUpdate() > 0;
        }
    }

    private int createBookingRecord(int eventID, int ticketOptionID, int customerID, int ticketingOfficerID, int numOfTickets, double amountPaid) throws SQLException {
        String insertBookingQuery = "INSERT INTO Booking (eventID, ticketOptionID, customerID, ticketingOfficerID, numOfTickets, amountPaid, bookedTime, bookingStatus) VALUES (?, ?, ?, ?, ?, ?, NOW(), 'Booked')";

        try (PreparedStatement pstmt = this.connection.prepareStatement(insertBookingQuery, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, eventID);
            pstmt.setInt(2, ticketOptionID);
            pstmt.setInt(3, customerID);
            pstmt.setInt(4, ticketingOfficerID);
            pstmt.setInt(5, numOfTickets);
            pstmt.setDouble(6, amountPaid);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1;
    }

    private String buildEmailContent(int bookingID, List<Ticket> tickets, double amountPaid) {
        StringBuilder htmlContentBuilder = new StringBuilder();
        htmlContentBuilder.append("<h1>Booking Confirmation</h1>")
                .append("<p>Your booking has been successfully created. Booking ID: ").append(bookingID).append("</p>")
                .append("<p>Amount Paid: ").append(amountPaid).append("</p>")
                .append("<p>Tickets:</p>");
        for (Ticket ticket : tickets) {
            htmlContentBuilder.append("<li>Ticket ID: ").append(ticket.getTicketID())
                    .append(" - Guest: ").append(ticket.getIsGuest() ? "Yes" : "No").append("</li>");
        }
        htmlContentBuilder.append("<p>Please note that cancellations are not allowed within 48 hours before the event.</p>");
        // Add any additional content as needed
        return htmlContentBuilder.toString();
    }

    public List<Booking> getBookings(int userID) {
        String query = "SELECT * FROM bookings WHERE customerID = ?";
        List<Booking> bookings = new ArrayList<>();

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, userID);
            ResultSet bookingsResults = pstmt.executeQuery();

            while (bookingsResults.next()) {
                // Assuming you have methods to fetch the related objects based on ID
                List<Ticket> tickets = getTicketsByBooking(bookingsResults.getInt("bookingID"));

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
                String bookingStatus = bookingsResults.getString("bookingStatus");

                List<Ticket> tickets = getTicketsByBooking(bookingID);

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

        try {
            // Start transaction
            this.connection.setAutoCommit(false);

            // Step 1: Retrieve eventID and ticketOptionID from booking to get cancellation fee and number of tickets
            String bookingQuery = "SELECT eventID, ticketOptionID, numOfTickets FROM Booking WHERE bookingID = ? AND customerID = ?";
            int eventID, ticketOptionID, numOfTickets;
            double cancellationFee;
            try (PreparedStatement pstmtBooking = this.connection.prepareStatement(bookingQuery)) {
                pstmtBooking.setInt(1, bookingID);
                pstmtBooking.setInt(2, userID);
                ResultSet rsBooking = pstmtBooking.executeQuery();
                if (rsBooking.next()) {
                    eventID = rsBooking.getInt("eventID");
                    ticketOptionID = rsBooking.getInt("ticketOptionID");
                    numOfTickets = rsBooking.getInt("numOfTickets");
                } else {
                    this.connection.rollback();
                    return false; // Booking not found or does not belong to the user
                }
            }

            // Step 2: Retrieve cancellation fee from Event and calculate refund amount
            String eventQuery = "SELECT ticketCancellationFee FROM Event WHERE eventID = ?";
            try (PreparedStatement pstmtEvent = this.connection.prepareStatement(eventQuery)) {
                pstmtEvent.setInt(1, eventID);
                ResultSet rsEvent = pstmtEvent.executeQuery();
                if (rsEvent.next()) {
                    cancellationFee = rsEvent.getDouble("ticketCancellationFee");
                } else {
                    this.connection.rollback();
                    return false; // Event not found
                }
            }

            double refundAmount = cancellationFee * numOfTickets;

            // Step 3: Update customer's account balance
            String updateCustomerQuery = "UPDATE Customer SET accountBalance = accountBalance + ? WHERE userID = ?";
            try (PreparedStatement pstmtUpdateCustomer = this.connection.prepareStatement(updateCustomerQuery)) {
                pstmtUpdateCustomer.setDouble(1, refundAmount);
                pstmtUpdateCustomer.setInt(2, userID);
                pstmtUpdateCustomer.executeUpdate();
            }

            // Step 4: Add back current slots in the event
            String updateEventSlotsQuery = "UPDATE Event SET currSlots = currSlots + ? WHERE eventID = ?";
            try (PreparedStatement pstmtUpdateEventSlots = this.connection.prepareStatement(updateEventSlotsQuery)) {
                pstmtUpdateEventSlots.setInt(1, numOfTickets);
                pstmtUpdateEventSlots.setInt(2, eventID);
                pstmtUpdateEventSlots.executeUpdate();
            }

            // Step 5: Create Refund entry
            String insertRefundQuery = "INSERT INTO Refund (bookingID, refundDate, refundStatus) VALUES (?, NOW(), 'Processed')";
            try (PreparedStatement pstmtInsertRefund = this.connection.prepareStatement(insertRefundQuery)) {
                pstmtInsertRefund.setInt(1, bookingID);
                pstmtInsertRefund.executeUpdate();
            }

            // Step 6: Decrease event revenue
            String updateEventRevenueQuery = "UPDATE Event SET revenue = revenue - ? WHERE eventID = ?";
            try (PreparedStatement pstmtUpdateEventRevenue = this.connection.prepareStatement(updateEventRevenueQuery)) {
                pstmtUpdateEventRevenue.setDouble(1, refundAmount);
                pstmtUpdateEventRevenue.setInt(2, eventID);
                pstmtUpdateEventRevenue.executeUpdate();
            }

            // Step 7: Mark booking as cancelled
            String cancelBookingQuery = "UPDATE Booking SET bookingStatus = 'Cancelled' WHERE bookingID = ?";
            try (PreparedStatement pstmtCancelBooking = this.connection.prepareStatement(cancelBookingQuery)) {
                pstmtCancelBooking.setInt(1, bookingID);
                pstmtCancelBooking.executeUpdate();
            }

            // Commit transaction
            this.connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                // Rollback in case of any error
                this.connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------
    // REFUND DATABASE

    public List<Refund> getRefundsByUser (int userID) {
        List<Refund> refunds = new ArrayList<>();
        String query = "SELECT r.* FROM Refund r " +
                "JOIN Booking b ON r.bookingID = b.bookingID " +
                "WHERE b.customerID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                refunds.add(new Refund(
                        rs.getInt("refundID"),
                        rs.getInt("bookingID"),
                        rs.getTimestamp("refundDate").toLocalDateTime(),
                        rs.getString("refundStatus")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get refunds by user: " + e.getMessage());
        }
        return refunds;
    }

    public List<Refund> getRefundsByEvent (int eventID) {
        List<Refund> refunds = new ArrayList<>();
        String query = "SELECT r.* FROM Refund r " +
                "JOIN Booking b ON r.bookingID = b.bookingID " +
                "WHERE b.eventID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                refunds.add(new Refund(
                        rs.getInt("refundID"),
                        rs.getInt("bookingID"),
                        rs.getTimestamp("refundDate").toLocalDateTime(),
                        rs.getString("refundStatus")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get refunds by event: " + e.getMessage());
        }
        return refunds;
    }


    // -----------------------------------------------------------
    // STATISTICS DATABASE
    public Map<Integer, Integer> getTotalTicketsSold(List<Integer> eventIDs) throws SQLException {
        Map<Integer, Integer> ticketsSoldPerEvent = new HashMap<>();
        String query = "SELECT Booking.eventID, COUNT(*) AS ticketsSold FROM Booking JOIN Ticket ON Booking.bookingID = Ticket.bookingID WHERE Booking.eventID IN (" + String.join(",", Collections.nCopies(eventIDs.size(), "?")) + ") GROUP BY Booking.eventID";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            int index = 1;
            for (Integer eventId : eventIDs) {
                pstmt.setInt(index++, eventId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ticketsSoldPerEvent.put(rs.getInt("eventID"), rs.getInt("ticketsSold"));
            }
        }
        return ticketsSoldPerEvent;
    }

    public Map<Integer, Double> getRevenue(List<Integer> eventIDs) throws SQLException {
        Map<Integer, Double> revenuePerEvent = new HashMap<>();
        String query = "SELECT eventID, revenue FROM Event WHERE eventID IN (" + String.join(",", Collections.nCopies(eventIDs.size(), "?")) + ")";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            int index = 1;
            for (Integer eventId : eventIDs) {
                pstmt.setInt(index++, eventId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                revenuePerEvent.put(rs.getInt("eventID"), rs.getDouble("revenue"));
            }
        }
        return revenuePerEvent;
    }

    public Map<Integer, Double> getAttendanceRate(List<Integer> eventIDs) throws SQLException {
        Map<Integer, Double> attendanceRatePerEvent = new HashMap<>();
        String query = "SELECT Booking.eventID, SUM(CASE WHEN Ticket.attended = 1 THEN 1 ELSE 0 END) / COUNT(Ticket.ticketID) AS attendanceRate FROM Booking JOIN Ticket ON Booking.bookingID = Ticket.bookingID WHERE Booking.eventID IN (" + String.join(",", Collections.nCopies(eventIDs.size(), "?")) + ") GROUP BY Booking.eventID";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            int index = 1;
            for (Integer eventId : eventIDs) {
                pstmt.setInt(index++, eventId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                attendanceRatePerEvent.put(rs.getInt("eventID"), rs.getDouble("attendanceRate"));
            }
        }
        return attendanceRatePerEvent;
    }

    public Map<Integer, Map<String, Integer>> getTicketTypeBreakdown(List<Integer> eventIDs) throws SQLException {
        Map<Integer, Map<String, Integer>> breakdownPerEvent = new HashMap<>();
        String query = "SELECT Booking.eventID, TicketOption.optionName, COUNT(*) AS count FROM Booking JOIN Ticket ON Booking.bookingID = Ticket.bookingID JOIN TicketOption ON Booking.ticketOptionID = TicketOption.ticketOptionID WHERE Booking.eventID IN (" + String.join(",", Collections.nCopies(eventIDs.size(), "?")) + ") GROUP BY Booking.eventID, TicketOption.optionName";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            int index = 1;
            for (Integer eventId : eventIDs) {
                pstmt.setInt(index++, eventId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int eventId = rs.getInt("eventID");
                String optionName = rs.getString("optionName");
                int count = rs.getInt("count");

                breakdownPerEvent.computeIfAbsent(eventId, k -> new HashMap<>()).put(optionName, count);
            }
        }
        return breakdownPerEvent;
    }

}
