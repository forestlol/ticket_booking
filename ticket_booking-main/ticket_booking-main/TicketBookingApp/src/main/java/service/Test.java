package service;

import user.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws SQLException {
        DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket-booking-system?serverTimezone=Asia/Singapore", "root", "");

        AccountService accountService = new AccountService(databaseService);

        // Assuming IDs 4 and 5 are for newly created Ticketing Officers, directly for demonstration
        List<Integer> newOfficerIDs = Arrays.asList(4, 5);

        // Test login with an existing user
        User loginSuccess = accountService.login("admin@gmail.com", "password");
        System.out.println("Login successful: " + loginSuccess);

        // Test creating a new user
        boolean createSuccess = accountService.createUser("officer02@gmail.com", "password", "T_Officer_1", "TicketingOfficer");
        System.out.println("New user 1 created: " + createSuccess);

        createSuccess = accountService.createUser("officer03@gmail.com", "password", "T_Officer_2", "TicketingOfficer");
        System.out.println("New user 2 created: " + createSuccess);

        Map<Integer, Boolean> result = accountService.addAuthorisedOfficer(1, newOfficerIDs);
        System.out.println("Authorized officers added: " + result);

        // Test logout
        accountService.logout();
        System.out.println("Logged out: " + (AccountService.getCurrentUser() == null));
    }
}