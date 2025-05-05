package ch.epfl.rechor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonTest {
    @Test
    void testJNumber() {
        Json.JNumber number = new Json.JNumber(42.5);
        assertEquals("42.5", number.toString());

        Json.JNumber integer = new Json.JNumber(42.0);
        assertEquals("42.0", integer.toString());
    }

    @Test
    void testJString() {
        Json.JString string = new Json.JString("hello");
        assertEquals("\"hello\"", string.toString());

        Json.JString empty = new Json.JString("");
        assertEquals("\"\"", empty.toString());
    }

    @Test
    void testJArray() {
        Json.JArray emptyArray = new Json.JArray(List.of());
        assertEquals("[]", emptyArray.toString());

        Json.JArray singleElementArray = new Json.JArray(List.of(new Json.JNumber(42.0)));
        assertEquals("[42.0]", singleElementArray.toString());

        Json.JArray multiElementArray = new Json.JArray(List.of(
                new Json.JNumber(42.0),
                new Json.JString("hello"),
                new Json.JArray(List.of(new Json.JNumber(1.0), new Json.JNumber(2.0)))
        ));
        assertEquals("[42.0,\"hello\",[1.0,2.0]]", multiElementArray.toString());
    }

    @Test
    void testJObject() {
        Json.JObject emptyObject = new Json.JObject(Map.of());
        assertEquals("{}", emptyObject.toString());

        Json.JObject singlePropertyObject = new Json.JObject(Map.of(
                "answer", new Json.JNumber(42.0)
        ));
        assertEquals("{\"answer\":42.0}", singlePropertyObject.toString());

        Json.JObject multiPropertyObject = new Json.JObject(Map.of(
                "answer", new Json.JNumber(42.0),
                "message", new Json.JString("hello"),
                "data", new Json.JArray(List.of(new Json.JNumber(1.0), new Json.JNumber(2.0)))
        ));

        // Since Map iteration order is not guaranteed, we need to check that all elements are present
        String result = multiPropertyObject.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("\"answer\":42.0"));
        assertTrue(result.contains("\"message\":\"hello\""));
        assertTrue(result.contains("\"data\":[1.0,2.0]"));
    }

    @Test
    void testComplexJson() {
        // Create a complex JSON structure similar to the GeoJSON example
        Json geoJson = new Json.JObject(Map.of(
                "type", new Json.JString("LineString"),
                "coordinates", new Json.JArray(List.of(
                        new Json.JArray(List.of(new Json.JNumber(6.62909), new Json.JNumber(46.51679))),
                        new Json.JArray(List.of(new Json.JNumber(6.83787), new Json.JNumber(46.54276))),
                        new Json.JArray(List.of(new Json.JNumber(6.91181), new Json.JNumber(46.69351))),
                        new Json.JArray(List.of(new Json.JNumber(7.15105), new Json.JNumber(46.80315)))
                ))
        ));

        String expected = "{\"type\":\"LineString\",\"coordinates\":[[6.62909,46.51679],[6.83787,46.54276],[6.91181,46.69351],[7.15105,46.80315]]}";
        // Or the other order
        String expected2 = "{\"coordinates\":[[6.62909,46.51679],[6.83787,46.54276],[6.91181,46.69351],[7.15105,46.80315]],\"type\":\"LineString\"}";

        String actual = geoJson.toString();
        assertTrue(actual.equals(expected) || actual.equals(expected2));
    }
}
