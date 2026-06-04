# Structured Concurrency — The `join()` you forgot

**Concept:** `join()` is the load-bearing barrier of `StructuredTaskScope`. Without it, the scope refuses to hand you a subtask result — even when every subtask succeeded.
**Difficulty:** ★★★ senior
**Est. time:** ~15 min

> **Preview API (Java 21, JEP 453).** `StructuredTaskScope` is a *preview* feature.
> This project builds without `--enable-preview`, so there is **no `.java` scaffold**
> and **no test** to run — everything lives in fences here. This is a *predict-the-output*
> exercise: commit to an answer in writing, then check `SOLUTION.md`.

## Background

Structured concurrency ties a set of subtasks to a lexical (try-with-resources) block:
fork children, `join()` to wait for them all, inspect results, then `close()` cleans up —
and a failed child cancels its siblings automatically. Nothing leaks past the block.

The lifecycle is strict: **`fork()` → `join()` → (inspect / `throwIfFailed()`) → `close()`**.
People who think of `Subtask` as "a `Future` but nicer" skip `join()` and reach straight
for the result. The API slaps them. Your job is to predict *exactly how*.

## The motivator (why this API exists)

With a plain `ExecutorService`, a sibling can be orphaned. `fetchUser` throws, but the
already-submitted `fetchOrder` keeps running on a pool thread, burning time and logging
*after* `main` has already moved on. Nobody cancels it. Structured concurrency exists to
make that bug unrepresentable: the failure cancels the sibling and `close()` blocks until
it actually dies.

## Task

Read the three snippets below. For **each**, write your prediction in `answers.txt`
*before* opening `SOLUTION.md`: the program's result, OR the exact exception type thrown
and which line throws it. Assume `fetchUser()` returns `"alice"` and `fetchOrder()` returns
`"42"`, both quickly and without error unless stated.

### Snippet A — the failure path (warm-up)

```java
String lookup() throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<String> user  = scope.fork(this::fetchUser);   // throws RuntimeException("boom")
        Subtask<String> order = scope.fork(this::fetchOrder);  // slow, succeeds
        scope.join();
        scope.throwIfFailed();
        return user.get() + "#" + order.get();
    }
}
```
*Predict:* what propagates out of `lookup()`, and what type wraps the `"boom"` cause?

### Snippet B — the trap (THIS is the one)

```java
String lookup() throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<String> user  = scope.fork(this::fetchUser);   // succeeds, returns "alice"
        Subtask<String> order = scope.fork(this::fetchOrder);  // succeeds, returns "42"
        scope.throwIfFailed();             // <-- write your prediction: what happens HERE?
        return user.get() + "#" + order.get();
    }
}
```
*Predict:* **both subtasks succeed.** Is the result `"alice#42"`? Or does something throw —
and if so, *what type*, and at *which line*?

### Snippet C — read-before-join (sanity check)

```java
String lookup() throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<String> user = scope.fork(this::fetchUser);    // succeeds, returns "alice"
        return user.get();                 // <-- predict: result or exception?
    }
}
```
*Predict:* the subtask completed successfully. Does `get()` return `"alice"`?

## Constraints

- Java 21. Do not paste these into a compiler — you have no `--enable-preview` here, and
  the point is to reason, not to run.
- Commit to one answer per snippet in `answers.txt` before reading the solution. No edits
  after you peek.

## How to verify

Write A, B, C predictions in `answers.txt`, then compare against `SOLUTION.md`. Score
yourself: getting **B** right (both succeed, still throws) is the whole exercise.

<details><summary>Hint (open only if stuck)</summary>
`Subtask.get()` and `throwIfFailed()` both have a precondition: the scope must have been
`join()`-ed. The precondition is about the *join*, not about whether anything failed.
Success doesn't exempt you. What kind of exception does a violated *precondition* throw?
</details>
