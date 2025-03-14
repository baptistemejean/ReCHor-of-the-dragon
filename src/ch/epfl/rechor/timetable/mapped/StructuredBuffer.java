package ch.epfl.rechor.timetable.mapped;

import java.nio.ByteBuffer;

/**
 * Represents a structured byte buffer, allowing access to flattened data
 * according to a defined structure.
 */
public final class StructuredBuffer {
    private final Structure structure;
    private final ByteBuffer buffer;
    private final int size;

    /**
     * Creates a new structured buffer with the given structure and buffer.
     *
     * @param structure The structure describing the data format
     * @param buffer The buffer containing the data
     * @throws IllegalArgumentException if the buffer size is not a multiple of the structure size
     */
    public StructuredBuffer(Structure structure, ByteBuffer buffer) {
        this.structure = structure;
        this.buffer = buffer;

        int structureSize = structure.totalSize();
        int bufferSize = buffer.capacity();

        if (bufferSize % structureSize != 0) {
            throw new IllegalArgumentException(
                    "Buffer size (" + bufferSize + ") is not a multiple of structure size (" + structureSize + ")");
        }

        this.size = bufferSize / structureSize;
    }

    /**
     * Returns the number of elements in this buffer.
     *
     * @return The number of elements
     */
    public int size() {
        return size;
    }

    /**
     * Gets an unsigned 8-bit integer value from the buffer.
     *
     * @param fieldIndex The index of the field
     * @param elementIndex The index of the element
     * @return The unsigned 8-bit integer value
     * @throws IndexOutOfBoundsException if either index is invalid
     */
    public int getU8(int fieldIndex, int elementIndex) throws IndexOutOfBoundsException {
        int offset = structure.offset(fieldIndex, elementIndex);
        return Byte.toUnsignedInt(buffer.get(offset));
    }

    /**
     * Gets an unsigned 16-bit integer value from the buffer.
     *
     * @param fieldIndex The index of the field
     * @param elementIndex The index of the element
     * @return The unsigned 16-bit integer value
     * @throws IndexOutOfBoundsException if either index is invalid
     */
    public int getU16(int fieldIndex, int elementIndex) throws IndexOutOfBoundsException {
        int offset = structure.offset(fieldIndex, elementIndex);
        return Short.toUnsignedInt(buffer.getShort(offset));
    }

    /**
     * Gets a signed 32-bit integer value from the buffer.
     *
     * @param fieldIndex The index of the field
     * @param elementIndex The index of the element
     * @return The signed 32-bit integer value
     * @throws IndexOutOfBoundsException if either index is invalid
     */
    public int getS32(int fieldIndex, int elementIndex) throws IndexOutOfBoundsException {
        int offset = structure.offset(fieldIndex, elementIndex);
        return buffer.getInt(offset);
    }
}