# Solution — The Key That Got Lost

## Ideal solution

Actual output of `StrandedKey.main`:

```
get(a)        = null
containsKey(a)= false
values has    = true
size (before) = 1
size (after)  = 2
get(a)        = alpha2
keys          = [Key(999), Key(999)]
```

`predictions.txt` (the committed answers):

```
get(a)         = null
containsKey(a) = false
values.contains("alpha") = true
size (before second put) = 1
size (after second put)  = 2
get(a) (after)           = alpha2
keySet() prints          = [Key(999), Key(999)]   // same object, twice
```

The explanations:

- **(a)** `get(a)` recomputes the bucket from `a`'s *current* `hashCode()` (999 → bucket 7), but the node lives in the bucket it was inserted into (1 → bucket 1). Lookup walks bucket 7, finds nothing, returns `null`. `values()` does a linear scan over every node in every bucket, so it still sees `"alpha"` sitting in bucket 1.
- **(b)** The second `put` also hashes to bucket 7, finds no existing node there whose key `equals` `a`, so it has nothing to overwrite and *appends a new node* — `size` goes 1 → 2. Both nodes hold a reference to the *same* `Key` object `a` (now `id=999`), which is why `keySet()` prints `Key(999)` twice.

### The fix
Never let a key's hash-relevant state change while it's in a hash structure. Use **immutable keys**: make the field `final`, set it once in the constructor, expose no mutator. Records are ideal here — `record Key(int id) {}` gives you a final component, value-based `equals`/`hashCode`, and no setter.

```java
record Key(int id) {}   // immutable; hashCode/equals derived from id, frozen at construction
```

### Scale up solution (treeification coda)
`binBecomesRedBlackTree = false`. With default capacity 16, eight colliding keys do **not** treeify: `treeifyBin` checks `MIN_TREEIFY_CAPACITY = 64` first, and since `table.length (16) < 64` it calls `resize()` instead of building a tree. The map keeps doubling (16 → 32 → 64) on the way; only once the table is ≥ 64 *and* a bin still has ≥ 8 nodes does that bin convert to a red-black tree. So `predictTreeification` must set the prediction to `false`.

## Why this works (and the gotcha)

A `HashMap` is an array of buckets. Placement and lookup both compute:

```
spread = h ^ (h >>> 16)          // HashMap.hash — mixes high bits into low bits
bucket = (table.length - 1) & spread
```

The `& (length-1)` only looks at the low bits of the hash, so without the spread, keys whose hashes differ only in high bits would all collide. The spread is why `hashCode()` quality across the *whole* int matters.

The trap is that **the bucket is a function of the key's hash at the moment of each operation**, not a property stored with the entry. Insertion freezes nothing about location — it just drops the node wherever the hash points *then*. Mutating the key moves the "address" the map will look up next time, while the node stays put. The entry is orphaned: reachable by a full scan, unreachable by key, and immortal until a resize re-buckets everything.

The `size == 2` twist is the part that breaks people: a `Set` (`keySet`) is supposed to forbid duplicate keys, yet here it contains the same object twice. There's no contradiction once you internalize that `HashMap` never re-checks old entries — equality is only consulted *within the bucket the current hash points to*. The original node is in a bucket nobody will ever hash to again with this key.

## Common mistakes
- **"`get` returns null so the entry is gone."** → It isn't; `values()` and `size()` prove it's still there. Null means "not found *at the bucket I looked in*", not "absent".
- **"`put` of an existing key always overwrites."** → Only if the existing node is in the bucket the *current* hash selects and `equals` matches. Move the key's hash and you get a second entry.
- **"8 entries in a bucket = a red-black tree."** → Only if `table.length >= 64`. Below that, the map resizes instead. Treeification needs *both* thresholds.
- **"Worst case is O(log n) always."** → Only if the colliding keys are `Comparable`. If they aren't, the tree falls back to comparing `System.identityHashCode` (via `tieBreakOrder`) to keep the tree balanced; it's still O(log n) for traversal but ordering is arbitrary.
- **Using a mutable object (or a collection) as a key.** → Same failure mode: any change to a hash-relevant field strands the entry.

## Interviewer Q&A

**Q:** Walk me through what `HashMap.get(key)` does internally.
**A:** Compute `h = key.hashCode()`, spread it with `h ^ (h >>> 16)`, index the bucket with `(table.length - 1) & spread`, then walk that bucket — a linked list (or red-black tree) — comparing each node's key by `==` then `equals` until a match or the end.

**Q:** Why the `h ^ (h >>> 16)` spread instead of using `hashCode()` directly?
**A:** The bucket index is `& (length - 1)`, which only keeps the low bits. XOR-ing in the high 16 bits ensures high-bit differences still affect bucket selection, reducing collisions for `hashCode`s that vary mainly in their high bits.

**Q:** A key is in the map; you mutate a field its `hashCode` depends on; `get` now returns null but `size()` is unchanged. Explain.
**A:** The node is still physically in its original bucket, so `size()` (a counter) is unchanged and a scan via `values()` finds it. But `get` re-derives the bucket from the *new* hash and searches a different bucket, finding nothing. The entry is stranded.

**Q:** After that, you `put` the same object again. Why does `size` become 2?
**A:** The second `put` hashes to the new bucket, finds no node there matching by `equals`, so it inserts a new node instead of overwriting. The map now has two nodes referencing the same key object — `keySet()` will even iterate it twice.

**Q:** How do you prevent this class of bug?
**A:** Use immutable keys: final hash-relevant fields, no mutators, ideally a `record` or other value type. Don't use mutable objects or collections as keys.

**Q:** When does a bucket treeify, exactly?
**A:** When a single bin reaches `TREEIFY_THRESHOLD = 8` nodes **and** `table.length >= MIN_TREEIFY_CAPACITY = 64`. If the bin hits 8 but the table is smaller than 64, the map resizes instead. Trees shrink back to lists below `UNTREEIFY_THRESHOLD = 6` on resize.

**Q:** So what's the real worst-case complexity of `get`, and what does it depend on?
**A:** O(n) if everything degrades to one long list (e.g. all hashes equal and keys aren't comparable in a way the tree can use). With treeification on Java 8+ it's O(log n) per bin — but only because the bin is a red-black tree; if keys aren't `Comparable`, the tree tie-breaks on `System.identityHashCode` via `tieBreakOrder` to stay balanced.

**Q:** Does the load factor cause treeification?
**A:** No — those are independent. Load factor (default 0.75) triggers a *resize* (table doubling) when `size > capacity * loadFactor`. Treeification is a per-bucket reaction to too many collisions in one bin, gated separately by the 8-and-64 thresholds.

**Q:** Why does `keySet()` return a `Set` that here contains "duplicates"? Isn't that a contract violation?
**A:** Not by `HashMap`'s rules. The duplicate only exists because the user violated the key's invariant by mutating it after insertion. `HashMap` never re-validates old entries; uniqueness is only enforced within the bucket the current hash selects. Garbage in (a mutable key), garbage out.
