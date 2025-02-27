package ch.epfl.rechor.journey;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.IcalBuilder;

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.UUID;

import static ch.epfl.rechor.IcalBuilder.CRLF;

public final class JourneyIcalConverter {
    /**
     * Private constructor to prevent instantiation.
     */
    private JourneyIcalConverter() {
        throw new UnsupportedOperationException("JourneyIcalConverter is a utility class and cannot be instantiated");
    }

    public static String toIcalendar(Journey journey) {
        StringJoiner j = new StringJoiner(" - ");
        for (Journey.Leg leg: journey.legs()) {
            switch (leg) {
                case Journey.Leg.Foot f -> j.add(FormatterFr.formatLeg(f));
                case Journey.Leg.Transport t -> j.add(FormatterFr.formatLeg(t));
            }
        }
        String description = j.toString();

        IcalBuilder builder = new IcalBuilder()
                .begin(IcalBuilder.Component.VCALENDAR)
                .add(IcalBuilder.Name.VERSION, "2.0")
                .add(IcalBuilder.Name.PRODID, "ReCHor")
                .begin(IcalBuilder.Component.VEVENT)
                .add(IcalBuilder.Name.UID, UUID.randomUUID().toString())
                .add(IcalBuilder.Name.DTSTAMP, LocalDateTime.now())
                .add(IcalBuilder.Name.DTSTART, journey.depTime())
                .add(IcalBuilder.Name.DTEND, journey.arrTime())
                .add(IcalBuilder.Name.SUMMARY, journey.depStop().name() + " → " + journey.arrStop().name())
                .add(IcalBuilder.Name.DESCRIPTION, description)
                .end()
                .end();

        return builder.build();
    }
}
