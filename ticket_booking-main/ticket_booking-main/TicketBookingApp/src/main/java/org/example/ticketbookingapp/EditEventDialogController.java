package org.example.ticketbookingapp;

import data.TicketOption;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import data.Event;
import service.AccountService;
import service.DatabaseService;
import service.EventService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditEventDialogController {

    private Event eventToEdit;

    private EventService eventService;
    private AccountService accountService;
    private DatabaseService databaseService; // Assuming you have an instance of DatabaseService

    private AdminController adminController;


    @FXML
    private TextField eventNameField;
    @FXML
    private TextField eventDescField;
    @FXML
    private TextField basePriceField;
    @FXML
    private TextField venueField;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField durationField;
    @FXML
    private TextField revenueField;
    @FXML
    private TextField currentSlotsField;
    @FXML
    private TextField totalSlotsField;
    @FXML
    private TextField cancellationFeeField;

    @FXML
    private VBox dynamicFormContainer;

    @FXML
    public void initialize() {
        try {
            if (databaseService == null) {
                databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            }
            this.eventService = new EventService(databaseService);
            this.accountService = new AccountService(databaseService);
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception
        }
    }
    public void setEventToEdit(Event event) {
        this.eventToEdit = event;

        eventNameField.setText(event.getEventName());
        eventDescField.setText(event.getEventDesc());
        basePriceField.setText(String.format("%.2f", event.getBasePrice())); // Assuming basePrice is a Double
        venueField.setText(event.getVenue());
        startTimeField.setText(event.getStartTime().toString()); // Assuming startTime is a LocalDateTime
        durationField.setText(String.valueOf(event.getDuration())); // Assuming duration is an Integer
        revenueField.setText(String.format("%.2f", event.getRevenue())); // Assuming revenue is a Double
        currentSlotsField.setText(String.valueOf(event.getCurrSlots())); // Assuming currSlots is an Integer
        totalSlotsField.setText(String.valueOf(event.getTotalSlots())); // Assuming totalSlots is an Integer
        cancellationFeeField.setText(String.format("%.2f", event.getTicketCancellationFee())); // Assuming ticketCancellationFee is a Double

        // Clear existing dynamic fields if any
        dynamicFormContainer.getChildren().clear();

        // Retrieve and display ticket options dynamically
        List<TicketOption> ticketOptions = databaseService.getTicketOptionsByEvent(event.getEventID());
        for (TicketOption option : ticketOptions) {
            Label label = new Label(option.getOptionName());
            label.setTextFill(Color.BLACK); // Set the text color to white
            label.setFont(new Font("System Bold", 18)); // Set the font style

            TextField textField = new TextField(String.valueOf(option.getTotalAvailable()));
            textField.setUserData(option.getTicketOptionID()); // Use ticket option ID as identifier

            dynamicFormContainer.getChildren().addAll(label, textField);
        }
    }


    // In your controller class where you have access to the text fields
    @FXML
    private void handleSaveAction(ActionEvent event) {
        Map<String, Object> updatedValues = new HashMap<>();
        updatedValues.put("eventName", eventNameField.getText());
        updatedValues.put("eventDesc", eventDescField.getText());
        updatedValues.put("basePrice", Float.parseFloat(basePriceField.getText()));
        updatedValues.put("venue", venueField.getText());
        updatedValues.put("duration", Integer.parseInt(durationField.getText()));
        updatedValues.put("revenue", Float.parseFloat(revenueField.getText()));
        updatedValues.put("currSlots", Integer.parseInt(currentSlotsField.getText()));
        updatedValues.put("totalSlots", Integer.parseInt(totalSlotsField.getText()));
        updatedValues.put("ticketCancellationFee", Float.parseFloat(cancellationFeeField.getText()));
        // Now handle the ticket options
        for (int i = 0; i < dynamicFormContainer.getChildren().size(); i += 2) { // Assuming each ticket option has two nodes: a Label and a TextField
            Label optionNameLabel = (Label) dynamicFormContainer.getChildren().get(i);
            TextField totalAvailableField = (TextField) dynamicFormContainer.getChildren().get(i + 1);

            String optionName = optionNameLabel.getText();
            int totalAvailable;
            try {
                totalAvailable = Integer.parseInt(totalAvailableField.getText());
            } catch (NumberFormatException e) {
                // Handle invalid format for totalAvailable
                System.out.println("Invalid format for total available slots.");
                return;
            }
            int ticketOptionID = (Integer) totalAvailableField.getUserData(); // Make sure you've set this correctly when creating the fields

            // Now update the ticket option in the database
            try {
                boolean success = databaseService.updateTicketOption(ticketOptionID, optionName, totalAvailable);
                if (!success) {
                    // Handle unsuccessful update
                    System.out.println("Failed to update ticket option with ID: " + ticketOptionID);
                }
            } catch (Exception e) {
                // Handle other exceptions
                e.printStackTrace();
                return;
            }
        }
        try {
            // Using ISO_LOCAL_DATE_TIME to parse the standard ISO format
            System.out.println(startTimeField.getText());
            LocalDateTime startTime = LocalDateTime.parse(startTimeField.getText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            updatedValues.put("startTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            eventService.updateEvent(eventToEdit.getEventID(), updatedValues);


            // Call refresh on AdminController
            if (adminController != null) {
                adminController.refreshEventsTable();
            }
            showAlert(Alert.AlertType.INFORMATION, "Success", "The event was updated successfully.");
            refreshEventsTable();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date-time format: " + e.getMessage());
            // Display error message to user
            return; // Exit the method if the date-time is invalid
        } catch (NumberFormatException ex) {
            System.out.println("Error parsing input to number: " + ex.getMessage());
            // Handle number format exception
        } catch (Exception ex) {
            System.out.println("Error updating event: " + ex.getMessage());
            // Handle other exceptions, possibly showing a user-friendly message
        }
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }


    private void refreshEventsTable() {
        //empty
    }


    @FXML
    private void handleBackAction(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }


}
