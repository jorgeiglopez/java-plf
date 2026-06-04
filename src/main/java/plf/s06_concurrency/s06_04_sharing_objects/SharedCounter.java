package plf.s06_concurrency.s06_04_sharing_objects;

import java.util.concurrent.CyclicBarrier;

public final class SharedCounter {

    // @GuardedBy: single writer per flag, visibility-only -> volatile is the policy.
    private volatile boolean stop = false;

    // ---- TASK: this field's publication policy is the bug. Do NOT change its name/type contract from outside. ----
    private volatile int count = 0;

    public boolean isStopped() {
        return stop;
    }

    public void requestStop() {
        stop = true;
    }

    public int count() {
        return count;
    }

    public void spinUntilStopped() {
        while (!stop) {
            // intentionally empty: no I/O, no barrier -> exposes the visibility question
        }
    }

    public void incrementManyTimes(int times) {
        for (int i = 0; i < times; i++) {
            count++;   // <-- read-modify-write on a volatile: visible, but NOT atomic
        }
    }

    public static int runIncrementContest(int threads, int perThread) throws InterruptedException {
        SharedCounter counter = new SharedCounter();
        CyclicBarrier startLine = new CyclicBarrier(threads);
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> {
                try {
                    startLine.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                counter.incrementManyTimes(perThread);
            });
        }
        for (Thread w : workers) {
            w.start();
        }
        for (Thread w : workers) {
            w.join();
        }
        return counter.count();
    }
}
