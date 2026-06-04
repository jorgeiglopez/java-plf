# Solution — A default isn't free

## Ideal solution

### The answer key (verbatim Java 21 `javac`, Corretto 21.0.x)

**Snippet A** — two *unrelated* interfaces each supply a `default double area()`.
The compiler has two competing bodies and refuses to choose:

```
error: types Drawable and Measurable are incompatible;
  class Tile inherits unrelated defaults for area() from types Drawable and Measurable
```

(`javac` prints it as a two-line diagnostic: a header line ending in
`are incompatible;`, then an indented continuation
`class Tile inherits unrelated defaults for area() from types Drawable and Measurable`.)

**Fix A** — `Tile` must override `area()` to break the tie. You may delegate to a
chosen superinterface with the qualified-super form:

```java
class Tile implements Drawable, Measurable {
    @Override public double area() { return Measurable.super.area(); } // pick a winner explicitly
}
```

**Snippet B** — byte-twin of A, but `Measurable` now *reabstracts* `area()`
(`double area();`). There is no longer a conflict — but there is also no usable
body, so `area()` is **effectively abstract** and the concrete class must implement it:

```
error: Tile is not abstract and does not override abstract method area() in Measurable
```

**Fix B** — `Tile` supplies a body (there is nothing to delegate to; `Measurable`'s
`area()` is abstract, so `Measurable.super.area()` would itself fail to compile):

```java
class Tile implements Drawable, Measurable {
    @Override public double area() { return 2.0; } // fill the hole; no super to delegate to
}
```

### The runnable payoff (`AreaResolution.java`)

The scaffold ships the *fixed* shape of Snippet A — both supertypes default, and the
concrete class is forced to resolve. The test wants the `Measurable` default (`1.0`):

```java
class AreaResolution implements AreaDrawable, AreaMeasurable {
    @Override
    public double area() {
        return AreaMeasurable.super.area(); // resolve to the chosen superinterface default
    }
}
```

`mvn -q -Dtest=AreaResolutionTest test` then passes. (The scaffold starts red because
its body throws `UnsupportedOperationException("TODO")` — it compiles, it just fails
the assertion until you resolve the conflict.)

### Scale up solutions

1. **Subinterface / most-specific wins.** Make `Measurable extends Drawable` and let
   both still default. Now `javac` is **silent** — clean compile, no override needed:
   ```java
   interface Drawable   { default double area() { return 0.0; } }
   interface Measurable extends Drawable { default double area() { return 1.0; } }
   class Tile implements Drawable, Measurable { } // compiles
   ```
   The "diamond" only barks when the two defaults are *unrelated*. Once one interface
   is a subinterface of the other and overrides the default, `Measurable.area()` is
   *more specific* and silently wins — specificity beats arithmetic. This is exactly
   why a confident "two defaults must collide" prediction is wrong.

2. **Class beats interface.** A concrete (or abstract-with-body) superclass method
   always outranks an interface default — the "class wins" rule:
   ```java
   interface Drawable { default double area() { return 0.0; } }
   abstract class Base { public double area() { return 5.0; } }
   class Tile extends Base implements Drawable { } // compiles; Tile.area() == 5.0
   ```
   This is the deep reason EJ20 still calls abstract classes useful for *skeletal
   implementations*: a real method body in the class hierarchy resolves ambiguity
   that interface defaults alone cannot.

3. **EJ23 — tagged class → sealed hierarchy.** Replace the `int kind` + `switch`
   anti-pattern with a sealed type; `area()` is plain abstract (no default), and a
   `switch` *expression* over the sealed permits-set is exhaustive with **no**
   `default` branch:
   ```java
   sealed interface Shape permits Circle, Square {
       double area();
   }
   record Circle(double r) implements Shape {
       @Override public double area() { return Math.PI * r * r; }
   }
   record Square(double s) implements Shape {
       @Override public double area() { return s * s; }
   }

   static String describe(Shape sh) {
       return switch (sh) {                 // no default: sealed => exhaustive
           case Circle c -> "circle " + c.area();
           case Square s -> "square " + s.area();
       };
   }
   ```
   Add a `Triangle` to `permits` and the `switch` stops compiling until you handle it
   — the compiler enforces the case analysis the tagged class left to runtime.

4. **EJ22 / EJ41 — type vs constant dump.** A *marker* interface (no methods) like
   `Serializable` legitimately **defines a type**: `x instanceof Serializable` is a
   meaningful, compile-time-checkable category, and APIs can demand
   `<T extends Serializable>`. A *constant* interface only holds `static final`
   fields:
   ```java
   interface PhysicalConstants {           // ANTI-PATTERN
       double AVOGADRO = 6.022_140_76e23;
       double BOLTZMANN = 1.380_649e-23;
   }
   ```
   Implementing it pollutes your class's *exported* API with constants you may never
   use, and those leaked names become a compatibility commitment (subclasses inherit
   them too). The fix is a `final` utility class with `private` constructor, a
   `static import`, or an `enum` — none of which leak into the type's API. So: marker
   interface = a real (if empty) type; constant interface = an implementation detail
   masquerading as a type.

## Why this works (and the gotcha)

