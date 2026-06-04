# Solution — Who poisoned the slot?

## Ideal solution

### The prediction
`Demo.main` throws **`ClassCastException`**. It does **not** throw inside
`NameProcessor.totalLength`, and it does **not** throw at `registry.get(List.class)`. It throws
at:

```java
names.forEach(name -> System.out.println(name.toUpperCase()));   // Demo.java
```

on the **first element**, before printing anything. The message reads roughly:

```
class java.lang.Integer cannot be cast to class java.lang.String
```

(the JDK adds the module/loader noise around it).

### The one-sentence root cause
> The contract broke at `registry.put(List.class, List.of(1, 2, 3))` inside
> `NameProcessor.totalLength`; because of **erasure** there is only one `List.class` token, so
> the container cannot tell a `List<String>` slot from a `List<Integer>` slot and happily
> overwrites it.

### Walking the corruption
1. `Demo` stores `List.of("alpha","beta","gamma")` under `List.class`. So far so good.
2. `NameProcessor.totalLength` reads it back, sums the lengths (works — they really are
   `String`s), then **silently overwrites the same key** with `List.of(1, 2, 3)` and returns
   an innocent-looking `int`. The caller sees only a number; the slot has been poisoned.
3. Back in `Demo`, `List<String> names = registry.get(List.class)` succeeds. `get` runs
   `List.class.cast(value)` — and the value *is* a `List`, so the cast passes. No warning, no
   exception. The static type `List<String>` is a lie the compiler can't catch.
4. `names.forEach(name -> ... name.toUpperCase())`: the lambda parameter is typed `String`, so
   `javac` inserts a checked cast `(String) element`. The first element is an `Integer` →
   **`ClassCastException`**, at the read site, far from the `put` that caused it.

Why `get()` is powerless: `List.class` is a single, *raw* `Class<List>` object. The language
has **no** `List<String>.class` literal — that's a compile error — so the container can only
ever be keyed by the erased `List.class`. `cast` therefore checks reifiable type (`instanceof
List`) and nothing about element type. EJ33's container is typesafe *only for reifiable keys*.

## Why this works (and the gotcha)

The trap is **misattribution of blame**, engineered with three accomplices:

- **The executioner** is the stream/`forEach` — it's where the JVM throws, so it *looks*
  guilty. It is innocent; it faithfully executed a cast the compiler demanded.
- **The accomplice** is `TypesafeMap.get`. It returns corrupt data without a peep because
  `Class.cast` can't see generics. It enabled the lie but didn't author it.
- **The murderer** is the `put(List.class, ...)` line inside `NameProcessor` — invisible from
  the call site because it lives behind a black-box API.

The senior insight: a `Class<T>` heterogeneous container is only typesafe for **reifiable**
types. The moment a key is a generic type (`List<String>`, `Map<K,V>`), erasure collapses every
instantiation onto one token, and *any* code holding the map can clobber your slot with a
differently-parameterized value. `get()` can never detect it, no matter how you implement it.

## Common mistakes
- **Blaming the stream / `forEach`.** It's the crime scene, not the criminal. If your sentence
  fingers `forEach` or `toUpperCase`, you failed the exercise.
- **Blaming `get()`.** `get` is where you *notice* nothing wrong; `put` is where it went wrong.
  `get` cannot be fixed to catch this — there is no element-type information to check against.
- **"Just make `put` validate the element type."** With what? The value is a `List`; to inspect
  its element type you'd have to peek at a runtime element (empty list defeats that) — and even
  then the *key* can't record the intended element type, because `List.class` is the only token
  available. The defect is structural, not a missing `if`.
- **Trying `List<String>.class` as the key.** Compile error: *cannot select from parameterized
  type*. There is no class literal for a parameterized type.
- **Expecting the CCE at `get()`.** `List.class.cast(aList)` always passes for any `List`.

