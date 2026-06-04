# Virtual Threads — The Carrier Held Hostage

**Concept:** a virtual thread frees its carrier when it parks — *unless* `synchronized` pins it to the carrier (Java 21).
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
`Executors.newVirtualThreadPerTaskExecutor()` will happily spawn a million virtual
threads. They scale because a virtual thread that *parks* (blocks on I/O, a latch, a
lock, sleep) **unmounts** from its carrier (a platform thread, one per CPU by default),
so the carrier can run another virtual thread. The whole model rests on that one move:
*park ⇒ unmount*.

So here is the question that separates fluent from fluent-sounding: **which blocking
calls actually attempt to unmount, and which silently hold the carrier hostage?** In
Java 21, parking *while inside a `synchronized` block* cannot unmount — the carrier is
**pinned**. Ten thousand virtual threads then queue behind however many CPUs you have.

## Task
`CarrierStarvation.java` launches `tasks` virtual threads; each blocks on a shared
`gate` latch. The harness checks whether **all** of them reached the gate *at the same
time* (proof that every carrier was free to mount a new virtual thread).

Run the test (below). `nonPinning...` is green. `pinningArm...` is **red**: its tasks
each park *inside `synchronized (perTaskMonitor)`*, so their carriers pin and only a
CPU-count's worth of tasks ever reach the gate together — the latch never empties.

1. Fix the **production** code in `CarrierStarvation.runOne` so the `PINNING` arm also
   lets every task reach the gate. The monitor is uncontended (a fresh object per task),
   so mutual exclusion is not the point — the point is *parking without surrendering the
   carrier*. Replace the construct that pins with one that parks cleanly.
2. Do **not** weaken the test, change `tasks`, or delete the lock entirely if your
   intent was real mutual exclusion (here it is per-task, so any park-friendly guard works).

## Constraints
- Java 21, no preview flags, no external libraries.
- Do **not** modify `CarrierStarvationTest`.
- Keep `allReachedGateConcurrently` deterministic — no `Thread.sleep` to "wait it out".

## Scale up (Optional)
**Part B — same wall-clock, opposite cause.** Run two arms under
`-Djdk.virtualThreadScheduler.parallelism=1` (one carrier), each launching N=8 virtual
threads that hold a guard *across* a 1-second `Thread.sleep`:

- **Arm 1:** `synchronized (sharedLock) { Thread.sleep(1s); }`
- **Arm 2:** `lock.lock(); try { Thread.sleep(1s); } finally { lock.unlock(); }`
  on a shared `ReentrantLock`.

Both print **~8 seconds**. Implement `heldAcrossSleepWallClockMillis` and answer in
`SOLUTION.md`'s terms: *only one of these consumed a carrier the entire time. Which?
And what does `-Djdk.tracePinnedThreads=full` print for each?*

## How to verify
```
mvn -q -Dtest=CarrierStarvationTest test
```
Both tests green ⇒ Part A solved. For Part B, run your `main`/arm twice with
`-Djdk.virtualThreadScheduler.parallelism=1 -Djdk.tracePinnedThreads=full` and read the
two stack-trace outputs (one arm emits a pinned-thread trace, the other emits nothing).

<details><summary>Hint (open only if stuck)</summary>
What does a virtual thread need in order to *unmount*? Its continuation must be able to
peel off the carrier's stack. A held monitor (`synchronized`) is a native frame that
nails the continuation down in Java 21. `ReentrantLock.lock()`/`unlock()` is plain Java —
parking inside it unmounts cleanly. The fix is one type swap, not new logic.
</details>
