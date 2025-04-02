package ch.epfl.rechor.timetable;

public interface Transfers extends Indexed {
    int depStationId(int id) throws IndexOutOfBoundsException;

    int minutes(int id) throws IndexOutOfBoundsException;

    int arrivingAt(int stationId) throws IndexOutOfBoundsException;

    int minutesBetween(
            int depStationId, int arrStationId
    ) throws IndexOutOfBoundsException;
}

