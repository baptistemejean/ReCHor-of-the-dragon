package ch.epfl.rechor.timetable.mapped;


import ch.epfl.rechor.PackedRange;
import org.junit.jupiter.api.Test;


import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.NoSuchElementException;


import static org.junit.jupiter.api.Assertions.*;


class MyBufferedTransfersTest {
    private static final HexFormat HEX = HexFormat.ofDelimiter(" ");
    private static final ByteBuffer BUFFER = ByteBuffer.wrap(HEX.parseHex(
            "00 01 00 02 05 " +          // Changement de 1 → 2 en 5 minutes
                    "00 03 00 02 0A " +  // Changement de 3 → 2 en 10 minutes
                    "00 04 00 02 0F " +  // Changement de 4 → 2 en 15 minutes
                    "00 02 00 05 14"    // Changement de 2 → 5 en 20 minutes
    ));


    private final BufferedTransfers transfers = new BufferedTransfers(BUFFER);


    @Test
    void sizeReturnsCorrectValue() {
        assertEquals(4, transfers.size());
    }


    @Test
    void depStationIdReturnsCorrectValues() {
        assertEquals(1, transfers.depStationId(0));
        assertEquals(3, transfers.depStationId(1));
        assertEquals(4, transfers.depStationId(2));
        assertEquals(2, transfers.depStationId(3));
    }


    @Test
    void arrivingAtReturnsCorrectInterval() {
        int interval = transfers.arrivingAt(2);
        assertEquals(0, PackedRange.startInclusive(interval));
        assertEquals(3, PackedRange.endExclusive(interval));


        int interval2 = transfers.arrivingAt(5);
        assertEquals(3, PackedRange.startInclusive(interval2));
        assertEquals(4, PackedRange.endExclusive(interval2));
    }


