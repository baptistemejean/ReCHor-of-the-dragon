package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyStructuredBufferTest {

    private static final List<String> STRING_TABLE = List.of(
            "1",
            "70",
            "Anet",
            "Ins",
            "Lausanne",
            "Losanna",
            "Palézieux"
    );

    @Test
    void bufferedStationsWork() {
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 04 04 b6 ca 14 21 14 1f a1 00 06 04 dc cc 12 21 18 da 03");

        BufferedStations stations = new BufferedStations(STRING_TABLE, ByteBuffer.wrap(bytes));

        assertEquals("Lausanne", stations.name(0));
        assertEquals( "Palézieux", stations.name(1));

        assertEquals(6.629092, round(stations.longitude(0), 6));
        assertEquals(6.837875, round(stations.longitude(1), 6) );

        assertEquals(46.516792, round(stations.latitude(0), 6));
        assertEquals(46.542764, round(stations.latitude(1), 6));

        assertEquals(2, stations.size());

        assertThrows(IndexOutOfBoundsException.class, () -> {
            stations.name(2);
        });

    }

    @Test
    void bufferedStationsAliasesWork() {
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03");

        BufferedStationAliases stations = new BufferedStationAliases(STRING_TABLE, ByteBuffer.wrap(bytes));

        assertEquals("Losanna", stations.alias(0));
        assertEquals( "Lausanne", stations.stationName(0));

        assertEquals("Anet", stations.alias(1));
        assertEquals( "Ins", stations.stationName(1));

        assertEquals(2, stations.size());

        assertThrows(IndexOutOfBoundsException.class, () -> {
            stations.alias(2);
        });

    }

    @Test
    void bufferedPlatformsWork() {
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] b1 = hexFormat.parseHex("00 00 00 00 00 01 00 00 00 00 00 01");
        BufferedPlatforms platforms = new BufferedPlatforms(STRING_TABLE, ByteBuffer.wrap(b1));

        byte[] b2 = hexFormat.parseHex("00 04 04 b6 ca 14 21 14 1f a1 00 06 04 dc cc 12 21 18 da 03");
        BufferedStations stations = new BufferedStations(STRING_TABLE, ByteBuffer.wrap(b2));

        assertEquals("Lausanne", stations.name(platforms.stationId(0)));
        assertEquals( "1", platforms.name(0));

        assertEquals("Lausanne", stations.name(platforms.stationId(1)));
        assertEquals( "70", platforms.name(1));

        assertEquals("Palézieux", stations.name(platforms.stationId(2)));
        assertEquals( "1", platforms.name(2));

        assertEquals(3, platforms.size());

        assertThrows(IndexOutOfBoundsException.class, () -> {
            platforms.name(3);
        });

    }

    private static double round(double a, int places) {
        return Math.round(a * Math.pow(10, places)) / Math.pow(10, places);
    }
}
