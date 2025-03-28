package ch.epfl.rechor.journey;

import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for extracting optimal journeys from a travel profile.
 * This class provides methods to extract and process journey information
 * based on Pareto frontier criteria.
 */
public final class JourneyExtractor {
    // Private constructor to prevent instantiation of utility class
    private JourneyExtractor() {}

    /**
     * Extracts all optimal journeys for a given profile and departure station.
     *
     * @param profile Profile containing Pareto frontier and connection information
     * @param depStationId Departure station identifier
     * @return Sorted list of journeys, ordered by departure and arrival times
     */
    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> extractedJourneys = new ArrayList<>();
        ParetoFront paretoFront = profile.forStation(depStationId);

        // Extract journeys for each criteria in the Pareto frontier
        paretoFront.forEach(criteria -> {
            Journey journey = new Journey(extractLegsFromCriteria(profile, depStationId, criteria));
            extractedJourneys.add(journey);
        });

        // Sort journeys by departure time, then by arrival time
        extractedJourneys.sort(Comparator
                .comparing(Journey::depTime)
                .thenComparing(Journey::arrTime)
        );

        return extractedJourneys;
    }

    /**
     * Extracts journey legs from a specific Pareto frontier criteria.
     *
     * @param profile Travel profile containing timetable and connection data
     * @param depStationId Initial departure station identifier
     * @param initialCriteria Pareto frontier criteria for journey extraction
     * @return List of journey legs representing the optimal route
     */
    private static List<Journey.Leg> extractLegsFromCriteria(
            Profile profile,
            int depStationId,
            long initialCriteria
    ) {
        TimeTable timeTable = profile.timeTable();
        Connections connections = profile.connections();

        List<Journey.Leg> legs = new ArrayList<>();
        int currentStopId = depStationId;
        int arrMins = PackedCriteria.arrMins(initialCriteria);
        int currentArrMins = 0;

        int payload = PackedCriteria.payload(initialCriteria);
        int connectionId = payload >> 8;
        int connectionDepStopId = connections.depStopId(connectionId);

        // Initial foot leg if needed
        if (timeTable.stationId(connectionDepStopId) != currentStopId) {
            addFootLeg(connections.depMins(connectionId), false, currentStopId, connectionDepStopId, timeTable, profile, legs);
        }

        for (int remainingChanges = PackedCriteria.changes(initialCriteria); remainingChanges >= 0; remainingChanges--) {
            ParetoFront paretoFront = profile.forStation(timeTable.stationId(currentStopId));
            long currentCriteria = paretoFront.get(arrMins, remainingChanges);

            payload = PackedCriteria.payload(currentCriteria);
            connectionId = payload >> 8;
            int numStops = payload & 0xFF;
            connectionDepStopId = connections.depStopId(connectionId);

            // Add foot leg if previous leg was a transport leg
            if (!legs.isEmpty() && legs.getLast() instanceof Journey.Leg.Transport) {
                addFootLeg(currentArrMins, true, currentStopId, connectionDepStopId, timeTable, profile, legs);
            }

            // Add transport leg
            connectionId = addTransportLeg(profile, timeTable, connectionId, numStops, legs);

            // Update loop variables
            currentStopId = connections.arrStopId(connectionId);
            currentArrMins = connections.arrMins(connectionId);
        }

        // Final foot leg if needed
        if (timeTable.stationId(currentStopId) != profile.arrStationId()) {
            addFootLeg(currentArrMins, true, currentStopId, profile.arrStationId(), timeTable, profile, legs);
        }

        return legs;
    }

    /**
     * Adds a transport leg from connection and profile information.
     *
     * @param profile Travel profile
     * @param timeTable Timetable containing station and route details
     * @param connectionId Current connection identifier
     * @param numStops Number of intermediate stops
     * @param legs List of journey legs to modify
     * @return connectionId after changes
     */
    private static int addTransportLeg(
            Profile profile,
            TimeTable timeTable,
            int connectionId,
            int numStops,
            List<Journey.Leg> legs
    ) {
        Connections connections = profile.connections();
        Stations stations = timeTable.stations();
        Routes routes = timeTable.routes();

        int tripId = connections.tripId(connectionId);
        int depStopId = connections.depStopId(connectionId);

        int initialDepMins = connections.depMins(connectionId);

        List<Journey.Leg.IntermediateStop> intermediateStops = new ArrayList<>();

        for (int i = 0; i < numStops; i++) {
            LocalDateTime intermediateStopArrTime = dateTimeFromMins(
                    connections.arrMins(connectionId), profile.date()
            );
            connectionId = connections.nextConnectionId(connectionId);

            intermediateStops.add(new Journey.Leg.IntermediateStop(
                    stopFromStopId(connections.depStopId(connectionId), timeTable, stations),
                    intermediateStopArrTime,
                    dateTimeFromMins(connections.depMins(connectionId), profile.date())
            ));
        }

        int arrStopId = connections.arrStopId(connectionId);

        legs.add(new Journey.Leg.Transport(
                stopFromStopId(depStopId, timeTable, stations),
                dateTimeFromMins(initialDepMins, profile.date()),
                stopFromStopId(arrStopId, timeTable, stations),
                dateTimeFromMins(connections.arrMins(connectionId), profile.date()),
                intermediateStops,
                routes.vehicle(profile.trips().routeId(tripId)),
                routes.name(profile.trips().routeId(tripId)),
                profile.trips().destination(tripId)
        ));

        return connectionId;
    }

    /**
     * Creates a Stop object from a stop identifier.
     *
     * @param stopId Stop identifier
     * @param timeTable Timetable for station information
     * @param stations Station data
     * @return Stop with detailed station information
     */
    private static Stop stopFromStopId(int stopId, TimeTable timeTable, Stations stations) {
        int stationId = timeTable.stationId(stopId);
        return new Stop(
                stations.name(stationId),
                timeTable.platformName(stopId),
                stations.longitude(stationId),
                stations.latitude(stationId)
        );
    }

    /**
     * Converts minutes to a LocalDateTime based on a given date.
     *
     * @param mins Minutes since midnight
     * @param date Travel date
     * @return Corresponding LocalDateTime
     */
    private static LocalDateTime dateTimeFromMins(int mins, LocalDate date) {
        return LocalDateTime.of(date, LocalTime.of(mins / 60, mins % 60));
    }

    /**
     * Adds a foot leg between two stops when a transfer is possible.
     *
     * @param mins Minutes (could be departure or arrival)
     * @param isDepMins Whether the minutes given above describe departure or arrival times
     * @param depStopId Departure stop identifier
     * @param arrStopId Arrival stop identifier
     * @param timeTable Timetable for transfer information
     * @param profile Travel profile
     * @param legs List of journey legs to modify
     */
    private static void addFootLeg(
            int mins,
            boolean isDepMins,
            int depStopId,
            int arrStopId,
            TimeTable timeTable,
            Profile profile,
            List<Journey.Leg> legs
    ) {
        int depStationId = timeTable.stationId(depStopId);
        int arrStationId = timeTable.stationId(arrStopId);

        int transferRange = timeTable.transfers().arrivingAt(arrStationId);
        for (int i = PackedRange.startInclusive(transferRange); i < PackedRange.endExclusive(transferRange); ++i) {
            if (timeTable.transfers().depStationId(i) == depStationId) {
                int depMins;
                int arrMins;
                if (isDepMins) {
                    depMins = mins;
                    arrMins = mins + timeTable.transfers().minutes(i);
                } else {
                    depMins = mins - timeTable.transfers().minutes(i);
                    arrMins = mins;
                }

                legs.add(new Journey.Leg.Foot(
                        stopFromStopId(depStopId, timeTable, timeTable.stations()),
                        dateTimeFromMins(depMins, profile.date()),
                        stopFromStopId(arrStopId, timeTable, timeTable.stations()),
                        dateTimeFromMins(arrMins, profile.date())
                ));
                break;
            }
        }
    }
}