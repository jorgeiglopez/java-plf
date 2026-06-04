# Solution — Method References: The Receiver Slot

## Ideal solution

```java
public static Function<String, String> unboundUpper() {
    return String::toUpperCase;        // unbound: receiver is arg-zero
}

public static Supplier<String> boundUpper(String captured) {
    return captured::toUpperCase;      // bound: receiver captured now
}

public static Consumer<String> adderFor(List<String> target) {
    return target::add;                // bound: snapshots the list value now
}

public static String[] toStringArray(Stream<String> stream) {
    return stream.toArray(String[]::new);   // array ctor ref = IntFunction<String[]>
}

public static IntFunction<String[]> arrayMaker() {
    return String[]::new;
}
```

Test predictions:

- Beat 1: `unboundUpper().apply("hi")` → `"HI"`; `boundUpper("hi").get()` → `"HI"`. Same result, but the first
  *received* the string as arg-zero and the second *captured* it.
- Beat 2: `predictOriginal = List.of("x")`, `predictReassigned = List.of()`. The `Consumer` was bound to the
  `original` list at creation. Creating a second list afterward changes nothing the reference points to.
- Beat 3: `predict = {"a","b","c"}`.

## Why this works (and the gotcha)

**A method reference is a type-checked receiver-slot decision.** Pick the slot, the target SAM falls out:

| Form              | Receiver source                | Example                | Target SAM                    |
|-------------------|--------------------------------|------------------------|-------------------------------|
| Static `T::m`     | none                           | `Integer::parseInt`    | `Function<String,Integer>`    |
| Bound `obj::m`    | captured *now*                 | `"hi"::toUpperCase`    | `Supplier<String>`            |
| Unbound `T::m`    | supplied later as arg-zero     | `String::toUpperCase`  | `Function<String,String>`     |
| Constructor `T::new` | the object being built      | `ArrayList::new`       | `Supplier<List<…>>`           |

The **arity shift** (Beat 1) is the lens made visible: `String::toUpperCase` has arity 1 because the receiver
occupies arg-zero; `s::toUpperCase` has arity 0 because the receiver was already captured. Same method name — the
receiver moved out of the parameter list and into the closure.

The **eager-capture** gotcha (Beat 2): `obj::m` evaluates the receiver expression **once, at reference creation**
(JLS 15.13.3) — exactly like an argument, not like a lazy lambda body. It snapshots the *value*. Reassigning the
variable later cannot reach the already-captured value, so the original list is the one that receives `"x"`.
(This is also why `Objects.requireNonNull` on the receiver, and any side effects in the receiver expression, fire
at the creation site, not at each invocation.)

The **array constructor ref** (Beat 3): array length is an `int`, so the only SAM that fits is
`IntFunction<T[]>`. People reach for `Function<Integer,String[]>` and it does not satisfy `Stream.toArray`.
Almost nobody remembers `String[]::new` is `IntFunction<String[]>` — it is the EJ43 detail that gets skipped.

### Bonus probe answers (each verified with Java 21 `javac`)

**P1 — `BiFunction<String,String,String> f = Box::show;`** → **compiles, resolves to the static**, and
`f.apply("p","q")` returns `"static:pq"`. Why: for the *unbound* candidate `Box::show(String)`, arg-zero would
have to be a subtype of `Box`, but here arg-zero is `String` — not a subtype of `Box` — so the unbound candidate
is **not applicable**. The static `show(String,String)` *is* applicable. The slot is type-checked: the receiver
slot only accepts a `Box`. This is the better teaching moment than "static always wins" — it proves the slot is
checked by type, not matched by name.

**P2 — `BiFunction<Box2,String,String> g = Box2::show;`** → **compile error: "reference to show is ambiguous"**.
Now both candidates are applicable: the unbound `show(String)` (arg-zero `Box2` is a subtype of `Box2` ✓) and the
static `show(Box2,String)` (same shape ✓). Both match `(Box2,String)->String`, so JLS 15.13.1 declares it
ambiguous. The receiver slot became a competing overload candidate — the lens biting back.

**P3 — `Node::new`:** `Function<Node,Node> a = Node::new;` **compiles** (the one-arg ctor `Node(Node)` fills the
slot). `Supplier<Node> b = Node::new;` **fails** — there is no zero-arg constructor. Java 21 message:

