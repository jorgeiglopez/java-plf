# Solution — The Ledger That Loses Money

## Ideal solution

The four predictions, verified on Java 21 (Amazon Corretto 21.0.11):

| Constant                          | Naive guess | Real value | Why |
|-----------------------------------|-------------|------------|-----|
| `PREDICT_EQUALS_200_VS_20`        | `false`     | **`false`** | `BigDecimal.equals` includes scale: `2.00 != 2.0`. |
| `PREDICT_TREESET_SIZE`            | `0`/`2`     | **`1`**     | `TreeSet` uses `compareTo`; `2.0` compares `0` to `2.00`, so the 2nd `add` is dropped. |
| `PREDICT_TREESET_CONTAINS_2000`   | `false`     | **`true`**  | `contains` also uses `compareTo`; `2.000` compares `0` to the stored `2.00`. |
| `PREDICT_HASHSET_CONTAINS_20`     | `false`     | **`true`**  | `HashSet` uses `equals`/`hashCode`; you really did insert a `2.0`, so it is found. |

So the green test is:

```java
static final boolean PREDICT_EQUALS_200_VS_20 = false;
static final int PREDICT_TREESET_SIZE = 1;
static final boolean PREDICT_TREESET_CONTAINS_2000 = true;
static final boolean PREDICT_HASHSET_CONTAINS_20 = true;
```

The contradiction the exercise traps: `equals(USD 2.00, USD 2.0)` is `false`
(line 1), yet three lines later the `TreeSet` reports `size() == 1` and
`contains(USD 2.000) == true`. Same two objects, two truths — because the two
collections ask two different questions.

The buggy line in `Money.compareTo`:

```java
@Override
public int compareTo(Money o) {
    return amount.compareTo(o.amount); // ignores scale AND ignores currency
}
```

### Scale up solutions

**1. Repair `compareTo` (this is the only line you change).** Make
`compareTo == 0` hold *iff* `equals` is `true`. `BigDecimal.equals` means "same
unscaled value **and** same scale," and `Money.equals` also requires same
currency. So order by currency, then numeric value, then break the value-tie by
scale:

```java
@Override
public int compareTo(Money o) {
    int byCurrency = currency.compareTo(o.currency);
    if (byCurrency != 0) return byCurrency;
    int byValue = amount.compareTo(o.amount);
    if (byValue != 0) return byValue;
    return Integer.compare(amount.scale(), o.amount.scale());
}
```

Now `USD 2.00` and `USD 2.0` compare non-zero, so a `TreeSet` keeps **both**
(`size() == 2`), and ordering stays sane (`USD 2.0` still sorts before
`USD 3.0`). `compareTo(x, y) == 0` is now exactly `x.equals(y)`. Verified:
`treeSet.size() == 2`, contents `[USD 2.0, USD 2.00]`.

> Design note: tie-breaking by scale satisfies the *letter* of EJ14, but the
> deeper fix in real code is to **normalize on construction** (e.g.
> `amount.stripTrailingZeros()` or a fixed scale per currency) so `2.0` and
> `2.00` are the same object and the question never arises. State the trade-off
> in interview: consistency-by-tie-break vs consistency-by-canonical-form.

**2. Cross-currency horror (original buggy `compareTo`).** Adding `USD 2.00`
and `EUR 2.0` to a `TreeSet` yields `size() == 1`: the comparator looks only at
`amount`, both are numerically `2`, so the second `add` is dropped — **two
different currencies merged into one entry**. No `ClassCastException`, no
warning: both are `Money`, both are `Comparable`, the comparator returns `0`
legally. That is the indefensible bug — a USD posting and a EUR posting net to a
single line. The fix in (1), which compares currency first, makes them distinct.

**3. Make it a `record`.**

