package plf.s02_oop_class_design.s02_05_nested_classes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakTest {

    private final Leak outer = new Leak(8);

    @Test
    void innerTouchingNothing_doesNotCaptureEnclosing() {
        assertFalse(Leak.capturesEnclosing(outer.new InnerTouchingNothing()));
    }

    @Test
    void innerTouchingPayload_capturesEnclosing() {
        assertTrue(Leak.capturesEnclosing(outer.new InnerTouchingPayload()));
    }

    @Test
    void staticNested_neverCapturesEnclosing() {
        assertFalse(Leak.capturesEnclosing(new Leak.StaticNested()));
    }

    @Test
    void lambdaLocalOnly_doesNotCaptureEnclosing() {
        assertFalse(Leak.capturesEnclosing(outer.lambdaLocalOnly()));
    }

    @Test
    void lambdaTouchingPayload_capturesEnclosing() {
        assertTrue(Leak.capturesEnclosing(outer.lambdaTouchingPayload()));
    }

    // EXERCISE: this test starts red.
    @Test
    void whyEmptyInnerDoesNotPin_isCommitted() {
        String answer = Leak.whyEmptyInnerDoesNotPin();
        assertTrue(answer != null && !answer.isBlank(),
                "complete Leak.whyEmptyInnerDoesNotPin() with your one-sentence derivation");
    }
}
