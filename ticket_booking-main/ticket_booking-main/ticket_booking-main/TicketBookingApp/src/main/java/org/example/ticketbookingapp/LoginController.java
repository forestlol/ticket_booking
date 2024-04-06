package org.example.ticketbookingapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import service.AccountService;
import service.DatabaseService;
import user.User;


public class LoginController {

    private AccountService accountService;

    @FXML private Label wrongInput;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // Registration.fxml
    @FXML TextField emailRegisterField;
    @FXML TextField passwordRegisterField;
    @FXML TextField nameRegisterField;
    @FXML ComboBox<String> typeComboBox;
    @FXML Label errorLabel;

    public LoginController() {
        // Constructor is now empty
    }

    @FXML
    public void initialize() {
        try {
            // Initialization code that can throw SQLExceptions
            DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            accountService = new AccountService(databaseService);
        } catch (SQLException e) {
            e.printStackTrace();
            // Here, handle the exception, like showing an error message to the user
        }
    }

    @FXML
    void OpenRegistrationPage(ActionEvent event) {
        switchToRegister(event);
    }

    @FXML
    void OpenLoginPage(ActionEvent event) {
        switchToLogin(event);
    }

    @FXML
    void PerformLogin(ActionEvent event) {
        String email = usernameField.getText();
        String password = passwordField.getText();

        // Use accountService to perform login
        if (accountService.login(email, password)) {
            // Login successful, change to MainMenu.fxml
            System.out.println("Login Success!");
            switchToMainMenu(event);

        } else {
            // Login failed, show error message
            wrongInput.setVisible(true);
            wrongInput.setText("Wrong email or password!");
        }
    }

    @FXML
    void RegisterUser(ActionEvent event) {
        String email = emailRegisterField.getText();
        String password = passwordRegisterField.getText();
        String name = nameRegisterField.getText();
        String type = typeComboBox.getSelectionModel().getSelectedItem();

        //remove spaces in name
        name = name.trim();

        // Use accountService to perform login
        if (accountService.createUser(email,password,name,type)) {
            // Login successful, change to MainMenu.fxml
            switchToSuccess(event);

        } else {
            // Login failed, show error message
            errorLabel.setVisible(true);
        }
    }

    private void switchToMainMenu(ActionEvent event) {
        try {
            // Load the MainMenu.fxml file
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("MainMenu.fxml")));
            // Get the stage from the event that triggered this method
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // Set the new scene on the current stage
            stage.setScene(new Scene(root));
            // Optional: Set title for new stage
            stage.setTitle("Main Menu");
            // Show the updated stage
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (show an error message, log it, etc.)
        }
    }
    private void switchToLogin(ActionEvent event) {
        try {
            // Load the MainMenu.fxml file
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login.fxml")));
            // Get the stage from the event that triggered this method
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // Set the new scene on the current stage
            stage.setScene(new Scene(root));
            // Optional: Set title for new stage
            stage.setTitle("Login");
            // Show the updated stage
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (show an error message, log it, etc.)
        }
    }

    private void switchToRegister(ActionEvent event) {
        try {
            // Load the MainMenu.fxml file
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("register.fxml")));
            // Get the stage from the event that triggered this method
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // Set the new scene on the current stage
            stage.setScene(new Scene(root));
            // Optional: Set title for new stage
            stage.setTitle("Create Account");
            // Show the updated stage
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (show an error message, log it, etc.)
        }
    }

    private void switchToSuccess(ActionEvent event) {
        try {
            // Load the MainMenu.fxml file
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("registration-success.fxml")));
            // Get the stage from the event that triggered this method
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            // Set the new scene on the current stage
            stage.setScene(new Scene(root));
            // Optional: Set title for new stage
            stage.setTitle("Create account success!");
            // Show the updated stage
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (show an error message, log it, etc.)
        }
    }
}
