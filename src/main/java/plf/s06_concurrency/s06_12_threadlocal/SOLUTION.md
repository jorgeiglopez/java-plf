# Solution — The customer who logged in as someone else

## Ideal solution

```java
public static String handleLoginSafely(ExecutorService pool, String userId) {
    Future<String> f = pool.submit(() -> {
        CURRENT_USER.set(userId);
        try {
            return CURRENT_USER.get();
        } finally {
            CURRENT_USER.remove();
        }
    });
    return await(f);
}
```

The single load-bearing change is `CURRENT_USER.remove()` in a `finally`. `remove()` deletes the
entry from *this thread's* `ThreadLocalMap`, so the next access on the reused thread finds no
value and re-runs the `withInitial` supplier — handing the anonymous request `"anonymous"` again.

`finally` is mandatory, not stylistic: if the task body throws, the value must still be scrubbed
before the thread is handed back to the pool, or the leak persists.

## Why this works (and the gotcha)

A `ThreadLocal` does not store anything itself. The value lives in a `ThreadLocalMap` hanging off
the `Thread` object (`Thread.threadLocals`), keyed by the `ThreadLocal` instance. The map belongs
to the **thread**, so its contents outlive any single task and are shared by every task that runs
on that thread.

The trap is `withInitial`. The supplier is **not** a per-task reset. It is a lazy, per-*thread*
seed: it runs exactly once per thread — on the first `get()` that finds **no entry** for that key.
Walk the timeline on the single pooled worker:

1. Request A: `set("alice")` writes an entry → map now holds `alice`.
2. Request A returns. No `remove()`. The thread goes back to the pool **still holding `alice`**.
3. Request B runs on the **same** thread, calls `get()`. The map already has a value, so the
   supplier never fires. B reads `"alice"`.

Bob's anonymous request just executed as Alice. That is a real authorization incident produced by
three lines of innocent-looking code — no profiler, no heap dump, visible in a return value.

The reason `"anonymous"` *feels* right is the false mental model "`withInitial` = default on every
access." It isn't. `remove()` is the only thing that re-arms the seed, which is exactly why a
pooled `ThreadLocal` must be cleared in a `finally` on the way out of every task.

(Aside — the weak-key half of the story: the map keys `ThreadLocal` instances by *weak* reference
but holds *values* strongly. So even setting the `ThreadLocal` itself unreachable does not free the
value until the next `get/set/remove` on that thread happens to clear the stale slot — and if the
`ThreadLocal` is `static` the key is never collected at all. That is the memory-leak flavor of the
same bug; here the visible symptom is identity leakage, but both have the same cure: `remove()`.)

## Common mistakes

- **"`withInitial` resets per access, so B sees `anonymous`."** No. The supplier runs once per
  thread, only when the entry is absent. A reused thread already has the entry.
- **"The weak key gets GC'd, so there's no leak."** The *key* is weak; the *value* is strong. The
  value survives as long as the thread does — and pool threads never die.
- **`remove()` without `finally`.** If the task throws, the value is never cleared and the next
  task on that thread inherits it. Cleanup must be in `finally`.
- **Clearing at the start of B instead of the end of A.** Fragile: it assumes every future task
  remembers to scrub before reading. The owner that set the value must clean it up.
- **"Just don't reuse threads / add more threads."** Pooling is the whole point of an executor;
  per-task threads or virtual threads dodge *this* instance but the discipline (`set`/`remove`
  paired) is what's being tested. Prefer `ScopedValue` (s06_16) when on virtual threads.

## Interviewer Q&A

**Q:** Where does a `ThreadLocal` actually store its value?
**A:** In a `ThreadLocalMap` on the `Thread` object (`Thread.threadLocals`), keyed by the
`ThreadLocal` instance. The `ThreadLocal` object is just the key/handle; the storage is per-thread.

**Q:** With `ThreadLocal.withInitial(...)`, when does the supplier run?
**A:** Lazily, once per thread — on the first `get()` that finds no entry for that key on that
thread. It is a per-thread seed, not a per-task or per-call reset. After a value exists it never
fires again until `remove()` clears the entry.

**Q:** Pool of one thread: task A does `set("alice")` and returns without `remove()`; task B only
reads `get()`. What does B see and why?
**A:** `"alice"`. Same worker thread, map still holds the entry, so the supplier is skipped and B
inherits A's value — a cross-task identity leak.

**Q:** Why is `remove()` in a `finally` the fix, and why `finally` specifically?
**A:** `remove()` deletes the thread's entry so the next access re-seeds via the initializer.
`finally` guarantees the scrub even when the task body throws, so a poisoned value is never handed
to the next task on that thread.

**Q:** The map keys are weak references. Doesn't that prevent leaks on its own?
**A:** No. Keys are weak but values are strong. A cleared (stale) key leaves the value reachable
through the live thread until a later `get/set/remove` happens to purge the slot. Static
`ThreadLocal`s never have their key collected, so the value lives as long as the thread does.

**Q:** What's the memory-leak version of this in a real pool?
**A:** Workers never die, so any value left in their `threadLocals` (e.g. a `ThreadLocal<byte[]>`)
stays strongly reachable through the live thread for the life of the pool. Submitting many tasks
that `set` without `remove` causes steady heap retention / OOM. Same root cause, same fix.

**Q:** Does `InheritableThreadLocal` help here?
**A:** No — it copies values from the *creating* thread to *child* threads at thread creation. Pool
workers are created once and reused, so tasks don't inherit anything per-submit; it doesn't address
cross-task leakage on a reused thread.

**Q:** Modern alternative on virtual threads?
**A:** `ScopedValue` (s06_16). It's immutable and bound to a dynamic scope (`ScopedValue.where(...)
.run(...)`), so the binding is automatically torn down at scope exit — no manual `remove()`, no
leak, and it's cheap to inherit in structured concurrency.
