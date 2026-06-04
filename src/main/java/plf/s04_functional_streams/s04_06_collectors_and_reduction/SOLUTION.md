# Solution — The reduce that lies when you parallelize

## Ideal solution

```java
public static List<Tx> collectAllFixed(Stream<Tx> txns) {
    return txns.collect(Collectors.toList());
}

public static Map<String, BigDecimal> totalPerMerchant(Stream<Tx> txns) {
    return txns.collect(Collectors.groupingBy(
            Tx::merchant,
            Collectors.reducing(BigDecimal.ZERO, Tx::amount, BigDecimal::add)));
}

public static List<Tx> findByMerchant(Stream<Tx> txns, String merchant) {
    return txns.filter(t -> t.merchant().equals(merchant))
               .collect(Collectors.toList());
}
```

Required imports: `java.util.List`, `java.util.Map`, `java.math.BigDecimal`,
`java.util.stream.Collectors`, `java.util.stream.Stream`.

### Notes per task

- **Task 1** — `collect(toList())` works because `Collectors.toList()` supplies a
  *fresh* `ArrayList` per partition (its `supplier`), accumulates into each
  independently, and merges with a real `combiner` (`addAll`). No shared identity, so
  parallel splitting is safe. `toUnmodifiableList()` or the terminal `.toList()` are
  equally correct.
- **Task 2** — `groupingBy(Tx::merchant, reducing(ZERO, Tx::amount, BigDecimal::add))`
  is the "correct reduction" the broken `reduce` was pretending to be. `BigDecimal::add`
  is associative and side-effect-free; `BigDecimal.ZERO` is a true (immutable) identity.
  Resist `summingDouble(t -> t.amount().doubleValue())` — that reintroduces binary
  floating-point error on money.
- **Task 3 (coda, EJ47)** — return `List<Tx>`, not `Stream<Tx>`. A `Stream` is consumed
  by its first terminal/for-each and then throws `IllegalStateException: stream has
  already been operated upon or closed`. A `Collection` return type lets the caller
  iterate repeatedly, use it in a `for-each`, size it, and re-stream it. EJ47: prefer
  `Collection` to `Stream` as a return type unless you know the caller wants a pipeline.

## Why this works (and the gotcha)

The broken method uses the three-arg
`reduce(identity, accumulator, combiner)`. Its package-doc contract demands:

1. `identity` is an identity for the combiner: `combiner.apply(identity, x) == x`.
2. accumulator and combiner are associative.
3. compatibility:
   `combiner.apply(u, accumulator.apply(identity, t)) == accumulator.apply(u, t)`.

With `identity = new ArrayList<>()` — **one shared mutable instance** — law (3) is
already false. `accumulator.apply(identity, t)` mutates that shared list and returns it;
`combiner` then folds the list into itself. The function is *broken everywhere*. The
reason serial execution returns the right answer is that a sequential stream **never
calls the combiner** and threads everything through the one list in encounter order. One
thread, one list, no merge — accidental correctness. The moment the stream splits, every
leaf is seeded with the *same* list object, leaves race in `add` (corruption /
`ArrayIndexOutOfBoundsException`) and the combiner `addAll`s a list into itself
(duplication / loss). The output is a silently wrong list of the wrong size.

The cure is not "add `.sequential()`" and it is not a cleverer combiner. It is to stop
pretending mutable accumulation is reduction. A `Collector` is the right abstraction
precisely because it has a **`supplier`** that mints a *new* container per partition —
the missing fourth ingredient `reduce`'s shared identity can never provide. That is the
one sentence: **mutable accumulation needs a `Collector`'s per-partition supplier, not
`reduce`'s shared identity.**

The test guards both halves on purpose: it asserts `parallel.equals(expected)` AND
`serial.equals(expected)`, so you can't "pass" by forcing serial execution — that would
leave a method that is still contractually broken, just not currently exercised in
parallel.

## Common mistakes

- **"It works, ship it"** after testing only `.stream()` → the serial run never invokes
  the combiner, so the identity-contract violation is invisible. Parallel drops/dupes.
