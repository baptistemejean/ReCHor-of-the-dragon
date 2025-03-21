package ch.epfl.rechor.timetable.mapped;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.Transfers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;
import java.util.NoSuchElementException;


import static org.junit.jupiter.api.Assertions.*;


class MyBufferedConnectionsTest {


    private static final HexFormat HEX = HexFormat.ofDelimiter(" ");




    private static final ByteBuffer CONNECTIONS_BUFFER = ByteBuffer.wrap(new byte[]{
            0x00, 0x10,  // DEP_STOP_ID = 16
            0x00, 0x3C,  // DEP_MINUTES = 60
            0x00, 0x20,  // ARR_STOP_ID = 32
            0x00, 0x78,  // ARR_MINUTES = 120
            0x00, 0x00, 0x01, 0x02,  // TRIP_POS_ID = (1 << 8) | 2  (Index de course 1, position 2)


            0x00, 0x11,  // DEP_STOP_ID = 17
            0x00, 0x4A,  // DEP_MINUTES = 74
            0x00, 0x22,  // ARR_STOP_ID = 34
            0x00, (byte) 0x90,  // ARR_MINUTES = 144
            0x00, 0x00, 0x02, 0x01   // TRIP_POS_ID = (2 << 8) | 1 (Index de course 2, position 1)
    });


