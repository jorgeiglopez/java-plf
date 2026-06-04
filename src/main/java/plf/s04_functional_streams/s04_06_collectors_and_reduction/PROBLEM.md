# Collectors and Reduction — The reduce that lies when you parallelize

**Concept:** mutable accumulation needs a `Collector`'s per-partition supplier, not `reduce`'s shared identity — and serial execution can hide a contract violation that only surfaces in parallel.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Here is a method that "works". You run it, it returns the full list, you ship it:

```java
public static List<Tx> collectAll(Stream<Tx> txns) {
    return txns.reduce(
            new ArrayList<Tx>(),
            (list, t) -> { list.add(t); return list; },
            (a, b) -> { a.addAll(b); return a; });
}
```

Call it with `data.stream()` and you get `[t1, t2, ..., tn]`. Correct. Call it with
`data.parallelStream()` and you get a **shorter or duplicated** list — silently, no
exception, no stack trace. The serial run was a liar: it returned the right answer for
the wrong reason. Why?

The three-argument `reduce(identity, accumulator, combiner)` has a contract written in
the `java.util.stream` package doc. The `identity` must be an identity value, and the
combiner/accumulator must satisfy
`combiner.apply(u, accumulator.apply(identity, t)) == accumulator.apply(u, t)`.
A single shared `new ArrayList<>()` instance, mutated in place, breaks that law — but a
single-threaded stream never invokes the combiner, so it never trips the wire. That is
EJ46: stream functions must be side-effect-free, and "reduction" is not a license to
mutate a shared container.

## Task

Edit `TransactionAggregator.java`.

1. **`collectAllFixed`** — return every transaction so that serial and parallel agree
   (and both equal the input). You may **not** use `reduce`. Use `collect`.
2. **`totalPerMerchant`** — total `amount` per merchant. Sum with `BigDecimal`
   (no `double`, no `summingDouble`). Use `groupingBy` with a downstream reduction.
3. **`findByMerchant`** (coda) — return the matching transactions as a type the caller
   can iterate **more than once**. Think about EJ47: why not return `Stream<Tx>`?

## Constraints

- Java 21, no external libraries. Do **not** modify the test file.
- Do **not** "fix" Task 1 by deleting `.parallelStream()` — the test checks the serial
  result is correct too.
- Do **not** keep `collectAll` (the broken one) — it stays as the cautionary specimen;
  your work goes in the `Fixed`/new methods.
- Task 1 must not use `reduce`. Task 2 must not use `double`.

## How to verify

`mvn -q -Dtest=TransactionAggregatorTest test`

Before you fix anything, do the felt experiment: temporarily point `collectAllFixed` at
the broken `collectAll` body and watch `serialAndParallelAgreeOnFullCollection` fail —
the parallel size won't match. Then fix it for real.

<details><summary>Hint (open only if stuck)</summary>
A `Collector` has a `supplier` that mints a <em>fresh</em> container per partition, an
`accumulator`, and a `combiner` — the four-law contract `reduce` can't give you for
mutable containers. `Collectors.toList()` and
`Collectors.groupingBy(key, Collectors.reducing(zero, mapper, op))` already implement it.
For the coda, a `Stream` is consumed once; a `List` is not.
</details>
