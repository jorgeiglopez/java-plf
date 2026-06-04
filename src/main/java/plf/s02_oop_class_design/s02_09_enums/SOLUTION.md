# Solution — The knife in the room: extensible enums vs the JDK's enum containers

## Part 1 — the two compiler errors (exact `javac`, Java 21)

Both lines **fail to compile**, and the root cause of *both* is the same:
`EnumSet` and `EnumMap` are bounded `<E extends Enum<E>>`, and `Op` (an
interface) cannot satisfy that bound. But the two errors *surface* differently —
get the label right.

### Line A — `EnumSet.of(Basic.ADD, Sci.LOG)`

This is a **type-inference failure**, not a runtime "mono-type" check. The
matching overload is `static <E extends Enum<E>> EnumSet<E> of(E e1, E e2)`,
which requires **both** arguments to be the *same* `E`. `Basic` and `Sci` are
different types, so inference has contradictory equality constraints. `javac`
reports:

```
error: no suitable method found for of(Basic,Sci)
        var s = EnumSet.of(Basic.ADD, Sci.LOG);
                       ^
    method EnumSet.<E#2>of(E#2,E#2) is not applicable
      (inference variable E#2 has incompatible bounds
        equality constraints: Basic,Sci
        lower bounds: Sci,Basic)
    ... (the other arity overloads are reported as length mismatches) ...
  where E#2 ... :
    E#2 extends Enum<E#2> declared in method <E#2>of(E#2,E#2)
```

The load-bearing line is `equality constraints: Basic,Sci` — inference cannot
unify the two arguments to one `E`. It is a *compile-time* inference failure, not
a runtime exception. (Note: `EnumSet.of` is mono-type *by design of the
signature*, not by a runtime guard — there is no chance for code to run.)

### Line B — `new EnumMap<Op, String>(Op.class)`

This is a **bound violation on the type argument itself**, caught at the
declaration of the type. `EnumMap` is `class EnumMap<K extends Enum<K>, V>`;
`Op` is an interface, not an `Enum`. `javac` reports:

```
error: type argument Op is not within bounds of type-variable K
        var m = new EnumMap<Op, String>(Op.class);
                            ^
  where K is a type-variable:
    K extends Enum<K> declared in class EnumMap
```

You never even reach the constructor's `Class<K>` argument — the parameterized
type `EnumMap<Op, String>` is rejected first. (Even if you tried the raw form to
sneak past, `EnumMap(Class<K> keyType)` calls `keyType.getEnumConstants()` and
would `NullPointerException`/`ClassCastException` at runtime, because `Op.class`
is not an enum class. The compile error is the honest stopping point.)

**One mechanism, two blades:** the interface that buys EJ38 extensibility is
structurally barred from every enum-optimized container in the JDK, because all
of them are bounded `E extends Enum<E>`.

## Part 2 — Ideal solution

`Calculator.java`, the only edit is the body of `basicSymbols()`:

```java
public static EnumMap<Basic, String> basicSymbols() {
    EnumMap<Basic, String> m = new EnumMap<>(Basic.class);
    m.put(Basic.DIV, "/");   // insert in any scrambled order...
    m.put(Basic.ADD, "+");
    m.put(Basic.MUL, "*");
    m.put(Basic.SUB, "-");
    return m;                // ...values() still iterates ADD, SUB, MUL, DIV
}
```

`basicSymbols().values().toString()` is `"[+, -, *, /]"` — declaration/ordinal
order, regardless of insertion order. The `EnumMap` key type is the *concrete*
enum `Basic`, so this is legal and fast; it is the cross-enum key (`Op`) that was
illegal. That is the EJ38 tradeoff, stated precisely: **you keep array-speed
per enum; you lose only cross-enum keying.**

The provided `interfaceEnumStaysPolymorphic` test shows the upside survives:
`Calculator.run(Basic.ADD, 3, 4)` is `7.0`, `Calculator.run(Sci.POW, 2, 3)` is
`8.0` — an `Op` reference dispatches across both enums.

### Scale up solutions

**S1 — Why the order is what it is.** An `EnumMap` is backed by a plain array
indexed by the key's `ordinal()` (and `EnumSet` by a `long` bitmask over
ordinals). Iteration walks the array in index order, i.e. *declaration order*.
Insertion order is never stored, so it cannot affect iteration — this is exactly
why `EnumMap`/`EnumSet` beat `HashMap`/`HashSet` of enums on both speed and
determinism.

