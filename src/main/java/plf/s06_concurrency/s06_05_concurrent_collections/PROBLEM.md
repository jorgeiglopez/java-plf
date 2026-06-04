# Concurrent Collections — Where is the atomic boundary?

**Concept:** A concurrent collection makes each *operation* atomic, but a *sequence* of operations is still a race — and the atomic unit (a bin lock) can bite you from *inside* a single thread too.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

`ConcurrentHashMap` is thread-safe. So this word counter must be correct, right?

```java
Integer current = counts.get(word);
counts.put(word, current == null ? 1 : current + 1);
```

It passes every single-threaded test. Run it on 8 threads against a known corpus and the total comes up *short* — and short by a different amount every run. The map was never broken. The `get`-then-`put` sequence was: two atomic operations do not compose into one atomic operation.

That is one reading of a single ruler: **where is the atomic boundary?** This exercise measures that ruler twice — once *across threads* (Part A) and once *within one thread* (Part B), where the atomic unit is a `synchronized` bin and re-entering it is forbidden by contract.

## Task

Scaffold: `WordCounter.java`, `FibMemo.java`. Test: `ConcurrentCollectionsTest.java` (do **not** edit it).

**Part A — lost update across threads.** `WordCounter.record(String)` uses check-then-act (`get` then `put`). The test `lostUpdateUnderContention` fires 8 threads × 50 000 increments at one key and asserts the exact total. It starts **red**. Fix `record` so the read-modify-write is a *single* atomic map operation. Do not add locks, do not change the method signature.

**Part B — re-entering the atomic unit.** `FibMemo.fib(int)` is a "thread-safe memoized Fibonacci" using `computeIfAbsent`, whose mapping function calls `fib` again. `FibMemo.Key` has a fixed `hashCode() == 0`, so every key collides into one bin.

1. Run the test `forcedCollisionReentryThrows` (already green). `fib(20)` throws `IllegalStateException: Recursive update`. Explain in one line *why* — what is held across the lambda, and what does re-entry do to it?
2. Implement `fibSafe(int)` so the memoized result is correct **without** re-entering the map inside `computeIfAbsent`. Turn `fibSafeIsCorrect` green.

## Constraints

- Java 21, no external libraries.
- Do not modify `ConcurrentCollectionsTest.java`.
- Part A: exactly one atomic map call per increment; no `synchronized`, no extra locks.
- Part B: the mapping function passed to `computeIfAbsent` must not touch `this` map.

## Scale up (Optional)

1. Run `fib` with the *natural* key (a plain `record Key(int n) {}` with the default `hashCode`) in a 1000-iteration loop and observe the outcome. Why is it sometimes `6765`, sometimes a wrong number, sometimes an exception — and why is "sometimes correct" the *worst* result of the three?
2. Contrast iterators: a `ConcurrentHashMap` iterator vs a `CopyOnWriteArrayList` iterator under concurrent writes. Which throws `ConcurrentModificationException`? Which reflects later writes? Which rejects `iterator.set(...)`?
3. Why is `ConcurrentHashMap.size()` unreliable as a control-flow input mid-write?

## How to verify

```
mvn -q -Dtest=ConcurrentCollectionsTest test
```

All three tests green when done. `forcedCollisionReentryThrows` is green from the start and stays green.

<details><summary>Hint (open only if stuck)</summary>
Part A: one call replaces read-modify-write atomically — think <code>merge(key, 1, Integer::sum)</code> or <code>compute</code>. Part B: <code>computeIfAbsent</code> holds the bin's <code>synchronized</code> monitor across your lambda; calling <code>fib</code> inside re-enters that same bin. Compute the recurrence iteratively (bottom-up) and only <code>put</code> finished values — never recurse through the map.
</details>
