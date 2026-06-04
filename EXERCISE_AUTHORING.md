# Exercise Authoring Guide

> You are an exercise-writing agent. You arrive with **no prior context** — this
> file is your full brief. Read it top to bottom, then do the work for the **one
> section** you were assigned.

---

## 0. Your assignment (read first)

The orchestrator gives you a single **section id** at runtime, e.g.
`s06_concurrency`. You do **not** choose it, and you do **not** touch other
sections (other agents own those).

Your very first action:

1. Open **`src/main/java/plf/s00_index/index.md`** — the master index.
2. Find your section's heading. Under it is the list of **sub-topic packages**
   (leaf folders like `s06_03_liveness_hazards`) and a `Covers:` line for each.
3. Those `Covers:` bullets are your syllabus. **Author exercises for every
   sub-topic package in your section.**

---

## 1. Project TL;DR

- **java-plf** is a workbook for prepping a senior **Programming Language
  Fluency** interview in Java (the language + the JVM, not frameworks).
- **Java 21** (Amazon Corretto), **Maven** (`pom.xml`), **JUnit 5** already on
  the test classpath.
- Source root: `src/main/java/plf/`. Tests mirror under `src/test/java/plf/`.
- Topics are modeled as packages: `sNN_bigtopic/ sNN_MM_subtopic/`. There are 16
  big sections (`s01`–`s16`) plus `s00_index` (the index itself).
- Each leaf sub-topic package currently holds **only a `.gitkeep`** placeholder.
  Your exercises replace that emptiness.
- The single source of truth for what each package covers is
  `s00_index/index.md`.

---

## 2. What you deliver per sub-topic package

Work **inside the leaf package directory**, e.g.
`src/main/java/plf/s06_concurrency/s06_03_liveness_hazards/`:

- **`PROBLEM.md`** — the brief the student reads and works from. Optional: include one or more scale-ups
- **`SOLUTION.md`** — ideal solution(s) + explanation + **Interviewer Q&A**.
- **Optional scaffold/starter `.java` files and/or failing tests** when the
  exercise needs code to work on (see §5 for the hard rules).
- **Delete that package's `.gitkeep`** once you add real files.

Default cadence: **one `PROBLEM.md` + one `SOLUTION.md` per sub-topic package.**
A single `PROBLEM.md` may contain 1–3 small parts if the topic is gotcha-dense and their respective scale ups.
Bias toward **short and focused** over long and sprawling.

---

## 3. Design principles

- **Creative, didactic, hands-on.** Each exercise has exactly one clear learning
  objective and earns the student an "aha".
- **Aim at the gotchas.** Target the traps that bite people and the questions a
  real interviewer actually asks. Avoid trivia for its own sake.
- **Short and to the point.** 10–25 minutes of work. If it's longer, split it.
- **Scale scaffolding to difficulty.** Basic concept → a tiny predict/assert task
  with almost no scaffold. Hard concept (concurrency, JMM, GC, JVM internals) →
  richer scaffold: a harness, a partial implementation to finish, or a bug to
  diagnose. *The harder the topic, the more you hold their hand on plumbing so
  they spend their thinking on the concept.*
- **Deterministic and offline.** No network, no flaky timing, no external
  services. A test must pass/fail for the right reason every run.
- **Fail loudly.** Prefer exercises whose correctness is checked by assertions,
  unit tests, or an explicit "expected output" the student commits to first.
- **Minimal comments — `PROBLEM.md` does the talking (DRY).** All explanation,
  framing, and narrative live in `PROBLEM.md`, never in the `.java` scaffolds or
  tests. No "what this does" prose, no Javadoc essays, no comment that restates the
  code, narrates the trap, or duplicates `PROBLEM.md`/`SOLUTION.md`. A comment earns
  its place in code **only** if it does one of exactly three things:
    - **(A) Nudge** — points the student to exactly where they act: an orientation
      marker like `// <-- Task 1 here`, `// <-- change this`,
      `// <-- write your prediction`.
    - **(B) Critical info** — adds non-obvious information needed to understand the
      problem that is *not* already in `PROBLEM.md` (e.g. a constraint the code
      can't express). Rare; prefer `PROBLEM.md`.
    - **(C) Restriction** — marks what the student must **not** touch, e.g.
      `// ---- EDIT ONLY THESE LINES ----`, `// do not modify below`.

  **Less is always better — when in doubt, delete.** Don't multiply restriction
  banners; one per region. The single required marker is `// EXERCISE: starts red`
  on every intentionally-red test (§5). If code needs a paragraph to explain, that
  paragraph belongs in `PROBLEM.md`.

---

## 4. Exercise-type menu (a starting palette — research online and invent your own)

You are **not** limited to these. Mix and match; surprise the student.

- **Complete the feature** — scaffold with `TODO` stubs; student implements until
  the provided tests pass.
- **Fix the failing test** — plausible-looking code with a planted bug; a red test
  is provided; student fixes the *production* code (not the test).
- **Predict the compiler error** — a snippet that won't compile (e.g. an instance
  field in a `record`); student writes the exact `javac` message; the solution
  asserts it. *(Lives in markdown only — see §5.)*
