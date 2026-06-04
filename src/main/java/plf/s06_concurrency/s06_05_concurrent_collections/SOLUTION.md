# Solution — Where is the atomic boundary?

## Ideal solution

### Part A — collapse read-modify-write into one atomic op

```java
public void record(String word) {
    counts.merge(word, 1, Integer::sum);
}
```

`merge` performs the read, the remap, and the write as **one** atomic map operation (the bin is locked for the duration). The check-then-act window vanishes, so no update is lost. Equivalent alternatives:

```java
counts.compute(word, (k, v) -> v == null ? 1 : v + 1);
// or, if the value were an AtomicInteger:
counts.computeIfAbsent(word, k -> new AtomicInteger()).incrementAndGet();
```

`putIfAbsent` alone does **not** fix it — it only closes the *insert* race, not the increment race. The increment is still a lost-update unless the whole read-modify-write is atomic.

### Part B — memoize without re-entering the bin lock

```java
public long fibSafe(int n) {
    Long cached = memo.get(new Key(n));
    if (cached != null) return cached;

    long prev = 0, curr = 1;          // fib(0), fib(1)
    if (n == 0) curr = 0;
    for (int i = 2; i <= n; i++) {
        long next = prev + curr;
        prev = curr;
        curr = next;
    }
    long result = (n < 2) ? n : curr;
    memo.putIfAbsent(new Key(n), result);
    return result;
}
```

The recurrence is computed **bottom-up, in local variables**. The map is touched only by a plain `get` (lookup) and a plain `putIfAbsent` (store) — never *inside* a `computeIfAbsent` lambda. The atomic unit is entered, used, and released atomically; nothing spans it.

### Scale up solutions

1. **Natural key, looped:** With a normal `hashCode`, `fib(k-1)` and `fib(k-2)` usually map to *different* bins than `n`, so the re-entrant `computeIfAbsent` often completes silently — sometimes with a correct value, sometimes a corrupted count (a half-built node observed by the re-entrant call), and occasionally the `IllegalStateException` when two recursive calls collide in one bin. **"Sometimes correct" is the worst outcome:** a green run convinces you the code is fine and you ship a contract violation. You cannot test your way out of it — the Javadoc flatly forbids the mapping function from modifying the map, and the only safe move is to honor the contract, not the observed run. The forced-collision `Key` (fixed `hashCode == 0`) makes the violation *deterministic* so it can be asserted.

2. **Iterators:**
   - `ConcurrentHashMap` iterator: **weakly consistent** — never throws `ConcurrentModificationException`, traverses elements as they existed at/after iterator creation, *may or may not* reflect writes made during iteration. `iterator.remove()` works; it reflects into the map.
   - `CopyOnWriteArrayList` iterator: a **frozen snapshot** of the array at creation — never throws CME, never sees later writes, and `iterator.set/add/remove` throw `UnsupportedOperationException` (the snapshot is immutable). Writes to the list copy the whole backing array — cheap reads, expensive writes; good only for read-mostly data.

3. **`size()`:** `ConcurrentHashMap` tracks count across striped counter cells (`sumCount()` over `baseCount` + `CounterCell[]`) to avoid a global lock. Under concurrent mutation the sum is **approximate** — it can be stale the instant you read it. It's fine for metrics/heuristics, never as the basis for an invariant (`if (map.size() == N) ...` is a race).

## Why this works (and the gotcha)

One ruler, two readings:

- **Across threads (Part A):** `get` + `put` are each atomic, but the *sequence* is not. Two threads read the same value, both compute `v+1`, both write it — one increment is lost. The fix is not "more locking"; it's choosing a primitive (`merge`/`compute`) whose **atomic unit matches the invariant** (read-modify-write as a unit).

