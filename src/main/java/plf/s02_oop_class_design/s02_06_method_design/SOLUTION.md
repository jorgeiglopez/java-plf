# Solution — The Defensive Copy That Wasn't

## Predictions (the grading key)

- **Q1 (control, `LocalDate`):** `N`. `List.copyOf` made a private, unmodifiable
  list; the caller's later `add` touches *its* list, not yours.
- **Q2 (in-gate, `Appointment`):** `Y` in the broken scaffold — the leak. After
  the fix it must be `N`.
- **Q3 (out-gate, `Appointment`):** `Y` in the broken scaffold — the leak. After
  the fix it must be `N`.

If you predicted `N`/`N`/`N` ("copyOf is safe, I copied at the door") you fell
for it. That is exactly the muscle-memory the exercise targets.

## Ideal solution

Fix `Schedule.java` — copy each `Appointment` element on the way **in** and on
the way **out**. The `LocalDate` arm needs no change: `LocalDate` is immutable,
so `List.copyOf` is genuinely sufficient there.

```java
package plf.s02_oop_class_design.s02_06_method_design;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Schedule {

    private final List<LocalDate> days;
    private final List<Appointment> appointments;

    public Schedule(List<LocalDate> days, List<Appointment> appointments) {
        Objects.requireNonNull(days, "days");
        Objects.requireNonNull(appointments, "appointments");
        this.days = List.copyOf(days);              // immutable element -> copyOf is enough
        List<Appointment> in = new ArrayList<>(appointments.size());
        for (Appointment a : appointments) {
            in.add(new Appointment(a));             // element-wise copy IN
        }
        this.appointments = List.copyOf(in);        // wrap unmodifiable
    }

    public List<LocalDate> days() {
        return days;                                // already unmodifiable + immutable elements
    }

    public List<Appointment> appointments() {
        List<Appointment> out = new ArrayList<>(appointments.size());
        for (Appointment a : appointments) {
            out.add(new Appointment(a));            // element-wise copy OUT
        }
        return List.copyOf(out);                    // wrap unmodifiable
    }
}
```

Why these exact moves:

- **`days` stays `List.copyOf` and returns the field directly.** `LocalDate` is
  immutable, so no caller can corrupt a date through the reference. Copying the
  list once is the whole defense. This is the control arm; it proves the rule is
  *conditional on element immutability*, not "always copy elements".
- **`appointments` copies element-by-element with `new Appointment(a)`.** Both
  gates. In the constructor so the caller's surviving references can't reach in;
  in the accessor so the reference you hand out can't reach in either.
- **`List.copyOf(...)` last** to make both returned/stored lists unmodifiable, so
  `add`/`remove` throw `UnsupportedOperationException`. That keeps the
  structural defense you already had on top of the new element defense.

### Scale up solutions (Optional)

**1. Validation ordering (EJ49).** Validate *before* copying:

```java
public Schedule(List<LocalDate> days, List<Appointment> appointments) {
    Objects.requireNonNull(days, "days");
    Objects.requireNonNull(appointments, "appointments");
    for (LocalDate d : days) {
        if (d.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("past date: " + d);
        }
    }
    // ...then copy elements as above
}
```

The subtle part is the *order* of validate-vs-copy on a parameter you do **not**
control. If you validate the caller's list, then copy it, a hostile caller in
another thread can swap an element between your check and your copy
(time-of-check/time-of-use). EJ50's rule: **copy first, then validate the
copy** — for *mutable* parameters whose elements you must trust. Here the
elements (`LocalDate`) are immutable, so check-then-copy is safe; but if the
elements were mutable you would copy each `Appointment` first and validate the
copies. Know which case you are in.

**2. Empty, not null (EJ54).**

```java
public List<Appointment> appointmentsOn(LocalDate day) {
    List<Appointment> hits = new ArrayList<>();
    for (Appointment a : appointments) {
        // (match logic; appointments here would also carry a date in a fuller model)
    }
    return List.copyOf(hits);   // empty -> an empty unmodifiable list, never null
}
```

A caller writes `for (Appointment a : s.appointmentsOn(d))` with no null guard.
`List.of()` (and `Collections.emptyList()`) return a shared, immutable singleton
— zero allocation per call — so "return empty, not null" costs nothing and
deletes a whole class of `NullPointerException`. Returning `null` to mean "no
results" pushes a null-check onto every call site and forgets exactly one of
them in production.

**3. Optional, not null (EJ55).**

```java
public Optional<Appointment> firstAppointment() {
    if (appointments.isEmpty()) return Optional.empty();
    return Optional.of(new Appointment(appointments.get(0)));  // defensive copy inside
}
```

`Optional` communicates "maybe absent" in the type, but it is *not* a defensive
copy. Wrapping a live mutable `Appointment` in an `Optional` still leaks it —
`opt.get().setTime(...)` mutates your state. Copy the payload, then wrap. Never
return `Optional<Collection>` (EJ55): return an empty collection instead.

## Why this works (and the gotcha)

`List.copyOf(c)` returns a new, unmodifiable `List` whose **elements are the same
references** as `c`. It copies the *container*, not the *contents*. So:

- For an **immutable** element type (`LocalDate`, `String`, boxed numbers,
  records of immutable components), sharing the references is harmless — nobody
  can change a `LocalDate` through any reference. `copyOf` is a complete defense.
