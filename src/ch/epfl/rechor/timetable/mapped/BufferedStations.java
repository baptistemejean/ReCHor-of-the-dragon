package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Stations;

import java.nio.ByteBuffer;
import java.util.List;

import static ch.epfl.rechor.timetable.mapped.Structure.field;
import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.*;

/**
 * Represents a buffered implementation of the {@link Stations} interface,
 * storing station data in a structured buffer for efficient access.
 */
public class BufferedStations implements Stations {
    // Constants for field indices
    private final static int NAME_ID = 0;
    private final static int LON_ID = 1;
    private final static int LAT_ID = 2;

    /** Conversion factor for longitude and latitude values. */
    private final static double LON_LAT_CONVERSION = Math.scalb(360, -32);

    // Structure definition
    private static final Structure STRUCTURE = new Structure(
            field(NAME_ID, U16),
            field(LON_ID, S32),
            field(LAT_ID, S32)
    );

    private final StructuredBuffer structuredBuffer;
    private final List<String> stringTable;

    /**
     * Constructs a {@link BufferedStations} instance with the given string table and byte buffer.
     *
     * @param stringTable the list of strings
     * @param buffer the byte buffer containing structured station data
     */
    public BufferedStations(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = stringTable;
        this.structuredBuffer = new StructuredBuffer(STRUCTURE, buffer);
    }

    /**
     * Returns the name of the station with the given index.
     *
     * @param id the station index
     * @return the name of the station
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public String name(int id) throws IndexOutOfBoundsException {
        return stringTable.get(structuredBuffer.getU16(NAME_ID, id));
    }

    /**
     * Returns the longitude of the station with the given index.
     *
     * @param id the station index
     * @return the longitude of the station in degrees
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public double longitude(int id) throws IndexOutOfBoundsException {
        return structuredBuffer.getS32(LON_ID, id) * LON_LAT_CONVERSION;
    }

    /**
     * Returns the latitude of the station with the given index.
     *
     * @param id the station index
     * @return the latitude of the station in degrees
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public double latitude(int id) throws IndexOutOfBoundsException {
        return structuredBuffer.getS32(LAT_ID, id) * LON_LAT_CONVERSION;
    }

    /**
     * Returns the total number of stations stored in the buffer.
     *
     * @return the number of stations
     */
    @Override
    public int size() {
        return structuredBuffer.size();
    }
}