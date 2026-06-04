# Solution — The buffer that back-pressures you anyway

## Ideal solution

`oneAtATime` is the same private constructor as `greedy`, with initial demand of
`1` and the re-request flag on:

```java
public static GatedSubscriber oneAtATime(CountDownLatch gate) {
    return new GatedSubscriber(gate, 1L, true);
}
```

That's all the production change requires. The constructor already stores
`initialRequest` and `reRequestOne`; `onSubscribe` does
`subscription.request(initialRequest)`, and `onNext` does
`if (reRequestOne) subscription.request(1);` after the gate releases.

The two subscribers therefore differ only in *when* and *how much* demand they
grant:

| Subscriber   | onSubscribe              | per onNext   | what bounds the producer |
|--------------|--------------------------|--------------|--------------------------|
| greedy       | `request(Long.MAX_VALUE)`| nothing      | the **buffer** (4 slots) |
| oneAtATime   | `request(1)`             | `request(1)` | the **demand** (1)       |

Run it and both tests pass: the producer thread reaches `WAITING` inside
`submit()`, `publishedAll` is `false`, and the consumer has `received() == 1`
(it's parked on item 0).

### Scale up solutions

1. **request(0) → onError.** `SubmissionPublisher` calls `onError` with
   `java.lang.IllegalArgumentException: non-positive subscription request`. Any
   `request(n)` with `n <= 0` is a signal failure under Reactive Streams **§3.9**;
   the subscription is then cancelled. Demand is a `long`; non-positive demand is
   meaningless, so the spec mandates an error rather than silently ignoring it.

2. **offer with drop.** `offer(item, timeout, onDrop)` does not park
   indefinitely. If a subscriber's buffer is full, it waits up to `timeout`; if
   still full, it invokes `onDrop.test(subscriber, item)`. Returning `false`
   drops the item (offer returns a negative number = lag/drops). Returning `true`
   asks the publisher to retry the drop once. So `offer` is the *non-blocking,
   lossy* escape hatch; `submit` is the *blocking, lossless* default. Same
   bounded buffer, opposite policy when it fills.

3. **Demand accumulates.** Two `request(1)` calls grant cumulative demand of 2 —
   the publisher delivers up to two items before it must wait for more demand.
   `request(n)` *adds* `n` to outstanding demand (capped at `Long.MAX_VALUE`); it
   is not "set the ceiling to n." That's why `request(Long.MAX_VALUE)` means
   "effectively unbounded forever," not "deliver MAX_VALUE items then stop."

## Why this works (and the gotcha)

Back-pressure in reactive streams is a *pull* contract: the publisher may emit at
most as many items as the subscriber has requested. So far so good — the
disciplined subscriber that requests one at a time obviously throttles the
producer, and `submit()` blocks once that single unit of demand is spent.

The trap is the greedy subscriber. `request(Long.MAX_VALUE)` removes demand as a
limiter — the subscriber has effectively said "send me everything." The tempting
conclusion is "so its buffer grows without bound until OOM." **It doesn't.**
`SubmissionPublisher` keeps a *bounded* per-subscriber buffer
(`Flow.defaultBufferSize()` = 256 by default; we pinned it to 4). When the slow
consumer can't drain it, the buffer fills, and `submit()` parks the producing
thread via `ForkJoinPool.managedBlock`. The producer blocks **exactly the same
way** as for the disciplined subscriber — just for a different reason: the buffer,
not the demand.

The one-sentence payload: *unbounded demand does not mean unbounded memory — the
bounded buffer re-imposes back-pressure anyway, so both subscribers stall the
producer with no OOM.* The senior insight is naming **which** limiter engages
(demand vs buffer), because they produce the identical observable symptom.

The test proves it without any timing race: the consumer is gated on a
`CountDownLatch`, so it is deterministically parked on item 0; the producer is
then guaranteed to exhaust the 4-slot buffer (greedy) or the single demand unit
(disciplined) and reach `Thread.State.WAITING`. Polling for `WAITING` with
`onSpinWait()` is correct because that state is reached and held until the test
releases the gate.

## Common mistakes

- **"Greedy subscriber OOMs."** No. The bounded buffer parks `submit()` long
  before memory is a concern. This is the headline gotcha.
- **"`request(MAX_VALUE)` opts out of back-pressure entirely."** It opts out of
  *demand-based* back-pressure; the producer still blocks on the buffer. The only
  true opt-out is an *unbounded* buffer plus unbounded demand — and
  `SubmissionPublisher`'s buffer is never unbounded.
- **"The publisher automatically slows down to match the consumer."** There is no
  magic rate-matching. The publisher buffers, then either *blocks* (`submit`) or
  *drops* (`offer`). Those are the only two mechanisms.
- **"`request(n)` sets the demand to n."** It *adds* n. Calling `request(1)`
  three times means three items may flow, not one.
- **Calling `request(0)` to "pause."** That is a §3.9 violation → `onError` +
  cancellation, not a pause.

## Interviewer Q&A

**Q:** What does back-pressure mean in reactive streams?
**A:** The subscriber controls the rate by granting demand via
`Subscription.request(n)`; the publisher may deliver at most the outstanding
demand. Flow is pull-driven, not push-driven.

**Q:** A subscriber calls `request(Long.MAX_VALUE)` against a slow consumer and a
fast `SubmissionPublisher`. Does it OOM?
**A:** No. `SubmissionPublisher` has a bounded per-subscriber buffer
(`defaultBufferSize()` = 256 by default). Once it fills, `submit()` blocks the
producing thread. Unbounded demand shifts the bottleneck from demand to the
buffer; it does not remove the bound.

**Q:** So what's the observable difference between the greedy and the disciplined
subscriber here?
**A:** None in symptom — both make `submit()` block. The difference is the cause:
disciplined blocks because demand is exhausted (1 unit), greedy blocks because the
4-slot buffer is full. Same stall, different limiter.

**Q:** How does `submit()` actually block, and what thread does it block?
**A:** It blocks the *calling* (producer) thread, parking it via
`ForkJoinPool.managedBlock` / `LockSupport.park` until buffer space frees up. The
consumer runs on the publisher's executor, so it is a different thread.

**Q:** What's the non-blocking alternative, and what's its cost?
**A:** `offer(item, timeout, onDrop)`. It waits up to `timeout` then *drops* the
item, invoking the `onDrop` handler. You trade blocking for lossiness — useful
when staleness is acceptable but stalling the producer is not.

**Q:** What happens on `request(0)` or `request(-1)`?
**A:** A Reactive Streams §3.9 violation: the publisher signals `onError` with
`IllegalArgumentException` ("non-positive subscription request") and cancels the
subscription. It is not a way to pause.

**Q:** Is `request(n)` additive or absolute?
**A:** Additive. Outstanding demand accumulates (saturating at `Long.MAX_VALUE`).
`request(MAX_VALUE)` therefore means "effectively unbounded," not "exactly
MAX_VALUE items then stop."

**Q:** `onComplete`/`onError` vs `onNext` — can they race?
**A:** No. `SubmissionPublisher` serializes signals per subscriber: `onComplete`
and `onError` are terminal and are delivered after any in-flight `onNext`, never
concurrently with it. A correct subscriber must not call `request` after a
terminal signal.

**Q:** When would you reach for `SubmissionPublisher` versus a full library like
Reactor/RxJava?
**A:** `SubmissionPublisher` is a minimal JDK building block — good for adapting
to the `Flow` SPI or simple fan-out. For rich operator chains (map/flatMap/retry/
backpressure strategies, schedulers), you'd use a reactive library; the JDK
deliberately ships only the interfaces plus this one reference publisher.
