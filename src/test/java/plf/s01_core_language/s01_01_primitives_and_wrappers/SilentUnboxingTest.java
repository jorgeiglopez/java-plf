package plf.s01_core_language.s01_01_primitives_and_wrappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SilentUnboxingTest {

    private static final String PREDICTED_SILENT_WRONG = "one"; // <-- change me

    @Test
    void line1_silentWrong_predictThenAssert() {
        // EXERCISE: starts red
        assertEquals(PREDICTED_SILENT_WRONG, SilentUnboxing.silentWrong());
    }

    @Test
    void line2_loudDead_predictTheExplosion() {
        assertThrows(NullPointerException.class, SilentUnboxing::loudDead);
    }
}
