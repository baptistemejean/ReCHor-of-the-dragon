package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.Json;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyGeoJsonConverter;
import ch.epfl.rechor.journey.JourneyIcalConverter;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
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
 * Detailed user interface for displaying journey information.
 * <p>
 * This class provides a comprehensive view of a journey, including:
 * <ul>
 * <li>Step-by-step journey details</li>
 * <li>Interactive map and calendar export buttons</li>
 * </ul>
 * </p>
 * The UI is designed to be responsive and dynamically update
 * when the journey changes.
 */
public record DetailUI(Node rootNode) {
    // Configuration Constants
    private static final String STYLE_SHEET = "detail.css";
    private static final String MAP_SCHEME = "https";
    private static final String MAP_AUTHORITY = "umap.osm.ch";
    private static final String MAP_PATH = "/fr/map";
    private static final String MAP_QUERY_PARAM = "data";

    // Layout Constants
    private static final int ANNOTATIONS_STROKE_WIDTH = 2;
    private static final int VEHICLE_ICON_SIZE = 31;
    private static final int ANNOTATIONS_CIRCLE_RADIUS = 3;
    private static final javafx.scene.paint.Color ANNOTATIONS_CIRCLE_COLOR = Color.BLACK;
    private static final javafx.scene.paint.Color ANNOTATIONS_LINE_COLOR = Color.RED;


    /**
     * Represents a pair of connected journey step circles.
     * Used for drawing connection lines between journey steps.
     */
    private record CirclePair(Circle departure, Circle arrival) {}

    /**
     * Custom GridPane that automatically draws connection lines
     * between journey step circles.
     */
    private static class StepsGridPane extends GridPane {
        private final List<CirclePair> circleConnections;
        private final Pane annotationsPane;

        /**
         * Constructs a new JourneyStepsGridPane.
         *
         * @param annotationsPane Pane for drawing additional annotations
         */
        public StepsGridPane(Pane annotationsPane) {
            super();
            this.annotationsPane = annotationsPane;
            this.circleConnections = new ArrayList<>();
//            configureLayout();
        }

        /**
         * Configures the initial layout of the grid pane.
         */
        private void configureLayout() {
//            setHgap(SPACING);
//            setVgap(5);
//            setPadding(new Insets(PADDING));
        }

        /**
         * Adds a pair of connected circles to be drawn.
         *
         * @param circlePair The pair of circles to connect
         */
        public void addCirclePair(CirclePair circlePair) {
            circleConnections.add(circlePair);
        }

        /**
         * Clears all existing circle connections and grid children.
         */
        public void clear() {
            circleConnections.clear();
            annotationsPane.getChildren().clear();
            this.getChildren().clear();
        }

        /**
         * Custom layout method to draw connection lines between circles.
         */
        @Override
        protected void layoutChildren() {
            super.layoutChildren();

            // Remove existing connection lines
            annotationsPane.getChildren().removeIf(node -> node instanceof Line);

            // Redraw connection lines
            for (CirclePair pair : circleConnections) {
                drawConnectionLine(pair);
            }
        }

        /**
         * Draws a connection line between two circles.
         *
         * @param pair The pair of circles to connect
         */
        private void drawConnectionLine(CirclePair pair) {
            double startX = pair.departure.getBoundsInParent().getCenterX();
            double startY = pair.departure.getBoundsInParent().getCenterY();
            double endX = pair.arrival.getBoundsInParent().getCenterX();
            double endY = pair.arrival.getBoundsInParent().getCenterY();

            Line line = new Line(startX, startY, endX, endY);
            line.setStroke(ANNOTATIONS_LINE_COLOR);
            line.setStrokeWidth(ANNOTATIONS_STROKE_WIDTH);

            annotationsPane.getChildren().add(line);
        }
    }

    /**
     * Creates a new DetailUI instance for the given journey.
     *
     * @param journeyO An observable value containing the journey to display
     * @return A new DetailUI instance
     */
    public static DetailUI create(ObservableValue<Journey> journeyO) {
        // Create the main container
        StackPane mainContainer = createMainContainer(journeyO);

        // Create scrollable root pane
        ScrollPane rootPane = createRootScrollPane(mainContainer);

        return new DetailUI(rootPane);
    }

