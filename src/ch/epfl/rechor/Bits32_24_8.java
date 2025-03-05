package ch.epfl.rechor;

/**
 * A utility class for packing and unpacking two values—one of 24 bits and one of 8 bits—
 * into a single 32-bit integer.
 * <p>
 * The most significant 24 bits store the 24-bit value, while the least significant 8 bits
 * store the 8-bit value.
 * </p>
 *
 * <p>This class is final and cannot be instantiated.</p>
 */
public final class Bits32_24_8 {

    /**
     * Private constructor to prevent instantiation.
     */
    private Bits32_24_8() {
        throw new UnsupportedOperationException("Bits32_24_8 is a utility class and cannot be instantiated");
    }

    /**
     * Packs a 24-bit value and an 8-bit value into a single 32-bit integer.
     *
     * @param bits24 The 24-bit value to be stored in the most significant 24 bits.
     * @param bits8 The 8-bit value to be stored in the least significant 8 bits.
     * @return A 32-bit integer combining the 24-bit and 8-bit values.
     * @throws IllegalArgumentException If the 24-bit value exceeds 24 bits or the 8-bit value exceeds 8 bits.
     */
    public static int pack(int bits24, int bits8) {
       Preconditions.checkArgument((bits24 >> 24) == 0);
       Preconditions.checkArgument((bits8 >> 8) == 0);
        return (bits24 << 8) | (bits8 & 0xFF);
    }

    /**
     * Extracts the 24 most significant bits from a 32-bit integer.
     *
     * @param bits32 The 32-bit integer containing the packed values.
     * @return The 24-bit value stored in the most significant bits.
     */
    public static int unpack24(int bits32) {
        return (bits32 >>> 8);
    }

    /**
     * Extracts the 8 least significant bits from a 32-bit integer.
     *
     * @param bits32 The 32-bit integer containing the packed values.
     * @return The 8-bit value stored in the least significant bits.
     */
    public static int unpack8(int bits32) {
        return (bits32 & 0xFF);
    }
}