package plf.s02_oop_class_design.s02_08_records_and_sealed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sealed area: the defensive default that lies")
class SealedAreaTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("Stage 2 area is correct today (Circle / Rectangle only)")
    void stage2CorrectToday() {
        double circle = SealedAreaStage2.area(new SealedAreaStage2.Circle(1.0));
        double rect = SealedAreaStage2.area(new SealedAreaStage2.Rectangle(2.0, 3.0));
        assertEquals(Math.PI, circle, EPS, "Circle(1) area should be PI");
        assertEquals(6.0, rect, EPS, "Rectangle(2,3) area should be 6");
    }

    @Test
    @DisplayName("compact constructor validates the record component")
    void compactConstructorRejectsBadRadius() {
        assertThrows(IllegalArgumentException.class,
                () -> new SealedAreaStage2.Circle(0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new SealedAreaStage2.Circle(-2.0));
    }

    @Test
    @DisplayName("record equality is by component value, not identity")
    void recordEqualityIsValueBased() {
        var a = new SealedAreaStage2.Rectangle(2.0, 3.0);
        var b = new SealedAreaStage2.Rectangle(2.0, 3.0);
        assertEquals(a, b, "two Rectangle(2,3) must be equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal records share a hashCode");
    }

    // EXERCISE: starts red
    static final double PREDICT_TRIANGLE_AREA = 6.0; // <-- write what you THINK area(triangle) returns
    @Test
    @DisplayName("the default branch silently swallows the new subtype")
    void defaultBranchLiesForTriangle() {
        // <-- Task 3: uncomment the two lines below after adding Triangle
        // double triangle = SealedAreaStage2.area(new SealedAreaStage2.Triangle(4.0, 3.0));
        // assertEquals(PREDICT_TRIANGLE_AREA, triangle, EPS, "what does area(triangle) actually return?");
        assertTrue(true, "placeholder until Task 3 is activated");
    }
}
