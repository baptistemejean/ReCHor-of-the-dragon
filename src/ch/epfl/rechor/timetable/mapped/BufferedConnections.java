package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Connections;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.*;
import static ch.epfl.rechor.timetable.mapped.Structure.field;

/**
 * Implementation of the Connections interface that accesses flattened data
 * in a {@link ByteBuffer}.
 */
public final class BufferedConnections implements Connections {
    // Constants for field indices
    private static final int DEP_STOP_ID = 0;
    private static final int DEP_MINUTES = 1;
    private static final int ARR_STOP_ID = 2;
    private static final int ARR_MINUTES = 3;
    private static final int TRIP_POS_ID = 4;

    // Structure definition for the main buffer
    private static final Structure STRUCTURE = new Structure(
            field(DEP_STOP_ID, U16),
            field(DEP_MINUTES, U16),
            field(ARR_STOP_ID, U16),
            field(ARR_MINUTES, U16),
            field(TRIP_POS_ID, S32)
    );

    private final StructuredBuffer structuredBuffer;
    private final IntBuffer successorBuffer;

    /**
     * Constructs a BufferedConnections from buffers containing the flattened data.
     *
     * @param structuredBuffer The buffer containing the connection data
     * @param succBuffer The buffer containing the successor connection data
     */
    public BufferedConnections(ByteBuffer structuredBuffer, ByteBuffer succBuffer) {
        this.structuredBuffer = new StructuredBuffer(STRUCTURE, structuredBuffer);
        this.successorBuffer = succBuffer.asIntBuffer();
    }

    @Override
    public int size() {
        return structuredBuffer.size();
    }

    @Override
    public int depStopId(int id) {
        return structuredBuffer.getU16(DEP_STOP_ID, id);
    }

    @Override
    public int depMins(int index) {
        return structuredBuffer.getU16(DEP_MINUTES, index);
    }

    @Override
    public int arrStopId(int index) {
        return structuredBuffer.getU16(ARR_STOP_ID, index);
    }

    @Override
    public int arrMins(int index) {
        return structuredBuffer.getU16(ARR_MINUTES, index);
    }

    @Override
    public int tripPos(int index) {
        int tripPosId = structuredBuffer.getS32(TRIP_POS_ID, index);
        return tripPosId >>> 8; // Extract the trip ID from the upper 24 bits
    }

    @Override
    public int tripId(int index) {
        int tripPosId = structuredBuffer.getS32(TRIP_POS_ID, index);
        return tripPosId & 0xFF; // Extract the position from the lower 8 bits
    }

    @Override
    public int nextConnectionId(int index) {
        return successorBuffer.get(index);
    }
}