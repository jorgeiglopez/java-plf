package plf.s04_functional_streams.s04_01_functional_interfaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamProbeTest {

    @Test
    void plainSamAcceptsLambda() {
        assertTrue(SamProbe.isEmptyViaPlain(""));
        assertFalse(SamProbe.isEmptyViaPlain("x"));
    }

    @Test
    void samWithDefaultsAcceptsLambda() {
        assertTrue(SamProbe.isEmptyViaDefaults(""));
        assertFalse(SamProbe.isEmptyViaDefaults("x"));
    }

    @Test
    void samWithObjectMethodsAcceptsLambda() {
        assertTrue(SamProbe.isEmptyViaObjectMethods(""));
        assertFalse(SamProbe.isEmptyViaObjectMethods("x"));
    }

    // EXERCISE: starts red
    @Test
    void predictAbstractMethodCount() {
        assertEquals(SamProbe.ABSTRACT_METHOD_COUNT_OF_TRAP, trapAbstractMethodCount());
    }

    private int trapAbstractMethodCount() {
        // boolean test(T) PLUS boolean equals(T) where T is unbounded.
        // equals(T) does NOT match Object.equals(Object), so it does not override it.
        return 2;
    }
}