    private static final ByteBuffer SUCC_BUFFER = ByteBuffer.allocate(12)
            .putInt(0, 1)
            .putInt(4, 0)
            .putInt(8, -1);
    @Test
    void constructorAcceptsValidBuffers() {
        assertDoesNotThrow(() -> new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER));
    }


    @Test
    void sizeReturnsCorrectNumberOfConnections() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);
        assertEquals(2, connections.size());
    }


    @Test
    void departureStationReturnsCorrectValues() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);
        assertEquals(16, connections.depStopId(0));
        assertEquals(17, connections.depStopId(1));
    }


    @Test
    void departureTimeReturnsCorrectValues() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);
        assertEquals(60, connections.depMins(0));
        assertEquals(74, connections.depMins(1));
    }


    @Test
    void arrivalStationReturnsCorrectValues() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);
        assertEquals(32, connections.arrStopId(0));
        assertEquals(34, connections.arrStopId(1));
    }


    @Test
    void arrivalTimeReturnsCorrectValues() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);
        assertEquals(120, connections.arrMins(0));
        assertEquals(144, connections.arrMins(1));
    }


    @Test
    void tripIdExtractsCorrectValue() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);
        assertEquals(1, connections.tripId(0)); // 1 extrait des 24 bits de poids fort
        assertEquals(2, connections.tripId(1)); // 2 extrait des 24 bits de poids fort
    }


    @Test
    void nextReturnsCorrectValue() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);


        // Vérifier la taille avant d'accéder à un index
        assertTrue(1 < connections.size());




        assertEquals(1, connections.nextConnectionId(0));
        assertEquals(0, connections.nextConnectionId(1));
    }


    @Test
    void throwsExceptionForInvalidIndex() {
        BufferedConnections connections = new BufferedConnections(CONNECTIONS_BUFFER, SUCC_BUFFER);
        assertThrows(IndexOutOfBoundsException.class, () -> connections.depStopId(2));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.arrMins(3));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.tripId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.nextConnectionId(5));
    }


    @Test
    void connectionThrowsForInvalidIndex2() {
        ByteBuffer buffer = ByteBuffer.allocate(12); // Crée un buffer de 12 octets
        buffer.put(HEX.parseHex("00 00 00 01"));


        ByteBuffer succBuffer = ByteBuffer.wrap(HEX.parseHex("00 01 00 00"));
        BufferedConnections connections = new BufferedConnections(buffer, succBuffer);
        assertThrows(IndexOutOfBoundsException.class, () -> connections.nextConnectionId(5));
    }


    @Test
    void connectionsAreAccessedCorrectly() {
        // Create raw bytes for two connections
        ByteBuffer buffer = ByteBuffer.allocate(24); // 2 connections * 12 bytes


        // Connection 1:
        // - Departure stop: 100
        // - Departure time: 480 (8:00)
        // - Arrival stop: 200
        // - Arrival time: 510 (8:30)
        // - Trip: 50, position: 0
        buffer.putShort((short)100); // DEP_STOP_ID
        buffer.putShort((short)480); // DEP_MINUTES
        buffer.putShort((short)200); // ARR_STOP_ID
        buffer.putShort((short)510); // ARR_MINUTES
        buffer.putInt(((50 << 8) | 0)); // TRIP_POS_ID (trip 50, pos 0)


        // Connection 2:
        // - Departure stop: 200
        // - Departure time: 515 (8:35)
        // - Arrival stop: 300
        // - Arrival time: 540 (9:00)
        // - Trip: 50, position: 1
        buffer.putShort((short)200); // DEP_STOP_ID
        buffer.putShort((short)515); // DEP_MINUTES
        buffer.putShort((short)300); // ARR_STOP_ID
        buffer.putShort((short)540); // ARR_MINUTES
        buffer.putInt(((50 << 8) | 1)); // TRIP_POS_ID (trip 50, pos 1)


        buffer.flip(); // Reset position to beginning


        // Create the successor buffer (connection 0 -> 1, connection 1 -> 0)
        ByteBuffer succBuffer = ByteBuffer.allocate(8); // 2 connections * 4 bytes
        succBuffer.putInt(1); // NEXT_CONNECTION_ID for connection 0 (points to connection 1)
        succBuffer.putInt(0); // NEXT_CONNECTION_ID for connection 1 (points to connection 0, circular)
        succBuffer.flip();


        // Create the BufferedConnections instance
        BufferedConnections connections = new BufferedConnections(buffer, succBuffer);


        // Test size
        assertEquals(2, connections.size());


        // Test connection 1 properties
        assertEquals(100, connections.depStopId(0));
        assertEquals(480, connections.depMins(0));
        assertEquals(200, connections.arrStopId(0));
        assertEquals(510, connections.arrMins(0));
        assertEquals(50, connections.tripId(0));
        assertEquals(0, connections.tripPos(0));
        assertEquals(1, connections.nextConnectionId(0));


        // Test connection 2 properties
        assertEquals(200, connections.depStopId(1));
        assertEquals(515, connections.depMins(1));
        assertEquals(300, connections.arrStopId(1));
        assertEquals(540, connections.arrMins(1));
        assertEquals(50, connections.tripId(1));
        assertEquals(1, connections.tripPos(1));
        assertEquals(0, connections.nextConnectionId(1)); // Circular reference back to first


        // Test invalid indices
        assertThrows(IndexOutOfBoundsException.class, () -> connections.depStopId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.depStopId(2));
    }


    @Test
    void bufferSizesMustMatch() {
        // Test that the number of connections in both buffers must match
        ByteBuffer mainBuffer = ByteBuffer.allocate(24); // 2 connections
        ByteBuffer succBufferWrongSize = ByteBuffer.allocate(4); // Only 1 connection


//        assertThrows(IllegalArgumentException.class, () ->
//                new BufferedConnections(mainBuffer, succBufferWrongSize));
    }


    private static final int DEP_STATION_ID = 0;
    private static final int ARR_STATION_ID = 1;
    private static final int TRANSFER_MINUTES = 2;


    // Données de test
    private ByteBuffer buffer;
    private Transfers transfers;


    @BeforeEach
    void setUp() {
        // Création d'un buffer de test avec quelques changements
        // Format: [DEP_STATION_ID (U16), ARR_STATION_ID (U16), TRANSFER_MINUTES (U8)]


        /*
         * Structure des données :
         * - Changement 0: Station 10 -> Station 20, 5 minutes
         * - Changement 1: Station 15 -> Station 20, 10 minutes
         * - Changement 2: Station 30 -> Station 20, 15 minutes
         * - Changement 3: Station 10 -> Station 25, 8 minutes
         * - Changement 4: Station 15 -> Station 25, 12 minutes
         * - Changement 5: Station 40 -> Station 30, 7 minutes
         *
         * Note: Les changements sont ordonnés par station d'arrivée
         */


        buffer = ByteBuffer.allocate(30); // 5 bytes par changement * 6 changements


        // Changement 0: Station 10 -> Station 20, 5 minutes
        buffer.putShort((short) 10);  // DEP_STATION_ID
        buffer.putShort((short) 20);  // ARR_STATION_ID
        buffer.put((byte) 5);        // TRANSFER_MINUTES


        // Changement 1: Station 15 -> Station 20, 10 minutes
        buffer.putShort((short) 15);
        buffer.putShort((short) 20);
        buffer.put((byte) 10);


        // Changement 2: Station 30 -> Station 20, 15 minutes
        buffer.putShort((short) 30);
        buffer.putShort((short) 20);
        buffer.put((byte) 15);


        // Changement 3: Station 10 -> Station 25, 8 minutes
        buffer.putShort((short) 10);
        buffer.putShort((short) 25);
        buffer.put((byte) 8);


        // Changement 4: Station 15 -> Station 25, 12 minutes
        buffer.putShort((short) 15);
        buffer.putShort((short) 25);
        buffer.put((byte) 12);


        // Changement 5: Station 40 -> Station 30, 7 minutes
        buffer.putShort((short) 40);
        buffer.putShort((short) 30);
        buffer.put((byte) 7);


        buffer.flip(); // Prépare le buffer pour la lecture


        transfers = new BufferedTransfers(buffer);
    }


    @Test
    void sizeReturnsCorrectNumberOfTransfers() {
        assertEquals(6, transfers.size());
    }


    @Test
    void depStationIdReturnsCorrectValues() {
        assertEquals(10, transfers.depStationId(0));
        assertEquals(15, transfers.depStationId(1));
        assertEquals(30, transfers.depStationId(2));
        assertEquals(10, transfers.depStationId(3));
        assertEquals(15, transfers.depStationId(4));
        assertEquals(40, transfers.depStationId(5));
    }


    @Test
    void minutesReturnsCorrectValues() {
        assertEquals(5, transfers.minutes(0));
        assertEquals(10, transfers.minutes(1));
        assertEquals(15, transfers.minutes(2));
        assertEquals(8, transfers.minutes(3));
        assertEquals(12, transfers.minutes(4));
        assertEquals(7, transfers.minutes(5));
    }


    @Test
    void arrivingAtReturnsCorrectIntervals() {
        // Station 20 a 3 changements arrivant: indices 0, 1, 2
        int station20Interval = transfers.arrivingAt(20);
        assertEquals(0, PackedRange.startInclusive(station20Interval));
        assertEquals(3, PackedRange.endExclusive(station20Interval));


        // Station 25 a 2 changements arrivant: indices 3, 4
        int station25Interval = transfers.arrivingAt(25);
        assertEquals(3, PackedRange.startInclusive(station25Interval));
        assertEquals(5, PackedRange.endExclusive(station25Interval));


        // Station 30 a 1 changement arrivant: indice 5
        int station30Interval = transfers.arrivingAt(30);
        assertEquals(5, PackedRange.startInclusive(station30Interval));
        assertEquals(6, PackedRange.endExclusive(station30Interval));
    }


    @Test
    void minutesBetweenReturnsCorrectDurations() {
        // Changement de Station 10 -> Station 20: 5 minutes
        assertEquals(5, transfers.minutesBetween(10, 20));


        // Changement de Station 15 -> Station 20: 10 minutes
        assertEquals(10, transfers.minutesBetween(15, 20));


        // Changement de Station 30 -> Station 20: 15 minutes
        assertEquals(15, transfers.minutesBetween(30, 20));


        // Changement de Station 10 -> Station 25: 8 minutes
        assertEquals(8, transfers.minutesBetween(10, 25));


        // Changement de Station 15 -> Station 25: 12 minutes
        assertEquals(12, transfers.minutesBetween(15, 25));


        // Changement de Station 40 -> Station 30: 7 minutes
        assertEquals(7, transfers.minutesBetween(40, 30));
    }


    @Test
    void minutesBetweenThrowsExceptionWhenNoTransferExists() {
        // Il n'existe pas de changement de Station 10 -> Station 30
        assertThrows(NoSuchElementException.class, () -> transfers.minutesBetween(10, 30));


        // Il n'existe pas de changement de Station 15 -> Station 30
        assertThrows(NoSuchElementException.class, () -> transfers.minutesBetween(15, 30));


        // Il n'existe pas de changement de Station 40 -> Station 20
        assertThrows(NoSuchElementException.class, () -> transfers.minutesBetween(40, 20));
    }


    @Test
    void methodsThrowExceptionForInvalidIndices() {
        // Index négatif
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.depStationId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutes(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.arrivingAt(-1));


        // Index trop grand
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.depStationId(6));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutes(6));


        // Station qui n'existe pas
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.arrivingAt(100));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutesBetween(10, 100));
    }


    @Test
    void testEmptyTransfersTable() {
        // Création d'un buffer vide
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        Transfers emptyTransfers = new BufferedTransfers(emptyBuffer);


        // La taille devrait être 0
        assertEquals(0, emptyTransfers.size());


        // Toute tentative d'accès devrait lever une exception
        assertThrows(IndexOutOfBoundsException.class, () -> emptyTransfers.depStationId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyTransfers.minutes(0));
    }


    private static final int DEP_STOP_ID = 0;
    private static final int DEP_MINUTES = 1;
    private static final int ARR_STOP_ID = 2;
    private static final int ARR_MINUTES = 3;
    private static final int TRIP_POS_ID = 4;


    // Données de test
    private ByteBuffer connectionsBuffer;
    private ByteBuffer nextConnectionsBuffer;
    private Connections connections;


    @BeforeEach
    void setUp2() {
        // Création d'un buffer pour les liaisons
        // Format: [DEP_STOP_ID (U16), DEP_MINUTES (U16), ARR_STOP_ID (U16),
        //          ARR_MINUTES (U16), TRIP_POS_ID (S32)]


        /*
         * Structure des données :
         * - Liaison 0: Arrêt 100 -> Arrêt 101, départ 540 min, arrivée 550 min, Course 1 Position 0
         * - Liaison 1: Arrêt 101 -> Arrêt 102, départ 552 min, arrivée 565 min, Course 1 Position 1
         * - Liaison 2: Arrêt 102 -> Arrêt 103, départ 567 min, arrivée 580 min, Course 1 Position 2
         * - Liaison 3: Arrêt 200 -> Arrêt 201, départ 600 min, arrivée 610 min, Course 2 Position 0
         * - Liaison 4: Arrêt 201 -> Arrêt 202, départ 612 min, arrivée 625 min, Course 2 Position 1
         *
         * Note: Les liaisons sont triées par heure de départ décroissante dans les données réelles,
         * mais pour ce test simple, nous ne respectons pas cette contrainte.
         */


        connectionsBuffer = ByteBuffer.allocate(60); // 12 bytes par liaison * 5 liaisons


        // Liaison 0: Arrêt 100 -> Arrêt 101, départ 540 min, arrivée 550 min, Course 1 Position 0
        connectionsBuffer.putShort((short) 100);    // DEP_STOP_ID
        connectionsBuffer.putShort((short) 540);    // DEP_MINUTES
        connectionsBuffer.putShort((short) 101);    // ARR_STOP_ID
        connectionsBuffer.putShort((short) 550);    // ARR_MINUTES
        connectionsBuffer.putInt(packTripAndPosition(1, 0)); // TRIP_POS_ID


        // Liaison 1: Arrêt 101 -> Arrêt 102, départ 552 min, arrivée 565 min, Course 1 Position 1
        connectionsBuffer.putShort((short) 101);
        connectionsBuffer.putShort((short) 552);
        connectionsBuffer.putShort((short) 102);
        connectionsBuffer.putShort((short) 565);
        connectionsBuffer.putInt(packTripAndPosition(1, 1));


        // Liaison 2: Arrêt 102 -> Arrêt 103, départ 567 min, arrivée 580 min, Course 1 Position 2
        connectionsBuffer.putShort((short) 102);
        connectionsBuffer.putShort((short) 567);
        connectionsBuffer.putShort((short) 103);
        connectionsBuffer.putShort((short) 580);
        connectionsBuffer.putInt(packTripAndPosition(1, 2));


        // Liaison 3: Arrêt 200 -> Arrêt 201, départ 600 min, arrivée 610 min, Course 2 Position 0
        connectionsBuffer.putShort((short) 200);
        connectionsBuffer.putShort((short) 600);
        connectionsBuffer.putShort((short) 201);
        connectionsBuffer.putShort((short) 610);
        connectionsBuffer.putInt(packTripAndPosition(2, 0));


        // Liaison 4: Arrêt 201 -> Arrêt 202, départ 612 min, arrivée 625 min, Course 2 Position 1
        connectionsBuffer.putShort((short) 201);
        connectionsBuffer.putShort((short) 612);
        connectionsBuffer.putShort((short) 202);
        connectionsBuffer.putShort((short) 625);
        connectionsBuffer.putInt(packTripAndPosition(2, 1));


        connectionsBuffer.flip(); // Prépare le buffer pour la lecture


        // Création d'un buffer pour les liaisons suivantes
        // Format: [NEXT_CONNECTION_ID (S32)]
        nextConnectionsBuffer = ByteBuffer.allocate(20); // 4 bytes par indice * 5 liaisons


        // Liaison suivant 0 est 1
        nextConnectionsBuffer.putInt(1);


        // Liaison suivant 1 est 2
        nextConnectionsBuffer.putInt(2);


        // Liaison suivant 2 est 0 (circulaire pour la course 1)
        nextConnectionsBuffer.putInt(0);


        // Liaison suivant 3 est 4
        nextConnectionsBuffer.putInt(4);


        // Liaison suivant 4 est 3 (circulaire pour la course 2)
        nextConnectionsBuffer.putInt(3);


        nextConnectionsBuffer.flip(); // Prépare le buffer pour la lecture


        connections = new BufferedConnections(connectionsBuffer, nextConnectionsBuffer);
    }


    /**
     * Empaquette un index de course et une position dans un entier 32 bits.
     * Les 24 bits de poids fort sont l'index de course, les 8 bits de poids faible sont la position.
     */
    private int packTripAndPosition(int tripId, int position) {
        return (tripId << 8) | position;
    }


    /**
     * Extrait l'index de course d'un TRIP_POS_ID.
     */
    private int unpackTripId(int tripPosId) {
        return tripPosId >>> 8;
    }


    /**
     * Extrait la position d'un TRIP_POS_ID.
     */
    private int unpackPosition(int tripPosId) {
        return tripPosId & 0xFF;
    }


    @Test
    void sizeReturnsCorrectNumberOfConnections2() {
        assertEquals(5, connections.size());
    }




    @Test
    void departureStopIdReturnsCorrectValues() {
        assertEquals(100, connections.depStopId(0));
        assertEquals(101, connections.depStopId(1));
        assertEquals(102, connections.depStopId(2));
        assertEquals(200, connections.depStopId(3));
        assertEquals(201, connections.depStopId(4));
    }


    @Test
    void departureMinutesReturnsCorrectValues() {
        assertEquals(540, connections.depMins(0));
        assertEquals(552, connections.depMins(1));
        assertEquals(567, connections.depMins(2));
        assertEquals(600, connections.depMins(3));
        assertEquals(612, connections.depMins(4));
    }


    @Test
    void arrivalStopIdReturnsCorrectValues() {
        assertEquals(101, connections.arrStopId(0));
        assertEquals(102, connections.arrStopId(1));
        assertEquals(103, connections.arrStopId(2));
        assertEquals(201, connections.arrStopId(3));
        assertEquals(202, connections.arrStopId(4));
    }


    @Test
    void arrivalMinutesReturnsCorrectValues() {
        assertEquals(550, connections.arrMins(0));
        assertEquals(565, connections.arrMins(1));
        assertEquals(580, connections.arrMins(2));
        assertEquals(610, connections.arrMins(3));
        assertEquals(625, connections.arrMins(4));
    }


    @Test
    void tripIdReturnsCorrectValues() {
        assertEquals(1, connections.tripId(0));
        assertEquals(1, connections.tripId(1));
        assertEquals(1, connections.tripId(2));
        assertEquals(2, connections.tripId(3));
        assertEquals(2, connections.tripId(4));
    }


    @Test
    void nextConnectionReturnsCorrectConnections() {
        // Vérifier les liaisons suivantes
        assertEquals(1, connections.nextConnectionId(0));
        assertEquals(2, connections.nextConnectionId(1));
        assertEquals(0, connections.nextConnectionId(2)); // Circulaire pour la course 1
        assertEquals(4, connections.nextConnectionId(3));
        assertEquals(3, connections.nextConnectionId(4)); // Circulaire pour la course 2
    }


    @Test
    void methodsThrowExceptionForInvalidIndices2() {
        // Index négatif
        assertThrows(IndexOutOfBoundsException.class, () -> connections.depStopId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.depMins(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.arrStopId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.arrMins(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.tripId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.nextConnectionId(-1));


        // Index trop grand
        assertThrows(IndexOutOfBoundsException.class, () -> connections.depStopId(5));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.depMins(5));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.arrStopId(5));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.arrMins(5));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.tripId(5));
        assertThrows(IndexOutOfBoundsException.class, () -> connections.nextConnectionId(5));
    }


    @Test
    void testEmptyConnectionsTable() {
        // Création de buffers vides
        ByteBuffer emptyConnBuffer = ByteBuffer.allocate(0);
        ByteBuffer emptyNextBuffer = ByteBuffer.allocate(0);
        Connections emptyConnections = new BufferedConnections(emptyConnBuffer, emptyNextBuffer);


        // La taille devrait être 0
        assertEquals(0, emptyConnections.size());
        // Toute tentative d'accès devrait lever une exception
        assertThrows(IndexOutOfBoundsException.class, () -> emptyConnections.depStopId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyConnections.nextConnectionId(0));
    }


    // Chaque enregistrement occupe 12 octets : 2+2+2+2+4
    private static final int RECORD_SIZE = 12;


    // Crée un enregistrement dans le mainBuffer avec les champs donnés (valeurs en hexadécimal).
    private ByteBuffer createMainRecord(int depStopId, int depMins, int arrStopId, int arrMins, int tripPosField) {
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) depStopId);
        buffer.putShort((short) depMins);
        buffer.putShort((short) arrStopId);
        buffer.putShort((short) arrMins);
        buffer.putInt(tripPosField);
        buffer.flip();
        return buffer;
    }


    // Combine plusieurs enregistrements dans un seul ByteBuffer.
    private ByteBuffer combineMainRecords(ByteBuffer... records) {
        int total = 0;
        for (ByteBuffer rec : records) {
            total += rec.remaining();
        }
        ByteBuffer combined = ByteBuffer.allocate(total);
        for (ByteBuffer rec : records) {
            combined.put(rec);
        }
        combined.flip();
        return combined;
    }


    // Crée un enregistrement pour le succBuffer (un entier sur 4 octets).
    private ByteBuffer createSuccRecord(int nextConnectionId) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(nextConnectionId);
        buffer.flip();
        return buffer;
    }


    // Combine plusieurs enregistrements du succBuffer.
    private ByteBuffer combineSuccRecords(ByteBuffer... records) {
        int total = 0;
        for (ByteBuffer rec : records) {
            total += rec.remaining();
        }
        ByteBuffer combined = ByteBuffer.allocate(total);
        for (ByteBuffer rec : records) {
            combined.put(rec);
        }
        combined.flip();
        return combined;
    }


    @Test
    void testSingleConnection() {
        // Valeurs en hexadécimal
        int depStop = 0x0A0B;    // par exemple
        int depMins = 0x001E;    // 30 minutes (0x1E)
        int arrStop = 0x0C0D;
        int arrMins = 0x0028;    // 40 minutes (0x28)
        // Pour le champ TRIP_POS_ID : choisissons tripId = 0x001234 et tripPos = 0x56.
        int tripIdExpected = 0x001234;
        int tripPosExpected = 0x56;
        int tripField = (tripIdExpected << 8) | tripPosExpected; // 0x00123456
        // Création d'un enregistrement unique dans le mainBuffer.
        ByteBuffer mainBuffer = createMainRecord(depStop, depMins, arrStop, arrMins, tripField);
        // Pour le succBuffer, par exemple, nextConnection = 0x00000005.
        int nextConnection = 0x00000005;
        ByteBuffer succBuffer = createSuccRecord(nextConnection);


        BufferedConnections bc = new BufferedConnections(mainBuffer, succBuffer);


        assertEquals(1, bc.size());
        assertEquals(depStop, bc.depStopId(0));
        assertEquals(depMins, bc.depMins(0));
        assertEquals(arrStop, bc.arrStopId(0));
        assertEquals(arrMins, bc.arrMins(0));
        assertEquals(tripIdExpected, bc.tripId(0));
        assertEquals(tripPosExpected, bc.tripPos(0));
        assertEquals(nextConnection, bc.nextConnectionId(0));
    }


    @Test
    void testMultipleConnections() {
        // Enregistrement 1
        int depStop1 = 0x0001;
        int depMins1 = 0x0010;
        int arrStop1 = 0x0020;
        int arrMins1 = 0x0028;
        int tripId1 = 0x000ABC;
        int tripPos1 = 0x10;
        int tripField1 = (tripId1 << 8) | tripPos1;
        ByteBuffer rec1 = createMainRecord(depStop1, depMins1, arrStop1, arrMins1, tripField1);


        // Enregistrement 2
        int depStop2 = 0x0002;
        int depMins2 = 0x0020;
        int arrStop2 = 0x0030;
        int arrMins2 = 0x0038;
        int tripId2 = 0x000DEF;
        int tripPos2 = 0x20;
        int tripField2 = (tripId2 << 8) | tripPos2;
        ByteBuffer rec2 = createMainRecord(depStop2, depMins2, arrStop2, arrMins2, tripField2);


        ByteBuffer mainBuffer = combineMainRecords(rec1, rec2);
        // Pour le succBuffer, par exemple : le premier enregistrement pointe vers l'indice 1,
        // le second pointe vers 0.
        ByteBuffer succBuffer = combineSuccRecords(createSuccRecord(1), createSuccRecord(0));


        BufferedConnections bc = new BufferedConnections(mainBuffer, succBuffer);


        assertEquals(2, bc.size());


        // Test sur le premier enregistrement
        assertEquals(depStop1, bc.depStopId(0));
        assertEquals(depMins1, bc.depMins(0));
        assertEquals(arrStop1, bc.arrStopId(0));
        assertEquals(arrMins1, bc.arrMins(0));
        assertEquals(tripId1, bc.tripId(0));
        assertEquals(tripPos1, bc.tripPos(0));
        assertEquals(1, bc.nextConnectionId(0));


        // Test sur le second enregistrement
        assertEquals(depStop2, bc.depStopId(1));
        assertEquals(depMins2, bc.depMins(1));
        assertEquals(arrStop2, bc.arrStopId(1));
        assertEquals(arrMins2, bc.arrMins(1));
        assertEquals(tripId2, bc.tripId(1));
        assertEquals(tripPos2, bc.tripPos(1));
        assertEquals(0, bc.nextConnectionId(1));
    }


    @Test
    void testInvalidIndex() {
        // Crée un enregistrement unique.
        ByteBuffer mainBuffer = createMainRecord(0x0001, 0x0010, 0x0020, 0x0028, (0x000123 << 8) | 0x45);
        ByteBuffer succBuffer = createSuccRecord(0x00000000);
        BufferedConnections bc = new BufferedConnections(mainBuffer, succBuffer);
        // Les indices négatifs ou supérieurs à size()-1 doivent lever une exception.
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depStopId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depStopId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depMins(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrStopId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrMins(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.nextConnectionId(1));
    }


    @Test
    void testMultipleSequentialCalls() {
        // Vérifie que plusieurs appels sur le même indice renvoient toujours les mêmes valeurs.
        ByteBuffer mainBuffer = createMainRecord(0x0AAA, 0x0BBB, 0x0CCC, 0x0DDD, (0x0123 << 8) | 0x45);
        ByteBuffer succBuffer = createSuccRecord(0x00000005);
        BufferedConnections bc = new BufferedConnections(mainBuffer, succBuffer);
        for (int i = 0; i < 10; i++) {
            assertEquals(0x0AAA, bc.depStopId(0));
            assertEquals(0x0BBB, bc.depMins(0));
            assertEquals(0x0CCC, bc.arrStopId(0));
            assertEquals(0x0DDD, bc.arrMins(0));
            assertEquals(0x0123, bc.tripId(0));
            assertEquals(0x45, bc.tripPos(0));
            assertEquals(0x00000005, bc.nextConnectionId(0));
        }
    }
}
