# Enums — The knife in the room: extensible enums vs the JDK's enum containers

**Concept:** an `enum` that implements an interface (EJ38) gains cross-enum
extensibility, but that very interface is structurally barred from `EnumSet` /
`EnumMap` — the JDK's enum-optimized containers demand a concrete `Enum` type.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
You want an "operation" abstraction that spans more than one enum: a `Basic`
calculator and a `Sci` calculator, both `implements Op`, so callers can hold an
`Op` and not care which enum it came from. The interface-enum pattern (EJ38) is
the blessed way to get an *extensible* enum.

Here is the trap a cocky mid-level dev walks straight into: having built `Op`,
they reach for `EnumSet.of(...)` to collect operations, or `new EnumMap<Op,…>`
to key by operation — because "enums get the fast array-backed containers." Both
lines **fail to compile**. The interface that bought you extensibility just
locked you out of every enum-optimized container in the JDK. Same wound, two
blades: `Op` is not an `Enum`.

Can you predict the *exact* compiler errors — and, more importantly, label the
mechanism correctly? (Mislabel it "a runtime mono-type check" and a senior
interviewer eviscerates you.)

## Task

**Part 1 — Predict the compiler errors (commit before reading the solution).**
Given this design:

```java
interface Op { double apply(double a, double b); }

enum Basic implements Op {
    ADD { public double apply(double a, double b) { return a + b; } },
    SUB { public double apply(double a, double b) { return a - b; } };
}
enum Sci implements Op {
    LOG { public double apply(double a, double b) { return Math.log(a) / Math.log(b); } };
}
```

Write down, in `answers.txt`, your prediction for **each** of these two lines —
does it compile, and if not, what is the *category* of error (`javac`, Java 21)?

```java
var s = EnumSet.of(Basic.ADD, Sci.LOG);          // line A
var m = new EnumMap<Op, String>(Op.class);       // line B
```

For each, state: (1) compile or not, (2) one sentence naming the *precise*
mechanism — do not just say "they're different enums". Then check `SOLUTION.md`.

**Part 2 — Make the green code green (runnable).**
Open `Calculator.java`. The `Op` / `Basic` / `Sci` interface-enums are already
written and already work polymorphically. Implement the stub:

- `basicSymbols()` — return an `EnumMap<Basic, String>` mapping
  `ADD -> "+"`, `SUB -> "-"`, `MUL -> "*"`, `DIV -> "/"`. **Insert them in any
  scrambled order you like** — that is the point of the exercise.

Done when `CalculatorTest` is fully green, including the test that asserts
`symbols.values().toString()` equals `"[+, -, *, /]"`.

## Constraints
- Java 21, no external libraries beyond JUnit 5.
- Do **not** modify `CalculatorTest.java`.
- Do **not** run a compiler on the Part 1 snippets until you've committed an
  answer in `answers.txt`. (Those snippets are intentionally not shipped as a
  `.java` file — they don't compile.)

## Scale up (Optional)
**S1 — Why the EnumMap order is what it is.** Your `basicSymbols()` inserts in a
scrambled order yet `values()` comes out `[+, -, *, /]`. In one sentence, say
what an `EnumMap` is backed by and why insertion order is irrelevant.

**S2 — The ordinal persistence burn (EJ35).** A teammate persists priorities to
a config file as `ordinal()` ints. V1 is `enum Priority { LOW, MEDIUM, HIGH }`.
Later someone alphabetizes the constants to `enum Priority { HIGH, LOW, MEDIUM }`
and ships it. A row stored as `1` is read back with `Priority.values()[1]`.
Predict (write it in `answers.txt`): what did `1` mean before, what does it
decode to now, and does anything throw? Then give the one-line fix.

## How to verify
```
mvn -q -Dtest=CalculatorTest test
```
The class ships with one test marked `// EXERCISE: starts red`
(`basicSymbolsAreOrdinalOrdered`); the other two pass out of the box. Implement
`basicSymbols()` to turn the red one green. For Parts 1 and the scale-ups,
compare your `answers.txt` against `SOLUTION.md`.

<details><summary>Hint (open only if stuck)</summary>
Look at the bound on the type variable of both <code>EnumSet.of</code> and
<code>EnumMap</code>'s constructor: <code>E extends Enum&lt;E&gt;</code>. Now
ask whether <code>Op</code> — an interface — can ever satisfy that bound. For
line A, also notice that the two-arg <code>of</code> overload wants <em>both</em>
arguments to be the <em>same</em> <code>E</code>; <code>Basic</code> and
<code>Sci</code> are not the same type. For Part 2, remember an
<code>EnumMap</code> is keyed by <code>ordinal()</code>.
</details>
