# Special & Legacy Collections — Equal but Not Alike

**Concept:** Identity vs equality is the contract that *picks your map* — and equality of two maps says nothing about how they iterate or how they treat reference identity and null.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
`EnumMap`, `IdentityHashMap`, and `WeakHashMap` aren't "exotic HashMaps." Each one
swaps out a different assumption you didn't know you were making. `EnumMap` keys on
*ordinal*, so it skips hashing entirely and iterates in declaration order.
`IdentityHashMap` keys on `==` and `System.identityHashCode`, not `equals`. And the
common reflex — "two equal maps behave the same" — collapses the moment you ask about
iteration order, reference identity, or whether a `null` *value* means the key is absent.

You'll commit four predictions to a test, then run it. Each one targets a reflex that
bites mid-level engineers.

## Task
Open `SpecialMapsTest.java`. Four `@Test` methods each declare a `predicted` value
seeded with a deliberately wrong placeholder. For each, write your real prediction
**before running**, then make the production code in `SpecialMaps.java` correct so the
test passes:

1. **`equalsAcrossTypes`** — an `EnumMap<Day,String>` and a `HashMap<Day,String>` copy
   of it. Implement `equalsAcrossTypes` to return `a.equals(b)`. Predict the boolean.
2. **`iterationOrder`** — predict the exact `List<Day>` you get from iterating the
   `EnumMap`'s key set (it was populated `WED` then `MON`).
3. **`identityKeyCount`** — already implemented: two `new String("US")` keys in an
   `IdentityHashMap`. Predict `size()`.
4. **`nullValueIsPresent`** — an `EnumMap` with `put(Day.MON, null)`. Implement
   `nullValueIsPresent` to return whether `MON` is a *present key* (not whether `get`
   returns non-null). Predict the boolean.

Stub methods throw `UnsupportedOperationException("TODO")` — replace those three lines
(`equalsAcrossTypes`, `nullValueIsPresent`; `identityKeyCount` and `iterationOrder` are
done) and fill the four predictions.

## Constraints
- Java 21, no external libraries.
- Do **not** weaken the assertions or change the seeded `Map`/key construction.
- Commit each prediction in writing before you run the test.

## Scale up (Optional)
1. Replace the `IdentityHashMap` with `new String("US")` keys by an interned `"US"`
   literal used twice. What does `size()` become, and why does that one line flip it?
2. Swap the `EnumMap` for a `Hashtable<Day,String>` and call `put(Day.MON, null)`.
   What happens, and how does that contrast with the `EnumMap` null-value result?
3. Name the JDK class that makes `EnumMap` and `EnumSet` serializable without writing
   the internal array/bitvector, and say what it writes instead.

## How to verify
`mvn -q -Dtest=SpecialMapsTest test` — green when all four predictions match and the two
stubs are implemented.

<details><summary>Hint (open only if stuck)</summary>
`AbstractMap.equals` compares entry sets, not runtime classes — type is irrelevant.
Iteration order is a *separate* contract: `EnumMap` guarantees ordinal order regardless
of insertion. For null: in a `Map`, `containsKey(k) == true` and `get(k) == null` can
both hold — absence and a null value are different states.
</details>
