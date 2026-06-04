# Object Method Contracts — The Ledger That Loses Money

**Concept:** `compareTo` must be *consistent with* `equals` (EJ14). Violate it in the direction your reflex creates — make `compareTo` say "equal" where `equals` says "different" — and a sorted collection silently drops real data. No exception, no warning.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You know the textbook bug: `equals` overridden, `hashCode` forgotten, `HashSet`
misbehaves. So you are careful. `Money` here ships a *correct*, mutually
consistent `equals`/`hashCode` pair. The defect lives in one place only:
`compareTo`.

`Money` wraps a `BigDecimal` amount and a currency. Two facts about `BigDecimal`
that bite at senior level:

- `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` → **`false`** (scale is
  part of `BigDecimal` equality).
- `new BigDecimal("2.0").compareTo(new BigDecimal("2.00"))` → **`0`** (compareTo
  ignores scale).

So `Money` follows the smug reflex — "amounts compare numerically" — and lets
`compareTo` delegate to `BigDecimal.compareTo`. Now `equals` says two line items
are *different* while `compareTo` says they are *the same*. Where does that
contradiction surface, and what does it cost you?

## Task

1. Read `Money.java`. Confirm that `equals` and `hashCode` are correct and
   consistent with each other — equal `Money` are equal and share a `hashCode`.
   The only suspect line is the last one, in `compareTo`.
2. Open
   `src/test/java/.../s02_07_object_methods_contracts/LedgerLeakTest.java`.
   It builds a `HashSet<Money>` and a `TreeSet<Money>`, each fed the *same* two
   amounts: `USD 2.00` and `USD 2.0`.
3. **Commit first.** On paper, write your prediction for all four `PREDICT_*`
   constants, from nothing — they are not multiple choice:
   - `equals(USD 2.00, USD 2.0)`
   - `treeSet.size()` after adding both
   - `treeSet.contains(USD 2.000)`
   - `hashSet.contains(USD 2.0)`
   Note the inputs differ on purpose: `2.000` probes the `TreeSet`, `2.0` probes
   the `HashSet`. A senior predicts both lookups `true` for *different* reasons.
   Do not run yet.
4. Run the test. It starts red. Each failure message reveals the real Java 21
   value. Edit **only** the four `PREDICT_*` constants until the suite is green.
   Do not touch `Money`, the set construction, or the assertions.
5. Diagnose in one sentence each: (a) why `treeSet.size()` is what it is even
   though `equals` returned `false`, and (b) which of the two amounts survived in
   the `TreeSet`, and where the other one went.

## The bait (read before you "fix" anything)

The obvious repair is to make `equals` agree with `compareTo` — delegate `equals`
to `BigDecimal.compareTo` so `2.0` and `2.00` are finally "equal everywhere."
**Do not.** `SOLUTION.md` shows that this "fix" makes a ledger silently net two
real line items — `USD 2.00` and `USD 2.0`, posted by different transactions —
into one. The money does not reconcile; it vanishes. The defect is in
`compareTo`, and that is the only place you are allowed to touch when you repair
it (Scale-up 1).

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Edit only the four `PREDICT_*` constants to get the test green. Everything else
  is fixed.
- When you *repair* the contract (Scale-up 1), the fix goes in `compareTo` only.
  `equals`/`hashCode` are already correct and must not change.

## Scale up (Optional)

1. **Repair `compareTo` correctly.** Rewrite it so it is consistent with
   `equals`: two `Money` compare equal (`0`) **iff** `equals` returns `true`.
   That means scale matters and currency matters. Keep the natural ordering
   sane (USD 2.0 still sorts before USD 3.0). Show that the `TreeSet` now holds
   both line items.
2. **Cross-currency horror.** With the *original* buggy `compareTo`, add
   `USD 2.00` and `EUR 2.0` to a `TreeSet`. Predict the size, then explain why
   the buggy comparator merges two *different currencies* — and why no
   `ClassCastException` or exception ever fires to warn you.
3. **Make it a `record`.** Convert `Money` to
   `record Money(BigDecimal amount, String currency) implements Comparable<Money>`.
   Which of `equals`/`hashCode`/`toString` can you now delete? Does turning it
   into a record fix the `compareTo` bug? Explain why the record's free
   `toString` (EJ12) is strictly better than the hand-written one here.
4. **`Comparator` escape hatch.** Without touching `Money`, build a `TreeSet`
   that keeps both line items by passing a `Comparator` to the constructor.
   What does that prove about `compareTo` being a class's *natural* order versus
   a collection's chosen order?

## How to verify

```
mvn -q -Dtest=LedgerLeakTest test
```

The test starts **red**. It is green only once all four `PREDICT_*` constants
match Java's real behavior. Then re-read your paper predictions and count how
many you got wrong — especially whether you guessed `hashSet.contains(USD 2.0)`
and `treeSet.contains(USD 2.000)` for the *same* reason (they are not).

<details><summary>Hint (open only if stuck)</summary>
A `TreeSet` never calls `equals`. It decides membership purely by `compareTo`: a
new element is a duplicate iff some existing element compares `0` to it. So the
second `add` of an "equal-by-compareTo" amount is dropped on the floor, and a
later `contains` returns `true` for any amount that compares `0` — including a
scale you never inserted. The `HashSet`, meanwhile, uses `equals`/`hashCode` and
never calls `compareTo`, so it keeps both `2.00` and `2.0` as distinct — but
`contains(USD 2.0)` is `true` there because you really did insert a `2.0`.
</details>
