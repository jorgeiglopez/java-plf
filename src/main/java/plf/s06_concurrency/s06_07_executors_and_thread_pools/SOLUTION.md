# Solution — `maximumPoolSize` is a Liar

## Ideal solution

```java
// core=2, max=10, unbounded LinkedBlockingQueue, 50 tasks
public static final int BEAT_1_PEAK_THREADS = 2;

// core=2, max=50, SynchronousQueue, 50 tasks
public static final int BEAT_2_PEAK_THREADS = 50;
```

`2`, not `10`. And `50`, not `2`. Same submit loop, same task count, same
`ThreadPoolExecutor` class — only the queue changed.

## Why this works (and the gotcha)

`ThreadPoolExecutor.execute` follows three steps in order:

1. workers `< corePoolSize` → start a core worker.
2. otherwise → `workQueue.offer(task)`.
3. **only if `offer()` returns `false`** → start a worker up to `maximumPoolSize`
   (else hand the task to the `RejectedExecutionHandler`).

So a worker beyond core is born **only when the queue refuses a task.** The queue
is the admission-control valve; `maximumPoolSize` is just the ceiling on what the
valve can trigger. If the valve never trips, `maximumPoolSize` is dead config.

**Beat 1 — unbounded `LinkedBlockingQueue`.** After the 2 core workers exist,
every subsequent `offer()` succeeds because the queue has unlimited room. Step 3
never fires. The remaining 48 tasks sit in the queue, drained two-at-a-time as
the core workers finish. Peak threads = **2**. The `max=10` you wrote is a lie
the queue tells you. (This is exactly why `Executors.newFixedThreadPool(n)` —
which pairs `max=n` with an unbounded `LinkedBlockingQueue` — is safe: `max`
equals `core`, so the unreachable `max` doesn't matter.)

**Beat 2 — `SynchronousQueue`.** This queue has **zero capacity**. Its `offer()`
returns `true` only if a consumer is *already parked* in `take()`/`poll()` at that
instant; otherwise it returns `false` immediately. In the harness every started
worker grabs its first task and parks on the latch — it never returns to poll the
queue. So no consumer is ever waiting, every `offer()` fails, and step 3 fires on
every task: the pool climbs straight to `max`. With `max=50` and 50 tasks, peak
threads = **50**.

The one-sentence takeaway a student can say back:
> *The queue's `offer()`, not `maximumPoolSize`, decides whether the pool grows —
> and `SynchronousQueue.offer()` fails unless a consumer is parked right now,
> while an unbounded queue's `offer()` never fails at all.*

### Why the test is deterministic (no `Thread.sleep`)

The naive way to demo this counts distinct thread names after `sleep(100)` tasks
— and it's flaky: a worker can recycle, an `offer()` can win a handoff race, and
`getLargestPoolSize()` reads 49 on a slow CI box. The harness removes all timing:
each task parks on a shared `CountDownLatch` and never completes until the test
releases it. Pinned workers can't recycle and can't become queue consumers, so
the pool reaches its policy-determined steady state and *stays* there. The probe
waits until `getPoolSize()` is stable with every worker busy, then reads
`getLargestPoolSize()`. Same integers every run.

### The trap inside the trap (why Beat 2 uses `max=50`, not `max=10`)

If you set `max=10` with the `SynchronousQueue`, task 11's `offer()` fails *and*
the pool is already at `max` → the task is rejected by the default `AbortPolicy`,
throwing `RejectedExecutionException` mid-loop. That drags a *third* mechanism
(rejection) into a *sizing* lesson and muddies which thing bit you. `max=50`
swallows the whole burst cleanly so the only variable on display is "does the
queue force growth." Queue capacity and pool `max` are a **joint admission-control
decision**, not two independent knobs.

## Common mistakes

- **"`max=10` means up to 10 threads."** Only if the queue can reject. With an
  unbounded queue, `max` is never consulted; you get `core` threads, period.
- **Reaching for `Executors.newFixedThreadPool` to "see the threads."** The
  factory hides the queue choice — the very thing that determines the answer. Use
  the raw `ThreadPoolExecutor` constructor when sizing behavior matters.
- **Bounding the queue to force growth, then forgetting rejection.** Swapping to
  `new ArrayBlockingQueue<>(4)` with `max=10` and 50 instantly-submitted tasks
  admits 2 (core) + 4 (queue) + 8 (grow to max) = 14, then throws
  `RejectedExecutionException` on task 15. You traded a sizing bug for a rejection
  bug.
- **Trusting a `sleep`-based distinct-thread count.** Worker recycling and handoff
  races make it read low intermittently; assert on `getLargestPoolSize()` with
  pinned tasks instead.

## Interviewer Q&A

**Q:** In what order does `ThreadPoolExecutor` decide to queue a task vs. spawn a
new thread?
**A:** Core first: if workers < `corePoolSize`, start a core worker. Otherwise
`offer()` to the queue. Only if `offer()` fails does it start a non-core worker up
to `maximumPoolSize`; if that also can't happen, the `RejectedExecutionHandler`
runs.

**Q:** You set `core=2, max=10` with an unbounded `LinkedBlockingQueue` and flood
it. How many threads run?
**A:** 2. The unbounded queue's `offer()` never fails, so the pool never grows
past core. `max=10` is dead configuration.

**Q:** Then how does `newFixedThreadPool(n)` ever use all `n` threads if it uses
an unbounded queue?
**A:** Because it sets `core == max == n`. All `n` workers are *core* workers, so
they're created by step 1, not by queue rejection. The unbounded queue only ever
buffers the overflow beyond `n`.

**Q:** What changes with a `SynchronousQueue`?
**A:** It has zero capacity — `offer()` succeeds only if a consumer is parked in
`take()` right now. Under a burst, offers fail constantly, so the pool grows to
`maximumPoolSize`. This is exactly what `newCachedThreadPool` uses (with `max =
Integer.MAX_VALUE`), which is why it can spawn unbounded threads under load.

**Q:** With `SynchronousQueue`, `core=2`, `max=10`, you submit 11 tasks that all
block. What happens to task 11?
**A:** Tasks 1–2 start core workers; tasks 3–10 each fail `offer()` and spawn
workers up to `max=10`; task 11 fails `offer()` but the pool is at `max`, so it's
rejected — default `AbortPolicy` throws `RejectedExecutionException`.

**Q:** When *does* a bounded `ArrayBlockingQueue` let the pool reach `max`?
**A:** When the queue fills. Fill order is core → queue → new workers. With
capacity `c`, the pool starts growing past core on roughly the `(core + c + 1)`-th
concurrently-pending task and reaches `max` after `core + c + max-core` are in
flight; beyond that, tasks are rejected. Queue capacity and `max` jointly set the
admission limit.

**Q:** What are the four standard `RejectedExecutionHandler` policies?
**A:** `AbortPolicy` (default — throws `RejectedExecutionException`),
`CallerRunsPolicy` (runs the task on the submitting thread, providing
backpressure), `DiscardPolicy` (silently drops it), and `DiscardOldestPolicy`
(drops the head of the queue and retries).

**Q:** Why is `getLargestPoolSize()` a better assertion than counting distinct
thread names from `sleep`-based tasks?
**A:** `getLargestPoolSize()` is the executor's own high-water mark and is exact.
Counting names is racy: workers recycle, `SynchronousQueue` handoffs can avoid
spawning a new worker, and on a slow box you under-count. Pinning every task on a
latch removes the timing entirely and makes the count reproducible.

**Q:** How would you size a pool for a CPU-bound vs. an I/O-bound workload?
**A:** CPU-bound: ~`Ncpu` (or `Ncpu + 1`) threads — more just adds context-switch
overhead. I/O-bound: `Ncpu * (1 + W/C)` where `W/C` is wait-time to compute-time
ratio, since threads spend most time blocked. Pair with a *bounded* queue plus a
rejection policy for backpressure — an unbounded queue under overload is a latent
`OutOfMemoryError`.
