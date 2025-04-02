package ch.epfl.rechor.timetable;

public interface Platforms extends Indexed {
    String name(int id) throws IndexOutOfBoundsException;

    int stationId(int id) throws IndexOutOfBoundsException;
}
