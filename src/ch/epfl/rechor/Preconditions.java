package ch.epfl.rechor;

/**
 * Utility class for checking preconditions in method arguments.
 * This class provides a simple way to enforce conditions on method inputs.
 */
public final class Preconditions {

    /**
     * Private constructor to prevent instantiation.
     */
    private Preconditions() {
    }

    /**
     * Checks whether a given condition is true. If not, it throws an {@link IllegalArgumentException}.
     *
     * @param shouldBeTrue The condition that must be true.
     * @throws IllegalArgumentException if the condition is false.
     */
    public static void checkArgument(boolean shouldBeTrue) {
        if (!shouldBeTrue) {
            throw new IllegalArgumentException();
        }
    }
}