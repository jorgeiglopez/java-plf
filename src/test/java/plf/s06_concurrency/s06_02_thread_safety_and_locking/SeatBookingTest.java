package plf.s06_concurrency.s06_02_thread_safety_and_locking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.assertTrue;

// EXERCISE: starts red
@DisplayName("Thread safety: a check-then-act compound action must not overbook")
class SeatBookingTest {

    private static final int CAPACITY = 1;
    private static final int THREADS = 64;

    @Test
    @DisplayName("with every thread released at once, booked never exceeds capacity")
    void compoundActionStaysAtomicUnderContention() throws Exception {
        for (int round = 0; round < 200; round++) {
            SeatBooking booking = new SeatBooking(CAPACITY);
            CyclicBarrier gate = new CyclicBarrier(THREADS);
            Thread[] threads = new Thread[THREADS];

            for (int i = 0; i < THREADS; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        gate.await();          // <-- all threads hit tryBook in the same instant
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    booking.tryBook();
                });
                threads[i].start();
            }
            for (Thread t : threads) {
                t.join();
            }

            assertTrue(booking.booked() <= CAPACITY,
                    "REVEAL: booked=" + booking.booked() + " exceeded capacity=" + CAPACITY
                            + " — locking each step did not lock the sequence");
        }
    }
}
