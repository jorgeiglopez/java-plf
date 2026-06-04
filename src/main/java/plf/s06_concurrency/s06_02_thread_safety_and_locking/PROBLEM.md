# Thread Safety and Locking — The seat that booked twice

**Concept:** locking each *step* does not lock the *sequence* — a check-then-act compound action needs ONE lock held across the whole thing.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
`SeatBooking` looks airtight. Every method that touches state is `synchronized`,
so each individual read and write is mutually exclusive and visible across
threads. Yet under load it cheerfully books seat #2 in a one-seat venue. The bug
is not in `booked()` or `add()` — both are correct in isolation. It lives in
`tryBook()`, which strings them together:

```java
if (booked() < capacity) {   // lock acquired, released
    add();                    // lock acquired again, released
}
```

Between the `booked()` read and the `add()` write the lock is **released**.
Two threads can both read `booked == 0`, both decide "there's room", and both
book. The invariant `booked <= capacity` is the thing you must guard — not any
single method.

## Task
1. Run the provided test `SeatBookingTest`. It is **red**: 64 threads released
   simultaneously by a `CyclicBarrier` all run `tryBook()` against a 1-seat
   venue, and `booked` ends up at 2 or more.
2. Fix **only** `tryBook()` in `SeatBooking.java` so the compound check-then-act
   executes atomically. The whole `if`-test-and-`add` must happen while holding
   the **same** monitor that `booked()` and `add()` use.
3. Do not weaken `booked()` or `add()`, do not change the test, and do not
   introduce `AtomicInteger` or any `java.util.concurrent` lock — this exercise
   is about intrinsic locks.

## Constraints
- Java 21, no external libraries.
- `synchronized` only (no `AtomicInteger`, no `ReentrantLock`).
- Keep `tryBook()`'s signature and behavior: return `true` if it booked a seat,
  `false` if the venue was full.

## How to verify
```
mvn -q -Dtest=SeatBookingTest test
```
Green means `booked` never exceeded capacity across all 200 rounds.

<details><summary>Hint (open only if stuck)</summary>
The two synchronized methods both lock on <code>this</code>. <code>synchronized</code> is
<em>reentrant</em>, so a <code>synchronized (this) { ... }</code> block in
<code>tryBook()</code> can call <code>booked()</code> and <code>add()</code> without
deadlocking — and now no other thread can slip in between the check and the act.
</details>
