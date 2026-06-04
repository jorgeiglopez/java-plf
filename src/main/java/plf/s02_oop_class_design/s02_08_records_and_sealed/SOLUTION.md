# Solution — The Switch That Lies

## Ideal solution

### Stage 1 — the safety net (no `default`)

`SealedAreaStage1.area` is a switch **expression** with no `default`:

```java
sealed interface Shape permits Circle, Rectangle {}
record Circle(double radius) implements Shape {
    Circle {
        if (radius <= 0) throw new IllegalArgumentException("radius must be > 0, got " + radius);
    }
}
record Rectangle(double width, double height) implements Shape {}

static double area(Shape shape) {
    double result = switch (shape) {        // no default
        case Circle c    -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
    };
    return result;
}
```

Now add a third permitted subtype but leave the switch untouched:

```java
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Triangle(double base, double height) implements Shape {}
// ... area(...) still only handles Circle and Rectangle ...
```

`javac` (Java 21, Amazon Corretto 21.0.11) refuses to compile:

```
error: the switch expression does not cover all possible input values
```

That is the safety net: a *compile-time event*, not a runtime surprise. You
cannot ship the gap.

### Stage 2 — the lie (`default -> 0.0`)

`SealedAreaStage2.area` is identical except for the defensive branch:

```java
static double area(Shape shape) {
    return switch (shape) {
        case Circle c    -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        default          -> 0.0;            // the "safe" branch
    };
}
```

Add the very same `Triangle` and the compiler says **nothing** — the `switch`
was already exhaustive because of `default`. At runtime:

```
area(new Triangle(4.0, 3.0))  ->  0.0      // real area is 6.0
area(new Circle(1.0))         ->  3.141592653589793
```

`Triangle(4,3)` should be `0.5 * 4 * 3 = 6.0`. It returns `0.0`. The test
`defaultBranchLiesForTriangle` asserts the real `0.0` against your prediction
once you uncomment Task 3 — that red bar is the scar.

### Scale up solutions

1. **Make the default honest.** Replace `default -> 0.0` with
   `case Triangle t -> 0.5 * t.base() * t.height();`. The switch is now exhaustive
   *by enumeration*, not by `default`. The payoff: if a future `Pentagon` is
   added to `permits` and you forget it, you are back to the Stage-1
   compile error — `the switch expression does not cover all possible input
   values` — instead of a silent `0.0`. Enumerated cases keep the safety net;
   `default` discards it.
2. **Compact-constructor guard.** Writing `this.radius = radius;` inside the
   compact constructor fails to compile:

   ```
   error: cannot assign a value to final variable radius
   ```

   A compact constructor receives the components as parameters, may *validate or
   normalize* them, and the canonical assignment `this.radius = radius;` is
   inserted **implicitly** at the end. The component fields are `final`, so an
   explicit assignment is both illegal and redundant. You may reassign the
   *parameter* (`radius = Math.abs(radius);`) to normalize, but never the field.
3. **`non-sealed` reopens the wound.** Marking a permitted subtype `non-sealed`
   lets *anyone* extend it with subclasses the compiler cannot enumerate. The set
   of possible runtime types is no longer finite/known, so the no-default switch
   can no longer be proven exhaustive — you are forced to add a `default` (or a
   catch-all pattern), which reintroduces exactly the blind spot from Stage 2.
   `non-sealed` trades exhaustiveness for open extensibility.

## Why this works (and the gotcha)

A `sealed` type declares a **closed, compiler-known** set of direct subtypes via
`permits`. A pattern `switch` over that type can be checked for exhaustiveness:
the compiler walks every permitted subtype and verifies a matching `case`. Miss
one and you get `the switch expression does not cover all possible input values`.
This is the entire value proposition of pairing sealed types with switches —
adding a subtype becomes a *compile error you must resolve*, not a runtime
fall-through.

The gotcha is reflexive defensiveness. A `default ->` branch makes the switch
**total by construction**, so the compiler concludes there is nothing left to
check and stops warning you. The new `Triangle` now matches `default` and returns
whatever you put there. The bug isn't loud: a `default -> 0.0` reads as a benign
"empty/edge case" and survives code review, where a thrown exception or a
negative sentinel would have screamed. *You made the bug quiet.* The fix is to
**not** add `default` to a sealed switch when you want the exhaustiveness
guarantee — enumerate the cases and let the compiler be your reviewer.

