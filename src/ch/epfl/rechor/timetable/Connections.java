package ch.epfl.rechor.timetable;

public interface Connections extends Indexed {
    int depStopId(int id) throws IndexOutOfBoundsException;

    int depMins(int id) throws IndexOutOfBoundsException;

    int arrStopId(int id) throws IndexOutOfBoundsException;

    int arrMins(int id) throws IndexOutOfBoundsException;

    int tripId(int id) throws IndexOutOfBoundsException;

    int tripPos(int id) throws IndexOutOfBoundsException;

    int nextConnectionId(int id) throws IndexOutOfBoundsException;
}
