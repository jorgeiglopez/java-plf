# Lambda Expressions — Captured, but not frozen

**Concept:** a lambda captures the *variable* (the reference), not the *object* it points to — and "effectively final" says nothing about mutability.
**Difficulty:** ★★☆ intermediate
**Est. time:** ~15 min

## Background
Everyone "knows" lambdas can only capture effectively-final locals, and most people
translate that to "the captured value is frozen, so it's safe." Then they capture a
`StringBuilder` and call a mutating method on it inside the lambda. Run the same
supplier twice and the answer changes. Why? What exactly did `final` protect?

## Task
1. Open `CaptureLab.java`. `buildSuppliers` makes one `Supplier<String>` per seed;
   each supplier body is `() -> sb.reverse().toString()` over a captured
   `StringBuilder`.
2. **Before running anything**, predict the result of *two* consecutive passes over
   the same supplier list, seeds `["ab", "cd", "ef"]`. Write both predictions into
   the two lists marked `// <-- write your prediction` in `CaptureLabTest.java`.
3. Run the test. If it's red, reconcile your mental model with reality, then fix the
   predictions until it's green.
4. State, in one sentence, *what* the `final`/effectively-final rule actually
   freezes here.

## Constraints
- Java 21, no external libraries.
- Commit to your two predictions in the test **before** you run it or run `main`.
- Do not change `CaptureLab.java`; only edit the two prediction lists in the test.

## Scale up (Optional)
- In `PROBLEM`/your head: replace the seeds with single-character strings
  (`["a","b","c"]`). Do both passes still differ? Explain.
- The line `out.add(() -> sb.reverse().toString());` compiles. Now imagine rewriting
  the loop as `for (int i = 0; i < seeds.size(); i++) { ... out.add(() -> i); }` —
  why does *that* fail to compile while the `StringBuilder` version doesn't? Answer
  in one sentence (the answer is in `SOLUTION.md`).

## How to verify
`mvn -q -Dtest=CaptureLabTest test` — green only when both predicted passes match
the real output.

<details><summary>Hint (open only if stuck)</summary>
`reverse()` mutates the `StringBuilder` in place and returns the same instance. The
lambda holds the same object every call. What does calling `reverse()` twice on the
same two-character buffer do?
</details>
