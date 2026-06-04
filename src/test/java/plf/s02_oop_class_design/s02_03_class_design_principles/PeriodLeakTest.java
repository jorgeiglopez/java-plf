package plf.s02_oop_class_design.s02_03_class_design_principles;

// EXERCISE: this test starts red.

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Class design: a final-field Period that still mutates from the outside")
class PeriodLeakTest {

    static final boolean PREDICT_INVARIANT_HOLDS = true; // <-- write your prediction, then run

    @Test
    @DisplayName("mutating the caller's original Date shifts the Period's end")
    void inboundAliasingBreaksInvariant() {
        Date start = new Date(0L);
        Date end = new Date(10_000L);
        Period p = new Period(start, end);

        long endBefore = p.getEnd().getTime();

        end.setTime(0L); // <-- mutate the caller's reference, never a getter

        long endAfter = p.getEnd().getTime();

        boolean invariantHeld = (endAfter == endBefore);

        assertEquals(PREDICT_INVARIANT_HOLDS, invariantHeld,
                "did Period.end survive a mutation of the caller's original Date?");

        assertTrue(invariantHeld,
                "Period leaked: end moved from " + endBefore + " to " + endAfter);
    }
}
