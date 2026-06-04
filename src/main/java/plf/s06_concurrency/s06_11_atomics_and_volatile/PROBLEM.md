# Atomics & Volatile — Does `final` rescue double-checked locking?

**Concept:** `volatile` publishes the *reference*; `final` publishes the *fields* — and a data race on the reference voids BOTH, so DCL needs `volatile` no matter what.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
Two cargo cults collide in double-checked locking (DCL). One says "just add `volatile`." The other says "the field is `final`, so the constructed value is safe — `volatile` is overkill." The senior question is which cult, if either, is right, and *which happens-before edge* each keyword actually buys you. This exercise makes you name both edges and prove that the data race defeats them.

There is also a 90-second warm-up that earns you the word *publish*: `volatile` makes a write **visible**, but it does **not** make `count++` **atomic**. Get that under your fingers first, then we go after DCL.

## Task

### Part 1 — Warm-up: volatile is not atomic (turn the test green)
Open `Counter.java`.

1. `volatileIncrement()` does `volatileCount++` on a `volatile int`. Convince yourself this still loses updates under contention: `count++` is read-modify-write, three separate actions, and `volatile` orders/​publishes each one but does not fuse them.
2. Implement `atomicIncrement()` so the read-modify-write is genuinely atomic, backed by the provided `AtomicInteger atomicCount`. Return the new value.
3. Turn `CounterTest` green: 8 threads × 100 000 increments must total exactly 800 000 with zero lost updates.

### Part 2 — The DCL trap (written analysis, no code to compile)
Study this lazy singleton. `Resource` has a `final` field, and `instance` is **not** `volatile`:

```java
class Resource {
    final int x;
    final int[] data;
    Resource() { x = 42; data = new int[]{ 7 }; }
}

class Holder {
    private static Resource instance;            // NOT volatile

    static Resource get() {
        if (instance == null) {                  // (1) racy read, no lock
            synchronized (Holder.class) {
                if (instance == null) {
                    instance = new Resource();   // (2) publish reference
                }
            }
        }
        return instance;                         // (3) reader uses it
    }
}
```

Answer these in `ANSWERS.md` (your own file):

1. A thread that takes the fast path at (1) and sees `instance != null` then reads `r.x`. **Can it legally read `x == 0`?** Yes or no, and cite the rule.
2. `x` is `final`. Spell out *exactly* what the JLS 17.5 final-field freeze guarantees, and the **one precondition** under which that guarantee holds. Does the fast-path read meet that precondition?
3. `data` is also `final`. A teammate says "so `data[0] == 7` is safe too." Is the reference `data` safe? Is the element `data[0]` safe? Treat them separately.
4. State the **two distinct happens-before edges** at play: which keyword orders the **reference publication**, and which orders the **constructor writes**. Why does fixing DCL require `volatile` regardless of `final`?
5. Closing gotcha: replace `Resource instance` with `AtomicReference<Resource>` and use `compareAndSet`. Does it fix DCL? If so, is it the `compareAndSet` (the CAS) that saves you, or something else? Name the actual mechanism.

## Constraints
- Java 21, no external libraries.
- Do **not** modify `CounterTest.java`.
- Part 2 is reasoning only — do not try to make the broken DCL "flaky-repro" in code; a real JVM almost never triggers it, which is exactly the trap. The proof is the memory model, not a lucky run.

## How to verify
- Part 1: `mvn -q -Dtest=CounterTest test` (green when `atomicIncrement` is correct).
- Part 2: compare your `ANSWERS.md` against `SOLUTION.md`. You must name **both** edges and correctly answer "no, `final` does not save DCL."

<details><summary>Hint (open only if stuck)</summary>
The freeze in JLS 17.5 guarantees the final field's constructed value only to a thread that reads the reference through a *correctly published* path. A racy read of a non-volatile static is by definition **not** that path — so seeing `instance != null` carries no ordering for the writes that preceded it. `volatile` on `instance` is what supplies the release/acquire that orders write-of-fields before write-of-reference.
</details>
