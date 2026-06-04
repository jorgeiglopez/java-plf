package plf.s04_functional_streams.s04_06_collectors_and_reduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import plf.s04_functional_streams.s04_06_collectors_and_reduction.TransactionAggregator.Tx;

class TransactionAggregatorTest {

    private static List<Tx> sample() {
        List<Tx> txns = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            String merchant = "m" + (i % 7);
            txns.add(new Tx(merchant, BigDecimal.valueOf(i)));
        }
        return txns;
    }

    // EXERCISE: starts red
    @Test
    void serialAndParallelAgreeOnFullCollection() {
        List<Tx> data = sample();
        List<Tx> expected = new ArrayList<>(data);
        for (int run = 0; run < 1000; run++) {
            List<Tx> serial = TransactionAggregator.collectAllFixed(data.stream());
            List<Tx> parallel = TransactionAggregator.collectAllFixed(data.parallelStream());
            assertEquals(expected.size(), parallel.size(), "parallel dropped/duplicated elements on run " + run);
            assertEquals(new HashSet<>(expected), new HashSet<>(parallel), "parallel lost elements on run " + run);
            assertEquals(serial, expected, "serial must also be correct, not just equal to parallel");
        }
    }

    // EXERCISE: starts red
    @Test
    void totalsPerMerchantUseBigDecimal() {
        List<Tx> data = sample();
        Map<String, BigDecimal> serial = TransactionAggregator.totalPerMerchant(data.stream());
        Map<String, BigDecimal> parallel = TransactionAggregator.totalPerMerchant(data.parallelStream());

        Map<String, BigDecimal> expected = data.stream()
                .collect(Collectors.groupingBy(Tx::merchant,
                        Collectors.reducing(BigDecimal.ZERO, Tx::amount, BigDecimal::add)));

        assertEquals(expected, serial);
        assertEquals(expected, parallel);
    }

    // EXERCISE: starts red
    @Test
    void findByMerchantIsReiterable() {
        List<Tx> data = sample();
        List<Tx> result = TransactionAggregator.findByMerchant(data.stream(), "m3");

        Set<String> first = new HashSet<>();
        for (Tx t : result) first.add(t.merchant());
        Set<String> second = new HashSet<>();
        for (Tx t : result) second.add(t.merchant());

        assertEquals(first, second, "result must be traversable more than once");
        assertTrue(result.stream().allMatch(t -> t.merchant().equals("m3")));
        assertEquals(5_000 / 7 + (5_000 % 7 > 3 ? 1 : 0), result.size());
    }
}
