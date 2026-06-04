# Stream Basics — The peek that never fired

**Concept:** a stream is lazy; a terminal op pulls only what it needs, so whole stages can be skipped — even side-effecting ones.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Everyone "knows" a stream pipeline runs every element through every stage once a
terminal op fires. So this program *must* blow up, right?

```java
long n = Stream.of("a", "b", "c")
    .peek(s -> { throw new IllegalStateException("touched " + s); })
    .count();
System.out.println(n);
```

A `peek` that throws on the first element, in front of a terminal op. The smug
answer is "it throws `IllegalStateException`." Run it and it prints `3`. The
`peek` action is **never invoked**. Now add one operator — `.filter(s -> true)`
before the `peek` — and the same program throws. One operator flips laziness on
and off. Why?

## Task

Open `LazyCount.java`. It contains three deterministic probes over the SIZED
source `Stream.of("a","b","c","d")`:

- `touchesWhenElided()` — counts how many times a non-throwing `peek` action runs
  before a bare `count()`.
- `touchesWhenFiltered()` — same, but with `.filter(s -> true)` inserted before the
  `peek`.
- `throwingPeekFires(withFilter)` — returns `true` iff a *throwing* `peek` actually
  fired (the exception escaped).

In `LazyCountTest.java`, two tests start red. Replace the four prediction values
with what you believe the JVM actually does **before running anything**:

1. `predictedElided` — how many times does `peek` run when `count()` follows it directly?
2. `predictedFiltered` — how many times does `peek` run when a `filter(s -> true)` sits in front?
3. `predictedThrowsWithoutFilter` — does the throwing `peek` fire without a filter?
4. `predictedThrowsWithFilter` — does it fire with the filter?

Commit to the numbers, then run the tests. When both pass, write one sentence:
*why does `filter(s -> true)` — a filter that keeps everything — change the answer?*

## Constraints

Java 21, no external libraries. Do **not** modify `LazyCount.java` or weaken the
assertions; only edit the four prediction values in the test.

## Scale up (Optional)

1. **EJ45 — judgment.** Sum the first 10 even squares two ways: a `Stream.iterate`
   pipeline that carries state, and a plain `for` loop. Which is more readable?
   Write down the rule you'd give a teammate for when *not* to reach for a stream.
2. **Single-use.** Predict the exact runtime exception (message included) of:
   ```java
   Stream<Integer> s = Stream.of(1, 2, 3);
   long c = s.count();
   List<Integer> l = s.map(n -> n * 2).toList();
   ```

## How to verify

`mvn -q -Dtest=LazyCountTest test` — both tests must go green with your predictions.

<details><summary>Hint (open only if stuck)</summary>
`count()` doesn't need to look at your elements — it needs a number. If the stream
can report its exact size, the terminal op asks for that number and traverses
nothing. The magic word is `SIZED`: it's a stream flag. `Stream.of(...)` is SIZED.
`map` preserves it. `filter` cannot (it doesn't know how many survive), so it
*clears* SIZED — and now `count()` has no shortcut and must walk every element,
running your `peek` along the way.
</details>
