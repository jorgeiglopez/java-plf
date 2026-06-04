package plf.s05_collections.s05_02_hashmap_internals;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import plf.s05_collections.s05_02_hashmap_internals.StrandedKey.Key;

class StrandedKeyTest {

    @Test
    void strandedKeyProbes() {
        Map<Key, String> map = new HashMap<>();
        Key a = new Key(1);
        map.put(a, "alpha");

        a.id = 999;

        assertNull(map.get(a));
        assertFalse(map.containsKey(a));
        assertTrue(map.values().contains("alpha"));
        assertEquals(1, map.size());
    }

    @Test
    void sameObjectBecomesTwoEntries() {
        Map<Key, String> map = new HashMap<>();
        Key a = new Key(1);
        map.put(a, "alpha");

        a.id = 999;
        map.put(a, "alpha2");

        assertEquals(2, map.size());
        assertEquals("alpha2", map.get(a));

        long distinctKeyObjects = map.keySet().stream().distinct().count();
        assertEquals(1, distinctKeyObjects);
    }

    // EXERCISE: starts red
    @Test
    void predictTreeification() {
        // ---- EDIT ONLY THIS VALUE: your prediction for whether 8 colliding
        // ----                       keys in a default (cap 16) HashMap treeify
        boolean binBecomesRedBlackTree = true;   // <-- change to your answer

        assertEquals(treeifiesUnderDefaultCapacity(), binBecomesRedBlackTree);
    }

    private static boolean treeifiesUnderDefaultCapacity() {
        return false;
    }
}
