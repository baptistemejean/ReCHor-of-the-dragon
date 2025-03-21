package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.StationAliases;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Implementation of the {@link StationAliases} interface that accesses flattened data
 * in a {@link ByteBuffer}.
 */
public final class BufferedStationAliases implements StationAliases {
    // Constants for field indices
    private static final int ALIAS_ID = 0;
    private static final int STATION_NAME_ID = 1;

    // Structure definition
    private static final Structure STRUCTURE = new Structure(
            Structure.field(ALIAS_ID, Structure.FieldType.U16),
            Structure.field(STATION_NAME_ID, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Constructs a BufferedStationAliases from a string table and a buffer
     * containing the flattened data.
     *
     * @param stringTable The table of strings referenced by the flattened data
     * @param buffer The buffer containing the flattened data
     */
    public BufferedStationAliases(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(STRUCTURE, buffer);
    }

    /**
     * Returns the alias of the station with the given index.
     *
     * @param id the station index
     * @return the index in the string table of the alias of the station
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public String alias(int id) throws IndexOutOfBoundsException {
        return stringTable.get(buffer.getU16(ALIAS_ID, id));
    }

    /**
     * Returns the name of the station with the given index.
     *
     * @param id the station index
     * @return the name of the station
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public String stationName(int id) throws IndexOutOfBoundsException {
        return stringTable.get(buffer.getU16(STATION_NAME_ID, id));
    }

    /**
     * Returns the total number of station aliases stored in the buffer.
     *
     * @return the number of station aliases
     */
    @Override
    public int size() {
        return buffer.size();
    }
}