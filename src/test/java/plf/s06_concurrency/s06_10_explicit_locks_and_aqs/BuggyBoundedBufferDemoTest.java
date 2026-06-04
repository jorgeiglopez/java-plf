package plf.s06_concurrency.s06_10_explicit_locks_and_aqs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class BuggyBoundedBufferDemoTest {

    private static Thread consumer(BuggyBoundedBuffer b) {
        return new Thread(() -> {
            try {
                b.take();
            } catch (InterruptedException ignored) {
            }
        });
    }

    private static void awaitConsumers(BuggyBoundedBuffer b, int n) {
        while (b.consumersWaiting() < n) {
            Thread.onSpinWait();
        }
    }

    @Test
    @Timeout(5)
    void singleConditionWithIfCorruptsCount() throws Exception {
        BuggyBoundedBuffer b = new BuggyBoundedBuffer(1);

        Thread c1 = consumer(b);
        Thread c2 = consumer(b);

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
        c1.join();
        c2.join();

        assertEquals(-1, b.size(),
                "lost wakeup: both consumers drain one produced item; count goes negative");
    }
}
