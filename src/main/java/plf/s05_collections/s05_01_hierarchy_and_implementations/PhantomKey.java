package plf.s05_collections.s05_01_hierarchy_and_implementations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PhantomKey {

    private PhantomKey() {
    }

    public static Map<List<Integer>, String> buildMap(List<Integer> key, String value) {
        Map<List<Integer>, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public static boolean containsAfterMutation(Map<List<Integer>, String> map, List<Integer> key) {
        key.add(3); // <-- the innocent mutation
        return map.containsKey(key);
    }

    public static int sizeAfterRePut(Map<List<Integer>, String> map, List<Integer> key, String value) {
        throw new UnsupportedOperationException("TODO"); // <-- Task 2 here
    }

    public static void main(String[] args) {
        List<Integer> key = new ArrayList<>(List.of(1, 2));
        Map<List<Integer>, String> map = buildMap(key, "v");
        System.out.println("size before     = " + map.size());
        System.out.println("contains (same) = " + containsAfterMutation(map, key));
        System.out.println("same reference  = " + map.keySet().iterator().next().equals(key));
    }
}
