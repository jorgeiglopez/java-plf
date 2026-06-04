# Concurrency Performance & Scalability — The Counter That Scales *Backwards*

**Concept:** under high write contention a shared `AtomicLong` scales *negatively* — adding threads makes it slower — and Amdahl's law alone can't explain why. The fix is to stop sharing the hot cache line (`LongAdder` / lock striping).
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You inherit a metrics counter built on `AtomicLong.incrementAndGet()`. Under load
it gets *slower* as you add cores. Speed-up should rise toward the core count;
instead the curve dips below 1.0x — eight threads are slower than one.

Two questions an interviewer will press on:

1. Amdahl's law caps speed-up at `1 / serialFraction`. With a 5% serial section
   that ceiling is 20x. So why does the *measured* curve fall **below 1.0x** — a
   number Amdahl's formula can never produce (it asymptotes upward, it never
   retreats)?
2. `LongAdder` flips the write curve positive. So is it strictly better than
   `AtomicLong`? (No. Know where it loses, or you'll cargo-cult it into the wrong
   workload.)

## Task

Three scaffold files have `TODO` stubs. Make `ScalabilityTest` green.

1. **`AdderCounter.java`** — implement `Counter` over a `java.util.concurrent.atomic.LongAdder`.
   `increment()` → `add(1)`; `read()` → `sum()`.
2. **`Amdahl.maxSpeedup(serialFraction, processors)`** — return Amdahl's law:
   `1 / (s + (1 - s) / N)`.
3. **`Amdahl.serialFractionFromSpeedup(measuredSpeedup, processors)`** — invert it:
   given a measured speed-up `S` on `N` processors, solve for `s`.

Then **run the demo** (`ScalabilityDemo.main`) and read the printed curves — the
`AtomicLong` curve sagging below 1.0x while `LongAdder` climbs is the whole point.

## Constraints

- Java 21, no external libraries. (Production benchmarks use JMH — see below — but
  it's not on this classpath, so the demo is a plain fixed-work harness.)
- Do **not** modify `ScalabilityTest`, `ThroughputHarness`, or `Counter`.
- The harness already asserts zero lost updates, so a half-correct counter throws.

## Scale up (optional)

- **Read-mostly betrayal.** Add a workload where threads mostly call `read()` and
  rarely `increment()`. `LongAdder.sum()` walks the whole `Cell[]`
  non-atomically (and isn't linearizable). Predict — then observe — `AtomicLong`
  *winning* this mix. When does `LongAdder` stop being the answer?
- **Back out the serial fraction.** Feed your *measured* multi-thread speed-up
  into `serialFractionFromSpeedup`. For the `AtomicLong` nosedive you'll get a
  *negative* or nonsensical `s`. Explain why: the model has no term for it.

## How to verify

```
mvn -q -Dtest=ScalabilityTest test
```

To *see* the scaling curves (observational, timing varies by machine):

```
mvn -q compile
java -cp target/classes plf.s06_concurrency.s06_13_performance_and_scalability.ScalabilityDemo
```

For real measurement you'd reach for JMH (markdown only — not on this classpath):

```java
@Benchmark
@Threads(8)
@BenchmarkMode(Mode.Throughput)
public void atomicInc(Blackhole bh) { counter.increment(); }
```

<details><summary>Hint (open only if stuck)</summary>
Amdahl is pure arithmetic; the inversion is algebra: from
`S = 1/(s + (1-s)/N)` solve `1/S = s + (1-s)/N` for `s`. The nosedive below 1.0x
isn't in this equation at all — that's the lesson, not a bug in your formula.
</details>
