package ch.epfl.rechor.timetable;

import java.time.LocalDate;

/**
 * A wrapper implementation of the TimeTable interface that caches trips and connections
 * data for a given date to improve performance for repeated queries.
 */
public class CachedTimeTable implements TimeTable {
    private final TimeTable underlyingTimeTable;
    private LocalDate cachedDate;
    private Trips cachedTrips;
    private Connections cachedConnections;

    /**
     * Constructs a new CachedTimeTable with the specified underlying TimeTable implementation.
     *
     * @param underlyingTimeTable The TimeTable implementation to delegate calls to
     */
    public CachedTimeTable(TimeTable underlyingTimeTable){
        this.underlyingTimeTable = underlyingTimeTable;
        this.cachedDate = null;
        this.cachedTrips = null;
        this.cachedConnections = null;
    }

    /**
     * Returns the stations data from the underlying TimeTable.
     *
     * @return The stations data
     */
    @Override
    public Stations stations() {
        return underlyingTimeTable.stations();
    }

    /**
     * Returns the station aliases data from the underlying TimeTable.
     *
     * @return The station aliases data
     */
    @Override
    public StationAliases stationAliases() {
        return underlyingTimeTable.stationAliases();
    }

    /**
     * Returns the platforms data from the underlying TimeTable.
     *
     * @return The platforms data
     */
    @Override
    public Platforms platforms() {
        return underlyingTimeTable.platforms();
    }

    /**
     * Returns the routes data from the underlying TimeTable.
     *
     * @return The routes data
     */
    @Override
    public Routes routes() {
        return underlyingTimeTable.routes();
    }

    /**
     * Returns the transfers data from the underlying TimeTable.
     *
     * @return The transfers data
     */
    @Override
    public Transfers transfers() {
        return underlyingTimeTable.transfers();
    }

    /**
     * Returns the trips for the specified date, using cached data if available.
     * <p>
     * If the requested date differs from the cached date, the cache is updated
     * with data for the new date.
     * </p>
     *
     * @param date The date for which to retrieve trips
     * @return The trips data for the specified date
     */
    @Override
    public Trips tripsFor(LocalDate date) {
        if (cachedDate == null || !cachedDate.equals(date)) {
            updateCache(date);
        }
        return cachedTrips;
    }

    /**
     * Returns the connections for the specified date, using cached data if available.
     * <p>
     * If the requested date differs from the cached date, the cache is updated
     * with data for the new date.
     * </p>
     *
     * @param date The date for which to retrieve connections
     * @return The connections data for the specified date
     */
    @Override
    public Connections connectionsFor(LocalDate date) {
        if (cachedDate == null || !cachedDate.equals(date)) {
            updateCache(date);
        }
        return cachedConnections;
    }

    /**
     * Updates the cache with trips and connections data for the specified date.
     * <p>
     * This method fetches fresh data from the underlying TimeTable implementation
     * and updates the cached date.
     * </p>
     *
     * @param date The date for which to update the cache
     */
    private void updateCache(LocalDate date) {
        cachedTrips = underlyingTimeTable.tripsFor(date);
        cachedConnections = underlyingTimeTable.connectionsFor(date);
        cachedDate = date;
    }
}