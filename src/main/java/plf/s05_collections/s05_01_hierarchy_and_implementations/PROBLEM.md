# Hierarchy & Implementations — The Phantom Key

**Concept:** a `HashMap` freezes a key's bucket from its `hashCode()` at insertion time; mutate a mutable key afterward and the entry haunts a map that swears it isn't there.
**Difficulty:** ★★★ senior
**Est. time:** ~15 min

## Background
You build a lookup keyed by a `List<Integer>` — a perfectly legal key, since `List` defines a content-based `hashCode`/`equals`. You insert one entry, keep building the list, then look it up by the *same reference*. Nobody flags appending to a list as dangerous. Yet the lookup fails, the entry can't be evicted, and re-inserting the same reference **grows** the map. No `setX`, no clever subclass — just `list.add(...)`.

Can you predict, before running, exactly what the map reports?

## Task
1. Open `PhantomKeyTest.java` and commit to two predictions by editing the two constants:
   - `PREDICTED_CONTAINS` — after the innocent `key.add(3)`, does `map.containsKey(key)` (the *same reference*) return `true` or `false`?
   - `PREDICTED_SIZE` — after mutating the key and then `map.put(key, value)` with that same reference, what is `map.size()`?
2. Implement `PhantomKey.sizeAfterRePut(map, key, value)` (currently a `TODO`): re-insert the (already mutated) `key` with `value`, then return `map.size()`.
3. Make `PhantomKeyTest` green by getting both predictions right AND implementing Task 2 correctly.
4. In one sentence, write down *why* the same reference is simultaneously "not contained" and "physically in the key set".

## Constraints
- Java 21, no external libraries.
- Do **not** weaken the assertions in the test to pass them — fix your predictions and implement the stub.
- `containsAfterMutation` deliberately mutates the key; leave that mutation in place.

## How to verify
`mvn -q -Dtest=PhantomKeyTest test`

You can also eyeball the live demo: `java plf.s05_collections.s05_01_hierarchy_and_implementations.PhantomKey`

## Scale up (Optional)
- **Defuse it:** change the key type so the corruption becomes impossible. Which one collection-design choice (key immutability) fixes every symptom at once?
- **Where it does NOT bite:** repeat the experiment with an `IdentityHashMap`. Does mutating the key still hide it? Explain using how that map computes buckets.
- **Sibling trap:** swap `HashMap` for a `TreeMap<List<Integer>, String>` built with the *natural* ordering of lists — does it even let you insert a `List` key? What about a `TreeMap` whose `Comparator` disagrees with `equals`?

<details><summary>Hint (open only if stuck)</summary>
A `HashMap` chooses a bucket from `key.hashCode()` <em>at put time</em> and never recomputes it for stored entries. `ArrayList.hashCode()` is derived from its current contents. So after `add(3)` the key's hash points at a <em>different</em> bucket than the one its entry lives in: lookups search the new bucket (empty), but iteration walks the table and still finds the entry sitting in the old bucket.
</details>
