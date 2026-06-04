package plf.s04_functional_streams.s04_07_primitive_and_parallel_streams;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public final class StreamPerfHarness {

    static final long N = 10_000_000L;

    // ---- Task: implement the four bars. Each returns the same sum: 0+1+...+(N-1). ----

    // Stream.iterate(...).limit(N): boxed Long, ORDERED-but-unSIZED source. Sequential.
    static long boxedSeq() {
        throw new UnsupportedOperationException("TODO"); // <-- bar 1
    }

    // Same source as boxedSeq, but .parallel().
    static long boxedPar() {
        throw new UnsupportedOperationException("TODO"); // <-- bar 2
    }

    // LongStream.range(0, N).boxed(): boxed Long, but SIZED|SUBSIZED source. .parallel().
    static long boxedSizedPar() {
        throw new UnsupportedOperationException("TODO"); // <-- bar 3
    }

    // LongStream.range(0, N): primitive long accumulator, SIZED source. .parallel().
    static long primPar() {
        throw new UnsupportedOperationException("TODO"); // <-- bar 4
    }

    static long time(String label, java.util.function.LongSupplier bar, long expected) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 7; i++) {           // warm up the JIT, keep the best run
            long t0 = System.nanoTime();
            long result = bar.getAsLong();
            long elapsed = System.nanoTime() - t0;
            if (result != expected) {
                throw new AssertionError(label + " wrong sum: " + result + " != " + expected);
            }
            best = Math.min(best, elapsed);
        }
        System.out.printf("%-16s %8.2f ms%n", label, best / 1_000_000.0);
        return best;
    }

    public static void main(String[] args) {
        System.out.println("cores reported: " + Runtime.getRuntime().availableProcessors());
        long expected = N * (N - 1) / 2;
        time("boxed_seq",       StreamPerfHarness::boxedSeq,      expected);
        time("boxed_par",       StreamPerfHarness::boxedPar,      expected);
        time("boxed_sized_par", StreamPerfHarness::boxedSizedPar, expected);
        time("prim_par",        StreamPerfHarness::primPar,       expected);
    }

    private StreamPerfHarness() {}
}
