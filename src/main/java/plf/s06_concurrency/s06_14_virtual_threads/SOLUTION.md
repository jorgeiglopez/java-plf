# Solution — The Carrier Held Hostage

## Ideal solution
The `PINNING` arm pins because it parks (`gate.await()`) while holding an intrinsic
monitor (`synchronized`). In Java 21 a held monitor is a native frame the continuation
cannot peel off, so the carrier is stuck. Swap the intrinsic lock for a `ReentrantLock`,
whose `lock()`/`unlock()` is plain Java — parking inside it unmounts the carrier cleanly.

```java
import java.util.concurrent.locks.ReentrantLock;

private static void runOne(Arm arm, CountDownLatch arrived, CountDownLatch gate) {
    switch (arm) {
        case PINNING -> {
            ReentrantLock perTaskLock = new ReentrantLock();   // was: Object perTaskMonitor
            perTaskLock.lock();
            try {
                arrived.countDown();
                awaitGate(gate);                                // parks → carrier UNMOUNTS now
            } finally {
                perTaskLock.unlock();
            }
        }
        case NON_PINNING -> {
            arrived.countDown();
            awaitGate(gate);
        }
    }
}
```

With this change, parking at the gate unmounts the carrier, so all 10,000 virtual
threads reach the gate concurrently, `arrived` counts to zero, and the test passes.

### Scale up solution (Part B)
```java
public static long heldAcrossSleepWallClockMillis(int tasks) {
    // Run under -Djdk.virtualThreadScheduler.parallelism=1
    var sharedLock = new java.util.concurrent.locks.ReentrantLock();
    long start = System.nanoTime();
    try (var exec = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < tasks; i++) {
            exec.submit(() -> {
                sharedLock.lock();
                try { Thread.sleep(java.time.Duration.ofSeconds(1)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { sharedLock.unlock(); }
            });
        }
    } // close() awaits all tasks
    return (System.nanoTime() - start) / 1_000_000;
}
```

**Both arms print ~8 s for N=8 — but for opposite reasons:**

| Arm | Mechanism | Carrier during the 1 s sleep | `-Djdk.tracePinnedThreads` |
|-----|-----------|------------------------------|----------------------------|
| `synchronized (sharedLock) { sleep }` | **Pinning** | held the whole time (cannot unmount) | prints a pinned-thread stack trace |
| `lock.lock(); …sleep…; unlock()` (shared) | **Lock contention** | unmounts; the carrier runs other VTs between acquisitions | prints **nothing** |

The `synchronized` arm serializes because each carrier is pinned for the full second
(with one carrier, that is 8 × 1 s). The `ReentrantLock` arm serializes because the
*critical section contains the sleep* — only one VT can hold the lock at a time, so the
sleeps queue. Same wall-clock, but only the first arm ever consumed a carrier; the second
parked cleanly and the carrier was free between acquisitions. `tracePinnedThreads` is the
proof: it fires only for the pinned arm.

## Why this works (and the gotcha)
The scalability of virtual threads is **entirely** about the *park ⇒ unmount* move. A
blocking call that parks normally hands the carrier back so it can mount another virtual
thread. Pinning breaks that move: in Java 21, if the virtual thread is inside a
`synchronized` block (or a native frame) when it tries to park, the continuation cannot
be unmounted and the carrier is held hostage. The default scheduler has only
`availableProcessors()` carriers, so a flood of pinned virtual threads collapses to
near-serial throughput.

The trap has two layers:
1. **"`synchronized` always pins"** is wrong. Pinning only *manifests* when the virtual
   thread would otherwise unmount — i.e. when it actually parks/blocks inside the monitor.
   A `synchronized` block doing pure CPU work (e.g. `map.computeIfAbsent` with a CPU-only
   lambda) never parks, so nothing tries to unmount and the word "pinning" doesn't apply.
   The right question is *"which block attempts to unmount?"* — not *"which pins?"*
