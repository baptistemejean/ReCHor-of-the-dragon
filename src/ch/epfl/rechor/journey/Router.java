package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record Router (TimeTable timeTable) {
    public Profile profile (LocalDate date, int arrStationId) {
        Connections connections = timeTable().connectionsFor(date);

        Profile.Builder profileBuilder = new Profile.Builder(timeTable, date, arrStationId);

        // Connections are by default sorted in decreasing order
        for (int connId = 0; connId < connections.size(); connId++) {
            ParetoFront.Builder frontBuilder = new ParetoFront.Builder();

            int finalConnId = connId; // For lambda expressions
            int connDepStopId = connections.depStopId(finalConnId);
            int connArrStopId = connections.arrStopId(finalConnId);
            int connTripId = connections.tripId(finalConnId);
            int connDepTime = connections.depMins(finalConnId);
            int connArrTime = connections.arrMins(finalConnId);

            // Case 1: Walking from connection arrival stop to destination (if transfer exists)
            int transferArrRange = timeTable.transfers().arrivingAt(arrStationId);
            for (int transferId = PackedRange.startInclusive(transferArrRange);
                 transferId < PackedRange.endExclusive(transferArrRange); ++transferId) {
                if (timeTable.transfers().depStationId(transferId) == timeTable.stationId(connArrStopId)) {
                    frontBuilder.add(connections.arrMins(finalConnId) + timeTable.transfers().minutes(transferId), 0,
                            finalConnId
                    );
                }
            }

            // Case 2: Continue with the next connection of the same trip
            if (profileBuilder.forTrip(connTripId) != null) {
                frontBuilder.addAll(profileBuilder.forTrip(connTripId));
            }

            // Case 3: Switch the vehicle at the end of the connection
            ParetoFront.Builder stationFronts = profileBuilder.forStation(timeTable.stationId(connArrStopId));
           if (stationFronts != null) {
               stationFronts.forEach((long t) -> {
                   if (PackedCriteria.depMins(t) >= connArrTime) {
                       frontBuilder.add(PackedCriteria.pack(PackedCriteria.arrMins(t), PackedCriteria.changes(t) + 1, finalConnId));
                   }
               });
           }

           if (frontBuilder.isEmpty()) continue;

            // Update trip front
            profileBuilder.setForTrip(connections.tripId(finalConnId),
                    profileBuilder.forTrip(connTripId) != null
                    ? profileBuilder.forTrip(connTripId).addAll(frontBuilder)
                    : frontBuilder);

            ParetoFront.Builder connDepStationBuilder = profileBuilder.forStation(timeTable.stationId(connDepStopId));
            if (connDepStationBuilder != null && connDepStationBuilder.fullyDominates(frontBuilder, connDepTime)) continue;

            // Update station fronts
            int transferDepRange = timeTable.transfers().arrivingAt(timeTable.stationId(connDepStopId));
            for (int transferId = PackedRange.startInclusive(transferDepRange);
                 transferId < PackedRange.endExclusive(transferDepRange); ++transferId) {
                int d = connDepTime - timeTable.transfers().minutes(transferId);
                ParetoFront.Builder stationFrontBuilder = new ParetoFront.Builder();
                frontBuilder.forEach((long t) -> {
                    stationFrontBuilder.add(PackedCriteria.withPayload(
                            PackedCriteria.withDepMins(t, d),
                            Bits32_24_8.pack(finalConnId, connections.tripPos(PackedCriteria.payload(t)) - connections.tripPos(finalConnId)
                            )));
                });

                int transferStationId = timeTable.transfers().depStationId(transferId);
                profileBuilder.setForStation(transferStationId,
                        profileBuilder.forStation(transferStationId) != null
                        ? profileBuilder.forStation(transferStationId).addAll(stationFrontBuilder)
                        : stationFrontBuilder);
            }

        }

        return profileBuilder.build();
    }


}
