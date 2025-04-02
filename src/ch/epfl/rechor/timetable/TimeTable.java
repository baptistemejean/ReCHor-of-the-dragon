package ch.epfl.rechor.timetable;

import java.time.LocalDate;

public interface TimeTable {
    Stations stations();

    StationAliases stationAliases();

    Platforms platforms();

    Routes routes();

    Transfers transfers();

    Trips tripsFor(LocalDate date);

    Connections connectionsFor(LocalDate date);

    default boolean isStationId(int stopId) {
        return stopId < stations().size();
    }

    default boolean isPlatformId(int stopId) {
        return stopId >= stations().size();
    }

    default int stationId(int stopId) {
        //        return isStationId(stopId) ? stopId : stopId - stations().size();
        return isStationId(stopId) ? stopId : platforms().stationId(stopId - stations().size());
    }

    default String platformName(int stopId) {
        return isPlatformId(stopId) ? platforms().name(stopId - stations().size()) : null;
    }
}
