package plf.s06_concurrency.s06_11_atomics_and_volatile;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public final class Counter {

    private volatile int volatileCount;
    private final AtomicInteger atomicCount = new AtomicInteger();

    public int volatileCount() {
        return volatileCount;
    }

    public void volatileIncrement() {
        volatileCount++;
    }

    public int atomicCount() {
        return atomicCount.get();
    }

    // ---- IMPLEMENT THIS ONE METHOD ----
    public int atomicIncrement() {
        // <-- Task 1: atomically read-modify-write atomicCount, return the new value
        throw new UnsupportedOperationException("TODO");
    }
    // -----------------------------------

    public static int hammer(int threads, int perThread, Runnable op) throws InterruptedException {
        CyclicBarrier gate = new CyclicBarrier(threads);
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                try {
                    gate.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                for (int n = 0; n < perThread; n++) {
                    op.run();
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
        return threads * perThread;
    }
}
