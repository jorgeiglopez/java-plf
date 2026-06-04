package plf.s05_collections.s05_04_iterators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import plf.s05_collections.s05_04_iterators.SmokeDetector.Outcome;

class SmokeDetectorTest {

    // EXERCISE: starts red
    @Test
    void length3StaysSilent() {
        Outcome predicted = SmokeDetector.predictLength3();
        Outcome actual = SmokeDetector.removeEvensWithForEach(List.of(1, 2, 3));
        assertEquals(actual, predicted);
    }

    // EXERCISE: starts red
    @Test
    void length4ThrowsCme() {
        Outcome predicted = SmokeDetector.predictLength4();
        Outcome actual = SmokeDetector.removeEvensWithForEach(List.of(1, 2, 3, 4));
        assertEquals(actual, predicted);
    }
}
