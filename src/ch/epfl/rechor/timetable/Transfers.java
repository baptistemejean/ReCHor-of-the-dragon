package ch.epfl.rechor.timetable;

public interface Transfers extends Indexed{
    public abstract int depStationId(int id) throws IndexOutOfBoundsException;

    public abstract int minutes(int id) throws IndexOutOfBoundsException;

    public abstract int arrivingAt(int stationId) throws IndexOutOfBoundsException;

    public abstract int minutesBetween(int depStationId, int arrStationId) throws IndexOutOfBoundsException;
}

