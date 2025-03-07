package ch.epfl.rechor.timetable;

public interface StationAliases extends Indexed{
    public abstract String alias(int id) throws IndexOutOfBoundsException;

    public abstract String stationName (int id) throws IndexOutOfBoundsException;
}
