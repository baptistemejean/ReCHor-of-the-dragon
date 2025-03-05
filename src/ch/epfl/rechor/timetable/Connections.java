package ch.epfl.rechor.timetable;

public interface Connections extends Indexed{
    public abstract int depStopId(int id) throws IndexOutOfBoundsException;

    public abstract int depMins(int id) throws IndexOutOfBoundsException;

    public abstract int arrStopId(int id) throws IndexOutOfBoundsException;

    public abstract int arrMins(int id) throws IndexOutOfBoundsException;

    public abstract int tripId(int id) throws IndexOutOfBoundsException;

    public abstract int tripPos(int id) throws IndexOutOfBoundsException;

    public abstract int nextConnectionId(int id) throws IndexOutOfBoundsException;
}
