package ch.epfl.rechor.journey;

import ch.epfl.rechor.Json;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A utility class for converting Journey objects to GeoJSON format.
 * The GeoJSON represents the journey's route as a LineString.
 */
public final class JourneyGeoJsonConverter {

    private JourneyGeoJsonConverter() {
        throw new UnsupportedOperationException("JourneyGeoJsonConverter is a utility class and cannot be instantiated");
    }

    /**
     * Converts a Journey object to a GeoJSON LineString representation.
     *
     * @param journey The journey to convert
     * @return A Json object representing the journey as a GeoJSON LineString
     */
    public static Json toGeoJson(Journey journey) {
        List<Json> coordinates = new ArrayList<>();
        Stop previousStop = null;

        // Process all legs in the journey
        for (Journey.Leg leg : journey.legs()) {
            // Add departure stop if it's not the same as the previous stop
            Stop depStop = leg.depStop();
            if (previousStop == null || areCoordinatesDifferent(previousStop, depStop)) {
                coordinates.add(stopToJsonCoordinates(depStop));
                previousStop = depStop;
            }

            // Add intermediate stops
            for (Journey.Leg.IntermediateStop intermediateStop : leg.intermediateStops()) {
                Stop stop = intermediateStop.stop();
                if (areCoordinatesDifferent(previousStop, stop)) {
                    coordinates.add(stopToJsonCoordinates(stop));
                    previousStop = stop;
                }
            }

            // Add arrival stop if it's not the same as the previous stop
            Stop arrStop = leg.arrStop();
            if (areCoordinatesDifferent(previousStop, arrStop)) {
                coordinates.add(stopToJsonCoordinates(arrStop));
                previousStop = arrStop;
            }
        }

        return new Json.JObject(Map.of(
                "type", new Json.JString("LineString"),
                "coordinates", new Json.JArray(coordinates)
        ));
    }

    /**
     * Converts a Stop to a JSON array of coordinates [longitude, latitude].
     * Both coordinates are rounded to 5 decimal places.
     *
     * @param stop The stop to convert
     * @return A JSON array of coordinates
     */
    private static Json.JArray stopToJsonCoordinates(Stop stop) {
        double roundedLon = roundToFiveDecimalPlaces(stop.longitude());
        double roundedLat = roundToFiveDecimalPlaces(stop.latitude());

        return new Json.JArray(List.of(
                new Json.JNumber(roundedLon),
                new Json.JNumber(roundedLat)
        ));
    }

    /**
     * Rounds a double value to 5 decimal places.
     *
     * @param value The value to round
     * @return The rounded value
     */
    private static double roundToFiveDecimalPlaces(double value) {
        return BigDecimal.valueOf(value)
                .setScale(5, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Checks if two stops have the same coordinates.
     *
     * @param stop1 The first stop
     * @param stop2 The second stop
     * @return true if both stops have the same coordinates, false otherwise
     */
    private static boolean areCoordinatesDifferent(Stop stop1, Stop stop2) {
        return roundToFiveDecimalPlaces(stop1.longitude()) !=
               roundToFiveDecimalPlaces(stop2.longitude()) ||
               roundToFiveDecimalPlaces(stop1.latitude()) !=
               roundToFiveDecimalPlaces(stop2.latitude());
    }
}
