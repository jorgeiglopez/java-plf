package plf.s06_concurrency.s06_05_concurrent_collections;

import java.util.concurrent.ConcurrentHashMap;

public final class FibMemo {

    // A key whose hash is fixed so every key collides into ONE bin.
    public record Key(int n) {
        @Override
        public int hashCode() {
            return 0; // <-- forces all keys into the same bin
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.n == n;
        }
    }

    private final ConcurrentHashMap<Key, Long> memo = new ConcurrentHashMap<>();

    // <-- Part B step 1: run this via the test, then explain why it throws
    public long fib(int n) {
        return memo.computeIfAbsent(new Key(n), k ->
                k.n() < 2 ? (long) k.n() : fib(k.n() - 1) + fib(k.n() - 2));
    }

    // ---- Part B: implement a memoization that does NOT re-enter the bin lock. ----
    public long fibSafe(int n) {
        throw new UnsupportedOperationException("TODO");
    }
}
