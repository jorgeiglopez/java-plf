# Records & Sealed Types — The Switch That Lies

**Concept:** a `switch` over a sealed type is exhaustive *for free* — and the
"safe" `default ->` branch you add out of habit is exactly what destroys that
safety net and lets a new permitted subtype slip through as silent garbage.
**Difficulty:** ★★☆ intermediate
**Est. time:** ~15 min

## Background

A sealed hierarchy plus a pattern `switch` gives you a compile-time guarantee:
the compiler knows *every* permitted subtype, so if your `switch` misses one, the
build fails. That turns "I forgot to handle the new shape" from a 2 a.m. runtime
surprise into a red squiggle in your IDE.

Then a well-meaning reviewer says *"add a `default` just to be safe."* You do. It
feels defensive. It is the bug. With a `default` present, the `switch` is
*already* exhaustive — so when someone adds a new subtype, the compiler stays
silent and your `default` quietly answers for it. The instinct that was supposed
to protect you is what blinds you.

This is a **two-stage, predict-then-prove** exercise. Stage 1 is the *control*
(no default — the safety net). Stage 2 is the *experiment* (with default — the
silent lie). The scar is the difference between them.

## Task

Two committed files model the same `sealed interface Shape permits Circle,
Rectangle`, both records, both with a switch-expression `area(Shape)`. They
compile and the test is green **today**.

**Stage 1 — arm the safety net (thought experiment, no code edit).**

1. Open `SealedAreaStage1.java`. Its `area` is a switch **expression** with **no
   `default`** over `Circle` and `Rectangle` only.
2. On paper, predict: *if I add `record Triangle(double base, double height)
   implements Shape`, add `Triangle` to `permits`, but do NOT touch the switch —
   what happens?* Compile error, runtime exception, or silent wrong answer?
3. Write down the **exact** `javac` message you expect (Java 21). Do not edit the
   file — Stage 1 stays compiling for everyone. Check your answer in
   `SOLUTION.md`.

**Stage 2 — detonate it (real edit, the experiment).**

4. Open `SealedAreaStage2.java`. Identical *except* its `area` ends in
   `default -> 0.0`.
5. Open `src/test/java/.../s02_08_records_and_sealed/SealedAreaTest.java`. In
   `PREDICT_TRIANGLE_AREA`, commit a **number**: what will
   `area(new Triangle(4.0, 3.0))` return once you wire it in? Write it down
   before you do anything else.
6. Now make the edit the reviewer asked for. In `SealedAreaStage2.java`:
   declare `record Triangle(double base, double height) implements Shape {}` and
   add `Triangle` to the `permits` clause.
7. In the test, activate `defaultBranchLiesForTriangle`: uncomment the two
   marked lines (`// <-- Task 3`). Run it.
8. Watch it go red. Read the real returned value off the failure message and the
   solution. Then change `PREDICT_TRIANGLE_AREA` to that value so the suite is
   green — and sit with how comfortable a wrong `0.0` looked.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Do **not** edit `SealedAreaStage1.java` — it must keep compiling. Stage 1 is a
  paper prediction; the breaking version lives only in `SOLUTION.md`.
- In Stage 2 you add `Triangle` and activate the test. Do **not** "fix" `area` by
  handling `Triangle` in the switch — the point is to *see the lie first*.
- `area` stays a switch **expression** (assigned / returned), not a statement.

## Scale up (Optional)

1. **Make the default honest.** Remove `default -> 0.0` from Stage 2's `area` and
   instead add a real `case Triangle t -> 0.5 * t.base() * t.height();`. Re-run.
   Now explain: with the explicit case, what would the compiler have done in step
   6 if you had *forgotten* `Triangle` entirely? (That is the safety net you threw
   away by adding `default`.)
2. **Compact-constructor guard.** `Circle` already rejects `radius <= 0` in a
   compact constructor. Predict the exact `javac` error if you instead wrote
   `this.radius = radius;` inside that compact constructor, then check
   `SOLUTION.md`. Why is the assignment both forbidden *and* unnecessary?
3. **`non-sealed` reopens the wound.** If `Shape` permitted a `non-sealed`
   subtype, what happens to the no-default switch's exhaustiveness guarantee, and
   why?

## How to verify

```
mvn -q -Dtest=SealedAreaTest test
```

The suite is **green before Task 3** (Stage 2 is correct for Circle/Rectangle,
the compact constructor validates, records compare by value). After you add
`Triangle` and uncomment the Task-3 lines, `defaultBranchLiesForTriangle` goes
**red** against your prediction — fix `PREDICT_TRIANGLE_AREA` to the real value to
return to green.

<details><summary>Hint (open only if stuck)</summary>
With no `default`, a pattern <code>switch</code> over a sealed type lists a finite,
compiler-known set of cases; miss one and the build fails. A <code>default</code>
branch makes the set "complete" by definition, so the compiler stops checking —
and any unhandled subtype falls into <code>default</code> at runtime. Ask what
<code>0.0</code> means for a triangle whose real area is <code>6.0</code>.
</details>
