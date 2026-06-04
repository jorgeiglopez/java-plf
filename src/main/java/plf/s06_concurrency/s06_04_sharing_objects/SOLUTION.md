# Solution — `volatile` Buys One Guarantee, Not Two

## Predictions (the aha)

1. **stop flag:** *yes*, `spinUntilStopped()` terminates. `volatile` gives the writing
   thread's `stop = true` a happens-before edge to the spinner's next read, and forbids
   the JIT from hoisting the read out of the loop. (Remove `volatile` and the answer
   flips to *no* — the loop can become `if (!stop) while (true) {}` and never sees the update.)
2. **count:** *not* 2,000,000. You get a number well short of it — lost updates. The
   tempting wrong answer is "2,000,000, because it's volatile."

## Ideal solution

Make the increment atomic. `volatile` on the field is now redundant for `count`
(`AtomicInteger` and locks supply their own happens-before), so drop it.

### Option A — `AtomicInteger` (lock-free, preferred for a counter)

```java
import java.util.concurrent.atomic.AtomicInteger;

private final AtomicInteger count = new AtomicInteger(0);

public int count() {
    return count.get();
}

public void incrementManyTimes(int times) {
    for (int i = 0; i < times; i++) {
        count.incrementAndGet();   // one atomic CAS loop, not three separate bytecodes
    }
}
```

### Option B — intrinsic lock around the compound action

```java
private int count = 0;   // no volatile: the lock supplies visibility AND atomicity

public synchronized int count() {
    return count;
}

public synchronized void incrementManyTimes(int times) {
    for (int i = 0; i < times; i++) {
        count++;
    }
}
```

Both are correct. `AtomicInteger.incrementAndGet()` is a non-blocking `getAndAddInt`
CAS loop on `Unsafe`; the `synchronized` version serializes the whole read-modify-write
behind the monitor. Note **once the lock is in place, `volatile` is no longer needed** —
unlocking a monitor flushes, locking it invalidates, so the lock already gives you the
visibility `volatile` was providing. Adding `volatile` back wouldn't be wrong, just
redundant.

The `stop` flag is left **exactly as it was** — `volatile boolean`, single writer,
visibility-only. That is the correct *and complete* fix for it; do not "upgrade" it to
a lock or an `AtomicBoolean`. There is no compound action on `stop`.

## Why this works (and the gotcha)

`count++` is not one operation. It compiles to:

```
getfield count    // read
iadd              // +1
putfield count    // write
```

`volatile` (JLS §17.4.5) puts a happens-before edge on each *individual* read and each
*individual* write — so every thread sees the latest value at the moment it reads. What
`volatile` does **not** do is make the read-increment-write *indivisible*. Two threads
both `getfield` 41, both `iadd` to 42, both `putfield` 42 — one increment evaporated.
Visibility was never the problem; **atomicity of the compound action** is.

The `stop` flag, by contrast, has a single writer and only ever does a plain assignment
(`stop = true`) and plain reads. There is no read-modify-write to tear, so visibility is
the *entire* requirement, and `volatile` delivers exactly that. Same keyword, two fields:
it fully solves one and only half-solves the other.

**One-sentence objective:** *visibility and atomicity are different guarantees, and
`volatile` buys only the first.*

### Why the test is deterministic

The two worker threads line up on a `CyclicBarrier` and start their tight increment
loops at the same instant, then each does 1,000,000 volatile read-modify-writes. Every
iteration is a real volatile load + store with a memory barrier the JIT cannot register-
cache or elide, so the interleaving window is wide and stays open for the whole run —
the loss is large and stable (no `Thread.sleep`, no I/O, no luck). The fixed version,
using an atomic or a lock, lands on exactly 2,000,000 every run. Pass and fail both
happen for the right reason, every time.

### Synchronization policy (document it — JCiP `@GuardedBy`)

```
stop  : visibility-only, single writer  -> volatile        (NOT a lock, NOT atomic)
count : compound action (read-modify-write) -> AtomicInteger  (or @GuardedBy("this") + synchronized)
```

