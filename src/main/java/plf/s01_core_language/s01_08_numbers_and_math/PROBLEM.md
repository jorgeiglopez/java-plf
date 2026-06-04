# Numbers and Math — The Rounding Heist

**Concept:** "I used `BigDecimal`, so I'm safe" is a lie. The constructor you pick, the comparison method, and the `divide` overload all decide whether your money is correct.
**Difficulty:** ★★☆ intermediate
**Est. time:** ~20 min

## Background
A teammate wrote a `PriceBook` for summing money. It does everything the
internet told them to do: no `double` arithmetic, `BigDecimal` everywhere, an
explicit `RoundingMode`. The test is still **red**. A single price of `$1.005`
rounds to `$1.00` instead of `$1.01`, "1.0 dollars" is reported as a *different*
amount from "1.00 dollars", and splitting a bill three ways blows up with an
exception. Every bug is the kind you only catch if you know what `BigDecimal`
actually stores. Can you find all three?

> The reflex you've rehearsed — *"HALF_UP on a `.005` rounds down because floats
> lie"* — is exactly what detonates here. The expected value really is `1.01`.

## Task
The scaffold `PriceBook.java` ships with **three planted bugs** in PRODUCTION
code. Fix them until `PriceBookTest` is green.

1. **Predict first (commit on paper before you run anything).** Fill in this
   table with what each expression evaluates to in Java 21:

   | # | Expression | Your prediction |
   |---|------------|-----------------|
   | 1 | `new BigDecimal(1.005).setScale(2, HALF_UP)` | ? |
   | 2 | `new BigDecimal("1.005").setScale(2, HALF_UP)` | ? |
   | 3 | `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` | ? |
   | 4 | `new BigDecimal("1.0").compareTo(new BigDecimal("1.00"))` | ? |
   | 5 | `BigDecimal.ONE.divide(BigDecimal.valueOf(3))` | ? |
   | 6 | `BigDecimal.valueOf(1.005)` | ? |

2. **Fix `PriceBook.roundedTotal`** (Bug 1) so `1.005` totals to `1.01`.
3. **Fix `PriceBook.sameAmount`** (Bug 2) so `1.0` and `1.00` count as the same
   amount of money.
4. **Fix `PriceBook.perPersonShare`** (Bug 3) so splitting `1.00` across `3`
   people returns `0.33` instead of throwing.
5. Run the test. Then compare rows 1–6 of your table against `SOLUTION.md` and
   note any you got wrong — that gap is the lesson.

## Constraints
- Java 21, no external libraries (JUnit 5 only).
- Edit **only** `PriceBook.java`. Do **not** modify `PriceBookTest.java`.
- Do not "fix" anything by using `double` arithmetic or by changing the
  expected values in the test.
- `perPersonShare` must use **scale + `RoundingMode`** (the right tool for
  money), not a `MathContext` (significant-figure precision).

## Scale up (Optional)
The same "the obvious answer is wrong" trap lives in integer math. Predict each,
then verify (these are markdown-only — reason them out, the answers are in
`SOLUTION.md`):

1. `Integer.MAX_VALUE + 1` — what is it, and why?
2. `Math.abs(Integer.MIN_VALUE)` — does `abs` always return a non-negative `int`?
3. `Math.addExact(Integer.MAX_VALUE, 1)` vs the plain `+` above — what's the
   difference in behavior?
4. `Math.negateExact(Integer.MIN_VALUE)` — does it succeed?
5. `Math.floorMod(-7, 3)` vs `-7 % 3` — same result?

## How to verify
```
mvn -q -Dtest=PriceBookTest test
```
The test starts **red** (4 failures, 1 pass) and must end fully **green** once
all three production bugs are fixed.

<details><summary>Hint (open only if stuck)</summary>
`new BigDecimal(double)` stores the *exact binary* value of the double, so
`1.005` is really `1.00499999…` and rounds down. `BigDecimal.valueOf(double)`
and `new BigDecimal(String)` do not. `equals` on `BigDecimal` also compares
*scale*; `compareTo == 0` compares value only. And `divide` with no scale throws
on a non-terminating quotient — give it a scale and a `RoundingMode`.
</details>
