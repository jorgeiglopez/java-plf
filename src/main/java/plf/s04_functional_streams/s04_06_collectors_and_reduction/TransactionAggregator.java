package plf.s04_functional_streams.s04_06_collectors_and_reduction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class TransactionAggregator {

    public record Tx(String merchant, BigDecimal amount) {}

    // The liar: returns the right answer serially, the wrong answer in parallel.
    public static List<Tx> collectAll(Stream<Tx> txns) {
        return txns.reduce(
                new ArrayList<Tx>(),
                (list, t) -> { list.add(t); return list; },
                (a, b) -> { a.addAll(b); return a; });
    }

    // Task 1: re-implement so serial and parallel agree. Do NOT use reduce.
    public static List<Tx> collectAllFixed(Stream<Tx> txns) {
        throw new UnsupportedOperationException("TODO");
    }

    // Task 2: total amount per merchant, summed with BigDecimal (no double).
    public static Map<String, BigDecimal> totalPerMerchant(Stream<Tx> txns) {
        throw new UnsupportedOperationException("TODO");
    }

    // Task 3 (coda): return something the caller can iterate more than once.
    public static List<Tx> findByMerchant(Stream<Tx> txns, String merchant) {
        throw new UnsupportedOperationException("TODO");
    }
}
