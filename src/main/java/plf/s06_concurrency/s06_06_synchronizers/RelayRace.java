package plf.s06_concurrency.s06_06_synchronizers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RelayRace {

    private RelayRace() {
    }

    public static List<String> run() throws InterruptedException {
        List<String> log = new ArrayList<>();
        latch(log);
        barrier(log);
        semaphore(log);
        phaser(log);
        exchanger(log);
        return log;
    }

    // ---- CountDownLatch: the starter's gun (one-shot) ----
    private static void latch(List<String> log) throws InterruptedException {
        CountDownLatch gun = new CountDownLatch(1);
        gun.countDown();
        gun.countDown();
        gun.await();
        log.add("latch.count=" + gun.getCount());
        gun.await();
        log.add("latch.secondAwaitReturned");
    }

    // ---- CyclicBarrier: regroup each lap; last arriver runs the action ----
    private static void barrier(List<String> log) throws InterruptedException {
        Shared shared = new Shared();
        CyclicBarrier lap = new CyclicBarrier(3, () -> log.add("barrier.action.total=" + shared.total));
        Runnable runner = () -> {
            try {
                for (int i = 0; i < 2; i++) {
                    shared.total += 10;
                    lap.await();
                }
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        Thread a = new Thread(runner);
        Thread b = new Thread(runner);
        Thread c = new Thread(runner);
        a.start();
        b.start();
        c.start();
        a.join();
        b.join();
        c.join();
        log.add("barrier.final.total=" + shared.total);
    }

    // ---- Semaphore: N lanes; release is not ownership-bound (just a counter) ----
    private static void semaphore(List<String> log) throws InterruptedException {
        Semaphore lanes = new Semaphore(2);
        lanes.acquire();
        lanes.release();
        lanes.release();
        lanes.release();
        log.add("semaphore.permits=" + lanes.availablePermits());
    }

    // ---- Phaser: runners deregister; onAdvance terminates the phaser ----
    private static void phaser(List<String> log) {
        Phaser phaser = new Phaser(1) {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                return true;
            }
        };
        int reached = phaser.arrive();
        log.add("phaser.arriveReturned=" + reached);
        log.add("phaser.terminated=" + phaser.isTerminated());
        int afterTerm = phaser.arriveAndAwaitAdvance();
        log.add("phaser.afterTerminationPhase=" + afterTerm);
    }

    // ---- Exchanger: two-party rendezvous; alone it never completes ----
    private static void exchanger(List<String> log) {
        Exchanger<String> baton = new Exchanger<>();
        try {
            baton.exchange("solo", 50, TimeUnit.MILLISECONDS); // <-- no partner ever arrives
            log.add("exchanger.UNREACHABLE");
        } catch (TimeoutException e) {
            log.add("exchanger.outcome=TimeoutException");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class Shared {
        int total; // <-- intentionally NOT volatile
    }
}