```java
public record Money(BigDecimal amount, String currency) implements Comparable<Money> {
    @Override
    public int compareTo(Money o) {
        int byCurrency = currency.compareTo(o.currency);
        if (byCurrency != 0) return byCurrency;
        int byValue = amount.compareTo(o.amount);
        if (byValue != 0) return byValue;
        return Integer.compare(amount.scale(), o.amount.scale());
    }
}
```

You can delete `equals`, `hashCode`, and `toString` — the record generates all
three from its components, and the generated `equals`/`hashCode` use
`BigDecimal.equals` (scale-sensitive), identical in behavior to the hand-written
pair. You **cannot** delete `compareTo`: records do *not* implement
`Comparable` for you. So **the record does not fix the bug** — if you write
`compareTo` wrong, a record is just as corrupt. The free `toString` (EJ12) is
strictly better than the hand-rolled `currency + " " + amount`: it never drifts
out of sync when you add a component, and it shows component names, which is
exactly the kind of self-describing output EJ12 wants.

**4. `Comparator` escape hatch.** Without touching `Money`:

```java
Set<Money> set = new TreeSet<>(
    Comparator.comparing(Money::currency)
              .thenComparing(Money::amount)
              .thenComparing(m -> m.amount().scale()));
set.add(new Money("2.00", "USD"));
set.add(new Money("2.0", "USD"));   // set.size() == 2
```

Passing a `Comparator` overrides the class's natural order for *this collection
only*. It proves natural order (`compareTo`) is a property of the *type*, while
a `Comparator` is a property of the *use site*. When you cannot change the type,
supply the order at the collection — but a broken natural `compareTo` still
lurks for every caller who uses the default.

## Why this works (and the gotcha)

`TreeSet`/`TreeMap` are *sorted* structures backed by a red-black tree. They
decide equality of elements **solely** by the comparator (or `compareTo` when
none is given) — `equals` is never consulted. The `SortedSet` contract is
explicit: a sorted set "is consistent with equals if and only if its natural
ordering is consistent with equals," and "behaves strangely" otherwise. "Behaves
strangely" here means **silent data loss**: any element that compares `0` to one
already present is treated as a duplicate and discarded by `add`, and any query
that compares `0` to a stored element is reported `present` by `contains`.

The trap is the *direction* of the inconsistency. The famous bug is
`compareTo` stricter than `equals`. This one is the reflexive opposite: the smug
move — "amounts compare numerically, just use `BigDecimal.compareTo`" — makes
`compareTo` *coarser* than `equals`. `equals` says `USD 2.00 != USD 2.0`;
`compareTo` says they are the same. The `HashSet` (which only ever calls
`equals`/`hashCode`) keeps both; the `TreeSet` (which only ever calls
`compareTo`) keeps one. Same data, two sizes, no exception. In a ledger, the
dropped `2.0` is a real transaction that has now disappeared from your sorted
book of records.

And the "obvious fix" of pushing `compareTo` *into* `equals` is worse, not
better: it makes the whole type agree that `2.00 == 2.0`, so your `HashSet`,
your dedup logic, and your reconciliation all start merging line items you were
required to keep distinct. The contract you must honor is EJ14's: keep
`equals` as-is and make `compareTo` agree with *it* — never the reverse.

## Common mistakes

- **"`TreeSet` calls `equals` like every other Set."** It does not. It uses
  `compareTo`/`Comparator` exclusively. That single fact explains the whole
  exercise.
- **"Delegating `equals` to `compareTo` fixes the inconsistency."** It removes
  the inconsistency by making the type *lose information* (`2.00` and `2.0`
  become equal everywhere) — the ledger silently nets distinct postings.
- **"A `record` would have prevented this."** Records auto-generate
  `equals`/`hashCode`/`toString` but **not** `compareTo`; the bug survives the
  conversion untouched.
- **"No exception means no bug."** Comparator-vs-equals inconsistency is never
  reported. The collection just quietly holds the wrong number of elements.
- **"Comparing only the amount is fine; who mixes currencies?"** The buggy
  comparator merges `USD 2.00` with `EUR 2.0` into one entry — a correctness
  bug even within a single book if more than one currency is present.
