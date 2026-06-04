# HashMap Internals — The Key That Got Lost

**Concept:** a `HashMap` entry is found by *re-deriving its bucket from the key's current `hashCode()`* — so mutating a key after insertion strands it, and a `Set`-backed `keySet()` can even end up holding the "same" key twice.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
You insert a key, then mutate one of the fields that its `hashCode()` depends on. The entry is *physically still in the table* — `values()` can see it — but `get(key)` returns `null`. Then you `put` the same object again and `size()` climbs to 2. One object, two entries, in a `Map` whose key set is supposed to be a `Set`. Why?

`HashMap` stores an entry in bucket `(table.length - 1) & hash(key.hashCode())`, where `hash(h) = h ^ (h >>> 16)` (the "spread" that mixes high bits down). Lookup recomputes that bucket *from the key you hand it right now*. If the key's hash changed since insertion, lookup walks the wrong bucket.

## Task
Open `StrandedKey.java`. The `Key` class has `hashCode() == id` and an `equals` based on `id`. Scenario: a `Key a` with `id=1` is `put` into a fresh `HashMap`, then `a.id` is set to `999`, then `a` is `put` again with a new value.

1. **Before running anything**, write your predicted value for each probe in `predictions.txt`:
   ```
   get(a)         = ?
   containsKey(a) = ?
   values.contains("alpha") = ?
   size (before second put) = ?
   size (after second put)  = ?
   get(a) (after)           = ?
   keySet() prints          = ?
   ```
2. Run `StrandedKey.main` and compare.
3. In one sentence each, explain (a) why `get(a)` returns `null` while `values()` still sees the value, and (b) why the second `put` *adds* an entry instead of overwriting.

## Constraints
- Java 21, no external libraries.
- Commit your `predictions.txt` answers **before** running anything.
- Do not change `StrandedKey.java`'s logic — only read it.

## Scale up (Optional — treeification coda)
A bin (single bucket) converts to a red-black tree only when `TREEIFY_THRESHOLD = 8` **and** `table.length >= MIN_TREEIFY_CAPACITY = 64`; otherwise the map *resizes* instead.

In `StrandedKeyTest.predictTreeification`, set `binBecomesRedBlackTree` to your prediction for: *if you cram 8 keys that all collide into the same bucket of a fresh default-capacity (16) `HashMap`, does that bin become a red-black tree?* The test is red until your prediction matches reality.

## How to verify
```
# run the harness
java -cp target/classes plf.s05_collections.s05_02_hashmap_internals.StrandedKey

# the two behavior tests should pass; the coda test is red until you answer it
mvn -q -Dtest=StrandedKeyTest test
```

<details><summary>Hint (open only if stuck)</summary>
With `hashCode() == id`: at `id=1` the spread is `1 ^ (1>>>16) = 1`, bucket `15 & 1 = 1`. At `id=999`, bucket `15 & 999 = 7`. Insertion put it in bucket 1; lookup now searches bucket 7. For the coda: 8 collisions in a 16-slot table — which threshold is checked *first*?
</details>
