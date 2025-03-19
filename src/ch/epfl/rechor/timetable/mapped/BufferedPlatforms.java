package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Platforms;

import java.nio.ByteBuffer;
import java.util.List;

import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.S32;
import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.U16;
import static ch.epfl.rechor.timetable.mapped.Structure.field;

/**
 * Represents a buffered implementation of the {@link Platforms} interface,
 * storing platform data in a structured buffer for efficient access.
 */
public class BufferedPlatforms implements Platforms {
    // Constants for field indices
    private final static int NAME_ID = 0;
    private final static int STATION_ID = 1;

    // Structure definition
    private static final Structure STRUCTURE = new Structure(
            field(NAME_ID, U16),
            field(STATION_ID, U16)
    );

    private StructuredBuffer structuredBuffer;
    private List<String> stringTable;

    /**
     * Constructs a {@link BufferedPlatforms} instance with the given string table and byte buffer.
     *
     * @param stringTable the list of strings
     * @param buffer the byte buffer containing structured platform data
     */
    public BufferedPlatforms(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = stringTable;
        this.structuredBuffer = new StructuredBuffer(STRUCTURE, buffer);
    }

    /**
     * Returns the name of the platform with the given index.
     *
     * @param id the platform index
     * @return the name of the platform
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public String name(int id) throws IndexOutOfBoundsException {
        return stringTable.get(structuredBuffer.getU16(NAME_ID, id));
    }

    /**
     * Returns the station index associated with the platform.
     *
     * @param id the platform index
     * @return the station index corresponding to the platform
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int stationId(int id) throws IndexOutOfBoundsException {
        return structuredBuffer.getU16(STATION_ID, id);
    }

    /**
     * Returns the total number of platforms stored in the buffer.
     *
     * @return the number of platforms
     */
    @Override
    public int size() {
        return structuredBuffer.size();
    }
}