package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Preconditions;

/**
 * Utility class for describing flattened data structures.
 */
public final class Structure {

    /**
     * Enumeration of possible field types.
     */
    public enum FieldType {
        U8,  // Unsigned 8-bit integer
        U16, // Unsigned 16-bit integer
        S32  // Signed 32-bit integer
    }

    /**
     * Represents a field in the structure.
     */
    public record Field(int index, FieldType type) {
        /**
         * Creates a new field with the given index and type.
         *
         * @param index The index of the field
         * @param type The type of the field
         * @throws NullPointerException if type is null
         */
        public Field {
            if (type == null) {
                throw new NullPointerException();
            }
        }
    }

    private final int[] offsets;
    private final int totalSize;

    /**
     * Creates a new structure with the given fields.
     *
     * @param fields The fields of the structure
     * @throws IllegalArgumentException if the fields are not in order
     */
    public Structure(Field... fields) throws IllegalArgumentException {
        // Check if fields are in order
        for (int i = 0; i < fields.length; i++) {
            Preconditions.checkArgument(fields[i].index() == i);
        }

        // Calculate offsets and total size
        offsets = new int[fields.length];
        int currentOffset = 0;

        for (int i = 0; i < fields.length; i++) {
            offsets[i] = currentOffset;
            currentOffset += sizeOf(fields[i].type());
        }

        this.totalSize = currentOffset;
    }

    /**
     * Returns the total size of the structure in bytes.
     *
     * @return The total size of the structure
     */
    public int totalSize() {
        return totalSize;
    }

    /**
     * Returns the offset of the given field in the given element.
     *
     * @param fieldIndex The index of the field
     * @param elementIndex The index of the element
     * @return The offset of the field in the element
     * @throws IndexOutOfBoundsException if the field index is invalid
     */
    public int offset(int fieldIndex, int elementIndex) throws IndexOutOfBoundsException {
//        if (fieldIndex < 0 || fieldIndex >= fields.length) {
//            throw new IndexOutOfBoundsException("Invalid field index");
//        }
        return elementIndex * totalSize + offsets[fieldIndex];
    }

    /**
     * Utility method to create a field without using 'new'.
     *
     * @param index The index of the field
     * @param type The type of the field
     * @return A new field with the given index and type
     */
    public static Field field(int index, FieldType type) {
        return new Field(index, type);
    }

    /**
     * Returns the size in bytes of the given field type.
     *
     * @param type The field type
     * @return The size in bytes of the field type
     */
    private static int sizeOf(FieldType type) {
        return switch (type) {
            case U8 -> 1;
            case U16 -> 2;
            case S32 -> 4;
        };
    }
}