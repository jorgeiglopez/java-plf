# Solution — Does `final` rescue double-checked locking?

## Ideal solution

### Part 1 — `atomicIncrement`

```java
private final AtomicInteger atomicCount = new AtomicInteger();

public int atomicIncrement() {
    return atomicCount.incrementAndGet();
}
```

`incrementAndGet()` is a single atomic read-modify-write, implemented as a CAS retry loop (`VarHandle.compareAndSet` / `Unsafe.compareAndSwapInt`): read the current value, compute +1, CAS it in; if another thread won the race, the CAS fails and the loop retries. No update is ever lost, so 8 × 100 000 = exactly **800 000**.

Contrast with `volatileIncrement()`: `volatileCount++` compiles to *read `volatileCount`* → *add 1* → *write `volatileCount`*. `volatile` makes each of those three sub-actions visible immediately, but two threads can both read the same value, both add 1, and both write back the same result — one increment is lost. `volatile` buys **visibility**, never **atomicity** of a compound action. That is the word *publish* earned: publication ≠ mutual exclusion.

### Part 2 — DCL analysis

**1. Can the fast-path reader see `x == 0`? YES.** Under the JMM, with `instance` non-`volatile`, the read at (1) and the write at (2) form a **data race**. There is no happens-before edge between the writing thread's `instance = new Resource()` and the reading thread's `return instance`. So the reader may observe the *reference* write (it sees non-null) while the *constructor's field writes* are not yet ordered before it — it can read `x == 0`, the default. The compiler/CPU is free to publish the reference before the field stores complete (the classic allocate / publish-ref / run-constructor reordering).

**2. What `final` guarantees (JLS 17.5).** At the end of `Resource`'s constructor a **freeze** action occurs on each final field. The guarantee: any thread that reads the object reference **through a correctly published / safe-publication path** is guaranteed to see the final field's constructed value (`x == 42`). The **one precondition** is *correct publication* — the reference must not reach the reader via a data race. The fast-path read at (1) reads `instance` through exactly such a data race, so it does **not** meet the precondition. Therefore `final` does **not** rescue DCL: `instance != null` together with `x == 0` is legal. (Do not soften this to "almost works" — it does not work here, full stop.)

**3. `data` — two separate questions.**
- The `final` reference `data` is covered by the same freeze as `x`: safe **only** via correct publication, which the racy read isn't. So even the reference can read as `null`.
- `data[0]` is a write to the *array contents after* the array reference was assigned. The final-field freeze covers final fields reachable at the freeze, and per JSR-133 it does extend to the *contents reachable through final fields at the end of the constructor* — but, again, **only** under safe publication. Through the data race you get **no** guarantee for the reference *or* the element. Two layers of cargo cult: "final array means the elements are safe" is wrong twice over.

