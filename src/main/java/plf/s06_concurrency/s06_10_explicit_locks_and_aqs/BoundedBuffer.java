package plf.s06_concurrency.s06_10_explicit_locks_and_aqs;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class BoundedBuffer {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private final int[] buf;
    private int head = 0;
    private int tail = 0;
    private int count = 0;

    public BoundedBuffer(int capacity) {
        this.buf = new int[capacity];
    }

    public void put(int value) throws InterruptedException {
        throw new UnsupportedOperationException("TODO");   // <-- implement (Task 1)
    }

    public int take() throws InterruptedException {
        throw new UnsupportedOperationException("TODO");   // <-- implement (Task 2)
    }

    // ---- test-coordination probe: do NOT use inside put/take ----
    int consumersWaiting() {
        lock.lock();
        try {
            return lock.getWaitQueueLength(notEmpty);
        } finally {
            lock.unlock();
        }
    }

    int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
