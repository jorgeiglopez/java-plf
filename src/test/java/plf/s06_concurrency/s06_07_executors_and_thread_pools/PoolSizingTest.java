package plf.s06_concurrency.s06_07_executors_and_thread_pools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

class PoolSizingTest {

    // EXERCISE: starts red
    @Test
    void unboundedQueuePinsPoolAtCore() {
        ThreadPoolExecutor pool =
                PoolProbe.newPool(2, 10, new LinkedBlockingQueue<>());

        int peak = PoolProbe.peakThreads(pool, 50);

        assertEquals(Prediction.BEAT_1_PEAK_THREADS, peak);
    }

    // EXERCISE: starts red
    @Test
    void synchronousQueueGrowsPoolToMax() {
        ThreadPoolExecutor pool =
                PoolProbe.newPool(2, 50, new SynchronousQueue<>());

        int peak = PoolProbe.peakThreads(pool, 50);

        assertEquals(Prediction.BEAT_2_PEAK_THREADS, peak);
    }
}
