package plf.s03_generics.s03_05_generics_and_arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Generics & arrays: the cast you never wrote")
class GenericArrayTest {

    enum Throws { OBJECT_LINE, STRING_LINE, NEITHER, BOTH }

    static final Throws PREDICT_WHICH_THROWS = Throws.NEITHER; // <-- Task 1: commit before running

    @Test
    @DisplayName("identical fillBroken call: which declared type explodes?")
    void displacement() {
        boolean objectLineThrew = false;
        try {
            Object[] a = GenericArray.fillBroken("x", 3);
            assertEquals(3, a.length);
        } catch (ClassCastException e) {
            objectLineThrew = true;
        }

        boolean stringLineThrew = false;
        try {
            String[] b = GenericArray.fillBroken("x", 3);
            assertEquals(3, b.length);
        } catch (ClassCastException e) {
            stringLineThrew = true;
        }

        Throws actual = objectLineThrew
                ? (stringLineThrew ? Throws.BOTH : Throws.OBJECT_LINE)
                : (stringLineThrew ? Throws.STRING_LINE : Throws.NEITHER);

        assertEquals(PREDICT_WHICH_THROWS, actual,
                "the call expression is identical on both lines — so whose bytecode holds the failing checkcast?");
    }

    @Test
    @DisplayName("fillSafe returns a genuine String[] that survives the assignment")
    void fillSafeProducesRealArray() {
        // EXERCISE: starts red
        String[] b = GenericArray.fillSafe(String.class, "x", 3);

        assertEquals(String[].class, b.getClass());
        assertArrayEquals(new String[] {"x", "x", "x"}, b);
    }
}
