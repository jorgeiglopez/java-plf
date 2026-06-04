# Solution — The annotation that lies

## Ideal solution

The escape sentence (Task 1):

> **`Object[]`** escapes the method, so a **`checkcast`** fails at the call site.

(More fully: the array `toArray` builds is `Object[]` at runtime; the caller's assignment
`String[] r = pickTwo(...)` forces a `checkcast String[]`, and `Object[]` is not a subtype
of `String[]`, so the JVM throws `ClassCastException`.)

The fix (Task 2) — return a `List<T>`, which has no array component type to mismatch:

```java
static <T> List<T> pickTwoSafe(T a, T b, T c) {
    return List.of(a, b);
}
```

`Arrays.asList(a, b)` is an equally valid escape-free return; the only difference is that
`List.of` is immutable and rejects nulls, while `Arrays.asList` permits both. Either way
the backing array (if any) stays encapsulated inside the `List` and never reaches the
caller as a `T[]`, so there is no `checkcast` and no pollution.

## Why this works (and the gotcha)

`T... args` compiles to a `T[]` parameter. Generics are erased, so the type variable's
array becomes its **bound's** array — here `Object[]`. `toArray` returns that `Object[]`
under the static type `T[]`. The lie lives in two places:

1. **`@SafeVarargs` on `toArray`.** The annotation only suppresses the unchecked/heap-
   pollution *warning*. It is a programmer assertion: "trust me, this array doesn't
   escape." But `return args;` makes it escape. The annotation compiles, the warning
   vanishes, and the bug is fully intact. The annotation is a *promise*, not a fix.

2. **The invisible cast.** There is no cast in your source. The compiler inserts a
   `checkcast String[]` at `String[] result = pickTwo(...)`, because `pickTwo`'s erased
   return type is `Object[]` but the call's inferred type is `String[]`. The exception
   surfaces at the **assignment**, not inside either method — which is exactly why it's
   so disorienting.

The decisive subtlety: the CCE only fires when the result is assigned to a *reified* array
type like `String[]`. Assign it to `Object[]` and discard it, and nothing throws — the
heap pollution is latent until someone trusts the static type. That's heap pollution: a
reference whose static type lies about the object it points to.

Returning `List<T>` removes the array from the method's contract. Lists are generic
containers with no reifiable component type, so there is nothing for a `checkcast` to
reject. The escape point is closed at its source — you did not merely silence a warning.

A method like `static <T> List<T> listOf(T... args) { return List.of(args); }` is the
*honest* `@SafeVarargs` case: the array `args` is only read and never escapes, so the
annotation's promise is true.

## Common mistakes

- **Adding `@SafeVarargs` to "fix" it.** It removes the warning, not the CCE. The
  annotation has zero runtime effect.
- **Putting `@SafeVarargs` on `pickTwo`.** It won't even compile — `@SafeVarargs` is only
  legal on actual varargs methods (and on `static`/`final`/`private` ones). `pickTwo` is
  not varargs.
- **Returning `T[]` from the fix** (e.g. `(T[]) new Object[]{a, b}`). Same escape, same
  CCE — you moved the lie, you didn't remove it.
- **Blaming the line `pickTwo` or `toArray`.** The CCE materializes at the caller's
  assignment, at the `checkcast`, not inside either method body.
- **Assuming the array is `String[]`.** Erasure makes it `Object[]`; that mismatch *is*
  the bug.

## Interviewer Q&A

**Q:** What does `T... args` actually compile to, and what is the array's runtime type?
**A:** A `T[]` parameter. Because of erasure the runtime array type is the erasure of
`T` — for an unbounded `T` that's `Object[]`. The varargs syntax is pure sugar over an
array argument.

**Q:** What exactly does `@SafeVarargs` do?
**A:** It suppresses the compiler's heap-pollution warning on a generic varargs method.
It is an assertion by the author that the method does not do anything unsafe with the
varargs array (chiefly: does not let it escape and does not store untyped objects into
it). It has no runtime effect whatsoever.

**Q:** In this exercise, where does the `ClassCastException` come from — there's no cast
in the source.
**A:** The compiler inserts a `checkcast String[]` at the caller's assignment
`String[] r = pickTwo(...)`, because `pickTwo`'s erased return type is `Object[]` while
the inferred return type is `String[]`. The actual array is `Object[]`, the `checkcast`
to `String[]` fails.

**Q:** When is it *safe* to put `@SafeVarargs` on a method?
**A:** When the method neither (a) stores anything into the varargs array nor (b) lets a
reference to the array escape — i.e. it only reads the elements. `Arrays.asList`-style or
"iterate and collect" methods qualify; "return the array" methods do not.

**Q:** Why does returning `List<T>` fix it where `@SafeVarargs` cannot?
**A:** A `List<T>` is not an array — it has no reifiable component type, so no `checkcast`
on a component type is ever generated. The array, if used internally, stays encapsulated
and never reaches the caller, so the escape that caused the pollution is gone.

**Q:** What is "heap pollution," precisely?
**A:** A variable of a parameterized type referring to an object that is not of that
parameterized type — e.g. a reference typed `String[]` (or `List<String>`) actually
pointing to an `Object[]` (or a `List<Integer>`). It's usually silent until a later
operation trusts the static type and a `checkcast` or implicit cast fails.

**Q:** Why is `@SafeVarargs` only allowed on `static`, `final`, or `private` instance
methods (since Java 9)?
**A:** Because the safety guarantee must hold for the actual code that runs. An overridable
instance method could be overridden by an unsafe implementation, making the superclass's
promise meaningless — so the annotation is restricted to methods that cannot be overridden.

**Q:** Would `Arrays.asList(a, b)` instead of `List.of(a, b)` change anything here?
**A:** Functionally both close the escape and pass the test. `Arrays.asList` returns a
fixed-size mutable view that allows nulls; `List.of` returns an immutable list that
rejects nulls. Neither leaks a `T[]` to the caller, so neither pollutes the heap.
```
