# Design Patterns — The Defensive Copy That Locked the Wrong Thing

**Concept:** the immutable-value-object pattern needs a *deep* defensive copy; `List.copyOf` (and `Collections.unmodifiableList`) only lock the spine, so an "immutable" object whose elements are mutable is a lie.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

The textbook fix for a leaky constructor is the defensive copy, and every
"Effective Java" summary tells you to reach for `List.copyOf`. So here is
`Receipt`: a `final` class with a `private final List<Item> items` whose
constructor does the canonical `this.items = List.copyOf(items)`. Looks
bulletproof. You can even *prove* it: call `receipt.items().add(...)` and watch it
throw `UnsupportedOperationException`. The list is locked. The object is
immutable. Right?

Now the question. Someone still holds the *original* `List<Item>` you passed in.
They call `original.get(0).setPriceCents(100_000)` on an element. What does
`receipt.totalCents()` do? Commit to **one** named outcome before you run
anything — the proof that the copy was "safe" is exactly what sells the lie.

## Task

1. Read `Receipt.java`. Note three things: the class is `final`, the field is
   `private final`, and the constructor defensively copies with `List.copyOf`.
   This is the idiomatic immutable-value-object pattern as most people write it.
2. Open `src/test/java/.../s02_10_design_patterns/ReceiptTest.java`. The first
   test (`spineIsLocked`) is the **bait**: it proves `items().add(...)` throws and
   that mutating the *source list's structure* (`source.add(...)`) does **not**
   change the receipt. That genuinely passes — the spine really is copied.
3. The second test mutates an **element** through the original reference:
   `source.get(0).setPriceCents(100_000)`. **Commit first.** Set `PREDICTION` to
   exactly one of:
   - `Outcome.THROWS` — the list is unmodifiable, so the mutation is rejected.
   - `Outcome.IGNORED` — the record copied the data, so the receipt is unaffected.
   - `Outcome.TOTAL_CHANGES` — the copy was shallow; the receipt's total moves.

   Then set `PREDICT_TOTAL_AFTER_MUTATION` to the integer total you expect.
4. Run the test. Where it fails, the assertion reveals the real Java 21 value.
   Edit **only** the two `PREDICT_*`/`PREDICTION` lines until the suite is green.
   Do **not** touch `Receipt`, the mutation calls, or the assertions.
5. Diagnose in one sentence each: (a) why `items().add(...)` throws but the
   element mutation still lands, and (b) what "shallow immutability" means for the
   object graph below the list.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Edit only `PREDICTION` and `PREDICT_TOTAL_AFTER_MUTATION` in the test.
- The reflexive "fix" — swapping `List.copyOf` for `Collections.unmodifiableList`
  — is **forbidden as a fix**, because it locks the same wrong thing (the spine,
  not the elements). Identifying *what* must be deep-copied is the exercise.

## Scale up (Optional)

1. **Make it genuinely immutable.** Redesign so the element mutation cannot leak,
   without changing `Receipt`'s public shape (`Item` keeps its setter; callers
   keep passing a `List<Item>`). Decide what must be deep-copied, where, and
   whether `Item` itself should change. State the cost.
2. **`List.copyOf` vs `Collections.unmodifiableList`.** One is a copy, one is a
   live view. Predict, then verify: after building each over a mutable backing
   list, does adding to the *backing* list show through? Does mutating an
   *element* show through? Which of those two differences this exercise actually
   depends on.
3. **`record Receipt(List<Item> items)`.** Rewrite `Receipt` as a record with a
   compact constructor doing `items = List.copyOf(items)`. Does the trap change at
   all? Argue why records give you *shallow* immutability by design and never
   deep-copy components.
4. **Reasoning-only — the Singleton you cannot unit-test offline.** A lazy
   singleton uses double-checked locking on a **non-`volatile`** `instance` field.
   Explain why this is broken under the Java Memory Model (partially-constructed
   object publication / reordering), why it nonetheless "passes" almost every time
   you run it, and give the two idiomatic fixes (the `volatile` field and the
   initialization-on-demand holder class). Do not write a flaky test — argue it.

## How to verify

```
mvn -q -Dtest=ReceiptTest test
```

The suite starts **red**. It is green only once `PREDICTION` and
`PREDICT_TOTAL_AFTER_MUTATION` match Java's real behavior. Then count: did you
name the lie before it was revealed, or did the locked spine fool you?

<details><summary>Hint (open only if stuck)</summary>
`List.copyOf` copies the *references* in the list, not the objects they point to.
The new list cannot grow or shrink (spine locked), and it does not see structural
changes to the source list. But every `Item` in the new list is the **same
object** as in the source list. Mutating that shared object via any reference —
the source's or the receipt's — is visible through both.
</details>
