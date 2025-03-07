package ch.epfl.rechor.timetable;

public interface Stations extends Indexed {
    public abstract String name(int id) throws IndexOutOfBoundsException;

    public abstract double longitude(int id) throws IndexOutOfBoundsException;

    public abstract double latitude(int id) throws IndexOutOfBoundsException;
}
