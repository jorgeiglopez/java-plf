# Thread Fundamentals — Three threads, one lie

**Concept:** `Thread.State` distinguishes *waiting on a condition* (`WAITING`) from *contending for a monitor lock* (`BLOCKED`) — and `wait(0)` does **not** mean a timed wait.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You can name the six thread states. Fine. But can you *predict* them? Three live threads are parked in this harness, and a naive read of the source will get at least one wrong:

- the **waiter** is inside `monitor.wait(0L)`,
- the **contender** is stuck at `synchronized (monitor) { ... }` while another thread holds that monitor,
- the **sleeper** is inside `Thread.sleep(60_000)`.

Two of these look like they share a state. They don't. And one of them looks *timed* but isn't. Commit to your answers before you run anything — the gut-punch only lands if you've written the wrong guess down first.

## Task

1. Read `StateObservatory.java`. Trace what each named thread (`waiter`, `contender`, `sleeper`) is doing at the moment its state is read.
2. Open the test `StateObservatoryTest.java`. Fill in the three `PREDICTED_*` constants with the `Thread.State` you believe each thread is in. **Do this before running.**
3. Run the test. If a prediction is wrong, the assertion fails and tells you the truth. Fix your prediction to match reality.
4. In one sentence each, explain *why* the waiter and the sleeper are in the states they're in — specifically why `wait(0L)` lands where it does.

## Constraints

- Java 21, no external libraries.
- Edit **only** the three `PREDICTED_*` constants in the test. Do not touch `StateObservatory.java` or the assertions.
- The harness is fully deterministic — every thread is driven into its target state before any state is read. Run it as many times as you like; the answer never changes.

## How to verify

```
mvn -q -Dtest=StateObservatoryTest test
```

Green means your three predictions match the live JVM.

<details><summary>Hint (open only if stuck)</summary>
Two questions decide everything. (1) When a thread is parked inside <code>Object.wait(...)</code>, has it released the monitor, or is it holding it? (2) Read the Javadoc contract for <code>Object.wait(long)</code>: what does a timeout argument of <code>0</code> mean?
</details>
