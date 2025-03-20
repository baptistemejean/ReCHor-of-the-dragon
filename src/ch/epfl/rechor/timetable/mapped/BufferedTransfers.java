package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Transfers;
import ch.epfl.rechor.PackedRange;
import java.nio.ByteBuffer;

/**
 * Implementation of the Transfers interface that accesses flattened data
 * in a ByteBuffer.
 */
public final class BufferedTransfers implements Transfers {
    // Constants for field indices
    private static final int DEP_STATION_ID = 0;
    private static final int ARR_STATION_ID = 1;
    private static final int TRANSFER_MINUTES = 2;

    // Structure definition
    private static final Structure STRUCTURE = new Structure(
            Structure.field(DEP_STATION_ID, Structure.FieldType.U16),
            Structure.field(ARR_STATION_ID, Structure.FieldType.U16),
            Structure.field(TRANSFER_MINUTES, Structure.FieldType.U8)
    );

    private final StructuredBuffer buffer;
    private final int[] arrivingTransfers;

    /**
     * Constructs a BufferedTransfers from a buffer containing the flattened data.
     * @param buffer The buffer containing the transfer data
     */
    public BufferedTransfers(ByteBuffer buffer) {
        this.buffer = new StructuredBuffer(STRUCTURE, buffer);

        // First pass: determine the number of stations
        int maxStationId = -1;
        for (int i = 0; i < size(); i++) {
            int arrStationId = this.buffer.getU16(ARR_STATION_ID, i);
            maxStationId = Math.max(maxStationId, arrStationId);
        }

        // Create the lookup table for arriving transfers
        arrivingTransfers = new int[maxStationId + 1];
//
//        // Initialize with invalid intervals
//        for (int i = 0; i < arrivingTransfers.length; i++) {
//            arrivingTransfers[i] = PackedRange.pack(-1, -1);
//        }

        // Second pass: fill the lookup table
        int currentArrStation = -1;
        int startIndex = 0;

        for (int i = 0; i < size(); i++) {
            int arrStationId = this.buffer.getU16(ARR_STATION_ID, i);

            if (arrStationId != currentArrStation) {
                // Close the previous interval if there was one
                if (currentArrStation != -1) {
                    arrivingTransfers[currentArrStation] = PackedRange.pack(startIndex, i);
                }

                // Start a new interval
                currentArrStation = arrStationId;
                startIndex = i;
            }
        }

        // Close the last interval
        if (currentArrStation != -1) {
            arrivingTransfers[currentArrStation] = PackedRange.pack(startIndex, size());
        }
    }

    @Override
    public int size() {
        return buffer.size();
    }

    @Override
    public int depStationId(int index) {
        return buffer.getU16(DEP_STATION_ID, index);
    }

    @Override
    public int arrivingAt(int index) {
//        return buffer.getU16(ARR_STATION_ID, index);
        return arrivingTransfers[index];
    }

    @Override
    public int minutesBetween(int depStationId, int arrStationId) throws IndexOutOfBoundsException {
        int arrivingAt = arrivingAt(arrStationId);
        int startInterval = PackedRange.startInclusive(arrivingAt);
        int endInterval = PackedRange.endExclusive(arrivingAt);

        for (int i = startInterval; i < endInterval; i++) {
            if (depStationId(i) == depStationId) {
                return minutes(i);
            }
        }

        return -1;
    }

    @Override
    public int minutes(int index) {
        return buffer.getU8(TRANSFER_MINUTES, index);
    }

//    @Override
//    public int arrivingAt(int stationId) {
//        if (stationId < 0 || stationId >= arrivingTransfers.length) {
//            return packInterval(-1, -1); // No transfers arriving at this station
//        }
//
//        return arrivingTransfers[stationId];
//    }
}