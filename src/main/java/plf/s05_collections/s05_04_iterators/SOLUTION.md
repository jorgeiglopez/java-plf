# Solution — The Smoke Detector That Stays Silent

## Ideal solution

```java
public static Outcome predictLength3() {
    // [1,2,3] removing evens silently completes as [1,3]
    return new Completed(List.of(1, 3));
}

public static Outcome predictLength4() {
    // [1,2,3,4] throws CME
    return new Threw();
}
```

Verified against JDK 21 `ArrayList`:

- `[1, 2, 3]` → `Completed([1, 3])` — **no CME**.
- `[1, 2, 3, 4]` → `Threw` — **CME**.

Same loop, lists differing only in length. The difference is pure cursor
arithmetic.

### The exact JDK 21 trace

`ArrayList.Itr` state: `cursor` (next index to return), `size` (live field on the
backing list), `expectedModCount`. Key methods:

- `hasNext()` → `return cursor != size;`
- `next()` → calls `checkForComodification()` **first** (the *only* throw site),
  then returns `elementData[cursor]` and does `cursor++`.
- `checkForComodification()` → `if (modCount != expectedModCount) throw new CME();`
- `list.remove(Object)` removes the element and bumps `modCount` — it does **not**
  touch the iterator's `expectedModCount`. The iterator is now stale.

**`[1, 2, 3]` (size starts 3):**

| step | cursor | size | call | effect |
|------|--------|------|------|--------|
| 1 | 0 | 3 | hasNext: 0≠3 → true; next() ok → returns 1 | cursor=1 |
| 2 | 1 | 3 | hasNext: 1≠3 → true; next() ok → returns 2 | cursor=2; `2%2==0` → remove(2) → size=2, modCount++ |
| 3 | 2 | 2 | hasNext: **2≠2 → false** | loop ends — no further `next()`, so `checkForComodification` is never reached |

Result: `[1, 3]`, silent. The smoke detector never sounds even though we mutated
mid-iteration. `cursor` happened to land exactly on the new `size`.

**`[1, 2, 3, 4]` (size starts 4):**

| step | cursor | size | call | effect |
|------|--------|------|------|--------|
| 1 | 0 | 4 | hasNext true; next() → 1 | cursor=1 |
| 2 | 1 | 4 | hasNext true; next() → 2 | cursor=2; remove(2) → size=3, modCount++ |
| 3 | 2 | 3 | hasNext: **2≠3 → true**; next() → **checkForComodification → modCount≠expectedModCount → CME** | boom |

Result: `ConcurrentModificationException`, thrown on the *next* `next()`, not at
`remove`. The list at throw time is the corrupted `[1, 3, 4]`.

**The single rule:** CME fires only if, after a structural modification, the loop
performs another `next()`. That happens iff `hasNext()` is still true, i.e.
`cursor != size`. Removing an element when the cursor sits at the (new)
second-to-last position pushes `cursor == size`, ending the loop before the check.
So **CME is a function of where the cursor lands, not of whether you mutated.**

## Scale up solutions

**1. Second-to-last vs last on `[1, 2, 3, 4, 5]`.**
Remove value `4` (second-to-last) by value in a for-each: at the point of removal
`cursor` advances to `4`, `size` drops to `4`, so `hasNext()` is `4 != 4` → false
→ **no CME**, result `[1, 2, 3, 5]`. Remove value `5` (last) instead: after the
mutation `cursor` is `4`, `size` is `4`... actually you only reach value 5 on the
final iteration where `cursor` becomes `5` after returning it; the removal sets
`size=4`, and `hasNext()` is `5 != 4` → false too — *but* the throw site differs:
removing the **last** element while it is the element just returned leaves
`cursor (5) > size (4)`, and the for-each makes one more `hasNext()` check which is
false, so it also ends. The reliably-silent case is the **second-to-last**; the
last-element case is the classic textbook example that *does* throw in many
hand-traces because people miscount — the safe statement to give an interviewer is:
"removing the second-to-last element is the canonical silent-CME case because it
forces `cursor == size`." (Run it: second-to-last is silent; the general lesson is
that silence is length/position dependent and must never be relied on.)

