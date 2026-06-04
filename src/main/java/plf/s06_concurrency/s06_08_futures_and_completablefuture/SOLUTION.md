# Solution — The box-in-a-box that swallows your exception

## Ideal solution

```java
public static CompletableFuture<Integer> checkout(int orderId) {
    return priceOf(orderId).thenCompose(price -> chargeAsync(price));
    // equivalently: .thenCompose(OrderPipeline::chargeAsync)
}

public static CompletableFuture<Integer> checkoutWithRecovery(int orderId) {
    return priceOf(orderId)
            .thenCompose(price -> chargeAsync(price))
            .exceptionally(ex -> -999);
}
```

`thenCompose` flattens: the function returns a `CompletableFuture<Integer>`, and the outer
*adopts* that inner future — its value and, crucially, its failure. So `checkout(-1)` returns a
future that fails with a `CompletionException` caused by `IllegalStateException`, and the
`.exceptionally(...)` in task 2 sits on a stage that genuinely sees that failure.

## Why this works (and the gotcha)

`thenApply(f)` takes whatever `f` returns and stuffs it, verbatim, into the result future. If
`f` returns an `Integer`, you get `CompletableFuture<Integer>`. But here `f` returns a
`CompletableFuture<Integer>`, so `thenApply` gives you:

```java
CompletableFuture<CompletableFuture<Integer>> nested =
    priceOf(id).thenApply(price -> chargeAsync(price)); // box in a box
```

Three things go wrong, and they compound:

1. **The type.** `nested.join()` returns the *inner* `CompletableFuture`, not an amount.
   `nested.join().getClass()` is `CompletableFuture`. The declared type won't even let you do
   integer arithmetic on it — the compiler is the first to complain (which is why the task forces
   the flat return type).

2. **The vanished failure.** The lambda returns the inner future *reference* the instant it is
   created — before the pool has even run the charge. So the **outer** stage completes
   *normally*, holding a future that is still in flight and about to throw. `nested.join()`
   succeeds. The exception lives, undelivered, inside `nested.join()`; you only see it if you
   reach in and `join()` the inner one too: `nested.join().join()` throws `CompletionException`.
   In production that is the alert that never fires — the outer was green the whole time.

3. **Dead recovery.** Reaching for `nested.exceptionally(ex -> -999)` feels like the fix. It is
   dead code: `exceptionally` runs only when *its upstream* fails, and the outer never failed.
   The handler never fires. The decline is still orphaned.

`thenCompose` is the cure for all three: it expects the function to return a `CompletionStage`
and flattens it into the chain, so the outer future *is* the inner future's fate — value,
exception, and timing included. Memorable form: **`thenApply` puts your value in a box; if that
value is itself a future, you get a box in a box — `thenCompose` is the only thing that unwraps
it.** It is the `flatMap` of `CompletableFuture` (`thenApply` is `map`).

## Common mistakes

- `priceOf(id).thenApply(OrderPipeline::chargeAsync)` → compiles, returns
  `CompletableFuture<CompletableFuture<Integer>>`; `join()` hands you the inner future, and a
  decline never surfaces on the outer.
- "I'll just add `.exceptionally()` to the nested chain." → dead code; the outer stage never
  failed, so the handler never runs.
- `priceOf(id).thenApply(price -> chargeAsync(price).join())` → "fixes" the type by blocking
  the composing thread on the inner future. It works functionally but burns a pool thread on a
  blocking `join` mid-pipeline — exactly what async composition exists to avoid (and on a small
  `commonPool` it can deadlock under load). Use `thenCompose`.
- Confusing the wrapper types: a stage failure surfaces as `CompletionException` from `join()`
  but `ExecutionException` from `get()`. The *cause* is the original exception in both.

## Interviewer Q&A

**Q:** What's the difference between `thenApply` and `thenCompose`?
**A:** `thenApply` maps the value with a plain function (`T -> U`) and wraps the result.
`thenCompose` takes a function returning another `CompletionStage` (`T -> CompletionStage<U>`)
and flattens it into the chain. `thenApply` is `map`; `thenCompose` is `flatMap`. If your
function itself returns a future, `thenApply` nests (`CF<CF<U>>`) and `thenCompose` flattens
(`CF<U>`).

**Q:** With `thenApply` and a future-returning lambda, why does a failure in the inner future
not fail the outer one?
**A:** The lambda returns the inner future *reference* synchronously, the moment it's created.
The outer stage's job ("call the function, store its result") is done immediately and completes
*normally*. The inner future fails later, independently, and its failure is never propagated to
the outer — it's orphaned inside the value the outer holds.

**Q:** Show me, in one line, how you'd prove the swallowed failure exists with the broken code.
**A:** `nested.join().join()` — the outer `join()` succeeds and returns the inner future, the
second `join()` then throws `CompletionException` caused by the original exception. The outer
was green; the corpse was in the box.

**Q:** Someone "fixes" the nesting with `.exceptionally()` on the outer. Why doesn't it help?
**A:** `exceptionally` only fires when its upstream stage completed exceptionally. With
`thenApply` the outer completed normally, so the handler is dead code — the decline is still
unhandled.

**Q:** `get()` vs `join()` — what exception do you get, and what's a clean way to remember it?
**A:** Both throw on failure with the *original* exception as the cause. `get()` is the
checked-exception API: it throws `ExecutionException` (and `InterruptedException`). `join()` is
the unchecked variant for use inside lambdas/streams: it throws `CompletionException`. Same
cause, different wrapper.

**Q:** `thenApply` vs `thenApplyAsync` — which thread runs the callback?
**A:** `thenApply` (no `Async`) runs on whichever thread *completed the prior stage* — which can
be the calling thread if the prior stage was already complete. There is no thread of its own.
`thenApplyAsync` dispatches to the `ForkJoinPool.commonPool()` (or a supplied executor),
guaranteeing a hand-off. People wrongly assume "async stage = always another thread."

**Q:** Why is blocking with `.join()` inside a `thenApply` lambda a bad fix?
**A:** It occupies a pool thread for the entire duration of the inner future. On the
`commonPool` (parallelism `cores - 1`) a handful of such blocked stages can starve or deadlock
the pool. Async composition exists precisely to avoid blocking threads on pending work — use
`thenCompose`.

**Q:** When would you reach for `handle` or `whenComplete` instead of `exceptionally`?
**A:** `exceptionally` only sees the failure path and produces a fallback value.
`handle((value, ex) -> ...)` sees both outcomes and can transform either (it always runs and
returns a new value). `whenComplete((value, ex) -> ...)` also sees both but is for side effects
— it passes the original result/exception through unchanged. Use `handle` to recover-and-map,
`whenComplete` to observe without altering the outcome.
