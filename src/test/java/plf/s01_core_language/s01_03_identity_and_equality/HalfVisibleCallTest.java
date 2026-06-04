package plf.s01_core_language.s01_03_identity_and_equality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Do NOT change IdentityLab.f — only the EXPECTED predictions below are wrong.
class HalfVisibleCallTest {

    // ---- YOUR PREDICTIONS (edit these) ----
    static final String EXPECTED_A_AFTER = "A";   // WRONG on purpose
    static final String EXPECTED_B_AFTER = "Z";   // WRONG on purpose
    static final boolean EXPECTED_A_IS_ORIG_A = false; // WRONG on purpose
    static final boolean EXPECTED_B_IS_ORIG_B = false; // WRONG on purpose

    @Test
    void halfVisibleCall() {
        StringBuilder a = new StringBuilder("A");
        StringBuilder b = new StringBuilder("B");
        StringBuilder origA = a;
        StringBuilder origB = b;

        IdentityLab.f(a, b);

        assertEquals(EXPECTED_A_AFTER, a.toString(), "line 1: what is a after the call?");
        assertEquals(EXPECTED_B_AFTER, b.toString(), "line 2: what is b after the call?");
        if (EXPECTED_A_IS_ORIG_A) {
            assertSame(origA, a, "line 3: a still points at the original object?");
        } else {
            assertEquals(true, EXPECTED_A_IS_ORIG_A, "line 3: predict a == origA (true/false)");
        }
        if (EXPECTED_B_IS_ORIG_B) {
            assertSame(origB, b, "line 4: b still points at the original object?");
        } else {
            assertEquals(true, EXPECTED_B_IS_ORIG_B, "line 4: predict b == origB (true/false)");
        }
    }
}
