# Solution — The cache that forgets its favourite key

## Ideal solution
Flip the third constructor argument (`accessOrder`) from `false` to `true`:

```java
public BoundedCache(int capacity) {
    super(16, 0.75f, true);   // accessOrder = true -> get() reorders recency
    this.capacity = capacity;
}

@Override
protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() > capacity;
}
```

That single character of meaning is the entire difference between FIFO and LRU.

### Scale up solutions

**1. Peek without touching.**
In access-order mode, a "use" is whatever calls the internal `get` logic:
`get`, `getOrDefault`, `compute`, `computeIfPresent`, `merge`, and `put` of an existing
key all move the entry to the MRU end. Methods that do **not** reorder:
`containsKey`, `containsValue`, `forEach`, `entrySet()`/`keySet()` iteration,
and the size/empty queries. So a non-promoting peek must avoid `get`:

```java
public V peek(K key) {
    return containsKey(key) ? rawValueWithoutPromoting(key) : null;
}
```

The catch: there is no public, recency-neutral value lookup — `get` is the only one,
and it promotes. Pragmatic options:
- iterate `entrySet()` (which does not reorder) and match the key, or
- accept that `peek` semantics simply aren't part of `LinkedHashMap`'s contract and
  document it.

```java
public V peek(K key) {
    for (Map.Entry<K, V> e : entrySet()) {       // entrySet iteration does NOT promote
        if (java.util.Objects.equals(e.getKey(), key)) {
            return e.getValue();
        }
    }
    return null;
}
```

Proof it doesn't promote: cap 3, put 1/2/3, `peek(1)`, `put(4)` → key `1` is still the
eldest, so `1` is evicted (a real `get(1)` would have saved it). That asymmetry is the
test.

**2. Iterate-while-getting.**
On an access-order map, `get` *structurally* modifies the map (it relinks the entry to
the MRU end and bumps `modCount`). So calling `get` while iterating `entrySet()` throws
`ConcurrentModificationException` — single-threaded, no second thread required:

```java
for (Map.Entry<Integer, String> e : cache.entrySet()) {
    cache.get(e.getKey());   // bumps modCount -> next iterator step throws CME
}
```

This is why "just wrap it in `Collections.synchronizedMap`" does not make an
access-order LRU cache safe to iterate concurrently: the read itself mutates, so an
external iteration races the very `get`s it is trying to observe.

## Why this works (and the gotcha)
`LinkedHashMap` keeps a doubly-linked list threaded through its entries. The
`accessOrder` flag chosen at construction decides what that list orders by:

- `accessOrder = false` (the default, and what `new LinkedHashMap<>(cap, 0.75f)` gives
  you) → **insertion order**. The eldest entry is the first one *put*. Reads never move
  anything. That is FIFO. Your hot key sits at the front forever and is evicted first.
- `accessOrder = true` → **access order**. Every `get`/`put` moves the touched entry to
  the back (most-recently-used). The eldest is the genuinely least-recently-*used*
  entry. That is LRU.

The trap is that the FIFO version *looks* correct: it caps, it evicts, and a test that
asserts `get(1)` returns `"a"` passes either way — insertion order returns the right
value too. The bug only surfaces when you assert on **eviction identity**: after
`get(1)`, is it key `2` that dies (LRU) or key `1` (FIFO)? That is exactly what
`accessKeepsHotEntryAlive` pins down with `containsKey(1)` true / `containsKey(2)` false.

`removeEldestEntry` is a predicate, not a remover: it returns a boolean and the map does
the removal. `size() > capacity` is correct; `>=` would evict while still at capacity and
silently cap you at `capacity - 1`.

## Common mistakes
- `new LinkedHashMap<>(cap, 0.75f)` or `accessOrder = false` → a FIFO cache that
  evicts hot keys; value-equality tests ship it green.
- Testing `assertEquals("a", cache.get(1))` instead of eviction identity → proves
  nothing; FIFO passes it.
- `size() >= capacity` in `removeEldestEntry` → off-by-one, holds `capacity - 1` entries.
- Mutating the map *inside* `removeEldestEntry` (`this.remove(...)`, `this.put(...)`):
  it runs mid-`put` inside `afterNodeInsertion`, before the structure settles. On Java 21
  you get no clean exception — just a silently wrong size or a lost entry. Only read
  `eldest` and decide; never restructure there.
- Assuming `Collections.synchronizedMap(cache)` makes it safe — in access-order mode
  `get` mutates, so iteration still races and CME still fires.

## Interviewer Q&A

**Q:** What is the minimal change to turn a `LinkedHashMap` into an LRU cache?
**A:** Construct it with `accessOrder = true` (the three-arg constructor) and override
`removeEldestEntry` to return `size() > capacity`.

**Q:** What does `accessOrder = true` actually change at runtime?
**A:** It reorders the internal linked list on every access (`get`/`put`), moving the
touched entry to the most-recently-used end, so the eldest entry is the least-recently
*used* rather than the first inserted.

**Q:** Why isn't asserting that `get(1)` returns the right value enough to test LRU?
**A:** Both FIFO and LRU return the correct value for a present key; only the *eviction
order* differs. You must assert on which key survives eviction after an access.

**Q:** `>` or `>=` in `removeEldestEntry`, and why?
**A:** `>`. The method is called after the new entry is inserted, so at the moment of
the check `size()` already includes it; `size() > capacity` removes the eldest only when
you've genuinely exceeded capacity. `>=` evicts one too early.

**Q:** Can you iterate the cache and call `get` in the same loop?
**A:** Not on an access-order map — `get` structurally modifies it (bumps `modCount`),
so the next iterator step throws `ConcurrentModificationException`, even single-threaded.

**Q:** Is wrapping it in `Collections.synchronizedMap` enough to share it across threads?
**A:** No. Atomicity of single ops isn't the problem; because `get` mutates in
access-order mode, external iteration and the check-then-act of read-plus-reorder still
race. You'd need a proper concurrent design (e.g. striped locks, or Caffeine-style
buffering of access events) or full external synchronization around compound operations.

**Q:** Which operations count as a "use" and which let you peek without promoting?
**A:** `get`, `getOrDefault`, `compute*`, `merge`, and re-`put` of an existing key
promote. `containsKey`, `containsValue`, `forEach`, and `entrySet`/`keySet` iteration
do not. There is no built-in recency-neutral value lookup, which is why a true `peek`
is awkward on `LinkedHashMap`.

**Q:** What breaks if you mutate the map inside `removeEldestEntry`?
**A:** It runs inside `afterNodeInsertion` mid-`put`, before the structure is consistent.
Restructuring there corrupts the map — on Java 21 typically a silently wrong size or a
lost entry rather than a clean exception. Treat it as a read-only predicate over `eldest`.
