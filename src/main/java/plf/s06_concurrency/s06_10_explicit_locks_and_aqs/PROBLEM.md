# Explicit Locks and AQS — The signal that woke the wrong thread

**Concept:** a parked thread's wake is never proof its predicate holds — not after a spurious wakeup, not after another thread barges in, and not when one `signal()` targets a shared condition queue holding the wrong kind of waiter. You re-check in a `while` loop, and you give producers and consumers **separate** `Condition`s.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
A bounded buffer guarded by a single `ReentrantLock` is the canonical `Condition`
exercise. Almost everyone writes the wait as `if (count == 0) cond.await();` with a
lone shared `Condition` and a tidy `cond.signal()`. It looks correct and passes
naive tests. Then a producer's `signal()` wakes a *consumer* who can't proceed — or
wakes a consumer that, having only branched once with `if`, charges ahead and drains
an item that was never there.

This is the lost-wakeup family of bugs, and it is fully deterministic once you control
the interleaving. Can you implement the buffer so it survives it?

## Task
Edit `BoundedBuffer.java` — implement the two stubbed methods:

1. **`put(int value)`** — block while the buffer is full, then store the value and
   wake a waiting consumer.
2. **`take()`** — block while the buffer is empty, then remove and return a value and
   wake a waiting producer.

The scaffold already gives you `notFull` and `notEmpty` conditions and the ring-buffer
fields. Use them. Make `BoundedBufferTest` pass.

Then **run `BuggyBoundedBufferDemoTest`** and read `BuggyBoundedBuffer.java`: it is the
tempting wrong version (one shared `Condition`, `if`, `signal`). The demo test proves,
deterministically, that after producing **one** item and letting **two** consumers
race through, the buffer's `count` ends at **-1** — two consumers drained a single
item. Explain in one sentence to yourself why.

## Constraints
- Java 21, no external libraries.
- Do **not** modify the test files or the test-coordination probes
  (`consumersWaiting()`, `size()`).
- The waiting condition must be a `while` loop, not an `if`.

## Scale up (Optional)
1. **`tryLock` / fairness reflection (no code):** would constructing the lock as
   `new ReentrantLock(true)` (fair) save an `if`-based version? Answer yes/no and why.
2. **`ReentrantReadWriteLock`:** sketch when a read-write lock helps and when it hurts
   versus a plain `ReentrantLock`.
3. **`StampedLock`:** name the two landmines that bite people who reach for
   `tryOptimisticRead()` (hint: reentrancy, and the order of `validate`).

## How to verify
```
mvn -q -Dtest=BoundedBufferTest test          # your implementation: must go green
mvn -q -Dtest=BuggyBoundedBufferDemoTest test # the trap, demonstrated: stays green (it asserts the bug)
```

<details><summary>Hint (open only if stuck)</summary>
Two failures stack on the buggy version. (1) `if` re-checks the predicate zero times
after waking, so a spuriously- or wrongly-woken thread proceeds on a false predicate —
use `while`. (2) A single `Condition` mixes producers and consumers in one wait queue,
so `signal()` can wake the wrong type; either call `signalAll()` on the shared
condition, or — better — keep `notFull`/`notEmpty` separate and signal the right one.
The correct buffer needs **both** fixes.
</details>
