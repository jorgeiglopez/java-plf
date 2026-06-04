# Solution — Dispatch Is Decided in Two Different Places

## Ideal solution

The production class `DispatchLab` is already correct — the exercise is to fix the *predictions* in `DispatchPredictionsTest`. The corrected constants:

```java
static final String EXPECTED_SPEAK = "Woof";                       // override -> runtime type Dog
static final String EXPECTED_GREET_HI = "greet(String): hi";       // overload -> compile time, String most specific
static final String EXPECTED_GREET_NULL = "greet(String): null";   // overload -> String still most specific (null fits both)
static final String EXPECTED_REPRODUCE_RUNTIME = "Dog";            // the OBJECT is a Dog at runtime
static final String EXPECTED_CAST = "ClassCastException";          // (Cat) a compiles, throws at runtime
```

Running `DispatchLab.main` prints exactly:

```
1) a.speak()                 -> Woof
2) a.greet("hi")             -> greet(String): hi
3) a.greet(null)             -> greet(String): null
4) a.reproduce() runtime obj -> Dog
5) (Cat) a                   -> threw ClassCastException
```

The full diagnostic table — the model *is* the split between these two columns:

| # | Call | Decided when? | On which type? | Result |
|---|------|---------------|----------------|--------|
| 1 | `a.speak()` | **runtime** (virtual dispatch) | the **object** = `Dog` | `Woof` |
| 2 | `a.greet("hi")` | **compile time** (overload resolution) | the **reference** = `Animal` | `greet(String): hi` |
| 3 | `a.greet(null)` | **compile time** (most-specific) | the **reference** = `Animal` | `greet(String): null` |
| 4 | `var x = a.reproduce()` static type | **compile time** (declared return) | the **reference** = `Animal` | static type `Animal` |
| 4 | `x.getClass()` at runtime | **runtime** (the actual object) | the **object** = `Dog` | `Dog` |
| 5 | `Cat c = (Cat) a;` | compiles (both share `Animal`); **runtime** check fails | object is `Dog`, not `Cat` | `ClassCastException` |

Row 4 is deliberately a two-faced row. The **runtime class** of the returned object is `Dog` — that is what the harness and the test print (`x.getClass().getSimpleName()` → `Dog`). But the **static type** the compiler gives `var x` is `Animal`, the *declared* return type of `Animal.reproduce()` seen through an `Animal` reference. So `x` *is* a `Dog` at runtime, yet you cannot call a `Dog`-only method on `x` — the compiler only knows it as an `Animal`. Same object, two type views; the test asserts the runtime view, the compile-fail aside (below) probes the static view.

### The covariant compile-fail aside (markdown only — do not put in a `.java` file)

`Dog reproduce()` covariantly overrides `Animal reproduce()`: a subclass may narrow the return type. That override is real and useful — *if you call it through a `Dog` reference*. Through the `Animal` reference `a`, the compiler only sees the base signature, return type `Animal`:

```java
Animal a = new Dog();
var x = a.reproduce();   // OK: static type of x is Animal
Dog d = a.reproduce();   // DOES NOT COMPILE
```

`javac` (Java 21) rejects the second line:

```
error: incompatible types: Animal cannot be converted to Dog
        Dog d = a.reproduce();
                          ^
```

Even though the object returned really is a `Dog`, the *compile-time* return type seen through `a` (an `Animal`) is `Animal`, and `Animal` is not assignable to `Dog` without a cast. The covariant narrowing is invisible through the base reference. This is the covariant-return sting: covariance lives on the **static signature** you call through, not on the runtime object you get back. (`Dog d = ((Dog) a).reproduce();` or `Dog d = new Dog().reproduce();` would compile — now the static type at the call site is `Dog`.)

### Scale up solutions (Optional)

**1. `null` resolution.** Overload resolution (JLS §15.12.2) picks the **most specific applicable** method. `null` is assignable to both `Object` and `String`, so both `greet(Object)` and `greet(String)` are applicable; `String` is a subtype of `Object`, so `greet(String)` is strictly more specific and wins — at compile time, with no runtime involvement. To make `a.greet(null)` *fail to compile*, add a third overload whose parameter type is **unrelated** to `String`, e.g. `greet(Integer)`. Now `null` fits `String` and `Integer`, neither is more specific than the other, and you get `error: reference to greet is ambiguous / both method greet(String) ... and method greet(Integer) ... match`. (Adding `greet(CharSequence)` would *not* break it: `String` is still strictly more specific than `CharSequence`.)

