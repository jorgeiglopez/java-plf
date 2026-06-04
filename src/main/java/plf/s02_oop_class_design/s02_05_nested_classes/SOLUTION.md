# Solution — The list that wouldn't die

## Ideal solution

### The prediction table — answer key (Java 21, Corretto)

| # | construct | body touches | retains `Leak`? | synthetic field present |
|---|-----------|--------------|-----------------|-------------------------|
| 1 | non-static inner, empty body | nothing | **no** | none — no `this$0` emitted |
| 2 | non-static inner | `payload` | **yes** | `Leak this$0` |
| 3 | `static` nested | nothing | **no** | none (never handed an enclosing instance) |
| 4 | lambda | only a local `int` | **no** | `int arg$1` (captures the local, not `Leak`) |
| 5 | lambda | `payload` | **yes** | `Leak arg$1` |

**Rows 1 and 2 are the kill shot.** Same non-static inner class, same enclosing
`Leak`, same construction (`outer.new Inner...()`). Only the *body* differs — and
that alone decides whether the compiled class carries a `Leak this$0` field.
Empty body ⇒ no field ⇒ no leak. The widely-held "inner classes always pin the
outer" is **false on Java 21**.

### Running `Leak.main`

```
plf.s02_oop_class_design.s02_05_nested_classes.Leak$InnerTouchingNothing capturesEnclosing=false
plf.s02_oop_class_design.s02_05_nested_classes.Leak$InnerTouchingPayload capturesEnclosing=true
plf.s02_oop_class_design.s02_05_nested_classes.Leak$StaticNested capturesEnclosing=false
plf.s02_oop_class_design.s02_05_nested_classes.Leak$$Lambda/0x... capturesEnclosing=false
plf.s02_oop_class_design.s02_05_nested_classes.Leak$$Lambda/0x... capturesEnclosing=true
```

(The lambda class-name suffix after `$$Lambda/0x` is a runtime-generated hidden
class id and varies per run — only the boolean is asserted.)

### The blank — completed

`Leak.whyEmptyInnerDoesNotPin()`:

```java
public static String whyEmptyInnerDoesNotPin() {
    return "Constructing an inner class still requires an enclosing Leak (it is "
         + "passed to the synthetic constructor), but javac only emits the "
         + "this$0 field that stores it when the body actually references an "
         + "enclosing member; an empty body references nothing, so the field is "
         + "elided and nothing is retained.";
}
```

Any wording is correct that separates **construction** (always needs an
enclosing instance — it is a constructor parameter) from **retention** (only the
`this$0` field, emitted on use, keeps it alive).

### The single rule (task step 4)

> A nested construct retains its enclosing instance **iff its body references an
> enclosing member**, in which case `javac` stores it in the synthetic `this$0`
> (inner/local classes) or an `arg$N` of the enclosing type (lambdas); `static`
> nested classes never have a `this$0`.

### Scale up solutions (Optional)

1. **Construction vs retention.** From an unrelated class,
   `new Leak.InnerTouchingNothing()` does **not** compile:

   ```
   error: an enclosing instance that contains Leak.InnerTouchingNothing is required
   ```

   The correct form supplies the enclosing instance explicitly:
   `outer.new InnerTouchingNothing()`. So the enclosing `Leak` *is* required at
   construction — it is a hidden first parameter of the inner class's
   constructor (`getDeclaredConstructors()` shows `ctor(Leak)` even for the empty
   inner). The resolution: that argument is consumed by the constructor; it is
   only *kept* (assigned to `this$0`) if the body uses it. Empty body ⇒ argument
   is passed, used for nothing, never stored ⇒ no retention. Construction needs
   it; retention doesn't.

