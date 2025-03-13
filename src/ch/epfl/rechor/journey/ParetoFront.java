package ch.epfl.rechor.journey;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

public class ParetoFront {
    // Static empty ParetoFront instance
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    // Private array to store packed criteria
    private final long[] packedCriteria;

    // Private constructor that takes packed tuples
    ParetoFront(long[] tuples) {
        this.packedCriteria = tuples;
    }

    /**
     * Returns the size of the Pareto frontier
     * @return number of tuples in the frontier
     */
    public int size() {
        return packedCriteria.length;
    }

    /**
     * Retrieves the packed criteria for given arrival time and changes
     * @param arrMins arrival time in minutes
     * @param changes number of changes
     * @return packed tuple matching the criteria
     * @throws NoSuchElementException if no matching tuple exists
     */
    public long get(int arrMins, int changes) {
        for (long packed : packedCriteria) {
            // Unpack and check if matches the given criteria
            int tupleArrMins = PackedCriteria.arrMins(packed);
            int tupleChanges = PackedCriteria.changes(packed);

            if (tupleArrMins == arrMins && tupleChanges == changes) {
                return packed;
            }
        }

        throw new NoSuchElementException("No tuple found with arrival time " + arrMins + " and changes " + changes);
    }

    public boolean equals(ParetoFront obj) {
        return Arrays.equals(obj.packedCriteria, this.packedCriteria);
    }

    /**
     * Applies the given action to each tuple in the frontier
     * @param action consumer to apply to each packed tuple
     */
    public void forEach(LongConsumer action) {
        for (long packedTuple : packedCriteria) {
            action.accept(packedTuple);
        }
    }

    /**
     * Provides a readable string representation of the Pareto frontier
     * @return string representation of the frontier
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ParetoFront[\n");
        for (long packed : packedCriteria) {
            int arrMins = PackedCriteria.arrMins(packed);
            int changes = PackedCriteria.changes(packed);
            sb.append(String.format("  {arrMins: %d, changes: %d}\n", arrMins, changes));
        }
        sb.append(String.format("  {size: %d}\n", size()));
        sb.append("]");
        return sb.toString();
    }

    /**
     * Builder inner class for constructing ParetoFront instances
     */
    public static class Builder {
        // Internal array to store packed tuples during construction
        private long[] tuples;
        private int size;

        private final static int DEFAULT_CAPACITY = 3;

        /**
         * Default constructor creating an empty builder
         */
        public Builder() {
            this.tuples = new long[DEFAULT_CAPACITY];
            this.size = 0;
        }

        /**
         * Copy constructor
         * @param that builder to copy
         */
        public Builder(Builder that) {
            this.tuples = Arrays.copyOf(that.tuples, that.tuples.length);
            this.size = that.size;
        }

        /**
         * Checks if the builder's frontier is empty
         * @return true if no tuples, false otherwise
         */
        public boolean isEmpty() {
            return size == 0;
        }

        /**
         * Clears all tuples from the builder
         * @return this builder
         */
        public Builder clear() {
            this.tuples = new long[DEFAULT_CAPACITY];
            this.size = 0;
            return this;
        }

        /**
         * Adds a packed tuple to the frontier if it's not dominated
         * @param packedTuple tuple to add
         * @return this builder
         */
        public Builder add(long packedTuple) {
            // Ensure dominance and remove dominated tuples
            boolean shouldAdd = true;
            int destPos = 0;
            for (int i = 0; i < size; i++) {
                if (packedTuple > tuples[i]) {
                    if (PackedCriteria.dominatesOrIsEqual(tuples[i], packedTuple)) {
                        shouldAdd = false;
                        break;
                    }
                    destPos = i + 1;
                } else {
                    if (PackedCriteria.dominatesOrIsEqual(packedTuple, tuples[i])) {
                        // Remove the dominated tuple
                        System.arraycopy(tuples, i + 1, tuples, i, size - i - 1);
                        size--;
                        i--;
                    }
                }
            }

            if (shouldAdd) {
                // Resize if needed
                 if (size == tuples.length) {
                     tuples = Arrays.copyOf(tuples, tuples.length * 2);
                 }

                if (size != 0) {
                    System.arraycopy(tuples, destPos, tuples, destPos + 1, tuples.length - destPos - 1);
                }

                tuples[destPos] = packedTuple;
                size++;
            }

            return this;
        }

        /**
         * Adds a tuple with arrival time, changes, and payload
         * @param arrMins arrival time in minutes
         * @param changes number of changes
         * @param payload additional payload
         * @return this builder
         */
        public Builder add(int arrMins, int changes, int payload) {
            // Pack arrival time and changes into a single long
            long packedTuple = PackedCriteria.pack(arrMins, changes, payload);
            return add(packedTuple);
        }

        /**
         * Adds all tuples from another builder
         * @param that builder to add tuples from
         * @return this builder
         */
        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.tuples[i]);
            }
            return this;
        }

        /**
         * Checks if all tuples in another builder are fully dominated
         * @param that builder to check
         * @param depMins departure time in minutes
         * @return true if all tuples are dominated, false otherwise
         */
        public boolean fullyDominates(Builder that, int depMins) {
            for (int i = 0; i < that.size; i++) {
                boolean dominated = false;
                for (int j = 0; j < this.size; j++) {
                    if (PackedCriteria.dominatesOrIsEqual(this.tuples[j], PackedCriteria.withDepMins(that.tuples[i], depMins))) {
                        dominated = true;
                        break;
                    }
                }
                if (!dominated) {
                    return false;
                }
            }
            return true;
        }

        /*
        var b1 = new ParetoFront.Builder();
        add(b1, 50, 60, 5, 1);
        add(b1, 50, 65, 4, 2);
        add(b1, 50, 70, 3, 3);

        var b2 = new ParetoFront.Builder();
        b2.add(60, 5, 1);
        b2.add(65, 4, 2);
        b2.add(70, 3, 3);
        */

        /**
         * Applies the given action to each tuple
         * @param action consumer to apply to each packed tuple
         */
        public void forEach(LongConsumer action) {
            for (int i = 0; i < size; i++) {
                action.accept(tuples[i]);
            }
        }

        /**
         * Builds and returns an immutable ParetoFront
         * @return constructed ParetoFront
         */
        public ParetoFront build() {
            // Create a copy to ensure immutability
            return new ParetoFront(Arrays.copyOf(tuples, size));
        }

        public boolean equals(Builder obj) {
            return Arrays.equals(obj.tuples, this.tuples) && obj.size == this.size;
        }

        @Override
        public String toString() {
            return this.build().toString();
        }
    }
}