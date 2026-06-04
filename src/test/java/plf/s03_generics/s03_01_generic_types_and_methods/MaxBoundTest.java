package plf.s03_generics.s03_01_generic_types_and_methods;

// EXERCISE: starts red

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Recursive bound: inference fails, an explicit type witness rescues it")
class MaxBoundTest {

    static final String PREDICTED_REASON = "TODO: why does T fail to be inferred?"; // <-- Task 1 here

    @Test
    @DisplayName("Task 1: commit the one-line reason inference cannot pick a T")
    void reasonIsCommitted() {
        assertEquals(
                "T must satisfy T extends Comparable<T>, but String and StringBuilder force "
                        + "conflicting equality constraints on T, so no single T exists",
                PREDICTED_REASON);
    }

    @Test
    @DisplayName("Task 2: the witnessed call compiles and returns the lexicographic max")
    void witnessedMaxReturnsWorld() {
        assertEquals("world", MaxBound.maxOfTwoStrings());
    }
}
