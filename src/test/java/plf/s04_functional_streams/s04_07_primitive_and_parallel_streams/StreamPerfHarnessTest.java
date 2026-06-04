package plf.s04_functional_streams.s04_07_primitive_and_parallel_streams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StreamPerfHarnessTest {

    private static final long EXPECTED =
            StreamPerfHarness.N * (StreamPerfHarness.N - 1) / 2;

    @Test
    void allBarsComputeTheSameSum() {
        assertEquals(EXPECTED, StreamPerfHarness.boxedSeq());
        assertEquals(EXPECTED, StreamPerfHarness.boxedPar());
        assertEquals(EXPECTED, StreamPerfHarness.boxedSizedPar());
        assertEquals(EXPECTED, StreamPerfHarness.primPar());
    }

    // EXERCISE: starts red
    @Test
    void splittabilityIsTheBiggerCliff() {
        long boxedSeq      = best(StreamPerfHarness::boxedSeq);
        long boxedPar      = best(StreamPerfHarness::boxedPar);
        long boxedSizedPar = best(StreamPerfHarness::boxedSizedPar);
        long primPar       = best(StreamPerfHarness::primPar);

        // Threads bought almost nothing while the source refused to split.
        assertTrue(boxedPar > boxedSeq * 0.5,
                "boxed_par should be no big win over boxed_seq (source won't split): "
                        + boxedPar + "ns vs " + boxedSeq + "ns");

        // Fixing splittability (still boxed) lands NEAR the primitive bar...
        assertTrue(boxedSizedPar <= primPar * 2.0,
                "boxed_sized_par should be within ~2x of prim_par "
                        + "(splittability dominates, boxing is the smaller cliff): "
                        + boxedSizedPar + "ns vs " + primPar + "ns");

        // ...and is itself a large win over the unsplittable boxed_par.
        assertTrue(boxedSizedPar < boxedPar / 3,
                "boxed_sized_par should crush boxed_par: "
                        + boxedSizedPar + "ns vs " + boxedPar + "ns");
    }

    private static long best(java.util.function.LongSupplier bar) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 7; i++) {
            long t0 = System.nanoTime();
            assertEquals(EXPECTED, bar.getAsLong());
            best = Math.min(best, System.nanoTime() - t0);
        }
        return best;
    }
}
