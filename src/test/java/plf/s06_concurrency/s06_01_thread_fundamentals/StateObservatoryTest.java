package plf.s06_concurrency.s06_01_thread_fundamentals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StateObservatoryTest {

    // ---- WRITE YOUR PREDICTIONS HERE (before running) ----
    static final Thread.State PREDICTED_WAITER    = Thread.State.BLOCKED;        // <-- your guess
    static final Thread.State PREDICTED_CONTENDER  = Thread.State.WAITING;        // <-- your guess
    static final Thread.State PREDICTED_SLEEPER    = Thread.State.WAITING;        // <-- your guess
    // ------------------------------------------------------

    // EXERCISE: starts red
    @Test
    void predictionsMatchReality() throws InterruptedException {
        StateObservatory obs = new StateObservatory();
        obs.start();
        try {
            assertEquals(PREDICTED_WAITER, obs.waiterState(),
                    "thread blocked in monitor.wait(0)");
            assertEquals(PREDICTED_CONTENDER, obs.contenderState(),
                    "thread contending to enter a held monitor");
            assertEquals(PREDICTED_SLEEPER, obs.sleeperState(),
                    "thread inside Thread.sleep(60_000)");
        } finally {
            obs.stop();
        }
    }
}
