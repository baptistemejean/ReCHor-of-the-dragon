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
        return stopId <= 33275;
    };

    public default boolean isPlatformId(int stopId){
        return stopId > 33275;
    }

    public default int stationId(int stopId){
        return stopId;
    }

    public default String platformName(int stopId){
        if (isPlatformId(stopId)){
            return platforms().name(stopId);
        }
        return null;
    }
}
