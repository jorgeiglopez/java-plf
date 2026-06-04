package plf.s06_concurrency.s06_13_performance_and_scalability;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

public final class ThroughputHarness {

    private ThroughputHarness() {
    }

    public record Result(int threads, long totalOps, long nanos) {
        public double opsPerSecond() {
            return nanos == 0 ? 0 : (totalOps * 1_000_000_000.0) / nanos;
        }
    }

    public static Result measure(Supplier<Counter> factory, int threads, long opsPerThread) {
        Counter counter = factory.get();
        CyclicBarrier startGate = new CyclicBarrier(threads + 1);
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                await(startGate);
                for (long n = 0; n < opsPerThread; n++) {
                    counter.increment();
                }
            });
            workers.add(t);
            t.start();
        }

        await(startGate);
        long start = System.nanoTime();
        for (Thread t : workers) {
            join(t);
        }
        long elapsed = System.nanoTime() - start;

        long expected = (long) threads * opsPerThread;
        if (counter.read() != expected) {
            throw new IllegalStateException("lost updates: " + counter.read() + " != " + expected);
        }
        return new Result(threads, expected, elapsed);
    }

    private static void await(CyclicBarrier gate) {
        try {
            gate.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void join(Thread t) {
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
