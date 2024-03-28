package service;

import user.Customer;
import user.EventManager;
import user.TicketingOfficer;
import user.User;

import data.Event;

import com.myapp.util.DatabaseUtil;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class AccountService {
    private DatabaseService databaseService;
    private static User currentUser = null;

    public AccountService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    // Login method
    public User login(String email, String password) {

//        return databaseService.authenticateUser(email, password);

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
                // Assume additional fields are fetched here as needed

                switch (type)
                {
                    case "Customer":
                        double accountBalance = 0;
                        String accountBalanceQuery = "SELECT accountBalance FROM Customer WHERE userID = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(accountBalanceQuery)) {
                            pstmt.setInt(1, userID);
                            ResultSet rsBalance = pstmt.executeQuery();
                            if (rsBalance.next()) {
                                accountBalance = rsBalance.getDouble("accountBalance");
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        currentUser = new Customer(userID, email, password, name, type, accountBalance);
                        return currentUser;
                    case "EventManager":
                        List<Event> managedEvents = fetchEventsManagedByEventManager(conn, userID);
                        currentUser = new EventManager(userID, email, password, name, type, managedEvents);
                        return currentUser;

                    case "TicketOfficer":
                        List<Event> ticketingEvents = fetchEventsForTicketingOfficer(conn, userID);
                        currentUser = new TicketingOfficer(userID, email, password, name, type, ticketingEvents);
                        return currentUser;
                    default:
                        return null; // or throw an exception
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;

    }


    private List<Event> fetchEventsManagedByEventManager(Connection conn, int managerID) {
        List<Event> events = new ArrayList<>();
        String query = "SELECT * FROM Event WHERE managerID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, managerID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int eventID = rs.getInt("eventID");
                List<Integer> ticketOfficerIDs = fetchTicketOfficerIDsForEvent(conn, eventID);
                List<Integer> ticketOptionIDs = fetchTicketOptionIDsForEvent(conn, eventID);
                // Fetch other Event details and create Event objects
                Event event = new Event(
                        eventID,
                        managerID,
                        ticketOfficerIDs,
                        ticketOptionIDs,
                        rs.getDouble("ticketCancellationFee"),
                        rs.getDouble("basePrice"),
                        rs.getString("eventName"),
                        rs.getString("venue"),
                        rs.getTimestamp("startTime").toLocalDateTime(),
                        rs.getInt("duration"),
                        rs.getTimestamp("endTime").toLocalDateTime(),
                        new ArrayList<>(), // Assuming tickets are fetched separately or initialized elsewhere
                        rs.getInt("numTicketsAvailable")
                );
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }
    private List<Event> fetchEventsForTicketingOfficer(Connection conn, int ticketOfficerID) {
        List<Event> events = new ArrayList<>();
        String query = "SELECT e.* FROM Event e JOIN AuthorisedOfficers ao ON e.eventID = ao.eventID WHERE ao.ticketOfficerID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, ticketOfficerID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int eventID = rs.getInt("eventID");
                int managerID = rs.getInt("managerID"); // Assuming managerID is directly in Event table
                List<Integer> ticketOfficerIDs = Arrays.asList(ticketOfficerID); // Since this event is associated with this ticket officer
                List<Integer> ticketOptionIDs = fetchTicketOptionIDsForEvent(conn, eventID);
                // Fetch other Event details and create Event objects
                Event event = new Event(
                        eventID,
                        managerID,
                        ticketOfficerIDs,
                        ticketOptionIDs,
                        rs.getDouble("ticketCancellationFee"),
                        rs.getDouble("basePrice"),
                        rs.getString("eventName"),
                        rs.getString("venue"),
                        rs.getTimestamp("startTime").toLocalDateTime(),
                        rs.getInt("duration"),
                        rs.getTimestamp("endTime").toLocalDateTime(),
                        new ArrayList<>(), // Assuming tickets are fetched separately or initialized elsewhere
                        rs.getInt("numTicketsAvailable")
                );
                events.add(event);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    private List<Integer> fetchTicketOfficerIDsForEvent(Connection conn, int eventID) throws SQLException {
        List<Integer> ticketOfficerIDs = new ArrayList<>();
        String query = "SELECT ticketOfficerID FROM AuthorisedOfficers WHERE eventID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ticketOfficerIDs.add(rs.getInt("ticketOfficerID"));
                }
            }
        }
        return ticketOfficerIDs;
    }

    private List<Integer> fetchTicketOptionIDsForEvent(Connection conn, int eventID) throws SQLException {
        List<Integer> ticketOptionIDs = new ArrayList<>();
        String query = "SELECT ticketOptionID FROM TicketOption WHERE eventID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, eventID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ticketOptionIDs.add(rs.getInt("ticketOptionID"));
                }
            }
        }
        return ticketOptionIDs;
    }

    // Logout method
    public void logout(int userID) {
        currentUser = null;
    }

    // Create Customer method
    public Customer createCustomer(String email, String password, String name, String type, double accountBalance) {
//        Customer customer = new Customer(email, name, password); // Create a new customer
//        databaseService.saveUser(customer); // Save the customer to the database

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            String insertUserSQL = "INSERT INTO User (email, password, name, type) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtUser.setString(1, email);
                pstmtUser.setString(2, password);
                pstmtUser.setString(3, name);
                pstmtUser.setString(4, type);
                pstmtUser.executeUpdate();

                try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);

                        String insertCustomerSQL = "INSERT INTO Customer (userID, accountBalance) VALUES (?, ?)";
                        try (PreparedStatement pstmtCustomer = conn.prepareStatement(insertCustomerSQL)) {
                            pstmtCustomer.setInt(1, userId);
                            pstmtCustomer.setDouble(2, accountBalance);
                            pstmtCustomer.executeUpdate();
                        }
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            conn.commit();
            return new Customer(id, password, email, name, type, accountBalance);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
