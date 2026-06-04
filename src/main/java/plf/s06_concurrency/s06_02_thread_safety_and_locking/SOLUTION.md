# Solution — The seat that booked twice

## Ideal solution
Hold the monitor across the entire compound action. Because intrinsic locks are
reentrant, the block can still call the already-`synchronized` `booked()` and
`add()`:

```java
public boolean tryBook() {
    synchronized (this) {
        if (booked() < capacity) {
            add();
            return true;
        }
        return false;
    }
}
```

Equivalent and slightly cleaner — mark the whole method `synchronized` and touch
the field directly (the per-step methods become unnecessary plumbing):

```java
public synchronized boolean tryBook() {
    if (booked < capacity) {
        booked++;
        return true;
    }
    return false;
}
```

Both lock on `this`, the same monitor `booked()` and `add()` already use, so the
check and the act are now indivisible with respect to every other access path.

## Why this works (and the gotcha)
`synchronized` gives you two guarantees on monitor enter/exit: **mutual
exclusion** (only one thread in the critical section) and a **happens-before
edge** (JLS 17.4.5 — the unlock by one thread happens-before the next lock, so
writes are visible). The original `booked()` and `add()` each get both
guarantees *for their own single operation*. That is exactly the trap: making
each step atomic does **not** make the *sequence* atomic.

The invariant you must protect is `booked <= capacity`, and it spans a read and a
write. In the broken version the lock is released between them, opening a window:

```
T1: booked()  -> 0   (lock, read, unlock)
T2: booked()  -> 0   (lock, read, unlock)   <-- T1 hasn't added yet
T1: add()            booked = 1
T2: add()            booked = 2              <-- overbooked
```

The test makes this **deterministic** rather than relying on luck: a
`CyclicBarrier(64)` releases all 64 threads at the same instant, so they pile
into the check-then-act window together. The broken code overbooks essentially
every round; the fixed code never does.

You don't guard a *method*, you guard an *invariant* — and the invariant is only
as safe as its leakiest access path. One unguarded or independently-locked
compound action anywhere and the synchronization elsewhere is theater.

## Common mistakes
- **"Every method is synchronized, so the class is thread-safe."** Per-method
  locking makes each call atomic, not any sequence of calls. Compound actions
  (check-then-act, read-modify-write, put-if-absent) need one lock for the whole
  compound.
- **Making `booked` `volatile` and dropping the lock.** `volatile` fixes
  visibility and gives `int`/`long` atomic loads/stores, but `if (booked <
  capacity) booked++` is still two operations with a gap — still overbooks.
- **`synchronized (new Object())` or locking on a different object inside
  `tryBook()`.** A fresh or different monitor excludes nobody; you must lock the
  **same** monitor (`this`) that the other accessors use.
- **Fear of deadlock from calling `booked()`/`add()` inside `synchronized
  (this)`.** Intrinsic locks are reentrant — the same thread re-acquiring its own
  monitor is free.
- **"Synchronized only gives mutual exclusion."** It also publishes writes
  (happens-before on unlock→lock). Dropping the lock loses *both* properties.

## Interviewer Q&A

**Q:** Both `booked()` and `add()` are `synchronized`. Why isn't `tryBook()`
thread-safe?
**A:** Each method is atomic on its own, but `tryBook()` releases the lock
between the check and the act. Two threads can both pass the check before either
adds, so they both book. Atomic steps don't compose into an atomic sequence.

**Q:** Define the invariant being violated and where it lives.
**A:** `booked <= capacity`. It spans a read of `booked` and a conditional write,
so the unit that must be guarded is the read-decide-write, not the individual
field accesses.

**Q:** Two guarantees `synchronized` provides?
**A:** Mutual exclusion (one thread in the critical section) and a happens-before
edge on monitor exit→enter, so writes made under the lock are visible to the next
holder (JLS 17.4.5).

**Q:** Would making `booked` `volatile` fix it?
**A:** No. `volatile` fixes visibility and gives the field atomic loads/stores,
but the check-then-act is still two operations with a gap. The compound action is
not atomic, so it still overbooks.

**Q:** You call `booked()` (which is `synchronized`) from inside `synchronized
(this)`. Doesn't the thread block on its own lock?
**A:** No — intrinsic locks are reentrant. A thread already holding `this` can
re-enter any block or method locked on `this`; the hold count just increments.

**Q:** Suppose someone adds another method that does a raw `add()` without going
through `tryBook()`. Is the class still safe?
**A:** No. The invariant is only as strong as its leakiest access path. A single
unguarded compound action (or an independently-locked one) elsewhere reintroduces
the race regardless of how careful `tryBook()` is. Thread safety is a property of
the whole synchronization policy, not one method.

**Q:** How did the test make a race deterministic instead of flaky?
**A:** A `CyclicBarrier(64)` blocks every thread until all 64 arrive, then
releases them together so they enter the check-then-act window simultaneously.
That forces the interleaving every round rather than hoping the scheduler hits
it. No `Thread.sleep`, so it passes or fails for the right reason every run.

**Q:** When would you reach for `AtomicInteger` or `ReentrantLock` instead?
**A:** `AtomicInteger` (CAS / `compareAndSet` in a loop) when the compound is a
single-variable read-modify-write and you want lock-free throughput.
`ReentrantLock` when you need `tryLock`, timed/interruptible acquisition,
fairness, or multiple `Condition`s. For a plain "guard this small invariant with
one monitor", intrinsic `synchronized` is the simplest correct tool.
