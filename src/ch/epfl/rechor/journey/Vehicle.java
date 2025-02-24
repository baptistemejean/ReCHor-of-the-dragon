package ch.epfl.rechor.journey;

import java.util.List;

/**
 * Enum representing different types of vehicles used in a journey.
 */
public enum Vehicle {
    TRAM,         // Represents a tramway
    METRO,        // Represents a metro/subway
    TRAIN,        // Represents a train
    BUS,          // Represents a bus
    FERRY,        // Represents a ferry or boat transport
    AERIAL_LIFT,  // Represents an aerial lift (cable car, gondola, etc.)
    FUNICULAR;    // Represents a funicular railway

    /**
     * A constant list containing all vehicle types.
     * Currently, this list is empty.
     */
    public static final List<Vehicle> ALL = List.of();
}