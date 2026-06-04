package plf.s06_concurrency.s06_17_reactive_flow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// EXERCISE: starts red
@DisplayName("Flow back-pressure: the bounded buffer blocks the producer regardless of demand")
class BackpressureLabTest {

    private static final int ITEMS = 50;

    @Test
    @DisplayName("greedy request(MAX_VALUE): still blocks submit() once the 4-slot buffer fills")
    void unboundedDemandStillBlocksOnTheBuffer() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        BackpressureLab.GatedSubscriber sub = BackpressureLab.GatedSubscriber.greedy(gate);
        assertProducerBlocks(sub, gate);
    }

    @Test
    @DisplayName("disciplined request(1)-per-onNext: blocks submit() on demand, not on the buffer")
    void boundedDemandBlocksOnDemand() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        BackpressureLab.GatedSubscriber sub = BackpressureLab.GatedSubscriber.oneAtATime(gate);
        assertProducerBlocks(sub, gate);
    }

    private void assertProducerBlocks(BackpressureLab.GatedSubscriber sub, CountDownLatch gate)
            throws Exception {
        try (SubmissionPublisher<Integer> pub =
                     new SubmissionPublisher<>(ForkJoinPool.commonPool(), BackpressureLab.BUFFER_CAPACITY)) {
            pub.subscribe(sub);

            AtomicBoolean publishedAll = new AtomicBoolean(false);
            Thread producer = new Thread(() -> {
                try {
                    for (int i = 0; i < ITEMS; i++) {
                        pub.submit(i);
                    }
                    publishedAll.set(true);
                } catch (IllegalStateException closedAfterAssertions) {
                    // publisher closed by the try-with-resources while this thread was parked
                }
            });
            producer.setDaemon(true);
            producer.start();

            sub.awaitFirstDelivery();                 // consumer is now parked on item 0
            awaitBlocked(producer);                    // producer parks inside submit()

            assertSame(Thread.State.WAITING, producer.getState(),
                    "REVEAL: producer not blocked — submit() drained freely, so back-pressure never engaged");
            assertFalse(publishedAll.get(),
                    "REVEAL: all " + ITEMS + " items submitted without blocking — buffer did not bound the producer");
            assertEquals(1, sub.received(),
                    "REVEAL: consumer delivered " + sub.received() + " items, expected exactly 1 (it is gated on item 0)");

            gate.countDown();                          // release the consumer so the producer can finish
        }
    }

    private static void awaitBlocked(Thread t) {
        // Deterministic: the producer MUST reach WAITING once buffer+demand are exhausted.
        while (t.getState() != Thread.State.WAITING) {
            Thread.onSpinWait();
        }
    }
}
