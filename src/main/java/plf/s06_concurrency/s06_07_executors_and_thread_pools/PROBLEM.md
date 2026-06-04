# Executors & Thread Pools — `maximumPoolSize` is a Liar

**Concept:** The work queue — not `maximumPoolSize` — decides whether a
`ThreadPoolExecutor` ever grows past its core threads.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You configure `new ThreadPoolExecutor(2, 10, ...)` and reason: "burst of work
arrives, the pool spins up to 10 threads, throughput is high." A teammate reviews
it and says the pool will **never** exceed 2 threads. One of you is wrong.

`ThreadPoolExecutor` admits a task in a strict order: **(1)** if fewer than
`corePoolSize` workers exist, start a new core worker; **(2)** else try to
`offer()` the task to the work queue; **(3)** only if the `offer()` *fails* does
it start a new worker, up to `maximumPoolSize`. The hinge of the whole policy is
step 2: *when does `offer()` fail?* Answer that and you can predict the thread
count exactly.

## Task

You ship two pool configurations against the same 50-task burst. Predict the
**peak number of worker threads** each one creates, commit your numbers, then run
the harness to find out.

1. Open `Prediction.java`. Set the two `int` constants:
   - `BEAT_1_PEAK_THREADS` — `core=2, max=10, new LinkedBlockingQueue<>()` (unbounded), 50 tasks.
   - `BEAT_2_PEAK_THREADS` — `core=2, max=50, new SynchronousQueue<>()`, 50 tasks.
2. Write each prediction **before** running the test. Commit to integers.
3. Run the test. If your number is wrong, do not patch the number blindly —
   work out *which queue behavior* you mispredicted, then fix it.

You do not modify `PoolProbe.java` or the test. The harness pins every started
worker on a latch (no thread ever recycles), so `getLargestPoolSize()` is exact
and reproducible — there is no timing luck involved.

## Constraints

- Java 21, no external libraries.
- Edit **only** `Prediction.java`.

## How to verify

```
mvn -q -Dtest=PoolSizingTest test
```

Green when both predictions match the observed peak thread counts.

<details><summary>Hint (open only if stuck)</summary>
Ask: for each queue type, does <code>offer()</code> succeed when 48 tasks are
already pending and every worker is busy? An unbounded <code>LinkedBlockingQueue</code>
always has room. A <code>SynchronousQueue</code> has zero capacity — its
<code>offer()</code> succeeds <em>only</em> if a consumer is parked in
<code>take()</code> at that instant. Which one ever forces step 3?
</details>
