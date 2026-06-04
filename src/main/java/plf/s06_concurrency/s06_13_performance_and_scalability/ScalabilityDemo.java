package plf.s06_concurrency.s06_13_performance_and_scalability;

public final class ScalabilityDemo {

    private static final long OPS_PER_THREAD = 5_000_000L;

    public static void main(String[] args) {
        int maxThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        run("AtomicLong", AtomicCounter::new, maxThreads);
        run("LongAdder ", AdderCounter::new, maxThreads);
    }

    private static void run(String label, java.util.function.Supplier<Counter> factory, int maxThreads) {
        ThroughputHarness.measure(factory, 1, OPS_PER_THREAD); // warm up
        double base = ThroughputHarness.measure(factory, 1, OPS_PER_THREAD).opsPerSecond();
        System.out.printf("%n%s  (1-thread = %,.0f ops/s)%n", label, base);
        for (int t = 1; t <= maxThreads; t++) {
            var r = ThroughputHarness.measure(factory, t, OPS_PER_THREAD);
            System.out.printf("  threads=%-2d  ops/s=%,14.0f  speedup=%.2fx%n",
                    t, r.opsPerSecond(), r.opsPerSecond() / base);
        }
    }
}
