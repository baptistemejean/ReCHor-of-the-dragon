package ch.epfl.rechor.journey;

import ch.epfl.rechor.Preconditions;

/**
 * A utility class for packing and unpacking optimization criteria into a 64-bit long value.
 * <p>
 * The packed format consists of:
 * <ul>
 *   <li>12 bits for the arrival time (in minutes from a predefined origin).</li>
 *   <li>7 bits for the number of changes.</li>
 *   <li>32 bits for the payload.</li>
 *   <li>Optionally, 12 bits for the departure time (stored as its complement).</li>
 * </ul>
 * If the departure time is present, it is stored in the most significant 12 bits.
 * If absent, its corresponding bits are set to zero, marking its absence.
 * </p>
 *
 * <p>This class is final and cannot be instantiated.</p>
 */
public final class PackedCriteria {
    // Constants for bit sizes
    private static final int TIME_BITS = 12;
    private static final int CHANGES_BITS = 7;
    private static final int PAYLOAD_BITS = 32;
    // Bit shifts for each field
    private static final int CHANGES_SHIFT = PAYLOAD_BITS;
    private static final int ARRIVAL_SHIFT = CHANGES_BITS + PAYLOAD_BITS;
    private static final long DEPARTURE_SHIFT = TIME_BITS + CHANGES_BITS + PAYLOAD_BITS;
    // Bit masks
    private static final long TIME_MASK = 0xFFFL;
    private static final long CHANGES_MASK = 0b1111111L;
    private static final long PAYLOAD_MASK = 0xFFFFFFFFL;
    // Special value representing no departure time
    private static final int NO_DEPARTURE_TIME = 0;
    // Time representation constants
    private static final int TIME_ORIGIN = -240;
    private static final int COMPLEMENT = 4095;
    private static final int MAX_TIME = 3119;

    /**
     * Private constructor to prevent instantiation.
     */
    private PackedCriteria() {
        throw new UnsupportedOperationException(
                "PackedCriteria is a utility class and cannot be instantiated");
    }

    /**
     * Packs optimization criteria into a 64-bit long (without departure time).
     *
     * @param arrMins The arrival time in minutes since midnight.
     * @param changes The number of changes (max 127).
     * @param payload The payload (32-bit).
     * @return A packed long representing the criteria.
     * @throws IllegalArgumentException If the arrival time is out of range or changes exceed 7
     *                                  bits.
     */
    public static long pack(int arrMins, int changes, int payload) {
        Preconditions.checkArgument(arrMins >= TIME_ORIGIN && arrMins <= MAX_TIME + TIME_ORIGIN);
        Preconditions.checkArgument(changes >>> CHANGES_BITS == 0);

        //        int translatedArrMins = arrMins - TIME_ORIGIN;

        return (((long) arrMins - TIME_ORIGIN) << ARRIVAL_SHIFT) |
               (((long) changes) << CHANGES_SHIFT) | (Integer.toUnsignedLong(payload));
    }

    /**
     * Checks if the packed criteria includes a departure time.
     *
     * @param criteria The packed criteria.
     * @return True if a departure time is included, false otherwise.
     */
    public static boolean hasDepMins(long criteria) {
        //        long temp = criteria >>> DEPARTURE_SHIFT;
        return (criteria >>> DEPARTURE_SHIFT) != 0;
    }

    /**
     * Extracts the departure time from the packed criteria.
     *
     * @param criteria The packed criteria.
     * @return The departure time in minutes after midnight.
     * @throws IllegalArgumentException If no departure time is included.
     */
    public static int depMins(long criteria) {
        Preconditions.checkArgument(hasDepMins(criteria));

        return COMPLEMENT - ((int) (criteria >>> DEPARTURE_SHIFT)) + TIME_ORIGIN;
    }

    /**
     * Extracts the arrival time from the packed criteria.
     *
     * @param criteria The packed criteria.
     * @return The arrival time in minutes after midnight.
     */
    public static int arrMins(long criteria) {
        return (int) ((criteria >>> ARRIVAL_SHIFT) & TIME_MASK) + TIME_ORIGIN;
    }

    /**
     * Extracts the number of changes from the packed criteria.
     *
     * @param criteria The packed criteria.
     * @return The number of changes.
     */
    public static int changes(long criteria) {
        return (int) ((criteria >>> PAYLOAD_BITS) & CHANGES_MASK);
    }

    /**
     * Extracts the payload from the packed criteria.
     *
     * @param criteria The packed criteria.
     * @return The payload value.
     */
    public static int payload(long criteria) {
        return (int) ((criteria) & PAYLOAD_MASK);
    }

    /**
     * Checks if one set of criteria dominates or is equal to another.
     *
     * @param criteria1 First criteria.
     * @param criteria2 Second criteria.
     * @return True if {@code criteria1} dominates or is equal to {@code criteria2}.
     * @throws IllegalArgumentException If one has a departure time and the other does not.
     */
    public static boolean dominatesOrIsEqual(long criteria1, long criteria2) {

        if (hasDepMins(criteria1) && hasDepMins(criteria2)) {
            return arrMins(criteria1) <= arrMins(criteria2) &&
                   depMins(criteria1) >= depMins(criteria2) &&
                   changes(criteria1) <= changes(criteria2);
        } else if (!hasDepMins(criteria1) && !hasDepMins(criteria2)) {
            return arrMins(criteria1) <= arrMins(criteria2) &&
                   changes(criteria1) <= changes(criteria2);

        } else {
            throw new IllegalArgumentException("Inconsistent departure time presence.");
        }
    }

    /**
     * Removes the departure time from the packed criteria.
     *
     * @param criteria The packed criteria.
     * @return The criteria without departure minutes.
     */
    public static long withoutDepMins(long criteria) {
        return criteria & ~(TIME_MASK << DEPARTURE_SHIFT);
    }

    /**
     * Adds a departure time to the packed criteria.
     *
     * @param criteria The packed criteria.
     * @param depMins  The departure time in minutes after midnight.
     * @return The updated packed criteria.
     * @throws IllegalArgumentException If the departure time is out of range.
     */
    public static long withDepMins(long criteria, int depMins) {
        Preconditions.checkArgument(depMins >= TIME_ORIGIN && depMins <= MAX_TIME);

        depMins = COMPLEMENT - (depMins - TIME_ORIGIN);

        return criteria | (((long) depMins) << DEPARTURE_SHIFT);
    }

    /**
     * Adds one additional change to the packed criteria.
     *
     * @param criteria The packed criteria.
     * @return The updated packed criteria with one more change.
     */
    public static long withAdditionalChange(long criteria) {
        Preconditions.checkArgument(changes(criteria) != CHANGES_MASK);

        return criteria + (1L << CHANGES_SHIFT);
    }

    /**
     * Sets a new payload value for the packed criteria.
     *
     * @param criteria The packed criteria.
     * @param payload1 The new payload.
     * @return The updated packed criteria with the new payload.
     */
    public static long withPayload(long criteria, int payload1) {
        return (criteria & ~(PAYLOAD_MASK)) | (Integer.toUnsignedLong(payload1));
    }
}
