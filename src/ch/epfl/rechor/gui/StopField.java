package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

import java.util.List;

/**
 * A custom field component for stop selection.
 * <p>
 * This component provides a text field with a dropdown list that shows matching stops
 * as the user types. Users can navigate the suggestions using keyboard arrow keys and
 * select an item by clicking and pressing TAB.
 */
public record StopField(TextField field, ObservableValue<String> stopO) {
    /** Maximum number of search results to display in the dropdown */
    private static final int MAX_RESULTS = 25;

    /** Maximum height of the dropdown list in pixels */
    private static final double MAX_LIST_HEIGHT = 240;

    /**
     * Creates a new StopField based on the provided stop index.
     *
     * @param stopIndex The index containing stop names for autocomplete suggestions
     * @return A new StopField instance
     */
    public static StopField create(StopIndex stopIndex) {
        // Create the text field and selected stop property
        TextField textField = new TextField();
        ObjectProperty<String> selectedStop = new SimpleObjectProperty<>();

        // Set up the popup and list view for suggestions
        Popup popup = setupPopup();
        ObservableList<String> listedResults = FXCollections.observableArrayList();
        ListView<String> listView = setupListView(listedResults);

        // Add the list view to the popup
        popup.getContent().add(listView);

        // Initialize with matches for current text (empty initially)
        updateSuggestions(stopIndex, textField.getText(), listedResults);

        // Create listeners for updating the popup
        ChangeListener<String> textChangeListener = createTextChangeListener(stopIndex, listView, listedResults);
        ChangeListener<javafx.geometry.Bounds> boundsChangeListener = createBoundsChangeListener(textField, popup);

        // Configure focus behavior
        configureFocusBehavior(textField, popup, selectedStop, listView, stopIndex, listedResults, textChangeListener, boundsChangeListener);

        // Configure keyboard navigation
        configureKeyboardNavigation(textField, listView, listedResults);

        return new StopField(textField, selectedStop);
    }

    /**
     * Sets the field's text and updates the associated stop property.
     *
     * @param stopName The name of the stop to set
     */
    public void setTo(String stopName) {
        field.setText(stopName);
        ((ObjectProperty<String>) stopO).set(stopName);
    }

    /**
     * Creates and configures a popup for displaying autocomplete suggestions.
     *
     * @return A configured popup
     */
    private static Popup setupPopup() {
        Popup popup = new Popup();
        popup.setHideOnEscape(false);
        return popup;
    }

    /**
     * Creates and configures a list view for displaying autocomplete suggestions.
     *
     * @param items The observable list that will contain the suggestions
     * @return A configured list view
     */
    private static ListView<String> setupListView(ObservableList<String> items) {
        ListView<String> listView = new ListView<>(items);
        listView.setFocusTraversable(false);
        listView.setMaxHeight(MAX_LIST_HEIGHT);
        return listView;
    }

    /**
     * Updates the list of autocomplete suggestions based on the current text input.
     *
     * @param stopIndex The index containing stop names for autocomplete suggestions
     * @param text The current text in the field
     * @param resultsList The observable list to update with new suggestions
     */
    private static void updateSuggestions(StopIndex stopIndex, String text, ObservableList<String> resultsList) {
        List<String> matches = stopIndex.stopsMatching(text, MAX_RESULTS);
        resultsList.clear();
        resultsList.addAll(matches);
    }

    /**
     * Creates a listener that updates suggestions when the text changes.
     *
     * @param stopIndex The index containing stop names
     * @param resultsList The list to update with new suggestions
     * @return A change listener for text changes
     */
    private static ChangeListener<String> createTextChangeListener(
            StopIndex stopIndex,
            ListView<String> listView,
            ObservableList<String> resultsList) {
        return (observable, oldValue, newValue) -> {
            updateSuggestions(stopIndex, newValue, resultsList);
            listView.getSelectionModel().select(0);
        };
    }

    /**
     * Creates a listener that updates the popup position when the text field's bounds change.
     *
     * @param textField The text field to monitor
     * @param popup The popup to reposition
     * @return A change listener for bounds changes
     */
    private static ChangeListener<javafx.geometry.Bounds> createBoundsChangeListener(
            TextField textField,
            Popup popup) {
        return (observable, oldValue, newValue) -> {
            javafx.geometry.Bounds newScreenBounds = textField.localToScreen(newValue);
            popup.setAnchorX(newScreenBounds.getMinX());
            popup.setAnchorY(newScreenBounds.getMaxY());
        };
    }

