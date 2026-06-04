# Solution — Ghost in the Map

## Ideal solution

The four predictions, corrected to Java 21 (Amazon Corretto) behavior:

```java
static final String  PREDICT_GET_AFTER_MUTATION = null;  // get(key)            -> null
static final boolean PREDICT_CONTAINS_KEY        = false; // containsKey(key)    -> false
static final boolean PREDICT_CONTAINS_VALUE      = true;  // containsValue(...)  -> true
static final int     PREDICT_SIZE_AFTER_REINSERT = 2;     // size() after re-put -> 2
```

With those values the suite is green. The contradiction is the point: at the
**same instant**, for the **same key object**, `get(key)` and `containsKey(key)`
say "not here," while `containsValue("origin-ish")` says "here." Re-inserting the
same object then drives `size()` to **2** — one live entry plus one orphaned
ghost.

Trace of the run:

```
put(key=(1,2), "origin-ish")        size=1   get -> "origin-ish"   (bucket 2)
key.setX(2)   // key is now (2,2)            // bucket NOT recomputed
get(key=(2,2))            -> null            // hashes to bucket 1, empty
containsKey(key=(2,2))    -> false           // same hash path as get
containsValue("origin-ish") -> true          // O(n) equals scan over values
put(key=(2,2), "second")            size=2   // new entry in bucket 1;
                                             // orphan still squats in bucket 2
get(key=(2,2))            -> "second"
```

### Scale up solutions

**1. Pin the bucket math by hand.**
HashMap's index is `spread(h) & (capacity - 1)` where `spread(h) = h ^ (h >>> 16)`.
Default capacity is 16, so the mask is `& 15`.

- `Objects.hash(1, 2) = 994`. For small ints `h >>> 16 == 0`, so the spread is a
  no-op: `spread(994) = 994`. Bucket = `994 & 15 = 2`.
- `Objects.hash(2, 2) = 1025`. `spread(1025) = 1025`. Bucket = `1025 & 15 = 1`.

So mutating `x` from `1` to `2` moves the key from **bucket 2 to bucket 1** — a
genuine move, which is why `get` misses.

Why the blog-favorite `1→17` would **not** work: `Objects.hash(17, 2) = 1490`,
`spread(1490) = 1490`, `1490 & 15 = 2` — the **same** bucket as `(1,2)`. With
`(1,2)→(17,2)` the entry stays in bucket 2, `get` still finds it, and the
exercise would pass on the author's machine and then "fail" (i.e., not
demonstrate the bug) under review. The honest move is to pin an empirically
verified pair; `1→2` is verified, `1→17` is a collision. Note the spread is a
no-op only for small ints — if you "improve" `Point` to a larger hash, hand math
on `& 15` silently breaks, which is exactly why you run it instead of trusting
paper.

**2. Make the bug disappear (the structural fix).**
Do not touch `equals`/`hashCode` logic — make the state they read **immutable**:

```java
public final class Point {
    private final int x;   // final
    private final int y;   // final

    public Point(int x, int y) { this.x = x; this.y = y; }
    public int x() { return x; }
    public int y() { return y; }
    // no setters

    @Override public boolean equals(Object o) {
        return o instanceof Point p && x == p.x && y == p.y;
    }
    @Override public int hashCode() { return Objects.hash(x, y); }
    @Override public String toString() { return "Point(" + x + ", " + y + ")"; }
}
```

Or, in modern Java, a one-liner that is final and immutable by construction:

```java
public record Point(int x, int y) { }
```

Both make the hashCode unchangeable for the life of the object, so the bucket
chosen at `put` stays valid forever. If a caller wants a "moved" point, they
create a new instance and re-key the map deliberately. **Hash keys must be
immutable (or at least never mutated while in the map).**

**3. `instanceof` vs `getClass()`.**
`MutableKeyPoint` is `final`, so `instanceof` is fine: no subclass can exist, so
there is no asymmetry risk and `instanceof` correctly accepts only `MutableKeyPoint`.

The danger arises in a **non-final** class with a subclass. Suppose `Point`
(uses `instanceof`) has subclass `ColoredPoint` that also accepts `Point` in its
`equals`. Then `point.equals(coloredPoint)` can be `true` while
`coloredPoint.equals(point)` is `false` (or the subclass relaxes the check),
breaking **symmetry** and **transitivity** — the second contract trap. `getClass()`
restores symmetry (`a.equals(b)` requires identical runtime classes) but breaks
Liskov substitutability: a `ColoredPoint` is then never `.equals` to a `Point`
with the same coordinates. The standard senior answer: prefer composition over
inheritance for value types, or make the class `final` (as here) so `instanceof`
is unambiguously safe.

## Why this works (and the gotcha)

A `HashMap` computes a key's bucket exactly **once**, when you `put` it:
`index = spread(key.hashCode()) & (capacity - 1)`. That index is captured in the
table; nothing recomputes it when the key object's fields change. So:

- **`get` / `containsKey`** rehash the *current* key state, jump to the new
  bucket, and find an empty chain → `null` / `false`. The entry is not lost; it
  is in the *old* bucket, which these methods no longer visit.
- **`containsValue`** never hashes anything. It is an `O(n)` linear scan that
  calls `equals` on each stored value → it still finds `"origin-ish"`. That is
  why the two answers contradict each other at the same instant: **different code
  paths.**
