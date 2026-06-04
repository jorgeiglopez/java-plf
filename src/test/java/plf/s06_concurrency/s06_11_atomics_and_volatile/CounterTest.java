package plf.s06_concurrency.s06_11_atomics_and_volatile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// EXERCISE: starts red
@DisplayName("Atomics: volatile gives visibility, not atomic read-modify-write")
class CounterTest {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 100_000;

    @Test
    @DisplayName("atomicIncrement loses no updates: total == THREADS * PER_THREAD")
    void atomicIncrementIsAtomic() throws Exception {
        Counter counter = new Counter();

        int expected = Counter.hammer(THREADS, PER_THREAD, counter::atomicIncrement);

        assertEquals(expected, counter.atomicCount(),
                "REVEAL: a CAS retry loop must reach " + expected
                        + " with zero lost updates");
    }
}
