# Ordering — The TreeSet that quietly eats an element

**Concept:** a `TreeSet`/`TreeMap` built with a `Comparator` uses **only** that comparator to decide identity — it never delegates to `Comparable.compareTo`, even when `compareTo` is perfectly correct.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
`Points.Point` is a `record` with a **correct, total** `compareTo` (distance², then `x`, then `y` — no two distinct points ever compare equal). Drop the four points into a no-arg `TreeSet` and you get `size() == 4`. Good.

Now build the SAME four points into `new TreeSet<>(Comparator.comparingInt(Point::distSq))` and `size()` is **3**. One point vanished. Your record's `compareTo` is right — you can test it — so why did supplying a comparator silently lose an element?

The four points: `(1,5)` and `(5,1)` both have distance² = 26. `(3,4)` = 25, `(2,2)` = 8.

## Task
1. Run `Points.main` (or the test). Observe two different sizes — `4` and `3` — for the same four points. Reach for the obvious fix: the `compareTo` must be wrong. It isn't. Convince yourself by reading it.
2. Fix `Points.byDistance()` so the comparator-built `TreeSet` also keeps all four points. Make `byDistance()` a **total** order (no two distinct points compare equal), the same way `compareTo` already is.
3. Do **not** touch `compareTo`, `FOUR`, or the test. The whole point is that editing `compareTo` does nothing here.

Turn `comparatorOrderingMustAlsoKeepAllFour` green.

## Constraints
Java 21, no external libraries. Edit only `byDistance()`. Do not modify the test file or `compareTo`.

## How to verify
`mvn -q -Dtest=PointsTest test`

<details><summary>Hint (open only if stuck)</summary>
A `TreeSet` decides "these two elements are the same" purely by whether its ordering returns `0`. With a no-arg `TreeSet` that ordering is `compareTo`; with `new TreeSet<>(cmp)` it is `cmp` — and `cmp` is `distSq`-only, so the two distance-26 points tie at `0` and one is dropped. Chain `.thenComparingInt(...)` onto `byDistance()` until distinct points never tie.
</details>
