package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

public class MyJourneyExtractorTest {
    private Profile readProfile(
            TimeTable timeTable, LocalDate date, int arrStationId
    ) throws IOException {
        Path path = Path.of("profile_" + date + "_" + arrStationId + ".txt");
        try (BufferedReader r = Files.newBufferedReader(path)) {
            Profile.Builder profileB = new Profile.Builder(timeTable, date, arrStationId);
            int stationId = -1;
            String line;
            while ((line = r.readLine()) != null) {
                stationId += 1;
                if (line.isEmpty()) continue;
                ParetoFront.Builder frontB = new ParetoFront.Builder();
                for (String t : line.split(","))
                    frontB.add(Long.parseLong(t, 16));

                profileB.setForStation(stationId, frontB);
            }
            return profileB.build();
        }
    }

    @Test
    public void printExtractedProfile() throws IOException {
        TimeTable t = FileTimeTable.in(Path.of("timetable"));
        LocalDate date = LocalDate.of(2025, Month.MARCH, 18);
        Profile p = readProfile(t, date, 11486);
        Router r = new Router(t);
        Profile gen = r.profile(date, 11486);
        List<Journey> js = JourneyExtractor.journeys(p, 7872);
        String j = JourneyIcalConverter.toIcalendar(js.get(32));
        System.out.println(j);
//        comp(gen.stationFront(), p.stationFront());
//        assertEquals(p.stationFront(), gen.stationFront());
    }

}
