# Solution — You Guarded the Wrong Door

## Prediction (the grading key)

`PREDICT_INVARIANT_HOLDS` is **`false`**. The fields are `final` and the getters
copy, yet mutating the caller's original `Date` (`end.setTime(0L)`) moves the
period's end from `10000` to `0`. If you wrote `true`, you trusted `final` and a
copying getter and forgot the constructor aliased the caller's live `Date`. That
is the entire trap.

## Ideal solution

Fix `Period.java` by copying each argument **in the constructor**, then validate
the copies. The getters already copy correctly; the inbound door was the only one
open.

```java
package plf.s02_oop_class_design.s02_03_class_design_principles;

import java.util.Date;

public final class Period {                      // final: no subclass can re-leak (EJ19)

    private final Date start;                    // private: not a public mutable field (EJ15-16)
    private final Date end;

    public Period(Date start, Date end) {
        this.start = new Date(start.getTime());  // copy IN (the door the scaffold left open)
        this.end = new Date(end.getTime());
        if (this.start.after(this.end)) {        // validate the COPIES, not the params (EJ50)
            throw new IllegalArgumentException(this.start + " after " + this.end);
        }
    }

    public Date getStart() {
        return new Date(start.getTime());         // copy OUT (already correct in the scaffold)
    }

    public Date getEnd() {
        return new Date(end.getTime());
    }
}
```

Three moves, in priority order:

- **Copy the arguments in the constructor.** `this.start = new Date(start.getTime())`
  makes the field point at an object only the `Period` can reach. The caller's
  surviving variable now references a *different* `Date`; mutating it can't touch
  your state. This is the one change that turns the test green.
- **`private` fields, accessors only (EJ15-16).** `public final Date` still lets
  anyone read the field and call `setTime` on it directly — `final` blocks
  reassignment, not mutation. Hide the field; expose copying getters.
- **`final` class (EJ19).** A subclass could override `getEnd()` to
  `return end;` and re-open the outbound door, or add a leaking accessor. If the
  class is not designed for inheritance, prohibit it: `final`.

## Why this works (and the gotcha)

`final` freezes the *reference*, never the *referent*. `private final Date start`
means "`start` will always point at the same `Date`", not "that `Date` can never
change". `Date` is mutable (`setTime`), so a `final` field of type `Date` is a
mutable object behind an unreassignable handle.

There are two boundaries where a mutable component can escape, the two "doors":

- **Outbound (the getter).** If `getEnd()` returns the field directly, the caller
  mutates your state through the return value. The scaffold *already closed this*
  with `new Date(end.getTime())`. That is the bait — the obvious leak is gone, so
  you feel safe.
- **Inbound (the constructor).** `this.end = end` stores the caller's *own*
  `Date`. The caller's variable and your field are now **aliases** of one object.
  The caller never needs a getter — they already hold the reference they passed
  in, and `end.setTime(0L)` reaches straight into your "immutable" period.

The one-sentence aha: **I copied at one door; aliasing needs both — and
immutability deletes both doors.** A defensive copy that lives in only one place
is decorative. EJ17/EJ50: copy mutable components on the way *in* and on the way
*out*, or — far better — make the component immutable so neither copy is needed.

## Common mistakes

- **"The field is `final`, so the object is immutable."** → `final` stops
  reassignment of the reference; the `Date` it points at is still mutable.
- **"I defensive-copied (in the getter), so I'm safe."** → You closed the
  outbound door only. The constructor aliased the caller's `Date`; the inbound
  door is wide open.
- **Re-fixing the getter when the test stays red.** → The getter was never the
  problem. The fix is `new Date(...)` in the *constructor*.
- **`public final Date` with copying getters.** → Pointless: callers bypass the
  getter and call `period.end.setTime(...)` on the public field directly. Make it
  `private`.
- **Validating the raw parameters, then copying.** → TOCTOU window (Scale-up 2).
  Copy first; validate the copies.
- **Using `start.clone()` to copy the argument.** → A `Date` subclass can
  override `clone()` to leak or return a malicious subtype (Scale-up 3). Use
  `new Date(d.getTime())`, whose result is always a plain `java.util.Date`.
