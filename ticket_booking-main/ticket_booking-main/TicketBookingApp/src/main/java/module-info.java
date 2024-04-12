module org.example.ticketbookingapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;

    // Open package to javafx.base module
    opens data to javafx.base;

    requires com.dlsc.formsfx;
    requires java.sql;
    requires java.mail;
    requires org.apache.pdfbox;
    requires commons.csv;
    requires activation;
    requires mysql.connector.java;

    opens org.example.ticketbookingapp to javafx.fxml;
    exports org.example.ticketbookingapp;
}