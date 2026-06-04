# Solution — The Inverted PECS Trap

## Prediction key

| Call                  | Compiles? | `T` inferred | Why |
|-----------------------|-----------|--------------|-----|
| (A) `transfer(ints, nums)` | **yes** | `Integer` | `List<Integer>` ⊆ `List<? extends Integer>`; `List<Number>` ⊆ `List<? super Integer>`. |
| (B) `transfer(nums, ints)` | **NO**  | — | No single `T` satisfies both bounds (see below). |
| (C) `transfer(nums, nums)` | **yes** | `Number` | One type, `Number`, satisfies *both* wildcards at once — same list as producer and consumer. |

(C) is the floor-drop: people who internalize "`super` = consumer only, `extends`
= producer only" never expect a single list to legally fill *both* roles.

The exact `javac` error for (B) is the teaching gold:

```
error: method transfer cannot be applied to given types;
  required: List<? extends T>,List<? super T>
  found:    List<Number>,List<Integer>
  reason: inference variable T has incompatible bounds
    upper bounds: Integer,Number
    lower bounds: Number
```

`src = List<Number>` pushes a **lower** bound `Number ≤ T`; `dst = List<Integer>`
pushes an **upper** bound `T ≤ Integer`. The compiler needs `Number ≤ T ≤ Integer`,
which is empty (Integer is a *subtype* of Number, not a supertype). Inference
fails — it never even gets to "is this a valid supertype list?".

## Ideal solution

```java
static void copy(List<? extends Number> src, List<? super Number> dst) {
    for (Number n : src) {
        dst.add(n);
    }
}
```

`src` is a pure producer (`? extends Number` — you can read `Number` out, never
write in), `dst` is a pure consumer (`? super Number` — you can write `Number`
in, but read out only as `Object`). No type parameter is needed because nothing
flows *between* two distinct unknown types here: everything is funneled through
the concrete pivot type `Number`.

### Why a type parameter would be wrong here

`copy` doesn't relate `src`'s element type to `dst`'s element type — both are
fixed against `Number`. The moment you *do* need them related (e.g. "move a `T`
from one list into another and the lists must agree on `T`"), you need the
`<T> transfer(List<? extends T>, List<? super T>)` form, and you inherit exactly
the inference behavior dissected above.

## Why this works (and the gotcha)

The trap is treating wildcards as a *compatibility* question ("is `List<Integer>`
assignable here?") when they are really a *data-flow* question that the compiler
turns into **bounds on an inference variable**. At a call site the compiler does
not check each parameter independently; it collects every bound `T` must satisfy
and looks for one type in the intersection:

- `? extends X` against an argument of element type `A` ⇒ `A ≤ T` *and* `T ≤ X`.
- `? super X` against an argument of element type `B` ⇒ `T ≤ B` *and* `X ≤ T`.

Call (B) makes the intersection empty. Call (C) makes it a single point,
`Number`, so the same list legally produces *and* consumes. The naive "super
accepts supertypes" reasoning skips the inference step entirely and gets (B) and
(C) backwards.

## Common mistakes

- **"`transfer(nums, ints)` compiles — super takes supertypes."** It does not.
  Inference needs `Number ≤ T ≤ Integer`; that set is empty. The error is an
  *inference* failure, not an assignment failure.
- **"`transfer(nums, nums)` can't compile — a consumer can't also be a producer."**
  It compiles fine with `T = Number`. The roles are about what the *compiler lets
  you do through each reference*, not a ban on one object playing both.
- **Adding `<T>` to `copy`.** Unnecessary and signals you think the two lists'
  element types must be related; they're both pinned to `Number`.
- **Trying `dst.add(src.get(0))` style with a raw read into `dst`** in a generic
  `transfer` where the lists are unrelated — that's the simultaneous
  producer+consumer case where a wildcard can't unify two captures and you're
  forced to an invariant `<T>` parameter.

## Interviewer Q&A

**Q:** State PECS and what each half buys you.
**A:** Producer-Extends, Consumer-Super. `? extends T` lets you *read* `T` out of
a structure (it's a producer) but forbids writing (the compiler can't know the
exact subtype). `? super T` lets you *write* `T` into a structure (it's a
consumer) but reads come back only as `Object`. You pick the bound by which
direction data flows.

**Q:** In `transfer(nums, ints)` with `nums : List<Number>`, `ints : List<Integer>`,
does it compile? Why?
**A:** No. The compiler must find one `T` with `Number ≤ T` (from the
`? extends T` producer) and `T ≤ Integer` (from the `? super T` consumer). That
intersection is empty because Integer is a subtype, not a supertype, of Number.
It fails as an *inference* error ("incompatible bounds"), before any
assignability check.

**Q:** Does `transfer(nums, nums)` compile, with the same `List<Number>` passed
twice?
**A:** Yes, `T` infers to `Number`: `List<Number>` satisfies both
`List<? extends Number>` and `List<? super Number>`. One list can legally be both
producer and consumer when a single type fits both wildcards.

**Q:** Why doesn't `copy` need a type parameter, while `transfer` does?
**A:** `copy` pins both lists to the concrete type `Number`; nothing relates the
two unknown element types, so there's nothing for a type variable to unify.
`transfer` declares `<T>` to *link* the producer's element type to the
consumer's, which is what creates the call-site bounds in the first place.

**Q:** When can't you use a wildcard at all — when are you forced to a named type
parameter?
**A:** When the same call must both read a value from a structure *and* write
that same value back into it (or into a second structure that must agree on the
type). Two independent wildcards capture to two distinct fresh types, so the
compiler can't prove the value read is assignable where you write it. Naming `<T>`
unifies them. (E.g. `Collections.swap` uses a helper with a named type variable
to "capture" the wildcard.)

**Q:** Bounded type parameter `<T extends Number>` — what does it erase to, and
why does that surprise people?
**A:** It erases to `Number`, not `Object`. Erasure replaces a type variable with
the erasure of its *leftmost bound*, so the bytecode descriptor of `transfer`
takes `List` args and the bridge/casts target `Number`. The common misconception
is "erasure always yields `Object`" — true only for *unbounded* parameters.

**Q:** `<T extends Comparable<T>>` — what's that pattern called and why use it?
**A:** A recursive (self-referential) type bound. It says "`T` must be comparable
to its own type," which is how `max(List<? extends T>)`-style methods guarantee
elements are mutually comparable. It erases to `Comparable`.

**Q:** Why does `? extends Number` forbid `list.add(someInteger)`?
**A:** `List<? extends Number>` might actually be a `List<Double>`. The compiler
can't prove an `Integer` is a valid element for the captured-but-unknown subtype,
so it rejects every `add` except `add(null)`. That read-only-ness is exactly what
makes it a safe *producer*.
