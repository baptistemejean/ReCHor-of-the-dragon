package ch.epfl.rechor.timetable;

public interface Platforms extends Indexed {
    public abstract String name(int id) throws IndexOutOfBoundsException;

    public abstract int stationId(int id) throws IndexOutOfBoundsException;
}
