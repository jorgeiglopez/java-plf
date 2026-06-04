package plf.s06_concurrency.s06_04_sharing_objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedCounterTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void volatileFlagMakesSpinnerTerminate() throws Exception {
        SharedCounter counter = new SharedCounter();
        CyclicBarrier running = new CyclicBarrier(2);

        Thread spinner = new Thread(() -> {
            try {
                running.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            counter.spinUntilStopped();
        });
        spinner.start();

        running.await();
        counter.requestStop();
        spinner.join();

        assertTrue(counter.isStopped());
    }

    // EXERCISE: starts red
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void twoThreadsIncrementWithoutLostUpdates() throws Exception {
        int threads = 2;
        int perThread = 1_000_000;

        int total = SharedCounter.runIncrementContest(threads, perThread);

        assertEquals(threads * perThread, total);
    }
}
