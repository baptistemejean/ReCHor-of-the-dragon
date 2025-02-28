package ch.epfl.rechor;

/**
 * A utility class for packing and unpacking integer intervals into a single 32-bit integer.
 * <p>
 * The 24 most significant bits store the inclusive start of the interval,
 * while the 8 least significant bits store the length of the interval.
 * </p>
 *
 * <p>This class is final and cannot be instantiated.</p>
 */
public final class PackedRange {

    /**
     * Packs an integer interval into a 32-bit integer.
     * <p>
     * The 24 most significant bits store {@code startInclusive}, and
     * the 8 least significant bits store the length of the interval.
     * </p>
     *
     * @param startInclusive The inclusive start of the interval (must fit within 24 bits).
     * @param endExclusive The exclusive end of the interval.
     * @return A 32-bit integer representing the packed interval.
     * @throws IllegalArgumentException If {@code startInclusive} exceeds 24 bits or
     *                                  if the interval length is not in the range [0, 255].
     */
    public static int pack(int startInclusive, int endExclusive) {
        int length = endExclusive - startInclusive;
        if ((startInclusive >> 24) != 0) {
            throw new IllegalArgumentException("startInclusive exceeds 24 bits");
        }
        if ((length < 0) || (length > 255)) {
            throw new IllegalArgumentException("Interval length must be between 0 and 255");
        }

        return (startInclusive << 8) | (length & 0xFF);
    }

    /**
     * Extracts the length of the interval from a packed 32-bit integer.
     *
     * @param interval The packed 32-bit integer representing an interval.
     * @return The length of the interval.
     */
    public static int length(int interval) {
        return interval & 0xFF;
    }

    /**
     * Extracts the inclusive start of the interval from a packed 32-bit integer.
     *
     * @param interval The packed 32-bit integer representing an interval.
     * @return The inclusive start of the interval.
     */
    public static int startInclusive(int interval) {
        return interval >> 8;
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
