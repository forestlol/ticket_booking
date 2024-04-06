package org.example.ticketbookingapp;

import data.Event;
import data.TicketOption;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import service.*;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CheckOutPageController {

    private AccountService accountService;
    private TicketService ticketService;
    private EventService eventService;

    private BookingService bookingService;

    private Event currentEvent;
    private final Map<String, Double> categoryPrices = new HashMap<>();

    @FXML private Label labelEventName;
    @FXML private Label labelStartTime;
    @FXML private Label labelDuration;
    @FXML private Label labelCancellationFee;
    @FXML private Label labelTotalPrice;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button decrementButton;
    @FXML private TextField quantityTextField;
    @FXML private Button incrementButton;
    @FXML private Button addToCartButton;
    @FXML private Text cartText;
    @FXML private Button buyTicketButton;
    @FXML
    private VBox categoriesContainer;

    @FXML
    private ListView<CartItem> cartListView;


    private final List<CartItem> cart = new ArrayList<>();
    private final DecimalFormat priceFormat = new DecimalFormat("#.00");

    private final ObservableList<CartItem> carts = FXCollections.observableArrayList();

    // Call this method from initialize()
    private void setupCartListView() {
        cartListView.setCellFactory(param -> new ListCell<CartItem>() {
            private final Button removeButton = new Button("Remove");

            @Override
            protected void updateItem(CartItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("%s: %d x $%.2f each", item.category, item.quantity, item.pricePerItem));
                    removeButton.setOnAction(event -> removeFromCart(item)); // Pass the item directly
                    setGraphic(removeButton);
                }
            }
        });

        cartListView.setItems(carts); // Make sure you're setting the correct list
    }


    // Inner class to store information about the cart items
    private static class CartItem {
        final String category;
        final int quantity;
        final double pricePerItem;

        CartItem(String category, int quantity, double pricePerItem) {
            this.category = category;
            this.quantity = quantity;
            this.pricePerItem = pricePerItem;
        }

        double getTotalPrice() {
            return quantity * pricePerItem;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CartItem cartItem = (CartItem) obj;
            return quantity == cartItem.quantity &&
                    Double.compare(cartItem.pricePerItem, pricePerItem) == 0 &&
                    Objects.equals(category, cartItem.category);
        }

        @Override
        public int hashCode() {
            return Objects.hash(category, quantity, pricePerItem);
        }
    }

    public CheckOutPageController() {
        // Constructor is empty, initialization happens in the @FXML initialize method
    }

    @FXML
    private void initialize() {
        try {
            DatabaseService databaseService = new DatabaseService("localhost:3306", "ticket_booking", "root", "");
            eventService = new EventService(databaseService);
            accountService = new AccountService(databaseService);
            ticketService = new TicketService(databaseService);
            bookingService = new BookingService(databaseService);

            setupQuantityTextField();
            setupAddToCartButton();
            //setupBuyTicketButton();
            setupCartListView();
            System.out.println("ListView initialized with items: " + carts.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupQuantityTextField() {
        quantityTextField.setText("0");
        decrementButton.setOnAction(e -> adjustQuantity(-1));
        incrementButton.setOnAction(e -> adjustQuantity(1));
    }

    private void adjustQuantity(int delta) {
        int quantity = Integer.parseInt(quantityTextField.getText());
        quantity = Math.max(0, quantity + delta);
        quantityTextField.setText(Integer.toString(quantity));
    }

    private void setupAddToCartButton() {
        addToCartButton.setOnAction(event -> {
            String selectedCategory = categoryComboBox.getValue();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityTextField.getText());
            } catch (NumberFormatException e) {
                quantity = 0;
            }

            if (quantity > 0 && selectedCategory != null && categoryPrices.containsKey(selectedCategory)) {
                double pricePerItem = categoryPrices.get(selectedCategory);
                CartItem newItem = new CartItem(selectedCategory, quantity, pricePerItem);
                carts.add(newItem); // Add directly to carts ObservableList
                updateTotalPriceDisplay();
            } else {
                // Show an error message or do nothing
            }
        });
    }


    @FXML
    private void handleBuyTicketAction(ActionEvent event) {
        try {
            for (CartItem item : carts) {
                int ticketOptionID = ticketService.getTicketOptionIDByName(currentEvent.getEventID(), item.category);
                if (ticketOptionID != -1) {
                    // Assuming you have a method createBooking in your BookingService
                    // This is a placeholder - adapt parameters and method call as needed
                    System.out.println("here");
                    bookingService.createBooking(currentEvent.getEventID(), ticketOptionID, AccountService.getCurrentUser().getID(), item.quantity);
                } else {
                    // Handle the case where the ticketOptionID was not found
                    // Show an error message or log the issue
                }
            }
            // Navigate to thank you page after successful booking creation
            navigateToThankYouPage(event);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle error (e.g., show error message)
        }
    }

    private void navigateToThankYouPage(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("thankyouPage.fxml"))); // Removed leading slash
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("thankyou");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading the login view.");
            e.printStackTrace();
            // Optionally, show an alert to the user
        }
    }

    // Call this method from the UI when a "Remove" link/button is clicked for a cart item
    // Method to remove a cart item
    private void removeFromCart(CartItem itemToRemove) {
        boolean isRemoved = carts.remove(itemToRemove);
        if (isRemoved) {
            System.out.println("Item removed successfully. Cart size is now " + carts.size());
        } else {
            System.out.println("Item not found in the cart. No removal occurred.");
        }
        updateTotalPriceDisplay(); // Update the total price display here
    }


    // Method to fetch and display available ticket categories and their prices
    private void updateCategoryPrices() {
        if (currentEvent == null) return;

        // Fetch the base price from the Event
        double basePrice = currentEvent.getBasePrice();

        // Fetch the multipliers for each category
        Map<String, Integer> availableSeats = new HashMap<>();
        Map<String, Double> priceMultipliers = new HashMap<>();
        for (TicketOption option : ticketService.getTicketOptionsByEvent(currentEvent.getEventID())) {
            availableSeats.put(option.getCategoryName(), option.getTotalAvailable());
            priceMultipliers.put(option.getCategoryName(), option.getPriceMultiplier());
        }

        // Calculate and update the prices for each category
        categoryPrices.clear();
        for (Map.Entry<String, Double> entry : priceMultipliers.entrySet()) {
            double price = basePrice * entry.getValue(); // Calculate the actual price using the multiplier
            categoryPrices.put(entry.getKey(), price);
        }

        // Now you can proceed to populate the ComboBox or any other UI elements with these updated prices
        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().addAll(categoryPrices.keySet());
        categoryComboBox.getSelectionModel().selectFirst();

        // Assuming you're showing available seats somewhere
        updateAvailableSeatsDisplay(currentEvent.getEventID());
    }

    // Call this method at appropriate place, for example, after setting the currentEvent
    public void setEventData(Event event) {
        this.currentEvent = event;
        updateEventDetailsUI();
        updateCategoryPrices(); // Ensure this is called after the currentEvent is updated
    }


    private void updateEventDetailsUI() {
        if (currentEvent != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
            labelEventName.setText(currentEvent.getEventName());
            labelStartTime.setText(currentEvent.getStartTime().format(formatter) + " / " + currentEvent.getVenue());
            labelDuration.setText("Duration: " + currentEvent.getDuration() + " minutes");
            labelCancellationFee.setText("Cancellation fee: $" + priceFormat.format(currentEvent.getTicketCancellationFee()));
            updateAvailableSeatsDisplay(currentEvent.getEventID());
        }
    }

    private void updateAvailableSeatsDisplay(int eventID) {
        Map<String, Integer> availableSeats = ticketService.getAvailableSeatsByCategory(eventID);

        // Clear any existing labels in the VBox container
        categoriesContainer.getChildren().clear();

        // Assuming availableSeats correctly maps category names to seat counts,
        // and there's no need to parse integers from category names.
        List<String> sortedCategories = new ArrayList<>(availableSeats.keySet());

        // If category names include numbers and you wish to sort by those numbers,
        // ensure the parsing logic is correct and handle any potential NumberFormatException.
        // For now, we'll sort category names alphabetically for simplicity.
        sortedCategories.sort(Comparator.naturalOrder());

        // Dynamically create a label for each category in sorted order
        for (String category : sortedCategories) {
            Integer seats = availableSeats.get(category);
            Label categoryLabel = new Label(String.format("%s: %d available", category, seats));
            categoryLabel.setStyle("-fx-text-fill: white;"); // Set the text color to white
            categoriesContainer.getChildren().add(categoryLabel); // Add the label to the VBox
        }

        // Update the ComboBox with the sorted category names, if necessary
        categoryComboBox.getItems().setAll(sortedCategories);
        if (!categoryComboBox.getItems().isEmpty()) {
            categoryComboBox.getSelectionModel().selectFirst();
        }

        // Reset the quantity text field to 0 as default
        quantityTextField.setText("0");

        // Optionally, update actions based on the new category selection
        // This might include updating the price or available quantity
        updatePriceAndQuantityBasedOnCategory(categoryComboBox.getValue());
    }

    private void updatePriceAndQuantityBasedOnCategory(String selectedCategory) {
        // Assuming categoryPrices map has been populated elsewhere as per your existing logic
        if (categoryPrices.containsKey(selectedCategory)) {
            double price = categoryPrices.get(selectedCategory);
            // Update UI elements to reflect price, etc., for the selected category
            // For example:
            // priceLabel.setText(String.format("$%.2f", price));
        }

        // Logic to update quantity available if necessary
        // This could involve querying availableSeats again or updating based on user selection
    }

    @FXML
    private void handleAddToCart() {
        String selectedCategory = categoryComboBox.getValue();
        int quantity;
        try {
            quantity = Integer.parseInt(quantityTextField.getText());
        } catch (NumberFormatException e) {
            // If the text field does not contain a valid integer, set the quantity to 0
            quantity = 0;
        }

        if (quantity > 0 && selectedCategory != null && categoryPrices.containsKey(selectedCategory)) {
            double pricePerItem = categoryPrices.get(selectedCategory);
            CartItem newItem = new CartItem(selectedCategory, quantity, pricePerItem);
            carts.add(newItem); // Add to the observable list bound to the ListView

            // Debugging output
            System.out.println("Added to cart: " + newItem.category + " x " + newItem.quantity);

            updateTotalPriceDisplay();
            // Since we are now only using the observable list, updateCartDisplay can be modified or removed if not used
            cartListView.refresh(); // Refresh the ListView to show the latest items

            // Debugging output
            System.out.println("ListView should now contain: " + carts.size() + " items.");
        } else {
            // Handle the case where the quantity is zero or the category is not selected
            // Possibly show an error message to the user
        }
    }

    private void updateTotalPriceDisplay() {
        // Calculate the total price using the carts ObservableList
        double total = carts.stream().mapToDouble(CartItem::getTotalPrice).sum();
        labelTotalPrice.setText(String.format("$%.2f", total));
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
}
