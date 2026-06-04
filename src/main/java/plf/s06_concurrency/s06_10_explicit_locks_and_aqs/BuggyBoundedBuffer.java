package plf.s06_concurrency.s06_10_explicit_locks_and_aqs;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class BuggyBoundedBuffer {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    private final int[] buf;
    private int head = 0;
    private int tail = 0;
    private int count = 0;

    public BuggyBoundedBuffer(int capacity) {
        this.buf = new int[capacity];
    }

    public void put(int value) throws InterruptedException {
        lock.lock();
        try {
            if (count == buf.length) cond.await();
            buf[tail] = value;
            tail = (tail + 1) % buf.length;
            count++;
            cond.signal();
        } finally {
            lock.unlock();
        }
    }

    public int take() throws InterruptedException {
        lock.lock();
        try {
            if (count == 0) cond.await();
            int v = buf[head];
            head = (head + 1) % buf.length;
            count--;
            cond.signal();
            return v;
        } finally {
            lock.unlock();
        }
    }

    int consumersWaiting() {
        lock.lock();
        try {
            return lock.getWaitQueueLength(cond);
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
