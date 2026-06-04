# Solution — The Poisoned Five

## Ideal solution

The four predictions, corrected to Java 21 (Amazon Corretto) behavior:

```java
static final boolean PREDICT_NEW_EQ_NEW = false;                 // new == new        -> false
static final boolean PREDICT_OF_EQ_OF = true;                    // of(5) == of(5)    -> true
static final long PREDICT_FRESH_CENTS_AFTER_MUTATION = 500;      // fresh five        -> 500 (untouched)
static final long PREDICT_CACHED_CENTS_AFTER_MUTATION = 99;      // cached five       -> 99  (poisoned)
```

With those values the suite is green. The lesson is the asymmetry between the last
two lines: the **same** fluent call `x.withCents(99)` leaves a freshly-`new`-ed five
at `500` but drives the cached five to `99` for the rest of the JVM's life.

Trace of the run:

```
Money.of(5) == Money.of(5)        -> true   // same cached instance
new(...) == new(...)              -> false  // a constructor must return a fresh object

freshFive().withCents(99)         -> 99     // mutated a throwaway object
freshFive().cents()               -> 500    // a brand-new five is unaffected

Money.of(5).withCents(99)         -> 99     // mutated the SHARED cached five
Money.of(5) (is poisoned)         == poisoned ? true   // same object
Money.of(5).cents()               -> 99     // every of(5) in the JVM now reads 99
```

The fix is **not** "remove the cache" and **not** "add a stronger something." The
cache is a legitimate, valuable optimisation. The fix is to make the cached type
**deeply immutable**, so the freedom the factory takes (sharing instances) is backed
by the contract that freedom requires (instances that cannot change):

```java
public final class Money {
    private static final int CACHE_MAX = 10;
    private static final Money[] CACHE = new Money[CACHE_MAX + 1];
    static { for (int d = 0; d <= CACHE_MAX; d++) CACHE[d] = new Money(d * 100L); }

    private final long cents;                 // <-- final: no instance can ever change

    private Money(long cents) { this.cents = cents; }

    public static Money of(int dollars) {
        if (dollars >= 0 && dollars <= CACHE_MAX) return CACHE[dollars];
        return new Money(dollars * 100L);
    }

    public long cents() { return cents; }

    // Fluent AND immutable: returns a NEW object, never mutates a shared one.
    public Money withCents(long newCents) { return new Money(newCents); }

    @Override public String toString() { return "Money(" + cents + ")"; }
}
```

Two changes carry the whole repair: `cents` is `final`, and `withCents` returns
`new Money(newCents)` instead of mutating `this`. Now `of(5).withCents(99)` builds a
fresh object and the cached five stays `500` — sharing is safe because there is
nothing shareable to corrupt. A `record Money(long cents)` would get you immutability
for free, though you'd hide the canonical constructor behind a private one and expose
`of(...)` as the public door.

### Scale up solutions

**1. EJ2 — Builder.**

```java
public final class Money {
    private final String currency;
    private final long cents;

    private Money(Builder b) { this.currency = b.currency; this.cents = b.cents; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String currency;          // required
        private long cents = 0;           // optional, defaulted

        public Builder currency(String c) { this.currency = c; return this; }
        public Builder cents(long c)      { this.cents = c;    return this; }

        public Money build() {
            if (currency == null) {                     // <-- invariant checked HERE
                throw new IllegalStateException("currency is required");
            }
            return new Money(this);
        }
    }
}
```

The invariant lives in `build()`, not in the setters, because a half-built Builder is
*allowed* to be incomplete — you only demand a valid combination at the moment you
freeze it into a `Money`. `IllegalStateException` is the idiomatic choice: the
*object's state* (the Builder) is wrong, not a single argument (which would be
`IllegalArgumentException`). `Money.builder().cents(500).build()` throws on the
`build()` line because `currency` was never set.

*Why can't a Builder cache the way `of()` does?* A Builder is mutable and one-shot by
design — its whole job is to accumulate parameters before producing a value. There is
no stable key to cache on until `build()` runs, and the natural interning point is the
*product* `Money`, not the Builder. Caching belongs to the static factory (`of`), and
it is safe there only because `Money` is immutable. Same blade: caching presupposes
immutability of the cached thing.

**2. EJ4 — Noninstantiability.** A utility class with only static members should be
non-instantiable. Make the constructor `private` *and throw*:

```java
public final class MoneyUtils {
    private MoneyUtils() { throw new AssertionError("no instances"); }   // <-- defends reflection
    public static long toCents(int dollars) { return dollars * 100L; }
}
```

A caller that writes `new MoneyUtils()` from another class gets this **exact** Java 21
(Corretto) `javac` error:

```
error: MoneyUtils() has private access in MoneyUtils
```

(Note: the call *would* compile inside `MoneyUtils` itself — a private constructor is
accessible within its own top-level class — so the non-compiling snippet only exists
in *client* code, which is why it lives here in markdown, never as a committed
`.java` file.)

