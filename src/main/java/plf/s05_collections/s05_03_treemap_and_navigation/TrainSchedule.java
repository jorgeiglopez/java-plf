package plf.s05_collections.s05_03_treemap_and_navigation;

import java.util.NavigableMap;
import java.util.TreeMap;

public final class TrainSchedule {

    private final NavigableMap<Integer, String> departures = new TreeMap<>();

    public void add(int minuteOfDay, String destination) {
        departures.put(minuteOfDay, destination);
    }

    // "The next train at or after t." Return null if none.
    public String nextAtOrAfter(int t) {
        throw new UnsupportedOperationException("TODO");       // <-- Task 1
    }

    // "The last train at or before t." Return null if none.
    public String lastAtOrBefore(int t) {
        throw new UnsupportedOperationException("TODO");       // <-- Task 2
    }

    // "The strictly NEXT train after t" (t itself does not count). Return null if none.
    public String strictlyAfter(int t) {
        throw new UnsupportedOperationException("TODO");       // <-- Task 3
    }

    // Destinations departing in the window [from, to) -- to is EXCLUSIVE.
    public NavigableMap<Integer, String> between(int from, int to) {
        throw new UnsupportedOperationException("TODO");       // <-- Task 4
    }
}