//        return customer;
    }

    // Create Event Manager method
    public EventManager createEventManager(String email, String password, String name, String type) {
//        EventManager eventManager = new EventManager(email, name, password); // Create a new event manager
//        databaseService.saveUser(eventManager); // Save the event manager to the database

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            int userId = 0;
            String insertUserSQL = "INSERT INTO User (email, password, name, type) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtUser.setString(1, email);
                pstmtUser.setString(2, password);
                pstmtUser.setString(3, name);
                pstmtUser.setString(4, type);
                pstmtUser.executeUpdate();

                try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            String insertManagerSQL = "INSERT INTO EventManager (userID) VALUES (?)";
            try (PreparedStatement pstmtManager = conn.prepareStatement(insertManagerSQL)) {
                pstmtManager.setInt(1, userId);
                pstmtManager.executeUpdate();
            }

            conn.commit();
            return new EventManager(userId, password, email, name, type);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
//        return eventManager;
    }

    // Create Ticket Officer method
    public TicketingOfficer createTicketOfficer(String email, String password, String name, String type) {
//        TicketOfficer ticketOfficer = new TicketOfficer(email, name, password); // Create a new ticket officer
//        databaseService.saveUser(ticketOfficer); // Save the ticket officer to the database

        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            int userId = 0;
            String insertUserSQL = "INSERT INTO User (email, password, name, type) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmtUser = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtUser.setString(1, email);
                pstmtUser.setString(2, password);
                pstmtUser.setString(3, name);
                pstmtUser.setString(4, type);
                pstmtUser.executeUpdate();

                try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            String insertOfficerSQL = "INSERT INTO TicketOfficer (userID) VALUES (?)";
            try (PreparedStatement pstmtOfficer = conn.prepareStatement(insertOfficerSQL)) {
                pstmtOfficer.setInt(1, userId);
                pstmtOfficer.executeUpdate();
            }

            conn.commit();
            return new TicketingOfficer(id, password, email, name, type);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
//        return ticketOfficer;
    }
}
