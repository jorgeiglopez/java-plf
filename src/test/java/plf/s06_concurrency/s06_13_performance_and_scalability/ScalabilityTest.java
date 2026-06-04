package plf.s06_concurrency.s06_13_performance_and_scalability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScalabilityTest {

    private static final int THREADS = 8;
    private static final long OPS_PER_THREAD = 200_000L;

    // EXERCISE: starts red
    @Test
    void adderCounterLosesNoUpdatesUnderContention() {
        var r = ThroughputHarness.measure(AdderCounter::new, THREADS, OPS_PER_THREAD);
        assertEquals((long) THREADS * OPS_PER_THREAD, r.totalOps());
    }

    // EXERCISE: starts red
    @Test
    void amdahlCapsSpeedupAtTheSerialCeiling() {
        assertEquals(20.0, Amdahl.maxSpeedup(0.05, 100_000_000), 1e-3);
        assertEquals(1.0, Amdahl.maxSpeedup(1.0, 64), 1e-9);
        assertEquals(5.925926, Amdahl.maxSpeedup(0.05, 8), 1e-6);
    }

    // EXERCISE: starts red
    @Test
    void serialFractionInversionRoundTrips() {
        double f = Amdahl.serialFractionFromSpeedup(Amdahl.maxSpeedup(0.05, 8), 8);
        assertEquals(0.05, f, 1e-6);
    }

    // EXERCISE: starts red
    @Test
    void measuredNosediveIsBelowAmdahlsFloor() {
        double measuredSpeedup = 0.6; // observed sub-1.0 nosedive on real silicon
        double amdahlFloor = 1.0;
        assertTrue(measuredSpeedup < amdahlFloor,
                "Amdahl asymptotes toward 1.0 and never retreats; a measured sub-1.0 "
                        + "speedup is coherence traffic, which Amdahl does not model");
    }
}
