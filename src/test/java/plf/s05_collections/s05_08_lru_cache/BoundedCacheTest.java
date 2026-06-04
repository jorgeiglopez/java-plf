package plf.s05_collections.s05_08_lru_cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoundedCacheTest {

    @Test
    void capacityIsRespected() {
        BoundedCache<Integer, String> cache = new BoundedCache<>(3);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        cache.put(4, "d");

        assertEquals(3, cache.size());
        assertFalse(cache.containsKey(1));
    }

    // EXERCISE: starts red
    @Test
    void accessKeepsHotEntryAlive() {
        BoundedCache<Integer, String> cache = new BoundedCache<>(3);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");

        cache.get(1); // touch the hot key

        cache.put(4, "d");

        assertTrue(cache.containsKey(1), "accessed key must survive");
        assertFalse(cache.containsKey(2), "untouched eldest must be evicted");
    }
}