- **Predict the output** — a small program; student writes expected stdout
  *before* running. Gold for autoboxing, `==` vs `equals`, the `Integer` cache,
  static-init order, ternary numeric promotion, `finally` return.
- **Boundary probe** — primitives/overflow: `Integer.MIN_VALUE` negation,
  `0.1 + 0.2`, `%` with negatives, `NaN` comparisons, `int` overflow, float→int
  narrowing. Assert the surprising-but-true value.
- **Implement from scratch** — build the data structure/algorithm yourself
  (your own `ArrayList`, open-addressing `HashMap`, LRU cache, ring buffer,
  bounded blocking queue); tests provided.
- **Spot the bug / code review** — find the race, the broken `equals`/`hashCode`,
  the resource leak, the `ConcurrentModificationException` waiting to happen.
- **Refactor** — tagged class → sealed hierarchy; loop → stream; finalizer →
  `Cleaner`; `synchronized` → `ReentrantLock`. Keep the provided tests green.
- **Repro & diagnose** — deterministically reproduce a deadlock / livelock /
  visibility bug / leak, then explain and fix it.
- **Contract probe** — deliberately break the `equals`/`hashCode`/`Comparable`
  contract and watch a `HashSet`/`TreeMap` misbehave; then repair it.
- **Fill-the-table** — which happens-before edges hold? big-O of each collection
  op? which GC for which goal? Student completes a table; solution is the key.
- **Micro-benchmark reasoning** — given a measurement (or a tiny JMH-style
  result), explain *why* (boxing, megamorphic call site, false sharing). Keep any
  runnable part tiny and deterministic.

---

## 5. Hard rules for code that ships in the build

Breaking these breaks the whole project's build for everyone. Follow exactly.

1. **Scaffold/starter `.java` files MUST COMPILE.** Stub unfinished work with
   `throw new UnsupportedOperationException("TODO")` (or a TODO comment + a
   harmless default) so `mvn compile` stays green.
2. **"Fix the failing test" tests MUST COMPILE but may FAIL.** That's their job.
   Mark each red test with the exact marker `// EXERCISE: starts red` — verbatim,
   don't reword it (reviewers and tooling grep for that literal string). No extra
   narrative file comment. Place them under `src/test/java/plf/...` mirroring the
   package path.
3. **Code that is *supposed* not to compile lives ONLY in markdown fences**
   inside `PROBLEM.md`/`SOLUTION.md`. Never commit a non-compiling `.java` file —
   it poisons the build. (So "predict the compiler error" exercises are
   markdown + a textual assertion, not a live source file.)
4. **Package declaration = folder path.** A `.java` file in
   `…/s06_concurrency/s06_03_liveness_hazards/` declares
   `package plf.s06_concurrency.s06_03_liveness_hazards;`.
5. **Solution code goes in `SOLUTION.md` as fenced blocks**, not as a second live
   `.java` file that would clash with the scaffold's class names.
6. **Java 21 features are fair game** (records, sealed types, pattern matching,
   switch expressions, virtual threads, etc.). No preview flags — stick to
   finalized Java 21 so `mvn` works out of the box.

Net effect: after your work, `mvn compile` is green; only the intentionally-red
exercise tests fail under `mvn test`, and only where you said so.

---

## 6. `PROBLEM.md` template

```markdown
# <Sub-topic> — <catchy exercise title>

**Concept:** <the one idea being drilled>
**Difficulty:** ★☆☆ basic | ★★☆ intermediate | ★★★ senior
**Est. time:** ~15 min

## Background
2–4 sentences. Frame the gotcha as a question the student should be itching to
answer.

## Task
Concrete, numbered steps. Name the scaffold files to edit and/or the tests to
turn green. Be unambiguous about "done".

## Constraints
e.g. Java 21, no external libraries, must be O(1) extra space, do NOT modify the
test file.

## Scale up (Optional)
One to five different tasks aiming to explore different areas of the topic. Normally they add up on difficulty

## How to verify
The exact command, e.g. `mvn -q -Dtest=LruCacheTest test`, or
"write your predicted output in answer.txt, then run X and compare."

<details><summary>Hint (open only if stuck)</summary>
A nudge toward the mechanism — never the full answer.
</details>
```

