# LRU Cache — The cache that forgets its favourite key

**Concept:** an LRU cache built on `LinkedHashMap` — and the single constructor flag that makes a read count as *use*.
**Difficulty:** ★★★ senior
**Est. time:** ~15 min

## Background
`BoundedCache` extends `LinkedHashMap`, caps itself at `capacity`, and evicts via
`removeEldestEntry`. It compiles, it caps correctly, and the first test passes. But
it has an absurd flaw: a key you read on every single request still gets evicted the
moment a new key arrives. The cache forgets the *most popular* entry. That is not
Least-Recently-Used — it is First-In-First-Out wearing an LRU costume.

What does an LRU cache have that a FIFO one doesn't? In `LinkedHashMap`, exactly one
constructor argument.

## Task
1. Run the tests. `accessKeepsHotEntryAlive` is red: after `get(1)`, key `1` is the
   eldest by *insertion* order, so `put(4)` evicts it instead of the untouched key `2`.
2. Fix `BoundedCache` (production code, not the test) so that a `get` counts as a use
   and reorders recency. The change is one argument.
3. Both tests must go green. Do **not** weaken `capacityIsRespected`.

## Constraints
- Java 21, no external libraries.
- Edit only `BoundedCache.java`. Do **not** modify the test file.
- Keep `removeEldestEntry` returning `size() > capacity` (it is already correct —
  `>=` would silently cap you one entry short).

## Scale up (Optional)
1. **Peek without touching.** Add a `peek(K key)` that returns the value but does
   **not** count as a use (recency unchanged). Which `Map` methods reorder in
   access-order mode and which don't? Prove your `peek` doesn't promote the key.
2. **Iterate-while-getting.** Write a loop that iterates `entrySet()` and calls
   `get(...)` inside it on the same access-order cache. What happens, and why —
   single-threaded?

## How to verify
`mvn -q -Dtest=BoundedCacheTest test`

<details><summary>Hint (open only if stuck)</summary>
`LinkedHashMap(int, float, boolean)` — the third argument is `accessOrder`. With
`false` (the default) iteration/eviction order is insertion order; with `true`, every
`get`/`put` moves the entry to the most-recently-used end.
</details>
