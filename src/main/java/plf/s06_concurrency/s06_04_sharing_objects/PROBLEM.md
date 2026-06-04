# Sharing Objects Safely — `volatile` Buys One Guarantee, Not Two

**Concept:** `volatile` guarantees *visibility and ordering*, but **not atomicity of compound actions**. Same keyword, two fields, opposite outcomes.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

`SharedCounter` has two `volatile` fields. One — a `stop` flag spun on by a worker
thread — is **correct**: drop the `volatile` and the worker can spin forever, because
the JIT is free to hoist a non-volatile read out of the loop. The other — an `int count`
that two threads each `count++` a million times — looks just as protected. It compiles,
it's `volatile`, it must add up to 2,000,000... right?

Before you run anything, **commit to a prediction** for each:

1. With `volatile boolean stop`, does `spinUntilStopped()` terminate once another thread calls `requestStop()`? (yes / no)
2. With `volatile int count`, after two threads each do `count++` 1,000,000 times and join, what is the final `count`? (write a number)

Write both answers down. Then run the tests and feel the gap.

## Task

1. Read `SharedCounter.java`. Predict the two answers above.
2. Run the tests. `volatileFlagMakesSpinnerTerminate` passes — the flag works.
   `twoThreadsIncrementWithoutLostUpdates` is **red**: the total comes up far short of
   2,000,000 (routinely ~1.1M–1.4M on a server JIT — nearly half the increments vanish).
3. **Fix the production code** in `SharedCounter` (not the test) so the increment total
   is exactly 2,000,000 every run, while keeping the `stop` flag's behaviour and its
   visibility-only policy intact.
4. In one sentence each, state in `SOLUTION.md` terms: *why is `volatile` the correct,
   complete fix for `stop`, yet insufficient for `count`?*

## Constraints

- Java 21, no external libraries.
- Do **not** modify the test file.
- Do **not** weaken the `stop` flag (it must stay correct and stay visibility-only).
- The fix for `count` must guarantee atomicity of the increment, not merely visibility.

## How to verify

```
mvn -q -Dtest=SharedCounterTest test
```

Both tests green, and the increment test stays green across repeated runs.

<details><summary>Hint (open only if stuck)</summary>
`count++` is three bytecodes: `getfield`, `iadd`, `putfield`. `volatile` makes each of
the read and the write visible, but two threads can interleave *between* the read and
the write — both read 41, both write 42, one increment is lost. You need the whole
read-modify-write to be one atomic step. Reach for `java.util.concurrent.atomic`, or a
lock around the compound action.
</details>
