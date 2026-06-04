# Solution — Equal but Not Alike

## Ideal solution

Two stubs to implement:

```java
static boolean equalsAcrossTypes(Map<Day, String> a, Map<Day, String> b) {
    return a.equals(b);
}

static boolean nullValueIsPresent() {
    Map<Day, String> m = new EnumMap<>(Day.class);
    m.put(Day.MON, null);
    return m.containsKey(Day.MON);   // true — get() would return null and mislead you
}
```

Correct predictions:

| Test | Prediction | Why |
|------|-----------|-----|
| `equalsAcrossTypes` | `true` | `AbstractMap.equals` compares entry sets; runtime class is irrelevant |
| `iterationOrder` | `List.of(Day.MON, Day.WED)` | `EnumMap` iterates in **ordinal** order, not insertion order (inserted WED then MON) |
| `identityKeyCount` | `2` | `IdentityHashMap` uses `==`; two `new String("US")` are distinct objects |
| `nullValueIsPresent` | `true` | `EnumMap` permits null values; `containsKey` is true even though `get` returns null |

```java
List<Day> predictedOrder = List.of(Day.MON, Day.WED);
boolean predictedEquals  = true;
int     predictedCount   = 2;
boolean predictedNull    = true;
```

### Scale up solutions

1. **Interned literal key.** Using the literal `"US"` twice as the key:
   ```java
   Map<String, Integer> m = new IdentityHashMap<>();
   m.put("US", 1);
   m.put("US", 2);   // same interned object -> overwrites
   // m.size() == 1
   ```
   String literals are interned into the pool, so both `put` calls hand
   `IdentityHashMap` the *same reference* — `==` matches, the second overwrites the
   first. With `new String("US")` you fabricate fresh objects, so `==` fails and you get
   two entries. This is the one-line flip that defines `IdentityHashMap`: it answers
   "same object?", never "equal value?".

2. **`Hashtable` and null.** `new Hashtable<Day,String>().put(Day.MON, null)` throws
   `NullPointerException` — `Hashtable` forbids **both** null keys and null values
   (it predates the Collections framework and its contract is stricter). `EnumMap`, by
   contrast, forbids null *keys* (NPE on `put(null, ...)`) but accepts null *values*,
   and a null value is a present entry: `containsKey` true, `get` null. So `EnumMap`
   and `Hashtable` disagree on exactly what "null is allowed" means.

3. **Serialization proxy.** Both `EnumMap` and `EnumSet` serialize through a private
   `SerializationProxy` (the serialization-proxy pattern). It does **not** write the
   internal `Object[]` (EnumMap) or `long`/`long[]` bitvector (EnumSet). It writes the
   element/key enum *type* plus the constants (and values), and reconstructs the
   instance on read. That keeps the on-disk form independent of the in-memory layout
   and of whether `EnumSet` is `RegularEnumSet` or `JumboEnumSet`.

## Why this works (and the gotcha)

One mechanism — **what counts as "the same"** — explains every result.

- **`equals` is value-based and type-blind.** `AbstractMap.equals` (inherited by both
  `EnumMap` and `HashMap`) checks `size` then walks `entrySet`, comparing each
  `(key, value)`. It never calls `getClass()`. So an `EnumMap` equals a `HashMap` with
  the same entries. The trap: engineers assume `equals` implies *interchangeable
  behavior*.
- **Iteration order is a separate contract.** Equality says nothing about ordering.
  `EnumMap` is documented to iterate in the natural (ordinal) order of its keys —
  insertion order is irrelevant, so WED-then-MON iterates as MON, WED. A `HashMap` with
  the same entries iterates in hash order. Equal maps, different iteration: the reflex
  "equal ⇒ same order" is wrong.
- **Identity vs equality picks the map.** `IdentityHashMap` deliberately replaces
  `equals`/`hashCode` with `==`/`System.identityHashCode`. Two `new String("US")` are
  `equals` but not `==`, so they're two keys. The same code in a `HashMap` would hold
  one. This is the spine of the whole sub-topic: the collection you choose *is* the
  definition of "same key" you're committing to.
