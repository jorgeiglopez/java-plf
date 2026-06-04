# OOP Fundamentals — Dispatch Is Decided in Two Different Places

**Concept:** An **override** binds at **runtime** on the object's actual type; an **overload** is chosen at **compile time** on the reference's static type. One scaffold, one reference (`Animal a = new Dog()`), five calls — and the call decided at compile time is the one that humbles people.
**Difficulty:** ★★☆ intermediate
**Est. time:** ~20 min

## Background

Everyone "knows" polymorphism: `Animal a = new Dog(); a.speak()` calls `Dog.speak()` because dispatch follows the runtime object. True — for *overridden* methods. But that single fact gets over-applied. The same `a`, calling an *overloaded* `greet(...)`, does **not** consult the runtime type at all: the compiler picks the overload from `a`'s declared type, `Animal`, before the program ever runs.

So one reference makes two different decisions: `speak()` is resolved late (runtime, on `Dog`), `greet(...)` is resolved early (compile time, on `Animal`). If you believe "the real type always wins," you will get the override right and the overload wrong — which is exactly how you learn you were conflating two mechanisms.

The scaffold (`DispatchLab.java`):

```java
static class Animal {
    private final String species = "Animal";
    String speak()           { return species + " makes a generic noise"; }
    String greet(Object o)   { return "greet(Object): " + o; }
    String greet(String s)   { return "greet(String): " + s; }
    Animal reproduce()       { return new Animal(); }
}
static class Dog extends Animal {
    @Override String speak()    { return "Woof"; }
    @Override Dog reproduce()   { return new Dog(); }   // covariant return
}
static class Cat extends Animal {
    @Override String speak()    { return "Meow"; }
}
```

Driver:

```java
Animal a = new Dog();
a.speak();
a.greet("hi");
a.greet(null);
var x = a.reproduce();
Cat c = (Cat) a;
```

## Task

1. **Predict first, in writing.** Before compiling or running anything, fill in this table. For row 4, write the **static (compile-time) type** the compiler infers for `var x` — not the runtime object. For row 5, write whether the cast compiles, and if so, what it does at runtime.

   | # | Expression | Your prediction (exact string / type / outcome) |
   |---|------------|-------------------------------------------------|
   | 1 | `a.speak()` | |
   | 2 | `a.greet("hi")` | |
   | 3 | `a.greet(null)` | |
   | 4 | static type of `var x = a.reproduce()` | |
   | 5 | `Cat c = (Cat) a;` (compiles? runtime?) | |

2. **Commit, then verify.** Open the red test `DispatchPredictionsTest`. It encodes the predictions of a developer who thinks the runtime type governs *everything*, so it **starts failing**. Run it, read the real values off the failure messages, and fix the five `EXPECTED_*` constants so the test goes green.
3. **Do NOT modify `DispatchLab`.** The production classes are correct. Only the *predictions* in the test are wrong. In particular, do **not** add a third `greet(...)` overload (see Constraints).
4. Run `DispatchLab.main` and confirm the five printed lines match your corrected predictions.
5. **The covariant compile-fail aside.** In `SOLUTION.md` you will find two assignments — `var x = a.reproduce();` (compiles) and `Dog d = a.reproduce();` (does **not** compile). Predict, in one line, *why* the second one fails even though the object really is a `Dog`.

## Constraints

- Java 21, no external libraries (JUnit 5 only).
- Do not modify `DispatchLab` — fix only the test's `EXPECTED_*` constants.
- **`Animal` must declare exactly `greet(Object)` and `greet(String)`, on one inheritance line, and nothing else.** Adding a third unrelated overload (e.g. `greet(Integer)`) turns `a.greet(null)` from a deterministic call into a *compile error* (`reference to greet is ambiguous`). The trap depends on exactly two overloads.
- Commit your five predictions on paper/in notes *before* running anything.

## Scale up (Optional)

Same engine — early vs late binding — wearing new costumes. Predict each, then check `SOLUTION.md`.

1. **`null` resolution.** Why does `a.greet(null)` pick `greet(String)` rather than `greet(Object)`? What single change to the overload set would make `a.greet(null)` fail to *compile*?
2. **`var` is not dynamic.** `var x = a.reproduce();` — `x.getClass().getSimpleName()` prints `Dog` at runtime, yet `x.someDogOnlyMethod()` won't compile. Reconcile those two facts in one sentence.
3. **`@Override` as a tripwire.** Suppose someone "fixes" the overload trap by writing `@Override String greet(String s)` in `Dog`. Does that change which overload `a.greet("hi")` calls? What would `@Override String greet(CharSequence s)` in `Dog` do?
4. **Field "shadowing" vs method overriding.** Add `String species = "Dog";` to `Dog` and read `((Animal) new Dog()).species` vs `new Dog().species`. Fields are resolved on the static type — show that this is the *same* early-binding rule as overload selection.

## How to verify

```
mvn -q -Dtest=DispatchPredictionsTest test
```

The test starts red (row 1 fails first). It is green once all five `EXPECTED_*` constants match Java's real behavior. Then run `DispatchLab.main` and compare its five lines to your corrected table.

<details><summary>Hint (open only if stuck)</summary>
Ask two different questions per call. For <code>speak()</code>: "which class's
body runs?" — answered at runtime from the object (<code>Dog</code>). For
<code>greet(...)</code>: "which overload's signature is selected?" — answered at
compile time from the <em>declared</em> type (<code>Animal</code>), then the most
specific applicable overload wins (<code>String</code> is more specific than
<code>Object</code>, and <code>null</code> is assignable to both, so
<code>greet(String)</code>). <code>var</code> and the cast are both
compile-time-typed too: <code>var x</code> gets the method's <em>declared</em>
return type (<code>Animal</code>), and <code>(Cat) a</code> only checks
type-compatibility at compile time — the real <code>Dog</code>-vs-<code>Cat</code>
mismatch is caught at runtime as a <code>ClassCastException</code>.
</details>
