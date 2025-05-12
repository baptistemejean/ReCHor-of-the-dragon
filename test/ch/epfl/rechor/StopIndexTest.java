package ch.epfl.rechor;

import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StopIndexTest {
    private StopIndex stopIndex;

    @BeforeEach
    void setUp() {
        // Initialize stopIndex with a list of stop names and alternative names
        List<String> stopNames = List.of(
                "Lausanne",
                "Renens VD",
                "Mézières FR, village",
                "Mézières VD, village",
                "Mézery-près-Donneloye, village",
                "Charleville-Mézières",
                "Yverdon-les-Bains"
        );

        Map<String, String> alternativeNames = Map.of(
                "Losanna", "Lausanne",
                "Renan", "Renens VD",
                "Yverdon", "Yverdon-les-Bains"
        );

        stopIndex = new StopIndex(stopNames, alternativeNames);
    }

    @Test
    void testEmptyQuery() {
        List<String> results = stopIndex.stopsMatching("", 10);
        assertTrue(results.isEmpty());

        results = stopIndex.stopsMatching("   ", 10);
        assertTrue(results.isEmpty());
    }

    @Test
    void testExactMatch() {
        List<String> results = stopIndex.stopsMatching("Lausanne", 10);
        assertEquals(1, results.size());
        assertEquals("Lausanne", results.get(0));
    }

    @Test
    void testCaseInsensitiveMatch() {
        List<String> results = stopIndex.stopsMatching("lausanne", 10);
        assertEquals(1, results.size());
        assertEquals("Lausanne", results.get(0));
    }

    @Test
    void testPartialMatch() {
        List<String> results = stopIndex.stopsMatching("laus", 10);
        assertEquals(1, results.size());
        assertEquals("Lausanne", results.get(0));
    }

    @Test
    void testAccentInsensitiveMatch() {
        List<String> results = stopIndex.stopsMatching("mezieres", 10);
        assertEquals(3, results.size());
        // The two Mézières should be first, sorted by relevance
        assertTrue(results.contains("Mézières FR, village"));
        assertTrue(results.contains("Mézières VD, village"));
    }

    @Test
    void testMultiWordQuery() {
        // This should match "Mézières FR, village" and "Mézières VD, village"
        List<String> results = stopIndex.stopsMatching("mez vil", 10);
        assertEquals(4, results.size());
        assertTrue(results.contains("Mézières FR, village"));
        assertTrue(results.contains("Mézières VD, village"));
    }

    @Test
    void testQueryOrderIndependence() {
        // Order of words in query shouldn't matter
        List<String> results1 = stopIndex.stopsMatching("mez vil", 10);
        List<String> results2 = stopIndex.stopsMatching("vil mez", 10);

        assertEquals(results1.size(), results2.size());
        for (String result : results1) {
            assertTrue(results2.contains(result));
        }
    }

    @Test
    void testAlternativeNames() {
        // Check that alternative names resolve to their official names
        List<String> results = stopIndex.stopsMatching("Losanna", 10);
        assertEquals(1, results.size());
        assertEquals("Lausanne", results.get(0));
    }

    @Test
    void testMaxResults() {
        // Should limit results to maxResults
        List<String> results = stopIndex.stopsMatching("mez", 2);
        assertEquals(2, results.size());
    }

    @Test
    void testNoMatch() {
        List<String> results = stopIndex.stopsMatching("nonexistent", 10);
        assertTrue(results.isEmpty());
    }

    @Test
    void testScoring() {
        // Test that scoring works as expected
        // Query "mez vil" should match "Mézières FR, village" and "Mézières VD, village" with the same score,
        // followed by "Mézery-près-Donneloye, village" with a lower score
        List<String> results = stopIndex.stopsMatching("mez vil", 10);
        assertEquals(4, results.size());

        // Either FR or VD can be first since they have the same score
        assertTrue(results.get(0).startsWith("Mézières"));
        assertTrue(results.get(1).startsWith("Mézières"));
        assertEquals("Mézery-près-Donneloye, village", results.get(2));
    }

    @Test
    void testCaseSensitiveQuery() {
        // This should not match "Mézières FR, village" because of the uppercase Z
        List<String> results = stopIndex.stopsMatching("meZ vil", 10);
        assertFalse(results.contains("Mézières FR, village"));
    }

    @Test
    void testAccentedQuery() {
        // This should not match "Mézières FR, village" because é and è are different
        List<String> results = stopIndex.stopsMatching("mèz vil", 10);
        assertFalse(results.contains("Mézières FR, village"));
    }

    @Test
    void testWordBoundaryScoring() {
        // Create a new index with stops that test word boundary scoring
        List<String> specialStopNames = List.of(
                "Lausanne",
                "Lausanne-Gare",
                "Gare de Lausanne",
                "Gare Lausanne Centre"
        );

        StopIndex specialIndex = new StopIndex(specialStopNames, Map.of());

        // Test that "Lausanne" gets a higher score when at the beginning of a word
        List<String> results = specialIndex.stopsMatching("lau", 10);
        assertEquals(4, results.size());

        // "Lausanne" should come first as "lau" is at the beginning of the name
        assertEquals("Lausanne", results.get(0));

        // "Lausanne-Gare" should come second as "lau" is at the beginning but the stop name is longer
        assertEquals("Lausanne-Gare", results.get(1));
    }

    @Test
    void stopIndexWorksWithEveryString () throws IOException {
        TimeTable t = FileTimeTable.in(Path.of("timetable"));
        List<String> strs = new ArrayList<>();

        for (int i = 0; i < t.stations().size(); i ++) {
            strs.add(t.stations().name(i));
        }

        StopIndex specialIndex = new StopIndex(strs, Map.of());

        List<String> results = specialIndex.stopsMatching("Fribourg", 10);

        for (String r: results) {
            System.out.println(r);
        }
    }
}