**2. `remove(int)` vs `remove(Integer)`.**
`List` has both `remove(int index)` and `remove(Object o)`. `list.remove(2)` is an
`int` literal → calls `remove(int index)` → removes the element **at index 2**
(the value `3`), giving `[1, 2, 4, 5]`. To remove the *value* 2 you must write
`list.remove(Integer.valueOf(2))` or `list.remove((Object) 2)`. The whole exercise
relies on `remove(x)` where `x` is an `Integer` (so it dispatches to
`remove(Object)` — by value); swap in an `int` and the semantics flip silently.
This is the autoboxing landmine: same method name, overload chosen at compile time
by static type.

**3. Do it right with `Iterator.remove()`.**

```java
for (Iterator<Integer> it = list.iterator(); it.hasNext();) {
    if (it.next() % 2 == 0) {
        it.remove();
    }
}
// [1,2,3,4,5] -> [1,3,5], always, no CME
```

`Iterator.remove()` removes the last element returned by `next()` **and** updates
the iterator's `expectedModCount = modCount`, plus fixes `cursor` (`cursor =
lastRet`). It restores the iterator↔list invariant that `list.remove(x)` breaks.

**4. `ListIterator` add.** On `[1, 2, 3]`: `next()`→1, `next()`→2, then `add(99)`
inserts *before* the cursor, giving `[1, 2, 99, 3]`. After `add`, `cursor` is past
the inserted element: `nextIndex() == 3`, `previousIndex() == 2`. `add` also bumps
`expectedModCount`, so it does **not** trigger CME — `ListIterator` mutation is the
*supported* way to mutate during traversal. (`Iterator` has no `add`; only
`ListIterator` does, and only `List`s provide it.)

**5. `Enumeration` on `Vector`.** `Vector.elements()` returns a legacy
`Enumeration` with only `hasMoreElements()`/`nextElement()`. It is **not**
fail-fast: it indexes by a counter with no `modCount` check. Remove an element
mid-traversal and it does not throw — it silently yields the *wrong* sequence
(e.g. visiting `1 2 4 5` after removing `3`, skipping or repeating depending on
position). Silent wrong answers are arguably worse than a CME: CME at least tells
you the code is broken at dev time; the `Enumeration` ships a quiet data-corruption
bug. Same lesson — *no guarantee* — on older iron. (`Enumeration` is otherwise
obsolete; prefer `Iterator`.)

## Why this works (and the gotcha)

The trap is believing CME is a *guarantee* triggered by mutation. It is neither.
`modCount` lives on `AbstractList`/`ArrayList`; the iterator snapshots it as
`expectedModCount` and re-checks only inside `next()` (and `forEachRemaining`).
Because the check is *only* reached when the loop calls `next()` again, any
mutation that leaves `cursor == size` ends the loop first and the check never runs.
So CME is **best-effort** (the Javadoc literally says "cannot be guaranteed" and
"should be used only to detect bugs"). The corrupted-but-silent `[1, 3]` is the
smoke detector staying mute during a real fire.

Contrast the mechanisms (do not conflate them):

- **Fail-fast** (`ArrayList`, `HashMap`, ...): `modCount` heuristic; throws CME on a
  *best-effort* basis. Single-threaded mutation can still slip through silently.
- **Weakly consistent** (`ConcurrentHashMap`, `ConcurrentLinkedQueue`): the iterator
  never throws CME; it traverses the structure as it existed at some point and may
  or may not reflect concurrent updates. *Live view*, no snapshot.
- **Snapshot** (`CopyOnWriteArrayList`, `CopyOnWriteArraySet`): the iterator works on
  an immutable copy taken at creation; writes go to a fresh array. Never throws,
  never reflects later writes. *Frozen view.*

"Doesn't throw CME" is not one thing — weakly-consistent and snapshot are
*different* designs, and saying "ConcurrentHashMap is like CopyOnWriteArrayList" is
a fail.

## Common mistakes

- "Mutating during for-each always throws CME." → False; it is best-effort. Removing
  the second-to-last element completes silently with a wrong list.
- Thinking CME is thrown *by* `remove`. → It is thrown later, by the next `next()`
  via `checkForComodification`. At throw time the list is already mutated.
- `list.remove(2)` to remove the value 2. → Calls `remove(int index)`; removes index
  2 instead. Box it: `remove(Integer.valueOf(2))`.
- "ConcurrentHashMap iterators are snapshots like CopyOnWriteArrayList." → No;
  weakly consistent (live, may reflect updates) vs snapshot (frozen copy).
- Using `Enumeration`/raw index loops to "avoid CME." → You only removed the alarm,
  not the fire; now bugs are silent.

## Interviewer Q&A

**Q:** Walk me through exactly when a for-each over an `ArrayList` throws
`ConcurrentModificationException`.
**A:** The for-each uses `ArrayList.Itr`. CME is thrown only inside `next()` (and
`forEachRemaining`) via `checkForComodification`, which compares `modCount` to the
iterator's `expectedModCount`. A structural mutation bumps `modCount` but not
`expectedModCount`. So CME fires only if the loop calls `next()` again after the
mutation — i.e. if `hasNext()` (`cursor != size`) is still true.

**Q:** Then is CME guaranteed when you mutate mid-iteration?
**A:** No. It is best-effort, per the Javadoc. If the mutation leaves
`cursor == size` (e.g. removing the second-to-last element), `hasNext()` returns
false, the loop ends, and the check never runs — the list is silently corrupted
with no exception.

**Q:** Same loop removing evens: `[1,2,3]` vs `[1,2,3,4]`. Outcomes?
**A:** `[1,2,3]` completes silently as `[1,3]` (after removing 2, `cursor==size==2`).
`[1,2,3,4]` throws CME (after removing 2, `cursor=2`, `size=3`, so `hasNext()` is
true and the next `next()` detects the stale `modCount`).

**Q:** What does `list.remove(2)` do on `[1,2,3,4,5]`?
**A:** Removes index 2 (value 3) → `[1,2,4,5]`. `2` is an `int`, so it binds to
`remove(int)`, not `remove(Object)`. Box it to remove by value:
`list.remove(Integer.valueOf(2))`.

**Q:** How do you remove during iteration correctly?
**A:** Use the iterator's own `remove()`: `it.remove()` after `it.next()`. It
updates `expectedModCount` and fixes the cursor, preserving the iterator↔list
invariant. Or collect-then-`removeAll`, or `removeIf`.

**Q:** Why doesn't `ConcurrentHashMap` throw CME, and is it the same reason
`CopyOnWriteArrayList` doesn't?
**A:** Different mechanisms. `ConcurrentHashMap` iterators are *weakly consistent*:
they traverse the live structure, never throw, and may or may not reflect concurrent
updates. `CopyOnWriteArrayList` iterators are *snapshots*: a frozen copy of the
array at iterator creation; writes go to a new array, so the iterator never sees
later changes. One is a live view, the other a frozen view.

**Q:** `Iterator` vs `ListIterator` vs `Enumeration`?
**A:** `Iterator` (all `Collection`s): forward-only, `hasNext/next/remove`.
`ListIterator` (only `List`): bidirectional, plus `add`, `set`, `nextIndex`,
`previousIndex` — the supported way to mutate during traversal. `Enumeration`:
legacy (`Vector`, `Hashtable`), `hasMoreElements/nextElement`, no removal, **not
fail-fast** — mutate mid-traversal and it silently returns wrong elements. Prefer
`Iterator`.

**Q:** Where does `modCount` live and why?
**A:** On `AbstractList` (and similarly in `HashMap`, `ArrayDeque`, etc.). It's a
counter of structural modifications. Iterators snapshot it at creation and re-check
it to detect interleaved structural changes — a cheap, best-effort concurrency/bug
sentinel, not a lock and not a guarantee.
