package service;

import user.User;

import java.util.List;
import java.util.Map;

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
    public boolean login(String email, String password) {

        User user = this.databaseService.authenticateUser(email, password);
        if(user != null)
        {
            this.currentUser = user;
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
        return this.databaseService.createUser(email, password, name, type, accountBalance);
    }

    // Add authorised officers [Event Manager]
    public Map<String, Boolean> addAuthorisedOfficer(int eventID, List<Integer> userIDs) {
        return this.databaseService.addAuthorisedOfficer(currentUser.getID(), eventID, userIDs);
    }
}
