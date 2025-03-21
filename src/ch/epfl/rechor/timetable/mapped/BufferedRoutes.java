package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Routes;
import ch.epfl.rechor.journey.Vehicle;
import java.nio.ByteBuffer;
import java.util.List;

import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.*;
import static ch.epfl.rechor.timetable.mapped.Structure.field;

/**
 * Implementation of the Routes interface that accesses flattened data
 * in a ByteBuffer.
 */
public final class BufferedRoutes implements Routes {
    // Constants for field indices
    private static final int NAME_ID = 0;
    private static final int KIND = 1;

    // Structure definition
    private static final Structure STRUCTURE = new Structure(
            field(NAME_ID, U16),
            field(KIND, U8)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Constructs a BufferedRoutes from a string table and a buffer
     * containing the flattened data.
     *
     * @param stringTable The table of strings referenced by the flattened data
     * @param buffer The buffer containing the flattened data
     */
    public BufferedRoutes(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = stringTable;
        this.buffer = new StructuredBuffer(STRUCTURE, buffer);
    }

    @Override
    public String name(int index) {
        int nameStringIndex = buffer.getU16(NAME_ID, index);
        return stringTable.get(nameStringIndex);
    }

    @Override
    public Vehicle vehicle(int index) {
        int vehicleKind = buffer.getU8(KIND, index);
        return Vehicle.values()[vehicleKind];
    }

    /**
     * Returns the total number of routes stored in the buffer.
     *
     * @return the number of routes
     */
    @Override
    public int size() {
        return buffer.size();
    }
}