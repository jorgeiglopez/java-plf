# Arrays — The Covariant Trap (one fact, two faces)

**Concept:** Arrays are **covariant + reified** (store check is dynamic →
`ArrayStoreException`); generics are **invariant + erased** (store check is
static → won't compile). These are the *same* type-safety problem solved two
different ways — and that single fact also explains why `new T[]` is illegal.
**Difficulty:** ★★★ senior
**Est. time:** ~15 min

## Background

`Object[] a = new String[2];` compiles. So does the analogous-looking
`List<Object> l = new ArrayList<String>();` — right? And `a[0] = 42;` is just an
assignment, so it's fine — right? Two of those three claims are wrong, and the
*way* they're wrong is the whole interview. If arrays let you widen `String[]`
to `Object[]`, what stops you from poking an `Integer` into a `String[]`? And if
that's solvable for arrays, why can't you ever write `new T[]`?

## Task

**Commit your predictions in writing BEFORE you run or compile anything.** Fill
this table in `answers.txt` (or on paper). For each line, predict **Compiles?**
(yes/no) and **Runtime?** (runs / throws X / N/A because it didn't compile).

| # | Line | Compiles? | Runtime? |
|---|------|-----------|----------|
| 1 | `Object[] a = new String[2];` | ? | ? |
| 2 | `a[1] = "ok";`  *(after line 1)* | ? | ? |
| 3 | `a[0] = 42;`    *(after line 1)* | ? | ? |
| 4 | `a.getClass() == String[].class` | ? | ? (true/false) |
| 5 | `List<Object> l = new ArrayList<String>();` | ? | ? |
| 6 | `static <T> T[] make() { return new T[1]; }` | ? | ? |

Then, numbered:

1. Predict all six rows and commit them in writing. **Do not peek.**
2. Run the harness: `mvn -q exec:java` is *not* configured — instead just
   compile and run `ArrayCovariance.main` (see *How to verify*), or run the
   green tests. Confirm rows 1–4 against reality.
3. Rows 5 and 6 are the **must-not-compile** rows — they live only in this
   markdown (committing them would break the build). For each, write the
   **exact Java 21 `javac` error message** you expect.
4. **The blank that is the whole exercise.** In
   `ArrayCovariance.whyNoGenericArray()` (currently a
   `throw new UnsupportedOperationException("TODO")` stub), complete and return
   this sentence:
   > "`new T[]` is forbidden because the array store check needs ____, but `T`
   > is ____."
   Completing it turns the red test `whyNoGenericArray_isCommitted` green.
5. In one sentence, state why row 3 throws at **runtime** while row 5 fails at
   **compile time**. Use the words *reified* and *erased*.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Commit your six predictions **before** compiling/running. The burn on row 3 is
  the lesson — don't rob yourself of it.
- Do **not** edit the test file. Make the red test pass by completing the
  production stub.
- Rows 5 and 6 must never become a real `.java` file — they belong in markdown.

## Scale up (Optional)

1. **Per-store, not per-array.** Explain why row 2 (`a[1] = "ok"`) succeeds but
   row 3 (`a[0] = 42`) throws, given both stores go through the same
   `Object[]` reference. What does that tell you about *when* the check runs?
2. **The autoboxing tell.** Row 3's exception message is one specific string.
   What is it, and why that value rather than `"int"` or
   `"java.lang.Object"`?
3. **Generics the other way.** Would `List<? extends Object> l =
   new ArrayList<String>();` compile? Does it let you `l.add("x")`? Tie the
   answer back to why generics are invariant in the first place.

## How to verify

Predict first, then check:

```
# Run the green facts (rows 1–4):
mvn -q -Dtest=ArrayCovarianceTest#covariantStore_throwsArrayStoreException_withOffendingClassName test
mvn -q -Dtest=ArrayCovarianceTest#arrayComponentType_isReified test

# Or run the harness directly after `mvn -q test-compile`:
java -cp target/classes plf.s01_core_language.s01_06_arrays.ArrayCovariance

# The exercise test starts RED and turns green once you finish the stub:
mvn -q -Dtest=ArrayCovarianceTest#whyNoGenericArray_isCommitted test
```

Compare your row-5 and row-6 error strings, and your one-sentence answer,
against `SOLUTION.md`.

<details><summary>Hint (open only if stuck)</summary>
The array object physically remembers it was born a `String[]`
(`getClass()` proves it). The store check reads that remembered component type
on every write. A generic's element type is gone at runtime — so a store check
like the array's is *impossible*, which is why generics push the check to the
compiler (and forbid `new T[]`, because there's no component type to check
against).
</details>
