# Identity vs Equality — One Method, Two Ops, Half Visible

**Concept:** Java is *always* pass-by-value — the value passed is a **copy of the reference**. Mutating the shared object is visible to the caller; reassigning the copied reference is not. (`==` and `equals()` on `StringBuilder` are both identity.)
**Difficulty:** ★★☆ intermediate
**Est. time:** ~15 min

## Background
"Java passes objects by reference," right? Then this method should both mutate **and** rebind the caller's variables. Watch closely: the args at the call site are `a` and `b`, but the parameters are named `x` and `y` — `x` is a *copy* of the reference `a` held; it merely happens to point at the same object.

```java
static void f(StringBuilder x, StringBuilder y) {
    x.append("!");   // op 1
    y = x;           // op 2
}
```

Caller:

```java
StringBuilder a = new StringBuilder("A");
StringBuilder b = new StringBuilder("B");
StringBuilder origA = a;   // remember the original objects
StringBuilder origB = b;
f(a, b);
```

One of these two operations is visible to the caller. One is not. If you believe "pass-by-reference," you will get exactly half of this right — which is how you'll know you don't have the model yet.

## Task
1. **Predict first, in writing.** Before running anything, fill in your predictions for these five lines, and next to each of lines 1–4 write **one word**: `mutate`, `rebind`, or `untouched`.

   | # | Expression | Your prediction | One word |
   |---|------------|-----------------|----------|
   | 1 | `a` (its text after the call) | | |
   | 2 | `b` (its text after the call) | | |
   | 3 | `a == origA` | | |
   | 4 | `b == origB` | | |
   | 5 | `a.equals(b)` | | |

2. **Commit, then verify.** Open the red test `HalfVisibleCallTest`. It encodes the "pass-by-reference" prediction and therefore **starts failing**. Run it, read the real values off the failure messages, and fix the four `EXPECTED_*` constants so the test goes green.
3. **Do NOT touch `IdentityLab.f`.** The production method is correct. Only the *predictions* in the test are wrong.
4. Run `IdentityLab.main` and confirm the five printed lines match your corrected predictions.

## Constraints
- Java 21, no external libraries (JUnit 5 only).
- Do not modify `IdentityLab.f` — fix only the test's `EXPECTED_*` constants.
- Commit your predictions on paper/in comments *before* running anything.

## Scale up (Optional)
Same lie, new costume — find the copy. Each of these is the "the variable is a copy" rule wearing trivia. Predict `true`/`false` for each, then check against `SOLUTION.md`.

1. **Integer cache.** `Integer p = 127, q = 127; p == q` and then `Integer r = 128, s = 128; r == s`. Why do they differ? What does `.equals` give in both cases?
2. **String constant folding vs runtime concat.** With `String z = "b";`, predict `"ab" == "a" + "b"`, then `"ab" == ("a" + z)`, then `"ab" == ("a" + z).intern()`.
3. State, in one sentence, the single rule that explains the `StringBuilder` trap, the `Integer` cache, and the `String` interning result.

## How to verify
```
mvn -q -Dtest=HalfVisibleCallTest test
```
The test starts red. It is green once your four `EXPECTED_*` constants match Java's real behavior. Then run `IdentityLab.main` and compare its five lines to your table.

<details><summary>Hint (open only if stuck)</summary>
A method parameter is a fresh local variable initialized with a copy of the
argument. For an object argument, the copied value is the *reference*, not the
object. So `x.append(...)` reaches through to the shared object (visible to the
caller), but `y = x` only re-points the local copy `y` — the caller's `b` never
moves. Ask, for each line: did the *object* change, or did a *variable* get
re-pointed?
</details>
