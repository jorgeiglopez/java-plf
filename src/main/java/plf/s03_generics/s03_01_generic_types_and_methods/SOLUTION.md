# Solution — The Bound That Eats Inference

## Ideal solution

**Task 1 — the prediction.** The line

```java
Object x = MaxBound.max("hello", new StringBuilder("hello"));
```

is a **compile error**, and javac blames the **arguments**, not the bound. Under
`javac --release 21`:

```
error: method max in class MaxBound cannot be applied to given types;
  required: T,T
  found:    String,StringBuilder
  reason: inference variable T has incompatible bounds
    equality constraints: String,StringBuilder
    lower bounds: StringBuilder,String
  where T is a type-variable:
    T extends Comparable<T> declared in method <T>max(T,T)
```

The one-line reason to commit in `PREDICTED_REASON`:

> `T` must satisfy `T extends Comparable<T>`, but `String` and `StringBuilder`
> force conflicting **equality constraints** on `T`, so no single `T` exists.

**Task 2 — the witness.** Name `T` yourself with an explicit type witness:

```java
public static String maxOfTwoStrings() {
    return MaxBound.<String>max("hello", "world");
}
```

`"world".compareTo("hello") >= 0` is true (`'w' > 'h'`), so `max` returns
`"world"`. The test goes green.

(The unqualified `max("hello", "world")` also compiles, because both arguments are
`String` and inference lands on `T = String` cleanly. The witness is required only
when you *force* a specific `T`, or when both arguments differ. Writing it
explicitly here is the lesson: it makes `T` unambiguous and shows the rescue.)

### Scale up solutions

**1. The witness that still fails.**

```java
MaxBound.<CharSequence>max("hello", "world");
```

```
error: method max in class MaxBound cannot be applied to given types;
  reason: explicit type argument CharSequence does not conform to declared
          bound(s) Comparable<CharSequence>
```

Both arguments *are* `CharSequence`, so the arguments are fine — but the **bound**
now does the rejecting. `CharSequence` does not implement
`Comparable<CharSequence>` (it implements nothing comparable at all), so it fails
`T extends Comparable<T>`. Note how this error explicitly names the bound, whereas
Task 1's error named the arguments — same underlying constraint
(`T extends Comparable<T>`), two completely different messages depending on whether
`T` was *inferred* or *supplied*.

**2. Prove it's the bound, not the types.**

```java
static <T> T pick(T a, T b) { return a; }
// ...
Object o = pick("hello", new StringBuilder("hello")); // compiles
```

This compiles. With no bound, the two arguments produce only **upper-bound**
(lower-bound, in JLS direction) constraints, and inference computes the *least
upper bound*: `T = CharSequence & Serializable` (the intersection both types
share). The identical arguments that killed `max` are accepted by `pick` — proof
that the recursive bound, not the argument types, is what made `max` fail. The
bound turned widenable constraints into rigid equality constraints.

**3. A real recursive-bound type.** A type satisfies `T extends Comparable<T>`
only if it compares to *its own kind*:

```java
class Version implements Comparable<Version> { /* compareTo(Version) */ }   // OK
class BadVersion implements Comparable<String> { /* compareTo(String) */ }  // NOT OK
```

`max(new Version(...), new Version(...))` compiles. `max(new BadVersion(...), ...)`
does not: `BadVersion` is `Comparable<String>`, not `Comparable<BadVersion>`, so
`T = BadVersion` fails `T extends Comparable<T>`. Enums get this for free — the
compiler generates `Suit implements Comparable<Suit>` — which is exactly why
`Collections.sort`, `Collections.max`, and `TreeSet` accept enums and any
self-comparable type but reject asymmetric `Comparable` implementations.

## Why this works (and the gotcha)

`T extends Comparable<T>` is a **recursive (F-bounded) type parameter**: `T`
appears inside its own bound. Generic inference normally merges argument types by
their *least upper bound* — that's why `<T> T pick(T, T)` happily infers
`CharSequence & Serializable`. But when `T` sits inside `Comparable<T>`, that
position is **invariant**: `Comparable<String>` and `Comparable<StringBuilder>`
are unrelated types, so the arguments don't generate widenable bounds — they
generate **equality** constraints `T = String` and `T = StringBuilder`. Two
unequal equality constraints have no common solution, so inference reports
*incompatible bounds*.

The gotcha is the misdirection: the error reads `found: String,StringBuilder` and
`equality constraints: String,StringBuilder`, so it looks like an argument-type
problem. Developers "fix" it by upcasting the arguments to `CharSequence` — which
produces a *second*, different error (Scale up 1) because `CharSequence` fails the
bound. The actual cause is the bound's invariance, and the actual fix is to either
name a concrete self-comparable `T` with a witness, or pass two arguments of the
same self-comparable type.

