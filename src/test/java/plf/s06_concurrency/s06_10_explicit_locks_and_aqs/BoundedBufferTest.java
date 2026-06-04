package plf.s06_concurrency.s06_10_explicit_locks_and_aqs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class BoundedBufferTest {

    private static Thread consumer(BoundedBuffer b, ConcurrentLinkedQueue<Integer> sink) {
        return new Thread(() -> {
            try {
                sink.add(b.take());
            } catch (InterruptedException ignored) {
            }
        });
    }

    private static void awaitConsumers(BoundedBuffer b, int n) {
        while (b.consumersWaiting() < n) {
            Thread.onSpinWait();
        }
    }

    // EXERCISE: starts red
    @Test
    @Timeout(5)
    void oneItemIsTakenByExactlyOneConsumer() throws Exception {
        BoundedBuffer b = new BoundedBuffer(1);
        var taken = new ConcurrentLinkedQueue<Integer>();

        Thread c1 = consumer(b, taken);
        Thread c2 = consumer(b, taken);

        c1.start();
        awaitConsumers(b, 1);
        c2.start();
        awaitConsumers(b, 2);

        Thread p1 = new Thread(() -> {
            try {
                b.put(99);
            } catch (InterruptedException ignored) {
            }
        });
        p1.start();
        p1.join();

        Thread.sleep(50);

        assertEquals(1, taken.size(), "exactly one consumer may take the single produced item");
        assertEquals(99, taken.peek());
        assertEquals(0, b.size(), "count must never go negative");
        assertTrue(c1.isAlive() ^ c2.isAlive(), "the other consumer must still be legitimately waiting");

        c1.interrupt();
        c2.interrupt();
    }
}
