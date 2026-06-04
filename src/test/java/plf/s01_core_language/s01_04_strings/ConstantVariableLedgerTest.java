package plf.s01_core_language.s01_04_strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Do NOT change row1; it already states the truth.
class ConstantVariableLedgerTest {

    private static String runtime() {
        return new String("a");
    }

    @Test
    void row1_finalConstantVariable_isFoldedAndInterned() {
        final String s = "a";
        assertTrue((s + "b") == "ab");
        assertTrue((s + "b").equals("ab"));
    }

    @Test
    void row2_finalButRuntimeInit_isInterned() {
        final String s = runtime();
        assertTrue((s + "b") == "ab");         // <-- starts red: fix this prediction
        assertTrue((s + "b").equals("ab"));
    }
}
