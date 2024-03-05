module org.example.ticketbookingapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens org.example.ticketbookingapp to javafx.fxml;
    exports org.example.ticketbookingapp;
}