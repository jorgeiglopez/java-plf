# Futures & CompletableFuture — The box-in-a-box that swallows your exception

**Concept:** `thenApply` vs `thenCompose` — when your stage function *itself* returns a `CompletableFuture`, `thenApply` nests it (and silently orphans its failure); only `thenCompose` flattens it.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You are wiring an order checkout. `priceOf(orderId)` gives you a price; `chargeAsync(amount)`
*returns its own* `CompletableFuture<Integer>` (it talks to a payment service). You need to
chain them: take the price, then charge it.

The reflexive move is `priceOf(id).thenApply(price -> chargeAsync(price))`. It compiles. The
demo passes. Then a declined charge sails through to production with a green alert, because the
outer future completed *successfully* — holding a still-running, about-to-fail inner future that
nobody is watching. Why?

## Task

Open `OrderPipeline.java`. `priceOf` and `chargeAsync` are correct — do **not** touch them.
Implement the two TODOs so all three tests in `OrderPipelineTest` go green:

1. **`checkout(int orderId)`** — chain `priceOf` into `chargeAsync` and return a
   `CompletableFuture<Integer>` that yields the *charged amount* (price + 100), not a
   nested `CompletableFuture<CompletableFuture<Integer>>`. A declined charge
   (`chargeAsync` throws) must make the **returned** future fail.

2. **`checkoutWithRecovery(int orderId)`** — same chain, but recover from a decline by
   yielding `-999` instead of failing. The recovery handler must actually fire when the
   *charge* is declined.

## Constraints

- Java 21, no external libraries.
- Do not modify the test file, `priceOf`, or `chargeAsync`.
- Do not unwrap with a blocking `.get()`/`.join()` *inside* the composition — compose lazily.

## How to verify

```
mvn -q -Dtest=OrderPipelineTest test
```

All three tests pass.

<details><summary>Hint (open only if stuck)</summary>
When your lambda returns a `CompletableFuture`, ask: do I want a future-of-a-future, or one
flat future? One of `thenApply` / `thenCompose` flattens. Then ask *which future* the recovery
handler in task 2 is attached to — if it's attached to the already-completed outer, it has
nothing to recover.
</details>
