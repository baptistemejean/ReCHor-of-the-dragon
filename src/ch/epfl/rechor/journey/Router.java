package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;

import java.time.LocalDate;

/**
 * Router class responsible for computing optimal journeys through a public transport network.
 * This implementation uses a profile-based approach to find Pareto-optimal journeys
 * considering arrival time and number of transfers.
 */
public record Router(TimeTable timeTable) {

    /**
     * Computes a journey profile for trips arriving at the specified station on the given date.
     * The profile contains Pareto-optimal journeys with respect to arrival time and number of changes.
     *
     * @param date The date for which to compute the profile
     * @param arrStationId The ID of the destination station
     * @return A profile containing Pareto-optimal journeys to the destination
     */
    public Profile profile(LocalDate date, int arrStationId) {
        Connections connections = timeTable().connectionsFor(date);
        Profile.Builder profileBuilder = new Profile.Builder(timeTable, date, arrStationId);

        // Process connections in decreasing order (connections are pre-sorted)
        for (int connId = 0; connId < connections.size(); connId++) {
            // Initialize a new Pareto front for this connection
            ParetoFront.Builder frontBuilder = new ParetoFront.Builder();

            // Get connection details
            int connDepStopId = connections.depStopId(connId);
            int connArrStopId = connections.arrStopId(connId);
            int connTripId = connections.tripId(connId);
            int connDepTime = connections.depMins(connId);
            int connArrTime = connections.arrMins(connId);

            // Process the connection using three strategies
            processDirectWalkToDestination(arrStationId, connections, connId, connArrStopId, frontBuilder);
            processContinuingWithSameTrip(profileBuilder, connTripId, frontBuilder);
            processTransferAtArrival(profileBuilder, connArrStopId, connArrTime, connId, frontBuilder);

            // Skip if no valid journeys were found
            if (frontBuilder.isEmpty()) continue;

            // Update trip front
            updateTripFront(profileBuilder, connTripId, frontBuilder);

            // Skip if fully dominated by existing journeys
            ParetoFront.Builder connDepStationBuilder = profileBuilder.forStation(timeTable.stationId(connDepStopId));
            if (connDepStationBuilder != null && connDepStationBuilder.fullyDominates(frontBuilder, connDepTime)) continue;

            // Update station fronts with possible transfers
            updateStationFronts(profileBuilder, connections, connId, connDepStopId, connDepTime, frontBuilder);
        }

        return profileBuilder.build();
    }

    /**
     * Processes the possibility of walking directly from the connection's arrival stop to the destination.
     * Adds a journey to the front if a transfer exists.
     *
     * @param arrStationId The destination station ID
     * @param connections The connections data
     * @param connId The current connection ID
     * @param connArrStopId The arrival stop ID of the current connection
     * @param frontBuilder The Pareto front builder to update
     */
    private void processDirectWalkToDestination(int arrStationId, Connections connections,
                                                int connId, int connArrStopId,
                                                ParetoFront.Builder frontBuilder) {
        int transferArrRange = timeTable.transfers().arrivingAt(arrStationId);

        for (int transferId = PackedRange.startInclusive(transferArrRange);
             transferId < PackedRange.endExclusive(transferArrRange);
             ++transferId) {

            if (timeTable.transfers().depStationId(transferId) == timeTable.stationId(connArrStopId)) {
                // Add journey: connection + walk to destination
                int arrivalTime = connections.arrMins(connId) + timeTable.transfers().minutes(transferId);
                frontBuilder.add(arrivalTime, 0, connId);
            }
        }
    }

    /**
     * Processes the possibility of continuing with the next connection of the same trip.
     * Incorporates existing journeys for this trip into the current front.
     *
     * @param profileBuilder The profile builder containing existing journeys
     * @param connTripId The trip ID of the current connection
     * @param frontBuilder The Pareto front builder to update
     */
    private void processContinuingWithSameTrip(Profile.Builder profileBuilder, int connTripId,
                                               ParetoFront.Builder frontBuilder) {
        if (profileBuilder.forTrip(connTripId) != null) {
            frontBuilder.addAll(profileBuilder.forTrip(connTripId));
        }
    }

    /**
     * Processes the possibility of transferring at the arrival station of the connection.
     * Adds journeys with transfers to the front if they are not dominated.
     *
     * @param profileBuilder The profile builder containing existing journeys
     * @param connArrStopId The arrival stop ID of the current connection
     * @param connArrTime The arrival time of the current connection
     * @param connId The current connection ID
     * @param frontBuilder The Pareto front builder to update
     */
    private void processTransferAtArrival(Profile.Builder profileBuilder, int connArrStopId,
                                          int connArrTime, int connId,
                                          ParetoFront.Builder frontBuilder) {
        ParetoFront.Builder stationFronts = profileBuilder.forStation(timeTable.stationId(connArrStopId));

        if (stationFronts != null) {
            stationFronts.forEach((long t) -> {
                if (PackedCriteria.depMins(t) >= connArrTime) {
                    frontBuilder.add(PackedCriteria.pack(
                            PackedCriteria.arrMins(t),
                            PackedCriteria.changes(t) + 1,
                            connId
                    ));
                }
            });
        }
    }

    /**
     * Updates the trip front with the new journeys found.
     *
     * @param profileBuilder The profile builder to update
     * @param connTripId The trip ID of the current connection
     * @param frontBuilder The Pareto front builder with new journeys
     */
    private void updateTripFront(Profile.Builder profileBuilder, int connTripId, ParetoFront.Builder frontBuilder) {
        profileBuilder.setForTrip(
                connTripId,
                profileBuilder.forTrip(connTripId) != null
                ? profileBuilder.forTrip(connTripId).addAll(frontBuilder)
                : frontBuilder
        );
    }

    /**
     * Updates station fronts with journeys that include transfers to reach the departure station.
     *
     * @param profileBuilder The profile builder to update
     * @param connections The connections data
     * @param connId The current connection ID
     * @param connDepStopId The departure stop ID of the current connection
     * @param connDepTime The departure time of the current connection
     * @param frontBuilder The Pareto front builder with new journeys
     */
    private void updateStationFronts(Profile.Builder profileBuilder, Connections connections,
                                     int connId, int connDepStopId, int connDepTime,
                                     ParetoFront.Builder frontBuilder) {
        int transferDepRange = timeTable.transfers().arrivingAt(timeTable.stationId(connDepStopId));

        for (int transferId = PackedRange.startInclusive(transferDepRange);
             transferId < PackedRange.endExclusive(transferDepRange);
             ++transferId) {

            int adjustedDepTime = connDepTime - timeTable.transfers().minutes(transferId);

            ParetoFront.Builder stationFrontBuilder = new ParetoFront.Builder();

            frontBuilder.forEach((long t) -> {
                int tupleConnPosition = connections.tripPos(PackedCriteria.payload(t));
                stationFrontBuilder.add(
                        PackedCriteria.withPayload(
                                PackedCriteria.withDepMins(t, adjustedDepTime),
                                Bits32_24_8.pack(connId, tupleConnPosition - connections.tripPos(connId)
                                )
                        )
                );
            });

            int transferStationId = timeTable.transfers().depStationId(transferId);
            profileBuilder.setForStation(
                    transferStationId,
                    profileBuilder.forStation(transferStationId) != null
                    ? profileBuilder.forStation(transferStationId).addAll(stationFrontBuilder)
                    : stationFrontBuilder
            );
        }
    }
}