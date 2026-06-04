# Solution — The fallback that always runs

## Ideal solution

```java
public String emailOrAudit(String id) {
    return findById(id)
            .map(User::email)
            .filter(e -> !e.isBlank())
            .orElseGet(() -> audit("missing:" + id));   // supplier — runs only when absent
}

public Optional<String> middleNameOf(String id) {
    return findById(id).flatMap(User::middleName);      // flatMap un-nests Optional<Optional<String>>
}
```

Both changes are one word each: `orElse` → `orElseGet`, `map` → `flatMap`.

### Scale up solution

The `Optional<String> middleName` component breaks EJ55 in several ways:

- **`Optional` is not `Serializable`.** If `User implements Serializable`, serializing an instance throws
  `java.io.NotSerializableException: java.util.Optional` at runtime — not at compile time. The failure is
  delocalized to whatever later code tries to serialize it.
- **Optional adds a layer with no payoff in a field.** A field has three states already — present, `null`,
  and the boxed `Optional`. You've traded one null check for an allocation plus a wrapper, and callers can
  still pass `null` for the `Optional` itself.
- **It bloats the API and the object.** Every `User` now allocates an extra object for a value that is
  conceptually just "maybe a string."

Fix: declare the component as `String middleName` (nullable), or model absence with a separate type/empty
string per your domain. Reserve `Optional` for **return values** of methods/streams where the caller must be
forced to reckon with absence — never as a field, constructor parameter, or collection element. For a
collection-typed result, return an empty collection, not `Optional<List<...>>`; for a primitive, use
`OptionalInt`/`OptionalLong`/`OptionalDouble`, not `Optional<Integer>`.

## Why this works (and the gotcha)

**Eager fallback.** `orElse(x)` is an ordinary method call: Java evaluates the *argument* `x` before
`orElse` runs, regardless of whether the `Optional` is present. So `audit("missing:" + id)` executes on
**every** call — incrementing the audit counter and writing a row even when the email is present. The value
is then discarded because the `Optional` was non-empty. `orElseGet(Supplier)` instead receives a lambda and
invokes it **only** in the empty branch. Same shape for throwing: `orElseThrow(ex)` builds `ex` eagerly;
`orElseThrow(() -> ex)` defers. The devious version: if the fallback argument itself can throw
(`audit(user.id())` where `id` is `null`), it detonates on the *happy path*, before presence is ever checked —
because the argument is built first. The present branch you trusted is where it blows up.

**`map` nests; `flatMap` un-nests.** `Optional.map(f)` is spec'd as: apply `f`, then wrap the result with
`Optional.ofNullable`. When `f` already returns an `Optional` (`User::middleName`), you get
`Optional<Optional<String>>`. That won't assign to an `Optional<String>` target, so the compiler stops you —
*if* the target type is load-bearing. `flatMap(f)` expects `f` to return an `Optional` and returns it
directly: exactly `Optional<String>`.

**The trap behind the trap (null-swallowing `map`).** Because `map` wraps with `ofNullable`, a mapper that
returns `null` yields `Optional.empty()` — silently. No NPE, no warning; an absence you observe is a `null`
you never handled. This is the same DNA as the eager `orElse`: *the cost lands somewhere other than the line
that looks wrong.* Don't confuse it with the nesting case — they need different fixes. `flatMap` is mandatory
only when the **mapper returns an `Optional`**; it does nothing for a mapper that returns a bare `null`.

## Common mistakes

- `orElse(audit(...))` → `audit` runs every call; audit log fills with present-value rows, and a throwing
  argument NPEs on the happy path. Use `orElseGet`.
- "Fixing" the Task 2 compile error by slapping `.get()` on `map(User::middleName)` → returns the inner
  `String` unsafely (NPE on empty) and re-wrapping it is noise. `flatMap` is the only clean fix.
- Reaching for `flatMap` whenever `map` "feels wrong" → superstition. `flatMap` is for mappers that *return*
  `Optional`. A mapper returning a bare `null` is a different bug (`map` swallows it); `flatMap` won't help.
- `isPresent()` + `get()` instead of `map`/`orElseGet` → the imperative anti-pattern EJ55 warns against.
- Declaring `Optional` as a field/parameter → not `Serializable`, extra allocation, three-state nullability.

## Interviewer Q&A

**Q:** What's the difference between `orElse` and `orElseGet`?
**A:** `orElse(value)` takes an already-computed value; its argument is evaluated eagerly on every call,
present or not. `orElseGet(supplier)` takes a `Supplier` and invokes it only when the `Optional` is empty.
Use `orElseGet` whenever the fallback is expensive or has side effects.

**Q:** Given `opt.orElse(compute())` where the optional is present, does `compute()` run?
**A:** Yes. Method arguments are evaluated before the call, so `compute()` runs and its result is then thrown
away. That's a latent perf bug or, if `compute()` has side effects, a correctness bug.

**Q:** `opt.map(o -> o.getThing())` returns `Optional.empty()` but you expected a value. Why, no NPE?
**A:** `map` applies the mapper then wraps with `Optional.ofNullable`. If `getThing()` returned `null`, the
result is `empty()`, not an NPE. The null is silently absorbed into absence.

**Q:** When is `flatMap` mandatory over `map`?
**A:** When the mapper itself returns an `Optional`. `map` would give `Optional<Optional<X>>`; `flatMap`
flattens it to `Optional<X>`. It is *not* a general substitute for `map` when a mapper returns a bare nullable.

**Q:** Why does EJ55 say not to use `Optional` as a field?
**A:** `Optional` isn't `Serializable` (a `Serializable` class with an `Optional` field throws
`NotSerializableException`), it adds an allocation and a redundant nullability layer, and it bloats the type.
Use a nullable field or a domain type; reserve `Optional` for return values.

**Q:** When *should* you return `Optional`?
**A:** From a method whose result may be legitimately absent and where you want to force the caller to handle
that — e.g. a lookup. Don't return `Optional<Collection>` (return an empty collection) or box a primitive
(use `OptionalInt` et al.), and avoid it on hot paths since it allocates.

**Q:** Is returning `Optional` always better than returning `null`?
**A:** No. It's better at API boundaries where absence is meaningful and the caller might forget a null check.
On internal hot paths or for fields it costs allocation for little benefit; a documented nullable can be the
right call.

**Q:** How would you make the `audit("missing:" + id)` fallback safe if `id` could be `null`?
**A:** Move it into `orElseGet(() -> audit(...))` so it only runs in the absent branch, and validate/handle
the `null` id explicitly. The eager form builds and runs the argument before presence is checked, so a
throwing argument detonates even on the present path.
