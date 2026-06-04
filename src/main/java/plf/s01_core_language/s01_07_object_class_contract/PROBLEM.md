# The Object Class Contract — Ghost in the Map

**Concept:** a HashMap freezes a key's hash-based identity at insertion; mutating a live key after `put` orphans its own entry — even when `equals`/`hashCode` are perfectly correct.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Everyone knows the blog-post rule: "always override `hashCode` when you override
`equals`." `MutableKeyPoint` already does — its `equals`/`hashCode` are
internally consistent at every instant. So it is safe as a `HashMap` key, right?

Here is the trap. You `put(point, value)`, then mutate `point.x` on the very same
live object. Now ask the map about that exact key. Does `get` still find it? Does
`containsKey` agree with `containsValue`? If you re-insert the same object, what
is `size()`? Commit to all four answers **before** you run anything — the gap
between what you guess and what Java does is the whole lesson.

## Task

1. Read `MutableKeyPoint.java`. Confirm for yourself that `equals` and `hashCode`
   obey the Object contract *at any single instant* — equal points are equal and
   share a hashCode. There is **no** missing or broken method here.
2. Open `src/test/java/.../s01_07_object_class_contract/GhostInTheMapTest.java`.
   It walks through: `put(key, "origin-ish")` → mutate `key.x = 2` → then probe
   `get`, `containsKey`, `containsValue`, and finally re-insert and check `size`.
3. **Commit first.** On paper, write your prediction for each of the four
   `PREDICT_*` constants. The constants currently hold the *naive* guess (what
   most people say). Do not run yet.
4. Run the test. Each failure message reveals the real Java 21 value. Edit
   **only** the four `PREDICT_*` constants — one per line — until the suite is
   green. Do not touch `MutableKeyPoint`, the `put`/mutate calls, or the
   assertions.
5. Diagnose. In one sentence each, explain (a) why `get` and `containsValue`
   give contradictory answers about the *same* key at the *same* instant, and
   (b) why re-inserting the same object makes `size()` grow.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Edit only the four `PREDICT_*` constants in the test. Everything else is fixed.
- The "obvious fix" — adding or rewriting `hashCode` — is **forbidden**, because
  the hashCode is already correct. Adding one does not save you; identifying the
  real defect is the exercise.

## Scale up (Optional)

1. **Pin the bucket math by hand.** `Objects.hash(1, 2) = 994` and
   `Objects.hash(2, 2) = 1025`. HashMap does not use `hashCode() & 15` directly —
   it *spreads* first: `h ^ (h >>> 16)`, then masks with `capacity - 1`. Compute
   the bucket index for both keys on a default 16-bucket table and show that
   `1→2` in `x` moves the key to a different bucket. Then explain why the
   textbook example `1→17` would *not* move it (and would make this exercise pass
   by luck and fail in review).
2. **Make the bug disappear.** Redesign `MutableKeyPoint` so the same sequence
   cannot corrupt the map, without changing `equals`/`hashCode` semantics. (Hint:
   the fix is structural, not a method tweak.)
3. **`instanceof` vs `getClass()`.** `MutableKeyPoint` uses `instanceof` in
   `equals` and is `final`. Argue whether that is correct, and describe the
   symmetry bug you would introduce by using `instanceof` in a *non-final* class
   that has a subclass.

## How to verify

```
mvn -q -Dtest=GhostInTheMapTest test
```

The test starts **red**. It is green only once all four `PREDICT_*` constants
match Java's real behavior. Then re-read your paper predictions and count how
many you got wrong.

<details><summary>Hint (open only if stuck)</summary>
A HashMap stores each entry in a bucket chosen from the key's hashCode at the
moment you call `put`. It never recomputes that bucket on its own. After you
mutate the key, `get` hashes the *new* state, walks to a *different* bucket, and
finds nothing — while `containsValue` ignores hashing entirely and scans every
value with `equals`. The two methods take different code paths, so they can
disagree about the same key at the same instant.
</details>
