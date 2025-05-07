package ch.epfl.rechor.gui;

import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyGeoJsonConverter;
import ch.epfl.rechor.journey.JourneyIcalConverter;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the UI component that displays detailed information about a journey.
 * This class follows the record pattern to ensure immutability of the UI component.
 */
public record DetailUI(Node rootNode) {
    private static final String STYLE_SHEET = "detail.css";
    private static final String MAP_BASE_URL = "https://umap.osm.ch/fr/map/";
    private static final int SPACING = 10;
    private static final int PADDING = 10;
    private static final int STROKE_WIDTH = 2;

    /**
     * Represents a pair of circles that should be connected by a line.
     * This is used to track connections between journey steps.
     */
    private record CirclePair(Circle departure, Circle arrival) {}

    /**
     * A custom GridPane that draws connection lines between journey steps.
     * It automatically draws lines between pairs of circles when the layout is updated.
     */
    private static class StepsGridPane extends GridPane {
        final List<CirclePair> circleConnections = new ArrayList<>();

        StepsGridPane() {
            setHgap(SPACING);
            setVgap(5);
            setPadding(new Insets(PADDING));
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();

            // Remove any existing lines before redrawing
            getChildren().removeIf(node -> node instanceof Line);

            // Create new lines based on circle positions
            for (CirclePair pair : circleConnections) {
                double startX = pair.departure.getBoundsInParent().getCenterX();
                double startY = pair.departure.getBoundsInParent().getCenterY();
                double endX = pair.arrival.getBoundsInParent().getCenterX();
                double endY = pair.arrival.getBoundsInParent().getCenterY();

                Line line = new Line(startX, startY, endX, endY);
                line.setStroke(Color.RED);
                line.setStrokeWidth(STROKE_WIDTH);

                getChildren().add(line);
            }
        }
    }

