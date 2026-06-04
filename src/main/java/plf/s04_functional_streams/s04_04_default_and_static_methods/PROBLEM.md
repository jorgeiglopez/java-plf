# Default & Static Interface Methods — Who Wins?

**Concept:** when a method comes from both a superclass and an interface default, the **class always wins** — and that rule has a precondition most seniors forget.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
Java 8 let interfaces carry method bodies (`default`) so APIs could evolve without
breaking implementors. That immediately raises a question every implementor must
answer: *when a method is defined in more than one place, who wins?* The naive
answer — "the default runs, that's the whole point" — is wrong in ways the
compiler will and won't tell you about. This exercise drills the resolution rule
and the `Interface.super` syntax you need to recall under pressure.

## Task
Three sources, two scaffold files, one rule.

1. **Predict `concreteCase()`.** In `WhoWins.java`, `MyService extends ConcreteBase
   implements Logger`. `ConcreteBase.format()` returns `"[parent]"`; the `Logger`
   default `format()` returns `"[default]"`. In `WhoWinsTest.predictWhoWins`,
   replace `"???"` with the string you believe `new MyService().format()` returns
   — *before* running anything.

2. **Resolve the diamond.** In `Diamond.java`, `Merged implements Left, Right`,
   both of which declare a `default String id()`. The class does not compile until
   `id()` is overridden. Implement the override so it returns `Left`'s value (`"L"`)
   by *delegating to the inherited default* — find the syntax that names a specific
   super-interface's default method. `diamondResolvesToLeft` then passes.

3. **Reason about the abstract case (no code).** Answer in `SOLUTION.md` terms:
   given
   ```java
   abstract class AbstractBase { public abstract String format(); }
   class AbstractYields extends AbstractBase implements Logger { }   // Logger has the default
   ```
   does `AbstractYields` compile? If not, what is the exact error and why doesn't
   the `Logger` default step in to satisfy the abstract method?

## Constraints
- Java 21, no external libraries.
- Do NOT edit the test file. Make it pass by editing the scaffold.
- Commit to your Task 1 prediction *before* running the test.

## How to verify
`mvn -q -Dtest=WhoWinsTest test` — both tests green when your prediction is
correct and the diamond override is written.

<details><summary>Hint (open only if stuck)</summary>
Task 1: a `default` method is a *fallback* — it only applies when no class in the
inheritance chain provides the method. A concrete superclass does provide it.
Task 2: the syntax is `<InterfaceName>.super.<method>()`. It is the only place
`super` is legal with a qualifier in front of it.
</details>