The diamond operator and "inference picks the supertype" intuition are correct for
*unbounded* and *covariant* positions; the recursive bound is precisely the case
where that intuition breaks.

## Common mistakes

- **"It infers `CharSequence`."** No — `CharSequence` isn't `Comparable` and the
  bound is invariant; inference produces conflicting equality constraints and
  fails. Even if you *write* `<CharSequence>`, it's rejected by the bound.
- **Blaming the arguments.** The error says `found: String,StringBuilder`, but the
  arguments are individually valid (`String` is `Comparable<String>`). The conflict
  is that they demand *different* values for the *same* `T`.
- **"Add `default` / upcast to the common type to fix it."** Upcasting both to
  `CharSequence` swaps one error for another (fails the bound). The fix is a
  witness or matching argument types.
- **Confusing the witness's job.** A type witness `Class.<Type>method(...)` doesn't
  *cast* the result; it *names* the type variable so inference is bypassed. With
  `<String>` it's accepted; with `<CharSequence>` the bound rejects it.
- **Assuming `T extends Comparable<? super T>` would change Task 1.** It relaxes
  the bound for *subtypes' comparators*, but `String` and `StringBuilder` still
  generate conflicting constraints for a single `T`; the mixed call still fails.

## Interviewer Q&A

**Q:** What does `<T extends Comparable<T>>` mean, and why not just
`<T extends Comparable>`?
**A:** It's a recursive (F-bounded) bound: `T` must be comparable *to its own
type*. Raw `Comparable` would allow `T` to compare against anything (or nothing
type-safe), losing the guarantee that `a.compareTo(b)` takes a `T`. The recursive
form is what `Collections.max`, `sort`, and `TreeSet` rely on for type-safe
ordering.

**Q:** Why does `max("hello", new StringBuilder("hello"))` fail to compile?
**A:** `T` appears invariantly inside `Comparable<T>`, so the two arguments impose
*equality* constraints `T = String` and `T = StringBuilder`. They can't both hold,
so inference reports incompatible bounds. It's not that the arguments are
individually invalid — it's that they require different values for one `T`.

**Q:** The error blames the arguments. Is that the real cause?
**A:** No — it's the bound's invariance. Without the bound (`<T> T pick(T,T)`) the
exact same arguments infer `CharSequence & Serializable` and compile. The bound
converts mergeable upper-bound constraints into unmergeable equality constraints.

**Q:** How do you make a call like that compile?
**A:** Either pass two arguments of the same self-comparable type, or supply an
explicit type witness naming a concrete `T`, e.g.
`MaxBound.<String>max("hello", "world")`. The witness bypasses inference.

**Q:** Why does `MaxBound.<CharSequence>max("hello", "world")` fail even though
both arguments are `CharSequence`?
**A:** The arguments conform, but `CharSequence` doesn't implement
`Comparable<CharSequence>`, so it violates `T extends Comparable<T>`. The error
names the *bound* this time, not the arguments — different message, same
constraint.

**Q:** What exactly does a type witness do — is it a cast?
**A:** No. `Class.<Type>method(args)` supplies the type argument explicitly,
short-circuiting inference. It doesn't convert values; it tells the compiler what
`T` is. If the supplied type violates the bound, you still get a compile error.

**Q:** How does this relate to enums and `Collections.sort`?
**A:** The compiler generates `enum E implements Comparable<E>`, so enums satisfy
the recursive bound automatically — which is why `sort`, `max`, and `TreeSet`
accept them. A class declared `implements Comparable<SomeOtherType>` (asymmetric)
fails the same bound and is rejected at the call site, not at runtime.

**Q:** After erasure, what's the signature of `max`, and where's the `checkcast`?
**A:** Erasure replaces `T` with the erasure of its leftmost bound, so `max`
erases to `Comparable max(Comparable, Comparable)` (not `Object`). At a call site
returning `String`, the compiler inserts a `checkcast String` on the result.
That's the bytecode footprint of the recursive bound: `Comparable`, not `Object`,
in the erased signature.

**Q:** Would `<T extends Comparable<? super T>>` change the mixed-types call?
**A:** It's the more general, recommended bound (it lets `T` be ordered by a
comparator defined on a supertype), but it doesn't rescue
`max(String, StringBuilder)` — a single `T` still can't equal both. It mainly
matters when `T` extends a type that already implements `Comparable`.
