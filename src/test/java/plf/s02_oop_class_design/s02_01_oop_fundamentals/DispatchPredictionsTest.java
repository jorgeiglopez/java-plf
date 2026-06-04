package plf.s02_oop_class_design.s02_01_oop_fundamentals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// EXERCISE: this test starts RED on purpose. Fix only the EXPECTED_* constants.
class DispatchPredictionsTest {

    static final String EXPECTED_SPEAK = "Woof makes a generic noise"; // <-- change this
    static final String EXPECTED_GREET_HI = "greet(Object): hi";       // <-- change this
    static final String EXPECTED_GREET_NULL = "greet(Object): null";   // <-- change this
    static final String EXPECTED_REPRODUCE_RUNTIME = "Animal";         // <-- change this
    static final String EXPECTED_CAST = "Meow";                        // <-- change this

    @Test
    void dispatchPredictions() {
        DispatchLab.Animal a = new DispatchLab.Dog();

        assertEquals(EXPECTED_SPEAK, a.speak(), "row 1: a.speak()");
        assertEquals(EXPECTED_GREET_HI, a.greet("hi"), "row 2: a.greet(\"hi\")");
        assertEquals(EXPECTED_GREET_NULL, a.greet(null), "row 3: a.greet(null)");

        var x = a.reproduce();
        assertEquals(EXPECTED_REPRODUCE_RUNTIME, x.getClass().getSimpleName(),
                "row 4: runtime class of a.reproduce()");

        String castResult;
        try {
            DispatchLab.Cat c = (DispatchLab.Cat) a;
            castResult = c.speak();
        } catch (ClassCastException e) {
            castResult = e.getClass().getSimpleName();
        }
        assertEquals(EXPECTED_CAST, castResult, "row 5: (Cat) a");
    }
}