- **null value ≠ absent.** `Map` distinguishes "key maps to null" from "key absent".
  `get` returning null is ambiguous; `containsKey` is the only honest test. `EnumMap`
  exploits this (null values allowed); `Hashtable` sidesteps it by banning null
  entirely.

`EnumMap`/`EnumSet` get their speed precisely because enum identity *is* the ordinal —
no hashing, no `equals`, just array indexing (or bit-twiddling for `EnumSet`).

## Common mistakes
- Predicting `false` for cross-type `equals` ("different classes") → `AbstractMap.equals`
  ignores class; it's entry-based.
- Predicting insertion order (WED, MON) for the `EnumMap` → it's ordinal order (MON, WED).
- Predicting `1` for the `IdentityHashMap` → `==` on two `new String` keys gives 2.
- Using `get(MON) != null` to test presence → returns false for a null value; you must
  use `containsKey`.
- Assuming `EnumMap` rejects null like `Map.of` → it rejects null *keys*, allows null
  *values*.

## Interviewer Q&A

**Q:** When would you reach for `IdentityHashMap` over `HashMap`?
**A:** When you genuinely want reference identity as the key contract — e.g. a graph
node visitation map, a serialization "already seen this exact object" table, or
proxy/identity bookkeeping where two `equals` objects must still be distinct keys. It's
a semantic choice, not a performance tweak.

**Q:** Why does `EnumMap` iterate in ordinal order, and can you change that?
**A:** It's backed by an array indexed by `ordinal()`, so iteration walks the array in
order — declaration order of the enum. You can't change it; there's no insertion-ordered
or comparator-ordered `EnumMap`. If you need a different order, copy into a
`LinkedHashMap` or `TreeMap` with a comparator.

**Q:** An `EnumMap` and a `HashMap` hold identical entries. Are they `equals`? Do they
iterate identically?
**A:** `equals` is true (entry-set comparison, type-blind via `AbstractMap`). Iteration
is not guaranteed identical — `EnumMap` is ordinal order, `HashMap` is hash order. Equal
does not imply same iteration order.

**Q:** What's the difference between `get(k) == null` and `!containsKey(k)`?
**A:** `get` returning null is ambiguous: the key may be absent *or* mapped to a null
value. `containsKey` disambiguates. In maps that permit null values (HashMap, EnumMap)
you must use `containsKey` to test presence.

**Q:** Why are `Vector` and `Hashtable` considered obsolete?
**A:** Every method is `synchronized`, so you pay lock cost on every access even
single-threaded, and the per-method locking doesn't make compound operations atomic
anyway. They predate the Collections framework. Use `ArrayList`/`HashMap` (unsynchronized)
or `ConcurrentHashMap`/`CopyOnWriteArrayList`/`Collections.synchronizedXxx` when you
actually need thread safety — those give you the right granularity.

**Q:** `Hashtable` vs `HashMap` on null — what's the contract?
**A:** `Hashtable` forbids both null keys and null values (NPE). `HashMap` allows one
null key and any number of null values. `EnumMap` forbids null keys but allows null
values. `ConcurrentHashMap` forbids both (so null can't be confused with "absent" under
concurrency).

**Q:** How does `EnumSet` store its elements, and why no `LinkedEnumSet`?
**A:** A bitvector keyed by ordinal — a single `long` (`RegularEnumSet`) for ≤64
constants, a `long[]` (`JumboEnumSet`) above that. Membership is bit operations, not
hashing. Iteration is ordinal order by construction, so there's no insertion-ordered
variant to offer.

**Q:** `EnumMap`/`EnumSet` are `Serializable` — does the bitvector/array go on the wire?
**A:** No. Both use a serialization proxy that writes the enum type and the
constants/values, then rebuilds the instance on read. The internal layout (and the
Regular-vs-Jumbo distinction for `EnumSet`) never leaks into the serialized form.

**Q:** Classic `WeakHashMap` leak — what is it?
**A:** Keys are weak, values are *strong*. If a value (directly or transitively)
references its own key, that strong reference keeps the key reachable, the weak key is
never cleared, and the entry never evicts — a leak. Fix by not letting values reference
keys, or wrap the value in a `WeakReference`.