**4. The two edges.**
- **`volatile` orders the reference publication.** Writing a `volatile instance` is a *release*; reading it is an *acquire*. The release/acquire edge orders *everything the writer did before the write* (the constructor's field stores) before *everything the reader does after the read*. This is the edge DCL is missing.
- **`final` orders the constructor writes** relative to the freeze — but the freeze only yields a happens-before to a reader on the *safe-publication* path.

DCL needs `volatile` **regardless of `final`** because the bug is **publication of the reference through a race**, and only `volatile` (or some other happens-before edge) cures that. `final` addresses a different edge (constructor writes ↔ freeze) and is voided by the same race. Fix:

```java
private static volatile Resource instance;   // the one-word fix
```

**5. Closing gotcha — `AtomicReference`.** Yes, it fixes DCL:

```java
private static final AtomicReference<Resource> ref = new AtomicReference<>();
static Resource get() {
    Resource r = ref.get();
    if (r == null) {
        synchronized (Holder.class) {
            r = ref.get();
            if (r == null) { r = new Resource(); ref.set(r); }
        }
    }
    return r;
}
```

But it is **not** the `compareAndSet`/CAS that saves you. `AtomicReference`'s value is `volatile`-backed: `ref.set` is a volatile-write (release) and `ref.get` is a volatile-read (acquire). It is that **release/acquire on the reference write** — the same edge plain `volatile` gives — that orders the constructor stores before publication. CAS is a decoy. Anyone who answers "atomics fix DCL because the CAS is atomic" failed the question: a single CAS makes the *swap* atomic, but atomicity of the swap is not what DCL was missing — **safe publication of the reference** was.

## Why this works (and the gotcha)

The whole exercise pivots on one confusion: **atomicity vs. publication.**
- The warm-up proves `volatile` ≠ atomic (Part 1). That kills "make it volatile and `count++` is fine."
- DCL proves the singleton bug was never about atomicity at all — it is about **ordering of the writes that precede the reference publication**, i.e. safe publication. `final` *looks* like it should help (it guarantees the constructed value) but its guarantee is gated on safe publication, which the racy fast-path read lacks. So `final` saves nothing here, and `volatile` is mandatory.

The trap is the cargo cult "final fixes everything." It guarantees the constructed value of the field, not the safe publication of the *reference*. Two different edges; the data race voids both.

## Common mistakes
- "Add `volatile` for visibility" hand-waving → the real reason is **ordering**: it orders the constructor's writes before the reference publication (release/acquire), preventing a reader from seeing a non-null reference to a half-built object.
- "`x` is `final`, so DCL is safe" → no; the freeze guarantee requires safe publication, which a racy non-volatile read is not. `x == 0` is legal.
- "`final int[] data`, so `data[0]` is safe" → no; without safe publication you get nothing for the reference or the element.
- "`volatile count++` is atomic now" → no; it stays a lost-update race. Only `getAndIncrement`/CAS is atomic.
- "AtomicReference fixes DCL because of the CAS" → no; it fixes it because the field is `volatile`-backed (release/acquire on the reference write). CAS is irrelevant to the publication bug.
- Conflating `LongAdder` with `AtomicInteger`: `LongAdder` is a striped contended counter with a weakly-consistent `sum()` — great for hot statistics counters, **wrong** for a monotonic id generator that needs each `incrementAndGet` to return a unique sequential value.

## Interviewer Q&A

**Q:** Does `volatile` make `count++` thread-safe?
**A:** No. `volatile` guarantees visibility and ordering of each read and each write, but `count++` is a compound read-modify-write; two threads can interleave and lose an update. You need an atomic op (`AtomicInteger.incrementAndGet`, a CAS loop) or a lock.

**Q:** Why is non-volatile double-checked locking broken?
**A:** The fast-path read of the non-volatile reference races with its write. With no happens-before edge, a reader can see the reference become non-null while the constructor's field writes are not yet ordered before it — observing a partially constructed object (fields at default values).

**Q:** The field is `final` — doesn't the final-field freeze save it?
**A:** No. JLS 17.5's freeze guarantees the constructed value of a final field only to a thread that reads the reference via a *correctly published* path. The racy non-volatile read is not such a path, so `instance != null` with `x == 0` is legal. `final` addresses constructor-writes-before-freeze; it does nothing about publishing the *reference* safely.

**Q:** So name the two happens-before edges precisely.
**A:** (1) `volatile` write/read of the reference = release/acquire, ordering everything the writer did (constructor stores) before everything the reader does after the read — this is the edge DCL lacks. (2) `final`-field freeze, ordering the constructor's writes before the freeze, but yielding a happens-before only under safe publication. DCL needs edge (1); `final` cannot substitute for it.

**Q:** What's the minimal fix?
**A:** Make the field `private static volatile Resource instance;`. One word. The volatile write supplies the release that orders the constructor stores before the reference publication, and the volatile read supplies the acquire.

**Q:** Does swapping to `AtomicReference` fix it, and why?
**A:** Yes, but not because of `compareAndSet`. The atomic's value is `volatile`-backed: `set` is a release-write and `get` is an acquire-read of the reference, giving the same publication edge as plain `volatile`. The CAS atomicity is irrelevant to the publication bug — it's a decoy.

**Q:** When would you prefer `LongAdder` over `AtomicInteger`?
**A:** Under high write contention on a counter you only read occasionally (metrics/stats). `LongAdder` stripes updates across per-thread cells to cut CAS contention, and `sum()` aggregates them — weakly consistent, no single atomic value. It's wrong when you need each increment to return a unique monotonic value (e.g. an id generator); use `AtomicInteger`/`AtomicLong` there.

**Q:** `AtomicInteger.incrementAndGet` is "lock-free." What does that buy you, and what's the cost?
**A:** Lock-free = no thread holding a lock can block others indefinitely; system-wide progress is guaranteed via CAS retries, so no deadlock and better behavior under contention than a lock for short ops. The cost is the retry loop wastes work under heavy contention (and can starve an individual thread — it's lock-free, not wait-free); at extreme contention striping (`LongAdder`) or a different design can win.
