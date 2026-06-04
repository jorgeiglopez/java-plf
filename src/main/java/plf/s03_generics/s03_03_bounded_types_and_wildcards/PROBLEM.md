# Bounded Types & Wildcards — The Inverted PECS Trap

**Concept:** bounded wildcards encode *data-flow direction* (PECS), and the `T`
the compiler infers at a call site is not always the one you expect.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You know PECS: **P**roducer-**E**xtends, **C**onsumer-**S**uper. So this method
looks like textbook generics, and it is:

```java
static <T extends Number> void transfer(List<? extends T> src, List<? super T> dst) {
    dst.add(src.get(0));
}
```

`src` produces a `T`, `dst` consumes it. Clean. Now here's the part that bites
seniors who "know" PECS: **what does the compiler infer `T` to be at each call,
and which calls even compile?** "Super accepts supertypes" is the mantra that
walks people straight off a cliff here.

```java
List<Integer> ints = new ArrayList<>(List.of(1, 2, 3));
List<Number>  nums = new ArrayList<>();

transfer(ints, nums);   // (A)
transfer(nums, ints);   // (B)
transfer(nums, nums);   // (C)
```

## Task

1. **Predict — commit before compiling.** For each of (A), (B), (C), write down
   in a scratch file: *does it compile?* and if so, *what does `T` infer to?*
   Lock in your answers before reading the test.

2. **Explain the surprise.** Pick the one call that defied your prediction and
   write a single sentence — grounded in *how the compiler bounds `T`* — saying
   why it behaves the way it does. (The mantra "super means supertype" is not an
   explanation; it's the thing that fooled you.)

3. **Implement `copy` in `Transfer.java`.** Replace the `UnsupportedOperationException`
   stub with a method that moves *every* element from a producer into a consumer.
   Keep the given signature:
   ```java
   static void copy(List<? extends Number> src, List<? super Number> dst) { ... }
   ```
   Make `TransferTest` green.

## Constraints

- Java 21, no external libraries.
- Do **not** edit the `transfer` method or the test file.
- `copy` must keep its given signature (no type parameter — that's the point).
- Call (B) does **not** compile, so it lives only in this markdown, never in a
  `.java` file. Don't try to add it to the test.

## How to verify

```
mvn -q -Dtest=TransferTest test
```

All three tests must pass. Then check your prediction table against the
`SOLUTION.md` "prediction key".

<details><summary>Hint (open only if stuck)</summary>
At a call site the compiler picks <em>one</em> concrete <code>T</code> that
satisfies <em>both</em> parameter bounds at once. For (B), <code>src</code> being
<code>List&lt;Number&gt;</code> forces <code>T</code> toward <code>Number</code>,
but <code>dst</code> being <code>List&lt;Integer&gt;</code> demands
<code>T</code> be a <em>subtype</em> of <code>Integer</code>. No single
<code>T</code> can be both. For (C), one type — <code>Number</code> — satisfies
<em>both</em> wildcards, so a list playing producer <em>and</em> consumer is fine.
</details>