- **Leaving `Date` in place at all.** → The real fix is `Instant`. Patching
  `Date` keeps a mutable component you must guard forever (Scale-up 5).

## Scale up solutions (Optional)

**1. Minimize accessibility and prohibit inheritance (EJ15-16, EJ19).** Shown in
the ideal solution: fields `private`, class `final`. Rationale — a `public`
mutable field can never be made safe (the accessor is bypassable), and `final`
encodes the design decision "not built for extension". The alternative to `final`
is to *design and document* for inheritance (carefully spec self-use, provide a
copy constructor for subclasses, etc.); if you are not doing that work, prohibit
extension. There is no safe middle ("a non-final class nobody is supposed to
extend") — that is the fragile-base-class invitation.

**2. Copy before you validate — the TOCTOU fix (EJ50).** Order matters when the
parameter is a *mutable object you do not own*:

```java
public Period(Date start, Date end) {
    this.start = new Date(start.getTime());  // copy FIRST into storage you own
    this.end = new Date(end.getTime());
    if (this.start.after(this.end)) {        // then validate the copies
        throw new IllegalArgumentException(this.start + " after " + this.end);
    }
}
```

If you validated the *parameters* and then copied, a hostile caller on another
thread could mutate `end` in the window between the `after` check and the copy
(time-of-check to time-of-use). The validated thing and the stored thing would
differ. Copying first removes the window: nothing outside the constructor can
reach `this.start`/`this.end`, so a check against them is final. The rule: for a
mutable parameter, **copy into your own storage, then validate that storage** —
and the validation must read the fields, never the parameters.

**3. Why `clone()` is the wrong tool (the trap-within-the-trap).** `Date` is
non-final, so a parameter typed `Date` may at runtime be a malicious subclass:

```java
final class EvilDate extends Date {
    EvilDate(long t) { super(t); }
    @Override public Object clone() { /* leak `this` to an attacker, then */ return super.clone(); }
}
```

`d.clone()` dispatches to the *override* and can hand the attacker a live
reference (or return a subtype that keeps mutating). `new Date(d.getTime())`
calls only `getTime()` (a value read) and constructs a *plain* `java.util.Date` —
its runtime class is exactly `java.util.Date`, subtype stripped. EJ50: never use
`clone()` to defensively copy a parameter whose type can be subclassed; use a
constructor or factory that you control. (Note: this attack is on the *inbound*
argument. The getter's `new Date(end.getTime())` already returns a plain `Date`,
so the outbound side was never exposed to it — don't conflate the two doors.)

**4. Composition over inheritance (EJ18).** You want a labeled period. The
inheritance route is fragile:

```java
// DON'T: extends couples you to Period's self-use and overridable surface.
class LabeledPeriod extends Period {
    private final String label;
    LabeledPeriod(Date s, Date e, String label) { super(s, e); this.label = label; }
}
```

Subclassing exposes you to the fragile-base-class problem: if a future `Period`
method calls another overridable method internally, your subclass can break when
the base changes, and you inherit every public method whether or not it makes
sense for a labeled period. Prefer **holding** a `Period`:

```java
public final class LabeledPeriod {
    private final Period period;     // composition: Period is a black box
    private final String label;

    public LabeledPeriod(Period period, String label) {
        this.period = period;        // Period is immutable, so no copy needed here
        this.label = label;
    }

    public Date getStart() { return period.getStart(); }  // forward
    public Date getEnd()   { return period.getEnd(); }
    public String label()  { return label; }
}
```

Composition forwards a deliberate subset of behavior, is immune to `Period`'s
internal changes, and — because `Period` is immutable — stores the field without
copying. `extends` cannot give you that isolation; you are welded to the base
class's implementation details.

**5. The kill-shot: `Instant` deletes the problem (EJ17).** Replace the mutable
component with an immutable one and *both* copies disappear:

```java
package plf.s02_oop_class_design.s02_03_class_design_principles;

import java.time.Instant;

public record Period(Instant start, Instant end) {     // immutable component
    public Period {                                    // compact constructor: validate only
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(start + " after " + end);
        }
        // no copies: Instant cannot be mutated, so aliasing is harmless
    }
}
```

`Instant` has no mutators; aliasing it is safe because nobody can change the
object behind any reference. The constructor needs no inbound copy, the accessor
needs no outbound copy, and `final`/`equals`/`hashCode`/`toString` come free from
the record (EJ12). The leak is now *unrepresentable*, not patched — that is the
whole argument for EJ17. (If you must accept legacy `Date` at the boundary,
convert once on the way in: `Instant in = arg.toInstant();`, and hand back
`Date.from(start)` only if a caller truly needs the legacy type.)

## Interviewer Q&A

**Q:** The fields are `final`. Doesn't that make `Period` immutable?
**A:** No. `final` makes the *reference* unreassignable; it says nothing about
the object the reference points at. `Date` is mutable, so a `final Date` field is
a mutable object behind a frozen handle. Immutability requires that the component
*itself* can't change (or that you copy it at every boundary).

**Q:** The getter already returns `new Date(...)`. Where is the leak?
**A:** In the constructor. `this.end = end` stores the caller's own `Date`, so
the field and the caller's variable alias one object. The caller mutates their
variable — `end.setTime(0L)` — and your "private" state moves. The leak never
goes through the getter at all.

**Q:** So why copy on the way *in* if you already copy on the way *out*?
**A:** They close different doors. The outbound copy stops a caller mutating the
reference you *return*. The inbound copy stops a caller mutating the reference
they *passed in* and still hold. A mutable component can escape at both
boundaries; one copy guards one door. You need both — or an immutable component,
which needs neither.

**Q:** Should you validate (`start.after(end)`) before or after the defensive
copy?
**A:** After — validate the copies. If you validate the raw parameters then copy,
a hostile caller on another thread can mutate the argument between the check and
the copy (TOCTOU), so your "validated" object isn't what you stored. Copy first
into storage you own, then validate that storage; the check then reads the fields,
never the parameters.

**Q:** Could you use `start.clone()` to make the inbound copy?
**A:** Don't. `Date` is non-final, so the argument might be a subclass whose
`clone()` is overridden to leak `this` or return a still-mutable subtype. Use
`new Date(d.getTime())` — it reads a `long` and constructs a plain
`java.util.Date`, so the subtype is stripped and no attacker code runs. EJ50:
never defensively copy a subclassable parameter via `clone()`.

**Q:** The fields are `public final`. Is that ever acceptable for a mutable type?
**A:** No. A `public` field is directly reachable (`period.end.setTime(...)`),
bypassing any copying getter, and `final` only blocks reassignment. Make mutable
fields `private` and expose accessors (EJ16). Public fields are tolerable only
for immutable constants, and even then `private` + accessor is the safer default
for evolution.

**Q:** Why mark the class `final`?
**A:** EJ19: design for inheritance or prohibit it. If the class isn't built and
documented for safe extension, a subclass could override `getEnd()` to return the
field raw, or add a leaking accessor, re-opening a door you closed. `final`
encodes "not extensible" and removes that risk. The alternative is the real work
of designing for inheritance (specifying self-use, a copy constructor for
subclasses), which here buys nothing.

**Q:** You need a "period with a label." Extend `Period` or compose it?
**A:** Compose — hold a `Period` field and forward what you need (EJ18).
Inheritance welds you to `Period`'s overridable surface and self-use (fragile
base class) and forces every public method onto the subclass. Composition exposes
a deliberate subset, is immune to `Period`'s internal changes, and — since
`Period` is immutable — stores the field with no copy.

**Q:** What's the right fix overall, beyond copying `Date` everywhere?
**A:** Make the component immutable: model the interval with `java.time.Instant`
(e.g. a `record Period(Instant start, Instant end)`). Then there is nothing to
copy in or out, `final`/`equals`/`hashCode`/`toString` come free, and the leak is
unrepresentable. EJ17 — immutability deletes the whole class of bug instead of
patching each boundary.
