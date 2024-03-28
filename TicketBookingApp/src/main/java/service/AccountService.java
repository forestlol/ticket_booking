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
import java.util.Map;

public class AccountService {
    private DatabaseService databaseService;
    private static User currentUser = null;

    public AccountService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public static User getCurrentUser() {
        return this.currentUser;
    }

    // Login method
    public boolean login(String email, String password) {

        User user = databaseService.authenticateUser(email, password);
        if(user != null)
        {
            currentUser = user;
            return true;
        }
        return false;
    }

    // Logout method
    public void logout() {
        currentUser = null;
    }

    // Create user
    public User createUser(String email, String password, String name, String type, double accountBalance) {
        return databaseService.createUser(email, password, name, type, accountBalance);
    }

    // Add authorised officers [Event Manager]
    public Map<String, Boolean> addAuthorisedOfficer(int eventID, List<int> userIDs) {
        return databaseService.addAuthorisedOfficer(this.currentUser.getID(), eventID, userIDs);
    }
}
