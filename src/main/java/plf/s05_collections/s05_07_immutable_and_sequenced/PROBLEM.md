# Immutable & Sequenced Collections — The Type That Lies

**Concept:** A `List<String>` (or `SequencedCollection<String>`) reference tells you
*nothing* about what the object will do. The same static type can be truly immutable,
an unmodifiable *view* over a still-live backing list, or fully mutable — and a
`reversed()` view doesn't just read live, it *remaps your write verbs*.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
Java 9 gave us `List.of(...)`; Java 21 retro-fitted `List`, `Deque`,
`LinkedHashMap`, and `TreeMap` with `SequencedCollection` / `SequencedMap`
(`getFirst`, `addFirst`, `putFirst`, `reversed()`, ...). All of it shares one
booby trap: **the reference type is a promise the runtime object may not keep.**
`addFirst` compiles against any `List` but blows up on `List.of(...)`. An
"unmodifiable" wrapper is read-only *through that handle* but still mirrors a
backing list someone else can mutate. And `reversed()` is a *view* whose
`putFirst` is the original's `putLast`. Can you predict each before you run it?

## Task
Three failing/blank spots in `CollectionLensTest`, escalating in cruelty. Commit
to a prediction **before** running anything.

1. **Cold open — `coldOpen_addFirstOnImmutableList`.** `List.of("a","b")` is a
   `SequencedCollection`, so `.addFirst(...)` compiles. Predict what it throws at
   runtime and put that class into the `predicted` variable.

2. **Kill shot — `killShot_reversedRemapsYourVerbs`.** Trace this by hand:
   ```java
   var m = new LinkedHashMap<String,Integer>();
   m.put("a",1); m.put("b",2);
   var reversed = m.reversed();   // view, taken BEFORE the next two writes
   m.put("c",3);                  // mutate the ORIGINAL
   reversed.putFirst("d",4);      // write THROUGH the reversed view
   ```
   Replace `"REPLACE_ME"` with the exact final encounter order of `m`'s keys.
   (The `reversed.firstEntry()` line is already filled in — predict whether you
   agree with it first.)

3. **The fix — `auditReturnsAStableSnapshot`.** This test is **red**. Read
   `CollectionLens.audit(...)`: it wraps the caller's list with
   `Collections.unmodifiableList(...)` and hands it back as a "snapshot." The
   caller then mutates their original list. The snapshot changes too. Fix
   `audit` so the returned value is a genuine, defensively-copied snapshot that
   stays read-only — without changing the test.

## Constraints
- Java 21, no external libraries.
- Do **not** edit the test file except the two prediction placeholders in Tasks
  1 and 2. Task 3 is fixed in `CollectionLens.java` only.
- The fix in Task 3 must keep the return read-only (mutating it still throws).

## How to verify
Write your predictions first, then:
`mvn -q -Dtest=CollectionLensTest test`
All three tests green = done.

<details><summary>Hint (open only if stuck)</summary>
`reversed()` flips the *ends*: front-of-reversed is back-of-original, so
`putFirst` on the reversed view appends at the original's tail. For Task 3,
`Collections.unmodifiableList` is a live view, not a copy — what factory makes an
unmodifiable *snapshot* in one call? It also rejects nulls.
</details>
