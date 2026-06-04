package plf.s06_concurrency.s06_13_performance_and_scalability;

import java.util.concurrent.atomic.AtomicLong;

public final class AtomicCounter implements Counter {

    private final AtomicLong value = new AtomicLong();

    @Override
    public void increment() {
        value.incrementAndGet();
    }

    @Override
    public long read() {
        return value.get();
    }
}
