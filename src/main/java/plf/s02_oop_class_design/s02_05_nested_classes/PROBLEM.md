# Nested Classes — The list that wouldn't die

**Concept:** A nested construct retains its enclosing `Outer` instance **only
when its body actually touches an enclosing member** — and this is true for
**inner classes too**, not just lambdas (Java 21). The enclosing instance is
always *required to construct* an inner class, but it is only *retained* (the
leak) when used. `static` nested classes can never retain it.
**Difficulty:** ★★★ senior
**Est. time:** ~15 min

## Background

`Outer` holds a fat `byte[] payload`. A method returns five `Runnable`s built
five ways: a non-static inner class that touches nothing, a non-static inner
class that reads `payload`, a `static` nested class, a lambda that uses only a
local, and a lambda that reads `payload`. You stash all five in a long-lived
`List<Runnable>` and drop the `Outer` reference.

Which `Runnable`s keep that `byte[]` reachable — i.e. leak it? Most seniors
answer "all the inner ones" (they swear inner classes *always* pin `Outer`) or
"none of the lambdas" (they think lambdas never capture `this`). **Both rules are
wrong.** The truth is one rule for all four: *capture follows what the body
touches.* The synthetic `this$0` field — the thing that pins `Outer` — is only
emitted by `javac` when the body references the enclosing instance.

## Task

**Commit your predictions in writing BEFORE you run or compile anything.** Fill
this table (in `answers.txt` or on paper). For each construct, predict whether
its compiled class carries a field of type `Leak` — i.e. whether it **retains
(pins) the enclosing `Leak`** after `outer = null`.

| # | construct | body touches | retains `Leak` (pins payload)? |
|---|-----------|--------------|--------------------------------|
| 1 | non-static inner, empty body | nothing | ? |
| 2 | non-static inner | `payload` | ? |
| 3 | `static` nested | nothing | ? |
| 4 | lambda | only a local `int` | ? |
| 5 | lambda | `payload` | ? |

Then, numbered:

1. Predict all five rows and commit them. **Do not peek.** Row 1 vs row 2 is the
   blood: same inner class, only the *body* differs.
2. Run the harness (`Leak.main`) or the green tests in `LeakTest`. Each construct
   is fed to `Leak.capturesEnclosing(...)`, which inspects
   `getClass().getDeclaredFields()` for a field of type `Leak`. Confirm rows 1–5
   against reality.
3. **The blank that is the whole exercise.** In
   `Leak.whyEmptyInnerDoesNotPin()` (currently a
   `throw new UnsupportedOperationException("TODO")` stub), complete and return
   one sentence answering: *why does the empty non-static inner (row 1) NOT
   retain `Leak`, even though constructing it still required an enclosing
   `Leak` instance?* Returning a non-blank sentence turns the red test
   `whyEmptyInnerDoesNotPin_isCommitted` green.
4. In one sentence, state the single rule that explains all five rows. Use the
   word **`this$0`**.

## Constraints

- Java 21 (Amazon Corretto), no external libraries (JUnit 5 only).
- Commit your five predictions **before** compiling/running. Row 1 is the burn —
  don't rob yourself of it.
- Do **not** edit the test file. Turn the red test green by completing the
  production stub.
- Do not "fix" the leak — the exercise is to *see* which constructs leak, not to
  stop them.

## Scale up (Optional)

1. **Construction vs retention.** `new Leak.InnerTouchingNothing()` (no enclosing
   instance) does not compile, yet the *finished* object retains no `Leak`. Square
   these: if construction needs an enclosing `Leak`, where does it go? Predict the
   exact Java 21 `javac` message for `new Leak.InnerTouchingNothing()` called from
   an unrelated class, and the correct alternative that compiles.
2. **The effectively-final wall.** Make `lambdaLocalOnly` capture a local, then
   reassign that local on the next line. Predict the exact `javac` message. Why
   does the array-covariance-style "just mutate it" not work here?
3. **`Outer.this` vs `this`.** Inside `InnerTouchingPayload.run()`, what does
   plain `this` refer to, and how do you name the enclosing `Leak` instance? Give
   the qualified syntax.
4. **EJ25 — one public top-level class per file.** Put two `public` top-level
   classes in one `.java` file. Predict the exact `javac` message. Then state
   EJ24's rule: of static-nested / inner / local / anonymous, which should you
   reach for first, and why.

## How to verify

Predict first, then check:

```
# Green facts (rows 1–5):
mvn -q -Dtest=LeakTest#innerTouchingNothing_doesNotCaptureEnclosing test
mvn -q -Dtest=LeakTest#innerTouchingPayload_capturesEnclosing test
mvn -q -Dtest=LeakTest#staticNested_neverCapturesEnclosing test
mvn -q -Dtest=LeakTest#lambdaLocalOnly_doesNotCaptureEnclosing test
mvn -q -Dtest=LeakTest#lambdaTouchingPayload_capturesEnclosing test

# Or run the harness directly after `mvn -q test-compile`:
java -cp target/classes plf.s02_oop_class_design.s02_05_nested_classes.Leak

# The exercise test starts RED and turns green once you finish the stub:
mvn -q -Dtest=LeakTest#whyEmptyInnerDoesNotPin_isCommitted test
```

Compare your one-sentence rule and your scale-up `javac` strings against
`SOLUTION.md`.

<details><summary>Hint (open only if stuck)</summary>
`javac` emits the synthetic field that pins the enclosing instance — `this$0`
for inner/local classes, an `arg$N` of type `Leak` for lambdas — **only if the
body reads an enclosing member** (a field, a method, or `Outer.this`). No touch,
no field, no leak. The enclosing instance is still *passed to the constructor* of
an inner class (that is why you can't build one without an `Outer`), but if the
body never uses it, the compiler doesn't store it. `static` nested classes are
never handed an enclosing instance at all.
</details>