**Accuracy note (a claim worth correcting):** in Java 21 the exhaustiveness check
applies to pattern `switch` **statements too**, not just expressions — a
statement form without `default` over a sealed type fails with `the switch
statement does not cover all possible input values`. The reason to prefer the
**expression** form here is not that the statement "dodges" the check; it is that
an expression must produce a value, which we assign/return and feed straight into
`assertEquals`, so the lie surfaces as a concrete failing number rather than
something you have to print.

## Common mistakes

- **Adding `default -> 0.0` "to be safe."** It removes the compile-time
  exhaustiveness check — the opposite of safe. New subtypes vanish into it.
- **Picking a loud sentinel like `-1.0`.** A negative area screams "bug" in
  review and never ships. The dangerous default is a *plausible* one (`0.0`) that
  reads as an edge case. (This exercise uses `0.0` on purpose.)
- **`this.radius = radius;` in a compact constructor.** Illegal — components are
  `final` and assigned implicitly. Reassign the *parameter* to normalize instead.
- **Assuming a `switch` statement escapes the check.** In Java 21 it does not;
  both forms enforce exhaustiveness over a sealed type.
- **Thinking records are deeply immutable.** Components are `final`, but a
  component of a mutable type (e.g. `int[]`) is *not* defensively copied — the
  record is only *shallowly* immutable.

## Interviewer Q&A

**Q:** What does `sealed ... permits` buy you that a plain interface does not?
**A:** A closed, compiler-known set of permitted subtypes. That enables
exhaustiveness checking in pattern `switch` (and `instanceof` reasoning) and
documents the full hierarchy in one place. Every permitted subtype must be
`final`, `sealed`, or `non-sealed`, and must be in the same module (or same
package if unnamed).

**Q:** A switch expression over a sealed type compiles with no `default`. You add
a new permitted subtype. What happens?
**A:** Compile error: `the switch expression does not cover all possible input
values`. The compiler forces you to handle the new case — that is the safety net.

**Q:** Why does adding a `default` branch defeat that?
**A:** `default` makes the switch total by construction, so the compiler considers
it already-exhaustive and stops checking. A new subtype silently falls into
`default` at runtime. You trade a compile-time error for a runtime wrong answer.

**Q:** Is the exhaustiveness check only for switch *expressions*?
**A:** No. In Java 21 a pattern switch *statement* over a sealed type is also
checked (`the switch statement does not cover all possible input values`). We
prefer the expression form here only because it yields a value we can assert on,
not because the statement form is unchecked.

**Q:** Walk me through a compact constructor. Can it assign `this.field`?
**A:** A compact constructor takes the record's components as parameters, lets you
validate/normalize them, and the canonical `this.field = field` assignments are
appended implicitly. You cannot write `this.field = ...` yourself — components are
`final`, so it fails with `cannot assign a value to final variable`. You *can*
reassign the parameter (e.g. `name = name.strip();`) to normalize before the
implicit assignment.

**Q:** Are records immutable? Can two records be equal?
**A:** Records are *shallowly* immutable: the class is implicitly `final` and the
component fields are `final`, but a component of a mutable type isn't
defensively copied, so its contents can still change. Equality is value-based:
the compiler-generated `equals`/`hashCode` compare components, so
`new Rectangle(2,3).equals(new Rectangle(2,3))` is `true` and their hash codes
match.

**Q:** When *should* you use `default` on a sealed switch, and when not?
**A:** Omit `default` when you want the compiler to force you to handle every
subtype (domain logic where missing a case is a bug). Add `default` (or a
catch-all `case Shape s`) only when truly open-ended behavior is intended, or when
a permitted subtype is `non-sealed` and the set is no longer closed — but accept
that you have given up exhaustiveness.

**Q:** How would you give `area` a real `Triangle` case while keeping the
guarantee?
**A:** Add `case Triangle t -> 0.5 * t.base() * t.height();` instead of a
`default`. The switch stays exhaustive by enumeration, so a *future* unhandled
subtype reverts to a compile error rather than a silent fallback.
