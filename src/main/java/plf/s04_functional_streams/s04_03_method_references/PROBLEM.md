# Method References — The Receiver Slot

**Concept:** a method reference is a *type-checked receiver-slot decision* — where does the receiver come from?
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

`s -> s.toUpperCase()` and `String::toUpperCase` look like cosmetic swaps. They are not.
Every method reference answers one question: **where does the receiver (the `this` the method runs on) come from?**

- **Static** ref → there is no receiver.
- **Bound** ref (`obj::m`) → the receiver is captured *now*, when the reference is created.
- **Unbound** ref (`Type::m`) → the receiver is supplied *later*, as argument zero.
- **Constructor** ref (`Type::new`) → the receiver is the object being built.

That single lens explains why `String::toUpperCase` is a `Function<String,String>` (arity 1, receiver is arg-zero)
while `s::toUpperCase` is a `Supplier<String>` (arity 0, receiver was captured). The receiver moved.

Crucially, the slot is **type-checked**, not name-matched: the same `Type::m` can fail to compile, resolve to a
static, or be *ambiguous* depending purely on the target functional type.

## Task

Open `ReceiverSlotLab.java`. Three beats, each a TODO. Predict in `ReceiverSlotLabTest.java` *first*, then implement.

1. **Arity shift.** Fill `unboundUpper()` to return `toUpperCase` wired as a `Function<String,String>`, and
   `boundUpper(captured)` to return it wired as a `Supplier<String>`. Same method name, two receiver slots.
2. **Eager capture (snapshot).** Fill `adderFor(target)` to return a `Consumer<String>` that adds to `target` via
   a method reference. In the test, predict which list receives `"x"` after the caller creates a *second* list.
3. **Array constructor ref.** Fill `toStringArray(stream)` using `stream.toArray(...)` with an array constructor
   reference, and `arrayMaker()` returning that reference as its own value. Predict the array.

Then answer the **bonus probes** below in `SOLUTION.md` (no code — reason it out, the answers are verified there).

## Constraints

- Java 21, no external libraries.
- Each stub is a method reference, *not* a lambda. Write `Type::m` / `obj::m` / `Type::new`, never `x -> ...`.
- Do **not** modify the test file's structure — only fill in your predictions.

## Bonus probes (predict compile vs. runtime — no lambdas, no IDE)

Given:

```java
class Box {
    String show(String s)            { return "instance:" + s; }
    static String show(String s, String t) { return "static:" + s + t; }
}
```

**P1.** `BiFunction<String,String,String> f = Box::show;` — compile or fail? If it compiles, what does
`f.apply("p","q")` return: the instance form or the static form? Why?

Now a second `Box`:

```java
class Box2 {
    String show(String s)        { return "i"; }
    static String show(Box2 b, String s) { return "s"; }
}
```

**P2.** `BiFunction<Box2,String,String> g = Box2::show;` — compile or fail? Name the mechanism.

And a constructor:

```java
class Node { Node(Node parent) {} }
```

**P3.** `Function<Node,Node> a = Node::new;` vs `Supplier<Node> b = Node::new;` — which compiles, which fails, and
what substring will the failing javac message contain?

**P4.** `int[] arr = Stream.of(1,2,3).toArray(int[]::new);` — compile or fail? Why does a *primitive* array
constructor ref not satisfy `Stream.toArray`?

## How to verify

`mvn -q -Dtest=ReceiverSlotLabTest test` — the test starts **red**. Fill your predictions and implement the three
stubs until it is green. Then check your bonus-probe answers against `SOLUTION.md`.

<details><summary>Hint (open only if stuck)</summary>
Beat 1: the unbound ref needs the receiver as a parameter, so the SAM must have one parameter of the receiver type.
Beat 2: `obj::m` snapshots the *value* of `obj` at reference-creation time, not the variable. Reassigning the
variable afterward does nothing. Beat 3: array length is an `int`, so the slot is `IntFunction<String[]>`.
</details>
