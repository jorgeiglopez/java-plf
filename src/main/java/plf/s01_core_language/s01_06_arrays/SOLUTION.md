# Solution — The Covariant Trap (one fact, two faces)

## Ideal solution

### The prediction table — answer key

| # | Line | Compiles? | Runtime? |
|---|------|-----------|----------|
| 1 | `Object[] a = new String[2];` | **yes** | runs (arrays are covariant; widening `String[]` → `Object[]` is legal) |
| 2 | `a[1] = "ok";` | **yes** | runs (value *is* a `String`; store check passes) |
| 3 | `a[0] = 42;` | **yes** | **throws `ArrayStoreException`** at runtime |
| 4 | `a.getClass() == String[].class` | **yes** | **true** (the array is reified — it remembers it is a `String[]`) |
| 5 | `List<Object> l = new ArrayList<String>();` | **no** | N/A — `javac` error |
| 6 | `static <T> T[] make() { return new T[1]; }` | **no** | N/A — `javac` error |

The surprise is **row 3**: it *compiles* (the static type of `a` is `Object[]`,
and `42` autoboxes to `Object`), then explodes at runtime. The matched surprise
is **row 5**: the structurally identical generic version is caught at *compile
time* instead.

### Exact Java 21 (Corretto) outputs

Running `ArrayCovariance.main`:

```
runtime component type: [Ljava.lang.String;
ArrayStoreException message: java.lang.Integer
```

Row 3's exception, fully:

```
java.lang.ArrayStoreException: java.lang.Integer
```

The message is the **fully qualified class name of the offending value** —
`java.lang.Integer`, because `42` autoboxed to an `Integer` before the store.

Row 5's `javac` error (exact):

```
error: incompatible types: ArrayList<String> cannot be converted to List<Object>
```

Row 6's `javac` error (exact):

```
error: generic array creation
```

### The blank — completed

`ArrayCovariance.whyNoGenericArray()`:

```java
public static String whyNoGenericArray() {
    return "new T[] is forbidden because the array store check needs a reified "
         + "(runtime-known) component type to validate every write, but T is "
         + "erased at runtime, so there is no component type for the check to "
         + "compare against.";
}
```

Any wording is correct that connects the two blanks as:
**needs → a reified / runtime-known component type**; **but `T` is → erased /
gone at runtime**.

### Scale up solutions (Optional)

1. **Per-store, not per-array.** The covariant *assignment* (`Object[] a = new
   String[2]`) is checked once and statically; it is always legal. The
   `aastore` bytecode then runs a **dynamic check on every individual write**,
   comparing the value's class against the array's reified component type.
   `"ok"` is a `String`, so it passes; `42` (boxed to `Integer`) is not, so it
   throws. The check is *per element store and value-typed*, not a blanket "you
   can't write to a covariant array." Devs who think covariance makes the array
   read-only are wrong.
2. **The autoboxing tell.** The message is `java.lang.Integer`, not `int` and
   not `java.lang.Object`. Arrays hold references, so `42` is autoboxed to an
   `Integer` *before* the store; the store check sees an `Integer` instance and
   reports its runtime class. The static type at the call site is irrelevant —
   the JVM only knows the actual object.
3. **Generics the other way.** `List<? extends Object> l = new
   ArrayList<String>();` **compiles** — the wildcard makes the reference
   covariant on purpose. But you **cannot** `l.add("x")` (or add anything except
   `null`): with `? extends`, the compiler can't prove your value fits the
   unknown element type, so `add` is statically rejected. That is the static
   mirror of `ArrayStoreException` — generics forbid the unsafe *write at
   compile time* rather than catching it at runtime. Plain (non-wildcard)
   `List<Object>` vs `List<String>` is invariant precisely so a write through
   the wrong-typed reference can never happen.

## Why this works (and the gotcha)

There is **one** problem here and **two** solutions:

> If you can widen a `String[]` to `Object[]` (or a `List<String>` to a
> `List<Object>`), what stops a bad write — storing an `Integer` where only
> `String`s belong?

- **Arrays** are *covariant* (the widening is allowed) and *reified* (the array
  object carries its component type at runtime — `getClass()` returns
  `String[].class`). So Java permits the widening and inserts a **runtime store
  check** on every write. The cost: a whole class of bugs slips past the
  compiler and detonates as `ArrayStoreException` in production.

- **Generics** are *invariant* (the widening is simply forbidden) and *erased*
  (no element type survives to runtime, so a runtime store check is
  *impossible*). With erasure, the only place to enforce safety is the compiler
  — so the language makes generics invariant and rejects row 5 statically.

**The senior insight (the kill shot):** `new T[]` being illegal is *the same
fact viewed a third time*. An array can only exist if it has a reified component
type to run its store check against. `T` is erased — there is no runtime
component type — so a `T[]` literally cannot perform the check that defines what
an array *is*. Hence `javac` says **`generic array creation`**. Covariance, the
runtime `ArrayStoreException`, and the no-`new T[]` rule are one mechanism
(reification) seen from three angles. If you can derive the `new T[]` ban from
the store check, you understand reification; if you just recite "arrays
covariant, generics invariant," you've memorized a sentence.

