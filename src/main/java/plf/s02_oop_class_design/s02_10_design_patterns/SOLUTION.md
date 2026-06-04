# Solution — The Defensive Copy That Locked the Wrong Thing

## Ideal solution

The two predictions, corrected to Java 21 (Amazon Corretto 21.0.11) behavior:

```java
static final Outcome PREDICTION = Outcome.TOTAL_CHANGES;     // not THROWS, not IGNORED
static final int PREDICT_TOTAL_AFTER_MUTATION = 100_250;     // 100_000 + 250
```

With those values the suite is green. Trace of the run:

```
new Receipt(source)              items = List.copyOf(source)   // spine copied
receipt.totalCents()             -> 350   (100 + 250)
receipt.items().add(item999)     -> throws UnsupportedOperationException   // spine LOCKED  <-- the bait
source.add(item500)              receipt.totalCents() still 350            // structural change does NOT leak
source.get(0).setPriceCents(100_000)
receipt.totalCents()             -> 100_250   // element mutation LEAKS through the shared reference
```

The named outcome is `TOTAL_CHANGES`. The unmodifiable spine — the very thing you
used to *prove* the receipt was safe — is what sold the lie. `List.copyOf` locked
the list structure and left the `Item` objects shared.

### Scale up solutions

**1. Make it genuinely immutable.**
The leak is below the list. To stop it you must copy the *elements*, not just the
spine. Two honest options:

```java
// Option A: deep-copy each element on the way in (Item gains a copy constructor).
public Receipt(List<Item> items) {
    List<Item> deep = new ArrayList<>(items.size());
    for (Item item : items) {
        deep.add(new Item(item.priceCents()));   // fresh Item, no shared reference
    }
    this.items = List.copyOf(deep);              // spine + elements now both owned
}
```

```java
// Option B (preferred): make Item itself an immutable value type, then the
// shallow List.copyOf is automatically deep-safe because there is nothing to mutate.
public record Item(int priceCents) { }           // no setter -> sharing is harmless
// Receipt's constructor stays this.items = List.copyOf(items);
```

Cost: Option A copies on construction (O(n) allocation) and you must also defend
the *accessor* if `Item` stays mutable — `items()` hands out the same `Item`
objects, so callers could mutate them; you would deep-copy on the way out too, or
return an unmodifiable view of immutable elements. Option B pushes immutability
down to the leaf and removes the whole class of bug — **the real senior move is to
make the element immutable, not to copy harder.**

**2. `List.copyOf` vs `Collections.unmodifiableList`.**
Verified on Corretto 21.0.11:

| Build over a mutable backing list | `source.add(x)` shows through? | `source.get(0).mutate()` shows through? |
|---|---|---|
| `List.copyOf(source)` | **No** — it is a true copy of the spine | **Yes** — shares element objects |
| `Collections.unmodifiableList(source)` | **Yes** — it is a live *view* of `source` | **Yes** — shares element objects |

This exercise depends on the **second** column, which is identical for both: both
are *shallow*. The first column (copy vs view) is a different, also-important trap
— `unmodifiableList` would additionally leak `source.add(...)`, which `List.copyOf`
does not. So `List.copyOf` is the stronger of the two, and it *still* is not deep.
(Aside: `List.copyOf` of an already-immutable `List.of(...)` returns the **same
instance** — it elides the copy when it is provably safe.)

**3. `record Receipt(List<Item> items)`.**
```java
public record Receipt(List<Item> items) {
    public Receipt {
        items = List.copyOf(items);   // compact constructor, same defensive copy
    }
    public int totalCents() {
        return items.stream().mapToInt(Item::priceCents).sum();
    }
}
```
The trap is **unchanged**. A record's components are `final` references, and the
auto-generated accessor returns the component as-is. Records never deep-copy: the
compact constructor is the *only* place to harden inputs, and even there
`List.copyOf` gives you the same shallow guarantee. "Records are immutable" means
**shallowly** immutable — the reference cannot be reassigned; the object it points
to can still mutate if its type allows it. The fix is the same as Option B: make
the component type itself immutable.

**4. Reasoning-only — the non-`volatile` double-checked-locking singleton.**
```java
class Service {
    private static Service instance;                 // NOT volatile  <-- the bug
    static Service getInstance() {
        if (instance == null) {                      // 1st check (no lock)
            synchronized (Service.class) {
                if (instance == null) {              // 2nd check (locked)
                    instance = new Service();        // construct + publish
                }
            }
        }
        return instance;
    }
}
```
Why it is broken: `instance = new Service()` is not atomic. The JIT/CPU may
reorder it as *(a)* allocate, *(b)* publish the reference into `instance`,
*(c)* run the constructor. A second thread taking the lock-free fast path can see
a non-null `instance` whose fields are still defaults — a **partially constructed
object**. Without `volatile` there is no happens-before edge between the writing
thread's constructor and the reading thread's first read, so the reader may also
see stale field values. Why it "passes" anyway: the reordering window is tiny and
hardware/JIT-dependent, so it almost always works in a single-run unit test —
which is exactly why you must *reason* about it, not test it. The two idiomatic
fixes:
- **`private static volatile Service instance;`** — `volatile` forbids the harmful
  reordering and establishes happens-before, so a non-null read sees a fully
  constructed object. DCL is correct *only* with `volatile`.
