package ch.epfl.rechor.timetable.mapped;


import ch.epfl.rechor.journey.Vehicle;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;


class MyBufferedRoutesTest {


    @Test
    void constructorHandlesValidData() {
        ByteBuffer buffer = ByteBuffer.allocate(3 * 3); // 3 lignes, 3 octets chacune (U16 + U8)
        buffer.putShort(0, (short) 0);
        buffer.put(2, (byte) 1);
        buffer.putShort(3, (short) 2);
        buffer.put(5, (byte) 3);
        buffer.putShort(6, (short) 4);
        buffer.put(8, (byte) 5);


        List<String> stringTable = List.of("RouteA", "RouteB", "RouteC", "RouteD", "RouteE");
        BufferedRoutes routes = new BufferedRoutes(stringTable, buffer);


        assertEquals(3, routes.size());
    }


    @Test
    void nameReturnsCorrectValues() {
        ByteBuffer buffer = ByteBuffer.allocate(3 * 3);
        buffer.putShort(0, (short) 0);
        buffer.put(2, (byte) 1);
        buffer.putShort(3, (short) 2);
        buffer.put(5, (byte) 3);
        buffer.putShort(6, (short) 4);
        buffer.put(8, (byte) 5);


        List<String> stringTable = List.of("RouteA", "RouteB", "RouteC", "RouteD", "RouteE");
        BufferedRoutes routes = new BufferedRoutes(stringTable, buffer);


        assertEquals("RouteA", routes.name(0));
        assertEquals("RouteC", routes.name(1));
        assertEquals("RouteE", routes.name(2));
    }


    @Test
    void vehicleReturnsCorrectValues() {
        ByteBuffer buffer = ByteBuffer.allocate(3 * 3);
        buffer.putShort(0, (short) 0);
        buffer.put(2, (byte) 0); // TRAM
        buffer.putShort(3, (short) 1);
        buffer.put(5, (byte) 2); // TRAIN
        buffer.putShort(6, (short) 2);
        buffer.put(8, (byte) 5); // AERIAL_LIFT


        List<String> stringTable = List.of("RouteA", "RouteB", "RouteC");
        BufferedRoutes routes = new BufferedRoutes(stringTable, buffer);


        assertEquals(Vehicle.TRAM, routes.vehicle(0));
        assertEquals(Vehicle.TRAIN, routes.vehicle(1));
        assertEquals(Vehicle.AERIAL_LIFT, routes.vehicle(2));
    }


    @Test
    void nameThrowsExceptionForInvalidIndex() {
        ByteBuffer buffer = ByteBuffer.allocate(3 * 3);
        List<String> stringTable = List.of("RouteA", "RouteB", "RouteC");
        BufferedRoutes routes = new BufferedRoutes(stringTable, buffer);


        assertThrows(IndexOutOfBoundsException.class, () -> routes.name(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> routes.name(3));
    }


    @Test
    void vehicleThrowsExceptionForInvalidIndex() {
        ByteBuffer buffer = ByteBuffer.allocate(3 * 3);
        List<String> stringTable = List.of("RouteA", "RouteB", "RouteC");
        BufferedRoutes routes = new BufferedRoutes(stringTable, buffer);


        assertThrows(IndexOutOfBoundsException.class, () -> routes.vehicle(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> routes.vehicle(3));
    }


    @Test
    void vehicleHandlesAllEnumCases() {
        ByteBuffer buffer = ByteBuffer.allocate(7 * 3);
        for (int i = 0; i < 7; i++) {
            buffer.putShort(i * 3, (short) i);
            buffer.put(i * 3 + 2, (byte) i);
        }


        List<String> stringTable = List.of("RouteA", "RouteB", "RouteC", "RouteD", "RouteE", "RouteF", "RouteG");
        BufferedRoutes routes = new BufferedRoutes(stringTable, buffer);


        assertEquals(Vehicle.TRAM, routes.vehicle(0));
        assertEquals(Vehicle.METRO, routes.vehicle(1));
        assertEquals(Vehicle.TRAIN, routes.vehicle(2));
        assertEquals(Vehicle.BUS, routes.vehicle(3));
        assertEquals(Vehicle.FERRY, routes.vehicle(4));
        assertEquals(Vehicle.AERIAL_LIFT, routes.vehicle(5));
        assertEquals(Vehicle.FUNICULAR, routes.vehicle(6));
    }
}
