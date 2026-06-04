# ThreadLocal — The customer who logged in as someone else

**Concept:** a `ThreadLocal` value lives on the *thread*, not the task. In a pool the same worker thread is reused, so a value left behind by one task leaks into the next — and `withInitial` does **not** save you.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

You're building a web backend. Each request runs on a pooled worker thread, and you stash
the authenticated user in a `ThreadLocal` so deeper layers can read "who is calling" without
threading the user id through every method:

```java
static final ThreadLocal<String> CURRENT_USER =
        ThreadLocal.withInitial(() -> "anonymous");
```

Login does `CURRENT_USER.set(userId)`. Anonymous endpoints never call `set` — they just read
`CURRENT_USER.get()` and trust the initializer to hand them `"anonymous"`.

The pool has **one** worker thread. Request A is Alice logging in. Request B is a later,
unauthenticated request that only reads `CURRENT_USER.get()`. What does B see?

Most engineers answer `"anonymous"` with full confidence — "the `withInitial` supplier resets
it for each access." Run it and find out.

## Task

Open `RequestContext.java`.

1. **Predict first.** Before running anything, write down what
   `handleAnonymousRead` returns after a prior `handleLogin(pool, "alice")` on the same
   single-thread pool. The test `leakedIdentity_demonstratesTheBug` encodes the real answer —
   read it and check your prediction.
2. **Explain the mechanism** in one sentence: why doesn't the `withInitial` supplier run for
   request B and produce `"anonymous"`?
3. **Fix it.** Implement `handleLoginSafely` so that after a login, a later anonymous read
   correctly sees `"anonymous"` again. Make `safeLogin_reArmsTheInitializer` go green. You may
   only edit `handleLoginSafely`.

## Constraints

- Java 21, no external libraries.
- Do **not** modify the test file, the other two `handle*` methods, or the `CURRENT_USER`
  declaration.
- The pool is `Executors.newSingleThreadExecutor()` — one thread, reused. That is the point;
  don't try to "fix" the bug by adding threads.

## How to verify

```
mvn -q -Dtest=RequestContextTest test
```

Both tests must pass: `leakedIdentity_demonstratesTheBug` documents the leak; your fix turns
`safeLogin_reArmsTheInitializer` green.

<details><summary>Hint (open only if stuck)</summary>
The initializer is a per-*thread* lazy seed, not a per-*task* reset. It runs only when a
thread accesses the ThreadLocal and the map has **no entry** for that key. Once a value is
present, the supplier never fires again. What single call removes the entry and re-arms the
seed — and where must it run so it happens even if the task throws?
</details>
