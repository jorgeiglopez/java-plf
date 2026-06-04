# Object Creation — The Poisoned Five

**Concept:** a static factory method has a freedom a constructor does not — it may return a *shared, cached* instance. That freedom is a contract: the cached type must be immutable. A factory cannot keep that contract alone, and one fluent-looking mutator breaks it for the whole JVM.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

`new Money(...)` *always* hands you a fresh object — a constructor has no choice.
`Money.of(int)` does not: like `Integer.valueOf`, it caches small values (`0..10`)
and returns the same instance every time. That is the headline freedom of a static
factory method (Effective Java Item 1): *"it is not required to create a new object
each time it's invoked."*

Now the trap. `Money` looks immutable, and it ships a fluent-looking method
`withCents(long)`. A confident developer reads `of(5).withCents(99)` as "give me a
*copy* of five with 99 cents" — the way `String`, `LocalDate`, or `BigDecimal`
behave. It is **not** a copy. `withCents` mutates `this` and returns `this`.

So the same call site, `x.withCents(99)`:

- via `new Money(...)` → mutates a throwaway object. Harmless.
- via `Money.of(5)` → mutates the **single cached five** that every other caller in
  the JVM shares. Now `Money.of(5).cents()` reads `99` everywhere, forever.

Same code. One creation path survives; the other is poisoned. Can you predict which
values the map reports *before* you run it?

## Task

1. Read `Money.java`. Confirm the cache (`0..10`), the private constructor, the
   shared-instance return in `of`, and that `withCents` does `this.cents = ...; return this;`.
2. Open `src/test/java/.../s02_02_object_creation/MoneyFactoryTest.java`. It exercises
   two creation paths under the **same** fluent call and probes identity and state.
3. **Commit first.** On paper, write your prediction for each of the four `PREDICT_*`
   constants. They currently hold the *naive* guess; several are wrong. Do not run yet.
4. Run the test. Each failure message reveals the real Java 21 value. Edit **only**
   the four `PREDICT_*` constants — one per line — until the suite is green. Do not
   touch `Money`, the call sites, or the assertions.
5. Diagnose in one sentence each: (a) why `Money.of(5) == Money.of(5)` is `true` but
   `new` results are never `==`, and (b) why mutating the object returned by `of(5)`
   corrupts every later `of(5)` while doing the same to a `new` object does nothing.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Edit only the four `PREDICT_*` constants in the test. Everything else is fixed.
- The "obvious senior fix" — "cache like `Integer.valueOf`, ship it" — is the trap,
  not the solution. Caching presupposes immutability; identifying *why* this cache is
  unsafe is the exercise.

## Scale up (Optional)

The brief covers five creation idioms. Each is the *same blade* — "who controls
whether you get a fresh object, and what contract does that impose?" Work these on
paper (most are markdown-only by the build rules; see `SOLUTION.md`).

1. **EJ2 — Builder.** Rewrite `Money` construction behind a `Money.Builder` with a
   required `currency` and an optional `cents`, validating the invariant in `build()`,
   not in the setters. Predict: which line throws if `currency` is never set, and what
   exception type is idiomatic there? Then answer: *why can't a Builder cache the way
   `of()` does?*
2. **EJ4 — Noninstantiability.** Make a `MoneyUtils` class that must never be
   instantiated (only static helpers). Show the *exact* `javac` error a caller gets
   from `new MoneyUtils()`, and explain why the private constructor should also
   `throw new AssertionError()` — what attack does that defend against that the
   `private` modifier alone does not?
3. **EJ3 — Singleton.** Express "exactly one `Money` zero" three ways: a public final
   field, a static factory holding a single instance, and an `enum`. The
   double-checked-locking variant has a one-word bug — name it. Why is the single-element
   `enum` the senior default, and how does it relate to the cache in this exercise (a
   singleton *is* a factory cache of size one)?
4. **EJ5 — Dependency injection.** A `Wallet` needs a `Money` formatter. Contrast
   constructor injection (a `final` field set once) with field injection (a mutable
   field a framework pokes in later). Show how field injection defeats `final` and
   makes the class un-testable without a container.

## How to verify

```
mvn -q -Dtest=MoneyFactoryTest test
```

The test starts **red**. It is green only once all four `PREDICT_*` constants match
Java's real behavior. Then re-read your paper predictions and count how many you got
wrong — and which idiom would have prevented the bug entirely.

<details><summary>Hint (open only if stuck)</summary>
A constructor is *required* to return a brand-new object, so two `new` results can
never be `==`. A static factory is *not* required to — `of(5)` returns the same
cached instance each call, so `of(5) == of(5)` is `true`. That sharing is fine only
while `Money` is immutable. `withCents` is fluent but not immutable: it mutates
`this` in place. Mutating a `new` object touches only your throwaway copy; mutating
the object `of(5)` returned mutates the one cached five everyone shares.
</details>
