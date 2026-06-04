# Synchronizers — The Relay Race

**Concept:** every synchronizer is defined by its *release rule* — who unblocks whom, and when. Tell the five apart by their invariants, not their vocabulary.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
Five classes from `java.util.concurrent` all "coordinate threads": `CountDownLatch`,
`CyclicBarrier`, `Semaphore`, `Phaser`, `Exchanger`. People who only memorize names
guess wrong the moment the code probes an *edge*: a latch counted past zero, a barrier
action mutating shared non-`volatile` state, a `Semaphore` released more times than it
was acquired, a `Phaser` that has already terminated, an `Exchanger` with nobody on the
other end. This is one program that runs each construct through exactly such an edge —
can you predict precisely what it prints?

## Task
1. Read `RelayRace.java`. It runs all five synchronizers and appends to an ordered
   `List<String>` (no stdout races — the output is deterministic).
2. **Before running anything**, fill in `RelayRacePredictions.predicted()` with the
   exact transcript you expect — one `String` per entry, in order.
3. Run the test. It compares your prediction against the real transcript. Make it green
   by predicting correctly (do **not** change `RelayRace.java` to match a wrong guess).
4. For each line, be ready to say *which invariant* produced it.

The five edges, one per construct:
- **CountDownLatch** — the gun is fired twice on a count of 1. What is `getCount()`, and does the second `await()` block?
- **CyclicBarrier** — three runners each add `10` to a **non-`volatile`** `int total` per lap; the barrier action prints `total`. Which thread runs the action, when, and is the read of `total` even safe without `volatile`?
- **Semaphore** — start with 2 permits, `acquire()` once, then `release()` three times. How many permits now?
- **Phaser** — `onAdvance` returns `true`; a party arrives, the phaser terminates, then `arriveAndAwaitAdvance()` is called on the **terminated** phaser. Does it block? Throw? Return what?
- **Exchanger** — a single thread calls `exchange(x, 50, MILLISECONDS)` with no partner. What happens?

## Constraints
- Java 21, no external libraries.
- Do **not** modify `RelayRace.java` or the test. Only edit `RelayRacePredictions.java`.
- Commit to your prediction *before* running.

## How to verify
`mvn -q -Dtest=RelayRaceTest test` — green when your predicted transcript equals the
actual one.

<details><summary>Hint (open only if stuck)</summary>
A latch never re-arms and clamps at zero. A barrier action runs once on the last
arriver, *before* anyone is released, and `await()` establishes happens-before — so the
action sees every party's pre-`await` writes with no `volatile`. A `Semaphore` is a bare
counter; `release()` needs no prior `acquire()` and can push permits *above* the initial
count. A terminated `Phaser` makes its await/advance calls return *immediately* with a
*negative* phase. An `Exchanger` alone times out.
</details>