2. **"`ReentrantLock` = fast"** is wrong too (Part B). A lock held *across* a blocking
   call still serializes — by contention, not pinning. Identical wall-clock, opposite
   cause. The tell is `tracePinnedThreads`: it fires for the pinned arm only.

This is **version-specific**. JEP 491 (Java 24) reimplements `synchronized` so that
monitor-held parking no longer pins. The behavior drilled here is correct for **Java 21**.

## Common mistakes
- **Replacing `synchronized` with `synchronized` on a different object** → still pins;
  the object never mattered, the *construct* does.
- **Reciting "`synchronized` pins, `ReentrantLock` doesn't"** as a blanket rule → fails
  Part B, where the `ReentrantLock` arm serializes anyway (contention, not pinning).
- **Marking a CPU-only `synchronized` block as "slow/pinned"** → it never parks, so there
  is no unmount attempt and no pin.
- **Trying to "wait out" the starvation with a longer timeout** → carriers are pinned
  *forever* at the gate; no timeout helps. The latch never empties.
- **Leaning on `-Djdk.tracePinnedThreads` as the deliverable** → it is diagnostic proof,
  not the fix; the fix is the construct swap.

## Interviewer Q&A
**Q:** What is a carrier thread, and how many are there by default?
**A:** A carrier is a platform (OS) thread that *runs* a virtual thread when it is mounted.
The default scheduler is a `ForkJoinPool` sized to `Runtime.getRuntime().availableProcessors()`,
so by default there are CPU-count carriers (tunable via
`-Djdk.virtualThreadScheduler.parallelism`).

**Q:** What does "mount" and "unmount" mean?
**A:** Mounting copies the virtual thread's continuation onto a carrier's stack so it can
execute. Unmounting happens when the virtual thread parks: its stack is saved off-heap and
the carrier is freed to mount another virtual thread. That swap is what makes millions of
blocked virtual threads cheap.

**Q:** What is pinning, precisely?
**A:** Pinning is a Java 21 situation where a virtual thread *cannot unmount* even though it
parked, because it is inside a `synchronized` block (held monitor) or a native frame. The
carrier stays blocked for the duration, defeating scalability.

**Q:** Does every `synchronized` block pin?
**A:** No. Pinning only manifests if the virtual thread *parks/blocks while holding the
monitor*. A `synchronized` block that only does CPU work never parks, so there is no unmount
attempt and no pin. The precise question is "does this block attempt to unmount while
holding a monitor?"

**Q:** Both a `synchronized`+sleep arm and a `ReentrantLock`-held-across-sleep arm print
the same ~N seconds under one carrier. Are they the same bug?
**A:** No — opposite causes. The `synchronized` arm pins: one carrier is held the entire
sleep. The `ReentrantLock` arm has no pinning; it serializes because the critical section
*contains* the sleep, so it is lock contention. `-Djdk.tracePinnedThreads` fires only for
the `synchronized` arm.

**Q:** How do you fix pinning in Java 21?
**A:** Replace the `synchronized` block with a `ReentrantLock` (park-friendly), or restructure
so no blocking/parking call happens while a monitor is held. Diagnose with
`-Djdk.tracePinnedThreads=short|full`.

**Q:** Are virtual threads faster for CPU-bound work?
**A:** No. They win on *blocking I/O throughput* — many tasks that spend their time waiting.
CPU-bound work is still bounded by core count; virtual threads add no speedup there and may
add scheduling overhead.

**Q:** Why prefer one virtual thread per task over a bounded pool for I/O work?
**A:** Virtual threads are cheap and block harmlessly, so `newVirtualThreadPerTaskExecutor()`
removes pool-sizing guesswork: you stop multiplexing tasks onto a scarce pool and instead give
each task its own thread. The carrier pool underneath absorbs the actual parallelism.

**Q:** Will this pinning behavior persist in future Java?
**A:** No. JEP 491 (Java 24) makes `synchronized` no longer pin in the common case. The
pinning-on-`synchronized` behavior is specific to the Java 21 era and is what this exercise
drills.
