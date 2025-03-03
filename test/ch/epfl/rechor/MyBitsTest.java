package ch.epfl.rechor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MyBitsTest {
    private int example24() {
        return 5174302; // 0100 1110 1111 0100 0001 1110
    }

    private int example8() {
        return 78; // 0100 1110
    }

    private int example24Error() {
        return 13562910; // 1100 1110 1111 0100 0001 1110
    }

    private int example8Error() {
        return 206; // 1100 1110
    }

    @Test
    void packBitsWorksWithNormal() {
        int packed = Bits32_24_8.pack(example24(), example8());
        assertEquals(1324621390, packed); // 0100 1110 1111 0100 0001 1110 0100 1110
    }

    @Test
    void packBitsWorksWithZeros24() {
        int packed = Bits32_24_8.pack(0, example8());
        assertEquals(example8(), packed);
    }

    @Test
    void packBitsWorksWithZeros8() {
        int packed = Bits32_24_8.pack(example24(), 0);
        assertEquals(1324621312, packed); // 0100 1110 1111 0100 0001 1110 0000 0000
    }

    @Test
    void packThrowsWhen24OutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            int packed = Bits32_24_8.pack(example24Error() << 1, example8());
        });
    }

    @Test
    void packThrowsWhen8OutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> {
            int packed = Bits32_24_8.pack(example24(), example8Error() << 1);
        });
    }

    @Test
    void unpack8Works() {
        int packed = Bits32_24_8.pack(example24(), example8());
        int unpacked8 = Bits32_24_8.unpack8(packed);
        assertEquals(unpacked8, example8());
    }

    @Test
    void unpack24Works() {
        int packed = Bits32_24_8.pack(example24(), example8());
        int unpacked24 = Bits32_24_8.unpack24(packed);
        assertEquals(unpacked24, example24());
    }
}
