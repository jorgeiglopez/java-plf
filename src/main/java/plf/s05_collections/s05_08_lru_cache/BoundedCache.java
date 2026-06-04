package plf.s05_collections.s05_08_lru_cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class BoundedCache<K, V> extends LinkedHashMap<K, V> {

    private final int capacity;

    public BoundedCache(int capacity) {
        super(16, 0.75f, false); // <-- one of these args is wrong for LRU
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
