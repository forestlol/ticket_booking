module org.example.ticketbookingapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.sql;
    requires java.mail;
    requires org.apache.pdfbox;
    requires commons.csv;
    requires activation;

    opens org.example.ticketbookingapp to javafx.fxml;
    exports org.example.ticketbookingapp;
}