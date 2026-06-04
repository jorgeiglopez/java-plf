# Solution — The cycle is the deadlock

## Ideal solution

Impose a total order on the two monitors so both call sites agree on acquisition
order. Order by `System.identityHashCode`:

```java
public static void safeTransfer(Account from, Account to, long amt) {
    int hFrom = System.identityHashCode(from);
    int hTo   = System.identityHashCode(to);

    Account first  = hFrom <= hTo ? from : to;
    Account second = hFrom <= hTo ? to   : from;

    synchronized (first) {
        synchronized (second) {
            from.withdraw(amt);
            to.deposit(amt);
        }
    }
}
```

Both `safeTransfer(a, b)` and `safeTransfer(b, a)` now lock the *same* account
first (whichever has the smaller identity hash), so the lock-acquisition graph
has no cycle and a deadlock is impossible.

### Scale up solutions

**1. Hash-collision tie-breaker (JCiP Listing 10.3).** Two distinct accounts can
share an identity hash. When `hFrom == hTo`, `first`/`second` are chosen by an
arbitrary rule that the two threads may resolve in opposite directions — the
cycle returns for that pair. The bulletproof fix is a single *static* tie-breaker
lock acquired first only on the (rare) collision:

```java
private static final Object TIE_LOCK = new Object();

public static void safeTransfer(Account from, Account to, long amt) {
    int h1 = System.identityHashCode(from), h2 = System.identityHashCode(to);
    if (h1 < h2) {
        synchronized (from) { synchronized (to) { move(from, to, amt); } }
    } else if (h1 > h2) {
        synchronized (to) { synchronized (from) { move(from, to, amt); } }
    } else { // collision: serialize the whole pair behind one global lock
        synchronized (TIE_LOCK) {
            synchronized (from) { synchronized (to) { move(from, to, amt); } }
        }
    }
}
```

It is bulletproof because the tie-breaker is itself a *globally ordered* lock
(there is exactly one of it), so on collision every thread agrees to acquire it
first — the order is total again. A real production system sidesteps this by
giving each account a unique, final `long id` and ordering on that; ids never
collide, so no tie-breaker is needed.

**2. `tryLock` livelock → detector returns `null`.** With a correct
`tryLock()/finally-release` retry loop, no thread is ever `BLOCKED` — both are
`RUNNABLE`, spinning. `ThreadMXBean.findDeadlockedThreads()` only walks ownership
cycles among *monitors* and `AbstractOwnableSynchronizer` locks held by *blocked*
threads. A spinning retry loop owns nothing while it spins, so the bean reports
**`null`**. The MXBean is blind to livelock by construction — livelock is a
progress problem, not an ownership cycle.

**3. The leak that re-deadlocks → detector returns non-null again.** Drop the
`finally`:

```java
if (from.lock.tryLock()) {
    if (to.lock.tryLock()) { ... to.lock.unlock(); }
    // BUG: from.lock never released when inner tryLock fails
}
```

Now a thread that fails the inner acquire keeps the outer lock forever. The
threads become permanently `BLOCKED` on `ReentrantLock` — which is an
`AbstractOwnableSynchronizer` whose ownership cycles the bean *does* walk. So
`findDeadlockedThreads()` returns a **non-null** `long[]` again. The lesson: the
detector's blindness in #2 is *conditional on correct lock hygiene*. Broken
`tryLock` code deadlocks harder than the original and the bean sees it.

## Why this works (and the gotcha)

A deadlock is exactly a cycle in the "who-holds-what, who-waits-for-what" graph.
Two threads acquiring the same pair of locks in opposite orders is the minimal
cycle. You cannot fix it with timeouts, fairness, or retries — those change
*symptoms*. You fix it by making a cycle structurally impossible: define a total
order over locks and have every thread acquire in that order. With a total order,
the wait-for graph is acyclic by construction.

