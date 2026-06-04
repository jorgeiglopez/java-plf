package plf.s06_concurrency.s06_01_thread_fundamentals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

public final class StateObservatory {

    public static final long SLEEPER_MILLIS = 60_000L;

    private final Object monitor = new Object();
    private final Object blockerPark = new Object();

    private final CountDownLatch waiterInside = new CountDownLatch(1);
    private final CountDownLatch blockerHasLock = new CountDownLatch(1);
    private final CountDownLatch sleeperStarted = new CountDownLatch(1);

    private Thread waiter;
    private Thread blocker;
    private Thread contender;
    private Thread sleeper;

    public void start() throws InterruptedException {
        waiter = new Thread(this::waiterBody, "waiter");
        blocker = new Thread(this::blockerBody, "blocker");
        contender = new Thread(this::contenderBody, "contender");
        sleeper = new Thread(this::sleeperBody, "sleeper");

        waiter.start();
        waiterInside.await();
        spinUntil(waiter, Thread.State.WAITING);

        blocker.start();
        blockerHasLock.await();

        contender.start();
        spinUntil(contender, Thread.State.BLOCKED);

        sleeper.start();
        sleeperStarted.await();
        spinUntil(sleeper, Thread.State.TIMED_WAITING);
    }

    public Thread.State waiterState()    { return waiter.getState(); }
    public Thread.State contenderState() { return contender.getState(); }
    public Thread.State sleeperState()   { return sleeper.getState(); }

    public void stop() {
        synchronized (monitor) { monitor.notifyAll(); }
        synchronized (blockerPark) { blockerPark.notifyAll(); }
        sleeper.interrupt();
        join(waiter); join(blocker); join(contender); join(sleeper);
    }

    private void waiterBody() {
        synchronized (monitor) {
            waiterInside.countDown();
            try {
                monitor.wait(0L);           // <-- the line under the microscope
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void blockerBody() {
        synchronized (monitor) {
            blockerHasLock.countDown();
            synchronized (blockerPark) {
                try {
                    blockerPark.wait(0L);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void contenderBody() {
        synchronized (monitor) {
            // unreachable until the blocker releases the monitor
        }
    }

    private void sleeperBody() {
        sleeperStarted.countDown();
        try {
            Thread.sleep(SLEEPER_MILLIS);
        } catch (InterruptedException ignored) {
        }
    }

    private static void spinUntil(Thread t, Thread.State target) {
        while (t.getState() != target) {
            LockSupport.parkNanos(100_000L);
        }
    }

    private static void join(Thread t) {
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
