package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 * Represents a profile, which contains the Pareto frontier for all stations for a given
 * destination and travel date.
 *
 * @param timeTable the timetable corresponding to the profile
 * @param date the date corresponding to the profile
 * @param arrStationId the index of the arrival station corresponding to the profile
 * @param stationFront the table of Pareto frontiers for all stations, containing at a given
 *                     index the frontier of the station with the same index
 */
public record Profile(
        TimeTable timeTable,
        LocalDate date,
        int arrStationId,
        List<ParetoFront> stationFront) {

    /**
     * Constructs a profile with the given parameters.
     *
     * @param timeTable the timetable corresponding to the profile
     * @param date the date corresponding to the profile
     * @param arrStationId the index of the arrival station
     * @param stationFront the table of Pareto frontiers for all stations
     */
    public Profile {
        stationFront = List.copyOf(stationFront);
    }

    /**
     * Returns the connections corresponding to the profile.
     *
     * @return the connections for the date of this profile
     */
    public Connections connections() {
        return timeTable.connectionsFor(date);
    }

    /**
     * Returns the trips corresponding to the profile.
     *
     * @return the trips for the date of this profile
     */
    public Trips trips() {
        return timeTable.tripsFor(date);
    }

    /**
     * Returns the Pareto frontier for the station with the given index.
     *
     * @param stationId the index of the station
     * @return the Pareto frontier for the station
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public ParetoFront forStation(int stationId) throws IndexOutOfBoundsException {
        return stationFront.get(stationId);
    }

    /**
     * Builder for Profile.
     * Represents an augmented profile (with Pareto frontiers for both stations and trips)
     * under construction.
     */
    public static final class Builder {
        private final TimeTable timeTable;
        private final LocalDate date;
        private final int arrStationId;
        private List<ParetoFront.Builder> stationFrontBuilders = new ArrayList<>();
        private List<ParetoFront.Builder> tripFrontBuilders = new ArrayList<>();

        /**
         * Constructs a profile builder for the given timetable, date, and arrival station.
         *
         * @param timeTable the timetable
         * @param date the date
         * @param arrStationId the index of the arrival station
         */
        public Builder(TimeTable timeTable, LocalDate date, int arrStationId) {
            this.timeTable = timeTable;
            this.date = date;
            this.arrStationId = arrStationId;

            this.stationFrontBuilders = new ArrayList<>();
            this.tripFrontBuilders = new ArrayList<>();
        }

        /**
         * Returns the Pareto frontier builder for the station with the given index.
         *
         * @param stationId the index of the station
         * @return the Pareto frontier builder for the station, or null if none has been set
         * @throws IndexOutOfBoundsException if the index is invalid
         */
        public ParetoFront.Builder forStation(int stationId) throws IndexOutOfBoundsException {
            return stationFrontBuilders.get(stationId);
        }

        /**
         * Associates the given Pareto frontier builder with the station of the given index.
         *
         * @param stationId the index of the station
         * @param builder the Pareto frontier builder
         * @throws IndexOutOfBoundsException if the index is invalid
         */
        public void setForStation(int stationId, ParetoFront.Builder builder) throws IndexOutOfBoundsException {
            stationFrontBuilders.set(stationId, builder);

        }

        /**
         * Returns the Pareto frontier builder for the trip with the given index.
         *
         * @param tripId the index of the trip
         * @return the Pareto frontier builder for the trip, or null if none has been set
         * @throws IndexOutOfBoundsException if the index is invalid
         */
        public ParetoFront.Builder forTrip(int tripId) throws IndexOutOfBoundsException {
            return tripFrontBuilders.get(tripId);
        }

        /**
         * Associates the given Pareto frontier builder with the trip of the given index.
         *
         * @param tripId the index of the trip
         * @param builder the Pareto frontier builder
         * @throws IndexOutOfBoundsException if the index is invalid
         */
        public void setForTrip(int tripId, ParetoFront.Builder builder) throws IndexOutOfBoundsException {
            tripFrontBuilders.set(tripId, builder);
        }

        /**
         * Builds and returns the simple profile (without Pareto frontiers for trips).
         *
         * @return the built profile
         */
        public Profile build() {
            ParetoFront[] stationFronts = new ParetoFront[stationFrontBuilders.size()];
            for (int i = 0; i < stationFrontBuilders.size(); i++) {
                ParetoFront.Builder builder = stationFrontBuilders.get(i);
                stationFronts[i] = (builder != null)
                        ? builder.build()
                        : ParetoFront.EMPTY;
            }

            return new Profile(timeTable, date, arrStationId, List.of(stationFronts));
        }
    }
}
