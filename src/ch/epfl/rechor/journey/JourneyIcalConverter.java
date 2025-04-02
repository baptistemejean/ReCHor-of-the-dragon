package ch.epfl.rechor.journey;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.IcalBuilder;

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.UUID;

import static ch.epfl.rechor.IcalBuilder.Name.*;

/**
 * A utility class for converting {@link Journey} objects to iCalendar format.
 * <p>
 * This class provides functionality to transform journey information into a standard iCalendar
 * format string that can be used in calendar applications. The conversion includes details such as
 * departure and arrival times, locations, and formatted descriptions of each leg of the journey.
 * <p>
 * This class cannot be instantiated as it only contains static utility methods.
 *
 * @see Journey
 * @see IcalBuilder
 * @see FormatterFr
 */
public final class JourneyIcalConverter {
    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException always, as this utility class cannot be instantiated
     */
    private JourneyIcalConverter() {
        throw new UnsupportedOperationException(
                "JourneyIcalConverter is a utility class and cannot be instantiated");
    }

    /**
     * Converts a journey to iCalendar format.
     * <p>
     * Creates an iCalendar event with:
     * <ul>
     *   <li>A unique identifier</li>
     *   <li>Start time set to the journey's departure time</li>
     *   <li>End time set to the journey's arrival time</li>
     *   <li>Summary containing departure and arrival stop names</li>
     *   <li>Description containing formatted details of each journey leg</li>
     * </ul>
     *
     * @param journey the journey to convert to iCalendar format
     * @return a string containing the journey information in iCalendar format
     * @throws NullPointerException if journey is null
     * @see Journey
     * @see FormatterFr#formatLeg(Journey.Leg.Foot)
     * @see FormatterFr#formatLeg(Journey.Leg.Transport)
     */
    public static String toIcalendar(Journey journey) {
        StringJoiner j = new StringJoiner("\\n");
        for (Journey.Leg leg : journey.legs()) {
            switch (leg) {
                case Journey.Leg.Foot f -> j.add(FormatterFr.formatLeg(f));
                case Journey.Leg.Transport t -> j.add(FormatterFr.formatLeg(t));
            }
        }
        String description = j.toString();

        IcalBuilder builder = new IcalBuilder().begin(IcalBuilder.Component.VCALENDAR)
                .add(VERSION, "2.0")
                .add(PRODID, "ReCHor")
                .begin(IcalBuilder.Component.VEVENT)
                .add(UID, UUID.randomUUID().toString())
                .add(DTSTAMP, LocalDateTime.now())
                .add(DTSTART, journey.depTime())
                .add(DTEND, journey.arrTime())
                .add(SUMMARY, journey.depStop().name() + " → " + journey.arrStop().name())
                .add(DESCRIPTION, description)
                .end()
                .end();

        return builder.build();
    }
}