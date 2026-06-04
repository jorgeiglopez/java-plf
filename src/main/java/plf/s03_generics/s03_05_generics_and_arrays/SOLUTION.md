# Solution — The Cast You Never Wrote

## The prediction

`PREDICT_WHICH_THROWS` should be **`STRING_LINE`**.

```java
Object[] a = fillBroken("x", 3);   // line A — fine
String[] b = fillBroken("x", 3);   // line B — throws ClassCastException
```

Identical right-hand side, opposite outcomes. The asymmetry is the whole lesson.

## Ideal solution

`fillSafe` with the `Class<T>` token (matches the scaffold signature):

```java
public static <T> T[] fillSafe(Class<T> componentType, T item, int count) {
    // Array.newInstance returns Object, but its reified component type IS
    // componentType, so this cast is provably safe (EJ27): suppress it locally.
    @SuppressWarnings("unchecked")
    T[] arr = (T[]) Array.newInstance(componentType, count);
    Arrays.fill(arr, item);
    return arr;
}
```

Called as `fillSafe(String.class, "x", 3)`, this returns a genuine `String[]`:
`b.getClass() == String[].class`, and the caller's `checkcast [Ljava/lang/String;`
now **passes**, because the object really is a `String[]`.

The key difference from `fillBroken`: `new Object[n]` produces an array whose
*reified* component type is `Object`; `Array.newInstance(String.class, n)` produces
one whose reified component type is `String`. The token `Class<T>` carries the
runtime type information that `T` itself threw away under erasure.

### Scale up solutions (Optional)

**1. The displacement, proven in bytecode.** Disassembling the *caller* shows the
cast nobody typed:

```
// javap -c  on the calling class:
invokestatic  GenericArray.fillBroken:(Ljava/lang/Object;I)[Ljava/lang/Object;
checkcast     #41   // class "[Ljava/lang/String;"      <-- line B, inserted by the CALLER's compiler
astore_2
```

And the method itself has **no cast at all**:

```
// javap -c GenericArray  ->  fillBroken:
public static <T> T[] fillBroken(T, int);
  0: iload_1
  1: anewarray   java/lang/Object
  4: astore_2
  5: aload_2
  6: aload_0
  7: invokestatic java/util/Arrays.fill
 10: aload_2
 11: areturn                                <-- returns Object[]; zero checkcast
```

Erasure rewrites the signature to `(Object, int) -> Object[]`. The `(T[])` in the
source erased to `Object[] -> Object[]`, a no-op the compiler elided (JVMS 6.5
`checkcast` is only emitted when the target is not already assignable). So the
method can never throw. At the call site the compiler inserts `checkcast` only
when the inferred return type differs from what it already has: line A infers
`T = Object`, return type `Object[]`, already assignable to `Object[]` — **no
cast**. Line B infers `T = String`, return type `String[]`; to honor your declared
`String[] b`, the compiler emits `checkcast [Ljava/lang/String;` — which fails on
an actual `Object[]`. **The caller's compiler wrote the cast; the caller's `.class`
file throws.**

**2. The warning is not noise (EJ27).** `Array.newInstance` is declared to return
`Object`, so `(T[]) ...` is an unchecked cast and `javac -Xlint:unchecked` warns
once. It is safe to suppress because the *runtime* contract of
`Array.newInstance(componentType, n)` guarantees the result's component type is
exactly `componentType` — the cast describes a truth, not a lie. Contrast
`fillBroken`, whose identical-looking `(T[])` cast suppressed a warning that was a
real lie (`Object[]` is not a `String[]`). EJ27's rule: when you can *prove* an
unchecked cast safe, suppress it on the **narrowest possible scope** (a single local
declaration, not the method) and leave a comment saying why.

**3. The token that needs no suppression.** An array constructor reference is
statically type-safe end to end:

```java
public static <T> T[] fillSafe(IntFunction<T[]> generator, T item, int count) {
    T[] arr = generator.apply(count);   // returns T[] directly — no cast, no warning
    Arrays.fill(arr, item);
    return arr;
}
// fillSafe(String[]::new, "x", 3)
```

`String[]::new` has type `IntFunction<String[]>`; the compiler knows the return is
`String[]` with no cast at all, so there is **zero** unchecked warning. This is the
idiom `Stream.toArray(String[]::new)` and `Collection.toArray(String[]::new)` use.
`Array.newInstance` can't match it because its return type is erased to `Object` in
its own signature — reflection has no generic type to hand back.

**4. EJ28's actual recommendation: delete the array.**

```java
public static <T> List<T> fill(T item, int count) {
    return new ArrayList<>(Collections.nCopies(count, item));
}
```

`List<T>` is invariant and erased, but it is *never reified into a typed array*, so
there is no caller-side `checkcast`, no `ClassCastException`, no warning — the bug is
unrepresentable, not patched. **EJ28 in one line: prefer `List<T>` to `T[]`; lists
give you compile-time type safety where generic arrays give you a runtime lie.**

## Why this works (and the gotcha)

Arrays are **reified** (they carry their component type at runtime) and **covariant**.
Generics are **erased** (no type survives) and **invariant**. `T[]` sits on the fault
line between them, and that is why generic array creation is a trap.