- **Re-inserting** the mutated object hashes to the new bucket (bucket 1), finds
  no matching key there, and adds a *second* entry. The original entry still sits
  in bucket 2, unreachable by hash lookup but counted by `size()`. The same
  object is now "in" the map twice — a **ghost entry** that proves identity froze
  at insertion.

The gotcha that survives the interview: the bug is **not** a missing or broken
`hashCode`. The hashCode is *too* correct — it faithfully depends on mutable
state, and a key whose hash can change cannot live in a hash table. "Just add
hashCode" is the wrong fix; "freeze the key" is the right one.

## Common mistakes

- **"Add a hashCode and you're fine."** It already has a correct one. The defect
  is mutability, not absence. Adding/rewriting hashCode changes nothing.
- **Assuming `get` and `containsValue` must agree.** They take different paths
  (hash lookup vs linear `equals` scan); a mutated key makes them disagree.
- **Using `1→17` to "prove" the bug.** `(17,2)` collides into the *same* bucket
  as `(1,2)`, so `get` still works — the demo silently fails. Pin a verified pair
  (`1→2` moves bucket 2 → bucket 1).
- **Reasoning with `hashCode() & 15` directly.** HashMap *spreads* first
  (`h ^ (h >>> 16)`). It is a no-op for small ints, so people get lucky — until a
  larger hash, where the hand math breaks. Run it; don't trust the whiteboard.
- **Thinking `size()` can't grow from re-putting the "same" key.** Once the key
  moved buckets, it is no longer the "same" key *to the map's hash lookup*, so
  `put` inserts a new entry instead of replacing.

## Interviewer Q&A

**Q:** `MutableKeyPoint`'s `equals` and `hashCode` are both correct. Why does the
map still lose the key?
**A:** Because a HashMap computes the bucket from `hashCode` only at insertion and
never recomputes it. Mutating a field changes the hashCode, so a later `get`
hashes to a different bucket and misses. The contract is upheld at every instant;
it is *consistency across time* that breaks once the key is mutable.

**Q:** At one instant, `get(key)` returns `null` but `containsValue("origin-ish")`
returns `true` for the entry that key created. How is that not a JVM bug?
**A:** They use different algorithms. `get`/`containsKey` do an `O(1)` hash lookup
keyed on the *current* hashCode; `containsValue` does an `O(n)` linear scan over
values using `equals`, with no hashing. A mutated key derails the hash path but
not the scan, so they legitimately disagree.

**Q:** Walk me through the exact bucket math. Why does mutating `x` from 1 to 2
move the key, but 1 to 17 would not?
**A:** Index = `spread(h) & (cap-1)`, `spread(h) = h ^ (h >>> 16)`, default
`cap=16`. `Objects.hash(1,2)=994 → bucket 2`. `Objects.hash(2,2)=1025 → bucket 1`
(moves). `Objects.hash(17,2)=1490 → 1490 & 15 = 2` — same bucket, so `get` still
finds it. For small ints the `>>> 16` spread is a no-op, so you can do it by hand,
but the only safe way is to run it.

**Q:** After re-inserting the same mutated object, `size()` is 2. Explain.
**A:** The re-`put` hashes to the new bucket (1), finds no equal key there, and
adds a second entry. The original entry still sits in the old bucket (2),
unreachable by hash lookup but counted by `size()`. The same object now backs two
entries — a ghost.

**Q:** What is the correct fix, and why is "just add a stronger hashCode" wrong?
**A:** Make the key immutable: `final` fields with no setters, or a `record`. Then
the hashCode can never change while the key is in the map. A stronger hashCode
still depends on mutable state, so the bug persists — the problem is mutability,
not hash quality.

**Q:** `equals` here uses `instanceof`, and the class is `final`. Is that the
right call? When would `instanceof` be dangerous?
**A:** For a `final` class `instanceof` is correct and even slightly safer than
`getClass()` against null. It becomes dangerous in a non-final class with
subclasses: a subclass that also accepts the superclass can break symmetry and
transitivity. The fixes are `getClass()` (restores symmetry, sacrifices Liskov
substitutability) or, preferably, `final` / composition for value types.

**Q:** The exercise touches the Object contract broadly. What does the
`equals`/`hashCode` contract actually require, in one line each?
**A:** `equals` must be reflexive, symmetric, transitive, consistent, and
`x.equals(null)` is false. `hashCode` must be consistent, and *equal objects must
have equal hash codes* (unequal objects may collide). The asymmetry is the crux:
equal ⇒ same hash is mandatory; same hash ⇒ equal is not. A mutable key violates
the *consistency* clause across time.

**Q:** `clone()` and `finalize()` are also part of the Object contract. What are
the senior takes?
**A:** `Object.clone()` is shallow and only works if the class implements
`Cloneable`; calling it otherwise throws `CloneNotSupportedException`. `Cloneable`
is a broken marker interface (no `clone` method, protected `clone`), so prefer a
copy constructor or static factory. `finalize()` is deprecated since Java 9 and
removed for use; it is unpredictable, can resurrect objects, and hurts GC — use
`try-with-resources`/`AutoCloseable` or `java.lang.ref.Cleaner` for cleanup.

**Q:** `getClass()` and `toString()` — quick contract facts an interviewer expects?
**A:** `getClass()` returns the exact runtime class (a `Class<?>`), unaffected by
the static reference type; for an `int[]` it reports `[I`. The default
`Object.toString()` is `getClass().getName() + "@" + Integer.toHexString(hashCode())`
(e.g. `java.lang.Object@1b6d3586`); always override it for value types so logs and
debuggers are useful.
