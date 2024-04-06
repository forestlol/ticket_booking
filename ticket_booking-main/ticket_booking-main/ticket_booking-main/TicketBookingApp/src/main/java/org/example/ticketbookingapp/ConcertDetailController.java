package org.example.ticketbookingapp;

import data.Event;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import service.AccountService;
import service.DatabaseService;
import service.EventService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javafx.scene.input.MouseEvent;


public class ConcertDetailController {

    private EventService eventService;
    private AccountService accountService;

    private Event event;

    // Define FXML labels
    @FXML
    private Label labelEventName;
    @FXML
    private Label labelEventDesc;
    @FXML
    private Label labelVenue;
    @FXML
    private Label labelStartTime;
    @FXML
    private Label labelDuration;
    @FXML
    private Label labelRevenue;
    @FXML
    private Label labelCurrSlots;
    @FXML
    private Label labelTotalSlots;
    @FXML
    private Label labelCancellationFee;

    @FXML
    private Button logoutButton;

    @FXML
    private Button purchaseTicketButton;

    @FXML
    private void handlePurchaseTicketAction(ActionEvent event) {
        try {
            // Correct the path if 'CheckoutPage.fxml' is in the same package as the current class.
            // If it's in the root of the resources folder, add a leading slash like "/CheckoutPage.fxml".
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CheckoutPage.fxml"));
            Parent checkoutPage = loader.load();

            // Get the CheckoutPageController from the loader and pass the event data to it.
            CheckOutPageController checkoutController = loader.getController();
            checkoutController.setEventData(this.event);  // Ensure this method exists in your CheckOutPageController

            // Set up the scene with the checkout page.
            Scene scene = new Scene(checkoutPage);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception, for example, show an error dialog.
        }
    }


    public ConcertDetailController() {
        // Initialize your DatabaseService here, but it's better to use a method annotated with @FXML
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
    public void initialize() {
        try {
            DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            this.eventService = new EventService(databaseService);
            this.accountService = new AccountService(databaseService); // Add this line

            //populateEventList();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception (show an error dialog or log the error)
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

    public void setEventData(Event event) {
        this.event = event;
        updateEventDetailsUI();
    }

    private void updateEventDetailsUI() {
        if (event != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");

            labelEventName.setText(event.getEventName());
            labelEventDesc.setText(event.getEventDesc());
            labelStartTime.setText(event.getStartTime().format(formatter) + " / " + event.getVenue());
            labelDuration.setText("Duration: " + event.getDuration() + " minutes");
            labelRevenue.setText("Starting Price: $" + String.format("%.2f", event.getBasePrice()));
            labelCurrSlots.setText("Current slots available: " + String.valueOf(event.getCurrSlots()));
            labelTotalSlots.setText("Total slots: " + String.valueOf(event.getTotalSlots()));
            labelCancellationFee.setText("Cancellation fee: " + "$" + String.format("%.2f", event.getTicketCancellationFee()));
        }
    }
}