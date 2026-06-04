package plf.s04_functional_streams.s04_05_streams_basics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LazyCountTest {

    // EXERCISE: starts red
    @Test
    void predictPeekTouches() {
        int predictedElided = 4; // <-- write your prediction
        int predictedFiltered = 4; // <-- write your prediction

        assertEquals(predictedElided, LazyCount.touchesWhenElided());
        assertEquals(predictedFiltered, LazyCount.touchesWhenFiltered());
    }

    // EXERCISE: starts red
    @Test
    void predictThrowingPeek() {
        boolean predictedThrowsWithoutFilter = true; // <-- write your prediction
        boolean predictedThrowsWithFilter = true; // <-- write your prediction

        assertEquals(predictedThrowsWithoutFilter, LazyCount.throwingPeekFires(false));
        assertEquals(predictedThrowsWithFilter, LazyCount.throwingPeekFires(true));

        if (predictedThrowsWithoutFilter == predictedThrowsWithFilter) {
            assertFalse(LazyCount.throwingPeekFires(false));
            assertTrue(LazyCount.throwingPeekFires(true));
        }
    }
}
