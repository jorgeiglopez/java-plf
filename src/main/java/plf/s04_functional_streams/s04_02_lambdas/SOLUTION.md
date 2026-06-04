# Solution — Captured, but not frozen

## Ideal solution
Correct predictions for seeds `["ab", "cd", "ef"]`:

```java
List<String> predictedPass1 = List.of("ba", "dc", "fe");
List<String> predictedPass2 = List.of("ab", "cd", "ef");
```

Real output of `main`:

```
pass 1: [ba, dc, fe]
pass 2: [ab, cd, ef]
```

Pass 1 reverses each two-char buffer once (`ab`→`ba`). Each supplier holds the *same*
`StringBuilder` instance, so pass 2 calls `reverse()` on the already-reversed buffer,
flipping it back to the original (`ba`→`ab`). Odd passes differ from even passes.

One-sentence answer to step 4: effectively-final freezes the *binding* — `sb` will
always point at the same `StringBuilder` object — but it does nothing to stop that
object's internal state from mutating.

### Scale up solutions
- **Single-character seeds `["a","b","c"]`:** both passes print `[a, b, c]`. Reversing
  a one-character buffer is a no-op, so the mutation is invisible — the bug hides
  until a seed has length ≥ 2. (That's exactly how this ships to prod: it passes the
  unit test someone wrote with `"a"`.)
- **`out.add(() -> i)` with an `int i` loop counter:** that fails to compile because
  `i++` mutates `i`, so `i` is *not* effectively final (JLS 4.12.4), and a lambda may
  only capture effectively-final locals (JLS 15.27.2). The enhanced-for variable
  `seed` and the per-iteration `sb` are each assigned exactly once per iteration, so
  they *are* effectively final and capture fine. The reference being final is what the
  compiler enforces; the object behind it is none of its business.

## Why this works (and the gotcha)
A lambda captures a local by copying the *variable's value* into a synthetic final
field of the lambda's implementation. For a reference type, that value is the
reference — the address of the object — not a deep copy of the object. JLS 15.27.2
requires every captured local to be effectively final precisely so this copy can never
go stale; if the variable could be reassigned, the lambda's snapshot and the live
variable would diverge with no defined semantics.

The trap: people read "effectively final" as "immutable." It is not. The compiler
guarantees `sb` is never *rebound*; it makes no promise about `sb.reverse()`. Sharing
one mutable object across multiple lambda invocations (or multiple lambdas) is shared
mutable state with all its usual hazards — here, order-dependent results across passes.
Make the captured object immutable (a `String`, a record, a `List.copyOf`) and the
surprise disappears.

## Common mistakes
- Predicting `[ab, cd, ef]` twice ("capture froze the value") → the reference is
  frozen, the buffer is not; pass 1 mutates it.
- Predicting `[ba, dc, fe]` twice ("it reversed once and stays reversed") → each pass
  re-invokes `reverse()`, so it flips again.
- "Just make `sb` final to fix it" → it's already effectively final; `final` changes
  nothing. The fix is to stop mutating shared state (e.g. `() -> new
  StringBuilder(seed).reverse().toString()`).
- Believing the loop counter `() -> i` version "should work" → `i` is mutated by the
  loop, so it isn't effectively final and won't compile.

## Interviewer Q&A
**Q:** What does a lambda actually capture when it closes over a local variable?
**A:** The variable's current value, copied into a synthetic field. For a reference
type that value is the reference itself, not a copy of the referenced object.

**Q:** Why must captured locals be effectively final?
**A:** JLS 15.27.2 requires it so the captured copy can never become stale relative to
the source variable. Java has no concept of capturing a mutable variable by reference,
so it forbids the case entirely rather than define surprising semantics.

**Q:** Does "effectively final" mean the captured object is immutable?
**A:** No. It constrains the *variable* (no reassignment), not the *object*. A captured
`StringBuilder`, array, or collection can be freely mutated through the lambda.

**Q:** In this exercise, why does pass 2 differ from pass 1?
**A:** All suppliers share the same `StringBuilder` instances. `reverse()` mutates in
place, so the first pass reverses each buffer and the second pass reverses it back.

**Q:** How would you make the supplier safe / idempotent?
**A:** Don't share mutable state: build a fresh `StringBuilder` inside the lambda
(`() -> new StringBuilder(seed).reverse().toString()`), or capture an immutable value
and compute without mutation.

**Q:** Why does `for (int i = ...; i++) out.add(() -> i)` fail to compile, but the
enhanced-for `StringBuilder` version compiles?
**A:** `i++` reassigns `i`, so it isn't effectively final and can't be captured. The
enhanced-for variable and the per-iteration `sb` are each assigned once per iteration,
so they're effectively final.

**Q:** Lambda vs anonymous class — does either change this capture story?
**A:** No. Both capture effectively-final locals with the same value-copy semantics
(EJ42). They differ elsewhere — e.g. `this` inside a lambda is the enclosing instance,
while inside an anonymous class `this` is the anonymous object — but the capture rule
is identical.

**Q:** Is the `StringBuilder` reference itself final inside the lambda's generated
class?
**A:** Yes — it's stored in a synthetic final field of the lambda implementation, which
is exactly why mutation of the object (not the field) is the only thing that can vary.
