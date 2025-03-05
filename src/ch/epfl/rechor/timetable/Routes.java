package ch.epfl.rechor.timetable;

import ch.epfl.rechor.journey.Vehicle;

public interface Routes extends Indexed{
    public abstract Vehicle vehicle(int id) throws IndexOutOfBoundsException;

    public abstract String name(int id) throws IndexOutOfBoundsException;
}
