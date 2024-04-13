package org.example.ticketbookingapp;

import data.Event;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import service.AccountService;
import service.DatabaseService;
import service.EventService;
import user.Customer;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import service.DatabaseService;
import data.Booking;
import java.sql.SQLException;
import java.util.List;
import service.RefundService;

public class checkBookingController {

    private EventService eventService;
    private AccountService accountService;
    private DatabaseService databaseService; // Assuming you have an instance of DatabaseService

    @FXML
    private Label myBalance;
    @FXML
    private Button logoutButton;

    // Assuming this is inside your controller class and 'myGridPane' is a GridPane in your FXML file.
    @FXML
    private GridPane gridPane;

    @FXML
    private ListView<Booking> bookingListView;

    @FXML
    private ListView<Booking> refundListView;



    @FXML
    public void initialize() {
        try {
            if (databaseService == null) {
                databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            }
            this.eventService = new EventService(databaseService);
            this.accountService = new AccountService(databaseService);

            displayCustomerBalance();
            // Optionally, display user bookings when the view is initialized
            displayUserBookings();
            displayRefunds();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception
        }
    }


    // Method to fetch and display bookings for the logged-in user
    public void displayUserBookings() {
        int currentUserId = accountService.getCurrentUserID();
        List<Booking> userBookings = databaseService.getBookings(currentUserId);
        if (!userBookings.isEmpty()) {
            System.out.println("Updating list with new data: " + userBookings);
            updateListView(userBookings);
            displayRefunds();
        } else {
            System.out.println("No bookings found for user ID: " + currentUserId);
            bookingListView.getItems().clear(); // Ensure the list is cleared if there are no bookings
        }
    }


    public void displayRefunds() {
        int currentUserId = accountService.getCurrentUserID();
        List<Booking> cancelledBookings = databaseService.getCancelledBookings(currentUserId);
        if (!cancelledBookings.isEmpty()) {
            System.out.println("Updating list with new data: " + cancelledBookings);
            updateRefundListView(cancelledBookings);
        } else {
            System.out.println("No bookings found for user ID: " + currentUserId);
            refundListView.getItems().clear(); // Ensure the list is cleared if there are no bookings
        }
    }

    private void updateRefundListView(List<Booking> bookings) {
        // Clear the existing items before adding new items to the list
        refundListView.getItems().clear();
        refundListView.setItems(FXCollections.observableArrayList(bookings));
        refundListView.setCellFactory(bookingListView -> new ListCell<Booking>() {
            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Booking ID: " + booking.getBookingID() + ", Event ID: " + booking.getEventID() + ", Amount Paid: $" + booking.getAmountPaid() + ", Booking Time:" + booking.getBookingTime() + ", Status: " + booking.getBookingStatus());
                }
            }
        });
    }


    private void updateListView(List<Booking> bookings) {
        // Clear the existing items before adding new items to the list
        bookingListView.getItems().clear();
        bookingListView.setItems(FXCollections.observableArrayList(bookings));
        bookingListView.setCellFactory(bookingListView -> new ListCell<Booking>() {
            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Booking ID: " + booking.getBookingID() + ", Event ID: " + booking.getEventID() + ", Amount Paid: $" + booking.getAmountPaid());
                    Button refundButton = new Button("Refund");
                    refundButton.setOnAction(event -> handleRefundAction(booking));
                    setGraphic(refundButton);
                }
            }
        });
    }

    private void handleRefundAction(Booking booking) {
        // Alert confirmation dialog before proceeding
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to refund booking ID: " + booking.getBookingID() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            // First, check if the booking can be cancelled (must be more than 48 hours before the event)
            Event event = databaseService.getEvent(booking.getEventID());
            LocalDateTime eventStartTime = event.getStartTime();

            System.out.println(ChronoUnit.HOURS.between(LocalDateTime.now(), eventStartTime));
            long hoursBeforeEvent = ChronoUnit.HOURS.between(LocalDateTime.now(), eventStartTime);

            if (hoursBeforeEvent < 48) {
                // Inform user that the cancellation is not allowed as it's within 48 hours of the event start time
                new Alert(Alert.AlertType.ERROR, "Refund failed: Event starts within 48 hours and cannot be cancelled.").show();
                return; // Exit the method
            }

            // Proceed with the refund if the time condition is met
            boolean refundSuccessful = databaseService.cancelBooking(booking.getBookingID(), booking.getCustomerID());

            if (refundSuccessful) {
                Platform.runLater(() -> {
                    // UI update is run on the JavaFX Application Thread
                    displayUserBookings();
                    displayRefunds();
                    new Alert(Alert.AlertType.INFORMATION, "Refund successful for booking ID: " + booking.getBookingID()).show();
                });
            } else {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR, "Refund failed for booking ID: " + booking.getBookingID()).show();
                });
            }
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
}
