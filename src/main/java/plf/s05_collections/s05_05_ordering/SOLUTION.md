# Solution — The TreeSet that quietly eats an element

## Ideal solution
Make `byDistance()` a total order by chaining tie-breakers, exactly mirroring `compareTo`:

```java
public static Comparator<Point> byDistance() {
    return Comparator.comparingInt(Point::distSq)
            .thenComparingInt(Point::x)
            .thenComparingInt(Point::y);
}
```

Now `(1,5)` and `(5,1)` tie on `distSq` (26) but break on `x` (1 vs 5), so they no longer compare equal. `comparatorOrdered().size()` becomes `4` and the test goes green. `compareTo` was never edited — and was never the problem.

## Why this works (and the gotcha)
A `TreeSet` (and `TreeMap`) has no notion of `equals` for membership. It decides whether two elements are "the same" **solely by whether its ordering returns `0`**. Which ordering?

- `new TreeSet<>()` — no comparator supplied — uses the elements' **natural ordering**, i.e. `Comparable.compareTo`. Our `compareTo` is total, so all four points are distinct → `size() == 4`.
- `new TreeSet<>(comparator)` — stores that `comparator` in a field and routes **every** `add`/`contains` through `comparator.compare(...)`. It **never calls `compareTo`**. The supplied comparator is the *only* ordering source.

So when `byDistance()` was `comparingInt(Point::distSq)` alone, `(1,5)` and `(5,1)` both produced `compare == 0`. The tree concluded they were the same key and silently kept one — `size() == 3`. Editing `compareTo` to be "more correct" changes nothing, because the comparator-built tree never looks at it.

The lesson in one sentence: **natural order and a supplied `Comparator` are two different ordering sources; a tree-backed collection uses exactly one of them, and a constructor comparator never delegates to `compareTo`.** The cure is to make the *comparator you actually passed* a total order, via `thenComparing` chaining — comparator totality, not `equals`, is what prevents silent loss in a `TreeSet`.

(Note: `HashSet` keeps all four regardless, because the record's generated `equals`/`hashCode` compare all components. That green `HashSet`/red `TreeSet` contrast is the tell that ordering and equality are different contracts — Effective Java Item 14: keep `compareTo` consistent with `equals`, and here also keep your *comparator* consistent with `equals`.)

## Common mistakes
- **"`compareTo` must be broken" — editing it.** It's correct and total; the comparator-built tree never calls it. Zero effect on the failing test.
- **Adding only `.thenComparingInt(Point::x)`** — for *these* four points it happens to be enough (the colliding pair differs in `x`), but it is not a total order in general (e.g. `(0,5)` vs `(5,0)` would still tie at distSq and x... no — they differ in x; but `(3,4)` vs `(4,3)` differ in x too). The robust habit is to chain down to a component set that uniquely identifies the element — `x` then `y` — so distinct points can never tie.
- **`(a, b) -> a.distSq() - b.distSq()`** — subtraction comparators overflow for large/extreme `int` values (JLS 15.18.2 wraparound). Always use `Integer.compare` / `Comparator.comparingInt`, which is overflow-safe.
- **`reversed()` placement.** `comparingInt(Point::distSq).reversed().thenComparingInt(Point::x)` reverses **only** the distSq key, not the whole chain. `reversed()` binds to the comparator built so far, not to the keys added after it.

## Interviewer Q&A

**Q:** A `TreeSet` of four distinct objects reports `size() == 3`. What's the first thing you suspect?
**A:** The ordering used by the tree returns `0` for two of them. The tree uses its comparator (or natural ordering) — not `equals` — to decide membership, so two elements that "compare equal" collapse into one and the second `add` is dropped.

**Q:** I pass a `Comparator` to the `TreeSet` constructor but the element type also implements `Comparable`. Which is used?
**A:** Only the supplied comparator. The tree stores it in a field and routes every comparison through it; `Comparable.compareTo` is never called. Natural ordering is used *only* when no comparator is supplied.

**Q:** Why does `HashSet` keep all four but `TreeSet` (with the distSq comparator) doesn't?
**A:** `HashSet` uses `hashCode`/`equals`; the record compares all components, so all four are distinct. `TreeSet` uses the comparator's `compare`; with distSq-only, the two distance-26 points tie at `0` and one is lost. Different identity contracts.

**Q:** What does it mean for a comparator to be "consistent with equals," and why care?
**A:** `cmp.compare(a, b) == 0` iff `a.equals(b)`. Sorted sets/maps define membership by `compare`, so an inconsistent comparator makes them disagree with `equals`: elements silently vanish or `get`/`contains` miss keys. Effective Java Item 14 makes this explicit for `compareTo`; the same applies to any comparator you hand a `TreeSet`/`TreeMap`.

**Q:** Why not just write `(a, b) -> a.distSq() - b.distSq()`?
**A:** Integer subtraction can overflow (e.g. a large positive minus a large negative wraps to the wrong sign), giving a comparator that violates transitivity and antisymmetry. Use `Integer.compare` / `Comparator.comparingInt`, which compares without arithmetic.

**Q:** In `comparingInt(Point::distSq).reversed().thenComparingInt(Point::x)`, what is reversed?
**A:** Only the distSq key. `reversed()` flips the comparator built *up to that point*; keys appended afterward via `thenComparing` are in their natural direction. To reverse a later key, reverse that specific sub-comparator: `.thenComparing(Comparator.comparingInt(Point::x).reversed())`.

**Q:** Does `thenComparing` always evaluate every key?
**A:** No — it short-circuits. The next comparator is consulted only when the previous one returns `0`. So tie-breakers cost nothing on the common (non-tied) path.

**Q:** How would you give a `TreeMap` of these points a stable total ordering without implementing `Comparable`?
**A:** Pass a total comparator to the constructor: `new TreeMap<>(Comparator.comparingInt(Point::distSq).thenComparingInt(Point::x).thenComparingInt(Point::y))`. The constructor comparator fully defines key identity and ordering; no `Comparable` needed.
