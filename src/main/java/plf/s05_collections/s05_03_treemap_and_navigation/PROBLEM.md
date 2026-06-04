# TreeMap and Navigation — "This train" vs "the one before"

**Concept:** inclusive-vs-exclusive boundaries are the whole personality of `TreeMap`
navigation — and `TreeMap` keys live or die by `compareTo`, not `equals`.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
A `TreeMap` is a red-black tree, so keys are kept sorted and every lookup is
O(log n). The interesting part is not `get`/`put` — it's the navigation quartet,
where one sloppy choice gives you the wrong train:

- `ceiling(t)` = least key **≥ t**     ("at or after")
- `floor(t)`   = greatest key **≤ t**   ("at or before")
- `higher(t)`  = least key **> t**      (strictly after)
- `lower(t)`   = greatest key **< t**   (strictly before)

The trap: people conflate `ceiling` with `higher` and `floor` with `lower`. The
difference only bites on an exact hit — `floor(540)` returns `540` if it exists,
`lower(540)` skips it. Same rule, scaled to a range, is `subMap(from, to)`: the
upper bound is **exclusive**, just like `lower` vs `floor`.

Then there's the layer that ends careers: `TreeMap` decides key identity with
`compareTo` (or its `Comparator`), **never** `equals`/`hashCode`. Two keys that
compare to `0` are the *same* key, even if `equals` says otherwise.

## Task
Keys are minute-of-day (e.g. `540` = 09:00). Fill in `TrainSchedule.java`:

1. `nextAtOrAfter(t)` — the next departure at or after `t`.
2. `lastAtOrBefore(t)` — the last departure at or before `t`.
3. `strictlyAfter(t)` — the next departure strictly after `t` (a train exactly at
   `t` does **not** count).
4. `between(from, to)` — the departures in `[from, to)` (upper bound exclusive).

Pick the right navigation method for each; return `null` when there is no such train.

## Part B — predict before you run
Given keys `{540="A", 600="B", 660="C"}`, write your predicted answers, then check
against `SOLUTION.md`:

- `floor(600)` = ?   `lower(600)` = ?
- `ceiling(600)` = ? `higher(600)` = ?
- `between(540, 660)` contains which keys?
- `headMap(600, false)` view: what does `.put(600, "X")` do?

## Part C — the compareTo collapse
This one has a planted red test. `TreeMapIdentityTest` builds a `TreeMap<BigDecimal,String>`,
puts `new BigDecimal("1.0")`, then puts `new BigDecimal("1.00")`. The test currently
asserts `size() == 2`. Run it, watch it fail, and **fix the assertion to the true value**
— then be ready to explain *why* in the interview Q&A.

## Constraints
Java 21, no external libraries. Do not change method signatures in `TrainSchedule.java`.
For Part C, fix only the assertion, not the production behavior.

## How to verify
`mvn -q -Dtest=TrainScheduleTest test` for Parts 1–4.
`mvn -q -Dtest=TreeMapIdentityTest test` for Part C (red until you fix the assertion).

<details><summary>Hint (open only if stuck)</summary>
"At or after" / "at or before" are the *inclusive* methods (`ceiling`/`floor`).
"Strictly" means the *exclusive* method (`higher`/`lower`). For a range, the
two-arg `subMap(from, to)` is already `[from, to)`. For Part C, `new BigDecimal("1.0").compareTo(new BigDecimal("1.00"))` returns `0`.
</details>
