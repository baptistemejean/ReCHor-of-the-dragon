package ch.epfl.rechor.timetable;

public interface Stations extends Indexed {
    String name(int id) throws IndexOutOfBoundsException;

    double longitude(int id) throws IndexOutOfBoundsException;

    double latitude(int id) throws IndexOutOfBoundsException;
}
