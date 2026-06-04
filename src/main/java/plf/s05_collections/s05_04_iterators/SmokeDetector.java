package plf.s05_collections.s05_04_iterators;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public final class SmokeDetector {

    public sealed interface Outcome permits Completed, Threw {}

    public record Completed(List<Integer> result) implements Outcome {}

    public record Threw() implements Outcome {}

    private SmokeDetector() {}

    public static Outcome removeEvensWithForEach(List<Integer> start) {
        List<Integer> list = new ArrayList<>(start);
        try {
            for (Integer x : list) {
                if (x % 2 == 0) {
                    list.remove(x);
                }
            }
            return new Completed(list);
        } catch (ConcurrentModificationException e) {
            return new Threw();
        }
    }

    public static Outcome predictLength3() {
        throw new UnsupportedOperationException("TODO"); // <-- Task 1: replace with your predicted Outcome
    }

    public static Outcome predictLength4() {
        throw new UnsupportedOperationException("TODO"); // <-- Task 2: replace with your predicted Outcome
    }
}