State plainly in the class: `volatile` is in the policy's vocabulary for the flag and
**not** for the counter.

## Common mistakes

- **"It's `volatile`, so `count++` is thread-safe."** → Lost updates; `volatile` is
  visibility + ordering, never atomicity of a compound action.
- **Trying to "fix" `count` by adding *more* `volatile`** (e.g. on the loop variable) →
  Does nothing; the RMW is still three steps.
- **"Upgrading" the `stop` flag to a lock or `AtomicBoolean`** → Over-synchronization;
  it was already correct and complete. You also risk a reviewer thinking compound logic
  exists where none does.
- **Leaving `volatile` on `count` after adding a lock and assuming you *must*** → It's
  merely redundant once the lock supplies happens-before; not a bug, but say why.
- **Putting a `System.out.println` inside the spin loop while debugging** → The println
  inserts a memory barrier and can mask the visibility bug entirely; never diagnose a
  hoist with I/O in the loop.

## Interviewer Q&A

**Q:** What two guarantees does `volatile` give, and which does it not?
**A:** It gives *visibility* (a write is seen by subsequent reads on other threads) and
*ordering* (no reordering across the access; happens-before on the volatile write→read).
It does **not** give *atomicity* of compound actions like `x++`.

**Q:** Why does removing `volatile` from the `stop` flag let the worker loop forever?
**A:** A non-volatile read can be hoisted out of the loop by the JIT (the value is loop-
invariant as far as the optimizer can prove), so `while (!stop) {}` effectively becomes
`if (!stop) while (true) {}`. There's also no happens-before edge, so the writer's update
may never become visible. `volatile` forbids the hoist and supplies the edge.

**Q:** Walk me through *exactly* how an increment is lost on a `volatile int`.
**A:** `count++` is `getfield`/`iadd`/`putfield`. Thread A reads 41, thread B reads 41
before A writes, both compute 42, both write 42. Two increments, one net change — one
lost. Each access was visible; the compound action was not atomic.

**Q:** Give two correct fixes for the counter and their trade-offs.
**A:** `AtomicInteger.incrementAndGet()` — lock-free CAS loop, scales better under
contention, no monitor. Or `synchronized` around the increment — simpler, serializes the
whole RMW, but blocks. For a single hot counter, the atomic (or `LongAdder` under heavy
contention) is usually preferred.

**Q:** After you wrap `count` in a `synchronized` method, do you still need `volatile`
on it? Why?
**A:** No. Monitor exit happens-before a subsequent monitor entry, so the lock already
provides the visibility `volatile` gave. The `volatile` becomes redundant (harmless, but
unnecessary).

**Q:** Why is it wrong to "harden" the `stop` flag into an `AtomicBoolean` or a lock?
**A:** There's no compound action on it — single writer, plain assignment and reads.
Visibility is the whole requirement and `volatile` already delivers it. Adding a lock is
over-synchronization: more cost, more contention, no correctness gain, and it muddies the
documented policy.

**Q:** What's "safe publication" and how does it relate here?
**A:** Safe publication is making an object visible to other threads such that they see a
fully-constructed, correctly-initialized state. Mechanisms: a `static` initializer,
a `volatile`/`final`/`AtomicReference` field, or a thread-safe collection
(e.g. `ConcurrentHashMap`). Publishing a reference through a plain non-volatile field is a
data race and another thread may see a stale or partially-constructed object. The `stop`
flag uses one such mechanism (`volatile`) for its single value.

**Q:** Is an object with all `final` fields automatically safe to share without
synchronization?
**A:** Only if `this` did not *escape* during construction. JLS §17.5 freezes `final`
fields at the end of the constructor, so a reference published *after* construction sees
correct values without synchronization. But if the constructor leaks `this` (registers a
listener, starts a thread, stores itself in a static) another thread can observe the
object before the freeze and see default/garbage values. Also, `final` on an array or
collection reference protects only the reference, not the elements.
