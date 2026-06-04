package plf.s05_collections.s05_03_treemap_and_navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrainScheduleTest {   // EXERCISE: starts red

    private final TrainSchedule s = new TrainSchedule();

    @BeforeEach
    void seed() {
        s.add(540, "A");   // 09:00
        s.add(600, "B");   // 10:00
        s.add(660, "C");   // 11:00
    }

    @Test
    void nextAtOrAfter_isInclusive() {
        assertEquals("B", s.nextAtOrAfter(600));
        assertEquals("C", s.nextAtOrAfter(601));
        assertNull(s.nextAtOrAfter(661));
    }

    @Test
    void lastAtOrBefore_isInclusive() {
        assertEquals("B", s.lastAtOrBefore(600));
        assertEquals("A", s.lastAtOrBefore(599));
        assertNull(s.lastAtOrBefore(539));
    }

    @Test
    void strictlyAfter_isExclusive() {
        assertEquals("C", s.strictlyAfter(600));
        assertEquals("B", s.strictlyAfter(599));
        assertNull(s.strictlyAfter(660));
    }

    @Test
    void between_upperBoundIsExclusive() {
        assertEquals(List.of(540, 600), List.copyOf(s.between(540, 660).keySet()));
        assertEquals(List.of(600), List.copyOf(s.between(600, 660).keySet()));
    }
}
