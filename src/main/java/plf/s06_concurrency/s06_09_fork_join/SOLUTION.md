# Solution — Make the Recursion Actually Steal

## Ideal solution

Replace the two serial `compute()` calls with the canonical fork/compute/join
shape:

```java
int mid = lo + len / 2;
SumTask left = new SumTask(data, lo, mid, histogram);
SumTask right = new SumTask(data, mid, hi, histogram);

left.fork();                 // push left onto this worker's deque — stealable
long r = right.compute();    // do real work on THIS thread, here and now
long l = left.join();        // collect left; if not done, help by stealing
return l + r;
```

That is the only change. The threshold cutoff and everything else stay as-is.

## Why this works (and the gotcha)

`ForkJoinPool` parallelism comes from **work-stealing**: each worker owns a deque,
pushes forked tasks onto it, and pops from its own end; idle workers steal from the
*other* end of someone else's deque. Three shapes, three outcomes:

- **`left.compute(); right.compute();`** (the broken scaffold) — nothing is ever
  forked, so nothing is ever on a deque, so nothing is ever stealable. The whole
  tree runs on the single worker that picked up the root. The test proves it: the
  histogram has exactly **one** worker name, every run.

- **`left.fork(); long l = left.join(); long r = right.compute();`** — forking then
  immediately joining the *same* task is nearly as bad. `join()` finds `left` still
  on your own deque, pops it via `tryUnpush`, and runs it **inline**. You forked and
  un-forked in one breath; `right` doesn't even exist as a task yet, so there's
  nothing to steal *at this level*. (It can still fan out lopsidedly through deeper
  recursion, which is exactly why asserting on raw worker count for *this* shape is
  unreliable — don't.)

- **`left.fork(); long r = right.compute(); long l = left.join();`** (correct) —
  `left` sits on your deque, exposed, while you stay busy computing `right`. Another
  idle worker steals `left` and recurses into it in parallel. By the time you reach
  `left.join()`, it's often already done by someone else; if not, `join()` doesn't
  park — it runs other pending tasks (including stolen ones) until `left` completes.
  Work fans out across all four workers, deterministically.

The felt insight: **forking is publishing work for theft; the order of
fork/compute/join decides whether anyone ever gets to steal it.** `join()` is not
`Future.get()` — a fork/join worker blocked in `join()` keeps doing useful work
instead of going to sleep.

### Why the test is deterministic

The test uses a dedicated `new ForkJoinPool(4)` (not the common pool, whose
parallelism is `cores-1` and collapses to inline execution on a 1-core CI box). The
array is 4M longs with a 10k threshold, so the tree has hundreds of forkable nodes.
The serial scaffold provably touches one worker; the correct form provably touches
four. The submitting thread is plain `main` calling `pool.submit(task).join()` on
the returned `ForkJoinTask` — it blocks on the result without enrolling as a worker,
so `main` never appears in the histogram. (Had we used `pool.invoke(task)`, the
caller *would* be roped in as a worker and muddy the counts.)

## Common mistakes

- **`left.compute(); right.compute();`** → correct sum, zero parallelism. Splitting
  is not parallelizing.
- **`left.fork(); left.join(); right.compute();`** → `join()` pops `left` back and
  runs it inline; you serialized the sibling. Fork the one you'll join *last*.
- **Forking both then joining in the wrong order** (`fork both; join left; join
  right`) → join left before you've done any work yourself; the submitting worker
  blocks instead of computing. Always compute one half on the current thread.
- **No threshold** → forking down to single elements drowns you in task-creation
  overhead; framework bookkeeping dwarfs the additions.
- **`pool.invoke(task)` from the test thread** → the caller becomes a worker, so
  "did the submitter offload?" can no longer be asked.
- **Calling `task.compute()` from `main`** thinking it parallelizes → it just runs
  the whole thing on `main`. You must `submit`/`invoke` into a pool.

## Interviewer Q&A

**Q:** What does `fork()` actually do?
**A:** It pushes the task onto the current worker's work-stealing deque (LIFO for
the owner) and returns immediately. It schedules the task for asynchronous
execution; it does not run it. Only a worker thread can `fork` usefully — from an
external thread it submits to the pool.

**Q:** Why is `left.fork(); right.compute(); left.join();` better than
`left.fork(); right.fork(); left.join(); right.join();`?
**A:** In the first, the current thread does real work (`right`) instead of just
forking and waiting, so it's never idle. Forking both and joining both means the
current worker forks twice then blocks — fine, but it does no computation itself
between the forks and the joins, wasting that thread. The fork-one/compute-one
idiom keeps the submitting worker productive.

**Q:** How is `ForkJoinTask.join()` different from `Future.get()`?
**A:** `Future.get()` parks the calling thread until the result is ready. A
fork/join worker blocked in `join()` does **not** simply park — it runs other
pending/stolen tasks (it "helps") until the joined task completes. That's what keeps
all workers busy and is central to the framework.

**Q:** What is work-stealing?
**A:** Each worker has its own double-ended queue. It pushes/pops forked subtasks
from its own end (LIFO, cache-friendly). When a worker runs out of its own work it
**steals** from the *tail* of another worker's deque (FIFO of the oldest, usually
biggest, task). This balances load with minimal contention.

**Q:** Why does a sequential threshold matter, and how would you pick it?
**A:** Below some size, the cost of creating, forking, and joining a task exceeds
the cost of just summing inline, and you also flood the pool with millions of tiny
tasks. The threshold caps recursion depth. Pick it empirically — large enough that
per-task overhead is negligible, small enough to keep all cores fed (often tens of
thousands of elements for cheap per-element work).

**Q:** Why does `ForkJoinPool.commonPool()` sometimes appear to run everything on
one thread?
**A:** Its default parallelism is `Runtime.availableProcessors() - 1`. On a single
vCPU (common in CI containers/cgroups) that's 0/1, and tasks run inline on the
caller with no stealing. Tests asserting fan-out should use a dedicated pool with
explicit parallelism.

**Q:** A worker blocks inside `compute()` on a `Semaphore` (or any plain blocking
call). What happens to the pool's parallelism?
**A:** Nothing compensates. A raw `Semaphore.acquire()` doesn't go through
`ManagedBlocker`, so the pool has no idea the worker is stuck — it just parks,
holding its permit, and the effective parallelism drops. If a stolen sibling subtask
on that same starved set of workers needs another permit, you can self-deadlock with
parallelism-minus-one permits. To let the pool spawn a compensation thread, wrap the
blocking in `ForkJoinPool.ManagedBlocker`. Lesson: in fork/join, "one task = one
thread" is false — stealing means a single worker can be re-entered by an unrelated
subtask mid-`join`, breaking any assumption built on thread-affinity.

**Q:** You `fork()` a subtask, the sibling throws, you return early without
joining the forked one. The sum comes out wrong with no stack trace. Why?
**A:** A `ForkJoinTask` that completes exceptionally but is never `join()`ed (or is
`quietlyJoin()`ed) swallows its exception — no thread dies, nothing is logged, the
task just completes into the void and its contribution is lost or corrupt. `join()`
is what rethrows the wrapped exception. An unjoined exceptional task is silent
corruption, not a crash; always join everything you fork.
