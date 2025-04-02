package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Connections;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.S32;
import static ch.epfl.rechor.timetable.mapped.Structure.FieldType.U16;
import static ch.epfl.rechor.timetable.mapped.Structure.field;

/**
 * Implementation of the {@link Connections} interface that accesses flattened data in a
 * {@link ByteBuffer}.
 */
public final class BufferedConnections implements Connections {
    // Constants for field indices
    private static final int DEP_STOP_ID = 0;
    private static final int DEP_MINUTES = 1;
    private static final int ARR_STOP_ID = 2;
    private static final int ARR_MINUTES = 3;
    private static final int TRIP_POS_ID = 4;

    // Structure definition for the buffer
    private static final Structure STRUCTURE = new Structure(
            field(DEP_STOP_ID, U16),
            field(DEP_MINUTES, U16),
            field(ARR_STOP_ID, U16),
            field(ARR_MINUTES, U16),
            field(TRIP_POS_ID, S32)
    );

    private final StructuredBuffer buffer;
    private final IntBuffer successorBuffer;

    /**
     * Constructs a BufferedConnections from buffers containing the flattened data.
     *
     * @param buffer     The buffer containing the connection data
     * @param succBuffer The buffer containing the successor connection data
     */
    public BufferedConnections(ByteBuffer buffer, ByteBuffer succBuffer) {
        this.buffer = new StructuredBuffer(STRUCTURE, buffer);
        this.successorBuffer = succBuffer.asIntBuffer();
    }

    /**
     * Returns the departure station or platform index of the given connection.
     *
     * @param id the connection index
     * @return the departure station or platform index corresponding to the given connection
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int depStopId(int id) {
        return buffer.getU16(DEP_STOP_ID, id);
    }

    /**
     * Returns the departure time in minutes since midnight of the given connection.
     *
     * @param id the connection index
     * @return the departure time in minutes since midnight
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int depMins(int id) {
        return buffer.getU16(DEP_MINUTES, id);
    }

    /**
     * Returns the arrival station or platform index of the given connection.
     *
     * @param id the connection index
     * @return the arrival station or platform index corresponding to the given connection
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int arrStopId(int id) {
        return buffer.getU16(ARR_STOP_ID, id);
    }

    /**
     * Returns the arrival time in minutes since midnight of the given connection.
     *
     * @param id the connection index
     * @return the arrival time in minutes since midnight
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int arrMins(int id) {
        return buffer.getU16(ARR_MINUTES, id);
    }

    /**
     * Returns the index of trip associated with the given connection
     *
     * @param id the connection index
     * @return the index of the associated trip
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int tripId(int id) {
        int tripPosId = buffer.getS32(TRIP_POS_ID, id);
        return tripPosId >>> 8; // Extract the trip ID from the upper 24 bits
    }

    /**
     * Returns the position of the given connection in its associated trip
     *
     * @param id the connection index
     * @return the position of the given connection
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int tripPos(int id) {
        int tripPosId = buffer.getS32(TRIP_POS_ID, id);
        return tripPosId & 0xFF; // Extract the position from the lower 8 bits
    }

    /**
     * Returns the index of the connection following the given connection
     *
     * @param id the connection index
     * @return the index of the next connection
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Override
    public int nextConnectionId(int id) {
        return successorBuffer.get(id);
    }

    /**
     * Returns the total number of connections stored in the buffer.
     *
     * @return the number of connections
     */
    @Override
    public int size() {
        return buffer.size();
    }
}