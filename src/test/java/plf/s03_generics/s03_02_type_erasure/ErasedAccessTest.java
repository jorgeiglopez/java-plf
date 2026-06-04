package plf.s03_generics.s03_02_type_erasure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErasedAccessTest {

    @SuppressWarnings("unchecked")
    private static <T> T pollute(List<?> list) {
        return ErasedAccess.firstElement((List<T>) list);
    }

    @SuppressWarnings("unchecked")
    private static <T> T polluteSafe(List<?> list, Class<T> clazz) {
        return ErasedAccess.firstElementSafe((List<T>) list, clazz);
    }

    @Test
    @DisplayName("erased (T) cast: ClassCastException lands at the CALLER, not inside firstElement")
    void erasedCastFailsAtCaller() {
        List<Integer> ints = List.of(42);

        ClassCastException cce = null;
        try {
            String s = pollute(ints);
            org.junit.jupiter.api.Assertions.fail("expected a ClassCastException, got: " + s);
        } catch (ClassCastException thrown) {
            cce = thrown;
        }

        StackTraceElement top = cce.getStackTrace()[0];
        assertEquals("erasedCastFailsAtCaller", top.getMethodName(),
                "the checkcast was inserted at the caller's assignment, so the caller is on top");
        assertEquals(ErasedAccessTest.class.getName(), top.getClassName(),
                "the failure executes in the test, never in ErasedAccess.firstElement");
    }

    // EXERCISE: starts red
    @Test
    @DisplayName("token cast: ClassCastException lands INSIDE firstElementSafe")
    void tokenCastFailsInsideMethod() {
        List<Integer> ints = List.of(42);

        ClassCastException cce = assertThrows(ClassCastException.class,
                () -> polluteSafe(ints, String.class));

        boolean insideSafe = false;
        for (StackTraceElement e : cce.getStackTrace()) {
            if (e.getClassName().equals(ErasedAccess.class.getName())
                    && e.getMethodName().equals("firstElementSafe")) {
                insideSafe = true;
                break;
            }
        }
        assertTrue(insideSafe,
                "clazz.cast() must move the failure INTO firstElementSafe's stack frame");
    }

    @Test
    @DisplayName("token cast returns the value unharmed when the type actually matches")
    void tokenCastSucceedsOnMatch() {
        List<String> strings = List.of("ok");
        assertEquals("ok", ErasedAccess.firstElementSafe(strings, String.class));
    }
}
