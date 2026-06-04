package plf.s03_generics.s03_06_generics_and_varargs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnsafeVarargsTest {

    @Test
    void originalPickTwoPollutesTheHeap() {
        assertThrows(ClassCastException.class, () -> {
            String[] result = UnsafeVarargs.pickTwo("x", "y", "z");
            assertEquals("x", result[0]);
        });
    }

    // EXERCISE: starts red
    @Test
    void safePickTwoReturnsTheElements() {
        List<String> result = UnsafeVarargs.pickTwoSafe("x", "y", "z");
        assertEquals(List.of("x", "y"), result);
    }
}
