package ch.epfl.rechor;

import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.Stop;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Utility class for formatting various journey-related data into human-readable French text.
 */
public final class FormatterFr {

    /**
     * Private constructor to prevent instantiation.
     */
    private FormatterFr() {
        throw new UnsupportedOperationException("FormatterFr is a utility class and cannot be instantiated");
    }

    /**
     * Formats a duration into a human-readable French string.
     *
     * @param duration The duration to format.
     * @return A formatted string representing the duration.
     */
    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() - 60 * hours;
        System.out.println(minutes);
        if (hours > 0 ){
            return hours + " h " + minutes + " min";
        } else {
            return minutes + " min";
        }
    }

    /**
     * Formats a LocalDateTime into a time string in French format (e.g., "14h30").
     *
     * @param dateTime The LocalDateTime to format.
     * @return A formatted time string.
     */
    public static String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
//                .appendValue(ChronoField.DAY_OF_MONTH, 2)
//                .appendLiteral("/")
//                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
//                .appendLiteral("/")
//                .appendValue(ChronoField.YEAR, 4)
//                .appendLiteral(" - ")
                .appendValue(ChronoField.HOUR_OF_DAY)
                .appendLiteral('h')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .toFormatter();
        return formatter.format(dateTime);
    }

    /**
     * Formats a platform name based on the stop information.
     *
     * @param stop The stop to format the platform name for.
     * @return A formatted platform name, or an empty string if not applicable.
     */
    public static String formatPlatformName(Stop stop){
        if (isStation(stop)){
            return "";
        }
        return stop.platformName().matches("^\\d.*") ? "voie " + stop.platformName() : "quai " + stop.platformName();
    }

    /**
     * Determines whether a stop is a station (i.e., does not have a platform name).
     *
     * @param stop The stop to check.
     * @return true if the stop is a station, false otherwise.
     */
    public static boolean isStation(Stop stop) {
        return stop.platformName() == null || stop.platformName().isEmpty();
    }

    /**
     * Formats a walking leg of a journey.
     *
     * @param footLeg The foot journey leg.
     * @return A formatted string describing the walking leg.
     */
    public static String formatLeg(Journey.Leg.Foot footLeg) {
        String type = footLeg.isTransfer() ? "changement" : "trajet à pied";
        return type + " (" + formatDuration(footLeg.duration()) + ")";
    }

    /**
     * Formats a transport leg of a journey.
     *
     * @param leg The transport journey leg.
     * @return A formatted string describing the transport leg.
     */
    public static String formatLeg(Journey.Leg.Transport leg) {
        String depTime = formatTime(leg.depTime());
        String depStopName = leg.depStop().name();
        String depPlatform = formatPlatformName(leg.depStop());
        String arrTime = formatTime(leg.arrTime());
        String arrStopName = leg.arrStop().name();
        String arrPlatform = formatPlatformName(leg.arrStop());

        return depTime + " " + depStopName +
                (depPlatform.isEmpty() ? "" : " (" + depPlatform + ")") +
                " → " + arrStopName + " (arr. " + arrTime +
                (arrPlatform.isEmpty() ? "" : " " + arrPlatform) + ")";
    }

    /**
     * Formats the route and destination information for a transport leg.
     *
     * @param transportLeg The transport leg of the journey.
     * @return A formatted string describing the transport route and direction.
     */
    public static String formatRouteDestination(Journey.Leg.Transport transportLeg) {
        return transportLeg.route() + " Direction " + transportLeg.destination();
    }
}
