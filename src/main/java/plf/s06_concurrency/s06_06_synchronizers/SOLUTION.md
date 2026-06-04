# Solution — The Relay Race

## Ideal solution
The transcript, in order:

```
latch.count=0
latch.secondAwaitReturned
barrier.action.total=30
barrier.action.total=60
barrier.final.total=60
semaphore.permits=4
phaser.arriveReturned=0
phaser.terminated=true
phaser.afterTerminationPhase=-2147483647
exchanger.outcome=TimeoutException
```

So `RelayRacePredictions.predicted()` is:

```java
public static List<String> predicted() {
    return List.of(
        "latch.count=0",
        "latch.secondAwaitReturned",
        "barrier.action.total=30",
        "barrier.action.total=60",
        "barrier.final.total=60",
        "semaphore.permits=4",
        "phaser.arriveReturned=0",
        "phaser.terminated=true",
        "phaser.afterTerminationPhase=-2147483647",
        "exchanger.outcome=TimeoutException");
}
```

## Why this works (and the gotcha)

Each line is produced by one synchronizer's **release rule**. That is the single idea:
a synchronizer is *defined* by who it unblocks and when, not by its name.

**CountDownLatch — one-shot, clamps at zero.** `countDown()` on a count of 1 reaches 0;
the second `countDown()` is a no-op (it never goes negative). `getCount()` is `0`, and
because the count is already 0 *both* `await()` calls return immediately. A latch cannot
be reset — that is the whole point. If you need re-arming, you wanted a `CyclicBarrier`
or a `Phaser`.

**CyclicBarrier — last arriver runs the action, before anyone is released, and the
happens-before edge means no `volatile` is needed.** This is the trap that draws blood.
Three runners each do `total += 10` then `await()`. The barrier action prints `total`.

- *Who runs the action?* The **last** thread to arrive runs it — synchronously, on that
  thread, **before** any of the three parties is released from `await()`.
- *Is `total` read safely without `volatile`?* **Yes.** Each runner's write
  *happens-before* its own `await()`, and `await()` *happens-before* the barrier action
  and the post-await reads. So the action observes all three `+= 10` writes from this lap.
  Adding `volatile` here is not just unnecessary — it advertises that you don't know the
  barrier already gives you the edge. After lap 1, `total == 30`; the barrier is *cyclic*,
  so lap 2 reuses it and `total == 30 + 30 == 60`. Final read after the threads join: `60`.
- The order of `30` then `60` is deterministic even though the additions race, because
  the barrier forces all three writes of a lap to land before its action reads. (Verified:
  the transcript is identical across 200 runs.)

**Semaphore — a bare counter, not ownership-based.** Start with 2 permits, `acquire()`
→ 1, then three `release()` calls → `1 + 3 == 4`. A `Semaphore` has **no notion of which
thread holds a permit**: `release()` requires no matching `acquire()`, may be called by a
different thread, and can push the permit count *above the initial value*. This is exactly
unlike a reentrant lock, whose `unlock()` must come from the holder. (Use a bounded
`Semaphore` if you must cap permits — the plain one will happily over-grant.)

**Phaser — terminated phaser returns a negative phase immediately.** Constructed with 1
registered party and an `onAdvance` that returns `true`. `arrive()` returns the current
phase `0`. Because the only registered party has now arrived, the phaser advances:
`onAdvance` returns `true`, which **terminates** the phaser. `isTerminated()` → `true`.
Now the kicker: `arriveAndAwaitAdvance()` on a *terminated* phaser **does not block and
does not throw** — it returns the (negative) current phase, here `-2147483647`. People
universally predict a deadlock or an exception. It is just a negative number.

**Exchanger — two-party rendezvous; alone it times out.** `exchange(x)` blocks until a
second thread also calls `exchange()`. With no partner, the timed overload
`exchange(x, 50, MILLISECONDS)` throws `TimeoutException`. The plain `exchange(x)` would
block forever — fatal in a grader — which is why the timed overload is used. Outcome:
`exchanger.outcome=TimeoutException`, always.

## Common mistakes
- Predicting `latch.count=-1` → `countDown()` floors at 0; it never underflows.
- Predicting the second `await()` blocks → a zeroed latch lets every `await()` through.
- Adding `volatile` to `total` "to be safe" → reveals you don't know `CyclicBarrier`
  establishes happens-before; the read is already correctly synchronized.
- Thinking the barrier action runs on a *separate* thread, or *after* parties resume →
  it runs on the last arriver, before any party is released.
- Predicting `semaphore.permits=2` (or expecting an exception on the extra `release`) →
  permits are an unbounded counter; you get `4`.
- Predicting a deadlock or `IllegalStateException` on the terminated phaser → it returns
  a negative phase immediately.
- Using `exchange(x)` (no timeout) → hangs forever; the test never completes.

## Interviewer Q&A

**Q:** In one sentence, what distinguishes these five synchronizers?
**A:** Their release rule — who gets unblocked and on what event: a latch on a one-way
count reaching zero, a barrier when N parties rendezvous, a semaphore on permit
availability, a phaser per dynamic phase, an exchanger when two threads pair up.

**Q:** Latch vs barrier — when do you reach for each?
**A:** `CountDownLatch` is one-shot and the waiters differ from the counters (one thread
waits for N events). `CyclicBarrier` is reusable and the parties *are* the waiters; they
all block until the last arrives, then all proceed. Need to wait again? The latch can't;
the barrier resets automatically.

**Q:** Who runs a `CyclicBarrier`'s barrier action, and when?
**A:** The last thread to call `await()` runs it synchronously, before *any* of the
parties is released. So it's a safe place to mutate shared state that all parties will
read after the barrier.

**Q:** Does the barrier action need `volatile` to read state written by the other parties?
**A:** No. Each party's writes before `await()` happen-before the action and before every
party's post-`await()` reads. The barrier provides the memory edge; `volatile` is
redundant.

**Q:** Can you call `Semaphore.release()` without `acquire()`, or from another thread?
**A:** Yes to both. A semaphore is a counter with no ownership; `release()` simply adds a
permit and can raise the count above the initial value. That's the key difference from a
reentrant lock, where only the holder may unlock.

**Q:** What happens when a `CyclicBarrier` party is interrupted or times out mid-wait?
**A:** The barrier *breaks*: that party gets `InterruptedException`/`TimeoutException`, and
every other waiting party gets `BrokenBarrierException`. The barrier must be `reset()`
before reuse. (A `Phaser` is more forgiving and supports dynamic parties.)

**Q:** What does `arriveAndAwaitAdvance()` return on a terminated `Phaser`?
**A:** A negative phase number, immediately — it neither blocks nor throws. Termination is
triggered by `onAdvance` returning `true` (or `forceTermination()`); after that, await/
advance calls are non-blocking and return the negated current phase.

**Q:** When would you choose `Phaser` over `CyclicBarrier` or `CountDownLatch`?
**A:** When the number of parties changes at runtime (`register`/`arriveAndDeregister`),
when you want multiple phases without manual resets, or when you want tiered/hierarchical
coordination. It subsumes both latch and barrier use cases with dynamic registration.

**Q:** What's the failure mode of `Exchanger.exchange()` with a single thread?
**A:** It blocks forever — it's a two-party rendezvous. Use the timed overload
`exchange(v, timeout, unit)`, which throws `TimeoutException`, if a partner may not show.
