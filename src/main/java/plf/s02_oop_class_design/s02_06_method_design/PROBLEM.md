# Method Design — The Defensive Copy That Wasn't

**Concept:** a defensive copy is *element-wise* or it's decorative — the immutability of the element type decides whether `List.copyOf` is enough.
**Difficulty:** ★★☆ intermediate
**Est. time:** ~20 min

## Background

You inherit a `Schedule` that already looks bulletproof: its constructor stores
`List.copyOf(...)` of every argument, so a caller can't smuggle in a list and
mutate it later. The author copied at the door. Job done — right?

`Schedule` holds two lists: `List<LocalDate>` and `List<Appointment>`.
`LocalDate` is immutable. `Appointment` has a `setTime(...)`. Before you run
anything, ask yourself: **does `List.copyOf` actually protect both lists?** Copy
the *list*, or copy the *elements*?

## Task

You commit to three predictions **in writing first**, then let JUnit grade you.

1. Open `Schedule.java` and `Appointment.java`. Read them. Do **not** edit yet.
2. In `predictions.txt` (create it), write `Y` or `N` for each — *before* running:
   - **Q1 (control):** A caller passes its `List<LocalDate>`, then does
     `caller.add(anotherDate)`. Does `schedule.days()` grow? `Y`/`N`
   - **Q2 (in-gate):** A caller passes a `List<Appointment>` containing one
     appointment at 09:00, then calls `appt.setTime(23:59)` on that *same object*.
     Does `schedule.appointments().get(0).getTime()` become 23:59? `Y`/`N`
   - **Q3 (out-gate):** Someone calls
     `schedule.appointments().get(0).setTime(MIDNIGHT)`. Does the schedule's
     internal appointment change? `Y`/`N`
3. Run the test (below). It encodes your three predictions as assertions. Two of
   them start **red**. Read which.
4. **Fix `Schedule.java`** so all four tests pass. Do **not** touch the test.
   Make the copy genuinely defensive at **both** gates (in *and* out), for the
   `Appointment` arm. Use the copy constructor `new Appointment(a)` that is
   already provided. Returned collections must stay unmodifiable.

You are done when `mvn -Dtest=ScheduleTest test` is green and you can state, in
one sentence, why `List.copyOf` was the wrong fix for the `Appointment` arm but
the *right* fix for the `LocalDate` arm.

## Constraints

- Java 21, no libraries beyond JUnit 5. Deterministic, offline.
- Do **not** edit `ScheduleTest.java` or `Appointment.java`. Fix the production
  class `Schedule.java` only.
- Returned lists must reject mutation (`add` throws). Don't break that.
- Do not turn `Appointment` into a record or make its fields final — that would
  "solve" the exercise by deleting the trap. The point is to defend against a
  genuinely mutable element type.

## Scale up (Optional)

1. **Validation ordering (EJ49).** Add parameter validation to the constructor:
   reject `null` for either list with `Objects.requireNonNull`, and reject any
   `LocalDate` in the past with `IllegalArgumentException`. Put the validation
   *before* the copies. Then answer: if you validated *after* copying, what kind
   of object could still slip a `null` element past you, and why does ordering
   matter for a TOCTOU-style attack?
2. **Empty, not null (EJ54).** Add `appointmentsOn(LocalDate day)` that returns
   the matching appointments. When there are none, return `List.of()` — never
   `null`. Write a test proving a caller can loop over the result with no
   null-check. State why `Collections.emptyList()` / `List.of()` is allocation-
   free to hand out repeatedly.
3. **Optional, not null (EJ55).** Add `firstAppointment()` returning
   `Optional<Appointment>`. Make sure the `Appointment` inside the `Optional` is
   itself a defensive copy — an `Optional` wrapper does nothing to protect a
   mutable payload.

## How to verify

```
mvn -Dtest=ScheduleTest test
```

It starts red on Q2 and Q3 (the `Appointment` arm, in and out). After you fix
`Schedule.java`, all four tests pass. Compare your `predictions.txt` to the
actual pass/fail to see which gut calls betrayed you.

<details><summary>Hint (open only if stuck)</summary>
`List.copyOf` makes a new, unmodifiable *list* — but it copies element
*references*. For `LocalDate` (immutable) that's airtight. For `Appointment`
(mutable) the new list still points at the caller's live objects, so mutating an
element mutates yours. Defend element-wise: copy each `Appointment` on the way
**in** (in the constructor) and on the way **out** (in the accessor), then wrap
the result unmodifiable. "Deep copy" is the wrong phrase — `copyOf` does not
deep-copy, and you don't need `clone()`; you need `new Appointment(a)`.
</details>
