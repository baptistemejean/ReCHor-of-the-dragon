package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.LocalTimeStringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A UI component that allows users to input journey query parameters, including
 * departure and arrival stops, date, and time of travel.
 * <p>
 * This record provides access to both the root UI node and observable properties
 * for the user's input values.
 */
public record QueryUI(
        Node rootNode,
        ObservableValue<String> depStopO,
        ObservableValue<String> arrStopO,
        ObservableValue<LocalDate> dateO,
        ObservableValue<LocalTime> timeO) {

    private static final String STYLE_SHEET = "query.css";
    private static final String TIME_DISPLAY_FORMAT = "HH:mm";
    private static final String TIME_PARSE_FORMAT = "H:mm";
    private static final String DEP_PROMPT = "Nom de l'arrêt de départ";
    private static final String ARR_PROMPT = "Nom de l'arrêt d'arrivée";
    private static final String TIME_PROMPT = "HH:mm";

    /**
     * Creates a new QueryUI instance with all necessary UI components.
     *
     * @param stopIndex The index of stops for autocomplete functionality
     * @return A new QueryUI instance
     */
    public static QueryUI create(StopIndex stopIndex) {
        // Create the main container and apply stylesheet
        VBox mainContainer = new VBox();
        mainContainer.getStylesheets().add(STYLE_SHEET);

        // Create containers for stop fields and date/time fields
        HBox stopFieldsContainer = new HBox();
        HBox dateTimeFieldsContainer = new HBox();

        // Create and configure departure field
        StopField depField = StopField.create(stopIndex);
        TextField depTextField = depField.field();
        depTextField.setPromptText(DEP_PROMPT);
        depTextField.setId("depStop");
        Label depLabel = new Label("Départ\u202f:");

        // Create and configure arrival field
        StopField arrField = StopField.create(stopIndex);
        TextField arrTextField = arrField.field();
        arrTextField.setPromptText(ARR_PROMPT);
        Label arrLabel = new Label("Arrivée\u202f:");

        // Create and configure switch button
        Button switchButton = createSwitchButton(depField, arrField);

        // Add all stop-related components to their container
        stopFieldsContainer.getChildren().addAll(
                depLabel,
                depTextField,
                switchButton,
                arrLabel,
                arrTextField
        );

        // Create and configure date picker
        DatePicker datePicker = new DatePicker();
        datePicker.setId("date");
        Label datePickerLabel = new Label("Date\u202f:");

        // Create and configure time picker with formatter
        TextField timePicker = new TextField();
        timePicker.setId("time");
        timePicker.setPromptText(TIME_PROMPT);

        TextFormatter<LocalTime> timeFormatter = createTimeFormatter();
        timePicker.setTextFormatter(timeFormatter);

        Label timePickerLabel = new Label("Heure\u202f:");

        // Add all date/time-related components to their container
        dateTimeFieldsContainer.getChildren().addAll(
                datePickerLabel,
                datePicker,
                timePickerLabel,
                timePicker
        );

        // Assemble the main container
        mainContainer.getChildren().addAll(stopFieldsContainer, dateTimeFieldsContainer);

        // Return the complete QueryUI instance
        return new QueryUI(
                mainContainer,
                depField.stopO(),
                arrField.stopO(),
                datePicker.valueProperty(),
                timeFormatter.valueProperty()
        );
    }

    /**
     * Creates a button that allows users to switch departure and arrival stops.
     *
     * @param depField The departure stop field
     * @param arrField The arrival stop field
     * @return A configured switch button
     */
    private static Button createSwitchButton(StopField depField, StopField arrField) {
        Button switchButton = new Button("↔");
        switchButton.setOnAction(event -> {
            String dep = depField.field().getText();
            String arr = arrField.field().getText();

            depField.setTo(arr);
            arrField.setTo(dep);
        });
        return switchButton;
    }

    /**
     * Creates a text formatter for the time input field that ensures correct time format.
     *
     * @return A configured TextFormatter for LocalTime values
     */
    private static TextFormatter<LocalTime> createTimeFormatter() {
        // Formatter for displaying time: always two digits (e.g., "09:30")
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern(TIME_DISPLAY_FORMAT);
        // Formatter for parsing input: allows "H:mm" and "HH:mm"
        DateTimeFormatter parseFormatter = DateTimeFormatter.ofPattern(TIME_PARSE_FORMAT);

        // Converter for LocalTime <-> String
        LocalTimeStringConverter converter = new LocalTimeStringConverter(displayFormatter, parseFormatter);

        // TextFormatter to format and validate input
        return new TextFormatter<>(converter);
    }
}