package plf.s02_oop_class_design.s02_07_object_methods_contracts;

// EXERCISE: this test starts red.

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Object contracts: compareTo inconsistent with equals merges line items")
class LedgerLeakTest {

    // ---- WRITE YOUR FOUR PREDICTIONS, THEN RUN ----
    static final boolean PREDICT_EQUALS_200_VS_20 = false;      // <-- write your prediction
    static final int PREDICT_TREESET_SIZE = 0;                  // <-- write your prediction
    static final boolean PREDICT_TREESET_CONTAINS_2000 = false; // <-- write your prediction
    static final boolean PREDICT_HASHSET_CONTAINS_20 = false;   // <-- write your prediction
    // ------------------------------------------------

    private static Money usd(String amount) {
        return new Money(amount, "USD");
    }

    @Test
    @DisplayName("equals says different, the sorted set says same")
    void treeSetSilentlyMergesEqualAmounts() {
        Set<Money> hashSet = new HashSet<>();
        hashSet.add(usd("2.00"));
        hashSet.add(usd("2.0"));

        Set<Money> treeSet = new TreeSet<>();
        treeSet.add(usd("2.00"));
        treeSet.add(usd("2.0"));

        assertEquals(PREDICT_EQUALS_200_VS_20, usd("2.00").equals(usd("2.0")),
                "equals(USD 2.00, USD 2.0)");

        assertEquals(PREDICT_TREESET_SIZE, treeSet.size(),
                "treeSet.size() after adding both 2.00 and 2.0");

        assertEquals(PREDICT_TREESET_CONTAINS_2000, treeSet.contains(usd("2.000")),
                "treeSet.contains(USD 2.000)");

        assertEquals(PREDICT_HASHSET_CONTAINS_20, hashSet.contains(usd("2.0")),
                "hashSet.contains(USD 2.0)");
    }
}
