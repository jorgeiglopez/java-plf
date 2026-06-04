package plf.s05_collections.s05_05_ordering;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import plf.s05_collections.s05_05_ordering.Points.Point;

class PointsTest {

    @Test
    void hashSetKeepsAllFour() {
        assertEquals(4, new HashSet<>(Points.FOUR).size());
    }

    @Test
    void naturalOrderingKeepsAllFour() {
        assertEquals(4, Points.naturalOrdered().size());
    }

    // EXERCISE: starts red
    @Test
    void comparatorOrderingMustAlsoKeepAllFour() {
        assertEquals(4, Points.comparatorOrdered().size());
    }
}