- **Initialization-on-demand holder** — `private static class Holder { static final
  Service INSTANCE = new Service(); }` and `return Holder.INSTANCE;`. The JVM's
  class-initialization lock gives you lazy, thread-safe, lock-free publication with
  no `volatile` and no DCL at all. This is the preferred Java idiom; an `enum`
  singleton is the other (serialization- and reflection-safe).

## Why this works (and the gotcha)

`List.copyOf(items)` does exactly two things: it allocates a new immutable list and
copies the **element references** into it. It does **not** clone the elements. So:

- The **spine** is owned and frozen: you cannot `add`/`remove`/`set`, and the new
  list does not track structural changes to the source. That is real, and it is
  the bait — `items().add(...)` throwing `UnsupportedOperationException` *feels*
  like proof of immutability.
- The **elements** are shared: `receipt.items().get(0)` and `source.get(0)` are
  the *same* `Item` object. Mutating it through either reference is visible through
  both. The "immutable" receipt's `totalCents()` changes from 350 to 100_250.

The gotcha that survives the interview: a defensive copy is only as deep as the
mutability below it. `List.copyOf` and `Collections.unmodifiableList` give you
**shallow** immutability — they lock the container, not the contents. An object is
truly immutable only when *every* reachable component is immutable (or privately
deep-copied on the way in **and** out). The senior fix is to make `Item` an
immutable value type, which makes the shallow copy sufficient by construction.

## Common mistakes

- **"`items().add()` throws, so the receipt is immutable."** That only proves the
  *spine* is locked. The elements are still shared and mutable — different layer.
- **Predicting `THROWS` for the element mutation.** The mutation happens on the
  `Item`, not on the list; the unmodifiable list never sees it. `UnsupportedOperationException`
  is only thrown by *structural* list operations.
- **Predicting `IGNORED`.** That would require a deep copy. `List.copyOf` copies
  references, not objects, so there is nothing to insulate the receipt from a
  shared element's mutation.
- **"Switch to `Collections.unmodifiableList` to fix it."** Same shallow lock,
  and *worse* — it is a live view, so it would also leak `source.add(...)`.
- **Assuming a `record` deep-copies its components.** It never does; the compact
  constructor is your only hook, and even there you must harden the input yourself.
- **Reaching for `clone()` on the list.** `ArrayList.clone()` is also shallow; it
  shares elements exactly like `copyOf`.

## Interviewer Q&A

**Q:** You did the canonical defensive copy with `List.copyOf`, and `items().add()`
throws. Why is the object still mutable?
**A:** `List.copyOf` is *shallow*: it copies element references into a new
unmodifiable list. The list structure is locked, but every `Item` is the same
object the caller still holds. Mutating an element through that retained reference
changes the receipt's state. Locking the spine doesn't lock the contents.

**Q:** What is the precise difference between `List.copyOf` and
`Collections.unmodifiableList` here?
**A:** `List.copyOf` makes an independent copy of the spine, so later
`source.add(...)` does not show through. `unmodifiableList` is a live *view* of the
backing list, so `source.add(...)` does show through. Both share the elements, so
neither protects against element mutation. For defensive copies you want
`List.copyOf` (or `new ArrayList<>(...)`), never a view of a list the caller keeps.

**Q:** What does "shallow immutability" mean, and how do you get deep immutability?
**A:** Shallow immutability means the references in the object can't be reassigned
and the container can't be restructured, but the objects those references point to
can still change. Deep immutability requires every reachable component to itself be
immutable — or to be privately deep-copied on the way in and on the way out. The
clean fix is to make the component type (`Item`) immutable so sharing is harmless.

**Q:** Rewrite `Receipt` as a `record`. Does the trap go away?
**A:** No. Records give shallow immutability by design — components are `final`
references and accessors return them directly; records never deep-copy. The compact
constructor is the only place to harden inputs, and `List.copyOf` there is still
shallow. The trap is identical until you make `Item` immutable.

**Q:** Where would you put the defensive copy, and is one copy enough?
**A:** On the way **in** (constructor) and, if the element type stays mutable, on
the way **out** (accessor) too — otherwise the accessor hands back live elements
the caller can mutate. One copy of the spine is never enough when elements are
mutable; you need to copy the elements, or make them immutable so no copy of them
is needed.

**Q:** This is the immutable-value-object pattern. What is the anti-pattern it's
demonstrating?
**A:** A "leaky" or counterfeit immutable: an object that advertises immutability
(final class, final fields, unmodifiable collection) but exposes mutable internal
state through shared references — either via a shallow copy on construction or a
direct getter. The interviewer's term of art is "shallow immutability" or "escaping
mutable state"; the fix is full defensive copying or immutable components.

**Q:** Switch gears to the Singleton pattern. Why is double-checked locking broken
without `volatile`, and what are the idiomatic fixes?
**A:** `instance = new Service()` can be reordered so the reference is published
before the constructor finishes; without `volatile` there's no happens-before edge,
so a lock-free reader can see a non-null but partially constructed object. Fixes:
mark the field `volatile` (DCL is correct only then), or use the
initialization-on-demand holder class (JVM class-init lock gives lazy, lock-free,
thread-safe publication), or an `enum` singleton (also serialization/reflection
safe). The holder idiom is the preferred Java answer.

**Q:** Why can't you reliably unit-test that broken singleton offline, and what does
that tell you about testing concurrency bugs?
**A:** The reordering/visibility window is tiny and depends on JIT and hardware, so
the broken version passes almost every run — a green test doesn't prove correctness
under the JMM. Concurrency correctness is established by *reasoning* about
happens-before edges, not by a passing single-run test; tests can only find races,
never prove their absence.
