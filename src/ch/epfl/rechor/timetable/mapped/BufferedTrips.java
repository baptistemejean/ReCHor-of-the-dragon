package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Trips;
import java.nio.ByteBuffer;
import java.util.List;

import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.*;
import static ch.epfl.rechor.timetable.mapped.Structure.field;

/**
 * Implementation of the Trips interface that accesses flattened data
 * in a ByteBuffer.
 */
public final class BufferedTrips implements Trips {
    // Constants for field indices
    private static final int ROUTE_ID = 0;
    private static final int DESTINATION_ID = 1;

    // Structure definition
    private static final Structure STRUCTURE = new Structure(
            field(ROUTE_ID, U16),
            field(DESTINATION_ID, U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Constructs a BufferedTrips from a string table and a buffer
     * containing the flattened data.
     *
     * @param stringTable The table of strings referenced by the flattened data
     * @param buffer The buffer containing the flattened data
     */
    public BufferedTrips(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(STRUCTURE, buffer);
    }

    @Override
    public int routeId(int index) {
        return buffer.getU16(ROUTE_ID, index);
    }

    @Override
    public String destination(int index) {
        int destinationStringIndex = buffer.getU16(DESTINATION_ID, index);
        return stringTable.get(destinationStringIndex);
    }

    /**
     * Returns the total number of trips stored in the buffer.
     *
     * @return the number of trips
     */
    @Override
    public int size() {
        return buffer.size();
    }
}