The deep gotcha is **displacement**: the crime scene is not where the crime was
committed. `fillBroken`'s body looks guilty (`(T[]) arr`) but is innocent — that
cast vanishes under erasure. The instruction that actually throws is generated, by
the *caller's* compiler, at the assignment — code the programmer never wrote and
can't see without `javap`. And it is conditional: it appears only when the caller
assigns to a type more specific than the erased return (`String[]`), never when the
caller assigns to the erased type itself (`Object[]`). Same call, two compilers'
decisions, two fates.

The fix is to stop faking the array. `new Object[n]` makes an `Object[]`; no cast
turns it into a `String[]`. `Array.newInstance(componentType, n)` makes a real
`String[]`, so the caller's silent `checkcast` finally tells the truth. Better still,
`IntFunction<T[]>` keeps the whole thing statically typed; best of all, EJ28 says
return a `List<T>` and the array — with its reification mismatch — never exists.

## Common mistakes

- **"Both lines throw" / "neither throws."** → Only line B throws. Line A assigns to
  the erased return type (`Object[]`), needs no cast, runs clean. The identical RHS
  fools you into expecting identical behavior; the *declared variable type* decides.
- **"`fillBroken` throws inside the method."** → It can't; its compiled body has no
  cast (erased to `Object[] -> Object[]`). The exception is in the caller.
- **"Just add `@SuppressWarnings` to `fillBroken` and it's fixed."** → Suppressing a
  warning never changes runtime behavior. The cast still fails at the caller. The
  scaffold already suppresses it; the trap is unchanged.
- **"`Array.newInstance(String.class, n)` returns `Object[]`."** → Its *static*
  return type is `Object`, but the actual object's reified component type is
  `String` — `getClass()` is `String[].class`. That's exactly why the cast is safe.
- **"Use `(T[]) new Object[n]` but cast to the right type later."** → There is no
  "later" cast that converts an `Object[]` into a `String[]`; the array's reified
  type is fixed at creation. You must create it as `String[]` from the start.
- **"`new T[n]` would work if Java allowed it."** → It's forbidden precisely because
  `T` is erased — there'd be no reified component type for the array's store check.
  The token exists to supply that missing type.

## Interviewer Q&A

**Q:** Same call expression on two lines — `Object[] a = fillBroken("x",3)` and
`String[] b = fillBroken("x",3)`. Why does only the second throw?
**A:** Erasure makes `fillBroken` return `Object[]`. Line A assigns that to
`Object[]` — no cast needed, runs fine. Line B assigns to `String[]`, so the
compiler inserts `checkcast [Ljava/lang/String;` to honor the declared type; that
check fails at runtime because the object is really an `Object[]`. The outcome is
decided by the *declared variable type* at each call site, not by the method.

**Q:** Where physically is the cast that throws — which `.class` file?
**A:** The **caller's**. `javap -c` on `fillBroken` shows zero `checkcast`; the
caller's class shows `checkcast [Ljava/lang/String;` right after the `invokestatic`.
The caller's compiler generated it; the programmer never typed it.

**Q:** Why does the `(T[])` cast in the method body not throw?
**A:** With an unbounded `T`, it erases to `Object`, so `(T[])` becomes
`Object[] -> Object[]` — already assignable, so JVMS lets `javac` elide the
`checkcast` entirely. The cast is a no-op inside the method; it's "real" only at
call sites that infer a more specific `T`.

**Q:** How do you create a genuinely-typed `T[]` then?
**A:** Supply the runtime type that erasure removed. Either a `Class<T>` token with
`Array.newInstance(componentType, n)` (returns `Object`, needs one *safe* unchecked
cast), or — cleaner — an `IntFunction<T[]>` such as `String[]::new`, which is fully
statically typed and warns not at all. Both work by reintroducing reified type info.

**Q:** Your `fillSafe` still has an unchecked warning. Is that acceptable?
**A:** Yes, if it's the `Array.newInstance` version: `newInstance(componentType, n)`
guarantees the result's reified component type is `componentType`, so the cast is
provably safe. EJ27: suppress on the narrowest scope (one local) with a justifying
comment. The `IntFunction<T[]>` version avoids the warning altogether — prefer it
when the caller can pass `String[]::new`.

**Q:** Why isn't there an `ArrayStoreException` somewhere, like with covariant
arrays?
**A:** Because nothing ever *stores* a wrong element. `fillBroken` only ever puts
`String`s into an `Object[]` — legal. The failure is a *cast of the whole array
reference* (`Object[]` to `String[]`), which is `ClassCastException`, not a per-
element `ArrayStoreException`. Different mechanism, same root cause: the array's
reified type and the claimed type disagree.

**Q:** What does EJ28 actually recommend, and why?
**A:** Prefer lists to arrays. Arrays are covariant + reified, generics invariant +
erased; mixing them (as `T[]` does) yields exactly this displaced, runtime-only
failure plus mandatory unchecked warnings. A `List<T>` is checked entirely at
compile time and is never reified into a typed array, so the whole bug class
disappears. Returning `List<T>` instead of `T[]` makes the displacement
unrepresentable.

**Q:** If `fillBroken` returned to a `var`, what would `var` infer and what happens?
**A:** `var x = fillBroken("x", 3);` infers `x` as `Object[]` (the erased return
type), so no `checkcast` is inserted and it runs fine — same as line A. `var` can't
recover a type erasure discarded; it sees only `Object[]`. The explosion needs a
caller who *names* a more specific type.
