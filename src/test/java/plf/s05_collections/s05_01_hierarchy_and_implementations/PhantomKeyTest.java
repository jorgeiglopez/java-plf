package plf.s05_collections.s05_01_hierarchy_and_implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PhantomKeyTest {

    private static final boolean PREDICTED_CONTAINS = true; // <-- change me
    private static final int PREDICTED_SIZE = 1;            // <-- change me

    @Test
    void mutatedKeyVanishesButReferenceStillThere() {
        // EXERCISE: starts red
        List<Integer> key = new ArrayList<>(List.of(1, 2));
        Map<List<Integer>, String> map = PhantomKey.buildMap(key, "v");

        boolean contains = PhantomKey.containsAfterMutation(map, key);
        boolean stillInside = map.keySet().iterator().next() == key;

        assertEquals(PREDICTED_CONTAINS, contains);
        assertTrue(stillInside);
    }

    @Test
    void rePuttingSameReferenceGrowsTheMap() {
        List<Integer> key = new ArrayList<>(List.of(1, 2, 3));
        Map<List<Integer>, String> map = PhantomKey.buildMap(key, "v");
        key.add(4);

        int size = PhantomKey.sizeAfterRePut(map, key, "w");

        assertEquals(PREDICTED_SIZE, size);
        assertFalse(map.containsKey(new ArrayList<>(List.of(1, 2, 3))));
    }
}
