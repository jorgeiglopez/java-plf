package plf.s01_core_language.s01_07_object_class_contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Do NOT change MutableKeyPoint, the put/mutate calls, or the assertions. Only edit the PREDICT_* constants.
@DisplayName("Object contract: mutable HashMap key becomes a ghost entry")
class GhostInTheMapTest {

    // ---- EDIT ONLY THESE FOUR LINES ----
    static final String PREDICT_GET_AFTER_MUTATION = "origin-ish";
    static final boolean PREDICT_CONTAINS_KEY = true;
    static final boolean PREDICT_CONTAINS_VALUE = true;
    static final int PREDICT_SIZE_AFTER_REINSERT = 1;
    // ------------------------------------

    @Test
    @DisplayName("get / containsKey / containsValue disagree once the key mutates")
    void mutableKeyCorruptsLookup() {
        Map<MutableKeyPoint, String> map = new HashMap<>();
        MutableKeyPoint key = new MutableKeyPoint(1, 2);
        map.put(key, "origin-ish");

        assertEquals("origin-ish", map.get(key), "sanity: lookup works before mutation");
        assertEquals(1, map.size(), "sanity: one entry before mutation");

        key.setX(2);

        assertEquals(PREDICT_GET_AFTER_MUTATION, map.get(key),
                "REVEAL: get(key) after mutation is actually null (key hashes to a new, empty bucket)");

        assertEquals(PREDICT_CONTAINS_KEY, map.containsKey(key),
                "REVEAL: containsKey(key) is actually false (same broken hash path as get)");

        assertEquals(PREDICT_CONTAINS_VALUE, map.containsValue("origin-ish"),
                "REVEAL: containsValue is actually true (linear scan, never hashes the key)");

        map.put(key, "second");
        assertEquals(PREDICT_SIZE_AFTER_REINSERT, map.size(),
                "REVEAL: size() is actually 2 — the same object is now 'in' the map twice (ghost entry)");
    }

    @Test
    @DisplayName("the contract held at every instant; only mutation-across-time broke it")
    void contractIsInternallyConsistent() {
        MutableKeyPoint a = new MutableKeyPoint(3, 4);
        MutableKeyPoint b = new MutableKeyPoint(3, 4);
        assertTrue(a.equals(b), "equal points are equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal points share a hashCode (contract upheld)");

        MutableKeyPoint c = new MutableKeyPoint(5, 6);
        assertFalse(a.equals(c), "different points are not equal");
        assertNull(null, "placeholder to keep the contract demo self-contained");
    }
}
