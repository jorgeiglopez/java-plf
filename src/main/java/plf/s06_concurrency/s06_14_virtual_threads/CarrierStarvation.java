package plf.s06_concurrency.s06_14_virtual_threads;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class CarrierStarvation {

    private CarrierStarvation() {}

    public enum Arm { PINNING, NON_PINNING }

    /**
     * Launches {@code tasks} virtual threads. Each blocks on {@code gate}.
     * If carriers are starved, fewer than {@code tasks} threads reach the gate
     * at once and {@code arrived} never counts down to zero within the timeout.
     */
    public static boolean allReachedGateConcurrently(Arm arm, int tasks, long timeoutMillis)
            throws InterruptedException {
        CountDownLatch arrived = new CountDownLatch(tasks);
        CountDownLatch gate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> runOne(arm, arrived, gate));
            }
            boolean all = arrived.await(timeoutMillis, TimeUnit.MILLISECONDS);
            gate.countDown();
            return all;
        }
    }

    private static void runOne(Arm arm, CountDownLatch arrived, CountDownLatch gate) {
        switch (arm) {
            case PINNING -> {
                Object perTaskMonitor = new Object();          // <-- uncontended on purpose
                synchronized (perTaskMonitor) {
                    arrived.countDown();
                    awaitGate(gate);
                }
            }
            case NON_PINNING -> {
                arrived.countDown();
                awaitGate(gate);
            }
        }
    }

    private static void awaitGate(CountDownLatch gate) {
        try {
            gate.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** TODO: implement for the PROBLEM.md Part B wall-clock arms. */
    public static long heldAcrossSleepWallClockMillis(int tasks) {
        throw new UnsupportedOperationException("TODO");
    }
}