*Why also `throw`?* The `private` modifier stops ordinary callers, but **reflection
defeats it**: `MoneyUtils.class.getDeclaredConstructor()` + `setAccessible(true)` +
`newInstance()` will happily build an instance of a class whose constructor merely
does nothing. Throwing inside the constructor closes that door — reflective
instantiation then surfaces as `InvocationTargetException` wrapping your
`AssertionError`. (Empirically verified on Corretto 21.0.11: a plain private
constructor is built by reflection; one that throws `AssertionError` causes
`newInstance` to throw `InvocationTargetException` with cause `AssertionError`.)
Also: do **not** make such a class `abstract` to prevent instantiation — a subclass
can be instantiated, and it misleads readers into thinking it's designed for
extension.

**3. EJ3 — Singleton.** "Exactly one zero `Money`", three ways:

```java
// (a) public final field — simple, and the API obviously can't return anything else.
public final class ZeroField {
    public static final ZeroField INSTANCE = new ZeroField();
    private ZeroField() {}
}

// (b) static-factory singleton — flexibility: you could switch to non-singleton later
// without changing the API, and the factory is a method reference (Supplier<ZeroFactory>).
public final class ZeroFactory {
    private static final ZeroFactory INSTANCE = new ZeroFactory();
    private ZeroFactory() {}
    public static ZeroFactory getInstance() { return INSTANCE; }
}

// (c) enum singleton — the senior default (Effective Java Item 3).
public enum ZeroEnum { INSTANCE; }
```

The broken **double-checked-locking** lazy singleton — the one-word bug is
**`volatile`** (missing on the field):

```java
public final class LazyZero {
    private static LazyZero instance;          // <-- BUG: must be `volatile`
    private LazyZero() {}
    public static LazyZero getInstance() {
        if (instance == null) {                // first check (no lock)
            synchronized (LazyZero.class) {
                if (instance == null) {        // second check (locked)
                    instance = new LazyZero(); // construction can be reordered
                }
            }
        }
        return instance;
    }
}
```

Without `volatile`, another thread can observe a *non-null but partially constructed*
`instance`: the JMM permits the publish of the reference to be reordered ahead of the
writes that initialise the object's fields. `volatile` adds the happens-before edge
that forbids that reordering. The `enum` form sidesteps the entire problem: the JVM
guarantees enum constants are constructed once, lazily, and thread-safely at class
initialisation, and it is the only approach immune to both reflection *and*
serialization attacks (a deserialized ordinary singleton would otherwise create a
second instance unless you add `readResolve`).

*Relation to this exercise:* a singleton **is** a static factory that caches exactly
one instance. The same immutability discipline applies — a mutable singleton shared
across threads is the multi-threaded cousin of the poisoned five.

**4. EJ5 — Dependency injection.** Constructor injection vs field injection:

```java
// Constructor injection: the dependency is final, set once, and the object is
// fully formed and testable the instant `new` returns.
public final class Wallet {
    private final MoneyFormatter formatter;   // <-- final
    public Wallet(MoneyFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter);
    }
    public String render(Money m) { return formatter.format(m); }
}

// Field injection: the field cannot be final (a framework assigns it AFTER
// construction via reflection), so the object has an invalid intermediate state
// and you cannot unit-test it without the container wiring the field.
public final class WalletBad {
    @Inject private MoneyFormatter formatter; // <-- NOT final; null until injected
    public String render(Money m) { return formatter.format(m); } // NPE pre-injection
}
```

Field injection *defeats `final`* (you can't mark an externally-poked field `final`),
which removes the construction-time guarantee that the object is complete and the JMM
safe-publication benefit of final fields. It also defeats **testability**: with
constructor injection a test just calls `new Wallet(fakeFormatter)`; with field
injection the test needs reflection or a DI container to populate the private field.
The senior rule (Effective Java Item 5): wire dependencies in through the constructor;
prefer immutability; let the *caller* control object creation rather than hiding it
inside the class.

## Why this works (and the gotcha)

Object creation in Java has a sharp dividing line:

- A **constructor** (`new T(...)`) is *contractually required to return a fresh
  object* on every invocation. That's why `new Money(500) == new Money(500)` is always
  `false`.
- A **static factory method** (`Money.of(...)`) is *not* required to. It may return a
  cached, shared, even subtyped instance. That is its headline advantage — and
  `of(5) == of(5)` being `true` is that advantage made visible.

That freedom is a **contract the factory cannot keep alone**. Returning a shared
instance is safe *only if that instance can never change*. `Money` looked immutable
and even read fluently, but `withCents` quietly mutated `this` and returned `this`.
The instant a shared object is mutable, the cache becomes a JVM-wide landmine: the
same call site is harmless through `new` (you mutate a throwaway) and catastrophic
through `of` (you mutate the one instance everyone shares).

The gotcha that survives the interview: **"cache like `Integer.valueOf`, ship it" is
the wrong instinct.** Caching is not a free optimisation you bolt on — it is a
*promise* that the cached type is immutable. The lie that bites people is "fluent ==
immutable." A method that returns `this` is fluent and mutating; a method that returns
a *new* object is fluent and immutable. Only the second is safe to cache.

## Common mistakes

- **"Two `of(5)` calls must be different objects, like `new`."** A factory is free to
  reuse; `Integer.valueOf`, `Boolean.valueOf`, `List.of()` and friends all cache.
- **"`withCents` returns a copy, it's fluent."** Fluent only means it returns *a*
  `Money` for chaining. This one returns `this` after mutating it — fluent *and*
  in-place. Read the body, not the call site.
- **"Just remove the cache to fix it."** The cache is fine and valuable. The defect is
  mutability of a *shared* object; the fix is `final` fields + a copying `withCents`
  (or a `record`).
- **"Make the constructor public again so callers use `new`."** That hides the real
  lesson and throws away the factory's benefits (caching, naming, returning subtypes,
  controlling instance count).
