# Solution — The signal that woke the wrong thread

## Ideal solution

```java
public void put(int value) throws InterruptedException {
    lock.lock();
    try {
        while (count == buf.length) notFull.await();   // while, not if
        buf[tail] = value;
        tail = (tail + 1) % buf.length;
        count++;
        notEmpty.signal();                             // wake a consumer specifically
    } finally {
        lock.unlock();
    }
}

public int take() throws InterruptedException {
    lock.lock();
    try {
        while (count == 0) notEmpty.await();
        int v = buf[head];
        head = (head + 1) % buf.length;
        count--;
        notFull.signal();                              // wake a producer specifically
        return v;
    } finally {
        lock.unlock();
    }
}
```

Two load-bearing decisions: **`while`** for every wait, and **two separate
conditions** so `signal()` always targets the right kind of waiter.

A valid-but-weaker variant keeps a single shared `Condition` but replaces every
`signal()` with `signalAll()`. That works because every waiter re-checks its `while`
predicate on wakeup and the wrong-type waiters simply re-park. It is correct but
wakes O(n) threads to make one proceed — wasteful contention. Two conditions is the
idiomatic, scalable form. (`signal()` on a *shared* condition with `while` is still
broken: see below.)

### Scale up solutions
1. **Fairness:** No. A fair lock (`new ReentrantLock(true)`) only changes who acquires
   the lock next; it does nothing about a thread that already passed an `if` check and
   is now executing on a stale predicate. `if`-vs-`while` is a predicate-re-check bug,
   orthogonal to lock fairness. Fairness also costs throughput and never fully
   eliminates barging, so it is rarely the right tool.
2. **`ReentrantReadWriteLock`:** Helps when reads vastly outnumber writes and reads are
   non-trivial, letting readers run concurrently. It hurts under write-heavy or
   short-critical-section workloads — the bookkeeping and reader/writer handoff cost
   more than a plain mutex, and writers can starve under a steady read stream (unless
   fair, which is slower).
3. **`StampedLock` landmines:** (a) It is **not reentrant** — re-acquiring in the same
   thread deadlocks, and its read locks support no `Condition`. (b) With
   `tryOptimisticRead()` you must copy the fields into locals **and then** call
   `validate(stamp)` *before* using them; validating after you have acted on the data
   means you already used a torn read.

## Why this works (and the gotcha)

A `Condition.await()` releases the lock and parks the thread. When it returns, three
things may be true that the waking code never promised:

- **Spurious wakeup** — the JVM is permitted to return from `await()` with no signal at
  all.
- **Barging** — a non-fair `ReentrantLock` (the default) lets a freshly-arrived thread
  acquire the lock between a `signal()` and the awoken thread's reacquire, mutating
  state so the predicate is false again by the time the awoken thread runs.
- **Wrong-target signal** — with one shared `Condition`, the FIFO wait queue interleaves
  producers and consumers. `signal()` wakes the longest waiter regardless of type.

The deterministic kill in `BuggyBoundedBuffer` (capacity 1, one `Condition`, `if`,
`signal`) is the third combined with the first. The test parks two consumers (queue:
`[C1, C2]`), then a single producer puts one item and `signal()`s. That wakes `C1`,
which takes the item (`count` 1→0) and itself `signal()`s — waking `C2`. But `C2`
entered `await()` *inside* `if (count == 0)`, so on wakeup it does **not** re-check;
it reads `buf[head]` from the now-empty buffer and decrements `count` to **-1**. Two
consumers drained one item. `while` alone wouldn't fully save you here either: had `C2`
re-checked and re-parked, that last `signal()` is consumed and lost, and a later
genuinely-waiting thread never gets woken — a slow-motion deadlock. The robust answer
is `while` **and** condition separation (or `signalAll`).

The interleaving is forced deterministically with `lock.getWaitQueueLength(condition)`:
the harness spins until exactly N threads are parked before launching the next actor,
so the bug reproduces on every run with no `Thread.sleep` race.

## Common mistakes
- `if (count == 0) cond.await();` → on any wakeup the thread proceeds on a stale
  predicate; reads garbage from an empty buffer or overwrites a full one.
- One shared `Condition` + `signal()` → wakes the wrong waiter type; the signal is
  spent without unblocking anyone who can make progress → lost wakeup / deadlock.
- "Make the lock fair to fix it" → fairness is about lock acquisition order, not
  predicate re-checking; the `if` bug survives a fair lock.
- Calling `await()` / `signal()` without holding the lock → `IllegalMonitorStateException`.
- Forgetting `lock.unlock()` in a `finally` → a thrown exception leaves the lock held
  forever.

## Interviewer Q&A

**Q:** Why `while` and not `if` around `await()`?
**A:** `await()` may return without the predicate holding — spurious wakeups, barging
on a non-fair lock, or a signal meant for someone else. `while` re-checks and re-parks;
`if` commits to a one-time, possibly-false assumption.

**Q:** Does a fair `ReentrantLock` let you safely use `if`?
**A:** No. Fairness orders lock acquisition; it doesn't guarantee the predicate still
holds when the awoken thread reacquires. Spurious wakeups alone already break `if`.

**Q:** Why two `Condition`s instead of one with `signalAll()`?
**A:** Correctness-wise either works *if* you loop. But two conditions let `signal()`
wake exactly one waiter of the correct type — O(1) wakeups instead of waking and
re-parking every waiter. It scales; `signalAll()` on a shared condition is a thundering
herd.

**Q:** Walk me through how one shared condition + `signal()` deadlocks.
**A:** Producers and consumers share one FIFO wait queue. A `signal()` from a consumer
can wake another consumer (the longest waiter), which re-checks, finds nothing, and
re-parks — consuming the only signal. No producer is ever woken, so the system stalls
with work outstanding.

**Q:** What's the difference between `signal()` and `signalAll()`?
**A:** `signal()` wakes one waiter on that condition (chosen by the queue, FIFO for
`ReentrantLock`'s `Condition`); `signalAll()` wakes all of them. `signalAll()` is the
safe default when one condition is overloaded with mixed waiter types.

**Q:** When would you reach for `ReentrantLock` over `synchronized`?
**A:** When you need `tryLock` (timeouts / non-blocking acquire), interruptible
acquisition, fairness, multiple `Condition`s per lock, or lock acquisition that doesn't
nest in a block (hand-over-hand locking). `synchronized` is simpler, auto-releases, and
is the default otherwise.

**Q:** `ReentrantLock` and `synchronized` are both built on AQS-like machinery — what is
AQS and what does a custom synchronizer override?
**A:** `AbstractQueuedSynchronizer` maintains an `int` state plus a FIFO wait queue and
the park/unpark/queueing plumbing. A custom synchronizer subclasses it and overrides
`tryAcquire`/`tryRelease` (exclusive) or `tryAcquireShared`/`tryReleaseShared`,
manipulating `state` via `getState`/`compareAndSetState` and, for a mutex, recording the
owner with `setExclusiveOwnerThread`. `ReentrantLock`, `Semaphore`, `CountDownLatch`,
and `ReentrantReadWriteLock` are all thin AQS subclasses.

**Q:** Why is `StampedLock` a trap for the unwary?
**A:** It isn't reentrant and offers no `Condition`s, so it can't drop into existing
`synchronized`/`ReentrantLock` patterns. Its optimistic read requires reading fields
into locals and then calling `validate(stamp)` before acting; validating after use means
you've already consumed a possibly-torn read.
