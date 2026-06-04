# Generics and Varargs — The annotation that lies

**Concept:** `@SafeVarargs` is a *promise* that the generic varargs array never escapes — not a fix. Break the promise and the annotation lies while a `ClassCastException` tells the truth.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

A generic varargs parameter `T... args` is really a `T[]`, and because of erasure that
array is born as `Object[]` at runtime. `@SafeVarargs` silences the heap-pollution
warning the compiler raises — but it changes *nothing* about the bytecode. So a method
can carry `@SafeVarargs`, compile without a single warning, and still hand a polluted
array to its caller.

In `UnsafeVarargs.java`, `toArray` is annotated `@SafeVarargs` and simply does
`return args;` — it *returns the backing array*. `pickTwo` delegates to it. The provided
test calls:

```java
String[] result = pickTwo("x", "y", "z");
```

and it throws a `ClassCastException` — with no visible cast anywhere in your code. Where
does the cast come from, and why doesn't the `@SafeVarargs` annotation prevent it?

## Task

1. **Diagnose.** Run the test, watch `originalPickTwoPollutesTheHeap` pass (it asserts the
   CCE). In `UnsafeVarargs.java`, replace the two blanks in the Javadoc on `pickTwoSafe`
   with one sentence of the form: *"`______` escapes the method, so a `______` fails at
   the call site."* (the runtime array type, and the bytecode instruction the compiler
   silently inserted).
2. **Fix.** Implement `pickTwoSafe` so it returns `List<T>` containing exactly the first
   two arguments. Turn `safePickTwoReturnsTheElements` green. You may **not** change
   `toArray`'s signature — fix the *escape*, don't suppress a warning.

## Constraints

- Java 21, no external libraries.
- Do not modify the test file, and do not change `toArray`'s signature.
- The fix must not return any `T[]` — the whole point is that the array must not escape.

## How to verify

```
mvn -q -Dtest=UnsafeVarargsTest test
```

`originalPickTwoPollutesTheHeap` should be green from the start (it proves the bug);
`safePickTwoReturnsTheElements` goes green once `pickTwoSafe` is implemented.

<details><summary>Hint (open only if stuck)</summary>
The array `toArray` creates is `Object[]`, not `String[]` — erasure picks the bound of
the type variable, which is `Object`. The assignment `String[] r = pickTwo(...)` forces a
`checkcast String[]`, and `Object[]` is not a subtype of `String[]`. A `List<T>` is
reifiable-free of this trap: it has no array component type to mismatch. `List.of(a, b)`
or `Arrays.asList(a, b)` both work — but `List.of` rejects nulls.
</details>
