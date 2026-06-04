# Solution — The Counter That Scales Backwards

## Ideal solution

`AdderCounter`:

```java
import java.util.concurrent.atomic.LongAdder;

public final class AdderCounter implements Counter {
    private final LongAdder value = new LongAdder();

    @Override public void increment() { value.add(1); }
    @Override public long read()      { return value.sum(); }
}
```

`Amdahl`:

```java
public static double maxSpeedup(double s, int n) {
    return 1.0 / (s + (1.0 - s) / n);
}

public static double serialFractionFromSpeedup(double speedup, int n) {
    // from  S = 1/(s + (1-s)/N):  1/S = s + (1-s)/N  ->  1/S - 1/N = s(1 - 1/N)
    return (1.0 / speedup - 1.0 / n) / (1.0 - 1.0 / n);
}
```

### Scale-up solutions

**Read-mostly betrayal.** With a 99% `read()` / 1% `increment()` mix and many
threads, `AtomicLong` typically *beats* `LongAdder`. `AtomicLong.get()` is a
single plain volatile read of one field. `LongAdder.sum()` has to iterate the
whole `Cell[]` and add every cell — O(cells) per read, and the result isn't even
linearizable (a concurrent increment can land in a cell already summed or not yet
summed). `LongAdder` optimizes the *write* path by paying on the *read* path, so
a read-heavy counter regresses. Right tool: `AtomicLong` (or just a contended
counter you read rarely). The lesson: "the fast concurrent thing" is workload-
relative.

**Back out the serial fraction.** Plug a measured `AtomicLong` 8-thread speed-up
of, say, 0.6 into `serialFractionFromSpeedup(0.6, 8)`:

```
s = (1/0.6 - 1/8) / (1 - 1/8) = (1.6667 - 0.125) / 0.875 = 1.762
```

A serial fraction of 176% is nonsense — `s` is a probability in `[0, 1]`. The
model breaks because the measured number lies *below* Amdahl's hard floor of
1.0x, and the formula has no term that can produce it.

## Why this works (and the gotcha)

`AtomicLong.incrementAndGet()` is a CAS retry loop over **one** memory word. Put
that word on one cache line and let N cores hammer it: every successful CAS
invalidates that line in every other core's cache, so the line ping-pongs across
the interconnect (MESI coherence traffic). Failed CAS attempts spin and retry.
Throughput doesn't just plateau — it *collapses*: 8 threads can be slower than 1.

`LongAdder` is **lock striping applied to a counter**. Internally
(`Striped64`) it keeps a `Cell[]`; under contention each thread hashes to its own
cell and increments *that*, so different threads touch different cache lines. The
cells are `@Contended`-padded to dodge false sharing. `sum()` adds the cells.
Writes stop contending → the curve climbs. That is the entire "stop sharing the
hot line" idea, in the JDK.

**The Amdahl gotcha.** Amdahl's law: `speedup = 1 / (s + (1-s)/N)`. As `N → ∞`
this rises toward `1/s` and **never** drops below 1.0x. It models *serial work
that can't be parallelized*; it does **not** model *coherence traffic*, which is
extra work that only *appears* when you add threads. So a measured sub-1.0
speed-up isn't "Amdahl being wrong" — it's a cost Amdahl never claimed to model.
Candidates who try to explain the nosedive with Amdahl alone have the wrong
mental model; the gap between Amdahl's prediction and the measured nosedive *is*
the cache.

**Why the harness coordinates with a `CyclicBarrier`:** all worker threads block
on the gate, then start incrementing simultaneously, maximizing the collision
window. The test's correctness assertion (zero lost updates) is deterministic;
the *timing* is observational only — never asserted — so the test can't be flaky.

## Common mistakes

- **`AtomicLong` is "lock-free, therefore scalable."** Lock-free ≠ contention-free.
  Single-word CAS still serializes through one cache line; it scales *negatively*
  under write contention.
- **`LongAdder` is strictly better than `AtomicLong`.** Only for write-heavy,
  read-rarely counters. `sum()` is O(cells) and non-linearizable; read-mostly
  workloads regress.
