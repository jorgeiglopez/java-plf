# Solution — The Saved Operand

## Ideal solution

`byte b = 100; b += b += b;` leaves **`b == 44`**. The four trace rows, filled in:

```java
public static int  r1_innerSum()      { return 200; }   // R1: b + b is an int, value 200
public static byte r2_innerStored()   { return -56; }   // R2: (byte)200 == -56
public static int  r3_outerSavedLhs() { return 100; }   // R3: outer LHS saved BEFORE rhs ran
public static byte r4_finalValue()    { return 44;  }   // R4: (byte)(100 + -56) == 44
```

(The compiler accepts a plain `-56` and `44` as `byte` returns because they are
constant expressions in range; no cast is needed in the source.)

### The trace, step by step

`+=` is **right-associative**, so the line parses as `b += (b += b)`. Both
`+=` are compound assignments, and JLS 15.26.2 expands each to
`b = (byte)(b + <rhs>)` with the left `b` evaluated *first* and its value saved.

1. **Outer `+=` reads its left operand.** The outer `b` is fetched and its value
   **saved: `100`**. This save happens *before* the right-hand side runs. (R3)
2. **Right-hand side runs: the inner `b += b`.**
   - `b + b` = `100 + 100`. Operands are promoted to `int` (binary numeric
     promotion), so this is the **`int` value `200`** — *not* a `byte`. (R1)
   - The inner `+=` casts back: `(byte)200`. `byte` is 8 bits, two's complement,
     range −128..127. `200 - 256 = -56`, so it stores and yields **`-56`**. (R2)
3. **Outer `+=` completes** using the operand it saved in step 1:
   `b = (byte)(100 + (-56))` = `(byte)44` = **`44`**. (R4)

So the real path is `200 → -56 → 44`. The seductive `300 → 44` story is wrong:
`300` is never computed, and the outer add uses the *saved* `100`, not the `-56`
that the inner assignment just wrote into `b`.

### Scale up solution

```java
var v = 100;
v += v += v;     // ==> 300
```

`var` infers the type of the initializer `100`, which is **`int`**. With no
`byte` in sight there is no narrowing cast, so nothing wraps. The same saved-LHS
rule still applies — `v = 100 + (v = 200)` — but in `int` the arithmetic is
exact: `100 + 200 = 300`. Swapping `byte` for `var` deletes the invisible cast,
and the entire trap evaporates. That contrast is the whole lesson: the danger was
never the operator, it was the *narrowing type behind it*.

## Why this works (and the gotcha)

There are **two** independent senior traps stacked on one line:

1. **The invisible narrowing cast.** `E1 op= E2` is `E1 = (T)(E1 op E2)`. The
   cast is why `byte b = 10; b += 5;` compiles even though `b = b + 5;` does
   **not** — the latter has no implicit cast, so javac (Java 21) rejects it:
   ```
   error: incompatible types: possible lossy conversion from int to byte
   ```
   The compound form silently casts; the trap is that it does so *even when the
   result overflows*, with no warning.

2. **The saved left operand (the kill).** JLS 15.26.2: *"the value of the left-hand
   operand is saved and then the right-hand operand is evaluated."* So in
   `b += (b += b)` the outer `b` contributes the value it held **before** the
   line started (`100`), not the value the inner assignment leaves behind
   (`-56`). Miss this and you compute `(byte)(-56 + -56)` or `(byte)(-56 + 100)`
   from the wrong direction — either way you reason your way to a wrong story for
   the answer, which in an interview is as bad as a wrong number.

The two's-complement step (R2) is its own mini-trap: `(byte)200` is **not** a
positive "low byte" of 200. Bit pattern `1100_1000` has the sign bit set, so it
is `200 - 256 = -56`. People who answer "low-byte truncation gives a positive
number" trip here.

## Common mistakes

- **`300 → (byte)300 = 44`** → right answer, wrong path. `300` is never formed;
  the inner narrowing to `-56` happens first, and the outer add uses the saved
  `100`. Getting `44` for the wrong reason fails the "explain it" follow-up.
- **Outer add uses `-56`** (`(byte)(-56 + -56) = -112`) → assumes the outer `+=`
  re-reads `b` *after* the inner store. It doesn't; the LHS was saved first.
- **`(byte)200` is positive** → forgets the sign bit; it's `-56`, not `+72` or
  `+200`.
- **"`b + b` stays a `byte`"** → forgets binary numeric promotion; `b + b` is an
  `int` (`200`), which is exactly why it can exceed the `byte` range.
- **Expecting `b = b + 5` to compile like `b += 5`** → only the compound form
  carries the implicit narrowing cast; the explicit form is a compile error.
- **`var` infers `byte`** → no. `var` infers the initializer's type; `100` is an
  `int` literal, so `var v = 100` is an `int`, and the scale-up is `300`.

## Interviewer Q&A

**Q:** What is `b` after `byte b = 100; b += b += b;`, and what type is `b + b`?
**A:** `b` is `44`. `b + b` is an `int` (`200`) because binary numeric promotion
widens both `byte` operands to `int` before the addition.

**Q:** Why does `byte b = 10; b += 5;` compile but `b = b + 5;` does not?
**A:** `b += 5` desugars to `b = (byte)(b + 5)` — a compound assignment carries an
implicit narrowing cast (JLS 15.26.2). `b = b + 5` has no such cast, and `b + 5`
is an `int`, so javac reports *"incompatible types: possible lossy conversion
from int to byte."*

**Q:** Trace `b += b += b` precisely. Why is the answer not built from `300`?
**A:** `+=` is right-associative: `b += (b += b)`. The outer `+=` saves its LHS
(`100`) first. Then the inner `b += b` computes `100 + 100 = 200` (int), casts to
`(byte)200 = -56`, and stores it. The outer add then uses the *saved* `100`:
`(byte)(100 + -56) = 44`. `300` is never computed.

**Q:** Why is `(byte)200` equal to `-56` rather than a positive number?
**A:** `byte` is 8-bit two's complement (range −128..127). `200`'s low byte is
`1100_1000`; the sign bit is set, so the value is `200 - 256 = -56`. It is sign
reinterpretation, not a positive "low-byte truncation."

**Q:** Which clause of JLS 15.26.2 is load-bearing here, and what does it say?
**A:** The rule that for `E1 op= E2`, the left operand `E1` is evaluated and its
value **saved**, *then* `E2` is evaluated, *then* the op and the narrowing cast
are applied. That save-before-RHS ordering is why the outer add uses `100`, not
the `-56` the inner store produced.

**Q:** Change `byte` to `var`: `var v = 100; v += v += v;`. Result, and why?
**A:** `300`. `var` infers the initializer's type — `100` is an `int` literal, so
`v` is `int`. With no narrowing cast, nothing wraps: `v = 100 + (v = 200) = 300`.
The saved-LHS rule still applies, but in `int` the arithmetic is exact.

**Q:** Does `+=` ever change *evaluation order* of side effects, e.g. with
`a[i++] += expr`?
**A:** Yes — the LHS, including its subexpressions, is fully evaluated and saved
first. In `a[i++] += expr`, `i++` runs once (the array reference and index are
computed and saved), then `expr`, then the store goes back to that *same* saved
element. The index is not re-evaluated for the store.

**Q:** How would you write the same intent safely so a reviewer isn't surprised?
**A:** Don't nest assignments inside compound assignments, and don't rely on
implicit narrowing. Use an explicit type and explicit cast:
`int sum = b + b + b; b = (byte) sum;` — now the promotion, the total, and the
narrowing are all visible, and the reader doesn't need JLS 15.26.2 to predict the
result.