- **`.sequential()` or deleting `.parallelStream()`** → masks the bug; the method is
  still contractually wrong and the test's serial-correctness check still passes while
  the parallel check is dodged rather than fixed. The grader checks both.
- **`reduce(new ArrayList<>(), (l,t)->{l.add(t);return l;}, (a,b)->a)`** (combiner
  discards `b`) → an even quieter liar: serial correct, parallel silently drops whole
  partitions with no exception at all.
- **`summingDouble`/`summingInt` for money** → reintroduces floating-point rounding on
  currency. Use `BigDecimal` reduction.
- **Returning `Stream<Tx>` from `findByMerchant`** → caller can traverse once, can't
  `for-each` twice, throws `IllegalStateException` on the second terminal op (EJ47).
- **`groupingBy(... , mapping(Tx::amount, toList()))` then summing later** → does the
  reduction in the wrong place and allocates a list per group needlessly.

## Interviewer Q&A

**Q:** The broken `collectAll` returns the correct list with `.stream()`. So is it
correct?
**A:** No. It violates the three-arg `reduce` identity contract regardless of
parallelism: `new ArrayList<>()` is a single shared mutable instance, not a fresh
identity per partition. Serial execution happens to return the right answer because a
sequential stream never invokes the combiner — accidental correctness. It is broken
everywhere; serial just hides it.

**Q:** What exactly is the contract `reduce(identity, accumulator, combiner)` requires?
**A:** The identity must satisfy `combiner.apply(identity, x) == x`; accumulator and
combiner must be associative; and they must be compatible:
`combiner.apply(u, accumulator.apply(identity, t))` equals `accumulator.apply(u, t)`. A
mutating accumulator over a shared container breaks the compatibility and identity laws.

**Q:** Why does `collect(toList())` work where `reduce` did not?
**A:** A `Collector` has a `supplier` that creates a *fresh* result container per
partition, an accumulator that mutates only that local container, a combiner that merges
two containers, and a finisher. The per-partition supplier is the piece `reduce`'s single
shared identity can't provide, so parallel splitting is safe.

**Q:** Could you make the `reduce` version correct without switching to `collect`?
**A:** Only by making it a true immutable fold — e.g. identity `List.of()` and an
accumulator/combiner that build *new* lists each step. That's correct but allocates a new
list per element and is O(n²)-ish; it's exactly why `collect` with a mutable supplier
exists. For mutable accumulation, `collect` is the right tool, not `reduce`.

**Q:** This is EJ46 — what's the rule and why does it bite here?
**A:** Stream behavioral parameters should be side-effect-free. The accumulator lambda
mutates a captured/shared container — a side effect. It's undefined under parallel
execution and, as shown, even the "reduction" framing doesn't make it safe. The safe
mutable accumulation idiom is `collect` with a proper `Collector`.

**Q:** Why `BigDecimal` reduction instead of `summingDouble` for the per-merchant totals?
**A:** Money in `double` accumulates binary floating-point error (e.g. 0.1 + 0.2). A
`BigDecimal` reduction with `BigDecimal.ZERO` identity and `BigDecimal::add` is exact and
associative, so it's also parallel-safe.

**Q:** Why prefer returning `List`/`Collection` over `Stream` from a public method (EJ47)?
**A:** A `Stream` can be traversed only once and can't be used directly in a `for-each`.
A `Collection` lets callers iterate repeatedly, index, size, and re-stream. Return a
`Stream` only when you specifically want to hand the caller a lazy pipeline and they
expect that.

**Q:** When *is* `reduce` the right choice over `collect`?
**A:** When the result is an immutable value combined by an associative, side-effect-free
operator with a genuine identity — sums, maxima, string concatenation via an immutable
accumulator, monoid-style folds. Use `collect` when you're filling a mutable container.

**Q:** The test runs 1000 iterations and checks both serial and parallel equal the
expected. Why both, and why so many runs?
**A:** Parallel corruption from the shared-identity bug is split- and timing-dependent,
so a single run can pass by luck; many runs make the failure reliable. Asserting the
serial result independently prevents "fixing" the test by forcing sequential execution
while leaving the contract violation in place.