```
error: incompatible types: invalid constructor reference
    constructor Node in class Node cannot be applied to given types
      required: Node
      found:    no arguments
```

Grade on the substring **"cannot be applied to given types"** — the verbatim text is not stable across JDK
versions, but that phrase is. (It is *not* "cannot find symbol: constructor Node()".)

**P4 — `Stream.of(1,2,3).toArray(int[]::new)`** → **compile error**. `Stream.<A>toArray(IntFunction<A[]>)`
requires `A` to be a *reference* type (`A extends Object`). `int[]::new` is `IntFunction<int[]>`, and `int` cannot
be the type variable `A`. The primitive array constructor ref exists and is legal — it just does not fit the
generic `A[]` slot. (Use a `Stream<Integer>` with `Integer[]::new`, or switch to `mapToInt(...).toArray()` for an
`int[]`.)

## Common mistakes

- Treating `obj::m` as lazy like a lambda body → it snapshots the receiver value at creation, not at call.
- Wiring `String::toUpperCase` to `Supplier<String>` (or `s::toUpperCase` to `Function`) → arity mismatch; the
  receiver slot dictates arity.
- Reaching for `Function<Integer,String[]>` for `toArray` → array length is `int`; the slot is `IntFunction`.
- Assuming `T::new` always satisfies `Supplier` → only if a zero-arg constructor exists.
- Believing an unbound `T::m` always loses to a static overload → it depends on whether arg-zero is a subtype of
  `T`; if both candidates apply, it is ambiguous, not a static win.

## Interviewer Q&A

**Q:** What are the four kinds of method reference?
**A:** Static (`Type::staticM`), bound instance (`obj::m`), unbound instance (`Type::m`), and constructor
(`Type::new`). They differ only in *where the receiver comes from*.

**Q:** `String::toUpperCase` is what functional type, and why is it arity 1?
**A:** `Function<String,String>` — it is *unbound*, so the receiver is supplied as argument zero, giving the SAM
one parameter. `"hi"::toUpperCase` is *bound* (`Supplier<String>`, arity 0) because the receiver was captured.

**Q:** When is the receiver of `obj::m` evaluated — at reference creation or at each call?
**A:** Once, at creation (JLS 15.13.3), like an argument. It snapshots the value. Reassigning the variable later
has no effect, and a null receiver throws a `NullPointerException` at the creation site, not at invocation.

**Q:** Why is `String[]::new` an `IntFunction<String[]>` and not a `Function<Integer,String[]>`?
**A:** An array constructor reference takes the length, which is a primitive `int`. The matching SAM is therefore
`IntFunction<T[]>`. This is what `Stream.toArray(String[]::new)` expects.

**Q:** Does `Stream.of(1,2,3).toArray(int[]::new)` compile?
**A:** No. `Stream.<A>toArray(IntFunction<A[]>)` needs `A` to be a reference type; `int` cannot be the type
variable `A`. The primitive-array ctor ref is legal but does not fit the generic `A[]` slot. Use `Integer[]::new`,
or `mapToInt(i->i).toArray()`.

**Q:** Given a `Box` with `String show(String)` and `static String show(String,String)`, what is
`BiFunction<String,String,String> f = Box::show`?
**A:** It compiles and resolves to the **static** method. The unbound candidate would need arg-zero to be a `Box`,
but arg-zero is `String`, so the unbound form is not applicable. The slot is type-checked by the receiver type.

**Q:** And when does that overload genuinely go ambiguous?
**A:** When both the unbound instance candidate and a static candidate are applicable with the same parameter
shape — e.g. `String show(String)` plus `static String show(Box,String)` targeted at
`BiFunction<Box,String,String>`. Both match, so javac reports "reference to show is ambiguous".

**Q:** Why prefer a method reference over the equivalent lambda (EJ43)?
**A:** When the lambda just forwards to an existing method, the reference is shorter and names the operation. EJ43:
use a method reference where it is clearer; keep the lambda when the parameter names add documentation value or the
reference would be longer/less clear (e.g. `x -> x` reads better than `Function.identity()` to some).