2. **The effectively-final wall.** Reassigning the captured local:

   ```java
   public Runnable lambdaLocalOnly() {
       int local = 7;
       Runnable r = () -> { int x = local; };
       local = 8;   // <-- breaks capture
       return r;
   }
   ```

   ```
   error: local variables referenced from a lambda expression must be final or effectively final
   ```

   (For a local/anonymous *class* the message says "inner class" instead of
   "lambda expression".) The array-covariance "just mutate it" trick fails
   because capture is **by value** of the local at capture time, not by
   reference; allowing post-capture mutation would make the captured copy and the
   live local diverge silently, so the language forbids it. (Capturing
   `this.payload` is different — that captures the *object reference* `this`, and
   `payload`'s contents may still change.)

3. **`Outer.this` vs `this`.** Inside `InnerTouchingPayload.run()`, plain `this`
   is the *inner* instance (the `Runnable`). The enclosing `Leak` is named
   `Leak.this` (qualified-`this`). So `payload` is shorthand for
   `Leak.this.payload`. If the inner class shadows a field, `Leak.this.foo`
   disambiguates the enclosing one from the inner `this.foo`.

4. **EJ25 — one public top-level class per file.** Two `public` top-level classes
   in one file:

   ```
   error: class B is public, should be declared in a file named B.java
   ```

   (One such error per offending public class.) Rule: at most one `public`
   top-level class per source file, and its name must match the file. EJ24's
   rule: **if a nested class doesn't need access to an enclosing instance, make it
   `static`.** Preference order: prefer a `static` member class; use a (non-static)
   inner class only when each instance genuinely needs its enclosing instance;
   use a local class when you also need a name and reuse; use an anonymous class
   (or, better, a lambda) for a one-shot, name-free instance. A non-static inner
   class that didn't need to be one wastes a reference and risks exactly the leak
   in this exercise.

## Why this works (and the gotcha)

There is **one** rule, and it kills two opposite myths at once:

> A nested construct keeps its enclosing `Outer` alive **iff its body touches an
> enclosing member.** When it does, `javac` synthesizes a field to hold the
> enclosing reference — `this$0` for inner/local classes, an `arg$N` of the
> enclosing type for lambdas. When it doesn't, no such field exists, and the
> construct holds nothing.

- **Myth A — "inner classes always pin `Outer`."** False. Row 1 (empty inner)
  has *no* `this$0`. The enclosing instance is required to *build* the inner
  object (it's a constructor parameter — that's why `new Outer.Inner()` won't
  compile), but if the body never uses it, the compiler discards it. Construction
  ≠ retention.

- **Myth B — "lambdas never capture `this`."** False. Row 5 captures `Leak` the
  instant the body reads `payload` (which desugars to `this.payload`). A lambda
  that touches an instance member pins `Outer` exactly like an inner class does.

**The senior insight (the kill shot):** inner classes and lambdas obey the *same*
capture rule — usage-driven retention via a synthetic enclosing-reference field.
The only real difference is the *default*: a lambda's body is the only thing that
can pull in `this`, whereas an inner class is *constructed* with the enclosing
instance in hand (so it's one keystroke away from capturing it, and people forget
the field is still elided when unused). The `static` modifier (EJ24) is the
guarantee: a `static` nested class is structurally incapable of holding `this$0`,
so it can *never* leak the enclosing instance. That is precisely why EJ24 says
"if you can make the member class `static`, do it." The leak in the title is the
`InnerTouchingPayload` / `lambdaTouchingPayload` rows surviving in a long-lived
list and dragging an 8-byte (or 200 MB) `payload` with them.

The teeth-row (row 1) exists to kill the lucky guesser who recites "inner = leak":
an empty inner body retains nothing, proving retention is usage-driven, not a
property of the inner-class keyword.

## Common mistakes

- **"Every non-static inner class pins `Outer`."** → Only if its body references
  an enclosing member. Empty body ⇒ no `this$0` ⇒ no retention (row 1).
- **"Lambdas never capture `this`."** → A lambda that reads an instance member
  (`payload`, i.e. `this.payload`) captures `Leak` (row 5).
- **"`getDeclaredFields()` always shows `this$0` for an inner class."** → It only
  shows `this$0` when the field was emitted, i.e. when the enclosing instance is
  used. The constructor parameter is always there; the field is not.
- **"`static` nested can still leak via `this$0`."** → A `static` nested class is
  never given an enclosing instance; it has no `this$0` and cannot leak `Outer`.
- **"`new Outer.Inner()` should work."** → It doesn't: `error: an enclosing
  instance that contains Outer.Inner is required`. Use `outer.new Inner()`.
- **"I can capture a local and then reassign it."** → No: `local variables
  referenced from a lambda expression must be final or effectively final`.
  Capture is by value at capture time.
- **"Inside the inner class, `this` is the `Outer`."** → `this` is the inner
  instance; the enclosing one is `Outer.this`.

## Interviewer Q&A

**Q:** A non-static inner class and a `static` nested class differ how, in one
sentence?
**A:** A non-static (inner) class is associated with an instance of the enclosing
class and can be *given* a reference to it (`Outer.this`); a `static` nested
class is not, has no `this$0`, and behaves like an ordinary top-level class that
just lives in another class's namespace. EJ24: prefer `static` unless you need
the enclosing instance.

**Q:** Does a non-static inner class always hold a reference to its enclosing
instance?
**A:** No — on Java 21 the synthetic `this$0` field is emitted only if the body
references an enclosing member. The enclosing instance is always *required to
construct* the inner class (it's a constructor parameter, so `new Outer.Inner()`
won't compile), but if the body never uses it, the field is elided and nothing is
retained. Construction ≠ retention.

**Q:** Then how can `new Outer.Inner()` fail to compile while the finished object
holds no `Outer`?
**A:** Those are two different points in time. At construction, `javac` must
thread an enclosing `Outer` into the inner constructor — without one, it can't
build the object, hence the compile error. At retention time, the constructor
only *stores* that argument in `this$0` if the body uses it; an empty body throws
it away. Required to build, not necessarily kept.

**Q:** Do lambdas capture `this`? When?
**A:** Only when the lambda body references an instance member or `this`
explicitly — then it captures the enclosing instance (you'll see an `arg$N` of
the enclosing type). A lambda touching only locals or statics captures no
enclosing reference. So lambdas follow the same usage-driven rule as inner
classes; they just don't get the enclosing instance handed to them at
construction.

**Q:** Why must captured locals be effectively final, and what's the exact error
if they aren't?
**A:** Capture is by *value* of the local at capture time, copied into a
synthetic field; allowing later reassignment would let the copy and the live
variable diverge silently. `javac` enforces it: `local variables referenced from
a lambda expression must be final or effectively final` (or "...from an inner
class..." for a local/anonymous class). Note capturing `this.field` is different —
that captures the object reference, whose contents can still change.

**Q:** Inside a non-static inner class, what does plain `this` refer to, and how
do you reach the enclosing instance?
**A:** `this` is the inner instance; the enclosing instance is `Outer.this`
(qualified `this`). If a field name is shadowed, `Outer.this.field` selects the
enclosing one and `this.field` the inner one.

**Q:** What's the practical consequence of all this for memory and for EJ24?
**A:** A non-static inner class or capturing lambda stored in a long-lived
structure can pin its entire enclosing object (and everything it transitively
references) — a classic leak, and a serialization landmine. EJ24's fix: if the
construct doesn't need the enclosing instance, make it `static` (or write a
lambda/`static` method) so it structurally *cannot* hold `this$0`.

**Q:** EJ25 — what happens if you put two `public` top-level classes in one
`.java` file?
**A:** It won't compile: `class B is public, should be declared in a file named
B.java`. At most one `public` top-level class per file, and its name must match
the filename. (You may have multiple package-private top-level classes in one
file, but that's discouraged — one top-level class per file aids discoverability.)
