package ch.epfl.rechor.journey;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

public class ParetoFront {
    // Static empty ParetoFront instance
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    // Private array to store packed tuples
    private final long[] packedTuples;

    // Private constructor that takes packed tuples
    private ParetoFront(long[] tuples) {
        this.packedTuples = tuples;
    }

    /**
     * Returns the size of the Pareto frontier
     * @return number of tuples in the frontier
     */
    public int size() {
        return packedTuples.length;
    }

    /**
     * Retrieves the packed criteria for given arrival time and changes
     * @param arrMins arrival time in minutes
     * @param changes number of changes
     * @return packed tuple matching the criteria
     * @throws NoSuchElementException if no matching tuple exists
     */
    public long get(int arrMins, int changes) {
        for (long packedTuple : packedTuples) {
            // Unpack and check if matches the given criteria
            int tupleArrMins = (int)((packedTuple >> 32) & 0xFFFFFFFFL);
            int tupleChanges = (int)(packedTuple & 0xFFFFFFFFL);

            if (tupleArrMins == arrMins && tupleChanges == changes) {
                return packedTuple;
            }
        }
        throw new NoSuchElementException("No tuple found with arrival time " + arrMins + " and changes " + changes);
    }

    /**
     * Applies the given action to each tuple in the frontier
     * @param action consumer to apply to each packed tuple
     */
    public void forEach(LongConsumer action) {
        for (long packedTuple : packedTuples) {
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
        for (long packedTuple : packedTuples) {
            int arrMins = (int)((packedTuple >> 32) & 0xFFFFFFFFL);
            int changes = (int)(packedTuple & 0xFFFFFFFFL);
            sb.append(String.format("  {arrMins: %d, changes: %d}\n", arrMins, changes));
        }
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

        /**
         * Default constructor creating an empty builder
         */
        public Builder() {
            this.tuples = new long[4]; // Initial capacity
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
            size = 0;
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
            for (int i = 0; i < size; i++) {
                if (dominates(tuples[i], packedTuple)) {
                    shouldAdd = false;
                    break;
                }
                if (dominates(packedTuple, tuples[i])) {
                    // Remove the dominated tuple
                    System.arraycopy(tuples, i + 1, tuples, i, size - i - 1);
                    size--;
                    i--;
                }
            }

            if (shouldAdd) {
                // Resize if needed
                if (size == tuples.length) {
                    tuples = Arrays.copyOf(tuples, tuples.length * 2);
                }
                tuples[size++] = packedTuple;
            }

            return this;
        }

        /**
         * Adds a tuple with arrival time, changes, and payload
         * @param arrMins arrival time in minutes
         * @param changes number of changes
         * @param payload additional payload (unused in this implementation)
         * @return this builder
         */
        public Builder add(int arrMins, int changes, int payload) {
            // Pack arrival time and changes into a single long
            long packedTuple = ((long)arrMins << 32) | (changes & 0xFFFFFFFFL);
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
                    if (dominates(this.tuples[j], that.tuples[i])) {
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

        /**
         * Checks if one packed tuple dominates another
         * @param tuple1 first tuple
         * @param tuple2 second tuple
         * @return true if tuple1 dominates tuple2, false otherwise
         */
        private boolean dominates(long tuple1, long tuple2) {
            int arrMins1 = (int)((tuple1 >> 32) & 0xFFFFFFFFL);
            int changes1 = (int)(tuple1 & 0xFFFFFFFFL);

            int arrMins2 = (int)((tuple2 >> 32) & 0xFFFFFFFFL);
            int changes2 = (int)(tuple2 & 0xFFFFFFFFL);

            return (arrMins1 <= arrMins2 && changes1 <= changes2) &&
                    (arrMins1 < arrMins2 || changes1 < changes2);
        }
    }
}