---

## 7. `SOLUTION.md` template

```markdown
# Solution — <exercise title>

## Ideal solution
Complete, working code in a fenced block. If several approaches are valid, show
each with its trade-off (e.g. `synchronized` vs `ReentrantLock`, iterative vs
recursive).

### Scale up solutions (Optional)
Explain the solution for each task

## Why this works (and the gotcha)
Explain the mechanism. Name the trap explicitly and say why the naive attempt
fails. This is the teaching payload — make it land.

## Common mistakes
- <wrong-but-tempting move> → <what actually happens>
- ...

## Interviewer Q&A
A graded ladder of questions an interviewer would ask off this exercise, warm-up
→ senior follow-up. 5–8 pairs.

**Q:** ...
**A:** ...

**Q:** ...
**A:** ...
```

The **Interviewer Q&A** section is **mandatory** in every `SOLUTION.md`.

---

## 8. Worked mini-example (sets the bar)

For `s02_oop_class_design/s02_08_records_and_sealed`, a basic predict-the-error
exercise:

**`PROBLEM.md`**
```markdown
# Records & Sealed Types — A record that won't compile

**Concept:** records have *no* instance fields beyond their components.
**Difficulty:** ★☆☆ basic
**Est. time:** ~8 min

## Background
Records look like classes, so it's tempting to add a stray field. The compiler
disagrees — loudly. Can you predict its exact words?

## Task
1. Consider this declaration:
   ```java
   public record Point(int x, int y) {
       private int distanceCache;   // <-- added field
   }
   ```
2. Without compiling, write the exact `javac` error message (Java 21) into
   `expected_error.txt`.
3. Then explain in one line *why* records forbid this.

## Constraints
Do not run a compiler until after you've committed to an answer.

## How to verify
Compare your line against `SOLUTION.md`.
```

**`SOLUTION.md`**
```markdown
# Solution — A record that won't compile

## Ideal solution
javac (Java 21) reports:
`error: field declaration must be static`
pointing at `private int distanceCache;`. Making it `private static int
distanceCache;` compiles, because the restriction is only on *instance* fields.

## Why this works (and the gotcha)
A record's state is exactly its components; the compiler derives the canonical
constructor, accessors, `equals`, `hashCode`, and `toString` from them. An extra
instance field would let an instance hold state outside that contract, so it's
banned. `static` fields are fine — they belong to the class, not the instance.

## Common mistakes
- Assuming records are "just classes" and can carry caches/flags as fields.
- Trying to "fix" it by adding a getter — the error is about the field, not access.

## Interviewer Q&A
**Q:** Why can't a record have instance fields beyond its components?
**A:** Its identity/contract (equals/hashCode/toString/accessors) is defined
purely by the components; extra instance state would break that guarantee.

**Q:** What *can* you add to a record body?
**A:** Static fields, static/instance methods, compact or custom canonical
constructors, additional constructors, and nested types.

**Q:** How would you cache a derived value then?
**A:** Compute lazily in an accessor, or precompute in the compact constructor
and store it as a component — not as a hidden mutable field.

**Q:** Are records immutable and final?
**A:** Components are final and the class is implicitly final; but a component of
a mutable type can still be mutated, so "shallowly immutable".
```

Note how scaffolding is minimal here (basic topic). For
`s06_03_liveness_hazards` you'd instead ship a runnable `Deadlock.java` harness
plus a test, because the topic is hard.

---

## 9. Before you finish — checklist

- [ ] Read your section in `s00_index/index.md`; covered **every** sub-topic
      package in it.
- [ ] Each leaf package has `PROBLEM.md` + `SOLUTION.md`; `.gitkeep` removed
      where you added files.
- [ ] Every `SOLUTION.md` has a populated **Interviewer Q&A** section.
- [ ] Any committed `.java` scaffold **compiles** (`mvn -q compile` green).
- [ ] Intentionally-red tests compile and are clearly marked; non-compiling
      snippets live in markdown only.
- [ ] Exercises are short, deterministic, offline, and gotcha-focused.
- [ ] Difficulty and scaffolding scale with the topic.
- [ ] `.java`/test files carry **only** comments that (A) nudge to where the
      student acts, (B) add critical non-obvious info, or (C) mark what's off-limits
      — no narrative, no code-restating prose, nothing duplicating `PROBLEM.md`.
      Less is better. Plus the required `// EXERCISE: starts red` marker (verbatim)
      on every red test.

Stay in your assigned section. If you spot something off elsewhere, note it in one
sentence and move on.