- **Reflexive defensive copying of `BigDecimal`** when implementing `clone`/
  copy logic. `BigDecimal` is immutable; copying it is cargo cult (see Q&A).

## Interviewer Q&A

**Q:** State the `Comparable`/`compareTo` consistency-with-`equals` contract in
one sentence.
**A:** It is *strongly recommended* that `x.compareTo(y) == 0` be true exactly
when `x.equals(y)` is true; it is not enforced by the language, but sorted
collections (`TreeSet`, `TreeMap`) rely on it, so violating it corrupts them.

**Q:** Exactly which method does `TreeSet.add`/`contains` use to decide
membership — `equals` or `compareTo`?
**A:** `compareTo` (or the supplied `Comparator`) only. `TreeSet` never calls
`equals`. An element is a duplicate iff some stored element compares `0` to it.

**Q:** In this exercise `equals` returns `false` for `2.00` vs `2.0`, but
`treeSet.size()` is `1`. Reconcile those.
**A:** The two collections ask different questions. `HashSet` asks `equals`/
`hashCode` and keeps both. `TreeSet` asks `compareTo`, which delegates to
`BigDecimal.compareTo` (scale-insensitive) and returns `0`, so the second `add`
is dropped. No contradiction in the code — only in the buggy contract.

**Q:** Why is `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` false while
`compareTo` returns 0?
**A:** `BigDecimal` equality is defined as same *unscaled value and same scale*,
so `2.0` (scale 1) and `2.00` (scale 2) are unequal; `compareTo` compares only
numeric value, so they tie. This asymmetry is the engine of the whole bug.

**Q:** Your colleague "fixes" it by making `equals` call `compareTo`. What
breaks?
**A:** The type now treats `2.00` and `2.0` as equal everywhere, including
`HashSet` and any dedup/reconciliation. Two distinct ledger postings collapse
into one and money silently disappears. EJ14 says fix `compareTo` to agree with
`equals`, never the other way.

**Q:** Write a `compareTo` that is consistent with this `equals`.
**A:** Order by currency, then by numeric amount, then break value-ties by
`BigDecimal.scale()`, so `compareTo == 0` holds iff currency, value, and scale
all match — exactly `equals`. (Better still: normalize the scale at
construction so the tie never exists.)

**Q:** Does converting `Money` to a `record` fix or hide the bug?
**A:** Neither. Records generate `equals`/`hashCode`/`toString` from components
but do **not** generate `compareTo`. You still implement `Comparable` by hand,
so a wrong `compareTo` is just as broken in a record.

**Q:** EJ12 says always override `toString`. The record gives it for free — why
prefer the generated one here?
**A:** The generated `toString` lists every component by name
(`Money[amount=2.00, currency=USD]`) and stays in sync automatically when
components change, whereas the hand-written `currency + " " + amount` silently
omits any field you forget to add. Free, complete, drift-proof.

**Q:** Suppose `Money` cached `BigDecimal.ONE` and you must implement `clone`
(EJ13). Do you deep-copy that `BigDecimal` in `clone`?
**A:** No. `BigDecimal` is immutable, so a defensive copy buys nothing and just
adds garbage — reflexive defensive copying is cargo cult. Defensive copies in
`clone`/constructors are for *mutable* component types (e.g. `Date`, arrays,
`List`). For an immutable value class like this, the right answer is usually to
not support `clone` at all and rely on immutability (EJ13: override `clone`
judiciously, or not at all).

**Q:** When *is* it legitimate to have `compareTo` inconsistent with `equals`,
and how do you signal it?
**A:** `BigDecimal` itself is the canonical example — natural ordering ignores
scale, equality does not. It is allowed, but you must *document it loudly*
("natural ordering is inconsistent with equals") and never use such a type as a
`TreeSet`/`TreeMap` element without a deliberately chosen `Comparator`, because
the sorted collection will otherwise lose data.
