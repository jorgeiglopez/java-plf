# Solution — The Phantom Key

## Ideal solution

Predictions that turn the test green:

```java
private static final boolean PREDICTED_CONTAINS = false; // contains(same ref) == false
private static final int PREDICTED_SIZE = 2;             // one reference, two entries
```

Task 2 — re-insert the already-mutated key and report the size:

```java
public static int sizeAfterRePut(Map<List<Integer>, String> map, List<Integer> key, String value) {
    map.put(key, value);
    return map.size();
}
```

Running the live demo prints:

```
size before     = 1
contains (same) = false
same reference  = true
```

The one-sentence answer: **the bucket is chosen from `key.hashCode()` at insert time and never recomputed, so after `add(3)` the key hashes to a different (empty) bucket — `containsKey` searches there and misses, while iteration walks the whole table and still finds the entry where it was originally placed.**

### Scale up solutions

- **Defuse it — make the key immutable.** Key by `List.copyOf(key)` (an unmodifiable snapshot) or by a `record`/value type whose components are final. Once the key's contents can't change after insertion, its `hashCode()` is stable and the bucket stays valid. The fix is a *collection-design* fix, not a `HashMap` patch: **never use a mutable object as a map key or set element while it's in the structure** (Effective Java Item 17 territory — favor immutability for keys).

- **`IdentityHashMap` does NOT bite.** It keys on reference identity, using `System.identityHashCode(key)` and `==`, both of which are independent of the list's contents. Mutating the list never changes its identity hash, so `containsKey(sameRef)` stays `true` no matter how many elements you append. (It's the wrong map semantically for content keys, but it is immune to this specific corruption.)

- **`TreeMap` sibling trap.** A `TreeMap<List<Integer>, String>` with *natural ordering* throws `ClassCastException` at the first `put`, because `List` is not `Comparable`. Give it a `Comparator<List<Integer>>` and it works — but `TreeMap` ignores `equals`/`hashCode` entirely and orders/locates keys **only** through the comparator. Mutating a key still breaks lookups (the tree's structure was built from the old comparison result), and a comparator inconsistent with `equals` makes `containsKey` return `false` for a key that is `.equals()` to one present. Different mechanism (comparator, not hash bucket), same lesson: **the key's positioning is frozen at insert time.**

## Why this works (and the gotcha)

`HashMap.put` computes `hash(key)`, masks it to an index, and stores the `Node` in that bucket. `HashSet` is `HashMap` underneath, so the same applies to set elements. The crucial fact: **stored entries cache nothing about being "movable"** — the map never re-buckets a live entry just because its key's contents changed. There is no observer on the key.

`ArrayList.hashCode()` (from `AbstractList`) is `1; for (e) hash = 31*hash + e.hashCode()` — a function of current contents. So:

- `[1, 2].hashCode()` = `994`  → entry parked in bucket(994).
- after `add(3)`, the same object's `hashCode()` = `30817` → `containsKey` probes bucket(30817), which is empty → **false**.
- the `Node` is still in bucket(994); iterating the table (`keySet().iterator()`) walks every bucket and yields that very reference → **`== key` is true**.
- `remove(key)` also probes bucket(30817) → misses → returns `null`/`false`; the entry is **unevictable** by key.
- `put(key, value)` again probes bucket(30817), finds nothing, inserts a *second* node → `size()` becomes **2**, one reference occupying two buckets.

The trap is devious precisely because the "mutation" is an innocent `list.add(...)`. Most developers brace at an obvious `setX`, but nobody thinks appending to a list they happened to use as a key is a structural hazard. It is.

## Common mistakes

- **Predicting `contains == true`** ("it's the same object!") → identity is irrelevant to a `HashMap`; only the *current* `hashCode()`/`equals` matter, and the hash moved.
- **Predicting `size == 1`** ("putting the same key just overwrites") → overwrite only happens when the new key lands in the same bucket and `equals` an existing entry; here it lands in a different bucket, so it's an insert.
- **"Just call `map.remove(key)` to clean up"** → it probes the new bucket and misses; the stale entry is stranded until you iterate-and-remove or rebuild the map.
- **Blaming `ArrayList`** → any mutable key with a content-based `hashCode` does this; the contract being violated is "keys must not change their hash while in the map", not an `ArrayList` bug.
- **Reaching for `Collections.synchronizedMap` / `ConcurrentHashMap`** → this is not a thread-safety bug; concurrency tools change nothing.

## Interviewer Q&A

**Q:** Why does `containsKey(sameReference)` return `false` after you appended to the list key?
**A:** `HashMap` locates a key by recomputing `hashCode()` and probing that bucket. After the append the key's content-derived hash changed, so it probes a different (empty) bucket and misses — even though the entry's `Node` is still physically in the table under the old hash.

**Q:** How can the same reference be "not contained" yet appear when you iterate the key set?
**A:** Lookups are hash-directed (probe one bucket); iteration is structural (walk every bucket). The entry never moved, so iteration still yields it while hash-directed lookup searches the wrong bucket.

**Q:** Re-putting the same reference made `size()` go from 1 to 2. Why isn't it an overwrite?
**A:** Overwrite requires the new key to hash into the existing entry's bucket and `equals` it. Here the new hash points elsewhere, so `put` inserts a brand-new node — one object now occupies two buckets.

**Q:** What's the correct rule for objects used as `HashMap` keys / `HashSet` elements?
**A:** They must be effectively immutable for the duration they're in the structure, at least with respect to fields that influence `equals`/`hashCode`. Prefer immutable keys (records, `List.copyOf`, `String`, boxed primitives).

**Q:** Does `IdentityHashMap` suffer the same problem? Why or why not?
**A:** No. It keys on `System.identityHashCode` and `==`, both independent of contents, so mutating the key never changes its bucket. It's immune to this corruption but has reference-equality semantics, which is usually not what you want for content keys.

**Q:** Would a `TreeMap` avoid this? What changes?
**A:** It dodges the *hash* mechanism but not the underlying lesson. `TreeMap` positions keys via `Comparable`/`Comparator`, so a plain `List` key throws `ClassCastException` (not `Comparable`), and even with a comparator the tree is built from comparisons made at insert time; mutating a key corrupts lookups, and a comparator inconsistent with `equals` makes `containsKey` lie. It also ignores `equals`/`hashCode` entirely.

**Q:** When choosing between `HashMap`, `LinkedHashMap`, and `TreeMap`, how does this trap inform your decision?
**A:** It doesn't change *which* you pick (that's about ordering and iteration cost) but reinforces a cross-cutting constraint: whatever map you choose, the key must be immutable while stored. `HashMap` for O(1) average unordered access, `LinkedHashMap` to preserve insertion (or access) order, `TreeMap` for sorted/navigable access at O(log n) — all share the frozen-key invariant.

**Q:** You discover a production map is "losing" entries this way. How do you recover the stranded entries?
**A:** You can't reach them by key lookup. Rebuild: iterate the entry set, snapshot each key into an immutable copy, and re-insert into a fresh map. Then fix the root cause by keying on immutable values so nothing can drift again.
