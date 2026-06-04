# Generic Types & Methods — The Bound That Eats Inference

**Concept:** a *recursive* type bound `T extends Comparable<T>` makes `T` appear
**invariantly** inside its own bound, so type inference cannot fall back on the
usual "pick the common supertype" trick. When two arguments are *different*
self-comparable types, inference doesn't quietly widen — it dies, and the error
points at the **arguments**, not the bound that actually killed it.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You know the diamond operator and that `<T extends Comparable<T>>` means
"comparable to its own kind." Here is the part almost everyone gets wrong.

Given the textbook generic method:

```java
static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
}
```

What happens here?

```java
max("hello", new StringBuilder("hello"));
```

The tempting story: *"`String` and `StringBuilder` share the supertype
`CharSequence`, so inference picks `T = CharSequence` and we're done."* That story
is wrong twice over — `CharSequence` is not even `Comparable`, **and** that is not
the reason the compiler rejects the call. The real reason is subtler, and the
error message will send you hunting in the wrong place.

This is a **predict-then-prove** exercise. First you commit, in words, to *why*
inference fails. Then you make a green test by writing the one thing that rescues
the call: an **explicit type witness**.

## Task

Two files are committed: `MaxBound.java` (the `max` method above, plus a stub) and
the test `MaxBoundTest.java`. The suite is **red** right now.

**Task 1 — name the killer (no compiler, no edit yet).**

1. Do **not** run `javac`. On paper, decide what this line does:
   ```java
   Object x = MaxBound.max("hello", new StringBuilder("hello"));
   ```
   Compile error, runtime exception, or silent wrong answer? If it's a compile
   error, what does javac *blame* — the bound, the return type, or the arguments?
2. In `MaxBoundTest.java`, set `PREDICTED_REASON` to a one-line explanation of why
   no single `T` can be inferred. The `reasonIsCommitted` test pins the exact
   wording the solution expects — match its intent, then check `SOLUTION.md`.

**Task 2 — rescue the call with a witness.**

3. Implement `maxOfTwoStrings()` in `MaxBound.java` so it returns
   `max("hello", "world")` and compiles. Use an **explicit type witness** at the
   call site so there is no ambiguity about what `T` is. The method must return
   the lexicographic max (`"world"`).
4. Run the suite. `witnessedMaxReturnsWorld` goes green.

## Constraints

- Java 21, JUnit 5 only.
- Edit **only** `maxOfTwoStrings()` in `MaxBound.java` and `PREDICTED_REASON` in
  the test. Do not change `max`, do not change the assertions.
- The broken call (`max("hello", new StringBuilder(...))`) is a *paper* exercise —
  it is intentionally not in any `.java` file because it does not compile. Reason
  about it; the real javac output is in `SOLUTION.md`.

## Scale up (Optional)

1. **The witness that still fails.** Predict the exact javac error for
   `MaxBound.<CharSequence>max("hello", "world")`. Both arguments *are*
   `CharSequence`, so why is this rejected too? Check `SOLUTION.md`. (Different
   error than Task 1 — and it tells you precisely what the recursive bound
   demands.)
2. **Prove it's the bound, not the types.** Write an unbounded
   `static <T> T pick(T a, T b)` and call `pick("hello", new StringBuilder("hello"))`.
   Does *that* compile? What `T` does it infer? Explain why removing
   `extends Comparable<T>` makes the same arguments suddenly acceptable.
3. **Make a real recursive-bound type.** Sketch
   `enum Suit implements Comparable<Suit>` (enums already do) versus a hand-rolled
   `class Version implements Comparable<Version>`. Why does `Comparable<String>`
   on a `Version` fail `T extends Comparable<T>`, while `Comparable<Version>`
   satisfies it?

## How to verify

```
mvn -q -Dtest=MaxBoundTest test
```

Red before you start (the stub throws, `PREDICTED_REASON` is a placeholder). Green
once you commit the reason and implement the witnessed call.

<details><summary>Hint (open only if stuck)</summary>
Because <code>T</code> appears inside <code>Comparable&lt;T&gt;</code>, the two
arguments don't generate <em>upper-bound</em> constraints ("T is at least a
String", "T is at least a StringBuilder") that a lub could merge — they generate
<em>equality</em> constraints (<code>T = String</code> and
<code>T = StringBuilder</code>). Two unequal equality constraints have no
solution. The fix names <code>T</code> yourself:
<code>MaxBound.&lt;String&gt;max(...)</code>.
</details>
