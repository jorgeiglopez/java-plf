# Solution — Two cliffs, and you'll rank them wrong

## Ideal solution

```java
static long boxedSeq() {
    return Stream.iterate(0L, n -> n + 1).limit(N)
            .reduce(0L, Long::sum);
}

static long boxedPar() {
    return Stream.iterate(0L, n -> n + 1).limit(N)
            .parallel()
            .reduce(0L, Long::sum);
}

static long boxedSizedPar() {
    return LongStream.range(0, N).boxed()
            .parallel()
            .reduce(0L, Long::sum);
}

static long primPar() {
    return LongStream.range(0, N)
            .parallel()
            .sum();
}
```

Typical numbers on an 8-core laptop (best-of-7, JIT-warmed), N = 10,000,000:

```
boxed_seq         ~90 ms
boxed_par         ~75 ms      (threads bought ~nothing)
boxed_sized_par   ~ 6 ms      (splittability fixed — huge)
prim_par          ~ 4 ms      (boxing removed — modest extra)
```

Absolute numbers vary; the *shape* does not: `boxed_par ≈ boxed_seq`,
`boxed_sized_par` is a 10x+ leap, and `prim_par` is only a little further.

## Why this works (and the gotcha)

There are two independent costs, and the harness isolates each with a control.

**Cliff 1 — splittability (the big one).** `Stream.iterate(seed, op)` is an
infinite, *unsized*, `ORDERED`-only stream. `limit(N)` bounds it but does **not**
restore the `SIZED` characteristic. The parallel engine can't say "the work is
N elements, split it in half" — it has to pull elements through an
`AbstractSpliterator` buffering path, essentially serially, then hand crumbs to
worker threads. So `boxed_par` barely beats `boxed_seq`: you paid for thread
coordination and got almost no parallelism. `LongStream.range(0, N)` instead
carries `SIZED | SUBSIZED`, so its spliterator splits by bisecting an index range
in O(1) — perfect fan-out. Crucially, `.boxed()` *wraps* that spliterator and
**keeps** `SIZED | SUBSIZED`. That's why `boxed_sized_par`, still allocating a
`Long` per element, still splits cleanly and leaps to ~6 ms.

**Cliff 2 — boxing (the smaller one).** `boxed_sized_par` vs `prim_par` is now a
pure boxing comparison: same parallelism, same clean split, the only difference
is `Long.valueOf` + unbox in `Long::sum` versus a primitive `long` accumulator in
`LongStream.sum()`. For a trivial `+` operation that gap is real but *modest* —
under 2x. The allocation pressure is genuine, but the JIT's escape analysis and a
cheap young-gen collection absorb most of it.

**The gotcha — you ranked them backwards.** Folklore (and every blog that
screamed "boxing kills streams!") primes you to predict that removing boxing is
the headline win. The numbers say otherwise: the jump from `boxed_par` to
`boxed_sized_par` (splittability) is the cliff; `boxed_sized_par` to `prim_par`
(boxing) is a step. People who only ever compare `iterate.parallel` against
`LongStream.range.sum` see a 40x gap and attribute the *whole* thing to "boxing"
or "GC ate my lunch" — they never inserted the `boxed_sized_par` control, so they
never saw that splittability did most of the work. The test pins this: it
demands `boxed_sized_par <= prim_par * 2`.

## Common mistakes

- **"Parallel uses all my cores, so `boxed_par` will be ~Nx faster."** No — an
  unsplittable source serializes the split, so parallel buys almost nothing and
  even loses to sequential once coordination overhead is counted.
- **"`limit(N)` makes the stream sized."** It bounds it but leaves it unsized;
  `iterate` is the culprit. (`IntStream.range`/`rangeClosed` are sized;
  `iterate` and `generate` are not.)
- **"Boxing is the dominant cost / GC ate my lunch."** For cheap per-element
  work, splittability dominates. Boxing is the smaller cliff here; you can only
  see that with the SIZED-but-boxed control.
- **Comparing only `iterate.parallel` vs `LongStream.range`.** That conflates two
  variables. The 40x gap is splittability *and* boxing compounded; without an
  intermediate control you can't attribute it.
- **`reduce(0L, Long::sum)` "is just a long add."** On a `Stream<Long>` it
  unboxes both operands and reboxes the result every accumulation;
  `LongStream.sum()` never boxes.

## Interviewer Q&A

**Q:** What does `.parallel()` actually do to a stream?
**A:** It flips the pipeline to execute on the `ForkJoinPool.commonPool()` (plus
the submitting thread), splitting the source via its `Spliterator` and combining
partial results. It changes nothing about correctness for a properly stateless,
associative reduction — only scheduling.

**Q:** Why does `Stream.iterate(...).limit(N).parallel()` barely beat sequential?
**A:** `iterate` produces an infinite, unsized, ORDERED spliterator; `limit`
doesn't restore `SIZED`. Without a size the engine can't bisect the work, so it
buffers elements through `AbstractSpliterator` largely serially. You pay
coordination overhead for almost no parallel work.

**Q:** Which `Spliterator` characteristics matter for parallel speedup, and which
sources have them?
**A:** `SIZED | SUBSIZED` enable O(1) balanced splitting. Array-backed and
range-backed sources (`IntStream.range`, `Arrays.stream`, `ArrayList`) have them;
`LinkedList` is SIZED but splits O(n) and unbalanced; `iterate`/`generate` and
`HashSet`/`BufferedReader.lines()` lack SIZED entirely.

**Q:** Does `.boxed()` on a `LongStream.range` lose splittability?
**A:** No. `.boxed()` wraps the underlying spliterator, preserving
`SIZED | SUBSIZED`, so it still splits binary by index. The only added cost is a
`Long` allocation per element.

**Q:** So in this exercise, what causes the 40x gap between `iterate.parallel` and
`prim_par` — boxing or splittability?
**A:** Mostly splittability. The SIZED-but-boxed control (`boxed_sized_par`)
already captures the large jump; removing boxing only adds a sub-2x improvement
on top. Boxing is real but the smaller cliff for cheap per-element work.

**Q:** When would boxing become the dominant cost instead?
**A:** When per-element work is tiny and allocation rate is the bottleneck, or in
sequential pipelines where there's no splittability dimension at all — e.g.
`Stream<Integer>` summing a billion elements vs `IntStream`. Primitive streams
also win on memory footprint and cache behavior regardless of parallelism.

**Q:** EJ48 says "use caution making streams parallel." Summarize the
preconditions for a parallel stream to actually pay off.
**A:** A source that splits cheaply and evenly (arrays, ranges,
`ArrayList`, `ConcurrentHashMap`), enough total elements times per-element cost to
amortize fork/join overhead (rule of thumb ~100k+ elements of real work), a
stateless and associative reduction, no shared mutable state, and a terminal op
that combines cheaply. Order-sensitive ops (`forEachOrdered`, `limit` on ordered
streams) erode the gains.

**Q:** What's the danger of doing blocking I/O inside a parallel stream?
**A:** It runs on the shared `commonPool` (sized `availableProcessors() - 1`).
Blocking those threads starves every other parallel stream in the JVM. Use a
dedicated pool (submit the stream task to your own `ForkJoinPool`) or don't
parallelize blocking work at all.
