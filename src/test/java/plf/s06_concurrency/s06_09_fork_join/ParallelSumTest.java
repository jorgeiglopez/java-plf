package plf.s06_concurrency.s06_09_fork_join;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Test;

class ParallelSumTest {

    private static final int PARALLELISM = 4;
    private static final int N = 4_000_000;

    private static long[] ramp() {
        long[] data = new long[N];
        for (int i = 0; i < N; i++) {
            data[i] = i;
        }
        return data;
    }

    @Test
    void sumIsCorrect() {
        ForkJoinPool pool = new ForkJoinPool(PARALLELISM);
        try {
            ParallelSum.Result r = ParallelSum.run(ramp(), pool);
            assertEquals((long) N * (N - 1) / 2, r.sum);
        } finally {
            pool.shutdown();
        }
    }

    // EXERCISE: starts red
    @Test
    void workFansOutAcrossWorkers() {
        ForkJoinPool pool = new ForkJoinPool(PARALLELISM);
        try {
            ParallelSum.Result r = ParallelSum.run(ramp(), pool);
            assertTrue(r.leavesPerWorker.size() > 1,
                    "leaves ran on " + r.leavesPerWorker.size()
                            + " worker(s); work was not stolen: " + r.leavesPerWorker);
            assertFalse(
                    r.leavesPerWorker.keySet().stream().anyMatch(n -> n.equals("main")),
                    "the submitting thread ran leaves instead of offloading");
        } finally {
            pool.shutdown();
        }
    }
}
