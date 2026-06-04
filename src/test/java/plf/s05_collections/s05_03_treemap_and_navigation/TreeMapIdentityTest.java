package plf.s05_collections.s05_03_treemap_and_navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class TreeMapIdentityTest {

    @Test
    void bigDecimalScaleCollapsesKeys() {
        TreeMap<BigDecimal, String> m = new TreeMap<>();
        m.put(new BigDecimal("1.0"), "first");
        m.put(new BigDecimal("1.00"), "second");

        // EXERCISE: starts red
        assertEquals(2, m.size());                       // <-- fix to the true value

        assertTrue(m.containsKey(new BigDecimal("1.000")));
    }
}
