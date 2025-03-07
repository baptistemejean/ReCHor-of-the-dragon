package ch.epfl.rechor.timetable;

public interface Trips extends Indexed{
    public abstract int routeId(int id) throws IndexOutOfBoundsException;

    public abstract String destination(int id) throws IndexOutOfBoundsException;
}