    /**
     * Creates the main container for journey details.
     *
     * @param journeyObservable The observable journey
     * @return A StackPane containing journey details and controls
     */
    private static StackPane createMainContainer(ObservableValue<Journey> journeyObservable) {
        StackPane mainContainer = new StackPane();

        // No journey placeholder
        VBox noJourneyBox = createNoJourneyBox();
        mainContainer.getChildren().add(noJourneyBox);

        // Journey details container
        VBox journeyDetailsContainer = new VBox();
        journeyDetailsContainer.setVisible(false);
        mainContainer.getChildren().add(journeyDetailsContainer);

        journeyObservable.subscribe((oldValue, newValue) -> {
            if (newValue != null) {
                noJourneyBox.setVisible(false);
                journeyDetailsContainer.setVisible(true);
            } else {
                noJourneyBox.setVisible(true);
                journeyDetailsContainer.setVisible(false);
            }
        });

        // Steps and annotations container
        StackPane stepsAndAnnotationsContainer = new StackPane();
        journeyDetailsContainer.getChildren().add(stepsAndAnnotationsContainer);

        // Action buttons
        HBox actionButtonsBox = createActionButtonsBox(journeyObservable);
        journeyDetailsContainer.getChildren().add(actionButtonsBox);

        // Annotations pane
        Pane annotationsPane = new Pane();
        annotationsPane.setId("annotations");

        // Steps grid
        StepsGridPane stepsGridPane = new StepsGridPane(annotationsPane);
        stepsGridPane.setId("legs");

        // Populate initial journey details
//        populateStepsGrid(journeyObservable.getValue(), stepsGridPane);

        // Add panes to container
        stepsAndAnnotationsContainer.getChildren().add(annotationsPane);
        stepsAndAnnotationsContainer.getChildren().add(stepsGridPane);

        // Add listener to update UI when journey changes
        addJourneyChangeListener(journeyObservable, stepsGridPane, actionButtonsBox);

        return mainContainer;
    }

    /**
     * Creates an HBox with action buttons for the journey.
     *
     * @param journeyObservable The observable journey
     * @return An HBox containing action buttons
     */
    private static HBox createActionButtonsBox(ObservableValue<Journey> journeyObservable) {
        HBox buttonsBox = new HBox();
        buttonsBox.setId("buttons");
        buttonsBox.setAlignment(Pos.CENTER);

        populateButtonsBox(journeyObservable.getValue(), buttonsBox);

        return buttonsBox;
    }

    /**
     * Adds a listener to update the UI when the journey changes.
     *
     * @param journeyObservable The observable journey
     * @param stepsGridPane The grid pane for journey steps
     * @param buttonsBox The box containing action buttons
     */
    private static void addJourneyChangeListener(
            ObservableValue<Journey> journeyObservable,
            StepsGridPane stepsGridPane,
            HBox buttonsBox) {

        journeyObservable.subscribe((oldValue, newValue) -> {
            // Clear and repopulate steps grid
            stepsGridPane.clear();
            populateStepsGrid(newValue, stepsGridPane);

            // Refresh action buttons
            buttonsBox.getChildren().clear();
            populateButtonsBox(newValue, buttonsBox);
        });
    }

    /**
     * Creates a scrollable root pane for the journey details.
     *
     * @param content The content to be scrolled
     * @return A ScrollPane containing the journey details
     */
    private static ScrollPane createRootScrollPane(Pane content) {
        ScrollPane rootPane = new ScrollPane(content);
        rootPane.setFitToWidth(true);
        rootPane.setPannable(true);
        rootPane.getStylesheets().add(STYLE_SHEET);
        rootPane.setId("detail");
        return rootPane;
    }

    /**
     * Creates a placeholder box when no journey is selected.
     *
     * @return A VBox with a "Aucun voyage" message
     */
    private static VBox createNoJourneyBox() {
        VBox noJourney = new VBox();
        noJourney.setId("no-journey");
        Text noJourneyLabel = new Text("Aucun voyage");
        noJourney.getChildren().add(noJourneyLabel);
        return noJourney;
    }

    /**
     * Populates the action buttons box with map and calendar buttons.
     *
     * @param journey The current journey
     * @param buttonsBox The HBox to populate with buttons
     */
    private static void populateButtonsBox(Journey journey, HBox buttonsBox) {
        Button mapButton = createMapButton(journey);
        Button calendarButton = createCalendarButton(journey);

        buttonsBox.getChildren().addAll(mapButton, calendarButton);
    }

    /**
     * Creates a map button to view the journey on a map.
     *
     * @param journey The journey to display on the map
     * @return A Button that opens the journey map
     */
    private static Button createMapButton(Journey journey) {
        Button mapButton = new Button("Carte");
        mapButton.setOnAction(e -> openJourneyMap(journey, mapButton));
        return mapButton;
    }

    /**
     * Creates a calendar button to export the journey to iCal.
     *
     * @param journey The journey to export
     * @return A Button that saves the journey to calendar
     */
    private static Button createCalendarButton(Journey journey) {
        Button calendarButton = new Button("Calendrier");
        calendarButton.setOnAction(e -> saveJourneyToCalendar(journey, calendarButton));
        return calendarButton;
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

        int rowCount = 0;

        for (Journey.Leg leg : journey.legs()) {
            switch (leg) {
                case Journey.Leg.Foot f -> {
                    rowCount = addFootLeg(f, grid, rowCount);
                }
                case Journey.Leg.Transport t -> {
                    rowCount = addTransportLeg(t, grid, rowCount);
                }
            }
        }
    }

    /**
     * Adds a formatted foot leg to the grid.
     * @param leg The leg to add
     * @param grid The grid to add the formatted leg to
     * @param rowCount The row count before adding this leg
     * @return The updated row count
     */
    private static int addFootLeg (Journey.Leg.Foot leg, StepsGridPane grid, int rowCount) {
        Text text = new Text(FormatterFr.formatLeg(leg));
        grid.add(text, 2, rowCount, 2, 1);

        return rowCount + 1;
    }

