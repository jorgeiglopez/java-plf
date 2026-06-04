package plf.s05_collections.s05_02_hashmap_internals;

import java.util.HashMap;
import java.util.Map;

public final class StrandedKey {

    public static final class Key {
        public int id;

        public Key(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && k.id == this.id;
        }

        @Override
        public String toString() {
            return "Key(" + id + ")";
        }
    }

    public static Map<Key, String> buildAndStrand() {
        Map<Key, String> map = new HashMap<>();
        Key a = new Key(1);
        Key b = new Key(2);
        map.put(a, "alpha");
        map.put(b, "beta");

        a.id = 999;        // <-- mutate the key AFTER it is in the map

        return map;
    }

    public static void main(String[] args) {
        Map<Key, String> map = new HashMap<>();
        Key a = new Key(1);
        map.put(a, "alpha");

        a.id = 999;

        System.out.println("get(a)        = " + map.get(a));
        System.out.println("containsKey(a)= " + map.containsKey(a));
        System.out.println("values has    = " + map.values().contains("alpha"));
        System.out.println("size (before) = " + map.size());

        map.put(a, "alpha2");   // same object reference, now-current id

        System.out.println("size (after)  = " + map.size());
        System.out.println("get(a)        = " + map.get(a));
        System.out.println("keys          = " + map.keySet());
    }
}
