package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;

public class MyRouterTest {
    @Test
    public void connectionsTest() throws IOException {
        TimeTable t = FileTimeTable.in(Path.of("timetable"));
        LocalDate date = LocalDate.of(2025, Month.MARCH, 18);

        Connections connections = t.connectionsFor(date);

        int count = 0;

        for (int i = 0; i < connections.size() - 1; i++) {
            if (connections.depMins(i) >= connections.depMins(i + 1)) {
                count++;
            }
        }

        System.out.println(count);
        System.out.println(connections.size());
    }
}
