package plf.s06_concurrency.s06_05_concurrent_collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WordCounter {

    private final ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();

    // ---- Part A: fix this method's body. Do NOT change the signature. ----
    public void record(String word) {
        Integer current = counts.get(word);          // <-- Task A: the atomic boundary splits here
        counts.put(word, current == null ? 1 : current + 1);
    }

    public int count(String word) {
        return counts.getOrDefault(word, 0);
    }

    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
