# Generics & Arrays — The Cast You Never Wrote

**Concept:** A generic method that returns `(T[]) new Object[n]` *compiles, runs, and
returns happily* — the lie is invisible inside the method because `T` erases to
`Object`, so `(T[])` becomes a no-op. The cast that actually fires is the one the
**caller's** compiler silently inserts at the assignment. Whether it throws depends
entirely on what type the *caller* assigns the result to — same call expression, two
different fates. This is EJ28: prefer lists to arrays, because generic array creation
forces exactly this displaced, deferred lie. (The reified-vs-erased / `ArrayStoreException`
mechanics live next door in `s01_06_arrays`; here we hunt the *displacement*.)

**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

```java
static <T> T[] fillBroken(T item, int count) {
    Object[] arr = new Object[count];
    Arrays.fill(arr, item);
    return (T[]) arr;          // "T erases to Object, so this cast is a no-op" — true, INSIDE here
}
```

This method compiles (with one unchecked warning) and works. Now the caller:

```java
Object[] a = fillBroken("x", 3);   // line A
String[] b = fillBroken("x", 3);   // line B
```

The right-hand side is **byte-for-byte identical** on both lines. One of them throws
`ClassCastException`; the other is perfectly fine. You never wrote a cast on either
line. So **who did**, and **why does only one line blow up**? Answer that and you
understand why `T[]` is a trap and why `List<T>` isn't.

## Task

Edit `GenericArray.java` and turn the red test green. Files in this package:

- `GenericArray.java` — `fillBroken` (the trap, **do not change**) and `fillSafe`
  (a `throw new UnsupportedOperationException("TODO")` stub — **Task 2**).
- `GenericArrayTest.java` (under `src/test/...`) — **do not edit**.

1. **Commit your prediction first.** In `GenericArrayTest`, set the literal
   `PREDICT_WHICH_THROWS` to one of `OBJECT_LINE`, `STRING_LINE`, `NEITHER`, `BOTH`
   — your honest call for *which* assignment throws `ClassCastException`. The two
   call expressions are identical; only the declared variable type differs. Write
   it down **before** you run.
2. **Implement `fillSafe(Class<T> componentType, T item, int count)`** so it returns
   a *real* `T[]` (a genuine `String[]` when called with `String.class`) — one that
   survives assignment to `String[]` in the caller. Use the `Class<T>` token to
   create an array with a real, reified component type instead of faking it from
   `Object[]`. This turns `fillSafeProducesRealArray` green.
3. **Diagnose in one sentence:** the method body of `fillBroken` contains no cast
   at all once compiled — so where, exactly, does the `ClassCastException` come
   from, and whose `.class` file holds the instruction that throws it?

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Fix `GenericArray.java` only. Do **not** edit `GenericArrayTest.java`.
- `fillSafe` must return an array whose runtime type is *exactly* `componentType[]`
  (so `b.getClass() == String[].class`), not an `Object[]` wearing a `T[]` label.
- Do not "fix" `fillBroken`. It is the specimen; leave it broken.

## Scale up (Optional)

1. **The displacement, proven in bytecode (Gilfoyle's dare).** Compile a tiny class
   with lines A and B, then run `javap -c` on **the caller class** (not on
   `GenericArray`). Find the `checkcast` instruction. Which method's `.class` file
   contains it? Then `javap -c GenericArray` and confirm `fillBroken`'s body has
   **zero** `checkcast` — its erased return type is `[Ljava/lang/Object;`. State who
   emitted the cast and why JVMS only emits it on line B, not line A.
2. **The warning is not noise (EJ27).** Your `fillSafe` almost certainly still
   produces *one* unchecked warning on the `(T[])` line, because
   `Array.newInstance` returns `Object`. Argue why that single warning is *safe to
   suppress* here (what does `Array.newInstance(componentType, n)` guarantee about
   the result's reified type?) — unlike `fillBroken`'s warning, which was a genuine
   lie. Localize a `@SuppressWarnings("unchecked")` to the smallest scope and
   justify it in a comment, per EJ27.
3. **The token that needs no suppression.** Re-implement `fillSafe` taking an
   `IntFunction<T[]>` (e.g. called as `String[]::new`) instead of `Class<T>`. Show
   that *this* version compiles with **zero** unchecked warnings. Why is the array
   constructor reference statically type-safe where `Array.newInstance` is not?
4. **EJ28's actual recommendation: delete the array.** Rewrite the whole API to
   return `List<T>` instead of `T[]`. Show that the displacement bug becomes
   *unrepresentable* — there is no caller-side `checkcast`, no `ClassCastException`,
   no warning. State the one-sentence rule EJ28 is really selling.

## How to verify

```
mvn -q -Dtest=GenericArrayTest test
```

`displacement` documents the trap (and checks your `PREDICT_WHICH_THROWS` literal);
`fillSafeProducesRealArray` starts **red** and turns green only once `fillSafe`
builds a real `componentType[]`. If you predicted `NEITHER` or `OBJECT_LINE`, you
are exactly the developer this exercise hunts: you trusted that an identical call
expression has an identical outcome.

<details><summary>Hint (open only if stuck)</summary>
Erasure rewrites `fillBroken`'s signature to return `Object[]`. The `(T[])` cast
in its body erases to `Object[] -> Object[]`, a no-op the compiler deletes — so the
method never throws. At the call site, the compiler knows the *static* return type
it inferred: `Object[]` for line A (no cast needed), `String[]` for line B. For line
B it must insert `checkcast [Ljava/lang/String;` to honor the type you declared —
and that runtime check fails because the object is really an `Object[]`. The fix:
`Array.newInstance(componentType, count)` makes an array whose *reified* component
type is genuinely `String`, so the caller's `checkcast` passes. `Class<T>` carries
the runtime type that `T` alone threw away.
</details>
