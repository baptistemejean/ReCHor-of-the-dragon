package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * A UI component that displays a summary of journey options.
 * <p>
 * This class creates a ListView showing available journeys with visual representation
 * of departure/arrival times, transfers, and duration. It supports selection of a journey
 * and provides access to the currently selected journey through an observable property.
 * </p>
 *
 * @param rootNode The root JavaFX node containing the summary UI
 * @param selectedJourneyO An observable value tracking the currently selected journey
 */
public record SummaryUI(Node rootNode, ObservableValue<Journey> selectedJourneyO) {
    // Configuration Constants
    private static final String STYLE_SHEET = "summary.css";

    // Layout Constants
    private static final int LINE_MARGIN = 5;
    private static final int CIRCLE_RADIUS = 3;

    private static final int VEHICLE_ICON_SIZE = 20;

    /**
     * Creates a new SummaryUI instance.
     *
     * @param journeys An observable list of available journeys to display
     * @param desiredTime An observable departure time reference used for initial selection
     * @return A new SummaryUI instance
     */
    public static SummaryUI create(ObservableValue<List<Journey>> journeys, ObservableValue<LocalTime> desiredTime) {
        ListView<Journey> journeyListView = createJourneyListView(journeys, desiredTime);
        journeyListView.setCellFactory(p -> new JourneyCell());

        return new SummaryUI(journeyListView, journeyListView.getSelectionModel().selectedItemProperty());
    }

    /**
     * Selects a journey from the list based on the desired departure time.
     * Attempts to find the first journey departing after the desired time,
     * or the last journey if no such journey exists.
     *
     * @param journeyListView The ListView containing journey options
     * @param journeys The list of available journeys
     * @param desiredTime The target departure time
     */
    private static void selectJourneyByDesiredTime(ListView<Journey> journeyListView,
                                                   List<Journey> journeys,
                                                   LocalTime desiredTime) {
        if (desiredTime == null || journeys == null || journeys.isEmpty()) {
            return;
        }

        // Find first journey departing after the desired time
        int index = 0;
        for (Journey journey : journeys) {
            if (journey.depTime().toLocalTime().isAfter(desiredTime)) {
                journeyListView.getSelectionModel().select(journey);
                journeyListView.scrollTo(index);
                return;
            }
            index++;
        }

        // If no journey found after desired time, select the last one
        journeyListView.getSelectionModel().select(journeys.getLast());
        journeyListView.scrollTo(journeys.size() - 1);
    }

    /**
     * Creates and configures the ListView component for displaying journeys.
     *
     * @param journeys An observable containing the list of journeys to display
     * @param desiredTime An observable containing the desired departure time
     * @return A configured ListView for journeys
     */
    private static ListView<Journey> createJourneyListView(ObservableValue<List<Journey>> journeys,
                                                           ObservableValue<LocalTime> desiredTime) {
        ObservableList<Journey> journeyItems = FXCollections.observableArrayList();
        ListView<Journey> journeyListView = new ListView<>(journeyItems);

        // Initialize with current journeys if available
        if (journeys.getValue() != null) {
            journeyItems.addAll(journeys.getValue());
        }

        // Select the right journey using the desired time
//        selectJourneyByDesiredTime(journeyListView, journeys.getValue(), desiredTime.getValue());

        // Listen to potential journey list changes
        journeys.subscribe((oldValue, newValue) -> {
            journeyItems.clear();
            if (newValue != null) {
                journeyItems.addAll(newValue);
            }
            selectJourneyByDesiredTime(journeyListView, newValue, desiredTime.getValue());
        });

        // Listen to potential desired time changes
        desiredTime.subscribe((oldValue, newValue) ->
                selectJourneyByDesiredTime(journeyListView, journeys.getValue(), newValue)
        );

        // Apply styling
        journeyListView.getStylesheets().add(STYLE_SHEET);
        journeyListView.setId("detail");

        return journeyListView;
    }

    /**
     * Custom ListCell implementation for rendering journey information.
     * <p>
     * Displays journey information including route, departure/arrival times,
     * duration, and a visual representation of transfers.
     * </p>
     */
    private static class JourneyCell extends ListCell<Journey> {
        // UI Components
        private final BorderPane mainContainer;
        private final ImageView vehicleIcon;
        private final Text routeLabel;
        private final Pane journeyTimelinePane;
        private final Text departureTimeLabel;
        private final Text arrivalTimeLabel;
        private final Text durationLabel;

        /**
         * Creates a new JourneyCell with all required UI components.
         */
        public JourneyCell() {
            // Initialize vehicle icon
            vehicleIcon = new ImageView();
            vehicleIcon.setFitWidth(VEHICLE_ICON_SIZE);
            vehicleIcon.setFitHeight(VEHICLE_ICON_SIZE);

            // Initialize route information components
            routeLabel = new Text();
            HBox routeContainer = new HBox(vehicleIcon, routeLabel);
            routeContainer.getStyleClass().add("route");

            // Initialize time components
            departureTimeLabel = new Text();
            departureTimeLabel.getStyleClass().add("departure");
            arrivalTimeLabel = new Text();

            // Initialize duration components
            durationLabel = new Text();
            HBox durationContainer = new HBox(durationLabel);
            durationContainer.getStyleClass().add("duration");

            // Initialize timeline pane to visualize the journey
            journeyTimelinePane = createJourneyTimelinePane();
            journeyTimelinePane.setPrefSize(0, 0);

            // Set up main container
            mainContainer = new BorderPane();
            mainContainer.setTop(routeContainer);
            mainContainer.setRight(arrivalTimeLabel);
            mainContainer.setBottom(durationContainer);
            mainContainer.setLeft(departureTimeLabel);
            mainContainer.setCenter(journeyTimelinePane);
            mainContainer.getStyleClass().add("journey");
        }

