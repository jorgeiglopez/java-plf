# Fork/Join Framework — Make the Recursion Actually Steal

**Concept:** the `fork(); compute(); join()` shape is what unlocks work-stealing — get the order wrong (or skip `fork` entirely) and your "parallel" sum runs on one thread.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

A `RecursiveTask` that splits an array in half and recurses *looks* parallel. But
splitting is not the same as parallelizing. The whole point of `ForkJoinPool` is
that an idle worker can **steal** a forked subtask off another worker's deque. If
your code never forks — or forks and immediately blocks on `join()` before doing
real work — there is nothing to steal, and the entire recursion collapses onto the
one worker that picked up the root.

`ParallelSum.SumTask.compute()` currently sums each half by calling
`left.compute(); right.compute();` directly. The sum is correct. The parallelism
is a lie. Can you see it, prove it, and fix it?

## Task

1. Run the tests. `sumIsCorrect` passes; `workFansOutAcrossWorkers` is **red** —
   every leaf recorded itself under a single worker name.
2. In `ParallelSum.java`, rewrite the marked lines in `compute()` so the two
   halves can run on different workers. Use the canonical shape:
   `fork` one child, `compute` the other on the current thread, then `join` the
   forked one.
3. Keep the `THRESHOLD` sequential cutoff — do **not** fork all the way down to
   single elements.
4. Both tests must pass: the sum stays `N·(N-1)/2`, and leaves now fan out across
   more than one worker, with the submitting `main` thread never running a leaf.

## Constraints

- Java 21, no external libraries.
- Do not modify `ParallelSumTest.java` and do not change `THRESHOLD` or the
  dedicated pool's parallelism.
- The fix is in the body of `compute()` only.

## How to verify

`mvn -q -Dtest=ParallelSumTest test`

<details><summary>Hint (open only if stuck)</summary>
The asymmetry is the lesson: `left.fork()` pushes left onto your deque so another
worker can grab it, then `right.compute()` keeps *you* busy doing real work, and
`left.join()` collects the result (stealing other tasks if it has to wait). If you
`fork` and then immediately `join` the same task, you pop it back and run it
inline — you never gave anyone a chance to steal it.
</details>