- **Explaining the nosedive with Amdahl.** Amdahl asymptotes upward and floors at
  1.0x; it can't produce sub-1.0. Inverting a sub-1.0 speed-up yields `s > 1`,
  which is the tell that you're outside the model.
- **Benchmarking with `System.nanoTime()` around a loop and trusting it.** JIT
  dead-code-elimination and warm-up effects wreck naive microbenchmarks; use JMH
  (`@Threads`, `Mode.Throughput`, `Blackhole`) for real numbers.
- **"It passed 100 runs, so it's correct."** Loop-testing proves nothing about
  happens-before / interleavings. For *correctness* under reordering, reach for
  `jcstress`, which enumerates interleavings — that's a separate axis from
  throughput.

## Interviewer Q&A

**Q:** Speed-up should rise with cores. Why does the `AtomicLong` counter get
*slower*?
**A:** All increments CAS one memory word on one cache line. Each successful CAS
invalidates that line everywhere else, so it ping-pongs across cores (MESI
coherence traffic) and failed CASes spin. The coherence cost grows with thread
count, so throughput collapses below the single-thread number.

**Q:** Amdahl's law says a 5% serial section caps speed-up at 20x. How does that
square with a measured speed-up below 1.0x?
**A:** It doesn't, and that's the point. Amdahl models only non-parallelizable
*serial* work; it asymptotes toward `1/s` and never goes below 1.0x. A sub-1.0
result is coherence/contention overhead — work that only exists *because* you
added threads — which Amdahl's formula has no term for.

**Q:** How does `LongAdder` fix it?
**A:** Lock striping for a counter. `Striped64` keeps a padded `Cell[]`; under
contention each thread hashes to its own cell, so threads write different cache
lines instead of fighting over one. `sum()` adds the cells. Less sharing → the
curve goes positive.

**Q:** Is `LongAdder` always the right choice over `AtomicLong`?
**A:** No. For read-mostly counters `AtomicLong.get()` is a single volatile read,
while `LongAdder.sum()` is O(cells) and not linearizable. `LongAdder` trades
cheap writes for expensive, approximate reads — great for hot write counters,
wrong for read-heavy ones.

**Q:** What does "not linearizable" mean for `sum()`?
**A:** `sum()` walks the cells without a global lock, so a concurrent increment
may or may not be included depending on timing; there's no single instant the
returned value corresponds to. It's eventually consistent, not a point-in-time
snapshot.

**Q:** What is false sharing and how does `LongAdder` avoid it?
**A:** False sharing is two unrelated variables landing on the same cache line, so
writing one invalidates the other's line for no logical reason. `Striped64`'s
cells are `@Contended` (padded) so each sits on its own line, avoiding it.

**Q:** Beyond striping, how would you reduce lock contention and context
switches in general?
**A:** Reduce lock scope / hold time, split one lock into several (lock
splitting), stripe a lock over key ranges (lock striping, e.g. legacy
`ConcurrentHashMap`), use read/write or `StampedLock` for read-heavy state,
prefer immutable/confined state to eliminate sharing, and right-size thread pools
to core count so runnable threads don't thrash the scheduler.

**Q:** You "tested" the concurrent counter by running it 100 times and it passed.
Convince me it's correct.
**A:** I can't from that. Loop-testing only samples whatever interleavings the
scheduler happened to pick; it says nothing about happens-before. For throughput
I'd use JMH (`@Threads`, `Mode.Throughput`, `Blackhole` to defeat dead-code
elimination). For *correctness* under reordering I'd use jcstress, which
systematically exercises interleavings and flags illegal outcomes.

**Q:** Why coordinate the harness threads with a `CyclicBarrier` instead of just
starting them?
**A:** To make them collide. The barrier releases all workers at the same instant,
maximizing overlap on the hot line so contention actually manifests. Staggered
starts can hide it. (Correctness is asserted deterministically; timing stays
observational so the test can't be flaky.)
