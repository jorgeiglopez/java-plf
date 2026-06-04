# Solution — The Rounding Heist

## Prediction table (the answers)

| # | Expression | Result | Why |
|---|------------|--------|-----|
| 1 | `new BigDecimal(1.005).setScale(2, HALF_UP)` | **`1.00`** | the `double` ctor stores `1.00499999999999989…`, so HALF_UP rounds **down** |
| 2 | `new BigDecimal("1.005").setScale(2, HALF_UP)` | **`1.01`** | the `String` ctor stores exactly `1.005`; HALF_UP rounds the exact half **up** |
| 3 | `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` | **`false`** | `equals` compares value **and scale** (scale 1 ≠ scale 2) |
| 4 | `new BigDecimal("1.0").compareTo(new BigDecimal("1.00"))` | **`0`** | `compareTo` ignores scale; same numeric value |
| 5 | `BigDecimal.ONE.divide(BigDecimal.valueOf(3))` | **throws `ArithmeticException`** | "Non-terminating decimal expansion; no exact representable decimal result." |
| 6 | `BigDecimal.valueOf(1.005)` | **`1.005`** | `valueOf` routes through `Double.toString`, giving the shortest round-trip decimal — not the binary leak |

Rows 1 vs 2 are the heist: **same literal, same scale, same rounding mode,
opposite cent.** The only difference is which constructor the value passed
through.

## Ideal solution
Three one-line fixes in `PriceBook.java`:

```java
package plf.s01_core_language.s01_08_numbers_and_math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class PriceBook {

    private PriceBook() { }

    // FIX 1: BigDecimal.valueOf(double) (via Double.toString) — or take a
    // String — instead of the binary-leaking new BigDecimal(double).
    public static BigDecimal roundedTotal(List<Double> prices) {
        BigDecimal total = BigDecimal.ZERO;
        for (double p : prices) {
            total = total.add(BigDecimal.valueOf(p));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // FIX 2: compareTo == 0 compares numeric value only (scale-insensitive).
    public static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    // FIX 3: divide(divisor, scale, RoundingMode) — bounded result, no throw.
    public static BigDecimal perPersonShare(BigDecimal total, int people) {
        return total.divide(BigDecimal.valueOf(people), 2, RoundingMode.HALF_UP);
    }
}
```

> **Even cleaner:** money should never be a `double` in the first place. If the
> API took `List<String>` (or `List<BigDecimal>` built from strings), Bug 1
> could not exist. Bug 1 is really "a `double` reached your money path at all".

## Why this works (and the gotcha)

**Bug 1 — the constructor poison.** `new BigDecimal(double)` is *exact*: it
captures the full binary value of the `double`. The literal `1.005` cannot be
represented in IEEE-754 double, so the nearest double is `1.00499999999999…`,
and `BigDecimal` faithfully stores all of it. HALF_UP then sees a digit *below*
the half and rounds **down** to `1.00`. The fix is to never let a binary
`double` define the value: `BigDecimal.valueOf(1.005)` gives `1.005` because it
goes through `Double.toString` (shortest decimal that round-trips), and
`new BigDecimal("1.005")` is exact by construction. With either, HALF_UP rounds
the genuine half **up** to `1.01`. The trap is that the smug dev "used
`BigDecimal`" — but piping a `double` through `new BigDecimal(double)` imports
the float lie they thought they'd escaped.

**Bug 2 — scale is part of `equals` identity.** `BigDecimal` is value + scale.
`1.0` (scale 1) and `1.00` (scale 2) are equal *amounts* but **not** `equals`,
because `equals` requires identical scale. For "same amount of money" you want
`compareTo(other) == 0`. (Consequence: never use `BigDecimal` as a `HashSet`
element or `HashMap` key if you expect `1.0` and `1.00` to collide — they hash
differently. Use `stripTrailingZeros`-normalized values, or a `TreeMap` with a
value comparator.)

**Bug 3 — `divide` with no scale demands an exact result.** The single-arg
`divide(divisor)` returns a result with a preferred scale and throws
`ArithmeticException` when the exact quotient is a non-terminating decimal (e.g.
`1/3`). For money, give it a scale and a `RoundingMode` —
`divide(divisor, 2, HALF_UP)`. Prefer the **scale** overload over a
`MathContext` (which controls *significant figures*, not decimal places): money
is about cents (a fixed number of decimal places), not significant digits.

## Common mistakes
- **"`BigDecimal` means I'm safe."** Only if no `double` ever defines a value.
  `new BigDecimal(double)` re-imports the binary error you were avoiding.
- **Expecting `1.005` to round down "because floats."** With the *string* /
  `valueOf` constructor the value is an exact half, and HALF_UP rounds it **up**
  to `1.01`. The float lie only appears with the `double` constructor.
- **Comparing money with `equals`.** Returns `false` for `1.0` vs `1.00`. Use
  `compareTo == 0`.
- **`divide` with no scale.** Throws on `1/3`, `1/7`, `10/3`, etc. Always pass a
  scale + `RoundingMode` for division you intend to keep.
