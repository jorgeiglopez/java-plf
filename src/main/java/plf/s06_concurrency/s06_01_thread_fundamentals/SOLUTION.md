# Solution — Three threads, one lie

## Ideal solution

The three predictions that turn the test green:

```java
static final Thread.State PREDICTED_WAITER    = Thread.State.WAITING;        // monitor.wait(0L)
static final Thread.State PREDICTED_CONTENDER  = Thread.State.BLOCKED;        // contending for a held monitor
static final Thread.State PREDICTED_SLEEPER    = Thread.State.TIMED_WAITING;  // Thread.sleep(60_000)
```

| Thread     | What it's doing                              | State           |
|------------|----------------------------------------------|-----------------|
| waiter     | inside `monitor.wait(0L)`                     | `WAITING`       |
| contender  | stuck at `synchronized (monitor)`, monitor held by another | `BLOCKED` |
| sleeper    | inside `Thread.sleep(60_000)`                | `TIMED_WAITING` |

## Why this works (and the gotcha)

**The lie is `wait(0L)`.** The smug read is "there's a timeout argument, therefore `TIMED_WAITING`." Wrong. The `Object.wait(long)` contract makes `0` the *no-timeout sentinel*: a timeout of zero means **wait forever**, exactly like the no-arg `wait()`. So the waiter is in plain `WAITING`. The thread that *looks* timed isn't, and the genuinely timed thread is the unrelated `Thread.sleep(60_000)` sleeper, which reports `TIMED_WAITING`. The two threads swap the state you'd assign at a glance.

**WAITING vs BLOCKED is the core distinction.** Both the waiter and the contender are "stopped near the same monitor," but they mean opposite things:

- `WAITING` (the waiter) — the thread *owned* the monitor, called `wait()`, and in doing so **released** it. It is now parked on a *condition*, waiting to be `notify()`-ed. It is not competing for any lock.
- `BLOCKED` (the contender) — the thread does **not** own the monitor and is parked trying to **acquire** it because someone else holds it. The moment the holder releases, the contender can proceed.

So the state tells you *what the thread is waiting for*: `WAITING` = waiting for a signal on a condition it chose to wait on; `BLOCKED` = waiting for a lock it needs to enter a `synchronized` region.

**Why the harness is deterministic** (and not a `sleep`-based race): each thread is driven into its target state before any state is read.

- The waiter signals `waiterInside` *inside* the `synchronized` block, then calls `wait(0L)`. The harness then spins until `waiter.getState() == WAITING`. A thread reaching `WAITING` from `wait()` has provably released the monitor — that release is the happens-before edge that lets the next thread acquire it.
- Only then does the blocker start; it acquires `monitor` (now free) and parks *while still holding it* (it waits on a different object, `blockerPark`, which does not release `monitor`). It signals `blockerHasLock`.
- The contender then starts and runs straight into `synchronized (monitor)`; since the blocker holds `monitor`, the contender is forced to `BLOCKED`. The harness spins until that is true.
- The sleeper signals, the harness spins until `TIMED_WAITING`.

No `Thread.sleep` appears in any assertion path, and the read of the sleeper happens during a 60-second sleep window, so it cannot have spuriously woken to `RUNNABLE`.

## Common mistakes

- **`wait(0)` → `TIMED_WAITING`.** The classic trap. Zero is "no timeout," so it's `WAITING`. Only a *positive* argument (`wait(50)`, `sleep(50)`, `join(50)`, `parkNanos`) yields `TIMED_WAITING`.
- **waiter → `BLOCKED`.** Assuming "it's stopped on a monitor, so it's blocked." A thread in `wait()` has released the monitor; it is not contending for any lock. `BLOCKED` is reserved for *acquiring* a monitor.
- **contender → `WAITING`.** Confusing "waiting around" (English) with `WAITING` (the JLS state). Contending to *enter* a `synchronized` block is `BLOCKED`, full stop.
- **Reading a sleeper's state with a tight `sleep(1)`.** A near-instant sleep can wake to `RUNNABLE` between the call and the read, making the assertion flaky. Use a duration far longer than the observation window.

## Interviewer Q&A

**Q:** Name the thread states and what each means.
**A:** `NEW` (created, not started), `RUNNABLE` (running or runnable, possibly waiting on the OS/IO), `BLOCKED` (waiting to acquire a monitor lock), `WAITING` (parked indefinitely via `wait()`/`join()`/`LockSupport.park()`), `TIMED_WAITING` (parked with a timeout via `sleep`, `wait(t)`, `join(t)`, `parkNanos`), `TERMINATED` (run finished).

**Q:** A thread sits inside `synchronized` calling `obj.wait()`. Is it `BLOCKED` or `WAITING`, and does it hold the lock?
**A:** `WAITING`. `wait()` atomically releases the monitor and parks the thread on the object's wait set, so it holds nothing. `BLOCKED` is for a thread trying to *acquire* a monitor it doesn't own.

**Q:** What state is a thread in during `obj.wait(0)`? Why does that surprise people?
**A:** `WAITING`. People see the `long` argument and assume `TIMED_WAITING`, but `0` is the contract's "wait forever" sentinel — identical to the no-arg `wait()`. Only a positive timeout gives `TIMED_WAITING`.

**Q:** Two threads are near the same monitor — one `WAITING`, one `BLOCKED`. What does each tell you operationally?
**A:** The `WAITING` thread owned the lock, called `wait()`, released it, and now needs a `notify`/`notifyAll` to make progress. The `BLOCKED` thread needs the lock and will proceed as soon as the current owner releases it. So `WAITING` signals a condition problem (nobody's signalling), `BLOCKED` signals contention.

**Q:** Is `RUNNABLE` the same as "currently executing on a CPU"?
**A:** No. `RUNNABLE` means runnable from the JVM's point of view — it may be on-CPU, waiting for a CPU, or blocked in a native/OS call like socket I/O. The JVM can't distinguish "running" from "blocked in the kernel," so both show as `RUNNABLE`.

**Q:** `start()` vs `run()`?
**A:** `start()` registers the thread with the scheduler and invokes `run()` on a *new* thread of execution. Calling `run()` directly just executes the body synchronously on the current thread — no new thread is created. Also, `start()` may be called at most once; a second call throws `IllegalThreadStateException`.

**Q:** `Runnable` vs `Callable`?
**A:** `Runnable.run()` returns `void` and can't throw checked exceptions. `Callable<V>.call()` returns a value and may throw checked exceptions. `Callable` needs an executor or a `FutureTask` to run and surface its result via `Future.get()`; a raw `Thread` only takes a `Runnable`.

**Q:** How would you make a "what state is this thread in" check deterministic in a test, without `Thread.sleep` races?
**A:** Use explicit coordination (latches/barriers) to confirm each thread has *entered* its target region, then bounded-spin on `getState()` until it equals the target. For monitor states, exploit happens-before: a thread reaching `WAITING` from `wait()` has provably released the monitor, so the next acquirer's success is your ordering proof. Keep any timed wait far longer than the observation window so the read can't catch it awake.
