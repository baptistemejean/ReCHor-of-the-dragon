package ch.epfl.rechor;

/**
 * A utility class for packing and unpacking integer intervals into a single 32-bit integer.
 * <p>
 * The 24 most significant bits store the inclusive start of the interval, while the 8 least
 * significant bits store the length of the interval.
 * </p>
 *
 * <p>This class is final and cannot be instantiated.</p>
 */
public final class PackedRange {
    /**
     * Private constructor to prevent instantiation.
     */
    private PackedRange() {
        throw new UnsupportedOperationException(
                "PackedRange is a utility class and cannot be instantiated");
    }

    /**
     * Packs an integer interval into a 32-bit integer.
     * <p>
     * The 24 most significant bits store {@code startInclusive}, and the 8 least significant bits
     * store the length of the interval.
     * </p>
     *
     * @param startInclusive The inclusive start of the interval (must fit within 24 bits).
     * @param endExclusive   The exclusive end of the interval.
     * @return A 32-bit integer representing the packed interval.
     * @throws IllegalArgumentException If {@code startInclusive} exceeds 24 bits or if the interval
     *                                  length is not in the range [0, 255].
     */
    public static int pack(int startInclusive, int endExclusive) {
        Preconditions.checkArgument((startInclusive >> 24) == 0);

        int length = endExclusive - startInclusive;
        Preconditions.checkArgument((length >= 0) && (length <= 255));

        return Bits32_24_8.pack(startInclusive, length);
    }

    /**
     * Extracts the length of the interval from a packed 32-bit integer.
     *
     * @param interval The packed 32-bit integer representing an interval.
     * @return The length of the interval.
     */
    public static int length(int interval) {
        return Bits32_24_8.unpack8(interval);
    }

    /**
     * Extracts the inclusive start of the interval from a packed 32-bit integer.
     *
     * @param interval The packed 32-bit integer representing an interval.
     * @return The inclusive start of the interval.
     */
    public static int startInclusive(int interval) {
        return Bits32_24_8.unpack24(interval);
    }

    /**
     * Computes the exclusive end of the interval from a packed 32-bit integer.
     *
     * @param interval The packed 32-bit integer representing an interval.
     * @return The exclusive end of the interval.
     */
    public static int endExclusive(int interval) {
        return startInclusive(interval) + length(interval);
    }
}
