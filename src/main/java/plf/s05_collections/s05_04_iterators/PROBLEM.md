# Iterators — The Smoke Detector That Stays Silent

**Concept:** A fail-fast iterator is a *best-effort* bug-catcher, not a safety
guarantee. `ConcurrentModificationException` (CME) is a function of **cursor
arithmetic**, not of "did you mutate the list."
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Everyone "knows" that mutating a collection while a for-each loop iterates throws
`ConcurrentModificationException`. So the smug answer is: *of course it throws.*

But `modCount`/fail-fast is a smoke detector, not a seatbelt — the Javadoc itself
says CME is "best-effort" and **must not be relied upon**. There are real lists
where you mutate mid-iteration and the loop finishes **silently with a corrupted
result**. That is the senior insight: the smoke detector can stay silent during a
real fire.

You will prove it on **two lists that differ only in length**, running the
*identical* code, and explain why one throws and one does not — down to the
`cursor`, `size`, `hasNext()`, and `checkForComodification()` calls in JDK 21's
`ArrayList.Itr`.

## Task

The harness `SmokeDetector.java` runs this loop over a copy of the input:

```java
for (Integer x : list) {
    if (x % 2 == 0) {
        list.remove(x);   // remove(Object) — by value, not by index
    }
}
```

It returns an `Outcome`: either `new Completed(resultList)` or `new Threw()`.

1. **Before running anything**, hand-trace the loop for `[1, 2, 3]` and implement
   `predictLength3()` to return the exact `Outcome` you expect (a `Completed` with
   the exact final list, or a `Threw`).
2. Do the same for `[1, 2, 3, 4]` in `predictLength4()`.
3. Run the test. If a prediction is wrong, do **not** change your trace blindly —
   re-trace with `cursor`/`size` on paper until the JDK behavior and your mental
   model agree.
4. In `SOLUTION.md` terms, be ready to state the single rule that decides whether
   CME fires.

## Constraints

- Java 21, no external libraries.
- Edit only the two `predictLengthN()` methods in `SmokeDetector.java`. Do **not**
  modify `removeEvensWithForEach`, the records, or the test.
- `Completed.equals` is record-derived, so a prediction matches only if the list
  contents match exactly.

## Scale up (Optional)

Answer these in `SOLUTION.md` form (no code required unless noted):

1. **Second-to-last vs last.** Start `[1, 2, 3, 4, 5]`. In a for-each, remove the
   value `4` (second-to-last) by value — does it throw? Now instead remove the
   value `5` (last). Predict each, then explain the difference via `cursor == size`.
2. **`remove(int)` vs `remove(Integer)`.** What does `list.remove(2)` do to
   `[1, 2, 3, 4, 5]` and why is that a landmine hiding inside this whole exercise?
3. **Do it right.** Rewrite the even-removal so it always works, using
   `Iterator.remove()`. What invariant does `Iterator.remove()` restore that
   `list.remove(x)` violates?
4. **`ListIterator`.** On `[1, 2, 3]`, call `next()` twice then `add(99)`. What is
   the list afterward, and what do `nextIndex()` / `previousIndex()` return?
5. **Older iron.** `Vector.elements()` returns an `Enumeration`. Iterate it while
   removing an element mid-traversal. Does it throw? What does the traversal
   "see"? Why is *silent wrong answers* arguably worse than a CME?

## How to verify

```
mvn -q -Dtest=SmokeDetectorTest test
```

Both tests go green only when both predicted `Outcome`s match JDK 21 reality.

<details><summary>Hint (open only if stuck)</summary>
`ArrayList`'s for-each iterator is `Itr`. `hasNext()` is `cursor != size`.
`next()` calls `checkForComodification()` **first** (that is the only place CME is
thrown), then returns `elementData[cursor++]`. CME fires only if the loop reaches
another `next()` after the mutation. So ask: after the removal, does `hasNext()`
return `true` (forcing another `next()` → boom) or `false` (loop ends → silence)?
</details>
