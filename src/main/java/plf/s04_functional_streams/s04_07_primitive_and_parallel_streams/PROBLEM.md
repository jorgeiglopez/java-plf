# Primitive & Parallel Streams — Two cliffs, and you'll rank them wrong

**Concept:** `.parallel()` is not free. When it fails to help, the cause is your
*source's splittability* and your *accumulator's boxing* — and one of those costs
is far bigger than folklore says.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You sum `0 + 1 + ... + (N-1)` four different ways. All four return the same
`long`. Only the plumbing differs: boxed vs primitive, splittable source vs not,
sequential vs parallel. Conventional wisdom says "parallel streams use all your
cores" and "boxing is what kills stream performance." This exercise hands you a
harness that lets the numbers settle the argument.

The four bars:

| bar               | source                                        | accumulator      | mode       |
|-------------------|-----------------------------------------------|------------------|------------|
| `boxed_seq`       | `Stream.iterate(0L, n -> n+1).limit(N)`       | `reduce(0L,Long::sum)` (boxed) | sequential |
| `boxed_par`       | same `iterate(...).limit(N)`                  | boxed            | parallel   |
| `boxed_sized_par` | `LongStream.range(0,N).boxed()`               | boxed            | parallel   |
| `prim_par`        | `LongStream.range(0,N)`                        | `.sum()` (primitive) | parallel   |

Note the controls. `boxed_par` vs `boxed_seq` isolates *threading* (same source,
same boxing). `boxed_sized_par` vs `boxed_par` isolates *splittability* (both
parallel, both boxed — only the source's spliterator differs). `prim_par` vs
`boxed_sized_par` isolates *boxing* (both parallel, both cleanly splittable —
only the accumulator differs).

## Task

1. **Predict first — commit before you run.** Write down, in a comment or scratch
   file:
   - The *direction*: is `boxed_par` faster or slower than `boxed_seq`?
   - The *magnitude*: roughly how many times faster is `prim_par` than
     `boxed_seq`? 2x? 10x? 40x?
   - The *ranking*: of the two cliffs between `boxed_par` and `prim_par`
     (fixing splittability, and removing boxing), **which single step buys the
     most speed?**
2. Implement the four methods in `StreamPerfHarness.java`. Each must return
   `expected = N*(N-1)/2`. Do not change the source or the boxing for a given
   bar — that's the whole point of the controls.
3. Run the harness `main`, read the four numbers, and answer the three
   predictions. If you predicted the ranking wrong, write one sentence on *why*.
4. Make `StreamPerfHarnessTest` green. The red test, `splittabilityIsTheBigger
   Cliff`, encodes the answer: `boxed_sized_par` must land within ~2x of
   `prim_par`, and must crush `boxed_par`.

## Constraints

- Java 21, no external libraries. Do not weaken the test's assertions.
- `boxed_seq` / `boxed_par` MUST use `Stream.iterate(...).limit(N)` (the
  unsized source). `boxed_sized_par` MUST use `LongStream.range(0,N).boxed()`.
  `prim_par` MUST stay primitive end-to-end. Swapping sources defeats the lesson.
- This is timing-sensitive by nature; the test uses generous ratios (best-of-7,
  warmed) so it passes deterministically on a multi-core machine. If you have
  only 1 reported core, the parallel bars can't split — note it and reason from
  the printed `main` output instead.

## How to verify

`mvn -q -Dtest=StreamPerfHarnessTest test` — both tests green.
Run `StreamPerfHarness.main` to see the four times printed.

<details><summary>Hint (open only if stuck)</summary>
`Stream.iterate(seed, op)` is infinite and *unsized*; `limit` on it does not
restore `SIZED`, so the parallel engine must buffer elements one at a time to
split — that's why `boxed_par` barely beats `boxed_seq`. `LongStream.range`
carries `SIZED|SUBSIZED`, so it splits by halving an index range in O(1).
`.boxed()` wraps that spliterator but keeps `SIZED|SUBSIZED`, so it *still*
splits cleanly. The only thing left separating `boxed_sized_par` from `prim_par`
is `Long.valueOf` per element. Now ask: which gap is bigger?
</details>
