# Interfaces & Abstract Classes — A default isn't free

**Concept:** an interface `default` method is a contract you cannot quietly inherit
twice — and upstream interface evolution can refuse your class *two different ways*.
(EJ21 interface evolution; EJ22 interfaces define types.)
**Difficulty:** ★★☆ intermediate
**Est. time:** ~15 min

## Background

Java 8 let interfaces ship `default` methods so libraries could *evolve* without
breaking implementors (EJ21). The pitch: "just add a default, nobody has to change
anything." Mostly true — until two interfaces collide. The two snippets below are
**byte-twins**: identical class names, identical method `area()`, identical two
supertypes. They differ by **exactly one line**. Reflex says "same code, same
result." Reflex is wrong: each snippet is refused by `javac`, but for a *different*
reason and with a *different* fix. Can you predict both diagnostics before you peek?

## Task

Both snippets are markdown-only (they do **not** compile — that is the point). Open
`predictions.txt` (create it) and do this **in order**, no cheating:

1. Read **Snippet A** and **Snippet B** below.
2. Fill in **both** `// I predict:` lines in `predictions.txt` — the exact Java 21
   `javac` error text for each — **before** you run any compiler. No prediction, no
   credit. (Reveal order is the whole exercise: if you compile A, see its error, then
   "predict" B, you have learned nothing.)
3. For each snippet, also write the **one-line fix** that makes it compile.
4. Only then check yourself against `SOLUTION.md`.

**Snippet A — the two supertypes both `default`:**
```java
interface Drawable {
    default double area() { return 0.0; }
}
interface Measurable {
    default double area() { return 1.0; }
}
class Tile implements Drawable, Measurable {
}
```

**Snippet B — byte-twin of A; the ONLY change is line 5:**
```java
interface Drawable {
    default double area() { return 0.0; }
}
interface Measurable {
    double area();                // <-- the one changed line (was: default ... return 1.0;)
}
class Tile implements Drawable, Measurable {
}
```

`predictions.txt` skeleton:
```
Snippet A
// I predict: ____
fix:

Snippet B
// I predict: ____
fix:
```

There is also a tiny runnable payoff: the **fixed** hierarchy lives in
`AreaResolution.java` (a scaffold with a `TODO`) plus a red test
`AreaResolutionTest`. After you have committed your predictions, finish the scaffold
so the test goes green — that proves your fix actually resolves the conflict.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Do **not** run a compiler on the snippets until both `// I predict:` lines are
  filled.
- Do **not** edit the test. Fix the production scaffold `AreaResolution.java`.
- Exact text matters: "an error happened" is **not** a passing prediction. The
  verbatim `javac` line is the answer key.

## Scale up (Optional)

1. **The escape that compiles (subinterface / most-specific wins).** Change Snippet A
   so `Measurable extends Drawable` and both still default. Predict: error, or clean
   compile? Why does the "diamond" suddenly stop complaining?
2. **Class beats interface.** Replace one interface with an `abstract class Base`
   that has a *concrete* `area()`; the other supertype stays a defaulting interface.
   Predict the outcome and name the resolution rule.
3. **EJ23 — kill the tagged class.** Refactor a `Shape` that carries an
   `int kind` field and a `switch (kind)` into a sealed `interface Shape` with
   `record Circle`, `record Square` implementors and one `default`-free `area()`.
   Show how a `switch` *expression* over the sealed type gives you exhaustiveness for
   free (no `default` branch).
4. **EJ22 / EJ41 — type vs constant dump.** Write a *marker* interface (no methods,
   e.g. like `Serializable`) and a *constant* interface (only `static final`
   fields). Explain which one legitimately "defines a type" and why the constant
   interface is an anti-pattern that leaks into your API.

## How to verify

```
mvn -q -Dtest=AreaResolutionTest test
```

The test starts **red** (the scaffold throws). It goes green once you implement the
conflict resolution. Compare your two `// I predict:` lines against `SOLUTION.md`.

<details><summary>Hint (open only if stuck)</summary>
The two snippets fail for opposite reasons. In A, the compiler has <em>two</em>
competing bodies and refuses to pick — that is the classic "inherits unrelated
defaults". In B, after you reabstract one side, there is <em>zero</em> usable
body left, so the method is effectively abstract and the concrete class must
supply one. One fix overrides to break a tie; the other fix overrides to fill a
hole.
</details>
