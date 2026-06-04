# Primitives & Wrappers — Silent Wrong vs. Loud Dead

**Concept:** autoboxing/unboxing is invisible in the source, and it bites in two
opposite ways — a quietly *wrong* value, and a loud `NullPointerException` —
both from the same root cause.
**Difficulty:** ★★☆ intermediate
**Est. time:** ~15 min

## Background

You will read two map lookups. Both compile. Both look obviously correct. One
returns a value that is quietly, dangerously *wrong*; the other detonates at
runtime. Neither has a `==` in sight, and neither shows you a single `Integer`
or `Long` in the expression — the boxing happens where you cannot see it. Can
you predict, exactly, what each one does **before** you run it?

The trap: developers stare at `==` and the `Integer` cache, the participation
trophy of Java gotchas. The lookups that actually ship bugs in 2026 are these
two, where unboxing is silent until it isn't.

## Task

Edit only the test: `src/test/java/plf/.../s01_01_primitives_and_wrappers/SilentUnboxingTest.java`.

1. **Commit first, run never (yet).** Do **not** run the code. Read
   `SilentUnboxing.java`:
   - `silentWrong()` — a `Map<Long, String>` is `put(1L, "one")`, then looked up
     with `get(1)`.
   - `loudDead()` — `int n = counts.get("absent")` where `"absent"` is not a key.
2. **Line 1 — predict the silent value.** Replace the placeholder constant
   `PREDICTED_SILENT_WRONG` in the test with the **exact** value you believe
   `silentWrong()` returns. Your prediction is the *first argument* of an
   `assertEquals` — there is no comment to peek past; the red bar judges you.
3. **Line 2 — predict the failure mode.** The test already asserts that
   `loudDead()` throws. Decide for yourself whether you agree it throws (and
   *which* exception) before you trust that line.
4. **Run, get judged, then read the SOLUTION** to confirm the *mechanism* — not
   just the value — for each line.

"Done" = the test is green because both predictions were right, and you can
explain in one sentence *why* each line behaves as it does.

## Constraints

- Java 21, no external libraries beyond JUnit 5.
- Do **not** modify `SilentUnboxing.java`. Do **not** run a compiler/test until
  you have committed your prediction.
- Edit only the two prediction sites in the test (the constant, and your
  judgement of the `assertThrows` line). Do not change the methods under test.

## Scale up (optional, ascending difficulty)

Predict each value/output *before* checking it against `SOLUTION.md`. All are
deterministic on Java 21.

1. **The cache boundary (warm-up).**
   ```java
   Integer a = 127, b = 127;     // a == b ?
   Integer c = 1000, d = 1000;   // c == d ?
   ```
   Which is `true`, which is `false`, and what is the exact boundary?

2. **Cross-type `equals` has no mercy.**
   ```java
   Long  x = 1L;
   System.out.println(x == 1);          // ?
   System.out.println(x.equals(1));     // ?
   ```
   Why does one say `true` and the other `false`?

3. **The comparator that compiles its own death.**
   ```java
   List<Integer> nums = new ArrayList<>(List.of(-2_000_000_000, 2_000_000_000));
   nums.sort((p, q) -> p - q);   // "elegant"; predict nums after sorting
   ```
   Predict the resulting list. Then sort the same input with `Integer::compare`
   and explain the difference. (Hint: the boxing is the *misdirection* here.)

## How to verify

```
mvn -q -Dtest=SilentUnboxingTest test
```

Green only when both predictions are exactly right. For the scale-ups, run them
in `jshell` or a scratch `main` and compare to `SOLUTION.md`.

<details><summary>Hint (open only if stuck)</summary>
`Map.get` takes `Object`, so the argument is *boxed* before the lookup. What
type does the literal `1` box to? Is that the same type as the key you stored?
And for line 2: what does it mean to assign a possibly-`null` `Integer` into an
`int`?
</details>
