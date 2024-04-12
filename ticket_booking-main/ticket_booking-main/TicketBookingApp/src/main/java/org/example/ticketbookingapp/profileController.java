package org.example.ticketbookingapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import service.AccountService;
import service.DatabaseService;
import service.EventService;
import user.Customer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class profileController {

    private EventService eventService;
    private AccountService accountService;

    @FXML
    private Label myBalance;
    @FXML
    private Button logoutButton;

    // Assuming this is inside your controller class and 'myGridPane' is a GridPane in your FXML file.
    @FXML
    private GridPane gridPane;

    @FXML
    private TextField searchField; // This is the new TextField member for the search field

    @FXML
    public void initialize() {
        try {
            DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            this.eventService = new EventService(databaseService);
            this.accountService = new AccountService(databaseService);

            displayCustomerBalance();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception
        }
    }


    @FXML
    void handleLogoutAction(ActionEvent event) {
        System.out.println("Logging out...");
        accountService.logout();
        navigateToLogin(event);
    }

    private void navigateToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login.fxml"))); // Removed leading slash
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading the login view.");
            e.printStackTrace();
            // Optionally, show an alert to the user
        } catch (NullPointerException e) {
            System.err.println("Error: login.fxml not found.");
            e.printStackTrace();
            // Optionally, show an alert to the user
        }
    }

    @FXML
    private void handleConcertsLabelClick(MouseEvent event) {
        try {
            // Load MainMenu.fxml
            Parent mainMenu = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("MainMenu.fxml")));
            Scene scene = new Scene(mainMenu);

            // Get the current stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception, maybe show an error dialog
        }
    }

    @FXML
    public void displayCustomerBalance() {
        int currentUserId = accountService.getCurrentUserID();
        try {
            // Assuming 'databaseService' is already initialized and connected
            DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            Customer customer = databaseService.getCustomerDetails(currentUserId);
            if (customer != null) {
                myBalance.setText("Balance: $" + String.format("%.2f", customer.getBalance()));
            } else {
                myBalance.setText("Balance: N/A");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            myBalance.setText("Balance: Error");
            // Handle the error properly
        }
    }

    @FXML
    private void handleConcertsButtonClick(ActionEvent event) {
        try {
            // Load MainMenu.fxml
            Parent mainMenu = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("MainMenu.fxml")));
            Scene scene = new Scene(mainMenu);

            // Get the current stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception, maybe show an error dialog
        }
    }
}
