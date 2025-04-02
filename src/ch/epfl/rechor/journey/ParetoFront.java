package ch.epfl.rechor.journey;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

/**
 * Represents an immutable Pareto frontier of optimization criteria.
 * <p>
 * A Pareto frontier contains a set of non-dominated tuples, where each tuple represents a
 * combination of criteria (arrival time, changes, and optional payload). A tuple is considered
 * non-dominated if no other tuple is better in all criteria simultaneously.
 * <p>
 * This class provides functionality to create, query, and manipulate Pareto frontiers for
 * multi-criteria optimization problems, particularly in the context of journey planning.
 *
 * @see PackedCriteria
 */
public class ParetoFront {
    /**
     * A pre-defined empty Pareto frontier instance.
     */
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    /**
     * The array containing the packed criteria tuples that form this Pareto frontier. Each long
     * value represents a packed set of criteria according to {@link PackedCriteria}.
     */
    private final long[] packedCriteria;

    /**
     * Constructs a new Pareto frontier from an array of packed tuples.
     *
     * @param tuples the array of packed criteria tuples
     */
    ParetoFront(long[] tuples) {
        this.packedCriteria = tuples;
    }

    /**
     * Returns the size of the Pareto frontier.
     *
     * @return number of tuples in the frontier
     */
    public int size() {
        return packedCriteria.length;
    }

    /**
     * Retrieves the packed criteria for given arrival time and changes.
     *
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

        throw new NoSuchElementException(
                "No tuple found with arrival time " + arrMins + " and changes " + changes);
    }

    /**
     * Checks if this Pareto frontier equals another one.
     *
     * @param obj the other Pareto frontier to compare with
     * @return true if both frontiers contain the same tuples, false otherwise
     */
    public boolean equals(ParetoFront obj) {
        return Arrays.equals(obj.packedCriteria, this.packedCriteria);
    }

    /**
     * Applies the given action to each tuple in the frontier.
     *
     * @param action consumer to apply to each packed tuple
     */
    public void forEach(LongConsumer action) {
        for (long packedTuple : packedCriteria) {
            action.accept(packedTuple);
        }
    }

    /**
     * Provides a readable string representation of the Pareto frontier.
     * <p>
     * The representation includes all tuples with their unpacked criteria values and the total size
     * of the frontier.
     *
     * @return string representation of the frontier
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ParetoFront[\n");
        for (long packed : packedCriteria) {
            int arrMins = PackedCriteria.arrMins(packed);
            int changes = PackedCriteria.changes(packed);
            int payload = PackedCriteria.payload(packed);
            if (PackedCriteria.hasDepMins(packed)) {
                int depMins = PackedCriteria.depMins(packed);
                sb.append(String.format("  {arrMins: %d, depMins: %d, changes: %d, payload: %d}\n",
                        arrMins,
                        depMins,
                        changes,
                        payload
                ));
            } else {
                sb.append(String.format("  {arrMins: %d, changes: %d, payload: %d}\n",
                        arrMins,
                        changes,
                        payload
                ));
            }
        }
        sb.append(String.format("  {size: %d}\n", size()));
        sb.append("]");
        return sb.toString();
    }

    /**
     * Builder class for constructing ParetoFront instances.
     * <p>
     * This builder maintains a mutable collection of non-dominated tuples and provides methods to
     * add new tuples while preserving the Pareto optimality of the set.
     */
    public static class Builder {
        /**
         * Default initial capacity for the tuples array.
         */
        private final static int DEFAULT_CAPACITY = 3;

        private long[] tuples;
        private int size;

        /**
         * Creates an empty builder with default capacity.
         */
        public Builder() {
            this.tuples = new long[DEFAULT_CAPACITY];
            this.size = 0;
        }

        /**
         * Creates a new builder by copying another one.
         *
         * @param that builder to copy
         */
        public Builder(Builder that) {
            this.tuples = Arrays.copyOf(that.tuples, that.tuples.length);
            this.size = that.size;
        }

        /**
         * Checks if the builder's frontier is empty.
         *
         * @return true if no tuples are present, false otherwise
         */
        public boolean isEmpty() {
            return size == 0;
        }

        /**
         * Removes all tuples from the builder.
         *
         * @return this builder instance for method chaining
         */
        public Builder clear() {
            this.tuples = new long[DEFAULT_CAPACITY];
            this.size = 0;
            return this;
        }

        /**
         * Adds a packed tuple to the frontier if it's not dominated by any existing tuple.
         * <p>
         * Also removes any existing tuples that are dominated by the new tuple.
         *
         * @param packedTuple tuple to add
         * @return this builder instance for method chaining
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
                    System.arraycopy(tuples,
                            destPos,
                            tuples,
                            destPos + 1,
                            tuples.length - destPos - 1
                    );
                }

                tuples[destPos] = packedTuple;
                size++;
            }

            return this;
        }

        /**
         * Adds a tuple with specified arrival time, changes, and payload to the frontier.
         * <p>
         * The tuple will be packed using {@link PackedCriteria#pack} before being added.
         *
         * @param arrMins arrival time in minutes
         * @param changes number of changes
         * @param payload additional payload information
         * @return this builder instance for method chaining
         */
        public Builder add(int arrMins, int changes, int payload) {
            long packedTuple = PackedCriteria.pack(arrMins, changes, payload);
            return add(packedTuple);
        }

        /**
         * Adds all non-dominated tuples from another builder to this one.
         *
         * @param that builder to add tuples from
         * @return this builder instance for method chaining
         */
        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.tuples[i]);
            }
            return this;
        }

        /**
         * Checks if all tuples in another builder are fully dominated by tuples in this builder,
         * when considering a specific departure time.
         *
         * @param that    builder to check against
         * @param depMins departure time in minutes to use for comparison
         * @return true if all tuples in the other builder are dominated, false otherwise
         */
        public boolean fullyDominates(Builder that, int depMins) {
            for (int i = 0; i < that.size; i++) {
                boolean dominated = false;
                for (int j = 0; j < this.size; j++) {
                    if (PackedCriteria.dominatesOrIsEqual(this.tuples[j],
                            PackedCriteria.withDepMins(that.tuples[i], depMins)
                    )) {
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
         * Applies the given action to each tuple in the builder.
         *
         * @param action consumer to apply to each packed tuple
         */
        public void forEach(LongConsumer action) {
            for (int i = 0; i < size; i++) {
                action.accept(tuples[i]);
            }
        }

        /**
         * Builds and returns an immutable ParetoFront instance.
         * <p>
         * The returned Pareto frontier contains a copy of the tuples in this builder.
         *
         * @return a new immutable ParetoFront instance
         */
        public ParetoFront build() {
            return new ParetoFront(Arrays.copyOf(tuples, size));
        }

        /**
         * Checks if this builder equals another one.
         *
         * @param obj the other builder to compare with
         * @return true if both builders contain the same tuples, false otherwise
         */
        public boolean equals(Builder obj) {
            return Arrays.equals(obj.tuples, this.tuples) && obj.size == this.size;
        }

        /**
         * Returns a string representation of this builder by building a ParetoFront and using its
         * string representation.
         *
         * @return string representation of the builder's content
         */
        @Override
        public String toString() {
            return this.build().toString();
        }
    }
}