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
        List<ParetoFront> paretoFronts = new ArrayList<>();

        Connections connections = timeTable().connectionsFor(date);
        Trips trips = timeTable().tripsFor(date);

        Profile.Builder profileBuilder = new Profile.Builder(timeTable(), date, arrStationId);

        // Connections are by default sorted in decreasing order
        for (int connId = 0; connId < connections.size(); connId++) {
            ParetoFront.Builder frontBuilder = new ParetoFront.Builder();
            int connDepStopId = connections.depStopId(connId);
            int connArrStopId = connections.arrStopId(connId);
            int connTripId = connections.tripId(connId);
            int connDepTime = connections.depMins(connId);
            int connArrTime = connections.arrMins(connId);

            // Case 1: Walking from connection arrival stop to destination (if transfer exists)
            int transferArrRange = timeTable.transfers().arrivingAt(arrStationId);
            for (int transferId = PackedRange.startInclusive(transferArrRange);
                 transferId < PackedRange.endExclusive(transferArrRange); ++transferId) {
                if (timeTable.transfers().depStationId(transferId) == timeTable.stationId(connArrStopId)) {
                    frontBuilder.add(connections.arrMins(connId) + timeTable.transfers().minutes(transferId), 0,
                            connId
                    );
                }
            }

            // Case 2: Continue with the next connection of the same trip
            frontBuilder.addAll(profileBuilder.forTrip(connTripId));

            // Case 3: Switch the vehicle at the end of the connection
            int finalConnId = connId;
            profileBuilder.forStation(timeTable.stationId(connArrStopId)).forEach((long t) -> {
                if (PackedCriteria.depMins(t) >= connArrTime) {
                    frontBuilder.add(PackedCriteria.withAdditionalChange(PackedCriteria.withPayload(t,
                            finalConnId
                    )));
                }
            });

            // Update trip fronts
            profileBuilder.setForTrip(connections.tripId(connId),frontBuilder);

            // Update station fronts
            int transferDepRange = timeTable.transfers().arrivingAt(timeTable.stationId(connDepStopId));
            for (int transferId = PackedRange.startInclusive(transferDepRange);
                 transferId < PackedRange.endExclusive(transferDepRange); ++transferId) {
                int d = connDepTime - timeTable.transfers().minutes(transferId);
                ParetoFront.Builder stationFrontBuilder = new ParetoFront.Builder();
                frontBuilder.forEach((long t) -> {
                    stationFrontBuilder.add(PackedCriteria.withPayload(PackedCriteria.withDepMins(t, d), Bits32_24_8.pack(finalConnId, Bits32_24_8.unpack8(PackedCriteria.payload(t)))));
                });
                profileBuilder.setForStation(timeTable.transfers().depStationId(transferId), stationFrontBuilder);
            }

        }

        return profileBuilder.build();
    }
}
