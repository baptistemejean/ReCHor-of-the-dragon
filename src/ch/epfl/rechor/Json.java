package ch.epfl.rechor;

/**
 * The {@code Json} interface represents a generic JSON element.
 * It is a sealed interface permitting various specific JSON element types:
 * {@link Json.JArray}, {@link Json.JObject}, {@link Json.JString}, and {@link Json.JNumber}.
 */
public sealed interface Json permits Json.JArray, Json.JObject, Json.JString, Json.JNumber {

    /**
     * Represents a JSON array, which holds a list of {@link Json} elements.
     *
     * @param elements the list of JSON elements in the array
     */
    public record JArray(java.util.List<Json> elements) implements Json {
        /**
         * Returns the JSON string representation of this array.
         *
         * @return the JSON formatted string representing this array
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');

            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(elements.get(i).toString());
            }

            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Represents a JSON object, which holds key-value pairs.
     *
     * @param elements the map containing the object's keys and their corresponding {@link Json} values
     */
    public record JObject(java.util.Map<String, Json> elements) implements Json {
        /**
         * Returns the JSON string representation of this object.
         *
         * @return the JSON formatted string representing this object
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');

            boolean first = true;
            for (var entry : elements.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(entry.getKey()).append("\":").append(entry.getValue().toString());
                first = false;
            }

            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * Represents a JSON string.
     *
     * @param value the string value
     */
    public record JString(String value) implements Json {
        /**
         * Returns the JSON string representation of this string.
         *
         * @return the JSON formatted string representing this string value
         */
        @Override
        public String toString() {
            return "\"" + value + "\"";
        }
    }

    /**
     * Represents a JSON number.
     *
     * @param value the numeric value
     */
    public record JNumber(Double value) implements Json {
        /**
         * Returns the JSON string representation of this number.
         *
         * @return the JSON formatted string representing this number
         */
        @Override
        public String toString() {
            return Double.toString(value);
        }
    }
}
