package plf.s06_concurrency.s06_17_reactive_flow;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public final class BackpressureLab {

    public static final int BUFFER_CAPACITY = 4;

    /**
     * A subscriber whose {@code onNext} parks on {@code gate} until the test releases it,
     * so the producer's buffer fills deterministically (no sleeps, no races).
     */
    public static final class GatedSubscriber implements Flow.Subscriber<Integer> {

        private final CountDownLatch gate;
        private final long initialRequest;
        private final boolean reRequestOne;
        private final AtomicInteger received = new AtomicInteger();
        private final CountDownLatch firstDelivered = new CountDownLatch(1);
        private volatile Flow.Subscription subscription;
        private volatile Throwable error;

        private GatedSubscriber(CountDownLatch gate, long initialRequest, boolean reRequestOne) {
            this.gate = gate;
            this.initialRequest = initialRequest;
            this.reRequestOne = reRequestOne;
        }

        /** Greedy: grants unbounded demand up front, never re-requests. */
        public static GatedSubscriber greedy(CountDownLatch gate) {
            return new GatedSubscriber(gate, Long.MAX_VALUE, false);
        }

        /** Disciplined: grants one, then one more per delivered item. */
        public static GatedSubscriber oneAtATime(CountDownLatch gate) {
            // <-- Task: make this subscriber drive flow with single-item demand
            throw new UnsupportedOperationException("TODO: build the one-at-a-time subscriber");
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(initialRequest);
        }

        @Override
        public void onNext(Integer item) {
            received.incrementAndGet();
            firstDelivered.countDown();
            await(gate);
            if (reRequestOne) {
                subscription.request(1);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            this.error = throwable;
        }

        @Override
        public void onComplete() {
        }

        public int received() {
            return received.get();
        }

        public Throwable error() {
            return error;
        }

        /** Blocks until the first item has reached {@code onNext}. */
        public void awaitFirstDelivery() {
            await(firstDelivered);
        }

        private static void await(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private BackpressureLab() {
    }
}
