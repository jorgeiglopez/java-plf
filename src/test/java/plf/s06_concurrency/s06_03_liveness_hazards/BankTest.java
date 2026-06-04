package plf.s06_concurrency.s06_03_liveness_hazards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import plf.s06_concurrency.s06_03_liveness_hazards.Bank.Account;

class BankTest {

    @Test
    @Timeout(5)
    void cyclicTransferDeadlocks_andMxBeanSeesTheCycle() throws Exception {
        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);
        CyclicBarrier gate = new CyclicBarrier(2);

        Thread t1 = new Thread(() -> Bank.transfer(a, b, 100, gate), "t1");
        Thread t2 = new Thread(() -> Bank.transfer(b, a, 100, gate), "t2");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] cycle = awaitDeadlock(mx, t1.threadId(), t2.threadId());

        assertNotNull(cycle, "findDeadlockedThreads() must report the monitor cycle");
        assertEquals(2, cycle.length, "exactly the two transfer threads are in the cycle");
    }

    // EXERCISE: starts red
    @Test
    @Timeout(5)
    void safeTransferImposesGlobalOrder_noDeadlock() throws Exception {
        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);
        int rounds = 10_000;
        CountDownLatch start = new CountDownLatch(1);

        Thread t1 = spin(start, () -> Bank.safeTransfer(a, b, 1), rounds, "t1");
        Thread t2 = spin(start, () -> Bank.safeTransfer(b, a, 1), rounds, "t2");
        t1.start();
        t2.start();
        start.countDown();

        t1.join(4000);
        t2.join(4000);
        assertTrue(!t1.isAlive() && !t2.isAlive(), "safeTransfer must not deadlock");
        assertEquals(2000, a.balance() + b.balance(), "money is conserved");
    }

    private static Thread spin(CountDownLatch start, Runnable op, int rounds, String name) {
        Thread t = new Thread(() -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (int i = 0; i < rounds; i++) {
                op.run();
            }
        }, name);
        t.setDaemon(true);
        return t;
    }

    private static long[] awaitDeadlock(ThreadMXBean mx, long id1, long id2) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < deadline) {
            long[] ids = mx.findDeadlockedThreads();
            if (ids != null && contains(ids, id1) && contains(ids, id2)) {
                return ids;
            }
            Thread.sleep(10);
        }
        return null;
    }

    private static boolean contains(long[] ids, long id) {
        for (long x : ids) {
            if (x == id) {
                return true;
            }
        }
        return false;
    }
}
