package plf.s05_collections.s05_06_special_and_legacy_collections;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class SpecialMaps {

    public enum Day {
        MON,
        TUE,
        WED,
        THU,
        FRI
    }

    private SpecialMaps() {}

    static Map<Day, String> enumMap() {
        Map<Day, String> m = new EnumMap<>(Day.class);
        m.put(Day.WED, "c");
        m.put(Day.MON, "a");
        return m;
    }

    static Map<Day, String> hashCopyOf(Map<Day, String> source) {
        return new HashMap<>(source);
    }

    static boolean equalsAcrossTypes(Map<Day, String> a, Map<Day, String> b) {
        throw new UnsupportedOperationException("TODO"); // <-- Task 1
    }

    static List<Day> iterationOrder(Map<Day, String> m) {
        return new ArrayList<>(m.keySet());
    }

    static int identityKeyCount() {
        Map<String, Integer> m = new IdentityHashMap<>();
        m.put(new String("US"), 1); // <-- Task 2: two distinct String objects...
        m.put(new String("US"), 2); // ...both with value "US"
        return m.size();
    }

    static boolean nullValueIsPresent() {
        Map<Day, String> m = new EnumMap<>(Day.class);
        m.put(Day.MON, null);
        throw new UnsupportedOperationException("TODO"); // <-- Task 3
    }
}