    /**
     * Configures the behavior of the component when focus changes.
     *
     * @param textField The text field component
     * @param popup The popup for suggestions
     * @param selectedStop The property to update when a selection is made
     * @param listView The list view containing suggestions
     * @param textChangeListener The listener for text changes
     * @param boundsChangeListener The listener for bounds changes
     */
    private static void configureFocusBehavior(
            TextField textField,
            Popup popup,
            ObjectProperty<String> selectedStop,
            ListView<String> listView,
            StopIndex stopIndex,
            ObservableList<String> resultsList,
            ChangeListener<String> textChangeListener,
            ChangeListener<javafx.geometry.Bounds> boundsChangeListener) {

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Field gained focus
                handleFocusGained(textField, popup, selectedStop, listView, stopIndex, resultsList, textChangeListener, boundsChangeListener);
            } else {
                // Field lost focus
                handleFocusLost(textField, popup, selectedStop, listView, textChangeListener, boundsChangeListener);
            }
        });
    }

    /**
     * Handles actions when the text field gains focus.
     */
    private static void handleFocusGained(
            TextField textField,
            Popup popup,
            ObjectProperty<String> selectedStop,
            ListView<String> listView,
            StopIndex stopIndex,
            ObservableList<String> resultsList,
            ChangeListener<String> textChangeListener,
            ChangeListener<javafx.geometry.Bounds> boundsChangeListener) {

        // Restore selected value if it exists
        String selectedValue = selectedStop.getValue();
        if (selectedValue != null) {
            updateSuggestions(stopIndex, selectedValue, resultsList);
            textField.setText(selectedValue);
            listView.getSelectionModel().select(selectedValue);
        }

        // Show popup and add listeners
        popup.show(textField.getParent(), 0, 0);
        textField.textProperty().addListener(textChangeListener);
        textField.boundsInLocalProperty().addListener(boundsChangeListener);
    }

    /**
     * Handles actions when the text field loses focus.
     */
    private static void handleFocusLost(
            TextField textField,
            Popup popup,
            ObjectProperty<String> selectedStop,
            ListView<String> listView,
            ChangeListener<String> textChangeListener,
            ChangeListener<javafx.geometry.Bounds> boundsChangeListener) {

        // Update selected value and text field
        String selected = listView.getSelectionModel().getSelectedItem();
        selectedStop.set(selected);
        textField.setText(selected);

        // Hide popup and remove listeners
        popup.hide();
        textField.textProperty().removeListener(textChangeListener);
        textField.boundsInLocalProperty().removeListener(boundsChangeListener);
    }

    /**
     * Configures keyboard navigation for the suggestions list.
     *
     * @param textField The text field component
     * @param listView The list view containing suggestions
     * @param listedResults The list of suggestions
     */
    private static void configureKeyboardNavigation(
            TextField textField,
            ListView<String> listView,
            ObservableList<String> listedResults) {

        textField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case DOWN -> handleDownKeyPress(listView, listedResults, event);
                case UP -> handleUpKeyPress(listView, listedResults, event);
                case TAB -> textField.getParent().requestFocus();
                default -> { /* No action for other keys */ }
            }
        });
    }

    /**
     * Handles the down arrow key press for navigating suggestions.
     */
    private static void handleDownKeyPress(
            ListView<String> listView,
            ObservableList<String> listedResults,
            KeyEvent event) {

        int selectedId = listedResults.indexOf(listView.getSelectionModel().getSelectedItem());
        if (selectedId >= 0) {
            // Move selection down if not at the bottom
            if (selectedId < listedResults.size() - 1) {
                listView.getSelectionModel().select(selectedId + 1);
                listView.scrollTo(selectedId + 1);
            }
        }
        event.consume();
    }

    /**
     * Handles the up arrow key press for navigating suggestions.
     */
    private static void handleUpKeyPress(
            ListView<String> listView,
            ObservableList<String> listedResults,
            KeyEvent event) {

        int selectedId = listedResults.indexOf(listView.getSelectionModel().getSelectedItem());
        if (selectedId > 0) {
            // Move selection up if not at the top
            listView.getSelectionModel().select(selectedId - 1);
            listView.scrollTo(selectedId - 1);
        }
        event.consume();
    }
}