**S2 — The ordinal persistence burn (EJ35).** Under V1, `MEDIUM.ordinal()` was
`1`, so the stored `1` meant `MEDIUM`. After alphabetizing to
`{ HIGH, LOW, MEDIUM }`, `Priority.values()[1]` is now `LOW`. So the row silently
decodes as `LOW`. **Nothing throws** — it is silent data corruption, the worst
kind. Fix: never persist `ordinal()`; persist `name()` (the constant's string)
and read it back with `Priority.valueOf(stored)`. `name()` is stable across
reordering; `valueOf("MEDIUM")` still yields `MEDIUM` after the reshuffle (and
throws `IllegalArgumentException` loudly if a constant was renamed/removed —
again, better than silent corruption). Empirically: ordinal `1` → `LOW` after
reorder; `name()` `"MEDIUM"` → `MEDIUM`.

## Why this works (and the gotcha)

`ordinal()` is a **position, not an identity**. The JDK leans on that position
internally — `EnumSet`/`EnumMap` are array/bitmask structures indexed by
`ordinal()`, which is why they require a real `Enum` type (`E extends Enum<E>`)
and reject interfaces. That same requirement is what bites the EJ38 pattern: the
moment you generalize over enums with an interface, you hand back the ordinal the
containers need, so they refuse your interface type.

The naive "I made an interface-enum, now I'll EnumSet/EnumMap them" attempt fails
at compile time for that structural reason — *not* a runtime mono-type check
(Line A) and *not* "wrong constructor argument" (Line B). Naming it precisely is
the senior signal.

## Common mistakes
- Saying `EnumSet.of(Basic.ADD, Sci.LOG)` "throws at runtime because it's
  mono-type" → wrong; it never runs. It is a compile-time *type-inference*
  failure (`equality constraints: Basic,Sci`).
- Saying line B fails "because the constructor needs an enum `Class`" → close but
  mislabeled; it fails earlier, at the type argument `EnumMap<Op, ...>` violating
  `K extends Enum<K>`. You never reach the constructor.
- "I'll just use `EnumSet<Op>` / `EnumMap<Op,…>` then" → the same bound rejects
  `Op` at declaration. Use `Set<Op>`/`Map<Op,…>` (a `HashSet`/`HashMap`), paying
  hashing cost and losing ordinal ordering — that's the tradeoff.
- Persisting `ordinal()` for durable state → silent corruption on any reordering
  (S2). Persist `name()`.
- Assuming `EnumMap` iterates in insertion order → it iterates in *ordinal*
  (declaration) order.

## Interviewer Q&A

**Q:** Does `EnumSet.of(Basic.ADD, Sci.LOG)` compile? If not, what kind of error?
**A:** No. It is a compile-time **type-inference** failure. The two-arg overload
is `<E extends Enum<E>> of(E, E)` and needs both args to be the same `E`;
`Basic` and `Sci` give contradictory equality constraints
(`equality constraints: Basic,Sci`). It is not a runtime exception and not a
"mono-type" runtime guard.

**Q:** And `new EnumMap<Op, String>(Op.class)`?
**A:** Also a compile error, but a different shape: `Op` is not within the bound
`K extends Enum<K>` of `EnumMap`. It is rejected at the type argument, before the
constructor's `Class<K>` parameter even matters.

**Q:** What's the single underlying reason both fail?
**A:** `EnumSet` and `EnumMap` are bounded `E extends Enum<E>` because they are
implemented as arrays/bitmasks indexed by `ordinal()`. An interface like `Op`
isn't an `Enum`, so it can never satisfy that bound. Extensible enums (EJ38) and
the enum-optimized containers are mutually exclusive on the *key/element* type.

**Q:** So what do you actually use to hold a mixed bag of `Op`s?
**A:** A general `Set<Op>` / `Map<Op, V>` (e.g. `HashSet`/`HashMap`). You keep
the extensibility and polymorphism, but you pay hashing cost and lose
ordinal-ordered, array-backed iteration. That is the concrete EJ38 tradeoff.

**Q:** Why does `EnumMap` beat `HashMap<MyEnum, V>`?
**A:** `EnumMap` is an array indexed by `ordinal()` — no hashing, no collisions,
no boxing of the key's hash, cache-friendly, and iteration is deterministic in
declaration order. `HashMap` hashes keys and gives no ordering guarantee.

**Q:** In what order does `EnumMap.values()` / `EnumSet` iterate?
**A:** Declaration (ordinal) order, never insertion order. In the exercise,
inserting `DIV, ADD, MUL, SUB` still yields `[+, -, *, /]`.

**Q:** Why is persisting `ordinal()` a bug, and what do you persist instead?
**A:** `ordinal()` encodes *position*, which changes if anyone reorders, inserts,
or removes a constant — old data then decodes to the wrong constant *silently*,
with no exception. Persist `name()` and restore with `valueOf(...)`; the name is
stable, and a missing name throws loudly instead of corrupting.

**Q:** Give a legit reason to call `ordinal()` at all, then.
**A:** Only for transient, in-process indexing into your own
ordinal-parallel array — which is exactly what `EnumSet`/`EnumMap` do for you.
Effective Java's guidance: never derive a durable value from `ordinal()`; let
the JDK's enum containers use it internally and reach for them instead of
hand-rolling `ordinal`-indexed arrays.

**Q:** How would you add operations *without* an interface and keep one enum?
**A:** Constant-specific method bodies on a single enum (each constant overrides
an abstract method), which keeps everything one `Enum` type and so stays
compatible with `EnumSet`/`EnumMap`. You trade away cross-enum extensibility
(you can't add new constants from another module) — the inverse of the EJ38
tradeoff.
