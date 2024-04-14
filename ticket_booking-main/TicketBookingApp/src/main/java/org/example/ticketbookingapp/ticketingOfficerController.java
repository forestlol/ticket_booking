package org.example.ticketbookingapp;

import data.Event;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import service.AccountService;
import service.DatabaseService;
import service.EventService;
import service.TicketService;
import user.Customer;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;


public class ticketingOfficerController {

    @FXML
    private TableView<Event> eventsTable;
    @FXML
    private TableColumn<Event, String> eventNameColumn;  // Assuming you are showing event names

    private EventService eventService;
    private AccountService accountService;

    private DatabaseService databaseService;

    @FXML
    private TextField ticketIDfield;

    // Assume that TicketService is initialized somewhere in the controller
    private TicketService ticketService;

    @FXML
    public void initialize() {
        try {
            DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            this.eventService = new EventService(databaseService);
            this.accountService = new AccountService(databaseService);
            this.ticketService = new TicketService(databaseService);
            loadEvents();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception (show an error dialog or log the error)
        }
    }

    @FXML
    private void handleVerifyAction(ActionEvent event) {
        try {
            // Retrieve the ticket ID entered by the user
            int ticketID = Integer.parseInt(ticketIDfield.getText().trim());

            // Get the logged-in TicketingOfficer's ID
            int officerID = AccountService.getCurrentUser().getID(); // ticketingOfficer should be your TicketingOfficer instance
            // Call the verifyTicket method from the TicketService
            boolean isVerified = ticketService.verifyTicket(ticketID, officerID);

            // Show the result in an alert dialog
            showAlert(isVerified ? "Ticket Verification" : "Verification Failed",
                    isVerified ? "The ticket with ID " + ticketID + " is valid and has been verified." :
                            "The ticket with ID " + ticketID + " is invalid or has already been used.");

            // Clear the field if verification is successful
            if (isVerified) {
                ticketIDfield.setText("");
            }
        } catch (NumberFormatException e) {
            // Show an error message if the input is not a valid integer
            showAlert("Invalid Input", "Please enter a valid ticket ID.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadEvents() {
        eventsTable.getItems().setAll(eventService.getAuthorisedEvents());
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
}