    /**
     * Creates a new DetailUI instance for the given journey.
     *
     * @param journeyObservable An observable value containing the journey to display.
     * @return A new DetailUI instance.
     */
    public static DetailUI create(ObservableValue<Journey> journeyObservable) {
        BorderPane rootPane = new BorderPane();
        rootPane.getStylesheets().add(STYLE_SHEET);
        rootPane.setId("detail");

        // Create the "No journey" message
        StackPane noJourneyPane = createNoJourneyPane();

        // Create the content pane for journey details
        BorderPane journeyPane = new BorderPane();
        journeyPane.setVisible(false);

        // Set up the steps display with annotations
        StepsGridPane stepsGrid = new StepsGridPane();

        ScrollPane scrollPane = new ScrollPane(stepsGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        journeyPane.setCenter(scrollPane);

        // Add action buttons
        HBox buttonsBox = createButtonsBox();
        Button mapButton = new Button("Carte");
        Button calendarButton = new Button("Calendrier");

        buttonsBox.getChildren().addAll(mapButton, calendarButton);
        journeyPane.setBottom(buttonsBox);

        // Add both panes to the root
        rootPane.getChildren().addAll(noJourneyPane, journeyPane);

        // Initialize with current journey if available
        Journey currentJourney = journeyObservable.getValue();
        if (currentJourney != null) {
            updateJourneyView(currentJourney, stepsGrid, journeyPane, noJourneyPane);
            configureButtons(mapButton, calendarButton, currentJourney);
        }

        // Listen for changes to the journey
        journeyObservable.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateJourneyView(newValue, stepsGrid, journeyPane, noJourneyPane);
                configureButtons(mapButton, calendarButton, newValue);
            } else {
                journeyPane.setVisible(false);
                noJourneyPane.setVisible(true);
            }
        });

        return new DetailUI(rootPane);
    }

    /**
     * Creates a pane displaying a message when no journey is selected.
     *
     * @return A StackPane containing the "no journey" message
     */
    private static StackPane createNoJourneyPane() {
        StackPane noJourneyPane = new StackPane();
        noJourneyPane.setId("no-journey");
        Label noJourneyLabel = new Label("Aucun voyage");
        noJourneyPane.getChildren().add(noJourneyLabel);
        return noJourneyPane;
    }

    /**
     * Creates a HBox for containing action buttons.
     *
     * @return A styled HBox for buttons
     */
    private static HBox createButtonsBox() {
        HBox buttonsBox = new HBox(SPACING);
        buttonsBox.setPadding(new Insets(PADDING));
        buttonsBox.setAlignment(Pos.CENTER);
        return buttonsBox;
    }

    /**
     * Updates the view with the details of the given journey.
     *
     * @param journey The journey to display
     * @param stepsGrid The grid to populate with journey steps
     * @param journeyPane The journey details pane to make visible
     * @param noJourneyPane The "no journey" pane to hide
     */
    private static void updateJourneyView(Journey journey, StepsGridPane stepsGrid,
                                          BorderPane journeyPane, StackPane noJourneyPane) {
        stepsGrid.getChildren().clear();
        stepsGrid.circleConnections.clear();

        populateStepsGrid(journey, stepsGrid);

        journeyPane.setVisible(true);
        noJourneyPane.setVisible(false);
    }

    /**
     * Populates the steps grid with journey information.
     *
     * @param journey The journey to display
     * @param grid The grid to populate
     */
    private static void populateStepsGrid(Journey journey, StepsGridPane grid) {
        if (journey == null || journey.legs() == null || journey.legs().isEmpty()) {
            return;
        }

        // Row counter for grid placement
        int row = 0;
        Circle previousCircle = null;

        // Add header
        Label headerLabel = new Label("Étapes du voyage");
        headerLabel.getStyleClass().add("steps-header");
        grid.add(headerLabel, 0, row++, 2, 1);

        // Add each step
        for (var step : journey.legs()) {
            // Create departure indicator
            Circle departureCircle = new Circle(5, Color.RED);
            Label departureLabel = new Label(formatDateTime(step.depTime()));
            Label locationLabel = new Label(step.depStop().name());
            locationLabel.getStyleClass().add("location");

            grid.add(departureCircle, 0, row);
            grid.add(departureLabel, 1, row++);
            grid.add(locationLabel, 1, row++);

            // Connect with previous step if not the first one
            if (previousCircle != null) {
                grid.circleConnections.add(new CirclePair(previousCircle, departureCircle));
            }

            previousCircle = departureCircle;
        }

        // Add final destination
        if (!journey.legs().isEmpty()) {
            var lastStep = journey.legs().getLast();
            Circle arrivalCircle = new Circle(5, Color.RED);
            Label arrivalLabel = new Label(formatDateTime(lastStep.arrTime()));
            Label finalLocationLabel = new Label(lastStep.arrStop().name());
            finalLocationLabel.getStyleClass().add("location");

            grid.add(arrivalCircle, 0, row);
            grid.add(arrivalLabel, 1, row++);
            grid.add(finalLocationLabel, 1, row);

            // Connect with last step
            if (previousCircle != null) {
                grid.circleConnections.add(new CirclePair(previousCircle, arrivalCircle));
            }
        }
    }

    /**
     * Format a LocalDateTime for display.
     *
     * @param dateTime The datetime to format
     * @return Formatted date and time string
     */
    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Configures the map and calendar buttons for the given journey.
     *
     * @param mapButton The map button to configure
     * @param calendarButton The calendar button to configure
     * @param journey The journey to use for the button actions
     */
    private static void configureButtons(Button mapButton, Button calendarButton, Journey journey) {
        // Configure the map button to open a map with the journey path
        mapButton.setOnAction(e -> openJourneyMap(journey, mapButton));

        // Configure the calendar button to save journey as iCalendar file
        calendarButton.setOnAction(e -> saveJourneyToCalendar(journey, calendarButton));
    }

    /**
     * Opens a map showing the journey path.
     *
     * @param journey The journey to display on the map
     * @param sourceNode Node to use as parent for any error dialogs
     */
    private static void openJourneyMap(Journey journey, Node sourceNode) {
        try {
            String geoJsonString = "{\"type\":\"LineString\",\"coordinates\":" +
                    JourneyGeoJsonConverter.toGeoJson(journey) + "}";
            geoJsonString = geoJsonString.replaceAll("\\s+", "");

            String encodedData = URLEncoder.encode(geoJsonString, StandardCharsets.UTF_8);
            URI uri = new URI(MAP_BASE_URL + "?data=" + encodedData);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                showError(sourceNode, "Impossible d'ouvrir le navigateur web");
            }
        } catch (URISyntaxException | IOException ex) {
            showError(sourceNode, "Impossible d'ouvrir la carte: " + ex.getMessage());
        }
    }

    /**
     * Saves the journey details as an iCalendar (.ics) file.
     *
     * @param journey The journey to save
     * @param sourceNode Node to use as parent for dialogs
     */
    private static void saveJourneyToCalendar(Journey journey, Node sourceNode) {
        LocalDateTime journeyDate = journey.depTime();
        String defaultFileName = "voyage_" + journeyDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".ics";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le voyage au format iCalendar");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers iCalendar (*.ics)", "*.ics"));

        Optional.ofNullable(fileChooser.showSaveDialog(sourceNode.getScene().getWindow()))
                .ifPresent(file -> {
                    try {
                        String icalContent = JourneyIcalConverter.toIcalendar(journey);
                        Files.writeString(Path.of(file.getPath()), icalContent);
                    } catch (IOException ex) {
                        showError(sourceNode, "Impossible de sauvegarder le fichier: " + ex.getMessage());
                    }
                });
    }

    /**
     * Shows an error dialog with the specified message.
     *
     * @param parentNode The parent node for the dialog
     * @param message The error message to display
     */
    private static void showError(Node parentNode, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.initOwner(parentNode.getScene().getWindow());
        alert.showAndWait();
    }
}