The senior gotcha is the detector asymmetry. People assume
`findDeadlockedThreads()` "detects deadlocks." It detects *ownership cycles among
blocked threads*. Flip the same hazard into a livelock (correct `tryLock` spin)
and the bean goes blind (`null`) — same liveness failure, undetected, because
spinning threads own nothing. Break the `tryLock` hygiene and it lights up again.
Knowing *when the tool reports null* is the difference between memorizing an API
and understanding it.

## Common mistakes

- **"Locks are released each iteration, so there's no lasting cycle."** Deadlock
  is a *transient simultaneous* hold, not a steady state. The barrier in the test
  exists precisely to make that transient overlap deterministic.
- **Ordering on `account.id()` is fine — but ordering on `identityHashCode`
  without a tie-breaker is not bulletproof.** Hashes collide; ids don't.
- **`tryLock` "fixes" the deadlock.** It converts deadlock to livelock (or, if you
  forget `finally`, to a worse deadlock). Neither is a fix.
- **Expecting `findDeadlockedThreads()` to catch livelock.** It returns `null`;
  the threads are `RUNNABLE`, not `BLOCKED`, and own nothing.
- **Calling a `synchronized`-blocked thread `WAITING`.** A thread blocked on a
  monitor is `BLOCKED`. `WAITING` is `Object.wait()`/`park()` without timeout.

## Interviewer Q&A

**Q:** In one sentence, what is a deadlock?
**A:** A set of threads each holding a lock the next one needs, forming a cycle in
the wait-for graph so none can ever proceed.

**Q:** Why does ordering locks by `identityHashCode` prevent it?
**A:** It imposes a total order on lock acquisition; with a total order the
wait-for graph cannot contain a cycle, and a deadlock requires a cycle.

**Q:** What's the flaw in `identityHashCode` ordering, and the fix?
**A:** Two distinct objects can share an identity hash; on a tie the order is
ambiguous and the cycle can reappear. Fix with a single static tie-breaker lock
acquired first on collision, or order on a guaranteed-unique field like a final
`long id`.

**Q:** A junior "fixes" the deadlock with `tryLock(timeout)`. What did they
actually create?
**A:** Potentially livelock — both threads grab one lock, fail the second,
release, retry in lockstep, burning CPU and making no progress. With randomized
back-off it usually resolves; without it, it can spin indefinitely.

**Q:** How do you detect a monitor deadlock programmatically?
**A:** `ManagementFactory.getThreadMXBean().findDeadlockedThreads()` returns a
non-null `long[]` of thread ids in the cycle (and
`findMonitorDeadlockedThreads()` for monitor-only). It walks ownership cycles
among monitors and `AbstractOwnableSynchronizer` locks held by blocked threads.

**Q:** Does that same call detect livelock? Why or why not?
**A:** No — it returns `null`. Livelocked threads are `RUNNABLE`, spinning in a
retry loop, and own no lock while spinning, so there is no ownership cycle to
walk. The MXBean only sees deadlock, not lack of progress.

**Q:** So is "tryLock means the deadlock detector goes blind" always true?
**A:** Only with correct lock hygiene. If the `tryLock` path forgets to release
the outer lock on a failed inner acquire, threads block permanently on a
`ReentrantLock` (an `AbstractOwnableSynchronizer`), and the bean reports the cycle
again — non-null. The blindness is conditional on releasing in `finally`.

**Q:** Distinguish deadlock, livelock, and starvation.
**A:** Deadlock: threads blocked forever in a wait-for cycle (no CPU use).
Livelock: threads active and responding to each other but making no progress
(high CPU). Starvation: a thread makes progress in principle but is perpetually
denied the CPU/lock it needs — e.g. under an unfair lock or priority scheme.
`new ReentrantLock(true)` (fair) evens out the distribution but costs throughput;
`false` (default) permits barging, so no fairness guarantee.

**Q:** What thread state is a thread blocked on `synchronized`, and how is it
different from `WAITING`?
**A:** `BLOCKED` — it's contending for a monitor it doesn't own. `WAITING` is a
thread that voluntarily parked via `Object.wait()`, `LockSupport.park()`, or
`Thread.join()` with no timeout, waiting to be signaled.
