package org.example.ticketbookingapp;

import javafx.event.ActionEvent;
import data.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import service.AccountService;
import service.DatabaseService;
import service.EventService;
import user.Customer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class MainMenuController {

    private EventService eventService;
    private AccountService accountService;

    @FXML
    private Button logoutButton;

    // Assuming this is inside your controller class and 'myGridPane' is a GridPane in your FXML file.
    @FXML
    private GridPane gridPane;

    @FXML
    private Label myBalance;
    @FXML
    private TextField searchField; // This is the new TextField member for the search field

    @FXML
    void handleSearchAction(ActionEvent event) {
        String searchQuery = searchField.getText().toLowerCase();
        List<Event> eventsToShow;

        if (searchQuery.isEmpty()) {
            // If the search field is empty, show all events
            eventsToShow = eventService.getAllEvents();
        } else {
            // Otherwise, filter the events based on the search query
            eventsToShow = eventService.getAllEvents().stream()
                    .filter(e -> e.getEventName().toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        populateEventGrid(eventsToShow); // Update the grid with the relevant list of events
    }

    public void populateEventGrid(List<Event> events) {
        gridPane.getChildren().clear();
        gridPane.getRowConstraints().clear();
        gridPane.getColumnConstraints().clear();

        int columns = 2; // We want 2 columns
        for (int i = 0; i < columns; i++) {
            ColumnConstraints columnConstraints = new ColumnConstraints();
            columnConstraints.setPercentWidth(50); // Each column takes up 50% of the width
            gridPane.getColumnConstraints().add(columnConstraints);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            VBox eventBox = new VBox(5); // 5 is the spacing between elements
            eventBox.setPadding(new Insets(10)); // Padding around the VBox
            eventBox.setStyle("-fx-border-color: lightgrey; -fx-border-width: 1; -fx-background-color: white;");

            Label eventNameLabel = new Label(event.getEventName());
            eventNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            eventNameLabel.setWrapText(true);

            Label eventDescLabel = new Label(event.getEventDesc());
            eventDescLabel.setFont(Font.font("Arial", FontPosture.ITALIC, 12));
            eventDescLabel.setWrapText(true);

            Label venueLabel = new Label("Venue: " + event.getVenue());
            venueLabel.setWrapText(true);

            Label startTimeLabel = new Label("Event Date and Time: " + event.getStartTime().format(formatter));
            startTimeLabel.setWrapText(true);

            Label durationLabel = new Label("Duration: " + event.getDuration() + " minutes");
            durationLabel.setWrapText(true);

            Button detailsButton = new Button("View Details");
            detailsButton.setOnAction(e -> showEventDetails(event));
            detailsButton.setMaxWidth(Double.MAX_VALUE); // Make the button grow to VBox width

            // Add all the labels and the button to the VBox
            eventBox.getChildren().addAll(eventNameLabel, eventDescLabel, venueLabel, startTimeLabel, durationLabel, detailsButton);

            // Calculate row and column for placement in the grid
            int columnIndex = i % columns; // Determines the column index
            int rowIndex = i / columns; // Determines the row index

            // Add the event container to the GridPane
            gridPane.add(eventBox, columnIndex, rowIndex);
            GridPane.setMargin(eventBox, new Insets(10)); // Margin around the VBox for spacing
            GridPane.setVgrow(eventBox, Priority.ALWAYS); // Allow VBox to grow vertically
            GridPane.setHgrow(eventBox, Priority.ALWAYS); // Allow VBox to grow horizontally
        }

        // If the number of events is odd, add an empty VBox to balance the last row
        if (events.size() % columns != 0) {
            gridPane.add(new VBox(), 1, events.size() / columns);
        }
    }

    @FXML
    private void handleCheckBooking(MouseEvent event) {
        try {
            // Load MainMenu.fxml
            Parent checkBooking = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("checkBooking.fxml")));
            Scene scene = new Scene(checkBooking);

            // Get the current stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception, maybe show an error dialog
        }
    }

    private void showEventDetails(Event event) {
        try {
            // Load the new FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ConcertDetail.fxml"));
            Parent detailView = loader.load();

            // Set the event data to the detail view controller if needed
            ConcertDetailController detailController = loader.getController();
            detailController.setEventData(event);

            // Get the current stage from any control like the logout button
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();

            // Set the new scene on the existing stage
            currentStage.setScene(new Scene(detailView));

            // Optionally, if you want to keep the stage title or other properties
            // currentStage.setTitle("Event Details");

        } catch (IOException ex) {
            ex.printStackTrace();
            // handle exception
        }
    }


    private Event eventData;

    // Call this method when setting the event data from the MainMenuController
    public void setEventData(Event event) {
        this.eventData = event;
        updateUI();
    }

    // Update the UI elements with event data
    private void updateUI() {
        // Set event details to the UI components
    }

    public MainMenuController() {
        // Initialize your DatabaseService here, but it's better to use a method annotated with @FXML
    }

    @FXML
    public void initialize() {
        try {
            DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            this.eventService = new EventService(databaseService);
            this.accountService = new AccountService(databaseService);
            displayCustomerBalance();

            List<Event> events = eventService.getAllEvents();
            populateEventGrid(events); // Ensure this is called after the FXML has loaded.

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                List<Event> eventsToShow;
                if (newValue == null || newValue.trim().isEmpty()) {
                    eventsToShow = eventService.getAllEvents();
                } else {
                    String searchQuery = newValue.toLowerCase();
                    eventsToShow = eventService.getAllEvents().stream()
                            .filter(e -> e.getEventName().toLowerCase().contains(searchQuery))
                            .collect(Collectors.toList());
                }
                populateEventGrid(eventsToShow); // Update the grid with the relevant list of events
            });

        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception
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
