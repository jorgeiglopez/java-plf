package plf.s03_generics.s03_04_raw_types_and_warnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SafeCopyTest {

    @Test
    void legitimateCopyPreservesElements() {
        List<String> copy = SafeCopy.copyOf(String.class, List.of("ab", "cde"));
        assertEquals(2, copy.size());
        assertEquals(2, SafeCopy.processFirst(copy));
    }

    // EXERCISE: starts red
    @Test
    void copyMustFailFastOnTypeViolation() {
        List<Object> poison = List.of(1, 2, 3);
        assertThrows(
            ClassCastException.class,
            () -> SafeCopy.copyOf(String.class, poison),
            "copyOf must reject the violation at the copy site, not leak it downstream");
    }
}
