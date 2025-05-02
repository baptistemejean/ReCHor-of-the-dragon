package ch.epfl.rechor.journey;

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

        // Connections are by default sorted in decreasing order
        for (int i = 0; i < connections.size(); i++) {
            ParetoFront.Builder builder = new ParetoFront.Builder();
            int connectionArrStopId = connections.arrStopId(i);
            int depStopId = connections.depStopId(i);
            int arrStopId = connections.arrStopId(i);
            int tripId = connections.tripId(i);
            int depTime = connections.depMins(i);
            int arrTime = connections.arrMins(i);

            // Case 1: Walking from connection arrival stop to destination
            int transferDepStationId = timeTable.stationId(connectionArrStopId);

            // Find the right transfer from the transfer buffer
            int transferRange = timeTable.transfers().arrivingAt(arrStationId);
            for (int j = PackedRange.startInclusive(transferRange);
                 j < PackedRange.endExclusive(transferRange); ++j) {
                if (timeTable.transfers().depStationId(j) == transferDepStationId) {
                    builder.add(connections.arrMins(i) + timeTable.transfers().minutes(j), 0, 0);
                }
            }

            // builder.add();
            // Case 2: Continue with the next connection of the same trip

            // Case 3: Switch the vehicle at the end of the connection


        }

        return new Profile(timeTable(), date, arrStationId, paretoFronts);
    }
}