        /**
         * Creates a custom pane that visualizes the journey timeline with transfers.
         *
         * @return A configured Pane for displaying journey timeline
         */
        private Pane createJourneyTimelinePane() {
            return new Pane() {
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();

                    // Calculate vertical center of the pane
                    double centerY = getHeight() / 2.0;

                    // Create the journey line with margins
                    double lineStartX = LINE_MARGIN;
                    double lineEndX = getWidth() - LINE_MARGIN;

                    // Position the circles and lines
                    for (Node node : getChildren()) {
                        if (node instanceof Circle circle) {
                            // Get the relative X position (0.0 to 1.0)
                            Double relativeX = (Double) circle.getUserData();
                            if (relativeX == null) continue;

                            // Map to the actual pixel position, respecting margins
                            double actualX = lineStartX + relativeX * (lineEndX - lineStartX);

                            // Update the circle position
                            circle.setCenterX(actualX);
                            circle.setCenterY(centerY);
                        } else if (node instanceof Line line) {
                            // Position the journey line
                            line.setStartX(lineStartX);
                            line.setStartY(centerY);
                            line.setEndX(lineEndX);
                            line.setEndY(centerY);
                        }
                    }
                }
            };
        }

        @Override
        protected void updateItem(Journey journey, boolean empty) {
            super.updateItem(journey, empty);

            if (empty || journey == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            updateJourneyVisuals(journey);
            setGraphic(mainContainer);
        }

        /**
         * Updates the cell's visual components with journey information.
         *
         * @param journey The journey to display
         */
        private void updateJourneyVisuals(Journey journey) {
            // Clear previous timeline elements
            journeyTimelinePane.getChildren().clear();

            // Add main journey line
            Line journeyLine = new Line();
            journeyTimelinePane.getChildren().add(journeyLine);

            // Create departure and arrival points
            Circle departurePoint = createCircleWithStyle("dep-arr", 0.0);
            Circle arrivalPoint = createCircleWithStyle("dep-arr", 1.0);

            // Find first transport leg and add transfer points
            Journey.Leg.Transport firstTransportLeg = null;
            for (Journey.Leg leg : journey.legs()) {
                if (leg instanceof Journey.Leg.Transport transportLeg && firstTransportLeg == null) {
                    firstTransportLeg = transportLeg;
                } else if (leg instanceof Journey.Leg.Foot footLeg) {
                    // Add transfer points
                    double relativePosition = calculateRelativePosition(journey.depTime(),
                            footLeg.depTime(),
                            journey.duration());
                    Circle transferPoint = createCircleWithStyle("transfer", relativePosition);
                    journeyTimelinePane.getChildren().add(transferPoint);
                }
            }

            // Add departure and arrival points to the timeline
            journeyTimelinePane.getChildren().add(departurePoint);
            journeyTimelinePane.getChildren().add(arrivalPoint);

            // Update route information
            updateRouteInfo(firstTransportLeg);

            // Update time information
            departureTimeLabel.setText(FormatterFr.formatTime(journey.depTime()));
            arrivalTimeLabel.setText(FormatterFr.formatTime(journey.arrTime()));
            durationLabel.setText(FormatterFr.formatDuration(journey.duration()));
        }

        /**
         * Creates a circle with specified style class and relative position.
         *
         * @param styleClass CSS style class to apply
         * @param relativePosition Position as value between 0.0 and 1.0
         * @return Configured Circle instance
         */
        private Circle createCircleWithStyle(String styleClass, double relativePosition) {
            Circle circle = new Circle(CIRCLE_RADIUS);
            circle.getStyleClass().add(styleClass);
            circle.setUserData(relativePosition);
            return circle;
        }

        /**
         * Calculates the relative position of a time point within a journey.
         *
         * @param journeyStart Start time of the journey
         * @param timePoint The time point to calculate position for
         * @param journeyDuration Total duration of the journey
         * @return Relative position as value between 0.0 and 1.0
         */
        private double calculateRelativePosition(
                LocalDateTime journeyStart,
                LocalDateTime timePoint,
                Duration journeyDuration) {
            return (double) Duration.between(journeyStart, timePoint)
                    .toMillis() / journeyDuration.toMillis();
        }

        /**
         * Updates the route information based on the first transport leg.
         *
         * @param transportLeg The transport leg to display information for
         */
        private void updateRouteInfo(Journey.Leg.Transport transportLeg) {
            if (transportLeg != null) {
                routeLabel.setText(FormatterFr.formatRouteDestination(transportLeg));
                vehicleIcon.setImage(VehicleIcons.iconFor(transportLeg.vehicle()));
            }
        }
    }
}