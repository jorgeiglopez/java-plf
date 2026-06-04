# Variables & Initialization — Confident and Wrong

**Concept:** a virtual call from a superclass constructor reads subclass fields
*before* they are initialized — and `final` does **not** save you (JLS §12.5, §15.29).
**Difficulty:** ★★☆ intermediate
**Est. time:** ~15 min

## Background

A constructor runs your superclass *before* your subclass initializers. If the
superclass constructor calls an overridable method, virtual dispatch lands in
**your** override while **your** fields are still zeroed. Cocky devs answer this
one fast — and wrong. The twist that separates the seniors: one of these
"uninitialized" fields is `final` and reads a *correct* value anyway. Why?

You are looking at `Construction.java`:

```java
public abstract static class Animal {
    public final String snapshotInCtor;
    protected Animal() {
        // virtual dispatch into the subclass, mid-construction
        this.snapshotInCtor = "legs=" + legs() + " name=" + name() + " weight=" + weight();
    }
    public abstract int legs();
    public abstract String name();
    public abstract int weight();
}

public static final class Spider extends Animal {
    public final int legs = 8;                  // final + literal
    public String name = "tarantula";           // non-final reference
    public final int weight = computeWeight();   // final, initialized by a call
    @Override public int legs()    { return legs; }
    @Override public String name() { return name; }
    @Override public int weight()  { return weight; }
    private static int computeWeight() { return 5; }
}
```

## Task

1. **Commit your prediction FIRST.** Without compiling, fill in the blank below
   with the exact string the constructor captures into `snapshotInCtor` for
   `new Spider()`. Write it down (a comment, a scratch file — anywhere you can't
   edit after the fact):

   ```
   PREDICTION: snapshotInCtor == "legs=___ name=___ weight=___"
   ```

2. Then run the test and the `main` method (commands below) and compare.
3. In one sentence each, explain why `legs`, `name`, and `weight` read the
   values they do — and specifically why `legs` (also `final`) differs from
   `weight` (also `final`).

## Constraints

- Java 21, no external libraries beyond JUnit 5.
- Do **not** edit `Construction.java` or `ConstructionTest.java` to make your
  prediction "right." The point is the gap between what you predicted and what
  Java does.
- Decide your answer before you run anything.

## Scale up (optional)

1. **Make the constructor see `weight=5`** *without* removing the `final` on
   `weight` and *without* moving the `computeWeight()` call out of the field
   initializer. Is it possible? Explain.
2. **The honest fix.** Refactor so the superclass constructor never observes a
   half-built object, while keeping all three fields where they are. State the
   one rule of thumb you just applied.
3. **Varargs corner.** Predict the output of:
   ```java
   static String f(int... xs) { return xs == null ? "null" : "len=" + xs.length; }
   // f()          -> ?
   // f(1, 2)      -> ?
   // f((int[])null) -> ?
   ```
   Then say which call passes an *empty array* and which passes a *null array*.
4. **Overload phase.** Given both `g(int a, int b)` and `g(int... xs)`, what does
   `g(1, 2)` resolve to, and why does adding a varargs overload never silently
   "steal" an existing fixed-arity call?
5. **Static forward reference (markdown only — won't compile).** Why does
   `static int a = b; static int b = 1;` fail to compile, but routing the read
   through a method (`static int a = readB();`) compiles and yields `a == 0`?

## How to verify

```
mvn -q -Dtest=ConstructionTest test
```

The test is **green** — it asserts the true values. Your grade is whether your
*prediction* matched it. You can also eyeball the live output:

```
java -cp target/classes plf.s01_core_language.s01_05_variables_and_initialization.Construction
```

<details><summary>Hint (open only if stuck)</summary>
During <code>super()</code>, the subclass's field initializers have not run, so
real fields hold their zero/null defaults. But a <code>final</code> field whose
initializer is a <em>constant expression</em> (JLS §15.29) is a compile-time
constant: the compiler replaces every read of it with the literal, so there is
no field to be "uninitialized." A method-call initializer is not constant.
</details>
