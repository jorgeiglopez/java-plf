# Solution — The Type That Lies

## Ideal solution

**Task 1 — cold open.** `List.of(...)` returns a structurally immutable list. Every
mutator, including the new `SequencedCollection` ones, throws at runtime:

```java
Class<? extends Throwable> predicted = UnsupportedOperationException.class;
```

It compiles because `addFirst` is declared on `SequencedCollection`, which `List`
implements; immutability is a *runtime* property, not a compile-time one.

**Task 2 — kill shot.** Final encounter order of `m` is `a, b, c, d`:

```java
assertEquals("c", reversed.firstEntry().getKey());
assertEquals(List.of("a", "b", "c", "d"), List.copyOf(m.sequencedKeySet()));
```

Trace: `reversed` is a *live* view, so after `m.put("c",3)` the reversed front is
`c` (hence `firstEntry()` is `c=3`, not `b=2`). Then `reversed.putFirst("d",4)`
means "put at the front of the reversed order" = **put at the back of the
original** → `d` lands at the tail. `m` ends `{a=1, b=2, c=3, d=4}`.

**Task 3 — the fix.** Replace the live wrapper with a real snapshot:

```java
public static SequencedCollection<String> audit(List<String> input) {
    return List.copyOf(input);
}
```

`List.copyOf` eagerly copies into an immutable list, so later mutation of the
caller's list can't leak in, and the result is still read-only (mutators throw).

## Why this works (and the gotcha)

One spine: **the reference type lies about the object's behavior.** A
`List<String>` / `SequencedCollection<String>` handle can wrap three very
different objects:

- **Truly immutable** (`List.of`, `List.copyOf`) — every mutator throws
  `UnsupportedOperationException`; also null-hostile (`List.of(null)` throws NPE).
- **Unmodifiable view** (`Collections.unmodifiableList(backing)`) — read-only
  *through this handle only*. The backing list is still alive; mutate it and the
  "unmodifiable" view changes. That's the Task 3 bug: `audit` promised a snapshot
  but returned a window onto the caller's mutable list.
- **Fully mutable** (`new ArrayList<>()`) — anything goes.

The Java 21 twist is that `reversed()` is also a *view*, and it doesn't only read
live — it **remaps your verbs**. Front becomes back. `putFirst`→`putLast`,
`addFirst`→`addLast`, `pollFirst`→`pollLast`. The senior who "knows reversed() is
a view" still predicts `{d, a, b, c}` and eats the floor, because they pictured
`putFirst` writing to the visual front of the original. It writes to the tail.

The cure is the same everywhere: when you need a value, not a window, take a
**defensive copy** — `List.copyOf` / `Map.copyOf` / `Set.copyOf`. A copy is a
snapshot; a view is a wiretap.

## Common mistakes

- **Predicting a compile error for `List.of(...).addFirst(...)`.** It compiles
  fine; `SequencedCollection` declares the method. The failure is runtime UOE.
- **Predicting `{d, a, b, c}` in Task 2.** `putFirst` on the *reversed* view is
  `putLast` on the original, so `d` is appended → `{a, b, c, d}`.
- **Expecting `reversed.firstEntry()` to be `b`.** The view is live; the post-`put`
  front of reversed order is the newly added tail-most key `c`.
- **Thinking `Collections.unmodifiableList` copies.** It wraps. Mutate the backing
  list and the view shifts. Only `copyOf` (or `new ArrayList<>(...)` then wrap)
  snapshots.
- **Using `Collections.unmodifiableList(new ArrayList<>(input))` for Task 3.** It
  works, but `List.copyOf(input)` is the one-call idiom and is null-hostile by
  design (fails fast if the snapshot should never contain null).

## Interviewer Q&A

**Q:** What's the difference between `List.of(x)` and
`Collections.unmodifiableList(list)`?
**A:** `List.of` is genuinely immutable and self-contained — no backing
collection, mutators throw, nulls rejected. `unmodifiableList` is a read-only
*view* over a backing list that remains independently mutable; changes to the
backing list are visible through the view.

**Q:** `List.of("a","b").addFirst("z")` — compile error or runtime error? Which?
**A:** Compiles (Java 21 `List` implements `SequencedCollection`, which declares
`addFirst`). At runtime it throws `UnsupportedOperationException`.

**Q:** How do `List.of` and `Arrays.asList` differ on nulls and on size?
**A:** `List.of(null)` throws NPE and the list is fixed-size *and* immutable
(even `contains(null)` throws NPE). `Arrays.asList(null)` is allowed; the list is
fixed-size but element-mutable (`set` works, `add` throws).

**Q:** What does `reversed()` return, and what's the cost?
**A:** A *view*, not a copy — O(1) to create, backed by the same data. Iteration
and reads are live; on a `SequencedMap`/`SequencedSet` writes pass through with
the ends swapped.

**Q:** On `LinkedHashMap`, `reversed().putFirst(k,v)` — where does the entry land
in the original?
**A:** At the original's tail. The reversed view's "first" is the original's
"last," so `putFirst` on the view is `putLast` on the original.

**Q:** You're writing a method that must return an immutable snapshot of a
caller-supplied list. What do you write, and what's the trap?
**A:** `return List.copyOf(input);`. The trap is returning
`Collections.unmodifiableList(input)` — that's a live view, so the caller can
keep mutating their list and corrupt your "snapshot." Copy at the boundary.

**Q:** Is `List.copyOf(x)` always a fresh copy?
**A:** Not necessarily — if `x` is already an immutable `List.of`/`copyOf`
instance, it may be returned as-is (it's safe to share because it can't change).
For any mutable source it copies. Either way the result is immutable.

**Q:** Which standard collections became sequenced in Java 21, and which gained an
*encounter order* that isn't insertion order?
**A:** `List`, `Deque`/`ArrayDeque`, `LinkedHashSet`, `LinkedHashMap` (insertion
order); `SortedSet`/`TreeSet`, `SortedMap`/`TreeMap` are sequenced by their
comparator, so their encounter order is sort order, not insertion order.
