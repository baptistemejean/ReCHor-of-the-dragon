package ch.epfl.rechor.timetable;

import java.time.LocalDate;

public class CachedTimeTable implements TimeTable {
    private final TimeTable underlyingTimeTable;
    private LocalDate cachedDate;
    private Trips cachedTrips;
    private Connections cachedConnections;

    public CachedTimeTable(TimeTable underlyingTimeTable){
        this.underlyingTimeTable = underlyingTimeTable;
        this.cachedDate = null;
        this.cachedTrips = null;
        this.cachedConnections = null;
    }

    @Override
    public Stations stations() {
        return underlyingTimeTable.stations();
    }

    @Override
    public StationAliases stationAliases() {
        return underlyingTimeTable.stationAliases();
    }

    @Override
    public Platforms platforms() {
        return underlyingTimeTable.platforms();
    }

    @Override
    public Routes routes() {
        return underlyingTimeTable.routes();
    }

    @Override
    public Transfers transfers() {
        return underlyingTimeTable.transfers();
    }

    @Override
    public Trips tripsFor(LocalDate date) {
        if (cachedDate == null || !cachedDate.equals(date)) {
            updateCache(date);
        }
        return cachedTrips;
    }

    @Override
    public Connections connectionsFor(LocalDate date) {
        if (cachedDate == null || !cachedDate.equals(date)) {
            updateCache(date);
        }
        return cachedConnections;
    }

    private void updateCache(LocalDate date) {
        cachedTrips = underlyingTimeTable.tripsFor(date);
        cachedConnections = underlyingTimeTable.connectionsFor(date);
        cachedDate = date;
    }
}
