# Solution — The `join()` you forgot

## Answers

### Snippet A — failure path

`lookup()` propagates an **`ExecutionException`** whose cause is the
`RuntimeException("boom")` thrown by `fetchUser`. The `join()` runs (so the precondition is
satisfied); then `throwIfFailed()` sees a failed subtask and rethrows its exception wrapped
in `ExecutionException`. Because the scope is `ShutdownOnFailure`, the first failure already
*interrupted* the slow `fetchOrder` sibling, and `close()` (the implicit try-with-resources
exit) blocked until that interrupted thread actually terminated. Nothing leaked.

```java
String lookup() throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<String> user  = scope.fork(this::fetchUser);
        Subtask<String> order = scope.fork(this::fetchOrder);
        scope.join();              // barrier: waits for all, establishes happens-before
        scope.throwIfFailed();     // rethrows boom wrapped in ExecutionException
        return user.get() + "#" + order.get();   // unreached
    }
}
```

### Snippet B — the trap

**`throwIfFailed()` throws `IllegalStateException`** — at that line — *even though both
subtasks succeeded.* `join()` was never called. `throwIfFailed()`'s precondition is "the
scope has been joined," not "a subtask failed." No join, no result — success or failure
alike. The result is **never** `"alice#42"`; `user.get()` is never reached.

The fix is one line:

```java
String lookup() throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<String> user  = scope.fork(this::fetchUser);
        Subtask<String> order = scope.fork(this::fetchOrder);
        scope.join();              // <-- THE missing barrier
        scope.throwIfFailed();     // now legal: no failures, returns normally
        return user.get() + "#" + order.get();   // "alice#42"
    }
}
```

### Snippet C — read-before-join

**`user.get()` throws `IllegalStateException`**, despite the subtask having completed
successfully with `"alice"`. `Subtask.get()` requires that (1) the owning scope has been
`join()`-ed and (2) the subtask's state is `SUCCESS`. Condition (2) holds, but (1) does not
— `join()` was never called — so the precondition is violated. Same lesson as B from a
different angle: the gate is the *join*, not the subtask's success.

## Why this works (and the gotcha)

`StructuredTaskScope` is a small **state machine**, not a bag of `Future`s. `fork()` only
schedules; it does not let the owner thread observe anything. `join()` is the barrier that
(a) waits for all forked subtasks to finish or be cancelled, and (b) **flips the scope into
the "joined" state** that unlocks result inspection.

Two methods enforce that state as a *checked precondition*:

- `Subtask.get()` throws `IllegalStateException` unless the scope was joined **and** the
  subtask's state is `SUCCESS`.
- `throwIfFailed()` throws `IllegalStateException` unless the scope was joined.

The trap (Snippet B) is that people assume the exception is about *failure*. It isn't. With
both subtasks succeeding and no `join()`, you still get `IllegalStateException`. Success
does not exempt you.

**Why is `join()` load-bearing and not just bureaucracy?** The JEP specifies that `join()`
establishes a *happens-before* edge: actions performed by the subtask threads
happen-before the owner thread resumes from `join()`. That is the memory-visibility
guarantee that makes reading a subtask's result *safe to publish* to the owner. So the
precondition isn't arbitrary — without `join()` there is no happens-before edge, so the API
*refuses* to hand you a result rather than let you read an unpublished one.

Precision that separates a senior answer from folklore:
- The slap is **deterministic**, a *checked precondition*, not a data race. You will never
  "sometimes" get `"alice#42"` from Snippet B. It always throws.
- The happens-before edge is the *justification* for why a joined `SUCCESS` result is safely
  published — it is not the *enforcement* mechanism. Enforcement is the state check;
  visibility is the reason the state check is sound.
- `throwIfFailed()` throwing in B is about the **join precondition**, not the presence of a
  failure. (Contrast A, where join *was* done and the throw is a genuine `ExecutionException`.)

## Common mistakes

- **"`throwIfFailed()` handles everything, so I can skip `join()`."** → `IllegalStateException`
  at `throwIfFailed()`. It checks the join precondition first.
- **"Nothing failed, so `get()`/`throwIfFailed()` is safe without `join()`."** → still
  `IllegalStateException`. Success does not satisfy the join precondition.
- **Expecting `ExecutionException` from Snippet B.** → wrong type. `ExecutionException` is for
  a *failed but joined* subtask (Snippet A). The un-joined case is `IllegalStateException`.
- **Calling it "unsafe / racy."** → it is neither. It is a deterministic precondition check.
  Saying "race" in an interview signals you don't know the mechanism.
- **Treating `Subtask` like a `Future`.** A `Future.get()` blocks and returns; `Subtask.get()`
  does not block (the `join()` already waited) and enforces a lifecycle state instead.

## Interviewer Q&A

**Q:** In Snippet B both subtasks succeed. What does `lookup()` return?
**A:** Nothing — it throws `IllegalStateException` at `throwIfFailed()`. `join()` was never
called, and that method's precondition is "scope has been joined," independent of whether any
subtask failed. Success does not save you.

**Q:** Snippet A vs Snippet B both "fail." Why different exception types?
**A:** A is a *real* failure on a *joined* scope: `throwIfFailed()` rethrows the subtask's
cause wrapped in `ExecutionException`. B is a *precondition violation*: the scope was never
joined, so `throwIfFailed()` throws `IllegalStateException` before it ever looks at results.
Different layers — lifecycle check vs. result reporting.

**Q:** Why does `join()` matter for *correctness*, not just API hygiene?
**A:** `join()` establishes a happens-before edge: subtask actions happen-before the owner
resumes past `join()`. That is what makes a subtask's result safely visible (published) to
the owner thread. Without it there is no happens-before edge, so the API refuses to return a
result rather than expose an unpublished one.

**Q:** Is reading a result without `join()` a race condition?
**A:** No. It is a deterministic, checked precondition — `IllegalStateException` every single
run. The happens-before edge is the *reason* a joined result is safe to publish; it is not the
*enforcement* mechanism. Enforcement is the state machine.

**Q:** What is the strict lifecycle of a `StructuredTaskScope`?
**A:** `fork()` (schedule subtasks) → `join()` (await all + establish happens-before) →
inspect results / `throwIfFailed()` → `close()` (cleanup, typically via try-with-resources).
Out of order, the API throws `IllegalStateException`.

**Q:** What does `ShutdownOnFailure` do on the first failure, and how is that different from
`ExecutorService`?
**A:** On the first subtask failure it *interrupts* the still-running siblings (their state
becomes effectively cancelled), and `close()` blocks until every forked thread has terminated
— no thread outlives the block. With a plain `ExecutorService`, a failed task does not cancel
its peers; a sibling keeps running and can log/return after the caller has already moved on.
Structured concurrency makes that orphan-thread bug unrepresentable.

**Q:** How does `ShutdownOnSuccess` differ from `ShutdownOnFailure`?
**A:** `ShutdownOnSuccess` short-circuits on the *first success* (cancels the rest, you read
the winning result via `result()`) — useful for "race several sources, take the fastest."
`ShutdownOnFailure` waits for all and short-circuits on the *first failure* — useful for
fan-out where you need every result and any failure aborts the whole batch.

**Q:** Why is this whole thing only in `try`-with-resources?
**A:** `close()` is what enforces the structure: it cancels/awaits all subtasks so none can
outlive the lexical scope. try-with-resources guarantees `close()` runs even on early return
or exception, which is what bounds subtask lifetime to the block. Forking without an
auto-closed scope would reintroduce leaks.
