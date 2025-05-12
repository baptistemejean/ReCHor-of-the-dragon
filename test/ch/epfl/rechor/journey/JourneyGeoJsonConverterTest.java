package ch.epfl.rechor.journey;

import ch.epfl.rechor.Json;
import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JourneyGeoJsonConverterTest {
    private static int stationId(Stations stations, String name) {
        for (var i = 0; i < stations.size(); i += 1)
            if (stations.name(i).equals(name)) return i;
        throw new NoSuchElementException();
    }

    @Test
    void testToGeoJsonWithSingleLeg() {
        // Create stops
        Stop lausanne = new Stop("Lausanne", "1", 6.62909, 46.51679);
        Stop palezieux = new Stop("Palézieux", "2", 6.83787, 46.54276);

        // Create journey with a single transport leg
        LocalDateTime depTime = LocalDateTime.of(2023, 5, 3, 12, 0);
        LocalDateTime arrTime = LocalDateTime.of(2023, 5, 3, 12, 30);

        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                lausanne, depTime, palezieux, arrTime,
                List.of(), // No intermediate stops
                Vehicle.TRAIN, "IR90", "Brig"
        );

        Journey journey = new Journey(List.of(leg));

        // Convert to GeoJSON
        Json geoJson = JourneyGeoJsonConverter.toGeoJson(journey);

        // Check structure
        assertTrue(geoJson instanceof Json.JObject);
        Json.JObject jsonObject = (Json.JObject) geoJson;

        // Check type
        Json typeValue = jsonObject.elements().get("type");
        assertTrue(typeValue instanceof Json.JString);
        assertEquals("LineString", ((Json.JString) typeValue).value());

        // Check coordinates
        Json coordinatesValue = jsonObject.elements().get("coordinates");
        assertTrue(coordinatesValue instanceof Json.JArray);
        Json.JArray coordinates = (Json.JArray) coordinatesValue;

        // Should have 2 points
        assertEquals(2, coordinates.elements().size());

        // Check first point (Lausanne)
        Json firstPoint = coordinates.elements().get(0);
        assertTrue(firstPoint instanceof Json.JArray);
        Json.JArray firstPointArray = (Json.JArray) firstPoint;
        assertEquals(2, firstPointArray.elements().size());
        assertEquals(6.62909, ((Json.JNumber) firstPointArray.elements().get(0)).value(), 0.00001);
        assertEquals(46.51679, ((Json.JNumber) firstPointArray.elements().get(1)).value(), 0.00001);

        // Check second point (Palézieux)
        Json secondPoint = coordinates.elements().get(1);
        assertTrue(secondPoint instanceof Json.JArray);
        Json.JArray secondPointArray = (Json.JArray) secondPoint;
        assertEquals(2, secondPointArray.elements().size());
        assertEquals(6.83787, ((Json.JNumber) secondPointArray.elements().get(0)).value(), 0.00001);
        assertEquals(46.54276, ((Json.JNumber) secondPointArray.elements().get(1)).value(), 0.00001);
    }

    @Test
    void testToGeoJsonWithIntermediateStops() {
        // Create stops
        Stop lausanne = new Stop("Lausanne", "1", 6.62909, 46.51679);
        Stop romont = new Stop("Romont", "3", 6.91181, 46.69351);
        Stop fribourg = new Stop("Fribourg", "4", 7.15105, 46.80315);

        // Create intermediate stop
        LocalDateTime intermediateArrTime = LocalDateTime.of(2023, 5, 3, 12, 15);
        LocalDateTime intermediateDepTime = LocalDateTime.of(2023, 5, 3, 12, 17);
        Journey.Leg.IntermediateStop romontStop = new Journey.Leg.IntermediateStop(
                romont, intermediateArrTime, intermediateDepTime);

        // Create journey leg
        LocalDateTime depTime = LocalDateTime.of(2023, 5, 3, 12, 0);
        LocalDateTime arrTime = LocalDateTime.of(2023, 5, 3, 12, 30);

        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                lausanne, depTime, fribourg, arrTime,
                List.of(romontStop),
                Vehicle.TRAIN, "IR90", "Brig"
        );

        Journey journey = new Journey(List.of(leg));

        // Convert to GeoJSON
        Json geoJson = JourneyGeoJsonConverter.toGeoJson(journey);

        // Extract coordinates
        Json.JObject jsonObject = (Json.JObject) geoJson;
        Json.JArray coordinates = (Json.JArray) jsonObject.elements().get("coordinates");

        // Should have 3 points
        assertEquals(3, coordinates.elements().size());

        // Check intermediate point (Romont)
        Json intermediatePoint = coordinates.elements().get(1);
        Json.JArray intermediatePointArray = (Json.JArray) intermediatePoint;
        assertEquals(6.91181, ((Json.JNumber) intermediatePointArray.elements().get(0)).value(), 0.00001);
        assertEquals(46.69351, ((Json.JNumber) intermediatePointArray.elements().get(1)).value(), 0.00001);
    }

    @Test
    void testToGeoJsonWithMultipleLegs() {
        // Create stops
        Stop lausanne = new Stop("Lausanne", "1", 6.62909, 46.51679);
        Stop lausanneGare = new Stop("Lausanne, Gare", "1", 6.62919, 46.51689); // Slightly different coords
        Stop renens = new Stop("Renens VD", "1", 6.57806, 46.53423);

        // Create journey with transport + foot legs
        LocalDateTime time1 = LocalDateTime.of(2023, 5, 3, 12, 0);
        LocalDateTime time2 = LocalDateTime.of(2023, 5, 3, 12, 10);
        LocalDateTime time3 = LocalDateTime.of(2023, 5, 3, 12, 15);
        LocalDateTime time4 = LocalDateTime.of(2023, 5, 3, 12, 30);

        Journey.Leg.Transport legTransport = new Journey.Leg.Transport(
                lausanne, time1, lausanneGare, time2,
                List.of(), // No intermediate stops
                Vehicle.TRAIN, "IR90", "Brig"
        );

        Journey.Leg.Foot legFoot = new Journey.Leg.Foot(
                lausanneGare, time3, renens, time4
        );

        Journey journey = new Journey(List.of(legTransport, legFoot));

        // Convert to GeoJSON
        Json geoJson = JourneyGeoJsonConverter.toGeoJson(journey);

        // Extract coordinates
        Json.JObject jsonObject = (Json.JObject) geoJson;
        Json.JArray coordinates = (Json.JArray) jsonObject.elements().get("coordinates");

        // Should have 3 points
        assertEquals(3, coordinates.elements().size());
    }

    @Test
    void testCoordinateRounding() {
        // Create stops with many decimal places
        Stop stop1 = new Stop("Precision1", "1", 6.6290912345, 46.5167987654);
        Stop stop2 = new Stop("Precision2", "2", 6.8378712345, 46.5427687654);

        // Create journey
        LocalDateTime time1 = LocalDateTime.of(2023, 5, 3, 12, 0);
        LocalDateTime time2 = LocalDateTime.of(2023, 5, 3, 12, 30);

        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                stop1, time1, stop2, time2,
                List.of(),
                Vehicle.TRAIN, "IR90", "Brig"
        );

        Journey journey = new Journey(List.of(leg));

        // Convert to GeoJSON
        Json geoJson = JourneyGeoJsonConverter.toGeoJson(journey);

        // Extract coordinates
        Json.JObject jsonObject = (Json.JObject) geoJson;
        Json.JArray coordinates = (Json.JArray) jsonObject.elements().get("coordinates");

        // Check that coordinates are rounded to 5 decimal places
        Json.JArray point1 = (Json.JArray) coordinates.elements().get(0);
        double longitude1 = ((Json.JNumber) point1.elements().get(0)).value();
        double latitude1 = ((Json.JNumber) point1.elements().get(1)).value();

        // Check if we have exactly 5 decimal places
        assertEquals(6.62909, longitude1, 0.000001);
        assertEquals(46.51680, latitude1, 0.000001);
    }

    @Test
    void testDuplicatePointsFiltering() {
        // Create journey with a duplicated intermediate stop
        Stop lausanne = new Stop("Lausanne", "1", 6.62909, 46.51679);
        Stop duplicate1 = new Stop("Duplicate1", "2", 6.62909, 46.51679); // Same coordinates as Lausanne
        Stop duplicate2 = new Stop("Duplicate2", "3", 6.62909, 46.51679); // Same coordinates as Lausanne
        Stop fribourg = new Stop("Fribourg", "4", 7.15105, 46.80315);

        // Create intermediate stops
        Journey.Leg.IntermediateStop stop1 = new Journey.Leg.IntermediateStop(
                duplicate1,
                LocalDateTime.of(2023, 5, 3, 12, 5),
                LocalDateTime.of(2023, 5, 3, 12, 6));

        Journey.Leg.IntermediateStop stop2 = new Journey.Leg.IntermediateStop(
                duplicate2,
                LocalDateTime.of(2023, 5, 3, 12, 10),
                LocalDateTime.of(2023, 5, 3, 12, 11));

        // Create journey leg
        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                lausanne,
                LocalDateTime.of(2023, 5, 3, 12, 0),
                fribourg,
                LocalDateTime.of(2023, 5, 3, 12, 30),
                List.of(stop1, stop2),
                Vehicle.TRAIN, "IR90", "Brig"
        );

        Journey journey = new Journey(List.of(leg));

        // Convert to GeoJSON
        Json geoJson = JourneyGeoJsonConverter.toGeoJson(journey);

        // Extract coordinates
        Json.JObject jsonObject = (Json.JObject) geoJson;
        Json.JArray coordinates = (Json.JArray) jsonObject.elements().get("coordinates");

        // Should have only 2 points since duplicates should be filtered
        assertEquals(2, coordinates.elements().size());
    }

//    @Test
//    void geoJsonWorksWithExtractedJourneys () throws IOException {
//        String expected = "{\"coordinates\":[[6.56614,46.5222],[6.56459,46.52459],[6.56655,46.52775],[6.56997,46.53271],[6.57358,46.53772],[6.57852,46.53762],[6.57893,46.53704],[6.60263,46.52669],[6.62909,46.51679],[6.83787,46.54276],[6.91181,46.69351],[7.053,46.61922],[7.06144,46.60562],[7.05969,46.59468],[7.07325,46.58261]],\"type\":\"LineString\"}";
//
//        TimeTable timeTable =
//                new CachedTimeTable(FileTimeTable.in(Path.of("timetable")));
//        Stations stations = timeTable.stations();
//        LocalDate date = LocalDate.of(2025, Month.MARCH, 18);
//        int depStationId = stationId(stations, "Ecublens VD, EPFL");
//        int arrStationId = stationId(stations, "Gruyères");
//        Router router = new Router(timeTable);
//        Profile profile = router.profile(date, arrStationId);
//        List<Journey> journeys = JourneyExtractor
//                .journeys(profile, depStationId);
//
//        assertEquals(expected, JourneyGeoJsonConverter.toGeoJson(journeys.get(32)).toString());
//    }
}
