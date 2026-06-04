package plf.s04_functional_streams.s04_05_streams_basics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class LazyCount {

    private LazyCount() {}

    public static int touchesWhenElided() {
        AtomicInteger touches = new AtomicInteger();
        long n = Stream.of("a", "b", "c", "d")
                .peek(s -> touches.incrementAndGet())
                .count();
        if (n != 4) {
            throw new AssertionError("source size changed: " + n);
        }
        return touches.get();
    }

    public static int touchesWhenFiltered() {
        AtomicInteger touches = new AtomicInteger();
        long n = Stream.of("a", "b", "c", "d")
                .filter(s -> true)
                .peek(s -> touches.incrementAndGet())
                .count();
        if (n != 4) {
            throw new AssertionError("source size changed: " + n);
        }
        return touches.get();
    }

    // Returns true if the throwing peek actually fired (i.e. the exception escaped).
    public static boolean throwingPeekFires(boolean withFilter) {
        try {
            Stream<String> s = Stream.of("a", "b", "c");
            if (withFilter) {
                s = s.filter(x -> true);
            }
            s.peek(x -> {
                throw new IllegalStateException("touched " + x);
            }).count();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