- For a **mutable** element type (`Appointment` with `setTime`), the new list
  still points at the caller's live objects. Mutate one through *any* surviving
  reference — the original variable, or the reference returned by the accessor —
  and the schedule's "private" state changes under it.

The trap is precisely that `List.copyOf` is the *first* thing a competent dev
reaches for, and it is the *correct* fix for one arm and an *insufficient* fix
for the other. Same reflex, opposite verdicts, decided entirely by whether the
element type is immutable. The senior insight: **a defensive copy is element-wise
or it's decorative — element immutability decides.**

Note the phrasing: not "deep copy". `copyOf` does not deep-copy, and you do not
want `clone()` here. The fix is an explicit **element-wise copy via a copy
constructor** (`new Appointment(a)`) at both gates — or, better still, making
`Appointment` immutable so the whole problem evaporates (then `copyOf` alone
suffices, exactly as for `LocalDate`). When you control the element type, prefer
immutability; when you don't, copy element-wise at every boundary.

## Common mistakes

- **"I called `List.copyOf`, so it's safe."** → True for the list structure and
  for immutable elements; false for the mutable-element arm. The list is
  unmodifiable but its elements are still shared and mutable.
- **Fixing only the in-gate (constructor).** → Q2 goes green, Q3 stays red. The
  accessor still hands out references to the live internal `Appointment`s. Both
  gates or neither.
- **Fixing only the out-gate (accessor).** → Q3 goes green, Q2 stays red. The
  caller still holds the reference it passed in.
- **`clone()` or "deep copy".** → `clone()` is a contract minefield (EJ13) and
  invites a smug "but copyOf deep-copies" argument (it doesn't). Use a copy
  constructor.
- **Making `Appointment` a record / final fields to dodge the problem.** →
  That's changing the question. Sometimes the right answer in real code, but here
  the element type is mutable by assumption; defend against it.
- **Returning the unmodifiable list and thinking that's enough.** →
  Unmodifiable blocks `add`/`remove`/`set`; it does nothing to stop
  `get(0).setTime(...)`. Structural immutability ≠ element immutability.
- **Validating before copying a mutable parameter (scale-up).** → Leaves a
  TOCTOU window; copy first, validate the copy (EJ50).

## Interviewer Q&A

**Q:** A constructor does `this.list = List.copyOf(arg)`. Is the field now safe
from outside mutation?
**A:** The *list* is — it's a new unmodifiable list, so the caller can't
add/remove/replace through their reference. The *elements* are only safe if the
element type is immutable. `copyOf` copies element references, not the elements.

**Q:** So when is `List.copyOf` a complete defensive copy and when isn't it?
**A:** Complete when the element type is immutable (`LocalDate`, `String`,
records of immutable components). Insufficient when elements are mutable — then
you must copy each element (copy constructor or factory) on the way in and out.

**Q:** Why copy on the way *out* too, not just in the constructor?
**A:** The accessor returns a reference into your internal list. If you hand back
the live `Appointment`, the caller calls `getAppointments().get(0).setTime(...)`
and mutates your state. In-gate stops the caller's original reference; out-gate
stops the one you just handed them. Both, or the object isn't encapsulated.

**Q:** Is "deep copy" the right term for the fix?
**A:** No. Deep copy implies recursively cloning an arbitrary object graph and
invites `clone()`. Here you do a one-level, explicit element-wise copy via a copy
constructor `new Appointment(a)`. Precise and contract-safe.

**Q:** The naive fix is to make the returned list unmodifiable
(`Collections.unmodifiableList`). Does that close the leak?
**A:** No. Unmodifiable blocks structural changes (`add`/`set`/`remove`) but not
`get(0).setTime(...)`. It defends the list, not the mutable elements. You still
need element-wise copies.

**Q:** EJ50 says copy mutable parameters *before* validating them. Why that
order?
**A:** To close a TOCTOU window. If you validate the caller's object then copy
it, another thread can mutate it between check and copy, so your "validated" copy
holds invalid state. Copy first into storage you own, then validate the copy —
nothing can change it afterward. (For immutable elements the order doesn't
matter, but defaulting to copy-then-validate is the safe habit.)

**Q:** A method has no matching results. Return `null` or an empty collection?
**A:** Empty collection — `List.of()` / `Collections.emptyList()` (EJ54). They're
shared immutable singletons, so it's allocation-free, and it deletes a null-check
from every call site. `null` as "no results" is a guaranteed future NPE.

**Q:** When would `Optional` be appropriate here, and what's the catch?
**A:** For a single maybe-absent value (`firstAppointment()` → `Optional<Appointment>`),
not for collections (return an empty collection instead, EJ55). The catch:
`Optional` signals absence but is not a defensive copy — if the payload is
mutable you must copy it *before* wrapping, or you've leaked it inside an
`Optional`.

**Q:** You control `Appointment`'s source. What's the cleaner fix than copying
everywhere?
**A:** Make `Appointment` immutable (final class, final fields, no setters, e.g.
a record). Then it's in the same bucket as `LocalDate` and a single `List.copyOf`
is a complete defense at both gates — no per-element copying. Prefer immutability
when you own the type (EJ17); copy element-wise only when you don't.