- **Within one thread (Part B):** `computeIfAbsent` holds the bin's `synchronized` monitor across the mapping function. Re-entering the *same* map from inside that lambda is **mutation during mutation**: forbidden by contract. With colliding keys it deterministically throws `IllegalStateException: Recursive update`; with normal keys it's non-deterministic (silent corruption, accidental success, or the throw). The lesson isn't "memorize this footgun" — it's that the lambda runs *inside* the atomic unit, so anything that touches the same atomic unit from within is illegal. Same ruler, measured from the inside.

The trap: the broken code *reads as obviously correct* ("thread-safe map, clean memoization") and *sometimes runs clean*. Senior judgment is trusting the documented contract over a green test run.

## Common mistakes

- **`putIfAbsent` for the counter** → only the insert is atomic; the increment still loses updates.
- **Guarding `record` with `synchronized`** → "works" but throws away the whole point of a concurrent collection (you've serialized every write and reintroduced contention).
- **Reading the clean `fib(20)` once, seeing `6765`, and concluding it's fine** → the natural-key version is non-deterministic; a passing run proves nothing.
- **"Just catch the `IllegalStateException` and retry"** → you're catching a symptom of a contract violation; the corrupted-value case won't even throw.
- **Using `size()`/`isEmpty()` mid-write to drive logic** → both are approximate under concurrent mutation.
- **Expecting a `ConcurrentHashMap` iterator to throw CME** → it never does; it's weakly consistent, not fail-fast.

## Interviewer Q&A

**Q:** Is `if (!map.containsKey(k)) map.put(k, v)` thread-safe on a `ConcurrentHashMap`?
**A:** No. Each call is atomic, but the check-then-act sequence is a race — another thread can insert between the `containsKey` and the `put`. Use `putIfAbsent` / `computeIfAbsent`.

**Q:** Why does the `get`+`put` counter lose updates even though the map is thread-safe?
**A:** The read-modify-write spans two separate atomic operations. Two threads read the same current value, both increment locally, and both write back — one increment is overwritten. `merge`/`compute` make the whole RMW a single atomic bin operation.

**Q:** What's the difference between `merge`, `compute`, and `computeIfAbsent` here?
**A:** All lock the bin and run the function atomically. `computeIfAbsent` runs only when the key is absent (good for lazy init); `compute` always runs and can remove on null; `merge` is the clean idiom for "combine old value with a new contribution," e.g. `merge(k, 1, Integer::sum)`.

**Q:** Why does the recursive `computeIfAbsent` Fibonacci throw `IllegalStateException: Recursive update`?
**A:** `computeIfAbsent` holds the bin's `synchronized` monitor across the mapping function. The lambda re-enters the same map; with colliding keys the re-entrant call lands on a bin already mid-computation and the implementation detects the recursive update and throws. The Javadoc explicitly forbids the mapping function from modifying the map.

**Q:** With *normal* (non-colliding) keys, does it still throw?
**A:** Not reliably. The recursive calls often hit different bins, so you may get silent completion (sometimes a correct result, sometimes a corrupted one) and only occasionally the exception. It's unspecified behavior — that non-determinism is exactly why you trust the contract, not the run.

**Q:** Is `ConcurrentHashMap.size()` reliable under concurrent writes?
**A:** No. It sums striped counter cells without a global lock, so it's approximate and can be stale immediately. Use it for metrics, never as an invariant or loop bound during mutation.

**Q:** Contrast a `ConcurrentHashMap` iterator with a `CopyOnWriteArrayList` iterator.
**A:** CHM's is weakly consistent: never throws CME, may or may not reflect concurrent writes. COWAL's is a frozen snapshot: never throws CME, never reflects later writes, and rejects `set`/`add`/`remove` with `UnsupportedOperationException`. COWAL is for read-mostly data because every write copies the backing array.

**Q:** How does `ConcurrentHashMap` achieve lock-free reads but safe writes?
**A:** Reads traverse `volatile` bin/node references with no locking (visibility via `volatile` + happens-before). Writes use CAS to install the first node in an empty bin, and `synchronized` on the bin's head node only when the bin is non-empty — locking is per-bin, so contention is limited to colliding keys, not the whole map.