    /**
     * Adds a formatted transport leg to the grid.
     * @param leg The leg to add
     * @param grid The grid to add the formatted leg to
     * @param rowCount The row count before adding this leg
     * @return The updated row count
     */
    private static int addTransportLeg (Journey.Leg.Transport leg, StepsGridPane grid, int rowCount) {
        Text depTime = new Text(FormatterFr.formatTime(leg.depTime()));
        depTime.getStyleClass().add("departure");
        Text arrTime = new Text(FormatterFr.formatTime(leg.arrTime()));

        Text depStation = new Text(leg.depStop().name());
        Text arrStation = new Text(leg.arrStop().name());

        Text depPlatform = new Text(FormatterFr.formatPlatformName(leg.depStop()));
        depPlatform.getStyleClass().add("departure");
        Text arrPlatform = new Text(FormatterFr.formatPlatformName(leg.arrStop()));

        Circle depCircle = new Circle(ANNOTATIONS_CIRCLE_RADIUS, ANNOTATIONS_CIRCLE_COLOR);
        Circle arrCircle = new Circle(ANNOTATIONS_CIRCLE_RADIUS, ANNOTATIONS_CIRCLE_COLOR);

        ImageView vehicleImageView = new ImageView(VehicleIcons.iconFor(leg.vehicle()));
        vehicleImageView.setFitHeight(VEHICLE_ICON_SIZE);
        vehicleImageView.setFitWidth(VEHICLE_ICON_SIZE);

        Text destName = new Text(FormatterFr.formatRouteDestination(leg));

        GridPane intermediateStopsGrid = new GridPane();
        intermediateStopsGrid.getStyleClass().add("intermediate-stops");

        int intermediateStopsRowCount = 0;
        for (Journey.Leg.IntermediateStop intermediateStop: leg.intermediateStops()) {
            Text intermediateStopDepTime = new Text(FormatterFr.formatTime(intermediateStop.depTime()));
            Text intermediateStopArrTime = new Text(FormatterFr.formatTime(intermediateStop.arrTime()));
            Text intermediateStopName = new Text(intermediateStop.stop().name());
            intermediateStopsGrid.add(intermediateStopArrTime, 0, intermediateStopsRowCount);
            intermediateStopsGrid.add(intermediateStopDepTime, 1, intermediateStopsRowCount);
            intermediateStopsGrid.add(intermediateStopName, 2, intermediateStopsRowCount);
            intermediateStopsRowCount++;
        }

        TitledPane intermediateStopsPane = new TitledPane();
        StringBuilder intermediateStopsPreviewBuilder =
                new StringBuilder()
                .append(intermediateStopsRowCount)
                .append(" ")
                .append("arrêts")
                .append(", ")
                .append(FormatterFr.formatDuration(leg.duration()));

        intermediateStopsPane.setText(intermediateStopsPreviewBuilder.toString());

        intermediateStopsPane.setContent(intermediateStopsGrid);

        Accordion intermediateStopsView = new Accordion(intermediateStopsPane);

        grid.add(depTime, 0, rowCount);
        grid.add(depCircle, 1, rowCount);
        grid.add(depStation, 2, rowCount);
        grid.add(depPlatform, 3, rowCount);

        rowCount++;

        grid.add(destName, 2, rowCount, 2, 1);

        rowCount++;

        // Different styling depending on if there are intermediate stops to display
        if (intermediateStopsRowCount > 0) {
            grid.add(vehicleImageView, 0, rowCount - 1, 1, 2);
            grid.add(intermediateStopsView, 2, rowCount, 2, 1);

            rowCount++;
        } else {
            grid.add(vehicleImageView, 0, rowCount - 1, 1, 1);

        }

        grid.add(arrTime, 0, rowCount);
        grid.add(arrCircle, 1, rowCount);
        grid.add(arrStation, 2, rowCount);
        grid.add(arrPlatform, 3, rowCount);

        grid.addCirclePair(new CirclePair(depCircle, arrCircle));

        return ++rowCount;
    }

    /**
     * Opens a map showing the journey path.
     *
     * @param journey The journey to display on the map
     * @param sourceNode Node to use as parent for any error dialogs
     */
    private static void openJourneyMap(Journey journey, Node sourceNode) {
        try {
            Json geoJsonString = JourneyGeoJsonConverter.toGeoJson(journey);

            StringBuilder queryBuilder = new StringBuilder()
                    .append(MAP_QUERY_PARAM)
                    .append("=")
                    .append(URLEncoder.encode(geoJsonString.toString(), StandardCharsets.UTF_8));
            URI uri = new URI(MAP_SCHEME, MAP_AUTHORITY, MAP_PATH,  queryBuilder.toString(), null);

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
        StringBuilder defaultFileNameBuilder = new StringBuilder()
                .append("voyage_")
                .append(journeyDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .append(".ics");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le voyage au format iCalendar");
        fileChooser.setInitialFileName(defaultFileNameBuilder.toString());
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