# Solution — "This train" vs "the one before"

## Ideal solution

```java
public String nextAtOrAfter(int t) {            // at or after  -> inclusive
    Map.Entry<Integer, String> e = departures.ceilingEntry(t);
    return e == null ? null : e.getValue();
}

public String lastAtOrBefore(int t) {           // at or before -> inclusive
    Map.Entry<Integer, String> e = departures.floorEntry(t);
    return e == null ? null : e.getValue();
}

public String strictlyAfter(int t) {            // strictly after -> exclusive
    Map.Entry<Integer, String> e = departures.higherEntry(t);
    return e == null ? null : e.getValue();
}

public NavigableMap<Integer, String> between(int from, int to) {
    return departures.subMap(from, to);         // two-arg = [from, to)
}
```

The `*Entry` variants return the whole entry (and `null` for "no such key"),
which is cleaner than `ceilingKey` + a second `get`. The bare `ceiling`/`floor`/
`higher`/`lower` return the **key**; the `*Entry` forms return the **entry**.

### Part B — predicted values

Keys `{540, 600, 660}`:

| call | result | why |
|------|--------|-----|
| `floor(600)`   | `600` | greatest key ≤ 600 — the exact hit counts |
| `lower(600)`   | `540` | greatest key **strictly** < 600 — skips 600 |
| `ceiling(600)` | `600` | least key ≥ 600 — the exact hit counts |
| `higher(600)`  | `660` | least key **strictly** > 600 — skips 600 |
| `between(540, 660)` | `{540, 600}` | `[540, 660)` — 660 excluded |
| `headMap(600,false).put(600,"X")` | throws `IllegalArgumentException: key out of range` | the view's boundary is sticky; 600 is outside `[..,600)` |

The pair `floor`/`lower` (and `ceiling`/`higher`) differ only on the exact hit.
That single inclusive-vs-exclusive bit is the whole game, and `subMap`'s exclusive
upper bound is the same rule scaled to a range.

### Part C — the compareTo collapse

The true value is **`size() == 1`**. Fix:

```java
assertEquals(1, m.size());
```

`new BigDecimal("1.0").compareTo(new BigDecimal("1.00"))` returns `0` — `compareTo`
ignores scale, comparing numeric value only. `TreeMap` treats two keys as identical
when `compareTo` returns `0`, so the second `put` **overwrites** the first; the map
holds one key mapped to `"second"`. `containsKey(new BigDecimal("1.000"))` is also
`true` for the same reason. (`BigDecimal.equals`, by contrast, *does* compare scale,
so a `HashMap` would have held two keys — this is exactly the divergence that fools
people.)

## Why this works (and the gotcha)

`TreeMap` is a red-black tree: a self-balancing BST whose black-height invariant
bounds height at `≤ 2·log₂(n+1)`, so navigation, `get`, `put` are all O(log n).
Ordering is determined by the key's natural ordering (`Comparable.compareTo`) or a
supplied `Comparator` — **`hashCode` and `equals` are never consulted.** This has
two consequences students miss:

1. **Navigation inclusivity is a contract, not a guess.** `ceiling`/`floor` include
   an exact match; `higher`/`lower` exclude it. Picking the wrong one is an
   off-by-one that compiles, runs, and silently returns the wrong neighbor.
2. **Key identity = "`compareTo` returns 0".** Any `Comparator` or `compareTo`
   inconsistent with `equals` makes `TreeMap` disagree with `HashMap` about how many
   keys exist. `BigDecimal` scale is the classic landmine; a `Comparator` on
   `Math.abs(k)` (so `-5` and `5` collide) is another.

## Common mistakes

- Using `higher`/`lower` for "at or after"/"at or before" → off-by-one on the exact hit.
- Assuming `subMap(from, to)` includes `to` → it's `[from, to)`; use the six-arg
  `subMap(from, true, to, true)` for a closed range.
- `put`-ing a key outside a `subMap`/`headMap`/`tailMap` view's range → not a no-op,
  it throws `IllegalArgumentException: key out of range`.
- Expecting `TreeMap` to count `1.0` and `1.00` as two keys → `compareTo` says
  they're one. Trusting `equals`/`hashCode` to define `TreeMap` membership.
- Returning the key instead of the entry value, or NPE-ing on the `null` "no such
  key" sentinel.

## Interviewer Q&A

**Q:** What's the difference between `ceiling(k)` and `higher(k)`?
**A:** `ceiling` returns the least key ≥ k (inclusive of an exact match); `higher`
returns the least key strictly > k. They differ only when `k` is itself present.

**Q:** Is `subMap(from, to)` inclusive on both ends?
**A:** No — it's `[from, to)`: `from` inclusive, `to` exclusive. The six-arg
`subMap(from, fromInclusive, to, toInclusive)` lets you choose each end.

**Q:** What happens if you `put` a key outside a `subMap` view's range?
**A:** It throws `IllegalArgumentException: key out of range`. The view enforces its
bounds on writes, not just reads.

**Q:** Why does a `TreeMap<BigDecimal,String>` collapse `1.0` and `1.00` into one key
when a `HashMap` keeps them separate?
**A:** `TreeMap` decides identity by `compareTo`, and `BigDecimal.compareTo` ignores
scale (returns 0 for equal numeric values). `HashMap` uses `equals`/`hashCode`, and
`BigDecimal.equals` *does* compare scale, so it sees two distinct keys.

**Q:** Why is `TreeMap` O(log n) and what guarantees it?
**A:** It's a red-black tree. The red-black invariants keep it balanced — the
black-height rule bounds height at ≤ 2·log₂(n+1) — so every search/insert/delete
touches at most O(log n) nodes. (HashMap is O(1) average but degrades to O(log n)
in a treeified bucket; TreeMap is O(log n) always, with sorted iteration as a bonus.)

**Q:** Your `Comparator` is "inconsistent with equals." What breaks?
**A:** `TreeMap`/`TreeSet` will treat keys the comparator ranks as equal (compareTo
== 0) as the same key, even if `equals` disagrees — so the map can "lose" entries
relative to a `HashMap`, and its behavior violates the general `Map` contract (which
is defined in terms of `equals`). It's legal but you must document it.

**Q:** How do you get a descending view, and is it a copy?
**A:** `descendingMap()` (or `descendingKeySet()`) returns a *view* backed by the
same tree — no copy. `firstKey()` on it is the original's largest key. Mutations
write through, subject to the same range rules.

**Q:** `floorEntry` vs `floorKey` — when do you reach for each?
**A:** `floorKey` gives just the key; `floorEntry` gives key+value in one O(log n)
walk. If you need the value, prefer `floorEntry` to avoid a second `get`. Both return
`null` to signal "no such key," so guard for it.
