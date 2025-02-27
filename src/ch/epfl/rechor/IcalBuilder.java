package ch.epfl.rechor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;

public final class IcalBuilder {
    public static final String CRLF = "\r\n";

    private StringBuilder stringBuilder;

    private ArrayList<Component> startedComponents;

    public enum Component {
        VCALENDAR,
        VEVENT
    }

    public enum Name {
        BEGIN, END, PRODID, VERSION, UID, DTSTAMP, DTSTART, DTEND, SUMMARY, DESCRIPTION
    }

    public IcalBuilder () {
        startedComponents = new ArrayList<>();
        stringBuilder = new StringBuilder();
    }

    public IcalBuilder add(Name name, String value) {
        String folded = fold(name.toString() + ":" + value);
//        System.out.println(folded);
        addLine(folded);
        return this;
    }

    public IcalBuilder add(Name name, LocalDateTime dateTime) {
        addLine(name, formatDateTime(dateTime));
        return this;
    }

    public IcalBuilder begin(Component component) {
        startedComponents.add(component);
        addLine(Name.BEGIN, component.toString());
        return this;
    }

    public IcalBuilder end() {
        if (startedComponents.isEmpty()) {
            throw new IllegalArgumentException("No component left");
        }
        addLine(Name.END, startedComponents.getLast().toString());
        startedComponents.removeLast();
        return this;
    }

    public String build() {
        if (!startedComponents.isEmpty()) {
            throw new IllegalArgumentException("A component has begun but has never ended");
        }

        return stringBuilder.toString();
    }

    private void addLine(Name name, String value) {
        stringBuilder.append(name).append(":").append(value).append(CRLF);
    }

    private void addLine(String value) {
        stringBuilder.append(value).append(CRLF);
    }

    private static String fold (String value) {
        String folded = value;

        // Fold the string if it is bigger than 75 chars
        if (value.length() > 75) {
//            String subs = value.substring(0, 75);
//            if (subs.contains("\n")) {
//                folded = subs.split("\n", 2)[0] + "\n" + fold(" " + subs.split("\n", 2)[1]);
//            } else {
//
//            }
            folded = value.substring(0, 75) + CRLF + fold(" " + value.substring(75));

        }

        return folded;
    }

    private static String formatDateTime (LocalDateTime dateTime) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .appendLiteral("T")
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .toFormatter();
        return formatter.format(dateTime);
    }
}