**2. `var` is not dynamic.** `var` is compile-time type inference, not a runtime/dynamic type: `x` gets the *declared* type of the expression (`Animal`, the return type of `Animal.reproduce()` seen through `a`), so `x.someDogOnlyMethod()` won't compile — while `x.getClass()` reports the actual object, `Dog`, at runtime. One sentence: **`var` infers the static type of the initializer; `getClass()` reports the runtime object — they can differ.**

**3. `@Override` as a tripwire.** `@Override String greet(String s)` in `Dog` is a *legal override of the same overload* — it does not change which overload `a.greet("hi")` selects (still chosen at compile time on `Animal` as `greet(String)`); it only changes which *body* runs once selected, and through an `Animal` reference that body is `Dog`'s overriding one if `a`'s object is a `Dog`. By contrast, `@Override String greet(CharSequence s)` **fails to compile**: there is no `greet(CharSequence)` in `Animal` to override, so `@Override` errors with `method does not override or implement a method from a supertype`. (Without `@Override` it would silently become a *new third overload* — and then `greet(null)` could turn ambiguous: `@Override` is your tripwire against exactly that mistake.)

**4. Field shadowing vs method overriding.** Add `String species = "Dog";` to `Dog`:

```
new DogF().species            -> Dog
((AnimalF) new DogF()).species -> Animal
```

Fields are **not** polymorphic. A field access is resolved on the **static type** of the reference, exactly like overload selection — `((Animal) dog).species` reads `Animal.species` ("Animal"), `dog.species` reads `Dog.species` ("Dog"), for the *same object*. This is field *shadowing*, not overriding: both fields coexist, and which one you see depends on the compile-time type of the reference. It is the same early-binding rule as overload resolution, made visible on state instead of behavior.

## Why this works (and the gotcha)

Java resolves a member access in **two stages**, and they happen at **two different times**:

1. **Compile time — selection.** Using the *static* (declared) type of the reference, `javac` picks a method *signature*: for an overloaded name it runs overload resolution (most specific applicable wins); for a field it picks the field declared in that static type. The runtime object is irrelevant here — the bytecode already names a fixed signature.
2. **Runtime — dispatch.** *Only for instance methods*, the JVM then performs virtual dispatch (`invokevirtual`): among classes that override the selected signature, the **object's actual class** decides which body runs. Fields and `static` methods skip this step entirely.

So for `a.speak()`: stage 1 selects `Animal.speak()` (only one signature); stage 2 dispatches to `Dog.speak()` because the object is a `Dog`. Override = late binding, you see `Woof`.

