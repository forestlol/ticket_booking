package service;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
        Map<Integer, Boolean> results = new HashMap<>();

        try {
            this.connection.setAutoCommit(false); // Begin transaction

            // Verify that the event manager is managing the given event
            if (!isEventManagerOfEvent(eventManagerID, eventID)) {
                throw new RuntimeException("Event manager " + eventManagerID + " does not manage event " + eventID);
            }

            for (int userID : userIDs) {
                if (isTicketingOfficer(userID)) { // Validate that the user is a ticketing officer
                    if (!isOfficerAlreadyAuthorized(eventID, userID)) {
                        String insertQuery = "INSERT INTO AuthorisedOfficers (eventID, ticketingOfficerID, timeStamp) VALUES (?, ?, NOW())";
                        try (PreparedStatement pstmt = this.connection.prepareStatement(insertQuery)) {
                            pstmt.setInt(1, eventID);
                            pstmt.setInt(2, userID);
                            pstmt.executeUpdate();
                            results.put(userID, true);
                        } catch (SQLException ex) {
                            System.out.println("Failed to add officer " + userID + " to event " + eventID + ": " + ex.getMessage());
                            results.put(userID, false);
                        }
                    } else {
                        System.out.println("Officer " + userID + " is already authorized for event " + eventID);
                        results.put(userID, false); // Officer already authorized
                    }
                } else {
                    results.put(userID, false); // Not a ticketing officer
                }
            }

            this.connection.commit(); // Commit the transaction if all operations were successful
        } catch (Exception ex) {
            this.connection.rollback(); // Roll back in case of any error during the transaction
            throw ex;
        } finally {
            this.connection.setAutoCommit(true); // Reset auto-commit to true
        }

        return results;
    }

    private boolean isOfficerAlreadyAuthorized(int eventID, int userID) throws SQLException {
        String query = "SELECT COUNT(*) FROM AuthorisedOfficers WHERE eventID = ? AND ticketingOfficerID = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            pstmt.setInt(2, userID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }


    private boolean isEventManagerOfEvent(int eventManagerID, int eventID) throws SQLException {
        String query = "SELECT COUNT(*) FROM Event WHERE eventID = ? AND eventManagerID = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            pstmt.setInt(2, eventManagerID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private boolean isTicketingOfficer(int userID) throws SQLException {
        String query = "SELECT COUNT(*) FROM User WHERE userID = ? AND type = 'TicketingOfficer'";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }


    // -----------------------------------------------------------
    // EVENT DATABASE

    private static final Map<Integer, List<String[]>> categoryOptions = new HashMap<>() {{
        put(1, Collections.singletonList(new String[]{"Standard", "1"}));
        put(2, Arrays.asList(new String[]{"Standard", "1"}, new String[]{"VIP", "1.5"}));
        put(3, Arrays.asList(new String[]{"Cat3", "1"}, new String[]{"Cat2", "1.25"}, new String[]{"Cat1", "1.5"}));
        put(4, Arrays.asList(new String[]{"Cat4", "1"}, new String[]{"Cat3", "1.25"}, new String[]{"Cat2", "1.5"}, new String[]{"Cat1", "1.75"}));
        put(5, Arrays.asList(new String[]{"Cat5", "1"}, new String[]{"Cat4", "1.25"}, new String[]{"Cat3", "1.5"}, new String[]{"Cat2", "1.75"}, new String[]{"Cat1", "2"}));
    }};
    // Inside DatabaseService.java

// Assuming the categoryOptions map is a class member as previously defined
// ...

    public Event createEvent(int eventManagerID, Map<String, Object> details) throws SQLException {
        // Define SQL query for inserting a new event
        String eventQuery = "INSERT INTO Event (eventManagerID, basePrice, eventName, eventDesc, venue, startTime, duration, revenue, currSlots, totalSlots, ticketCancellationFee, isCancelled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Define SQL query for inserting ticket options
        String ticketOptionQuery = "INSERT INTO TicketOption (eventID, optionName, priceMultiplier, totalAvailable) VALUES (?, ?, ?, ?)";
        Connection conn = this.getConnection();
        conn.setAutoCommit(false); // Start transaction

        try (PreparedStatement pstmt = conn.prepareStatement(eventQuery, Statement.RETURN_GENERATED_KEYS)) {
            // Prepare and execute event insertion
            prepareEventStatement(pstmt, eventManagerID, details);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating event failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int eventID = generatedKeys.getInt(1);
                    int numOfCategories = (int) details.get("numOfCategories");
                    List<Map<String, Object>> categoriesInput = (List<Map<String, Object>>) details.get("categories");
                    List<String[]> selectedCategories = categoryOptions.get(numOfCategories);

                    if (selectedCategories == null) {
                        throw new IllegalArgumentException("Category configuration not found for the number of categories: " + numOfCategories);
                    }

                    for (int i = 0; i < selectedCategories.size(); i++) {
                        String[] categoryConfig = selectedCategories.get(i);
                        Map<String, Object> categoryInput = categoriesInput.get(i);

                        String optionName = categoryConfig[0];
                        double priceMultiplier = Double.parseDouble(categoryConfig[1]);
                        int seats = (int) categoryInput.get("seats");

                        try (PreparedStatement pstmtOption = conn.prepareStatement(ticketOptionQuery)) {
                            pstmtOption.setInt(1, eventID);
                            pstmtOption.setString(2, optionName);
                            pstmtOption.setDouble(3, priceMultiplier);
                            pstmtOption.setInt(4, seats);
                            pstmtOption.executeUpdate();
                        }
                    }

                    conn.commit(); // Commit transaction if all operations are successful
                    // Assuming you have a method getEvent which retrieves the event based on eventID
                    return getEvent(eventID); // Fetch and return the new event
                } else {
                    throw new SQLException("Creating event failed, no ID obtained.");
                }
            }
        } catch (SQLException ex) {
            conn.rollback(); // Rollback in case of any failure
            throw ex; // Rethrow the exception to handle it further up the call stack
        } finally {
            conn.setAutoCommit(true); // Reset auto-commit to true
        }
    }


    private void prepareEventStatement(PreparedStatement pstmt, int eventManagerID, Map<String, Object> details) throws SQLException {
        pstmt.setInt(1, eventManagerID);
        pstmt.setDouble(2, Double.parseDouble(details.get("basePrice").toString()));
        pstmt.setString(3, (String) details.get("eventName"));
        pstmt.setString(4, (String) details.get("eventDesc"));
        pstmt.setString(5, (String) details.get("venue"));
        pstmt.setTimestamp(6, Timestamp.valueOf((String) details.get("date")));
        pstmt.setInt(7, (Integer) details.get("duration"));
        pstmt.setDouble(8, 0.0); // No revenue at the start
        pstmt.setInt(9, (Integer) details.get("totalSlots")); // Set current slots as the same as total slots
        pstmt.setInt(10, (Integer) details.get("totalSlots"));
        pstmt.setDouble(11, Double.parseDouble(details.get("cancellationFee").toString()));
        pstmt.setBoolean(12, false); // Event is not cancelled at the start
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
        String query = "SELECT * FROM Event WHERE isCancelled != 1";
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

    public boolean updateTicketOption(int ticketOptionID, String optionName, int totalAvailable) {
        String query = "UPDATE TicketOption SET optionName = ?, totalAvailable = ? WHERE ticketOptionID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setString(1, optionName);
            pstmt.setInt(2, totalAvailable);
            pstmt.setInt(3, ticketOptionID);

            int updatedRows = pstmt.executeUpdate();
            return updatedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public Event updateEvent(int eventID, int userID, Map<String, Object> details) {
        String query = "UPDATE Event SET basePrice = ?, eventName = ?, eventDesc = ?, venue = ?, startTime = ?, duration = ?, revenue = ?, currSlots = ?, totalSlots = ?, ticketCancellationFee = ? WHERE eventID = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            Number basePrice = (Number) details.get("basePrice");
            Number revenue = (Number) details.get("revenue");
            Number cancellationFee = (Number) details.get("ticketCancellationFee");

            pstmt.setDouble(1, basePrice != null ? basePrice.doubleValue() : 0.0);
            pstmt.setString(2, (String) details.get("eventName"));
            pstmt.setString(3, (String) details.get("eventDesc"));
            pstmt.setString(4, (String) details.get("venue"));

            // Properly assigning startTimeStr from the details map
            String startTimeStr = (String) details.get("startTime");
            if (startTimeStr == null || startTimeStr.isEmpty()) {
                throw new RuntimeException("Invalid or missing date format for 'startTime'");
            }

            try {
                // Attempt to parse assuming the input format is already correct
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime startTime = LocalDateTime.parse(startTimeStr.replace("T", " "), formatter);
                pstmt.setTimestamp(5, Timestamp.valueOf(startTime));
            } catch (DateTimeParseException e) {
                throw new RuntimeException("Invalid date format for 'startTime': " + startTimeStr);
            }

            pstmt.setInt(6, (Integer) details.get("duration"));
            pstmt.setDouble(7, revenue != null ? revenue.doubleValue() : 0.0);
            pstmt.setInt(8, (Integer) details.get("currSlots"));
            pstmt.setInt(9, (Integer) details.get("totalSlots"));
            pstmt.setDouble(10, cancellationFee != null ? cancellationFee.doubleValue() : 0.0);
            pstmt.setInt(11, eventID);

            int success = pstmt.executeUpdate();
            if (success > 0) {
                return getEvent(eventID);
            } else {
                throw new RuntimeException("Error updating event: no event was updated.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Database error: " + e.getMessage());
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
                            refundPstmt.setInt(1, bookingID);
                            refundPstmt.executeUpdate();
                        }

                        // Step 3: Update the customer's account balance
                        String updateBalanceQuery = "UPDATE Customer SET accountBalance = accountBalance + ? WHERE userID = ?";
                        try (PreparedStatement balancePstmt = this.connection.prepareStatement(updateBalanceQuery)) {
                            balancePstmt.setDouble(1, amountPaid);
                            balancePstmt.setInt(2, customerID);
                            balancePstmt.executeUpdate();
                        }

                        //Step 4: Update booking cancellation status
                        String updateBookingStatusQuery = "UPDATE Booking SET bookingStatus = 'Cancelled' WHERE eventID = ?";
                        try (PreparedStatement updateBookingPstmt = this.connection.prepareStatement(updateBookingStatusQuery)) {
                            updateBookingPstmt.setInt(1, eventID);
                            updateBookingPstmt.executeUpdate();
                        }
                    }
                }
            }

            // Perform the revenue update outside of the loop and change isCancelled to true
            String updateEventRevenueQuery = "UPDATE Event SET revenue = COALESCE(revenue, 0) - COALESCE((SELECT SUM(amountPaid) FROM Booking WHERE eventID = ?), 0), isCancelled = 1 WHERE eventID = ?";
            try (PreparedStatement revenuePstmt = this.connection.prepareStatement(updateEventRevenueQuery)) {
                revenuePstmt.setInt(1, eventID);
                revenuePstmt.setInt(2, eventID);
                revenuePstmt.executeUpdate();
            }

            this.connection.commit(); // Commit the transaction
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                this.connection.rollback(); // Rollback in case of any failure
            } catch (SQLException ex) {
                ex.printStackTrace();
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

    public boolean verifyTicket(int ticketID, int officerID) {
        // Queries
        try {
            this.connection.setAutoCommit(false);

            // Check if the ticket is available to be verified and not cancelled
            String ticketQuery = "SELECT t.bookingID FROM Ticket t " +
                    "INNER JOIN Booking b ON t.bookingID = b.bookingID " +
                    "INNER JOIN AuthorisedOfficers a ON b.eventID = a.eventID " +
                    "WHERE t.ticketID = ? AND t.attended = 0 AND b.bookingStatus != 'Cancelled' " +
                    "AND a.ticketingOfficerID = ? FOR UPDATE";
            int bookingID = 0;
            try (PreparedStatement pstmt = this.connection.prepareStatement(ticketQuery)) {
                pstmt.setInt(1, ticketID);
                pstmt.setInt(2, officerID);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    bookingID = rs.getInt("bookingID");
                } else {
                    this.connection.rollback(); // Ticket not found, or already attended, or booking is cancelled
                    return false;
                }
            }

            // Update Ticket to mark as attendedString updateTicketQuery = "UPDATE Ticket SET attended = 1 WHERE bookingID = ?";
            String updateTicketQuery = "UPDATE Ticket SET attended = 1 WHERE bookingID = ?";
            // Update all Tickets related to the bookingID to mark as attended
            try (PreparedStatement updatePstmt = this.connection.prepareStatement(updateTicketQuery)) {
                updatePstmt.setInt(1, bookingID);
                int updatedRows = updatePstmt.executeUpdate();
                if (updatedRows == 0) {
                    this.connection.rollback();
                    return false;
                }
            }

            // Update Booking to mark as verified
            String updateBookingQuery = "UPDATE Booking SET bookingStatus = 'Verified' WHERE bookingID = ?";
            try (PreparedStatement updatePstmt = this.connection.prepareStatement(updateBookingQuery)) {
                updatePstmt.setInt(1, bookingID);
                int updatedRows = updatePstmt.executeUpdate();
                if (updatedRows == 0) {
                    this.connection.rollback(); // No booking updated
                    return false;
                }
            }

            // Commit transaction if all updates are successful
            this.connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                this.connection.rollback(); // Rollback on exception
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (this.connection != null) {
                    this.connection.setAutoCommit(true); // Reset auto-commit
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // -----------------------------------------------------------
    // BOOKING DATABASE


    // Return price of ticket
    public double ReturnTicketPrice(int eventID, int ticketOptionID, int numOfTickets) {
        Connection conn = null;

        double amountPaid = 0;
        try {
            conn = this.connection;
            conn.setAutoCommit(false); // Begin transaction
            // Calculate amount paid and check if enough balance
            amountPaid = calculateAmountPaid(eventID, ticketOptionID, numOfTickets);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Transaction rollback failed: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(false);
                } catch (SQLException e) {
                    System.err.println("Couldn't reset auto-commit: " + e.getMessage());
                }
            }
        }
        return amountPaid;
    }
    // This map defines the names and multipliers for the categories based on the number selected


    public Booking createBooking(int eventID, int ticketOptionID, int customerID, int ticketingOfficerID, int numOfTickets) {
        System.out.println("1: " + numOfTickets);
        if (numOfTickets < 1 || numOfTickets > 5) {
            throw new IllegalArgumentException("Number of tickets must be between 1 and 5.");
        }

        Connection conn = null;

        try {
            conn = this.connection;
            conn.setAutoCommit(false); // Begin transaction

            // Calculate amount paid and update event slots and ticket option availability
            double amountPaid = calculateAmountPaidAndUpdateSlots(conn, eventID, ticketOptionID, numOfTickets);
            if (!updateCustomerBalance(customerID, amountPaid)) {
                conn.rollback();
                return null;
            }

            int bookingID = insertBookingRecord(eventID, ticketOptionID, customerID, ticketingOfficerID, numOfTickets, amountPaid);
            if (bookingID == -1) {
                conn.rollback();
                return null;
            }

            List<Ticket> tickets = createTicketsForBooking(bookingID, numOfTickets);
            this.connection.commit(); // Commit the transaction

            // Send Email Notification
            EmailService emailService = new EmailService();
            String basePath = System.getProperty("user.dir"); // System base path
            String pdfFilePath = basePath + "\\src\\image\\ticket.pdf";
            String qrCodeFilePath = basePath + "\\src\\image\\qrcode.png";
            // Constructing HTML content for the email
            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<h1>Booking Confirmation</h1>")
                    .append("<p>Your booking has been successfully created. Booking ID: ").append(bookingID).append("</p>")
                    .append("<p>Amount Paid: $").append(amountPaid).append("</p>")
                    .append("<p>Please find your ticket attached.</p>");

            boolean emailSent = emailService.sendEmail(
                    AccountService.getCurrentUser().getEmail(),
                    "Booking Confirmation",
                    htmlContent.toString(),
                    pdfFilePath,
                    qrCodeFilePath,
                    numOfTickets
            );

            if (!emailSent) {
                System.out.println("Email could not be sent.");
            } else {
                System.out.println("Booking confirmation email sent successfully.");
            }

            return new Booking(bookingID, eventID, customerID, ticketingOfficerID, ticketOptionID, amountPaid, tickets, LocalDateTime.now(), "Booked");
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Transaction rollback failed: " + ex.getMessage());
                }
            }
            System.err.println("Booking creation failed: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(false);
                } catch (SQLException e) {
                    System.err.println("Couldn't reset auto-commit: " + e.getMessage());
                }
            }
        }
    }

    double getCustomerBalance(int customerID) throws SQLException {
        String query = "SELECT accountBalance FROM Customer WHERE userID = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, customerID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("accountBalance");
                } else {
                    throw new SQLException("Customer not found.");
                }
            }
        }
    }

    private boolean checkAndUpdateSlots(int eventID, int ticketOptionID, int numOfTickets) throws SQLException {
        try {
            // Update the Event's current slots
            String updateEventQuery = "UPDATE Event SET currSlots = currSlots - ? WHERE eventID = ? AND currSlots >= ?";
            try (PreparedStatement pstmtEvent = this.connection.prepareStatement(updateEventQuery)) {
                pstmtEvent.setInt(1, numOfTickets);
                pstmtEvent.setInt(2, eventID);
                pstmtEvent.setInt(3, numOfTickets);
                if (pstmtEvent.executeUpdate() == 0) {
                    // Rollback if there are not enough slots
                    this.connection.rollback();
                    return false;
                }
            }

            // Update the TicketOption's total available tickets
            String updateTicketOptionQuery = "UPDATE TicketOption SET totalAvailable = totalAvailable - ? WHERE ticketOptionID = ? AND totalAvailable >= ?";
            try (PreparedStatement pstmtTicketOption = this.connection.prepareStatement(updateTicketOptionQuery)) {
                pstmtTicketOption.setInt(1, numOfTickets);
                pstmtTicketOption.setInt(2, ticketOptionID);
                pstmtTicketOption.setInt(3, numOfTickets);
                if (pstmtTicketOption.executeUpdate() == 0) {
                    // Rollback if there are not enough tickets available in the option
                    this.connection.rollback();
                    return false;
                }
            }

            // Commit the transaction if all updates are successful
            this.connection.commit();
            return true;
        } catch (SQLException e) {
            this.connection.rollback();
            throw e;
        } finally {
            // Reset the auto-commit to its default state
            this.connection.setAutoCommit(false);
        }
    }



    private double calculateAmountPaid(int eventID, int ticketOptionID, int numOfTickets) throws SQLException {
        // Fetch price and calculate amount
        String query = "SELECT basePrice, priceMultiplier FROM Event JOIN TicketOption ON Event.eventID = TicketOption.eventID WHERE Event.eventID = ? AND TicketOption.ticketOptionID = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            pstmt.setInt(2, ticketOptionID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double basePrice = rs.getDouble("basePrice");
                double priceMultiplier = rs.getDouble("priceMultiplier");
                return basePrice * priceMultiplier * numOfTickets;
            }
            throw new SQLException("Failed to fetch price information.");
        }
    }

    private int insertBookingRecord(int eventID, int ticketOptionID, int customerID, int ticketingOfficerID, int numOfTickets, double amountPaid) throws SQLException {
        String query = "INSERT INTO Booking (eventID, ticketOptionID, customerID, ticketingOfficerID, numOfTickets, amountPaid, bookedTime, bookingStatus) VALUES (?, ?, ?, ?, ?, ?, NOW(), 'Booked')";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, eventID);
            pstmt.setInt(2, ticketOptionID);
            pstmt.setInt(3, customerID);
            pstmt.setInt(4, ticketingOfficerID);
            pstmt.setInt(5, numOfTickets);
            pstmt.setDouble(6, amountPaid);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating booking record failed, no rows affected.");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating booking record failed, no ID obtained.");
                }
            }
        }
    }

    private List<Ticket> createTicketsForBooking(int bookingID, int numOfTickets) throws SQLException {
        // Implementation to create tickets
        List<Ticket> tickets = new ArrayList<>();
        String query = "INSERT INTO Ticket (bookingID, isGuest, attended) VALUES (?, ?, 0)";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < numOfTickets; i++) {
                pstmt.setInt(1, bookingID);
                pstmt.setBoolean(2, i != 0);  // Assuming not a guest ticket
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        tickets.add(new Ticket(rs.getInt(1), bookingID, i != 0, false));
                    }
                }
            }
            return tickets;
        }
    }


    // Check if event's current slot is more than number of tickets, add revenue with amount paid based on basePrice and priceMultiplier, and update current slots to minus off number of tickets
    private double calculateAmountPaidAndUpdateSlots(Connection conn, int eventID, int ticketOptionID, int numOfTickets) throws SQLException {
        System.out.println("2: " + numOfTickets);
        double amountPaid = -1;
        String eventQuery = "SELECT basePrice, currSlots FROM Event WHERE eventID = ? FOR UPDATE";
        String ticketOptionQuery = "SELECT priceMultiplier, totalAvailable FROM TicketOption WHERE ticketOptionID = ? FOR UPDATE";

        this.connection.setAutoCommit(false); // Start transaction to ensure all operations are atomic

        try (PreparedStatement eventStmt = this.connection.prepareStatement(eventQuery);
             PreparedStatement ticketOptionStmt = this.connection.prepareStatement(ticketOptionQuery)) {

            eventStmt.setInt(1, eventID);
            ResultSet eventRs = eventStmt.executeQuery();

            if (eventRs.next()) {
                double basePrice = eventRs.getDouble("basePrice");
                int currSlots = eventRs.getInt("currSlots");

                if (currSlots < numOfTickets) {
                    this.connection.rollback();
                    throw new SQLException("Not enough slots available.");
                }

                ticketOptionStmt.setInt(1, ticketOptionID);
                ResultSet ticketOptionRs = ticketOptionStmt.executeQuery();

                if (ticketOptionRs.next()) {
                    float priceMultiplier = ticketOptionRs.getFloat("priceMultiplier");
                    int totalAvailable = ticketOptionRs.getInt("totalAvailable");

                    if (totalAvailable < numOfTickets) {
                        this.connection.rollback();
                        throw new SQLException("Not enough tickets available in the selected option.");
                    }

                    amountPaid = basePrice * priceMultiplier * numOfTickets;

                    // Update currSlots and revenue in Event
                    String updateEventQuery = "UPDATE Event SET currSlots = currSlots - ?, revenue = revenue + ? WHERE eventID = ?";
                    try (PreparedStatement updateEventStmt = this.connection.prepareStatement(updateEventQuery)) {
                        updateEventStmt.setInt(1, numOfTickets);
                        updateEventStmt.setDouble(2, amountPaid);
                        updateEventStmt.setInt(3, eventID);
                        updateEventStmt.executeUpdate();
                    }

                    String updateTicketOptionQuery = "UPDATE TicketOption SET totalAvailable = totalAvailable - ? WHERE eventID = ? AND ticketOptionID = ? AND totalAvailable >= ?";
                    try (PreparedStatement updateTicketOptionStmt = conn.prepareStatement(updateTicketOptionQuery)) {
                        updateTicketOptionStmt.setInt(1, numOfTickets);
                        updateTicketOptionStmt.setInt(2, eventID);
                        updateTicketOptionStmt.setInt(3, ticketOptionID);
                        updateTicketOptionStmt.setInt(4, numOfTickets); // Ensures there are enough tickets available to sell
                        int rowsUpdated = updateTicketOptionStmt.executeUpdate();
                        if (rowsUpdated == 0) {
                            throw new SQLException("Failed to update ticket option availability, not enough tickets available.");
                        }
                    }
                }
            }
            this.connection.commit(); // Commit all changes if all operations were successful
        } catch (SQLException e) {
            this.connection.rollback(); // Rollback transaction if any operation fails
            throw e; // Re-throw the exception to handle it in the calling method
        } finally {
            this.connection.setAutoCommit(false); // Reset auto-commit to true
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

    // Method to get a database connection
    public Connection getConnection() throws SQLException {
        // Ensure the connection is opened before returning it
        connect(); // This will connect if the connection is null or closed
        return this.connection;
    }

    public Customer getCustomerDetails(int customerId) throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            connect();
        }

        String sql = "SELECT u.email, c.accountBalance, u.password, u.name, u.type FROM customer c JOIN user u ON c.userID = u.userID WHERE c.userID = ?";
        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String email = rs.getString("email");
                    double balance = rs.getDouble("accountBalance");
                    String password = rs.getString("password"); // Make sure this is the correct column name for the password
                    String name = rs.getString("name"); // Make sure this is the correct column name for the name
                    String type = rs.getString("type"); // Make sure this is the correct column name for the type
                    // Make sure the type matches "Customer" before creating a Customer object
                    if ("Customer".equals(type)) {
                        return new Customer(customerId, email, password, name, type, balance);
                    } else {
                        throw new SQLException("User is not a Customer");
                    }
                } else {
                    throw new SQLException("Customer with ID " + customerId + " not found.");
                }
            }
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

    public List<Booking> getCancelledBookings(int userID) {
        String query = "SELECT * FROM booking WHERE customerID = ? AND bookingStatus != 'Booked'";
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

    public List<Booking> getBookings(int userID) {
        String query = "SELECT * FROM booking WHERE customerID = ? AND bookingStatus = 'Booked'";
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

            // Step 1: Retrieve eventID, ticketOptionID, number of tickets, and event start time
            String bookingQuery = "SELECT b.eventID, ticketOptionID, numOfTickets, e.startTime FROM Booking b JOIN Event e ON b.eventID = e.eventID WHERE bookingID = ? AND customerID = ?";
            int eventID, ticketOptionID, numOfTickets;
            Timestamp eventStartTime;
            try (PreparedStatement pstmtBooking = this.connection.prepareStatement(bookingQuery)) {
                pstmtBooking.setInt(1, bookingID);
                pstmtBooking.setInt(2, userID);
                ResultSet rsBooking = pstmtBooking.executeQuery();
                if (rsBooking.next()) {
                    eventID = rsBooking.getInt("eventID");
                    ticketOptionID = rsBooking.getInt("ticketOptionID");
                    numOfTickets = rsBooking.getInt("numOfTickets");
                    eventStartTime = rsBooking.getTimestamp("startTime");

                    // Check if the event starts within the next 48 hours
                    long hoursBeforeEvent = ChronoUnit.HOURS.between(LocalDateTime.now(), eventStartTime.toLocalDateTime());
                    if (hoursBeforeEvent < 48) {
                        this.connection.rollback();
                        return false; // Event starts within 48 hours, cancellation not allowed
                    }
                } else {
                    this.connection.rollback();
                    return false; // Booking not found or does not belong to the user
                }
            }

            // Step 2: Retrieve cancellation fee and calculate refund amount
            String eventQuery = "SELECT e.ticketCancellationFee, e.basePrice, t.priceMultiplier FROM Event e JOIN TicketOption t ON e.eventID = t.eventID WHERE e.eventID = ? AND t.ticketOptionID = ?";
            double cancellationFee, basePrice, priceMultiplier, refundAmount;
            try (PreparedStatement pstmtEvent = this.connection.prepareStatement(eventQuery)) {
                pstmtEvent.setInt(1, eventID);
                pstmtEvent.setInt(2, ticketOptionID);
                ResultSet rsEvent = pstmtEvent.executeQuery();
                if (rsEvent.next()) {
                    cancellationFee = rsEvent.getDouble("ticketCancellationFee");
                    basePrice = rsEvent.getDouble("basePrice");
                    priceMultiplier = rsEvent.getDouble("priceMultiplier");
                    double ticketPrice = basePrice * priceMultiplier;
                    refundAmount = (ticketPrice - cancellationFee) * numOfTickets;
                } else {
                    this.connection.rollback();
                    return false; // Event or TicketOption not found
                }
            }

            // Step 3: Update customer's account balance
            String updateCustomerQuery = "UPDATE Customer SET accountBalance = accountBalance + ? WHERE userID = ?";
            try (PreparedStatement pstmtUpdateCustomer = this.connection.prepareStatement(updateCustomerQuery)) {
                pstmtUpdateCustomer.setDouble(1, refundAmount);
                pstmtUpdateCustomer.setInt(2, userID);
                pstmtUpdateCustomer.executeUpdate();
            }

            // Step 4: Update available slots and ticket options
            String updateEventSlotsQuery = "UPDATE Event SET currSlots = currSlots + ? WHERE eventID = ?";
            try (PreparedStatement pstmtUpdateEventSlots = this.connection.prepareStatement(updateEventSlotsQuery)) {
                pstmtUpdateEventSlots.setInt(1, numOfTickets);
                pstmtUpdateEventSlots.setInt(2, eventID);
                pstmtUpdateEventSlots.executeUpdate();
            }

            String updateTicketOptionSlotsQuery = "UPDATE TicketOption SET totalAvailable = totalAvailable + ? WHERE ticketOptionID = ?";
            try (PreparedStatement pstmtUpdateTicketOptionSlots = this.connection.prepareStatement(updateTicketOptionSlotsQuery)) {
                pstmtUpdateTicketOptionSlots.setInt(1, numOfTickets);
                pstmtUpdateTicketOptionSlots.setInt(2, ticketOptionID);
                pstmtUpdateTicketOptionSlots.executeUpdate();
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

    public List<TicketingOfficer> getAllTicketingOfficers() throws SQLException {
        List<TicketingOfficer> ticketingOfficers = new ArrayList<>();

        String query = "SELECT * FROM user WHERE type = 'TicketingOfficer'";
        try (PreparedStatement pstmt = this.connection.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int userID = rs.getInt("userID");
                String email = rs.getString("email");
                String password = rs.getString("password"); // Be cautious about password handling in a real system
                String name = rs.getString("name");
                String type = rs.getString("type");

                TicketingOfficer officer = new TicketingOfficer(userID, email, password, name, type);
                ticketingOfficers.add(officer);
            }
        }
        return ticketingOfficers;
    }

}
