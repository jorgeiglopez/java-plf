package plf.s06_concurrency.s06_14_virtual_threads;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import plf.s06_concurrency.s06_14_virtual_threads.CarrierStarvation.Arm;

class CarrierStarvationTest {

    private static final int TASKS = 10_000;
    private static final long TIMEOUT_MILLIS = 5_000;

    @Test
    void nonPinningArmLetsEveryTaskReachTheGateAtOnce() throws InterruptedException {
        assertTrue(
                CarrierStarvation.allReachedGateConcurrently(Arm.NON_PINNING, TASKS, TIMEOUT_MILLIS),
                "A parking virtual thread must release its carrier so all " + TASKS + " run concurrently");
    }

    // EXERCISE: starts red
    @Test
    void pinningArmAlsoLetsEveryTaskReachTheGate() throws InterruptedException {
        assertTrue(
                CarrierStarvation.allReachedGateConcurrently(Arm.PINNING, TASKS, TIMEOUT_MILLIS),
                "Carriers were starved: fewer than " + TASKS + " threads reached the gate together");
    }
}
