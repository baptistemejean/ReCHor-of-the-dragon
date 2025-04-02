package ch.epfl.rechor.journey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Represents a journey composed of multiple legs.
 *
 * @param legs The list of legs that form the journey.
 */
public record Journey(List<Leg> legs) {

    /**
     * Constructs a Journey while ensuring validity constraints:
     * <ul>
     *     <li>The journey must contain at least one leg.</li>
     *     <li>Walking and transport legs must alternate. </li>
     *     <li>A leg cannot start before the previous one ends.</li>
     *     <li>The departure stop of a leg must match the arrival stop of the previous one.</li>
     * </ul>
     *
     * @throws IllegalArgumentException If any of the constraints are violated.
     */
    public Journey {
        if (legs.isEmpty()) {
            throw new IllegalArgumentException("The journey must contain at least one leg");
        }

        legs = List.copyOf(legs);

        for (int i = 1; i < legs.size(); i++) {
            Leg previous = legs.get(i - 1);
            Leg current = legs.get(i);

            if ((previous instanceof Leg.Foot && current instanceof Leg.Foot) ||
                (previous instanceof Leg.Transport && current instanceof Leg.Transport)) {
                throw new IllegalArgumentException("Walking and transport legs must alternate");
            }

            LocalDateTime previousArrival = previous.arrTime();
            LocalDateTime currentDeparture = current.depTime();

            if (currentDeparture.isBefore(previousArrival)) {
                throw new IllegalArgumentException("A leg cannot start before the previous one ends");
            }

            Stop previousArrivalStop = previous.arrStop();
            Stop currentDepartureStop = current.depStop();

            if (!previousArrivalStop.equals(currentDepartureStop)) {
                throw new IllegalArgumentException(
                        "The departure stop of a leg must be the arrival stop of the previous one");
            }
        }
    }

    /**
     * Returns the departure stop of the journey.
     */
    public Stop depStop() {
        return legs.getFirst().depStop();
    }

    /**
     * Returns the arrival stop of the journey.
     */
    public Stop arrStop() {
        return legs.getLast().arrStop();
    }

    /**
     * Returns the departure time of the journey.
     */
    public LocalDateTime depTime() {
        return legs.getFirst().depTime();
    }

    /**
     * Returns the arrival time of the journey.
     */
    public LocalDateTime arrTime() {
        return legs.getLast().arrTime();
    }

    /**
     * Returns the total duration of the journey.
     */
    public Duration duration() {
        return Duration.between(depTime(), arrTime());
    }

    /**
     * Sealed interface representing a leg of a journey.
     */
    public sealed interface Leg permits Leg.Transport, Leg.Foot {
        Stop depStop();

        LocalDateTime depTime();

        Stop arrStop();

        LocalDateTime arrTime();

        List<IntermediateStop> intermediateStops();

        /**
         * Returns the duration of the leg.
         */
        default Duration duration() {
            return Duration.between(depTime(), arrTime());
        }

        /**
         * Represents an intermediate stop during a journey leg.
         *
         * @param stop    The intermediate stop.
         * @param arrTime The arrival time at the stop.
         * @param depTime The departure time from the stop.
         */
        record IntermediateStop(Stop stop, LocalDateTime arrTime, LocalDateTime depTime) {
            public IntermediateStop {
                Objects.requireNonNull(stop, "Stop cannot be null");
                if (depTime.isBefore(arrTime)) {
                    throw new IllegalArgumentException(
                            "Departure time cannot be before arrival time");
                }
            }
        }

        /**
         * Represents a transport leg of the journey.
         *
         * @param depStop           The departure stop.
         * @param depTime           The departure time.
         * @param arrStop           The arrival stop.
         * @param arrTime           The arrival time.
         * @param intermediateStops The list of intermediate stops.
         * @param vehicle           The vehicle used for the leg.
         * @param route             The route name.
         * @param destination       The final destination of the vehicle.
         */
        record Transport(Stop depStop, LocalDateTime depTime, Stop arrStop, LocalDateTime arrTime,
                         List<IntermediateStop> intermediateStops, Vehicle vehicle, String route,
                         String destination) implements Leg {
            public Transport {
                Objects.requireNonNull(depStop, "Departure stop cannot be null");
                Objects.requireNonNull(depTime, "Departure time cannot be null");
                Objects.requireNonNull(arrStop, "Arrival stop cannot be null");
                Objects.requireNonNull(arrTime, "Arrival time cannot be null");
                Objects.requireNonNull(vehicle, "Vehicle cannot be null");
                Objects.requireNonNull(route, "Route cannot be null");
                Objects.requireNonNull(destination, "Destination cannot be null");
                if (arrTime.isBefore(depTime)) {
                    throw new IllegalArgumentException(
                            "Arrival time cannot be before departure time");
                }
                intermediateStops = List.copyOf(intermediateStops);
            }
        }

        /**
         * Represents a walking leg of the journey.
         *
         * @param depStop The departure stop.
         * @param depTime The departure time.
         * @param arrStop The arrival stop.
         * @param arrTime The arrival time.
         */
        record Foot(Stop depStop, LocalDateTime depTime, Stop arrStop,
                    LocalDateTime arrTime) implements Leg {
            public Foot {
                Objects.requireNonNull(depStop, "Departure stop cannot be null");
                Objects.requireNonNull(depTime, "Departure time cannot be null");
                Objects.requireNonNull(arrStop, "Arrival stop cannot be null");
                Objects.requireNonNull(arrTime, "Arrival time cannot be null");
                if (arrTime.isBefore(depTime)) {
                    throw new IllegalArgumentException(
                            "Arrival time cannot be before departure time");
                }
            }

            @Override
            public List<IntermediateStop> intermediateStops() {
                return List.of();
            }

            /**
             * Determines if the walking leg is a transfer within the same station.
             *
             * @return true if the leg is a transfer, false otherwise.
             */
            public boolean isTransfer() {
                return depStop.name().equals(arrStop.name());
            }
        }
    }
}