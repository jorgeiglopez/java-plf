package plf.s06_concurrency.s06_07_executors_and_thread_pools;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class PoolProbe {

    private PoolProbe() {
    }

    /**
     * Submits {@code taskCount} tasks; each parks on a latch so no worker ever
     * recycles. Once the pool has stopped growing (all live workers busy),
     * returns {@link ThreadPoolExecutor#getLargestPoolSize()} -- the peak number
     * of worker threads the pool actually created.
     */
    public static int peakThreads(ThreadPoolExecutor pool, int taskCount) {
        CountDownLatch release = new CountDownLatch(1);
        Runnable parkUntilReleased = () -> await(release);

        try {
            for (int i = 0; i < taskCount; i++) {
                pool.execute(parkUntilReleased);
            }
            awaitQuiescentPoolSize(pool);
            return pool.getLargestPoolSize();
        } finally {
            release.countDown();
            pool.shutdown();
            awaitTermination(pool);
        }
    }

    // ---- helpers below: do not modify ----

    public static ThreadPoolExecutor newPool(int core, int max, BlockingQueue<Runnable> queue) {
        return new ThreadPoolExecutor(core, max, 0L, TimeUnit.MILLISECONDS, queue);
    }

    /**
     * Spins until the worker count is stable across consecutive observations and
     * every live worker is actively running a (parked) task -- i.e. the pool has
     * reached the steady state implied by its queue/sizing policy.
     */
    private static void awaitQuiescentPoolSize(ThreadPoolExecutor pool) {
        int stableReads = 0;
        int last = -1;
        while (stableReads < 50) {
            int size = pool.getPoolSize();
            boolean allBusy = pool.getActiveCount() == size && size > 0;
            stableReads = (size == last && allBusy) ? stableReads + 1 : 0;
            last = size;
            Thread.onSpinWait();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("probe interrupted", e);
        }
    }

    private static void awaitTermination(ThreadPoolExecutor pool) {
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
