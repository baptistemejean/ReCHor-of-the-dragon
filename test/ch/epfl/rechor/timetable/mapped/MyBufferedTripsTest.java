package ch.epfl.rechor.timetable.mapped;


import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;


class MyBufferedTripsTest {
    // Exemples de données pour le test
    private static final List<String> STRING_TABLE = List.of("Gare A", "Gare B", "Gare C");


    // Structure des courses aplaties : ROUTE_ID (U16) + DESTINATION_ID (U16)
    private static final HexFormat HEX = HexFormat.ofDelimiter(" ");


    @Test
    void constructorRejectsInvalidBufferSize() {
        ByteBuffer buffer = ByteBuffer.allocate(3); // Doit être un multiple de 4 (2 + 2)
        assertThrows(IllegalArgumentException.class, () -> new BufferedTrips(STRING_TABLE, buffer));
    }


    @Test
    void sizeComputesCorrectly() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 01 00 02 00 03 00 01")); // 2 éléments (2x4 octets)
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertEquals(2, trips.size());
    }


    @Test
    void destinationReturnsCorrectName() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 02 00 01 00 00")); // 2 éléments
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertEquals("Gare C", trips.destination(0)); // Destination de la 1ère course
        assertEquals("Gare A", trips.destination(1)); // Destination de la 2ème course
    }


    @Test
    void destinationThrowsExceptionForInvalidIndex() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 01 00 02"));
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(1)); // Id max = 0
    }


    @Test
    void destinationThrowsExceptionForInvalidStringIndex() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 05")); // Index 5 hors limites de STRING_TABLE
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(0));
    }




    /**
     * Vérifie que le constructeur accepte un buffer valide.
     */
    @Test
    void constructorAcceptsValidBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 02 00 01 00 00")); // 2 éléments
        assertDoesNotThrow(() -> new BufferedTrips(STRING_TABLE, buffer));
    }


    /**
     * Vérifie que le constructeur rejette un buffer dont la taille n'est pas un multiple de la structure.
     */
    @Test
    void constructorRejectsInvalidBufferSize2() {
        ByteBuffer buffer = ByteBuffer.allocate(3); // Taille invalide
        assertThrows(IllegalArgumentException.class, () -> new BufferedTrips(STRING_TABLE, buffer));
    }


    /**
     * Vérifie que size() retourne le bon nombre de courses.
     */
    @Test
    void sizeComputesCorrectly2() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 02 00 01 00 00")); // 2 courses
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertEquals(2, trips.size());
    }


    /**
     * Vérifie que routeId() retourne l'index de la ligne correcte pour chaque course.
     */
    @Test
    void routeIdReturnsCorrectValues() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 02 00 01 00 00")); // 2 éléments
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertEquals(0, trips.routeId(0)); // Route de la 1ère course
        assertEquals(1, trips.routeId(1)); // Route de la 2ème course
    }


    /**
     * Vérifie que destination() retourne le bon nom pour chaque course.
     */
    @Test
    void destinationReturnsCorrectValues() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 02 00 01 00 00")); // 2 éléments
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertEquals("Gare C", trips.destination(0)); // Destination de la 1ère course
        assertEquals("Gare A", trips.destination(1)); // Destination de la 2ème course
    }


    /**
     * Vérifie que routeId() lève une exception si l'ID est invalide.
     */
    @Test
    void routeIdThrowsOnInvalidId() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 02 00 01 00 00")); // 2 éléments
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(2));
    }


    /**
     * Vérifie que destination() lève une exception si l'ID est invalide.
     */
    @Test
    void destinationThrowsOnInvalidId() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 02 00 01 00 00")); // 2 éléments
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(2));
    }


    /**
     * Vérifie qu'une exception est levée si l'index de destination est hors limites.
     */
    @Test
    void destinationThrowsOnInvalidDestinationIndex() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 00 05 00 01 00 00")); // Index 5 inexistant
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(0));
    }


    /**
     * Vérifie qu'une exception est levée si l'index de route est hors limites.
     */
    @Test
    void routeIdThrowsOnInvalidRouteIndex() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("11 50 00 02 00 01 00 00")); // Route index 16 inexistant
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(30));
    }


    @Test
    void routeNameReturnsCorrectValue2() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 01")); // Route A, type inconnu
        BufferedRoutes routes = new BufferedRoutes(STRING_TABLE, buffer);
        assertEquals("Gare A", routes.name(0));
    }


    @Test
    void vehicleThrowsForInvalidIndex2() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 00 01"));
        BufferedRoutes routes = new BufferedRoutes(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> routes.vehicle(10));
    }


    // --- Tests pour BufferedTrips ---
    @Test
    void tripDestinationReturnsCorrectValue2() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 01 00 02")); // Trip 0 -> Route 1, Destination 2
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertEquals("Gare C", trips.destination(0));
    }


    @Test
    void tripRouteIdReturnsCorrectValue2() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 01 00 02"));
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertEquals(1, trips.routeId(0));
    }


    @Test
    void tripThrowsForInvalidIndex2() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex("00 01 00 02"));
        BufferedTrips trips = new BufferedTrips(STRING_TABLE, buffer);
        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(10));
    }


    // Exemple de données de courses (trips)
    // Format: ROUTE_ID (U16) | DESTINATION_ID (U16)
    private static final byte[] TRIPS_BYTES = {
            0x00, 0x01, 0x00, 0x04,  // Route d'index 1, destination "Lausanne" (index 4)
            0x00, 0x02, 0x00, 0x02,  // Route d'index 2, destination "Anet" (index 2)
            0x00, 0x00, 0x00, 0x06   // Route d'index 0, destination "Palézieux" (index 6)
    };


    private static final List<String> STRING_TABLE2 = List.of(
            "1", "70", "Anet", "Ins", "Lausanne", "Losanna", "Palézieux"
    );


    @Test
    void sizeReturnsCorrectNumberOfTrips() {
        ByteBuffer buffer = ByteBuffer.wrap(TRIPS_BYTES);
        BufferedTrips trips = new BufferedTrips(STRING_TABLE2, buffer);


        assertEquals(3, trips.size());
    }


    @Test
    void routeIdReturnsCorrectRouteIndex() {
        ByteBuffer buffer = ByteBuffer.wrap(TRIPS_BYTES);
        BufferedTrips trips = new BufferedTrips(STRING_TABLE2, buffer);


        assertEquals(1, trips.routeId(0));
        assertEquals(2, trips.routeId(1));
        assertEquals(0, trips.routeId(2));
    }


    @Test
    void destinationReturnsCorrectDestinationName() {
        ByteBuffer buffer = ByteBuffer.wrap(TRIPS_BYTES);
        BufferedTrips trips = new BufferedTrips(STRING_TABLE2, buffer);


        assertEquals("Lausanne", trips.destination(0));
        assertEquals("Anet", trips.destination(1));
        assertEquals("Palézieux", trips.destination(2));
    }


    @Test
    void throwsExceptionForInvalidIndex() {
        ByteBuffer buffer = ByteBuffer.wrap(TRIPS_BYTES);
        BufferedTrips trips = new BufferedTrips(STRING_TABLE2, buffer);


        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(3));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(3));
    }
}
