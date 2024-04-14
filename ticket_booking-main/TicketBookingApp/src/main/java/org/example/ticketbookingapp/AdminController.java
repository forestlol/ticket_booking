package org.example.ticketbookingapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.util.Callback;
import data.Event;
import javafx.util.StringConverter;
import service.AccountService;
import service.DatabaseService;
import service.EventService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import javafx.stage.Stage;
import service.StatisticsService;
import user.TicketingOfficer;
import user.User;

public class AdminController {

    private DatabaseService databaseService;
    private EventService eventService;

    @FXML
    private TableView<Event> eventsTable;

    @FXML
    private TableColumn<Event, Void> actionsColumn;

    @FXML
    private ComboBox<Event> authorisedEvents;
    @FXML
    private ComboBox<TicketingOfficer> authorisedTO;

    public AdminController() {
        try {
            // Initialize the DatabaseService with your credentials
            databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            // Create an instance of EventService
            eventService = new EventService(databaseService);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() throws SQLException {
        loadEvents();
        setupActionsColumn();
        loadAuthorisedEvents();
        loadTicketingOfficers();
    }

    private void loadAuthorisedEvents() {
        List<Event> nonCancelledEvents = eventService.getAllEvents().stream()
                .filter(event -> !event.getIsCancelled())
                .collect(Collectors.toList());

        authorisedEvents.setConverter(new StringConverter<Event>() {
            @Override
            public String toString(Event event) {
                return event != null ? String.valueOf(event.getEventID()) : null;
            }

            @Override
            public Event fromString(String eventID) {
                return nonCancelledEvents.stream()
                        .filter(event -> Integer.toString(event.getEventID()).equals(eventID))
                        .findFirst()
                        .orElse(null);
            }
        });

        authorisedEvents.getItems().setAll(nonCancelledEvents);
    }

    private void loadTicketingOfficers() throws SQLException {
        List<TicketingOfficer> officers = databaseService.getAllTicketingOfficers();

        authorisedTO.setConverter(new StringConverter<TicketingOfficer>() {
            @Override
            public String toString(TicketingOfficer officer) {
                return officer != null ? String.valueOf(officer.getUserID()) : null;
            }

            @Override
            public TicketingOfficer fromString(String userID) {
                return officers.stream()
                        .filter(officer -> Integer.toString(officer.getUserID()).equals(userID))
                        .findFirst()
                        .orElse(null);
            }
        });

        authorisedTO.getItems().setAll(officers);
    }


    @FXML
    public void handleAuthorisedButton(ActionEvent event) {
        Event selectedEvent = authorisedEvents.getSelectionModel().getSelectedItem();
        TicketingOfficer selectedOfficer = authorisedTO.getSelectionModel().getSelectedItem();

        if (selectedEvent == null || selectedOfficer == null) {
            showAlert("Selection Error", "Please select both an event and a ticketing officer.", Alert.AlertType.ERROR);
            return;
        }

        try {
            List<Integer> officerIds = new ArrayList<>();
            officerIds.add(selectedOfficer.getUserID());
            Map<Integer, Boolean> result = databaseService.addAuthorisedOfficer(AccountService.getCurrentUserID(), selectedEvent.getEventID(), officerIds);

            // Provide feedback based on the operation's success or failure
            if (result.get(selectedOfficer.getUserID())) {
                showAlert("Authorization Success", "The ticketing officer has been successfully authorized for the event.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Authorization Failure", "Failed to authorize the ticketing officer for the event. They may already be authorized, or an error occurred.", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Error accessing the database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String header, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(header);
        alert.showAndWait();
    }

    private void loadEvents() {
        eventsTable.getItems().setAll(eventService.getManagedEvents());
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(new Callback<TableColumn<Event, Void>, TableCell<Event, Void>>() {
            @Override
            public TableCell<Event, Void> call(final TableColumn<Event, Void> param) {
                return new TableCell<Event, Void>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Cancel");
                    private final Button exportButton = new Button("Export");
                    private final HBox hbox = new HBox(editButton, deleteButton, exportButton); // Include the export button

                    {
                        editButton.setOnAction((ActionEvent event) -> {
                            Event eventData = getTableView().getItems().get(getIndex());
                            handleEditAction(eventData);
                        });
                        deleteButton.setOnAction((ActionEvent event) -> {
                            Event eventData = getTableView().getItems().get(getIndex());
                            try {
                                handleDeleteAction(eventData);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        exportButton.setOnAction(event -> {
                            Event eventData = getTableView().getItems().get(getIndex());
                            handleExportAction(eventData);
                        });
                        hbox.setSpacing(10);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                            setGraphic(null);
                        } else {
                            Event eventData = getTableView().getItems().get(getIndex());

                            // Set button states based on the event's data
                            deleteButton.setDisable(eventData.getIsCancelled());
                            deleteButton.setText(eventData.getIsCancelled() ? "Cancelled" : "Cancel");

                            setGraphic(hbox);  // Apply the graphics to the cell
                        }
                    }

                };
            }
        });
    }

    private void handleExportAction(Event eventData) {
        if (eventData == null) {
            showAlert("Export Error", "No event selected to export.", Alert.AlertType.ERROR);
            return;
        }

        StatisticsService statisticsService = new StatisticsService(databaseService);
        String outputPath = "output/Event_" + eventData.getEventID() + "_Data.csv";
        List<Integer> eventIDs = Collections.singletonList(eventData.getEventID());

        // Ensure the directory exists
        File directory = new File("output");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                showAlert("Export Error", "Failed to create directory for export.", Alert.AlertType.ERROR);
                return;
            }
        }

        try {
            statisticsService.exportDataToCSV(eventIDs, outputPath);
            showAlert("Export Success", "Event data exported successfully to " + outputPath, Alert.AlertType.INFORMATION);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showAlert("Export Error", "Failed to export event data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private void handleEditAction(Event eventData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("editEventDialog.fxml"));
            Parent root = loader.load();
            EditEventDialogController dialogController = loader.getController();
            dialogController.setEventToEdit(eventData);
            dialogController.setAdminController(this);  // Pass this controller to the dialog

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Event");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            // handle exception
        }
    }


    @FXML
    public void handleCreateAction(ActionEvent event) {
        try {
            // Load the FXML file for the Create Event Dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("createEventDialog.fxml")); // Adjust the path as necessary
            Parent root = loader.load();

            // Create a new Stage for the popup window
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create Event");
            dialogStage.initModality(Modality.APPLICATION_MODAL); // Block input events with other windows
            dialogStage.initOwner(((Node) event.getSource()).getScene().getWindow()); // Set the owner of the dialog

            // Set the scene and show the dialog Stage
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            // Refresh the events table after the dialog is closed
            refreshEventsTable();
            loadAuthorisedEvents();
            loadTicketingOfficers();
        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace for debugging
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDeleteAction(Event eventData) throws SQLException {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Cancellation");
        confirmationAlert.setHeaderText(null);
        confirmationAlert.setContentText("Are you sure you want to cancel the event: " + eventData.getEventName() + "?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // User chose OK
            eventService.cancelEvent(eventData.getEventID());
            // Now refresh the table to reflect the cancellation
            refreshEventsTable();
            loadAuthorisedEvents();
            loadTicketingOfficers();
            System.out.println("Cancellation confirmed for: " + eventData.getEventName());
        } else {
            // User chose Cancel or closed the dialog
            System.out.println("Cancellation aborted for: " + eventData.getEventName());
        }
    }

    public void refreshEventsTable() {
        // Call the loadEvents() method to repopulate the table with fresh data from the database.
        loadEvents();

        // Additionally, refresh the TableView to update its view with the new data.
        eventsTable.refresh();
    }


    @FXML
    void handleLogoutAction(ActionEvent event) throws IOException {
        navigateToLogin(event);
    }

    private void navigateToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Login");
        stage.show();
    }
}
