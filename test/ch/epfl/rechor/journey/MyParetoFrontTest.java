package ch.epfl.rechor.journey;

import org.junit.jupiter.api.Test;

import java.lang.constant.PackageDesc;
import java.util.ArrayList;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;


public class MyParetoFrontTest {

    // Example given at 2.2
    long[] example() {
        long l1 = PackedCriteria.pack(480, 3, 0);
        long l2 = PackedCriteria.pack(480, 4, 0);
        long l3 = PackedCriteria.pack(481, 2, 0);
        long l4 = PackedCriteria.pack(482, 1, 0);
        long l5 = PackedCriteria.pack(483, 0, 0);
        long l6 = PackedCriteria.pack(484, 1, 0);

        return new long[]{l1, l2, l3, l4, l5, l6};
    }

    @Test
    void addWorksWithGivenExample() {
        ParetoFront.Builder builder = new ParetoFront.Builder();

        long[] example = example();
        builder.add(example[0]);
        builder.add(example[1]);
        builder.add(example[5]);
        builder.add(example[2]);
        builder.add(example[3]);
        builder.add(example[4]);

        ParetoFront expected = new ParetoFront(new long[]{
                example[0],
                example[2],
                example[3],
                example[4],
        });

        ParetoFront actual = builder.build();

        assertTrue(expected.equals(actual)); // Assert equals doesn't work somehow
    }

    @Test
    void clearWorks() {
        ParetoFront.Builder builder = new ParetoFront.Builder();

        long[] example = example();
        builder.add(example[0]);
        builder.add(example[1]);
        builder.add(example[5]);
        builder.add(example[2]);
        builder.add(example[3]);
        builder.add(example[4]);

        builder.clear();

        assertTrue(builder.isEmpty());
    }

    @Test
    void builderConstructorWorks() {
        ParetoFront.Builder b1 = new ParetoFront.Builder();

        long[] example = example();
        b1.add(example[0]);
        b1.add(example[1]);
        b1.add(example[5]);
        b1.add(example[2]);
        b1.add(example[3]);
        b1.add(example[4]);

        ParetoFront.Builder b2 = new ParetoFront.Builder(b1);

        assertTrue(b1.equals(b2));
    }
//
//    @Test
//    void builderFullyDominatesWorksWithGivenExample() {
//        ParetoFront.Builder b1 = new ParetoFront.Builder();
//
//        long[] example = example();
//        b1.add(example[0]);
//        b1.add(example[1]);
//        b1.add(example[5]);
//        b1.add(example[2]);
//        b1.add(example[3]);
//        b1.add(example[4]);
//
//        ParetoFront.Builder b2 = new ParetoFront.Builder();
//        b2.add(example[1]);
//        b2.add(example[4]);
//        b2.add(example[0]);
//
//        assertTrue(b1.fullyDominates(b2, 400));
//    }
}
