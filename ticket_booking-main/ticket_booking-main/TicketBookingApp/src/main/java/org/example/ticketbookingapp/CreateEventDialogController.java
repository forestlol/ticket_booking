package org.example.ticketbookingapp;

import data.Event;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.AccountService;
import service.DatabaseService;
import service.EventService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;




public class CreateEventDialogController {


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
    private DatePicker dateField;
    @FXML
    private TextField durationField;
    @FXML
    private TextField cancellationFeeField;
    @FXML
    private ComboBox<Integer> NumOfCatField;
    @FXML
    private VBox categoryInputsContainer;
    @FXML
    private ComboBox<String> timeField;

    private List<TextField> categorySeatsFields = new ArrayList<>();

    @FXML
    private void resetFormFields() {
        // Clear all text fields
        eventNameField.clear();
        eventDescField.clear();
        basePriceField.clear();
        venueField.clear();
        durationField.clear();
        cancellationFeeField.clear();

        // Reset the date picker
        dateField.setValue(null);

        // Reset ComboBoxes
        NumOfCatField.getSelectionModel().clearSelection();
        timeField.getSelectionModel().clearSelection();

        // Clear dynamic category inputs
        categoryInputsContainer.getChildren().clear();

        // Optionally, you might want to reset the state of NumOfCatField and timeField to a default value
        NumOfCatField.setValue(null); // Set to null or a default value
        timeField.setValue(null); // Set to null or a default time

        // You may also want to clear any associated list if applicable
        categorySeatsFields.clear();  // Assuming you have a list to track dynamically added fields
    }



    private LocalDateTime getDateTime() {
        LocalDate date = dateField.getValue();
        String time = timeField.getSelectionModel().getSelectedItem();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime localTime = LocalTime.parse(time, formatter);
        return LocalDateTime.of(date, localTime);
    }

    @FXML
    public void initialize() {
        try {
            if (databaseService == null) {
                databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            }
            this.eventService = new EventService(databaseService);
            this.accountService = new AccountService(databaseService);

            loadNumOfCatField();
            loadTimeField();
            setupCategoryFieldListener();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exception
        }
    }

    private void loadNumOfCatField() {
        NumOfCatField.getItems().clear(); // Clear existing items, if any
        for (int i = 1; i <= 5; i++) {
            NumOfCatField.getItems().add(i);
        }
    }

