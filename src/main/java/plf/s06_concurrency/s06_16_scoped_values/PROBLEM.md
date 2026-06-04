# Scoped Values — Where Does `get()` Read From?

**Concept:** A `ScopedValue` binding is an immutable value bound to the *current thread's active call frame* — `get()` reads the **running thread's** binding at call time, not a value captured by a lambda and not a value shared with every thread.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

> **PREVIEW API.** `ScopedValue` and `StructuredTaskScope` are preview in Java 21
> (JEP 446 / JEP 453) and require `--enable-preview`. This project builds without
> preview flags, so there is **no live `.java` file** for this exercise — all code
> lives in the fences below. This is a *predict-the-output* exercise: commit your
> answers to `answers.txt`, then check against `SOLUTION.md`.

## Background

You've used `ThreadLocal`. Its mental model is a **mutable slot** per thread: you
`set(v)`, later code `get()`s `v`, and it stays set until you `remove()` it (or the
pooled thread leaks it). `ScopedValue` looks similar but breaks that model on
purpose. There is **no `set()`**. A value is *bound* only for the dynamic extent of
a `run(...)`, and the binding **reverts the instant that `run` returns** — no
`finally`, no `remove()`.

The trap this exercise drills: people think a lambda "captures" the binding, or that
any thread you start inside the extent "inherits" it. Both are wrong. Three probe
sites below read the *same* `ScopedValue` through the *same* lambda — and produce
three different results. Your job is to say which, and why.

## Task

Given this setup (assume each numbered probe runs in isolation, in order, inside the
`"bob"` extent shown):

```java
static final ScopedValue<String> USER = ScopedValue.newInstance();

void outer() {
    ScopedValue.where(USER, "alice").run(() -> {
        Runnable later = () -> System.out.println(USER.get());

        ScopedValue.where(USER, "bob").run(() -> {

            // (A) call it right here, same thread, still inside the "bob" extent:
            later.run();

            // (B) fork it via a StructuredTaskScope created inside the "bob" extent:
            try (var scope = new StructuredTaskScope<Object>()) {
                scope.fork(() -> { later.run(); return null; });
                scope.join();
            }

            // (C) hand it to an ad-hoc virtual thread started inside the extent:
            Thread.ofVirtual().start(later).join();
        });
    });
}
```

1. In `answers.txt`, write what each of `(A)`, `(B)`, `(C)` prints — or, if it does
   not print a normal value, write exactly what happens instead.
2. For each, write **one line** explaining *why* it does what it does. Aim each
   explanation at one belief: does the lambda *capture* the binding? Does the child
   thread *inherit* it?
3. Bonus: why is there no probe that *rebinds* `USER` after `scope.fork(...)` but
   before the forked task reads it? (What guarantee makes that question meaningless?)

## Constraints

- Java 21, preview features. Do not compile/run anything until you've committed your
  three predictions — the whole value is in predicting first.
- Reason about `(C)` carefully: where does the exception surface, and on which
  thread? Does `outer()` see it?

## How to verify

Write your three predictions + reasons in `answers.txt`, then compare against
`SOLUTION.md`. You "pass" only if all three reasons name the right mechanism, not
just the right output.

<details><summary>Hint (open only if stuck)</summary>
A lambda captures *variables*, not *bindings*. `USER` is the captured variable (the
`ScopedValue` handle); `USER.get()` is resolved fresh, against whichever thread is
executing the lambda, against that thread's currently-active binding set. So the
question for each probe is only: *which thread runs the lambda, and does that thread
have a binding for `USER`?*
</details>
