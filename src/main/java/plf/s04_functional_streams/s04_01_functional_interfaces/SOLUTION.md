# Solution — The `equals` That Wasn't Exempt

## Ideal solution

**Task 1** — target `PlainSam` with a lambda:
```java
static boolean isEmptyViaPlain(String s) {
    PlainSam<String> p = str -> str.isEmpty();
    return p.test(s);
}
```

**Task 2** — `Trap<T>` has **2** abstract methods, so:
```java
static final int ABSTRACT_METHOD_COUNT_OF_TRAP = 2;
```

**Task 3** — `@FunctionalInterface interface Trap<T> { boolean test(T t); boolean equals(T o); }`
**does NOT compile.** `javac` reports something like:
`Unexpected @FunctionalInterface annotation ... Trap is not a functional interface ... multiple non-overriding abstract methods found in interface Trap`.

The one-line rule: **the `Object`-method exemption is a signature match against `Object`'s methods, not a name match — `equals(T)` with unbounded `T` erases to `equals(Object)` only by coincidence of erasure, but for *overriding* the JLS compares the declared signature, and `equals(T)` does not override `Object.equals(Object)`, so it is a second abstract method.**

### Scale up solutions

- **A.** `int hashCode(T t)` — **second abstract method, does not compile.** `Object.hashCode()` takes **no** arguments; `hashCode(T)` has a different signature, so it does not override `Object.hashCode()`. Same trap as `equals`, different method. (`Q` would be functional only if you wrote `int hashCode()`.)

- **B.** `Comparator<T>` re-declares `boolean equals(Object obj)` — argument type is exactly `Object`, so it **does** override `Object.equals(Object)` and is exempt; the only abstract method left is `compare`. The library does this deliberately to attach Javadoc about consistency with `compare`. `Trap`'s `equals(T)` takes `T`, not `Object`, so it overrides nothing and counts. The difference is purely the parameter type: `Object` vs `T`.

- **C.** Removing `@FunctionalInterface` changes **nothing** about the type. `Trap<T>` still has two abstract methods, so it is still not functional, so `Trap<String> t = s -> true;` **still does not compile** ("incompatible types: ... is not a functional interface"). The annotation was never what made it functional or not — it only forces an *earlier, clearer* compile error at the interface declaration instead of at the lambda use site.

## Why this works (and the gotcha)

Two independent rules collide here.

1. **What makes an interface functional (JLS §9.8):** exactly one *abstract* method that is not a public method of `Object`. Methods that override `Object`'s public methods (`equals`, `hashCode`, `toString`) are **not counted**. `default` and `static` methods are not abstract, so they are not counted either — that is why `SamWithDefaults` is functional.

2. **What `@FunctionalInterface` does:** *nothing* to the type. It is a compile-time *assertion*. A SAM type is functional with or without it (every lambda you ever wrote against `Runnable`-shaped helper interfaces proves this). The annotation only makes the compiler reject the declaration up front if the count isn't exactly one — turning a confusing "not a functional interface" error at the *lambda* into a clear error at the *interface*.

The gotcha fuses rule 1 with erasure intuition. Engineers see the *name* `equals` and pattern-match "Object method → exempt." But the exemption is about **overriding**, and overriding requires a matching signature against `Object`'s actual method. `Object.equals` takes `Object`. `equals(T)` with unbounded `T` does not override it — it is an *overload* sitting in the interface as a brand-new abstract method. So `Trap` has two abstract methods and is not functional. The floor drops precisely because "exempt" was never about the name.

## Common mistakes

- **"`equals` is always exempt."** → Only `equals(Object)` is. `equals(T)`, `equals(String)`, or any non-`Object` parameter is a *new* abstract method.
- **"`@FunctionalInterface` makes it functional."** → It only checks. Deleting it doesn't make a non-functional interface accept lambdas; adding it to a two-abstract-method interface only moves the error earlier.
- **"`default` methods break functional-ness."** → No. Only *abstract* methods count; any number of `default`/`static`/private methods is fine.
- **"`hashCode(T)` is exempt because `hashCode` is an Object method."** → `Object.hashCode()` takes no args; `hashCode(T)` overrides nothing.
- **Counting `equals`, `hashCode`, `toString` re-declared with their *exact* `Object` signatures as abstract methods.** → Those are genuinely exempt (that's the `SamWithObjectMethods` case, which is functional).

## Interviewer Q&A

**Q:** What makes an interface a functional interface?
**A:** It has exactly one abstract method, ignoring (a) `default`/`static`/private methods and (b) abstract methods that override a public method of `Object`. That single method is the lambda's target.

**Q:** What does `@FunctionalInterface` actually do?
**A:** Nothing to the type — a SAM type is functional whether or not it's annotated. The annotation is a compile-time assertion: the compiler rejects the *declaration* if the abstract-method count isn't exactly one. It's documentation with a guard.

**Q:** `interface Trap<T> { boolean test(T t); boolean equals(T o); }` — functional or not, and why?
**A:** Not functional. `equals(T)` with unbounded `T` does not override `Object.equals(Object)` (different parameter type), so it's a second abstract method. The Object-method exemption is a signature match, not a name match.

**Q:** Then why is `Comparator` functional despite declaring `equals`?
**A:** It declares `boolean equals(Object obj)` — parameter type exactly `Object` — which *does* override `Object.equals`, so it's exempt. Only `compare` is left.

**Q:** If I delete `@FunctionalInterface` from `Trap`, can I now assign a lambda to it?
**A:** No. The annotation never affected the type. `Trap` still has two abstract methods, so a lambda still won't compile against it; you'd just get the error at the lambda site instead of the declaration.

**Q:** Can a functional interface have `default` methods? `static`? How many?
**A:** Yes to both, any number. They're not abstract, so they don't count toward the one-abstract-method rule. `Predicate` (with `and`/`or`/`negate` defaults plus static `isEqual`/`not`) is the canonical example.

**Q:** Name the four core standard functional interfaces and their shapes.
**A:** `Supplier<T>` = `() -> T`, `Consumer<T>` = `T -> void`, `Function<T,R>` = `T -> R`, `Predicate<T>` = `T -> boolean`. Plus `UnaryOperator<T>`/`BinaryOperator<T>` (Function specializations), `BiFunction`, and the primitive-specialized variants (`IntFunction`, `ToIntFunction`, `IntPredicate`...) that exist to avoid boxing.

**Q:** Why prefer the standard interfaces (EJ44) over rolling your own?
**A:** Familiarity (callers and readers recognize them), interoperability (they compose with `andThen`/`compose`/`and`/`or`), and the primitive specializations avoid autoboxing costs. Write your own only when you need a different arity/signature, a descriptive name that conveys a contract, or stronger documented invariants — and then annotate it `@FunctionalInterface`.

**Q:** Why annotate your own functional interface even though it's optional?
**A:** It documents intent and, crucially, prevents a future maintainer from accidentally adding a second abstract method and silently breaking every lambda call site — the compiler stops them at the declaration instead.
