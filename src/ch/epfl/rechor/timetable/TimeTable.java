package ch.epfl.rechor.timetable;

import java.time.LocalDate;

public interface TimeTable {
    public abstract Stations stations();

    public abstract StationAliases stationAliases();

    public abstract Platforms platforms();

    public abstract Routes routes();

    public abstract Transfers transfers();

    public abstract Trips tripsFor(LocalDate date);

    public abstract Connections connectionsFor(LocalDate date);

    public default boolean isStationId(int stopId){
        return stopId < stations().size();
    };

    public default boolean isPlatformId(int stopId){
        return stopId >= stations().size();
    }

    public default int stationId(int stopId){
        return isStationId(stopId) ? stopId : stopId - stations().size();
    }

    public default String platformName(int stopId){
        return isPlatformId(stopId) ? platforms().name(stationId(stopId)) : null;
    }
}
