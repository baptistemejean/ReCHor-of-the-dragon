package ch.epfl.rechor;

import ch.epfl.rechor.journey.PackedCriteria;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PackedCriteriaTest {
    private int exampleDepMins () {
        return 480; // 4h00 // 0001 1110 0000
    }

    private int exampleArrMins () {
        return 420; // 3h00 // 0001 1010 0100
    }

    private int examplePayload() {
        return 238723028; // 0000 1110 0011 1010 1001 1111 1101 0100
    }

    @Test
    void packWorksWithNormal() {
        long packed = PackedCriteria.pack(exampleArrMins(), 23, examplePayload()); // Changes 0010 111
        Assertions.assertEquals(230996464803796L, packed); // 0 0000 0000 0000 0001 1010 0100 0010 111 0000 1110 0011 1010 1001 1111 1101 0100
    }

    @Test
    void packWorksWithDepMins() {
        long packed = PackedCriteria.withDepMins(PackedCriteria.pack(exampleArrMins(), 23, examplePayload()), exampleDepMins()) ;; // Changes 0010 111
        Assertions.assertEquals(1081094907033722836L, packed); // 0 0001 1110 0000 0001 1010 0100 0010 111 0000 1110 0011 1010 1001 1111 1101 0100
    }


    @Test
    void unpackDepTimeWorks() {
        long packed = PackedCriteria.withDepMins(PackedCriteria.pack(exampleArrMins(), 23, examplePayload()), exampleDepMins()) ;
        int depTime = PackedCriteria.depMins(packed);
        Assertions.assertEquals(exampleDepMins(), depTime); // 0 0000 0000 0000 0001 1010 0100 0010 111 0000 1110 0011 1010 1001 1111 1101 0100
    }

    @Test
    void unpackArrTimeWorks() {
        long packed = PackedCriteria.pack(exampleArrMins(), 23, examplePayload()); // Changes 0010 111
        int arrTime = PackedCriteria.arrMins(packed);
        Assertions.assertEquals(exampleArrMins(), arrTime); // 0 0000 0000 0000 0001 1010 0100 0010 111 0000 1110 0011 1010 1001 1111 1101 0100
    }

    @Test
    void unpackChangesWorks() {
        long packed = PackedCriteria.pack(exampleArrMins(), 23, examplePayload()); // Changes 0010 111
        int changes = PackedCriteria.changes(packed);
        Assertions.assertEquals(23, changes); // 0 0000 0000 0000 0001 1010 0100 0010 111 0000 1110 0011 1010 1001 1111 1101 0100
    }

    @Test
    void unpackPayloadWorks() {
        long packed = PackedCriteria.pack(exampleArrMins(), 23, examplePayload()); // Changes 0010 111
        int payload = PackedCriteria.payload(packed);
        Assertions.assertEquals(examplePayload(), payload); // 0 0000 0000 0000 0001 1010 0100 0010 111 0000 1110 0011 1010 1001 1111 1101 0100
    }

    @Test
    void dominatesOrEqualsWorks() {
        long packed1 = PackedCriteria.pack(exampleArrMins(), 13, examplePayload());
        long packed2 = PackedCriteria.pack(exampleArrMins(), 14, examplePayload());
        Assertions.assertEquals(true, PackedCriteria.dominatesOrIsEqual(packed1, packed2));
    }

    @Test
    void withoutDepMinsWorks() {
        long packed = PackedCriteria.withDepMins(PackedCriteria.pack(exampleArrMins(), 23, examplePayload()), exampleDepMins()) ;
        Assertions.assertEquals(230996464803796L, PackedCriteria.withoutDepMins(packed)); // 0 0000 0000 0000 0001 1010 0100 0010 111 0000 1110 0011 1010 1001 1111 1101 0100
    }

    @Test
    void withAdditionalChangeWorks() {
        long packed = PackedCriteria.withAdditionalChange(PackedCriteria.pack(exampleArrMins(), 12, examplePayload())) ;
        int changes = PackedCriteria.changes(packed);
        Assertions.assertEquals(changes, 13);
    }

    @Test
    void withPayloadWorks() {
        long packed = PackedCriteria.withPayload(PackedCriteria.pack(exampleArrMins(), 23, 0), examplePayload()) ;
        Assertions.assertEquals(230996464803796L, packed);
    }

    // Errors

    @Test
    void packThrowsWithArrMinsOutOfRange() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            long packed = PackedCriteria.pack(-23, 23, examplePayload());
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            long packed = PackedCriteria.pack(4096, 23, examplePayload());
        });
    }

    @Test
    void withDepMinsThrowsWithDepMinsOutOfRange() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            long packed = PackedCriteria.withDepMins(PackedCriteria.pack(exampleArrMins(), 23, examplePayload()), -2);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            long packed = PackedCriteria.withDepMins(PackedCriteria.pack(exampleArrMins(), 23, examplePayload()), 4096);
        });
    }

    @Test
    void packThrowsWithChangesOutOfRange() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            long packed = PackedCriteria.pack(exampleArrMins(), 128, examplePayload());
        });
    }

    @Test
    void withAdditionalChangeThrowsWithTooManyChanges() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            long packed = PackedCriteria.withAdditionalChange(PackedCriteria.pack(exampleArrMins(), 127, examplePayload()));
        });
    }

    @Test
    void depMinsThrowsWithoutDepMinsInTheCriteria() {
        long packed = PackedCriteria.pack(exampleArrMins(), 23, examplePayload());
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            int depMins = PackedCriteria.depMins(packed);
        });
    }

    @Test
    void dominatesOrIsEqualThrowsWithInconsistentDepartureTimePresence() {
        long packed1 = PackedCriteria.pack(exampleArrMins(), 23, examplePayload());
        long packed2 = PackedCriteria.withDepMins(PackedCriteria.pack(exampleArrMins(), 23, examplePayload()), exampleDepMins());
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            boolean dominatesOrIsEqual = PackedCriteria.dominatesOrIsEqual(packed1, packed2);
        });
    }

}