## How to fix it for real (next sub-topic)
Replace the raw `Class<T>` key with a **super type token** (Gafter's gadget): an abstract
`TypeRef<T>` captured via an anonymous subclass so the *full* `ParameterizedType`
(`List<String>` vs `List<Integer>`) is reified at runtime and can serve as a distinct map key.

```java
// sketch only — full version is exercise s03_08 / a TypeToken
abstract class TypeRef<T> {
    final Type type;
    TypeRef() {
        this.type = ((ParameterizedType) getClass().getGenericSuperclass())
                        .getActualTypeArguments()[0];
    }
    @Override public boolean equals(Object o) {
        return o instanceof TypeRef<?> t && type.equals(t.type);
    }
    @Override public int hashCode() { return type.hashCode(); }
}
// new TypeRef<List<String>>(){}  and  new TypeRef<List<Integer>>(){}  are now DIFFERENT keys.
```

That distinguishes the two slots — at the cost of giving up the clean `Class.cast` guarantee
(you cast unchecked, trusting the token).

## Interviewer Q&A

**Q:** What is a typesafe heterogeneous container, and what makes it "typesafe"?
**A:** A container that can hold values of many different types, keyed by a `Class<T>` token,
with a `put(Class<T>, T)` / `get(Class<T>)` API over a `Map<Class<?>, Object>`. It's typesafe
because the key's type parameter ties the stored value's compile-time type to the key, and
`get` runs `type.cast(...)` so a wrong retrieval fails immediately — *for reifiable types*.

**Q:** In this exercise the CCE fires in `forEach`, far from the bug. Where did the type
contract actually break, and why doesn't `get()` catch it?
**A:** It broke at `put(List.class, List.of(1,2,3))` inside `NameProcessor`. `get` runs
`List.class.cast(value)`, which only checks `value instanceof List` — true for any list, of any
element type — so the corrupt list passes through. The CCE only appears when a
compiler-inserted `(String)` cast runs on an actual element.

**Q:** Why can't you key the slot by `List<String>.class` to keep the two lists apart?
**A:** There is no class literal for a parameterized type — `List<String>.class` is a compile
error. Erasure means all `List` instantiations share the single `List.class` object, so the
container can't even *express* "list of strings" as a key.

**Q:** Could you fix `get()` to detect the corruption?
**A:** No. `get()` has no element-type information to validate against — neither the key
(`List.class`) nor the value (an erased `List`) carries the intended element type. The defect is
structural to `Class`-token keys, not a missing check.

**Q:** Then how *do* you build a heterogeneous container that distinguishes `List<String>` from
`List<Integer>`?
**A:** Use a super type token (Gafter's gadget / Guava `TypeToken`): an abstract generic class
subclassed anonymously so its `ParameterizedType` is captured at runtime via
`getGenericSuperclass()`. Those tokens are distinct keys. The trade-off: you lose the checked
`Class.cast` and must cast unchecked, trusting the token's correctness.

**Q:** `<T> T get(Class<T> type)` uses `type.cast(...)`. What's the advantage over
`return (T) store.get(type);`?
**A:** `(T) store.get(type)` is an *unchecked* cast erased to a no-op — corruption surfaces
later as a heap-pollution CCE. `type.cast(...)` is a *dynamic* check against the reified class,
so for reifiable keys a wrong value fails right at `get()`. (For `List.class` even `cast` can't
help — it only checks the raw type.)

**Q:** This container is shared and mutable; any caller with the reference clobbered a slot.
What's the broader design lesson?
**A:** A `Class`-token map gives every holder write access to every typed slot with no
element-type protection on generic keys. Treat such maps as you would any shared mutable state:
confine or make immutable where possible, restrict who can `put`, and key only by reifiable
types — or move to super type tokens when generic keys are unavoidable.

**Q:** Where does `@SuppressWarnings("unchecked")` legitimately belong in a heterogeneous
container, and how would you justify it?
**A:** On the narrowest possible scope — typically a single statement in a token-based `get`
where you cast the stored `Object` to `T` after consulting the captured type — with a comment
proving the cast is safe given the put/get invariant (EJ27). For a plain `Class`-token version
you avoid the suppression entirely by using `Class.cast`.
