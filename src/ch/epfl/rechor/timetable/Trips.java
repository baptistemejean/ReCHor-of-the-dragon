package ch.epfl.rechor.timetable;

public interface Trips extends Indexed {
    int routeId(int id) throws IndexOutOfBoundsException;

    String destination(int id) throws IndexOutOfBoundsException;
}