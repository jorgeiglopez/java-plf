# String Handling — The Two Rows That Lie

**Concept:** `==` on concatenations is decided by *compile-time constant expression* (JLS 15.29), **not** by the `final` keyword.
**Difficulty:** ★★☆ intermediate
**Est. time:** ~15 min

## Background

Everyone "knows" Java interns string literals, so `"a" + "b" == "ab"` is `true`.
Then they "learn" that `final` makes a variable foldable, and they generalize.
That generalization is a trap. Two lines of code can be **char-for-char
identical** except for how the left operand was initialized — and one is `true`
while the other is `false`. Can you predict which, *before* the compiler tells
you?

## Task

You will fill in a **prediction ledger on paper first**, then run a test that
asserts the naive guess and watch it detonate.

1. Open `ConstantVariableLedgerTest.java` (under
   `src/test/java/plf/s01_core_language/s01_04_strings/`). Study the three
   expressions below. `runtime()` returns `new String("a")`.

   | # | Code                                                  | `==` ? | `.equals` ? |
   |---|-------------------------------------------------------|--------|-------------|
   | 1 | `final String s = "a";       (s + "b") == "ab"`       |  ___   |     ___     |
   | 2 | `final String s = runtime(); (s + "b") == "ab"`       |  ___   |     ___     |
   | 3 | `String s = "a";             (s + "b") == "ab"`       |  ___   |     ___     |

2. **Commit your four `==` predictions and three `.equals` predictions in ink**
   (write them down — do NOT run anything yet). The `.equals` column is part of
   the lesson: decide it too.
3. Now run the test. Row 1 is asserted correctly and passes. **Row 2 is asserted
   with the naive guess and starts red.** Read the failure trace.
4. Fix the single wrong assertion in `row2_finalButRuntimeInit_isInterned()` so
   it matches reality. Do **not** touch `row1`.
5. In one sentence, explain why Rows 2 and 3 land on the *same* `==` answer
   despite Row 2 using `final` and Row 3 not.

## Constraints

- Java 21, no external libraries beyond JUnit 5.
- Predict on paper **before** compiling — the whole exercise is the wrong guess
  in ink. Running it first teaches you nothing.
- You may only edit the asserted boolean in `row2`. Do not weaken the test by
  switching `==` to `.equals` — that erases the point.

## Scale up (Optional)

1. **`intern()` round-trip.** What does `(s + "b").intern() == "ab"` return for
   the Row 2 `s`? Add an assertion and predict first.
2. **`new String` identity.** Predict `new String("x") == "x"` and
   `new String("x").intern() == "x"`. Why do they differ?
3. **Text-block whitespace.** Predict the `length()` of
   ```java
   String tb = """
           hi""";
   ```
   versus the same block with `hi` on its own line followed by `"""` on the next
   line. Which one ends in `\n`? Assert your answer with `.equals`.

## How to verify

```
mvn -q -Dtest=ConstantVariableLedgerTest test
```

Before your fix: 1 failure in `row2` with trace `expected: <true> but was:
<false>` at the asserted line. After your fix: both tests green.

<details><summary>Hint (open only if stuck)</summary>
The compiler can fold an expression only when every operand is itself a
*constant expression*. A `final` local counts as a "constant variable" **only if
its initializer is itself a constant expression** (JLS 4.12.4). A method call is
never a constant expression — so `runtime()` disqualifies the variable, and the
`+` becomes a runtime `StringBuilder.append` that allocates a brand-new (un-pooled)
`String`.
</details>
