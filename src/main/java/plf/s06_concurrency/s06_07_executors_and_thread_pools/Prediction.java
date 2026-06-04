package plf.s06_concurrency.s06_07_executors_and_thread_pools;

public final class Prediction {

    private Prediction() {
    }

    // core=2, max=10, new LinkedBlockingQueue<>(), 50 tasks. Peak worker threads?
    public static final int BEAT_1_PEAK_THREADS = -1;   // <-- write your prediction

    // core=2, max=50, new SynchronousQueue<>(), 50 tasks. Peak worker threads?
    public static final int BEAT_2_PEAK_THREADS = -1;   // <-- write your prediction
}
