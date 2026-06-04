# Solution — The peek that never fired

## Ideal solution

The correct predictions:

```java
int predictedElided = 0;    // count() elides the pipeline; peek never runs
int predictedFiltered = 4;  // filter clears SIZED; count() must traverse

boolean predictedThrowsWithoutFilter = false; // peek never fires
boolean predictedThrowsWithFilter = true;     // peek fires on the first element
```

So:

- `touchesWhenElided()` returns **0**.
- `touchesWhenFiltered()` returns **4**.
- `throwingPeekFires(false)` returns **false** — and the bare `count()` returns 3.
- `throwingPeekFires(true)` returns **true**.

The one-sentence answer: `filter(s -> true)` clears the stream's `SIZED` flag
(filter can't know how many elements survive), so `count()` loses its size
shortcut and is forced to traverse every element — which runs the `peek`.

### Scale up solutions

**1. EJ45 — judgment.** The stateful `iterate` version is objectively worse:

```java
// Tempting, but a stateful one-liner that reads worse than the loop:
int viaStream = Stream.iterate(1, n -> n + 1)
        .filter(n -> n % 2 == 0)
        .limit(10)
        .mapToInt(n -> n * n)
        .sum();

// Plainer, faster, debuggable:
int viaLoop = 0;
for (int k = 1, taken = 0; taken < 10; k++) {
    if (k % 2 == 0) { viaLoop += k * k; taken++; }
}
```

Both give `1540`. The rule for a teammate: use a stream when the pipeline is a
clean sequence of stateless transforms over a source; reach for a loop when you
need shared mutable state, early `break`/`return`/`continue`, exception handling
per element, or to mutate locals — streams make those awkward or impossible, and
forcing them in hurts readability (EJ45: *use streams judiciously*).

**2. Single-use.** It throws:

```
java.lang.IllegalStateException: stream has already been operated upon or closed
```

`count()` is a terminal op; it binds and consumes the stream's spliterator. The
second terminal op (`map(...).toList()` — actually the `map` already touches the
already-consumed pipeline) finds the stream closed and throws. A `Stream` is a
single-use object, not a reusable collection.

## Why this works (and the gotcha)

The naive model is *horizontal-and-complete*: "the terminal op fires, and every
element flows through every stage." That model is wrong on both axes.

`count()` doesn't want your elements — it wants a number. When the source stream
reports `StreamOpFlag.SIZED` and every intermediate op preserves the size,
HotSpot's `ReduceOps`/`AbstractPipeline` machinery answers `count()` from the
spliterator's exact size and **traverses nothing**. Your `peek` is part of the
pipeline, but the pipeline is never walked, so the action — even one that throws —
never executes. That is laziness made falsifiable: a program that "should" throw,
doesn't.

Insert `filter(s -> true)` and the picture changes deterministically. `filter`
cannot predict its output size (a predicate might drop elements), so it **clears
SIZED**. Now `count()` has no exact size to read; it must traverse the source,
pulling each element through `filter` and `peek`. The `peek` fires four times (or
throws on the first element). Same source, one operator, laziness toggled — not a
vibe, just flag bookkeeping.

Why a *throwing* peek instead of "prints nothing"? Absence of output is
unconvincing: a skeptic can't tell "elision proved" from "I forgot the terminal
op." A program that should have thrown and silently returned 3 is a hard,
falsifiable signal you can't argue away.

Note: `map` preserves SIZED (one-in-one-out), so `Stream.of(...).map(...).count()`
still elides. Only size-altering ops (`filter`, `flatMap`, `distinct`, `limit`,
`takeWhile`) clear it.

## Common mistakes

- "`count()` runs the whole pipeline, so the throwing peek throws." → On a SIZED,
  size-preserving source it returns the size from the spliterator and never runs
  `peek`.
- "`filter(s -> true)` is a no-op, it changes nothing." → It keeps every element
  but still clears SIZED, which is exactly what re-enables traversal.
- Confusing this with `Stream.of(...).filter(...).count()` *eliding the filter*.
  It does **not**: `filter` clears SIZED, so the predicate runs on every element.
  Elision is `count()` skipping a *size-preserving* pipeline, not skipping filters.
- Assuming `peek` ordering is something you can pin down generally. The `peek`
  Javadoc says the number of invocations is unspecified under optimization — which
  is precisely why this exercise asserts a *count of 0 vs N* and a *throw vs
  no-throw*, both deterministic on Java 21, rather than an interleaving.

## Interviewer Q&A

**Q:** What does "a stream is lazy" actually mean?
**A:** Intermediate operations build a pipeline description but execute nothing.
No element is pulled and no lambda is invoked until a terminal operation runs —
and even then, only as much work as the terminal op demands.

**Q:** `Stream.of("a","b","c").peek(p).count()` — does `p` run?
**A:** No. The source is SIZED and `peek` is size-preserving, so `count()` answers
from the spliterator's exact size and traverses nothing; `peek` runs zero times.

**Q:** Add `.filter(s -> true)` before `peek`. Now does `p` run?
**A:** Yes, once per element. `filter` clears the SIZED flag (it can't know how
many survive), so `count()` has no size shortcut and must traverse, running `peek`.

**Q:** Does `map` clear SIZED the way `filter` does?
**A:** No. `map` is one-in-one-out, so it preserves SIZED; `Stream.of(...).map(f).count()`
still elides. Size-*altering* ops — `filter`, `flatMap`, `distinct`, `limit`,
`takeWhile` — clear it.

**Q:** Is it safe to rely on count-elision skipping side effects in production?
**A:** No — rely on it for *understanding*, not behavior. Side effects inside
stream ops are an anti-pattern (EJ46). The `peek`/`filter` Javadocs leave invocation
counts unspecified, so treat elision as JVM latitude you must not depend on.

**Q:** What happens if you call a second terminal op on the same stream?
**A:** `IllegalStateException: stream has already been operated upon or closed`.
A stream binds its spliterator on the first terminal op and is single-use; reuse
requires building a fresh stream.

**Q:** When should you *not* use a stream (EJ45)?
**A:** When you need shared mutable local state, early exit (break/return), checked
exceptions per element, or you're forcing a fundamentally imperative algorithm into
a pipeline. A plain loop is then clearer and often faster.

**Q:** How would you *prove* laziness to a skeptic without a debugger?
**A:** Put a throwing action in a stage and show the program completes normally —
a side effect that "should" have fired and didn't is falsifiable evidence the
stage was never traversed, which an empty console (could be a missing terminal op)
is not.
