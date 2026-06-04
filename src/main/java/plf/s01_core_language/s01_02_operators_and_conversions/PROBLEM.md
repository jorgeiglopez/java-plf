# Operators & Type Conversions — The Saved Operand

**Concept:** compound assignment (`op=`) hides a narrowing cast AND saves its left operand before the right side runs (JLS 15.26.2).
**Difficulty:** ★★★ senior
**Est. time:** ~15 min

## Background

`+=` looks like a harmless shortcut. It isn't. `E1 op= E2` is secretly
`E1 = (T)(E1 op E2)` — there is an *invisible cast* back to the type of the left
side, and the left side is *read once, before the right side runs*. One line
weaponizes both facts:

```java
byte b = 100;
b += b += b;     // what is b now?
```

Most people compute `100 + 100 + 100 = 300`, remember `byte` overflows, and
guess some wrapped value. They are wrong about the number **and** wrong about the
path. Can you trace it exactly?

## Task

You will commit to the full trace **before** you compile.

1. First, write down — on paper or in a comment — your predictions for these
   **four rows**. Note the operator associativity: `+=` is *right-associative*,
   so the line is `b += (b += b)`.

   | Row | Expression | Your prediction |
   |-----|-----------|-----------------|
   | R1  | `b + b` (the inner sum) — value **and type** | |
   | R2  | the `byte` actually **stored** by the inner `b += b` | |
   | R3  | the outer left-hand `b` value that is **used** in the final add | |
   | R4  | the final `byte` stored in `b` after the whole line | |

2. Now transcribe those four predictions into the scaffold
   [`CompoundAssignmentTrace.java`](./CompoundAssignmentTrace.java): replace each
   `throw new UnsupportedOperationException("TODO")` in `r1_innerSum`,
   `r2_innerStored`, `r3_outerSavedLhs`, and `r4_finalValue` with a `return` of
   your predicted value (mind the return types — two are `int`, two are `byte`).

3. Do **not** touch `runTheLine()`, `runTheVarLine()`, or the test. Run the test.
   It asserts the *real* values. Where you are wrong, the build goes red — fix the
   row, and then write one sentence explaining *why* that row is what it is.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Commit to all four rows **before** running the test.
- Do not modify the test file or the two `runThe...` methods.

## Scale up (Optional)

The trap lives entirely in the type `byte`. Change nothing else, just the type:

```java
var v = 100;
v += v += v;      // now what?
```

Predict the result, then make `varScaleUpInfersIntSoNoNarrowing` pass by reading
what `runTheVarLine()` returns. Explain in one line why swapping `byte` for `var`
makes the entire trap evaporate — that contrast *is* the lesson.

## How to verify

```
mvn -q -Dtest=CompoundAssignmentTraceTest test
```

All six assertions must go green. (Hint: the test starts red because the four
trace methods throw.)

<details><summary>Hint (open only if stuck)</summary>
The left side is read once, before the right side runs. So when the outer `+=`
finally adds, it adds the value the left `b` had at the *start* of the line — not
whatever the inner assignment just stored.
</details>