- **"Fixing" Bug 3 with `MathContext`.** It rounds to *significant figures*, so
  `new BigDecimal("100.00").divide(THREE, new MathContext(2))` gives `33`, not
  `33.33`. Wrong tool for currency.

## Scale up solutions

1. **`Integer.MAX_VALUE + 1` → `-2147483648` (`Integer.MIN_VALUE`).** `int` is
   32-bit two's complement; `+` wraps silently on overflow. No exception.
2. **`Math.abs(Integer.MIN_VALUE)` → `-2147483648` (still negative!).** Two's
   complement is asymmetric: there are `2^31` negatives but only `2^31 − 1`
   positives, so `MIN_VALUE` has no positive counterpart. `abs` computes
   `-MIN_VALUE`, which overflows back to `MIN_VALUE`. `abs` does **not**
   guarantee a non-negative result for `int`/`long`.
3. **`Math.addExact(Integer.MAX_VALUE, 1)` throws `ArithmeticException:
   integer overflow`.** The `*Exact` methods detect overflow and fail loudly
   instead of wrapping — use them when a wrong-but-silent number is dangerous
   (money, sizes, indices).
4. **`Math.negateExact(Integer.MIN_VALUE)` throws `ArithmeticException:
   integer overflow`** — the same asymmetry as #2, but it fails loudly instead
   of silently returning `MIN_VALUE`.
5. **`Math.floorMod(-7, 3)` → `2`; `-7 % 3` → `-1`.** `%` is *remainder* and
   takes the sign of the dividend; `floorMod` is true *modulo* and takes the
   sign of the divisor. For "bucket index from a possibly-negative hash", you
   want `floorMod` (always in `[0, divisor)`).

## Interviewer Q&A

**Q:** A colleague says "I used `BigDecimal`, so my money code is correct." Is
that enough?
**A:** No. `BigDecimal` removes float arithmetic error, but you can still
re-introduce it via `new BigDecimal(double)`, get wrong equality via `equals`
vs `compareTo`, and throw on `divide` with no scale. Correctness depends on
*how* you use it, not merely that you use it.

**Q:** What's the difference between `new BigDecimal(1.005)`,
`BigDecimal.valueOf(1.005)`, and `new BigDecimal("1.005")`?
**A:** `new BigDecimal(double)` stores the exact binary value of the double
(`1.00499999…`). `valueOf(double)` routes through `Double.toString`, giving the
shortest decimal that round-trips (`1.005`). `new BigDecimal(String)` parses the
literal exactly (`1.005`). Only the first carries the binary leak, so only it
rounds `1.005` down to `1.00`.

**Q:** `new BigDecimal("1.005").setScale(2, HALF_UP)` — what do you get, and
isn't HALF_UP supposed to round halves away from zero?
**A:** `1.01`. The string ctor stores an exact `1.005`, so the discarded digit
is exactly a half and HALF_UP rounds **up**. People expect `1.00` because they're
thinking of the *double* ctor, where the value is actually `1.00499…` (below the
half) and rounds down.

**Q:** Why is `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` false, and
how do you test "same amount"?
**A:** `BigDecimal.equals` compares unscaled value **and** scale; `1.0` has
scale 1, `1.00` scale 2, so they differ. For numeric equality use
`a.compareTo(b) == 0`, which ignores scale.

**Q:** What happens with `BigDecimal.ONE.divide(BigDecimal.valueOf(3))` and how
do you fix it?
**A:** It throws `ArithmeticException` ("Non-terminating decimal expansion")
because `1/3` has no exact finite decimal and no rounding was specified. Fix it
with `divide(divisor, scale, RoundingMode)`, e.g.
`divide(valueOf(3), 2, HALF_UP)` → `0.33`.

**Q:** For money division, would you reach for `MathContext` or a scale +
`RoundingMode`? Why?
**A:** Scale + `RoundingMode`. `MathContext` controls *significant figures*, so
`100.00 / 3` with `MathContext(2)` is `33`, not `33.33`. Currency wants a fixed
number of *decimal places* (cents), which is exactly what `scale` means.

**Q:** `Math.abs(Integer.MIN_VALUE)` returns a negative number. How is that
possible?
**A:** Two's complement is asymmetric — `-2^31` has no positive counterpart in
`int`. `abs` computes `-MIN_VALUE`, which overflows and wraps back to
`MIN_VALUE`. So `abs` is **not** guaranteed non-negative for `int`/`long`; for
guaranteed-safe code use `Math.absExact` (throws) or widen to `long`.

**Q:** When would you choose `Math.addExact`/`multiplyExact` over plain
operators, and what about `Math.floorMod` over `%`?
**A:** Use the `*Exact` methods when a silently-wrapped result is dangerous
(financial totals, allocation sizes, index math) — they throw on overflow
instead of producing a corrupt value. Use `floorMod` when you need a result with
the divisor's sign (always in `[0, n)`), e.g. mapping a possibly-negative hash to
a bucket index; `%` keeps the dividend's sign and can return a negative.
