package ch.epfl.rechor.journey;

import ch.epfl.rechor.Preconditions;

import static java.util.Objects.requireNonNull;

/**
 * Represents a stop in a journey, identified by its name, platform name, and geographical coordinates.
 *
 * @param name        The name of the stop, must not be null.
 * @param plateformName The platform name associated with the stop (can be empty or null).
 * @param longitude   The longitude of the stop, must be within [-180, 180].
 * @param latitude    The latitude of the stop, must be within [-90, 90].
 */
public record Stop(String name, String plateformName, double longitude, double latitude) {

    /**
     * Constructor that validates the input parameters.
     *
     * @throws NullPointerException     if the name is null.
     * @throws IllegalArgumentException if longitude is out of range [-180, 180]
     *                                  or latitude is out of range [-90, 90].
     */
    public Stop {
        requireNonNull(name, "Stop name cannot be null");
        Preconditions.checkArgument(longitude > -180 && longitude < 180);
        Preconditions.checkArgument(latitude > -90 && latitude < 90);
    }
}