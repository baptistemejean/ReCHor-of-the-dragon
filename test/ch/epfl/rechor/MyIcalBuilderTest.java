package ch.epfl.rechor;

import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyIcalConverter;
import ch.epfl.rechor.journey.Stop;
import ch.epfl.rechor.journey.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

public class MyIcalBuilderTest {
    private static Journey exampleJourney() {
        var s1 = new Stop("Ecublens VD, EPFL", null, 6.566141, 46.522196);
        var s2 = new Stop("Renens VD, gare", null, 6.578519, 46.537619);
        var s3 = new Stop("Renens VD", "4", 6.578935, 46.537042);
        var s4 = new Stop("Lausanne", "5", 6.629092, 46.516792);
        var s5 = new Stop("Lausanne", "1", 6.629092, 46.516792);
        var s6 = new Stop("Romont FR", "2", 6.911811, 46.693508);

        var d = LocalDate.of(2025, Month.FEBRUARY, 18);
        var l1 = new Journey.Leg.Transport(
                s1,
                d.atTime(16, 13),
                s2,
                d.atTime(16, 19),
                List.of(),
                Vehicle.METRO,
                "m1",
                "Renens VD, gare");

        var l2 = new Journey.Leg.Foot(s2, d.atTime(16, 19), s3, d.atTime(16, 22));

        var l3 = new Journey.Leg.Transport(
                s3,
                d.atTime(16, 26),
                s4,
                d.atTime(16, 33),
                List.of(),
                Vehicle.TRAIN,
                "R4",
                "Bex");

        var l4 = new Journey.Leg.Foot(s4, d.atTime(16, 33), s5, d.atTime(16, 38));

        var l5 = new Journey.Leg.Transport(
                s5,
                d.atTime(16, 40),
                s6,
                d.atTime(17, 13),
                List.of(),
                Vehicle.TRAIN,
                "IR15",
                "Luzern");

        return new Journey(List.of(l1, l2, l3, l4, l5));
    }

    @Test
    void testIcalBuilder () {
        IcalBuilder builder = new IcalBuilder().add(IcalBuilder.Name.SUMMARY,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit.Vestibulumcursusfringilla mattis. Maecenas efficitur vehicula accumsan. In justo libero, maximus ut interdum quis, maximus sed orci. Maecenas porttitor consectetur enim eget posuere. In vestibulum aliquet metus eu efficitur. Mauris rutrum diam nec odio cursus, ac porttitor massa finibus.")
                .begin(IcalBuilder.Component.VEVENT)
                .begin(IcalBuilder.Component.VCALENDAR)
                .add(IcalBuilder.Name.DTSTART, LocalDate.of(2025, Month.FEBRUARY, 22).atTime(12, 34))
                .end()
                .end();

        System.out.println(builder.build());
    }

    @Test
    void testJourneyIcalConverter () {
        System.out.println(JourneyIcalConverter.toIcalendar(exampleJourney()));
    }
}
