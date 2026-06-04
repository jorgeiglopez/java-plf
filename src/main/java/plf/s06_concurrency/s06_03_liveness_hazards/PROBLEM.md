# Liveness Hazards — The cycle is the deadlock

**Concept:** a cycle in the lock-acquisition graph *is* a deadlock; break it by imposing a single global lock order.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Two threads, a classic bank transfer. `transfer(a, b)` grabs `a`'s monitor then `b`'s.
`transfer(b, a)` grabs `b`'s then `a`'s. Run them at once and the program hangs
*forever* — no exception, no exit code. Why? Thread-1 holds `a` and waits for
`b`; thread-2 holds `b` and waits for `a`. That cycle in the lock-acquisition
graph **is** the deadlock.

The first test forces this cycle deterministically with a `CyclicBarrier` (both
threads provably hold one monitor before either reaches for the second) and then
asks `ThreadMXBean.findDeadlockedThreads()` to point at it. Your job is to make
the cycle impossible.

## Task

1. Implement `Bank.safeTransfer(from, to, amt)` so that `safeTransfer(a, b)` and
   `safeTransfer(b, a)` running concurrently **cannot** deadlock. The marker
   `// <-- Task 1 here` is in `Bank.java`.
2. The rule: acquire the two account monitors in a **global order that does not
   depend on argument order**. Order by `System.identityHashCode(account)` so
   both call sites agree which monitor comes first. Then transfer the money.
3. Turn `BankTest.safeTransferImposesGlobalOrder_noDeadlock` green (it starts red
   — `safeTransfer` throws `UnsupportedOperationException`).

Leave the existing `transfer` method and the deadlock test untouched — that test
must keep passing (it *proves* the unsafe version deadlocks).

## Constraints

Java 21, no external libraries. Do not modify the test file. Do not "fix" the
deadlock by adding a `tryLock` timeout to the unsafe path — that trades deadlock
for livelock and changes the detection story (see Scale up).

## Scale up (Optional)

1. **Hash-collision tie-breaker.** `System.identityHashCode` can collide for two
   distinct accounts. With a pure hash order, a collision (equal hashes) makes
   both threads pick the *same* "first" lock by an arbitrary tie-break — and if
   that tie-break disagrees, the cycle is back. Add a third, *static* tie-breaker
   lock acquired first whenever the two hashes are equal. Why is this the only
   bulletproof fix?
2. **Predict the detector.** Suppose instead of fixing the order you rewrite the
   path with `ReentrantLock.tryLock()` + back-off retry (releasing both in a
   `finally` on failure). The threads no longer block — they spin and retry in
   lockstep (livelock), burning ~100% CPU. **Predict:** does
   `findDeadlockedThreads()` return a non-null `long[]` or `null`? Justify in one
   sentence.
3. **The leak that re-deadlocks.** Now write the *buggy* `tryLock` version: grab
   the inner lock inside the outer `if`, but forget the `finally` that releases on
   the failed inner acquire. **Predict:** what does `findDeadlockedThreads()`
   return now, and why is it different from #2?

## How to verify

```
mvn -q -Dtest=BankTest test
```

Both tests pass when `safeTransfer` imposes a global order.

<details><summary>Hint (open only if stuck)</summary>
Compute `int ha = System.identityHashCode(from)` and `int hb =
System.identityHashCode(to)`. Pick `first`/`second` so the smaller hash is always
locked first, regardless of which is `from`. `synchronized (first) { synchronized
(second) { ... } }`, then `from.withdraw` / `to.deposit`.
</details>
