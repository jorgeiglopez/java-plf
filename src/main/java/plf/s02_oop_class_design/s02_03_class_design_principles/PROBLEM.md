# Class Design Principles — You Guarded the Wrong Door

**Concept:** A `final` field is not an immutable object. With a mutable component (`java.util.Date`), real defense is *element copying at every boundary* — and the boundary everyone forgets is the **constructor's inbound argument**, not the getter (EJ17, EJ50). Encapsulation also means private fields behind accessors, a `final` class, and ultimately deleting the problem with an immutable type.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Here is `Period.java`: two `public final Date` fields, a constructor that
validates `start.after(end)`, and getters that already return a **defensive
copy** — `new Date(start.getTime())`. The fields are `final`. The getters copy.
It looks like a careful, immutable value class.

It is not. A `final` field freezes the *reference*, never the object it points
at, and `Date` is mutable (`setTime`). The smug move when you see `Date` is
"defensive copy" — so you copy in the getter, go green, and walk away. You
guarded one door. The question this exercise traps: **which door did you leave
open, and can someone walk through it without ever touching a getter?**

## Task

1. Read `Period.java`. Note the shape: `public final Date start, end`; getters
   return `new Date(...)`; the constructor stores the arguments **raw**
   (`this.start = start;`).
2. Open
   `src/test/java/.../s02_03_class_design_principles/PeriodLeakTest.java`.
   It builds a `Period`, records `getEnd()`, then calls `end.setTime(0L)` on the
   **caller's original `Date`** — never on a getter return — and re-reads
   `getEnd()`.
3. **Commit first.** Set the literal `PREDICT_INVARIANT_HOLDS` to your honest
   prediction: does `Period`'s end survive a mutation of the caller's original
   `Date`? `true` = the period is immune; `false` = it shifts. The fields are
   `final` and the getters copy — write down `true` or `false` *before you run*.
   The gap between your literal and the red bar is the lesson.
4. Run the test. It starts **red**. The failure message tells you whether the
   end moved.
5. **Fix the production code** so the invariant holds and the test goes green.
   The only correct place is **Task 1** in `Period.java`: copy each argument in
   the constructor (`this.start = new Date(start.getTime());`). Do **not** touch
   the test.
6. Diagnose in one sentence: the getter already copied, so why did the period
   still move when you mutated a `Date` you passed *in*?

## The bait (read before you "fix" anything)

The reflex on seeing `Date` is "I'll defensive-copy in the getter" — and the
scaffold already did that for you. So the obvious leak (someone mutating a getter
return) is *already closed*. That is the bait: you feel safe. The breach is the
door you didn't look at — the constructor aliased the caller's live `Date`, and
that reference is still wired to the caller's variable. One copy is not a defense;
**aliasing needs both doors closed.** Fixing the getter again does nothing; the
fix is in the constructor.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Fix `Period.java` only. Do not modify `PeriodLeakTest.java`.
- Keep the `start.after(end)` validation; after you copy the arguments, validate
  the **copies** (EJ50) — see Scale-up 2 for why that ordering matters.

## Scale up (Optional)

1. **Minimize accessibility (EJ15–16, EJ19).** Make `start`/`end` `private`;
   keep only the getters as the public surface. Then make the class `final` so no
   subclass can override `getEnd()` to leak the field again. State the rule:
   public mutable fields are never acceptable; for a class designed *not* to be
   extended, mark it `final` (EJ19 — "design for inheritance or prohibit it").
2. **TOCTOU: copy before you validate (EJ50).** Show that validating the *raw*
   arguments and then copying leaves a hole: a hostile caller in another thread
   could swap the `Date` between your `after` check and your copy. Reorder to
   **copy first, then validate the copies**. Why must the validation read
   `this.start`/`this.end`, never the parameters?
3. **The `clone()` trap (the trap-within-the-trap).** Suppose you copied the
   inbound argument with `start.clone()` instead of `new Date(start.getTime())`.
   A `Date` *subclass* that overrides `clone()` (or `getTime()`) can defeat that.
   Explain why `clone()` is the wrong defensive-copy tool for a non-final
   parameter type, and why `new Date(d.getTime())` is immune (what is the runtime
   class of its result?).
4. **Composition over inheritance (EJ18).** You needed a "period with a label."
   Show why `class LabeledPeriod extends Period` is the wrong move (the fragile
   base class / self-use problem) and rewrite it to **hold** a `Period` field
   instead. One sentence: what does composition buy you that `extends` cannot?
5. **The kill-shot: immutability deletes the problem (EJ17).** Re-implement
   `Period` over `java.time.Instant` instead of `Date`. Show that with an
   immutable component, **both** copies vanish — no copy in the constructor, none
   in the getter — and the leak is *unrepresentable*, not merely patched. This is
   the point of EJ17: an immutable type erases the entire bug class.

## How to verify

```
mvn -q -Dtest=PeriodLeakTest test
```

The test starts **red**. It is green only once `Period`'s constructor copies its
arguments. Then re-read your `PREDICT_INVARIANT_HOLDS` literal: if you wrote
`true`, you are exactly the developer this exercise was built for — you trusted
`final` + a copying getter and forgot the inbound door.

<details><summary>Hint (open only if stuck)</summary>
`final` makes the *reference* `start` unreassignable; it says nothing about the
`Date` object on the other end. The getter returns a fresh copy, so mutating the
getter's return is harmless. But `this.start = start;` makes the field and the
caller's variable point at the *same* `Date`. When the caller does
`end.setTime(0L)`, your field sees it, because it is the same object. Copy on the
way **in** (`new Date(start.getTime())`) and the field points at an object only
you can reach. Better: use `Instant`, which can't be mutated at all.
</details>