For `a.greet("hi")` and `a.greet(null)`: stage 1 must *choose among overloads*, and it does so purely from the static type `Animal` — picking `greet(String)` as most specific. There is nothing for stage 2 to redirect (the body of `greet(String)` isn't overridden in `Dog`), so the runtime type never enters. Overload = early binding.

**The gotcha is the asymmetry under one reference.** `a` is typed `Animal` but points at a `Dog`. The *override* (`speak`) follows the `Dog`; the *overload* (`greet`) follows the `Animal`. A developer who internalized "the real type always wins" predicts `greet(Object)` for `greet("hi")` and especially confidently writes `greet(Object)` for `greet(null)` — and is wrong both times. `greet(null)` is the deep cut: most people assume `null` "has no type" so it falls to the broadest `Object`; in fact `null` is applicable to *every* reference type, and most-specific selection then prefers the narrowest, `String`.

`var x` and `(Cat) a` close the loop on the same theme: both are compile-time-typed. `var` takes the static type of the initializer (`Animal`), so the covariant narrowing on `Dog.reproduce()` is invisible through `a` — `Dog d = a.reproduce();` won't compile. `(Cat) a` is a *downcast*: the compiler allows it because `Dog` and `Cat` share the ancestor `Animal` (so it *could* be a `Cat`), but at runtime the object is a `Dog`, the check fails, and you get `ClassCastException`.

Encapsulation and abstraction frame this exercise but are **not** tested here (stated honestly): `Animal` hides its state in the `private final String species`, touched only by `speak()`, and `Animal` is the abstraction the caller programs against. Those are the *why* of polymorphism (program to the base type); the *mechanics* — when and on what type each call resolves — are what this drill makes you predict.

## Common mistakes

- **"The runtime type always wins."** Only for *overridden instance methods*. Overload selection, field access, and `static` method calls all bind on the *static* type — at compile time.
- **Predicting `greet(Object)` for `a.greet("hi")`.** The argument `"hi"` is a `String`; the static type `Animal` declares both overloads, and `greet(String)` is most specific. The runtime `Dog`-ness of `a` is irrelevant.
- **Predicting `greet(Object)` for `a.greet(null)` — the confident wrong answer.** `null` is applicable to both overloads; most-specific selection prefers `greet(String)`. People assume `null` defaults to `Object`; it does not.
- **Thinking `Dog d = a.reproduce();` compiles "because the object is a Dog".** The *compile-time* return type through an `Animal` reference is `Animal`; covariant narrowing is invisible through the base ref. It needs a cast or a `Dog`-typed call site.
- **Expecting `var x` to be a `Dog`.** `var` infers the *static* type of the initializer (`Animal`). `x.getClass()` is `Dog` at runtime, but the compiler only lets you use `Animal`'s API.
- **Expecting `(Cat) a` to be a compile error.** `Cat` and `Dog` share `Animal`, so the downcast is *plausible* and compiles; the mismatch is caught at runtime as `ClassCastException`. (A cast to an *unrelated* class with no shared ancestor would be the compile error.)
- **Adding a third `greet` overload "to be thorough".** `greet(Integer)` makes `a.greet(null)` ambiguous — a compile error. The trap requires exactly two overloads on one inheritance line.

## Interviewer Q&A

**Q:** `Animal a = new Dog(); a.speak()` calls `Dog.speak()`. Why?
**A:** Overriding uses *late* (dynamic) binding. The compiler selects the `speak()` signature from `a`'s static type `Animal`; at runtime `invokevirtual` dispatches to the overriding body in the object's actual class, `Dog`. The object's runtime type decides which body runs.

**Q:** Same `a`, now `a.greet("hi")` where `Animal` declares `greet(Object)` and `greet(String)`. Which runs, and when is that decided?
**A:** `greet(String)`, decided at **compile time**. Overload resolution uses the static type of the reference (`Animal`) and the static type of the argument (`String`), picking the most specific applicable overload. The runtime type of `a` plays no part — this is early binding.

**Q:** What does `a.greet(null)` resolve to, and why does it surprise people?
**A:** `greet(String)`. `null` is assignable to both `Object` and `String`, so both are applicable; `String` is strictly more specific (a subtype of `Object`), so it wins per JLS §15.12.2.5. People expect `greet(Object)` because they think `null` is "typeless" and falls to the broadest parameter — but most-specific selection prefers the narrowest reference type.

**Q:** When would `a.greet(null)` *fail to compile*?
**A:** When two applicable overloads are equally specific — e.g. add `greet(Integer)`. `null` fits `String` and `Integer`, neither is a subtype of the other, so the call is ambiguous: `error: reference to greet is ambiguous`. Adding `greet(CharSequence)` would *not* break it, since `String` is still strictly more specific than `CharSequence`.

**Q:** `Dog reproduce()` overrides `Animal reproduce()`. What's that called, and why won't `Dog d = a.reproduce();` compile when `a` is an `Animal`?
**A:** Covariant return type — an override may narrow the return type to a subtype. But overload/return-type info is read from the *static* type of the call site. Through an `Animal` reference the compiler sees the base return type `Animal`, and `Animal` isn't assignable to `Dog` without a cast: `error: incompatible types: Animal cannot be converted to Dog`. The narrowing is only visible through a `Dog`-typed reference.

**Q:** `var x = a.reproduce();` — what is `x`'s type, and what does `x.getClass()` return?
**A:** `var` infers the *static* type of the initializer, which is `Animal` (the declared return type through `a`). So you can only use `Animal`'s API on `x`. At runtime `x.getClass()` returns `Dog`, because the actual object is a `Dog`. Same object, two type views: compile-time `Animal`, runtime `Dog`.

**Q:** `Cat c = (Cat) a;` — does it compile? What happens at runtime?
**A:** It compiles: `Cat` and `Dog` both extend `Animal`, so the downcast is *type-plausible* and the compiler permits it. At runtime the object is a `Dog`, not a `Cat`, so the checked cast throws `ClassCastException`. To do it safely, guard with a pattern: `if (a instanceof Cat c) { ... }`.

**Q:** Contrast overriding with field shadowing. If `Dog` declares its own `species` field, what does `((Animal) dog).species` read?
**A:** It reads `Animal`'s `species`. Fields are **not** polymorphic — a field access binds on the *static* type of the reference, exactly like overload selection. `((Animal) dog).species` sees `Animal.species`, `dog.species` sees `Dog.species`, for the same object. Both fields coexist (shadowing), and the compile-time type chooses. Only instance *methods* are dispatched on the runtime type.

**Q:** Summarize the rule in one sentence an interviewer can grade.
**A:** *Selection* (which overload, which field, which `static` method) happens at compile time on the reference's static type; *dispatch* of an overridden instance method happens at runtime on the object's actual type — so an override binds late and everything else binds early.
