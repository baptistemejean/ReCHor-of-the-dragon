package ch.epfl.rechor.gui;

import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyExtractor;
import ch.epfl.rechor.journey.Profile;
import ch.epfl.rechor.journey.Router;
import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.NoSuchElementException;

public final class TestSummaryUI extends Application {
    public static void main(String[] args) { launch(args); }

    private static int stationId(Stations stations, String name) {
        for (var i = 0; i < stations.size(); i += 1)
            if (stations.name(i).equals(name)) return i;
        throw new NoSuchElementException();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        TimeTable timeTable = new CachedTimeTable(
                FileTimeTable.in(Path.of("timetable")));
        Stations stations = timeTable.stations();
        LocalDate date = LocalDate.of(2025, Month.MARCH, 18);
        int depStationId = stationId(stations, "Lausanne");
        int arrStationId = stationId(stations, "Gruyères");
        Router router = new Router(timeTable);
        Profile profile = router.profile(date, arrStationId);

        List<Journey> journeys = JourneyExtractor
                .journeys(profile, depStationId);

        ObjectProperty<List<Journey>> journeysO =
                new SimpleObjectProperty<>(journeys);
        ObjectProperty<LocalTime> depTimeO =
                new SimpleObjectProperty<>(LocalTime.of(21, 57));
        SummaryUI summaryUI = SummaryUI.create(journeysO, depTimeO);

        Button switchJourney = new Button("Change (test)");
        switchJourney.setOnAction(e -> {
            int newDepStationId = stationId(stations, "Ecublens VD, EPFL");
            journeysO.set(JourneyExtractor.journeys(profile, newDepStationId));
        });

        GridPane root = new GridPane();
        root.addRow(0, summaryUI.rootNode());
        root.addRow(1, switchJourney);

        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }
}