    private void setupCategoryFieldListener() {
        NumOfCatField.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateCategoryInputs(newVal);
            } else {
                // newVal is null, so clear the category inputs or handle it appropriately
                categoryInputsContainer.getChildren().clear();
                categorySeatsFields.clear();
            }
        });

    }

    private void updateCategoryInputs(int numberOfCategories) {
        categoryInputsContainer.getChildren().clear(); // Clear previous inputs
        categorySeatsFields.clear(); // Clear the old text fields list

        for (int i = 1; i <= numberOfCategories; i++) {
            Label label = new Label("Category " + i + ":");
            TextField textField = new TextField();
            textField.setPromptText("Enter seats for Category " + i);

            categorySeatsFields.add(textField); // Add new text field to the list
            categoryInputsContainer.getChildren().addAll(label, textField);
        }
    }

    private void loadTimeField() {
        timeField.getItems().clear(); // Clear existing items, if any
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        LocalTime time = LocalTime.MIDNIGHT;
        final LocalTime endTime = LocalTime.of(23, 45, 00);
        while (!time.equals(endTime)) {
            String formattedTime = time.format(timeFormatter);
            timeField.getItems().add(formattedTime);
            // For debugging: print each time added

            time = time.plusMinutes(15);
        }
        // Add the final time if not already included
        if (!timeField.getItems().contains(endTime.format(timeFormatter))) {
            timeField.getItems().add(endTime.format(timeFormatter));
        }
    }

    private boolean validateInput() {
        boolean isValid = true;
        StringBuilder errorMessage = new StringBuilder();

        if (eventNameField.getText().isEmpty()) {
            errorMessage.append("Event name is required.\n");
            isValid = false;
        }
        if (eventDescField.getText().isEmpty()) {
            errorMessage.append("Event description is required.\n");
            isValid = false;
        }
        try {
            double basePrice = Double.parseDouble(basePriceField.getText());
            if (basePrice < 0) {
                errorMessage.append("Base price must be a positive number.\n");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            errorMessage.append("Invalid format for base price.\n");
            isValid = false;
        }

        if (venueField.getText().isEmpty()) {
            errorMessage.append("Venue is required.\n");
            isValid = false;
        }
        if (dateField.getValue() == null) {
            errorMessage.append("Event date is required.\n");
            isValid = false;
        }
        try {
            int duration = Integer.parseInt(durationField.getText());
            if (duration <= 0) {
                errorMessage.append("Duration must be a positive integer.\n");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            errorMessage.append("Invalid format for duration.\n");
            isValid = false;
        }

        try {
            double cancellationFee = Double.parseDouble(cancellationFeeField.getText());
            if (cancellationFee < 0) {
                errorMessage.append("Cancellation fee must be a positive number.\n");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            errorMessage.append("Invalid format for cancellation fee.\n");
            isValid = false;
        }

        if (!isValid) {
            showAlert("Validation Error", errorMessage.toString(), Alert.AlertType.INFORMATION);
        }

        return isValid;
    }

    private void showAlert(String title, String header, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(header);
        alert.showAndWait();
    }


    @FXML
    private void handleCreateEventAction(ActionEvent event) throws SQLException {
        if (!validateInput()) {
            return; // Stop the event creation if validation fails
        }

        Map<String, Object> details = new HashMap<>();
        // Collect other event details
        details.put("eventName", eventNameField.getText());
        details.put("eventDesc", eventDescField.getText());
        details.put("basePrice", Double.parseDouble(basePriceField.getText()));
        details.put("venue", venueField.getText());
        details.put("date", getCombinedDateTime());
        details.put("duration", Integer.parseInt(durationField.getText()));
        details.put("revenue", Double.parseDouble("0"));
        // details.put("totalSlots", Integer.parseInt(totalSlotsField.getText()));
        details.put("cancellationFee", Double.parseDouble(cancellationFeeField.getText()));

        // Collect category details including the number of seats
        int totalSeats = 0;
        List<Map<String, Object>> categoryDetails = new ArrayList<>();
        for (TextField categorySeatsField : categorySeatsFields) {
            int seats = Integer.parseInt(categorySeatsField.getText());
            totalSeats += seats; // Sum up all the seats
            Map<String, Object> category = new HashMap<>();
            category.put("seats", Integer.parseInt(categorySeatsField.getText()));
            categoryDetails.add(category);
        }
        details.put("categories", categoryDetails);
        details.put("numOfCategories", NumOfCatField.getValue());

        details.put("totalSlots", totalSeats);
        details.put("currSlots", totalSeats); // Assuming the event starts with full capacity

        // Now pass this details map to the EventService
        Event createdEvent = eventService.createEvent(details);
        if (createdEvent != null) {
            showAlert("Success", "Event created successfully: " + createdEvent.getEventName(), Alert.AlertType.INFORMATION);
            resetFormFields();
        } else {
            showAlert("Error", "Failed to create event.", Alert.AlertType.ERROR);
        }
    }

    private String getCombinedDateTime() {
        LocalDate datePart = dateField.getValue();
        String timePart = timeField.getSelectionModel().getSelectedItem();

        // Ensure the time part is not null or empty to avoid parsing errors
        if (timePart == null || timePart.isEmpty()) {
            // Handle this case appropriately, perhaps by using a default time or alerting the user
            timePart = "00:00:00";
        }

        // Combine the date and time into a single LocalDateTime object
        LocalDateTime dateTime = LocalDateTime.of(datePart, LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm:ss")));

        // Format it into a string that matches the Timestamp format
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    @FXML
    private void handleBackAction(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

}
