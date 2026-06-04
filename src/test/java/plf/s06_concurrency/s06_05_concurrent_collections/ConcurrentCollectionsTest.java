package plf.s06_concurrency.s06_05_concurrent_collections;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentCollectionsTest {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 50_000;

    // EXERCISE: starts red
    @Test
    void lostUpdateUnderContention() throws Exception {
        WordCounter counter = new WordCounter();
        CyclicBarrier gate = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int t = 0; t < THREADS; t++) {
                pool.submit(() -> {
                    try {
                        gate.await();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    for (int i = 0; i < PER_THREAD; i++) {
                        counter.record("java");
                    }
                });
            }
            pool.shutdown();
            //noinspection ResultOfMethodCallIgnored
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(THREADS * PER_THREAD, counter.count("java"));
    }

    @Test
    void forcedCollisionReentryThrows() {
        FibMemo memo = new FibMemo();
        assertThrows(IllegalStateException.class, () -> memo.fib(20));
    }

    // EXERCISE: starts red
    @Test
    void fibSafeIsCorrect() {
        FibMemo memo = new FibMemo();
        assertEquals(6765L, memo.fibSafe(20));
        assertEquals(0L, memo.fibSafe(0));
        assertEquals(1L, memo.fibSafe(1));
    }
}
