package org.example.ticketbookingapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class LoginController {
    @FXML
    private Label wrongInput;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    void PerformLogin(MouseEvent event){
        //wrongInput.setText(usernameField.getText()+ " " + passwordField.getText());

        // switch to main Menu.fxml
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
}