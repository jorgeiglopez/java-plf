# Status Report — 2026-06-04

## My Opinion
**TL;DR:** A waste of tokens!

Very disappointed with the result. The exercises are difficult to understand (what to do), where do we need to put our changes. All the md files are verbose, confusing.

The only good part is the background section, which seems to be aimed in the right direction... but poorly executed.

## Cost

API-equivalent list price across the **entire project** (4 sessions + all
subagent/workflow debate agents, 2,587 assistant messages):

| Model | Cost |
|---|---:|
| Claude Opus 4.8 | $427.96 |
| Claude Sonnet 4.6 | $1.81 |
| **Total** | **$429.77** |

- **115.4M** total tokens, dominated by cache reads (**103.2M**, billed at
  $1.50/Mtok) — the long-running adversarial debate workflows re-read large
  shared context (authoring guide + index) on every agent turn.
- Output tokens: 1.1M. Cache writes: 8.7M.
- Most of the spend is the multi-agent debate authoring for s03–s06; the
  Sonnet line is the commit subagent.

> Figures are API list price. If billed under a Claude Code subscription, actual
> out-of-pocket cost is the plan fee, not this amount.

---

## Summary

Six of sixteen sections are fully authored and committed: **58 exercises**, each
with `PROBLEM.md` + `SOLUTION.md` + a Java scaffold and (mostly) a JUnit test.
Build is green; intentionally-red tests are all marked. Project-wide test time
caps are in place so a hung test can never eat the machine.

## Done

| Section | Sub-topics | Status |
|---|---:|---|
| s01 core_language | 8 | ✅ authored, comment-cleaned |
| s02 oop_class_design | 10 | ✅ authored, comment-cleaned |
| s03 generics | 7 | ✅ authored (4-agent adversarial debate) |
| s04 functional_streams | 8 | ✅ authored (debate, Opus) |
| s05 collections | 8 | ✅ authored (debate, Opus) |
| s06 concurrency | 17 | ✅ authored (debate, Opus) |

- **58** exercises with paired `PROBLEM.md` / `SOLUTION.md`.
- **58** test files; **44** carry the verbatim `// EXERCISE: starts red` marker.
- `s06_15` (StructuredTaskScope) and `s06_16` (ScopedValue) kept markdown-only —
  Java 21 preview APIs needing `--enable-preview`.
- **Test safety caps** (commit `c229a48`):
  - per-test JUnit timeout `20 s`, `SEPARATE_THREAD` mode;
  - surefire `forkedProcessTimeoutInSeconds = 180` as JVM-level hard-kill backstop.
- **Authoring guide** (`EXERCISE_AUTHORING.md`) updated with the A/B/C
  good-comment rule and the standardized red marker.

### Commits landed this round
```
c229a48 chore(test): cap test execution time
4beb55c feat(s06): concurrency & multithreading exercises
7678089 feat(s05): collections framework exercises
267d347 feat(s04): functional programming, lambdas & streams exercises
```
On `main`, **not pushed**.

## Pending

- **s07 jmm** (6) and **s08 jvm_internals** (8) — scaffolded, not authored. Deferred.
- **s09–s16** — scaffolded only.
- **s05_01 re-run** — s05_01 and s05_02 both authored the mutable-key-in-HashMap
  trap (duplicating s01_07); s05_01's real syllabus (collection hierarchy,
  ArrayList vs LinkedList) went uncovered.
- **Push** the 4 commits above to `origin/main`.