    @Test
    void arrivingAtThrowsForInvalidStationId() {
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.arrivingAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.arrivingAt(9999));
    }


    @Test
    void minutesReturnsCorrectValues() {
        assertEquals(5, transfers.minutes(0));
        assertEquals(10, transfers.minutes(1));
        assertEquals(15, transfers.minutes(2));
        assertEquals(20, transfers.minutes(3));
    }


    @Test
    void minutesBetweenReturnsCorrectValue() {
        assertEquals(5, transfers.minutesBetween(1, 2));
        assertEquals(10, transfers.minutesBetween(3, 2));
        assertEquals(15, transfers.minutesBetween(4, 2));
        assertEquals(20, transfers.minutesBetween(2, 5));
        assertThrows(NoSuchElementException.class, () -> transfers.minutesBetween(4, 5));


    }


    @Test
    void depStationIdThrowsForInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.depStationId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.depStationId(999));
    }


    @Test
    void minutesThrowsForInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutes(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutes(999));
    }


    @Test
    void minutesBetweenHandlesInvalidCases() {
        assertThrows(NoSuchElementException.class, () -> transfers.minutesBetween(99, 2));
        // Correction pour se conformer à la documentation
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutesBetween(1, 99));
    }


    @Test
    void constructorHandlesEmptyBuffer() {
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        BufferedTransfers emptyTransfers = new BufferedTransfers(emptyBuffer);
        assertEquals(0, emptyTransfers.size());
    }


    @Test
    void constructorRejectsInvalidBufferSize() {
        ByteBuffer invalidBuffer = ByteBuffer.allocate(7);
        assertThrows(IllegalArgumentException.class, () -> new BufferedTransfers(invalidBuffer));
    }


    @Test
    void arrivingAtHandlesMultipleTransfersToSameStation() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex(
                "00 01 00 02 05 " +
                        "00 03 00 02 10 " +
                        "00 04 00 02 15 " +
                        "00 05 00 02 20 " +
                        "00 06 00 03 25"
        ));
        BufferedTransfers multipleTransfers = new BufferedTransfers(buffer);


        int interval = multipleTransfers.arrivingAt(2);
        assertEquals(0, PackedRange.startInclusive(interval));
        assertEquals(4, PackedRange.endExclusive(interval));


        int interval3 = multipleTransfers.arrivingAt(3);
        assertEquals(4, PackedRange.startInclusive(interval3));
        assertEquals(5, PackedRange.endExclusive(interval3));
    }


    @Test
    void transfersAllowCircularConnections() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex(
                "00 01 00 01 07 " +
                        "00 02 00 02 05"
        ));
        BufferedTransfers circularTransfers = new BufferedTransfers(buffer);


        assertEquals(7, circularTransfers.minutesBetween(1, 1));
        assertEquals(5, circularTransfers.minutesBetween(2, 2));
    }


    @Test
    void allTransfersGoToSameStation() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex(
                "00 01 00 05 03 " +
                        "00 02 00 05 06 " +
                        "00 03 00 05 09 " +
                        "00 04 00 05 12 " +
                        "00 05 00 05 01"
        ));
        BufferedTransfers allToSame = new BufferedTransfers(buffer);


        int interval = allToSame.arrivingAt(5);
        assertEquals(0, PackedRange.startInclusive(interval));
        assertEquals(5, PackedRange.endExclusive(interval));
    }


    @Test
    void transfersHandleExtremeValues() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex(
                "00 01 00 02 00 " +
                        "00 02 00 03 FF"
        ));
        BufferedTransfers extremeTransfers = new BufferedTransfers(buffer);


        assertEquals(0, extremeTransfers.minutesBetween(1, 2));
        assertEquals(255, extremeTransfers.minutesBetween(2, 3));
    }


    @Test
    void constructorRejectsInvalidBuffers() {
        ByteBuffer tooSmallBuffer = ByteBuffer.wrap(HEX.parseHex("00 01"));
        ByteBuffer misalignedBuffer = ByteBuffer.wrap(HEX.parseHex("00 01 00 02 05 00"));


        assertThrows(IllegalArgumentException.class, () -> new BufferedTransfers(tooSmallBuffer));
        assertThrows(IllegalArgumentException.class, () -> new BufferedTransfers(misalignedBuffer));
    }


    @Test
    void allTransfersGoToSameStation2() {
        ByteBuffer buffer = ByteBuffer.wrap(HEX.parseHex(
                "00 01 00 05 03 " +
                        "00 02 00 05 06 " +
                        "00 03 00 05 09 " +
                        "00 04 00 05 12"
        ));
        BufferedTransfers allToSame = new BufferedTransfers(buffer);


        int interval = allToSame.arrivingAt(5);
        assertEquals(0, PackedRange.startInclusive(interval));
        assertEquals(4, PackedRange.endExclusive(interval));
    }


    @Test
    void constructorRejectsInvalidBufferSize2() {
        ByteBuffer invalidBuffer = ByteBuffer.allocate(7);
        assertThrows(IllegalArgumentException.class, () -> new BufferedTransfers(invalidBuffer));
    }


    @Test
    void constructorHandlesEmptyBuffer2() {
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        BufferedTransfers emptyTransfers = new BufferedTransfers(emptyBuffer);
        assertEquals(0, emptyTransfers.size());
    }


    @Test
    void arrivingAtThrowsForInvalidStationId2() {
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.arrivingAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.arrivingAt(9999));
    }


    // Exemple de données de changements
    // Les données sont ordonnées pour que les changements arrivant à la même gare soient consécutifs
    // Format: DEP_STATION_ID (U16) | ARR_STATION_ID (U16) | TRANSFER_MINUTES (U8)
    private static final byte[] TRANSFERS_BYTES = {
            // Gare d'arrivée 1
            0x00, 0x02, 0x00, 0x01, 0x05,  // Changement de la gare 2 vers gare 1, durée 5 min
            0x00, 0x03, 0x00, 0x01, 0x08,  // Changement de la gare 3 vers gare 1, durée 8 min


            // Gare d'arrivée 2
            0x00, 0x01, 0x00, 0x02, 0x04,  // Changement de la gare 1 vers gare 2, durée 4 min
            0x00, 0x04, 0x00, 0x02, 0x06,  // Changement de la gare 4 vers gare 2, durée 6 min


            // Gare d'arrivée 3
            0x00, 0x03, 0x00, 0x03, 0x03,  // Changement au sein de la gare 3, durée 3 min
            0x00, 0x04, 0x00, 0x03, 0x09   // Changement de la gare 4 vers gare 3, durée 9 min
    };


    private static final ByteBuffer BUFFER2 = ByteBuffer.wrap(HEX.parseHex(
            "00 02 00 01 05 " +
                    "00 03 00 01 08 " +
                    "00 01 00 02 04 " +
                    "00 04 00 02 06 " +
                    "00 03 00 03 03 " +
                    "00 04 00 03 09"
    ));


    private final BufferedTransfers transfers2 = new BufferedTransfers(BUFFER2);




    @Test
    void sizeReturnsCorrectNumberOfTransfers() {
        ByteBuffer buffer = ByteBuffer.wrap(TRANSFERS_BYTES);
        BufferedTransfers transfers = new BufferedTransfers(buffer);


        assertEquals(6, transfers.size());
    }


    @Test
    void depStationIdReturnsCorrectDepartureStationId() {
        ByteBuffer buffer = ByteBuffer.wrap(TRANSFERS_BYTES);
        BufferedTransfers transfers = new BufferedTransfers(buffer);


        assertEquals(2, transfers.depStationId(0));
        assertEquals(3, transfers.depStationId(1));
        assertEquals(1, transfers.depStationId(2));
        assertEquals(4, transfers.depStationId(3));
        assertEquals(3, transfers.depStationId(4));
        assertEquals(4, transfers.depStationId(5));
    }


    @Test
    void minutesReturnsCorrectTransferDuration() {
        ByteBuffer buffer = ByteBuffer.wrap(TRANSFERS_BYTES);
        BufferedTransfers transfers = new BufferedTransfers(buffer);


        assertEquals(5, transfers.minutes(0));
        assertEquals(8, transfers.minutes(1));
        assertEquals(4, transfers.minutes(2));
        assertEquals(6, transfers.minutes(3));
        assertEquals(3, transfers.minutes(4));
        assertEquals(9, transfers.minutes(5));
    }


    @Test
    void arrivingAtReturnsCorrectTransferInterval() {
        // Test pour la gare d'arrivée 1 (les deux premiers changements)
        int interval1 = transfers2.arrivingAt(1);


        // Vérifier que l'intervalle contient bien les index 0 et 1
        assertEquals(0, PackedRange.startInclusive(interval1));
        assertEquals(2, PackedRange.endExclusive(interval1));


        // Tester pour gare d'arrivée 2 (changements 2 et 3)
        int interval2 = transfers2.arrivingAt(2);
        assertEquals(2, PackedRange.startInclusive(interval2));
        assertEquals(4, PackedRange.endExclusive(interval2));


        // Tester pour gare d'arrivée 3 (changements 4 et 5)
        int interval3 = transfers2.arrivingAt(3);
        assertEquals(4, PackedRange.startInclusive(interval3));
        assertEquals(6, PackedRange.endExclusive(interval3));
    }


    @Test
    void minutesBetweenReturnsCorrectTransferTime() {
        ByteBuffer buffer = ByteBuffer.wrap(TRANSFERS_BYTES);
        BufferedTransfers transfers = new BufferedTransfers(buffer);


        // Test de changements existants
        assertEquals(5, transfers.minutesBetween(2, 1));
        assertEquals(8, transfers.minutesBetween(3, 1));
        assertEquals(4, transfers.minutesBetween(1, 2));
        assertEquals(6, transfers.minutesBetween(4, 2));
        assertEquals(3, transfers.minutesBetween(3, 3)); // Changement interne à une gare
        assertEquals(9, transfers.minutesBetween(4, 3));
    }


    @Test
    void minutesBetweenThrowsExceptionForNonExistingTransfer() {
        ByteBuffer buffer = ByteBuffer.wrap(TRANSFERS_BYTES);
        BufferedTransfers transfers = new BufferedTransfers(buffer);


        // Test de changements qui n'existent pas
        assertThrows(NoSuchElementException.class, () -> transfers.minutesBetween(1, 3));
    }


    @Test
    void throwsExceptionForInvalidIndex() {
        ByteBuffer buffer = ByteBuffer.wrap(TRANSFERS_BYTES);
        BufferedTransfers transfers = new BufferedTransfers(buffer);


        assertThrows(IndexOutOfBoundsException.class, () -> transfers.depStationId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.depStationId(6));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutes(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> transfers.minutes(6));
    }


    private static final int RECORD_SIZE = 5;


    /**
     * Crée un ByteBuffer contenant un enregistrement de transfert.
     * Les valeurs sont fournies en entier (on pourra utiliser des littéraux hexadécimaux)
     * et sont écrites dans le buffer au format binaire.
     */
    private ByteBuffer createTransferRecord(int fromStopId, int arrStopId, int transferMinutes) {
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
        buffer.putShort((short) fromStopId);
        buffer.putShort((short) arrStopId);
        buffer.put((byte) transferMinutes);
        buffer.flip();
        return buffer;
    }


    /**
     * Combine plusieurs enregistrements en un seul ByteBuffer.
     */
    private ByteBuffer combineTransferRecords(ByteBuffer... records) {
        int totalSize = 0;
        for (ByteBuffer rec : records) {
            totalSize += rec.remaining();
        }
        ByteBuffer combined = ByteBuffer.allocate(totalSize);
        for (ByteBuffer rec : records) {
            combined.put(rec);
        }
        combined.flip();
        return combined;
    }


    @Test
    void testSingleTransfer() {
        // Un enregistrement unique avec des valeurs en hexadécimal.
        int fromStop = 0x0A0B;   // par exemple 2571
        int arrStop  = 0x0010;   // 16
        int minutes  = 0x1E;     // 30 minutes


        ByteBuffer buffer = createTransferRecord(fromStop, arrStop, minutes);
        BufferedTransfers bt = new BufferedTransfers(buffer);


        // Vérification de la taille
        assertEquals(1, bt.size());
        // Vérification des champs
        assertEquals(fromStop, bt.depStationId(0));
        assertEquals(minutes, bt.minutes(0));


        // Vérification de arrivingAt : pour la station d'arrivée 0x0010,
        // on s'attend à avoir un intervalle couvrant l'enregistrement 0.
        int range = bt.arrivingAt(0x0010);
        // Si PackedRange est accessible, on peut vérifier ainsi :
        assertEquals(0, PackedRange.startInclusive(range));
        assertEquals(1, PackedRange.endExclusive(range));
    }


    @Test
    void testMultipleTransfers() {
        // Crée plusieurs enregistrements triés par ARR_STOP_ID (obligatoire pour la construction)
        // Enregistrement 0 : de 0x0001 vers 0x0010, 0x10 minutes
        ByteBuffer rec0 = createTransferRecord(0x0001, 0x0010, 0x10);
        // Enregistrement 1 : de 0x0002 vers 0x0010, 0x20 minutes
        ByteBuffer rec1 = createTransferRecord(0x0002, 0x0010, 0x20);
        // Enregistrement 2 : de 0x0001 vers 0x0020, 0x15 minutes
        ByteBuffer rec2 = createTransferRecord(0x0001, 0x0020, 0x15);
        // Combine-les (attention : les enregistrements doivent être triés par ARR_STOP_ID)
        ByteBuffer buffer = combineTransferRecords(rec0, rec1, rec2);
        BufferedTransfers bt = new BufferedTransfers(buffer);


        // Vérifie la taille
        assertEquals(3, bt.size());
        // Vérifie les champs de chaque enregistrement
        assertEquals(0x0001, bt.depStationId(0));
        assertEquals(0x10, bt.minutes(0)); // 0x10 = 16, ici on vérifie transferMinutes


        assertEquals(0x0002, bt.depStationId(1));
        assertEquals(0x20, bt.minutes(1)); // 0x20 = 32


        assertEquals(0x0001, bt.depStationId(2));
        assertEquals(0x15, bt.minutes(2)); // 0x15 = 21


        // Vérifie arrivingAt pour la station 0x0010 : les enregistrements 0 et 1
        int range1 = bt.arrivingAt(0x0010);
        assertEquals(0, PackedRange.startInclusive(range1));
        assertEquals(2, PackedRange.endExclusive(range1));


        // Vérifie arrivingAt pour la station 0x0020 : l'enregistrement 2 seulement
        int range2 = bt.arrivingAt(0x0020);
        assertEquals(2, PackedRange.startInclusive(range2));
        assertEquals(3, PackedRange.endExclusive(range2));
    }


    @Test
    void testMinutesBetween() {
        // Crée deux enregistrements pour la même station d'arrivée (0x0010)
        // Enregistrement 0 : de 0x0001 vers 0x0010, 0x10 minutes
        // Enregistrement 1 : de 0x0002 vers 0x0010, 0x20 minutes
        ByteBuffer buffer = combineTransferRecords(
                createTransferRecord(0x0001, 0x0010, 0x10),
                createTransferRecord(0x0002, 0x0010, 0x20)
        );
        BufferedTransfers bt = new BufferedTransfers(buffer);


        // Vérifie minutesBetween pour un enregistrement existant
        // Pour le départ 0x0002 et arrivée 0x0010, on s'attend à 0x20 (32)
        assertEquals(0x20, bt.minutesBetween(0x0002, 0x0010));


        // Pour un départ non présent, la méthode doit lever une exception
        assertThrows(NoSuchElementException.class, () -> bt.minutesBetween(0x0003, 0x0010));
    }


    @Test
    void testInvalidIndices() {
        // Crée un enregistrement unique.
        ByteBuffer buffer = createTransferRecord(0x0001, 0x0010, 0x10);
        BufferedTransfers bt = new BufferedTransfers(buffer);


        // Indice négatif ou hors bornes doit lever une exception.
        assertThrows(IndexOutOfBoundsException.class, () -> bt.depStationId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.depStationId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.minutes(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.minutes(1));
        // Pour arrivingAt, une station non présente ou négative doit lever une exception.
        assertThrows(IndexOutOfBoundsException.class, () -> bt.arrivingAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.arrivingAt(0xFFFF)); // station inconnue
    }


    @Test
    void testBufferSizeValidation() {
        // Le buffer doit avoir une capacité multiple de RECORD_SIZE.
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE - 1);
        buffer.put(new byte[RECORD_SIZE - 1]);
        buffer.flip();
        assertThrows(IllegalArgumentException.class, () -> new BufferedTransfers(buffer));
    }


    @Test
    void testMultipleSequentialCalls() {
        // Vérifie que plusieurs appels sur le même enregistrement renvoient toujours les mêmes valeurs.
        ByteBuffer buffer = createTransferRecord(0x0ABC, 0x0DEF, 0x0AA);
        BufferedTransfers bt = new BufferedTransfers(buffer);
        for (int i = 0; i < 10; i++) {
            assertEquals(0x0ABC, bt.depStationId(0));
            assertEquals(0x0AA, bt.minutes(0));
        }
    }
}
