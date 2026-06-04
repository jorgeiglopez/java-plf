# Functional Interfaces — The `equals` That Wasn't Exempt

**Concept:** A lambda targets any interface with **exactly one abstract method**; `@FunctionalInterface` only *asserts* that count — it does not create it. The exemption for `Object` methods is a **signature match**, not a name match.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Everyone "knows" that an interface is functional if it has one abstract method, and that re-declaring `Object` methods like `equals`/`hashCode`/`toString` "doesn't count." That last clause is where seniors get cocky. The exemption in JLS §9.8 is defined by *overriding* the public methods of `Object` — which means a **signature match against `Object`'s methods**, not a match against the *name* `equals`. Put a generic type parameter in the wrong spot and your "exempt" method quietly becomes a *second* abstract method.

So: when does the compiler accept a lambda for an interface, and what does `@FunctionalInterface` actually buy you?

## Task

1. **Task 1 — make a SAM a lambda target.** In `SamProbe.java`, finish `isEmptyViaPlain`: target the `PlainSam` interface with a lambda that reports whether a `String` is empty, then return its result. This turns `plainSamAcceptsLambda` green. (Notice `SamWithDefaults` and `SamWithObjectMethods` are already wired and already pass — convince yourself *why* each is still functional.)

2. **Task 2 — predict the trap.** Consider this declaration (it lives only here, in markdown — do **not** add it as a `.java` file):
   ```java
   @FunctionalInterface
   interface Trap<T> {
       boolean test(T t);
       boolean equals(T o);   // looks exempt... is it?
   }
   ```
   Decide how many **abstract** methods `Trap<T>` really has, then set the constant `ABSTRACT_METHOD_COUNT_OF_TRAP` in `SamProbe.java` to that number. This is the intentionally-red test `predictAbstractMethodCount` — flip the constant to the value you can defend.

3. In `SOLUTION.md`, confirm whether `@FunctionalInterface interface Trap` above compiles, and state the one-line rule that decides it.

## Constraints

- Java 21, no external libraries.
- Do **not** edit the test file.
- Do **not** create a `.java` file for `Trap` — code that is *supposed* not to compile stays in markdown.

## Scale up (Optional)

- **A.** Does this compile? `@FunctionalInterface interface Q<T> { boolean test(T t); int hashCode(T t); }` — predict before you check the solution.
- **B.** `Comparator<T>` has `compare` *and* a re-declared `equals(Object)`, yet it is `@FunctionalInterface`. Explain why the `equals` re-declaration is legal there but the `equals(T)` in `Trap` is not.
- **C.** Remove the `@FunctionalInterface` annotation from `Trap`. Does a lambda `Trap<String> t = s -> true;` compile now? Why or why not?

## How to verify

```
mvn -q -Dtest=SamProbeTest test
```
After Task 1, three tests pass. After Task 2, all four pass — and only if your predicted count is the defensible one.

<details><summary>Hint (open only if stuck)</summary>
The Object-method exemption is "does this method *override* a public method of <code>Object</code>?" Override means same erased signature. <code>Object.equals</code> takes an <code>Object</code>. With <code>T</code> unbounded, what is the erasure of <code>equals(T)</code>, and does it match?
</details>