The teeth-row (row 2) exists to kill the lucky guesser: `a[1] = "ok"` succeeds,
proving the check is per-store and value-typed, not a blanket write ban.

## Common mistakes

- **"Row 3 won't compile."** → It compiles fine; the static type is `Object[]`
  and `42` is an `Object`. The check is deferred to runtime — that's the trap.
- **"Row 5 throws `ArrayStoreException` too."** → No. Generics are erased, so
  there's nothing to check at runtime; the error is a *compile* error.
- **"The exception message is `int` or `java.lang.Object`."** → It's
  `java.lang.Integer` — `42` autoboxes, and the check reports the value's actual
  runtime class.
- **"`a.getClass()` is `Object[].class`."** → It's `String[].class`. Arrays are
  reified; the runtime type is the creation type, regardless of the reference's
  static type.
- **"Covariant arrays are effectively read-only."** → No — row 2 writes
  successfully. The check is per *value*, per *store*; a correctly-typed value
  goes in fine.
- **"`new T[]` fails because of generics being invariant."** → It fails because
  `T` is *erased*; there's no reified component type for the store check. The
  exact `javac` message is `generic array creation`, not anything about
  variance.
- **"`List<? extends Object> l = new ArrayList<String>()` lets me `add`."** → It
  compiles, but `add` (other than `null`) is rejected — the producer/`extends`
  side is read-only for writes.

## Interviewer Q&A

**Q:** Why does `Object[] a = new String[2]; a[0] = 42;` compile but throw at
runtime?
**A:** Arrays are covariant, so widening `String[]` to `Object[]` is legal and
`42` (an `Object` after autoboxing) is statically assignable — it compiles. But
arrays are *reified*: the object knows it's really a `String[]`, and the JVM runs
a dynamic store check on each write. An `Integer` isn't a `String`, so it throws
`ArrayStoreException` at runtime.

**Q:** What is the *exact* exception and its message?
**A:** `java.lang.ArrayStoreException` with message `java.lang.Integer` — the
fully qualified class name of the offending value (boxed, hence `Integer` not
`int`).

**Q:** Why doesn't `List<Object> l = new ArrayList<String>();` compile, and what
does `javac` say?
**A:** Generics are *invariant*: `ArrayList<String>` is not a subtype of
`List<Object>`, even though `String` is a subtype of `Object`. `javac` reports
`incompatible types: ArrayList<String> cannot be converted to List<Object>`.
Invariance exists precisely so you can't get a typed reference through which an
unsafe write would be possible.

**Q:** Generics solve the same "bad write" risk as arrays — why a *static* check
instead of a runtime one like `ArrayStoreException`?
**A:** Because generics are *erased* — no element type survives to runtime, so a
runtime store check is impossible. The only place left to enforce safety is the
compiler, so the language forbids the unsafe widening (invariance) up front.

**Q:** So why is `new T[]` a compile error, and what's the exact message?
**A:** `error: generic array creation`. An array needs a reified component type
to perform its store check; `T` is erased, so there's no runtime component type
to check against. The same reification that makes `ArrayStoreException` possible
is exactly what `T[]` lacks — so it can't exist. It's the covariance fact viewed
a third time.

**Q:** Then how do you create a generically-typed array when you must?
**A:** You can't write `new T[n]` directly. You either create an `Object[]` and
cast to `T[]` (suppressing the unchecked warning, accepting a possible
`ClassCastException` on misuse), or — cleanly — pass a
`Class<T>`/`IntFunction<T[]>` and use `Array.newInstance(componentType, n)` or
`stream.toArray(String[]::new)`. All of them work by supplying a *reified*
component type that `T` alone can't provide.

**Q:** `a[1] = "ok"` succeeds but `a[0] = 42` throws — both through the same
`Object[]` reference. What does that prove?
**A:** The store check is **per element store and value-typed**, not a property
of the reference or a blanket ban on writing to a covariant array. Each write
compares the actual value's class to the array's reified component type:
`String` passes, `Integer` fails.

**Q:** Does `List<? extends Object> l = new ArrayList<String>();` compile, and
can you `add` to it?
**A:** It compiles — `? extends` makes the reference covariant deliberately. But
you can't `add` anything except `null`: with an upper-bounded wildcard the
compiler can't prove your value matches the unknown element type. It's the
compile-time analogue of `ArrayStoreException` — the unsafe write is blocked
statically instead of at runtime.

**Q:** One-line summary an interviewer wants to hear?
**A:** Arrays: covariant + reified → unsafe writes caught at *runtime*
(`ArrayStoreException`). Generics: invariant + erased → unsafe writes caught at
*compile time*; and because `T` is erased, `new T[]` can't exist at all — one
reification fact, three consequences.
