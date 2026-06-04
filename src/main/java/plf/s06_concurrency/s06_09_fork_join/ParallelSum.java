package plf.s06_concurrency.s06_09_fork_join;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.LongAdder;

public final class ParallelSum {

    static final int THRESHOLD = 10_000;

    private ParallelSum() {
    }

    public static final class Result {
        public final long sum;
        public final Map<String, LongAdder> leavesPerWorker;

        Result(long sum, Map<String, LongAdder> leavesPerWorker) {
            this.sum = sum;
            this.leavesPerWorker = leavesPerWorker;
        }
    }

    public static Result run(long[] data, ForkJoinPool pool) {
        Map<String, LongAdder> histogram = new ConcurrentHashMap<>();
        SumTask root = new SumTask(data, 0, data.length, histogram);
        long sum = pool.submit(root).join();
        return new Result(sum, histogram);
    }

    static final class SumTask extends RecursiveTask<Long> {
        private final long[] data;
        private final int lo;
        private final int hi;
        private final Map<String, LongAdder> histogram;

        SumTask(long[] data, int lo, int hi, Map<String, LongAdder> histogram) {
            this.data = data;
            this.lo = lo;
            this.hi = hi;
            this.histogram = histogram;
        }

        @Override
        protected Long compute() {
            int len = hi - lo;
            if (len <= THRESHOLD) {
                histogram.computeIfAbsent(
                        Thread.currentThread().getName(), k -> new LongAdder()).increment();
                long s = 0;
                for (int i = lo; i < hi; i++) {
                    s += data[i];
                }
                return s;
            }
            int mid = lo + len / 2;
            SumTask left = new SumTask(data, lo, mid, histogram);
            SumTask right = new SumTask(data, mid, hi, histogram);

            // ---- Task: introduce fork/compute/join so the halves run in parallel ----
            long l = left.compute();            // <-- both halves run on THIS thread
            long r = right.compute();           // <-- nothing is ever forked
            return l + r;
        }
    }
}
