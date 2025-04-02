package ch.epfl.rechor.timetable;

public interface StationAliases extends Indexed {
    String alias(int id) throws IndexOutOfBoundsException;

    String stationName(int id) throws IndexOutOfBoundsException;
}
