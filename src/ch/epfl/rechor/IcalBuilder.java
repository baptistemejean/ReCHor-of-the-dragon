package ch.epfl.rechor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;

/**
 * A builder class for creating iCalendar (RFC 5545) format strings.
 * <p>
 * This class provides a fluent API for constructing well-formed iCalendar data,
 * handling proper formatting requirements such as line folding, component nesting,
 * and date-time formatting according to the iCalendar specification.
 * <p>
 * Example usage:
 * <pre>{@code
 * IcalBuilder builder = new IcalBuilder()
 *     .begin(IcalBuilder.Component.VCALENDAR)
 *     .add(IcalBuilder.Name.VERSION, "2.0")
 *     .add(IcalBuilder.Name.PRODID, "-//Example Inc//Example Calendar//EN")
 *     .begin(IcalBuilder.Component.VEVENT)
 *     .add(IcalBuilder.Name.DTSTART, LocalDateTime.now())
 *     .add(IcalBuilder.Name.SUMMARY, "Example Event")
 *     .end()
 *     .end();
 *
 * String icalData = builder.build();
 * }</pre>
 */
public final class IcalBuilder {
    /** The standard carriage return and line feed sequence for iCalendar format. */
    public static final String CRLF = "\r\n";

    /** StringBuilder used to construct the iCalendar content. */
    private final StringBuilder stringBuilder;

    /** Tracks the currently started components to ensure proper nesting. */
    private final ArrayList<Component> startedComponents;

    /**
     * Creates a new empty iCalendar builder.
     */
    public IcalBuilder() {
        startedComponents = new ArrayList<>();
        stringBuilder = new StringBuilder();
    }

    /**
     * Folds a line according to iCalendar specification (RFC 5545, section 3.1).
     * <p>
     * Lines longer than 75 characters are split, with continuation lines
     * beginning with a space character.
     *
     * @param value the string to fold
     * @return the folded string
     */
    private static String fold(String value) {
        String folded = value;

        // Fold the string if it is bigger than 75 chars
        if (value.length() > 75) {
            folded = value.substring(0, 75) + CRLF + fold(" " + value.substring(75));

        }

        return folded;
    }

    /**
     * Formats a LocalDateTime object into the iCalendar date-time format.
     * <p>
     * The format used is "YYYYMMDDThhmmss" (e.g., "20250402T143000").
     *
     * @param dateTime the date-time to format
     * @return the formatted date-time string
     */
    private static String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendValue(
                        ChronoField.YEAR,
                        4
                )
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .appendLiteral("T")
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .toFormatter();
        return formatter.format(dateTime);
    }

    /**
     * Adds a property with the specified name and string value to the iCalendar.
     * <p>
     * The property line will be folded if it exceeds 75 characters.
     *
     * @param name the property name
     * @param value the property value
     * @return this builder for method chaining
     */
    public IcalBuilder add(Name name, String value) {
        String folded = fold(name.toString() + ":" + value);
        addLine(folded);
        return this;
    }

    /**
     * Adds a date-time property with the specified name to the iCalendar.
     * <p>
     * The date-time will be formatted according to iCalendar specification.
     *
     * @param name the property name
     * @param dateTime the date-time value
     * @return this builder for method chaining
     */
    public IcalBuilder add(Name name, LocalDateTime dateTime) {
        addLine(name, formatDateTime(dateTime));
        return this;
    }

    /**
     * Begins a new iCalendar component.
     * <p>
     * Components can be nested according to the iCalendar specification.
     * For example, VEVENT components must be nested within a VCALENDAR component.
     *
     * @param component the component to begin
     * @return this builder for method chaining
     */
    public IcalBuilder begin(Component component) {
        startedComponents.add(component);
        addLine(Name.BEGIN, component.toString());
        return this;
    }

    /**
     * Ends the most recently started component.
     *
     * @return this builder for method chaining
     * @throws IllegalArgumentException if there are no started components to end
     */
    public IcalBuilder end() {
        Preconditions.checkArgument(!startedComponents.isEmpty());
        addLine(Name.END, startedComponents.getLast().toString());
        startedComponents.removeLast();
        return this;
    }

    /**
     * Builds the complete iCalendar string.
     *
     * @return the constructed iCalendar string
     * @throws IllegalArgumentException if there are unclosed components
     */
    public String build() {
        Preconditions.checkArgument(startedComponents.isEmpty());

        return stringBuilder.toString();
    }

    /**
     * Adds a property line with the specified name and value to the iCalendar.
     *
     * @param name the property name
     * @param value the property value
     */
    private void addLine(Name name, String value) {
        stringBuilder.append(name).append(":").append(value).append(CRLF);
    }

    /**
     * Adds a pre-formatted line to the iCalendar.
     *
     * @param value the line to add
     */
    private void addLine(String value) {
        stringBuilder.append(value).append(CRLF);
    }

    /**
     * Enumeration of iCalendar component types supported by this builder.
     */
    public enum Component {
        /** Calendar component that acts as a container for events. */
        VCALENDAR,

        /** Event component that represents a scheduled event. */
        VEVENT
    }

    /**
     * Enumeration of iCalendar property names supported by this builder.
     */
    public enum Name {
        /** Marks the beginning of a component. */
        BEGIN,

        /** Marks the end of a component. */
        END,

        /** Identifier for the product that created the calendar. */
        PRODID,

        /** iCalendar specification version. */
        VERSION,

        /** Unique identifier for the calendar component. */
        UID,

        /** Date and time when the calendar component was created. */
        DTSTAMP,

        /** Start date and time of the event. */
        DTSTART,

        /** End date and time of the event. */
        DTEND,

        /** Short summary or subject of the event. */
        SUMMARY,

        /** Detailed description of the event. */
        DESCRIPTION
    }
}