- **EJ4: `private` constructor is enough.** Reflection (`setAccessible(true)`) bypasses
  it; throw inside the constructor to truly forbid instances.
- **EJ3: lazy DCL without `volatile`.** Lets a thread see a half-constructed singleton;
  prefer an `enum` or a holder class.

## Interviewer Q&A

**Q:** What can a static factory method do that a constructor cannot?
**A:** Four things (Effective Java Item 1): it has a *name* (so multiple factories with
the same parameter types can coexist); it is *not required to create a new object*
each call (caching / instance control, as `of` does here and `Integer.valueOf` does);
it can return *any subtype* of its return type (e.g. an implementation hidden behind an
interface); and the returned class can *vary by input* and need not even exist when the
factory is written (service-provider frameworks). The cost: classes with only private
constructors can't be subclassed, and factories aren't as discoverable as constructors.

**Q:** In the exercise, `of(5) == of(5)` is `true` but two `new Money(...)` are never
`==`. Why?
**A:** A constructor must produce a fresh object every time, so two invocations are
distinct identities. The factory cached `0..10` and returns the *same* instance for
`of(5)`, so the two references are identical. That is instance control, the factory's
defining freedom.

**Q:** The same call `x.withCents(99)` is harmless via `new` and corrupts the JVM via
`of(5)`. Explain.
**A:** `withCents` mutates `this` and returns `this`. Through `new`, `x` is a throwaway
object nobody else holds, so mutating it is invisible. Through `of(5)`, `x` *is* the
single cached five that every caller shares, so mutating it changes what every future
`of(5)` observes. Same code, different object graph: one private, one shared.

**Q:** A senior dev says "cache small values like `Integer.valueOf` and ship it." What's
wrong?
**A:** Caching is a contract, not a free win: it is only safe if the cached type is
**immutable**. `Money` here is mutable (`withCents` writes a field), so the shared
instance can be poisoned. The right move is to make `Money` immutable — `final` fields,
no in-place mutators, a copying `withCents`, or a `record` — *then* cache.

**Q:** How do you make a utility class non-instantiable, and why isn't `private` on the
constructor sufficient?
**A:** Give it a single `private` constructor that `throw`s (e.g. `AssertionError`). A
bare `private` constructor blocks normal `new` (a client gets
`error: <ctor>() has private access`), but **reflection** can call
`setAccessible(true)` and build an instance anyway; throwing in the body defeats that
too (the reflective call then throws `InvocationTargetException` wrapping the error).
Don't use `abstract` for this — subclasses remain instantiable and it signals
"designed for extension," which is a lie.

**Q:** Give the exact `javac` error when client code does `new MoneyUtils()` on a
class whose only constructor is private.
**A:** `error: MoneyUtils() has private access in MoneyUtils`. Note it only fails from
*another* class — inside `MoneyUtils` itself the private constructor is accessible, so
the demonstration must put the offending `new` in client code.

**Q:** Why is an `enum` the preferred singleton, and what's the bug in lazy
double-checked locking?
**A:** A single-element `enum` is constructed once by the JVM, is thread-safe and lazy
by class-init semantics, and is immune to reflection and serialization attacks that can
otherwise create a second instance. The DCL bug is a **missing `volatile`** on the
instance field: without it the JMM may publish a non-null reference before the object's
fields are initialised, so another thread sees a partially constructed singleton.
`volatile` supplies the happens-before edge that forbids that reordering.

**Q:** Constructor injection vs field injection — why does the senior answer favour the
constructor?
**A:** Constructor injection lets the dependency be `final`, guarantees the object is
fully formed and valid the moment `new` returns, gives JMM safe publication for free,
and is trivially testable (`new Wallet(fake)` — no container). Field injection can't be
`final` (a framework sets it post-construction by reflection), leaves a window where the
field is null, and forces tests to use reflection or a DI container. The principle: the
caller should control creation and wiring; the class shouldn't reach out for its own
dependencies.

**Q:** When is a constructor actually the right choice over a factory, builder, or
singleton?
**A:** When you genuinely want a fresh, independent instance every time and have no need
for a name, instance control, subtype return, or staged construction — e.g. simple value
objects with one obvious way to build them. Factories shine when you want caching, named
variants, or to return an interface; builders when there are many optional/required
parameters; singletons/enums when exactly one instance must exist. Reach for the extra
machinery only when one of those freedoms earns its keep.
