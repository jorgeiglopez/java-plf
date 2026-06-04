# Raw Types & Unchecked Warnings — The Suppression That Lied

**Concept:** an `@SuppressWarnings("unchecked")` is a *promise* to the compiler that a cast is safe. When the promise is false, the heap is silently polluted and the `ClassCastException` detonates far from the crime scene.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

A teammate shipped this "obviously safe" utility:

```java
@SuppressWarnings("unchecked")
public static <T> List<T> copyOf(Class<T> type, Collection<?> src) {
    return (List<T>) new ArrayList<Object>(src);
}
```

Reviewers waved it through — "it just copies, what could go wrong?" Then production threw a `ClassCastException` from a stack frame that *never mentions `copyOf`*. The only honest signal — the unchecked-cast warning — had been muzzled with a comment-free `@SuppressWarnings`.

The method even *takes a `Class<T>` token* and then ignores it. That token is the whole point: it is the runtime evidence needed to make the cast real instead of a lie.

## Task

Open `SafeCopy.java` and `src/test/java/.../SafeCopyTest.java`. Then:

1. **Predict the crash, on paper, before running anything.** In `SafeCopyTest.copyMustFailFastOnTypeViolation`, mixed input flows into `copyOf(String.class, poison)`. With the *current* (broken) body, no exception is thrown there — the poison leaks. Write one sentence stating *which later line* a `ClassCastException` would fire on if that polluted list were instead passed to `processFirst`, and *why* the cast is compiler-inserted and invisible in the source.

2. **Fix `copyOf` so it fails fast.** Edit only the body inside the marked region. Use the `Class<T>` token to validate every element as you copy, so a type violation throws a `ClassCastException` *at the copy site* — not three call frames downstream. `processFirst` is off-limits; do not touch it.

3. **Make the suppression honest.** Either delete `@SuppressWarnings("unchecked")` entirely (if your fix needs no unchecked cast), or, if you keep one, replace it with the narrowest-scoped suppression possible plus a one-sentence invariant comment, adjacent to the annotation, that is *actually true* per JLS 4.12.2 (heap pollution). "Safe because we copy the elements" is **wrong** — copying references does not type-check them.

## Constraints

- Java 21, no external libraries.
- Edit **only** the marked region in `copyOf`. Do **not** modify `processFirst` or the test file.
- The fix must compile clean under `javac -Xlint:unchecked` (no leftover unjustified suppression).
- No raw types in your fixed signature or body.

## How to verify

```
mvn -q -Dtest=SafeCopyTest test
```

Both tests green. `legitimateCopyPreservesElements` proves you didn't break the happy path; `copyMustFailFastOnTypeViolation` proves the poison now dies at the copy site.

<details><summary>Hint (open only if stuck)</summary>
`Class<T>` has a method that casts *and* checks at runtime, throwing `ClassCastException` on a mismatch. Apply it to each element while building the new list. That turns the dishonest blanket cast into a verified, per-element one — the EJ27 way to make a generic copy reifiable.
</details>
