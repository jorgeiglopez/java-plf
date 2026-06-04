# Reactive & Flow API — The buffer that back-pressures you anyway

**Concept:** Back-pressure means the *subscriber* dictates flow by granting demand — but unbounded demand does **not** mean unbounded memory; the publisher's bounded buffer re-imposes back-pressure regardless.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

`java.util.concurrent.Flow` (final since Java 9) is the JDK's reactive-streams
contract. A `Flow.Subscriber` pulls data by calling `subscription.request(n)`;
the publisher may only deliver up to the demand granted. `SubmissionPublisher`
is the JDK's reference publisher.

Here's the question that separates seniors from the hand-wavers. You have a
*fast* `SubmissionPublisher` and a *slow* consumer. Two subscribers:

- **Disciplined** — `request(1)` in `onSubscribe`, then one more `request(1)` per `onNext`.
- **Greedy** — `request(Long.MAX_VALUE)` in `onSubscribe`, never again.

Most people say: "the disciplined one back-pressures and `submit()` blocks; the
greedy one opts out of back-pressure, so its buffer balloons until OOM."

**That second half is wrong, and it's the whole exercise.** A
`SubmissionPublisher` has a *bounded* per-subscriber buffer (here we force it to
`BUFFER_CAPACITY = 4`). Once that buffer fills and the slow consumer isn't
draining it, `submit()` **blocks the producing thread** — for the greedy
subscriber too. Unbounded demand just moves the bottleneck from *demand* to the
*buffer*. Both stall. No OOM.

## Task

You're given `BackpressureLab.java`. The greedy subscriber
(`GatedSubscriber.greedy`) is fully built; the disciplined one is a stub.

1. **Predict first** (write it down before touching code): when the slow
   consumer is parked on item 0 and the producer keeps calling `submit()`, which
   of the two subscribers makes `submit()` block? Why?
2. Implement `GatedSubscriber.oneAtATime(...)`: it must grant exactly **one**
   item of demand in `onSubscribe` and exactly **one more** after each delivered
   `onNext`. (Look at how `greedy` is wired — you need the same machinery with
   different demand settings.)
3. Run `BackpressureLabTest`. Both tests must go green: each asserts the producer
   thread reaches `Thread.State.WAITING` inside `submit()`, that not all items
   were published, and that the consumer received exactly 1.
4. Confront your prediction. If you expected the greedy subscriber *not* to
   block, write one sentence explaining what actually bounded it.

## Constraints

- Java 21, no external libraries (`java.util.concurrent.Flow` only).
- Do **not** modify `BackpressureLabTest.java` or `BUFFER_CAPACITY`.
- Determinism is enforced by latches and thread-state polling — no `sleep`.

## Scale up (optional)

1. **request(0) is illegal.** Subscribe with `subscription.request(0)` in
   `onSubscribe`. What lands in `onError`, and which Reactive Streams rule is
   that? (Hint: §3.9.)
2. **The non-blocking escape hatch.** Replace `submit()` with
   `offer(item, timeout, onDrop)`. Show that the producer no longer parks — it
   *drops* instead. What does the `onDrop` handler's return value control?
3. **Demand accumulates.** With the disciplined subscriber, call `request(1)`
   *twice* before the first `onNext` returns. Does the publisher now deliver two
   items before parking? Explain why demand is additive, not a fresh ceiling.

## How to verify

```
mvn -q -Dtest=BackpressureLabTest test
```

<details><summary>Hint (open only if stuck)</summary>
`greedy` calls the private constructor with `(gate, Long.MAX_VALUE, false)`.
The disciplined version is the *same* constructor with a different initial
request and the re-request flag flipped on. The blocking happens for both
because `new SubmissionPublisher<>(executor, 4)` caps the buffer at 4 — once
4 items sit undelivered, `submit()` invokes `ForkJoinPool.managedBlock` and
parks the caller.
</details>
