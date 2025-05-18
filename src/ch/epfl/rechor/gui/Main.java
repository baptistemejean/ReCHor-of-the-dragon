package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyExtractor;
import ch.epfl.rechor.journey.Profile;
import ch.epfl.rechor.journey.Router;
import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * Main application class for ReCHor, a railway journey planner application.
 * This class initializes the JavaFX application, sets up the UI components,
 * and manages the connection between the UI and the journey planning logic.
 */
public class Main extends Application {
    /** Application title displayed in the window */
    private final static String TITLE = "ReCHor";

    /** The path to the timetable data folder */
    private final static String TIMETABLE_PATH = "timetable";

    /** Observable list of journeys to be displayed in the UI */
    private ObservableValue<List<Journey>> displayedJourneys;

    /**
     * Application entry point.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Finds the ID of a station by its name.
     *
     * @param stations The stations database to search in
     * @param name The name of the station to find
     * @return The ID of the station
     * @throws NoSuchElementException If the station name is not found
     */
    private static int stationId(Stations stations, String name) {
        for (var i = 0; i < stations.size(); i++) {
            if (stations.name(i).equals(name)) {
                return i;
            }
        }
        throw new NoSuchElementException("Station not found: " + name);
    }

    /**
     * Initializes and starts the application.
     * Creates the UI components, sets up event handlers, and displays the main window.
     *
     * @param primaryStage The primary stage for this application
     * @throws Exception If an error occurs during application initialization
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize the timetable
        CachedTimeTable timeTable = new CachedTimeTable(FileTimeTable.in(Path.of(TIMETABLE_PATH)));
        StopIndex stopIndex = createStopIndex(timeTable);

        // Initialize UI components
        QueryUI queryUI = QueryUI.create(stopIndex);
        Stations stations = timeTable.stations();
        Router router = new Router(timeTable);
        ProfileCache profileCache = new ProfileCache();

        // Bind the journey search to UI inputs
        setupJourneyBinding(queryUI, stations, router, profileCache);

        // Create the summary and detail views
        SummaryUI summaryUI = SummaryUI.create(displayedJourneys, queryUI.timeO());
        DetailUI detailUI = DetailUI.create(summaryUI.selectedJourneyO());

        SplitPane summaryDetailContainer = new SplitPane(summaryUI.rootNode(), detailUI.rootNode());

        BorderPane root = new BorderPane();
        root.setCenter(summaryDetailContainer);
        root.setTop(queryUI.rootNode());

        Scene scene = new Scene(root);

        configureStage(primaryStage, scene);
        primaryStage.show();

        // Set initial focus to departure field
        Platform.runLater(() -> scene.lookup("#depStop").requestFocus());
    }

    /**
     * Creates a StopIndex containing all stations and their aliases.
     *
     * @param timeTable The timetable containing station information
     * @return A new StopIndex instance
     */
    private StopIndex createStopIndex(CachedTimeTable timeTable) {
        List<String> stringTable = new ArrayList<>();
        Map<String, String> aliasTable = new HashMap<>();

        for (int i = 0; i < timeTable.stations().size(); i++) {
            stringTable.add(timeTable.stations().name(i));
            if (i < timeTable.stationAliases().size()) {
                aliasTable.put(
                        timeTable.stationAliases().alias(i),
                        timeTable.stationAliases().stationName(i)
                );
            }
        }

        return new StopIndex(stringTable, aliasTable);
    }

    /**
     * Sets up the binding between the UI inputs and the journey search.
     *
     * @param queryUI The query UI component
     * @param stations The stations database
     * @param router The router for journey planning
     * @param profileCache Cache for route profiles
     */
    private void setupJourneyBinding(QueryUI queryUI, Stations stations, Router router, ProfileCache profileCache) {
        displayedJourneys = Bindings.createObjectBinding(
                () -> {
                    String depStop = queryUI.depStopO().getValue();
                    String arrStop = queryUI.arrStopO().getValue();
                    LocalDate date = queryUI.dateO().getValue();

                    // Check for null or empty values
                    if (depStop == null || depStop.isEmpty() ||
                        arrStop == null || arrStop.isEmpty() ||
                        date == null) {
                        return Collections.emptyList();
                    }

                    int depStationId = stationId(stations, depStop);
                    int arrStationId = stationId(stations, arrStop);

                    Profile profile = profileCache.setProfile(router, date, arrStationId);

                    return JourneyExtractor.journeys(profile, depStationId);
                },
                queryUI.depStopO(), queryUI.arrStopO(), queryUI.dateO()
        );
    }

    /**
     * Configures the primary stage.
     *
     * @param primaryStage The primary stage to configure
     * @param scene The scene to set on the stage
     */
    private void configureStage(Stage primaryStage, Scene scene) {
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setTitle(TITLE);
    }

    /**
     * A cache for journey profiles to avoid recalculating profiles when not necessary.
     * Stores the last calculated profile along with its parameters.
     */
    private static class ProfileCache {
        /** The date for which the profile was calculated */
        private LocalDate cachedDate;

        /** The arrival station ID for which the profile was calculated */
        private int cachedArrStationId;

        /** The cached profile */
        private Profile cachedProfile;

        /**
         * Gets a profile for the given parameters, using the cached profile if possible.
         *
         * @param router The router to use for profile calculation
         * @param newDate The date for which to get a profile
         * @param newArrStationId The arrival station ID for which to get a profile
         * @return A profile for the given parameters
         */
        public Profile setProfile(Router router, LocalDate newDate, int newArrStationId) {
            if (!Objects.equals(newDate, cachedDate) || newArrStationId != cachedArrStationId) {
                this.cachedProfile = router.profile(newDate, newArrStationId);
                this.cachedArrStationId = newArrStationId;
                this.cachedDate = newDate;
            }

            return this.cachedProfile;
        }
    }
}