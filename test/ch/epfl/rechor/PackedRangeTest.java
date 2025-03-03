package ch.epfl.rechor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PackedRangeTest {
    private int startIntervalExample() {
        return 339970; // 0000 0101 0011 0000 0000 0010
    }

    private int endIntervalExample() {
        return 340000; // 0000 0101 0011 0000 0010 0000
    }

    private int startIntervalErrorExample() {
        return 8388608; // 1000 0000 0000 0000 0000 0000 // 2^23
    }

    private int endIntervalErrorExample() {
        return startIntervalExample() - 2; // 0000 0101 0011 0000 0000 0000
    }

    private int lengthExample() {
        return 30; // 0001 1110
    }

    @Test
    void packWorksWithNormal() {
        int packed = PackedRange.pack(startIntervalExample(), endIntervalExample());
        assertEquals(87032350, packed); // 0000 0101 0011 0000 0000 0010 0001 1110
    }

    @Test
    void packWorksWithZeros() {
        int packed = PackedRange.pack(startIntervalExample(), startIntervalExample());
        assertEquals(87032320, packed); // 0000 0101 0011 0000 0000 0010 0000 0000
    }

    @Test
    void unpackLengthWorks() {
        int packed = PackedRange.pack(startIntervalExample(), endIntervalExample());
        int length = PackedRange.length(packed);
        assertEquals(lengthExample(), length);
    }

    @Test
    void unpackStartWorks() {
        int packed = PackedRange.pack(startIntervalExample(), endIntervalExample());
        int start = PackedRange.startInclusive(packed);
        assertEquals(startIntervalExample(), start);
    }

    @Test
    void unpackEndWorks() {
        int packed = PackedRange.pack(startIntervalExample(), endIntervalExample());
        int end = PackedRange.endExclusive(packed);
        assertEquals(endIntervalExample(), end);
    }

    @Test
    void packThrowsWhenStartOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            int packed = PackedRange.pack(startIntervalErrorExample() << 1, startIntervalErrorExample() << 1 + 1);
        });
    }

    @Test
    void packThrowsWhenLengthOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            int packed = PackedRange.pack(startIntervalExample(), startIntervalErrorExample());
        });
    }
}