The trap is the reflex "byte-twins → same result." They differ by one keyword and the
mechanism flips:

- **Snippet A** triggers JLS 8.4.8 / 9.4.1: a class that inherits *two* `default`
  implementations of the same signature from *unrelated* interfaces is ill-formed.
  `javac` will **not** silently pick one — you must override. The fix *breaks a tie*.
- **Snippet B** removes one default by reabstracting it. Now the rule that resolves an
  abstract-vs-default mix kicks in: **the abstract declaration dominates** (there is
  no most-specific *default* — the only override of `Drawable.area()` is an abstract
  one), so the method stays abstract. A `class` that is not `abstract` must implement
  every abstract method, hence the *different* error. The fix *fills a hole*.

That second case **is** EJ21's evolution scar made concrete: a library owner who
"just reabstracts a method upstream" (or whose interface picks up an abstract method)
can silently force a *downstream* implementor that compiled yesterday to fail today —
not with the friendly "unrelated defaults" message, but with "does not override
abstract method." Adding a `default` is *source*-compatible for a single interface;
combine it with a sibling interface or a later reabstraction and it is not free.

The smug reading predicts one error (or two identical ones). Two fire, for opposite
reasons, with opposite fixes. That asymmetry is the whole lesson.

## Common mistakes

- **Predicting "two defaults always collide."** → False. If one interface is a
  *subinterface* of the other and overrides the default, most-specific wins and it
  compiles clean (Scale up 1). The conflict is *conditional*.
- **Predicting the *same* error for A and B because the code "looks identical."** →
  A is `inherits unrelated defaults`; B is `does not override abstract method`.
  Different rule, different fix.
- **Fixing B with `Measurable.super.area()`.** → `error: abstract method area() in
  Measurable cannot be accessed directly`. You can only `X.super.m()` a *concrete*
  (default) method of a *direct* superinterface. In B, `Measurable.area()` is
  abstract — there is nothing to delegate to; you must write a real body.
- **Thinking the abstract class would have collided too.** → "Class wins": a real
  superclass body outranks any interface default (Scale up 2), so it compiles.
- **Treating a constant interface as "defining a type."** → It defines no usable
  type; it dumps constants into your API. Only methodless *marker* interfaces (and
  method-bearing ones) legitimately define types (EJ22/EJ41).

## Interviewer Q&A

**Q:** Two interfaces, each with a `default double area()`, and a class implements
both. What does `javac` do?
**A:** Refuses to compile: `class Tile inherits unrelated defaults for area() from
types Drawable and Measurable` (under a `types ... are incompatible;` header). It
will not auto-pick a winner; the class must override `area()`.

**Q:** How do you override and still call one of the inherited defaults?
**A:** The qualified-super form: `Measurable.super.area()` inside the override. It
must name a *direct* superinterface whose method is concrete (a `default`); you cannot
`super`-call an abstract one.

**Q:** Now reabstract one of them — change `Measurable`'s method to `double area();`.
Same code shape. Does it still fail, and with the same message?
**A:** It fails, but differently: `Tile is not abstract and does not override abstract
method area() in Measurable`. There's no longer a *conflict* (only one default
remains), but the abstract declaration leaves `area()` effectively abstract, so the
concrete class must implement it.

**Q:** Why does the abstract redeclaration "win" over the surviving default?
**A:** When the inherited candidates for a signature include an abstract one and there
is no single *most-specific default* overriding all others, the method is treated as
abstract. Resolution favors the abstract requirement; a concrete class then has to
supply the body. (Contrast: a most-specific *default* from a subinterface would win
and need no override.)

**Q:** This is Item 21 in *Effective Java*. What's the evolution risk in one sentence?
**A:** Adding a `default` lets an interface gain a method without breaking existing
implementors — but it's not free: combined with a sibling interface it can produce an
unrelated-defaults error, and a later reabstraction (or a clashing abstract sibling)
can silently force a previously-compiling implementor to add an override or break.

**Q:** When would you still reach for an abstract class over an interface (EJ20)?
**A:** For a *skeletal implementation* (`AbstractList`, `AbstractMap`): you want to
share real method bodies and state, fix construction, or get the "class wins" tie-
breaking over defaults. But you can only extend one class, so prefer interfaces for
the *type* and offer an abstract skeletal class alongside it.

**Q:** Marker interface vs constant interface — both have effectively no behavior. Why
is one good and one an anti-pattern (EJ22/EJ41)?
**A:** A marker interface (e.g. `Serializable`) *defines a type*: it's a compile-time-
checkable category you can test with `instanceof` and bound generics on. A constant
interface only holds `static final` fields; implementing it leaks those constants into
your class's exported API and into all subclasses, with no type meaning. Use a utility
class, `static import`, or `enum` instead.

**Q:** Sealed hierarchy vs tagged class (EJ23) — what does the compiler give you?
**A:** Exhaustiveness. A `sealed interface Shape permits Circle, Square` lets a
`switch` *expression* cover every case with no `default` branch; add a permitted type
and the `switch` fails to compile until handled. A tagged `int kind` + `switch (kind)`
pushes that check to runtime, where a missing case is a silent bug or a thrown default.
