package ch.epfl.rechor.gui;

import ch.epfl.rechor.journey.Vehicle;
import javafx.scene.image.Image;
import java.util.EnumMap;
import java.util.Map;

/**
 * Provides access to vehicle icons.
 * <p>
 * This class loads icons when first needed and caches them for future use.
 */
public final class VehicleIcons {
    private static final Map<Vehicle, javafx.scene.image.Image> ICON_CACHE = new EnumMap<>(Vehicle.class);

    // Private constructor to prevent instantiation
    private VehicleIcons() {}

    /**
     * Returns the icon for the given vehicle type.
     *
     * @param vehicle The vehicle type.
     * @return The icon for the given vehicle type.
     */
    public static javafx.scene.image.Image iconFor(Vehicle vehicle) {
        return ICON_CACHE.computeIfAbsent(vehicle, v -> new Image(v.name() + ".png"));
    }
}