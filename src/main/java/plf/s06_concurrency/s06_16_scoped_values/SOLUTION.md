# Solution — Where Does `get()` Read From?

## Ideal solution

```text
(A) bob
(B) bob
(C) NoSuchElementException — thrown in the CHILD virtual thread, printed to that
    thread's stderr by the default uncaught-exception handler. outer() does NOT
    catch it; the .join() on the main thread returns normally.
```

One line per probe:

- **(A) `bob`** — `later.run()` executes on the **same** thread that is currently
  inside the `"bob"` extent. `USER.get()` resolves against that thread's active
  binding, which is `bob`. This proves the lambda did **not** lexically capture
  `"alice"` (the value live when `later` was *created*); it reads the *running*
  thread's *current* binding.
- **(B) `bob`** — `StructuredTaskScope.fork` snapshots the *immutable* scoped-value
  binding set **at fork time** and installs it in the forked thread. The fork
  happens inside the `"bob"` extent, so the child inherits `USER = bob`. This is the
  one path that *does* propagate the binding to another thread.
- **(C) `NoSuchElementException`** — `Thread.ofVirtual().start(...)` is an **ad-hoc**
  thread. It does **not** inherit scoped-value bindings (only `fork` does). The child
  runs `later` with an *empty* binding set, so `USER.get()` finds no binding and
  throws `NoSuchElementException` — note: **not** `null` (that's `ThreadLocal`'s
  behaviour). The throw happens **on the child thread**, inside its run loop; the
  default handler prints it to stderr. `start(later).join()` on the parent returns
  normally — `outer()` never sees the exception.

### The contrast that makes it land

```java
// ThreadLocal — a MUTABLE SLOT, persists until removed, ad-hoc child *can* inherit
// if it's an InheritableThreadLocal, and get() returns null/initialValue when unset.
static final ThreadLocal<String> U = new ThreadLocal<>();
U.set("bob");
System.out.println(U.get());   // bob — and stays bob until U.remove()

// ScopedValue — an IMMUTABLE BINDING on a FRAME; reverts on run() return,
// get() THROWS when unbound, only fork() propagates.
static final ScopedValue<String> V = ScopedValue.newInstance();
ScopedValue.where(V, "bob").run(() -> System.out.println(V.get())); // bob
// here, V is unbound again; V.get() would throw NoSuchElementException
```

## Why this works (and the gotcha)

The single sentence: **`ScopedValue.get()` reads the running thread's active binding
— not the lambda's, not every thread's.**

A `ScopedValue` is **not** a value; it's a *key*. `where(USER, "alice").run(body)`
pushes a binding `USER -> "alice"` onto the *current thread's* binding stack for
exactly the dynamic extent of `body`, then pops it on return. `USER.get()` walks that
thread's current bindings for the key. Three consequences fall out:

1. **No lexical capture (A).** The lambda `later` captures the *variable* `USER` (a
   handle, effectively final), not any bound value. When `later` runs later, `get()`
   re-resolves against whatever binding is active *then, on that thread*. Inside the
   `"bob"` extent that's `bob`, even though `"alice"` was live when `later` was born.

2. **Inheritance is a fork-time snapshot, not a shared view (B).** `fork` copies the
   *immutable* binding set into the child. Because the set is immutable and bindings
   revert by stack discipline, there is no "shared mutable view" to race on — which
   is exactly why this is virtual-thread-friendly and why `InheritableThreadLocal`'s
   leak/copy problems don't apply. This also answers the **bonus**: there is no
   "rebind `USER` after `fork`, before the child reads" probe because *you cannot
   rebind* — there is no `set()`, and a new `where(...).run(...)` would be a *new*
   binding on the *parent's* frame that the already-forked child never sees. The
   child's snapshot is frozen at fork.

3. **Ad-hoc threads inherit nothing (C).** Only the structured-concurrency `fork`
   path carries bindings. A raw `Thread.ofVirtual().start()` starts with an empty
   binding set, so `get()` throws `NoSuchElementException`. And it throws *on the
   child*, so a parent `try/catch` around `.join()` catches nothing.

The naive answer to all three is "bob, because the closure captured the binding."
That answer is wrong twice over: bindings aren't captured by closures, and they
aren't inherited by ad-hoc threads.

## Common mistakes

- **"`later` captured `alice`, so (A) prints alice."** → No. Lambdas capture
  variables, not active bindings. (A) prints `bob` — the running thread's *current*
  binding.
- **"All three print bob, the closure carries the binding."** → (C) throws. Ad-hoc
  `ofVirtual().start()` does not inherit scoped values; only `fork` does.
- **"(C) throws, so `outer()` / `main` catches it."** → The exception is raised on
  the *child* thread and dies there (printed to stderr). The parent's `.join()`
  returns normally; `outer()` sees nothing.
- **"Unbound `get()` returns null, like `ThreadLocal`."** → It throws
  `NoSuchElementException`. Unbound is an *error*, not a default.
- **"I'll just `USER.set("bob")` to rebind."** → There is no `set()`. It won't
  compile. Rebinding is only a nested `where(...).run(...)`, scoped to that callee's
  frame and reverted on return.

## Interviewer Q&A

**Q:** What's the fundamental difference between `ThreadLocal` and `ScopedValue`?
**A:** `ThreadLocal` is a *mutable per-thread slot* that persists until `remove()`
(and leaks in pools if you forget). `ScopedValue` is an *immutable binding* tied to a
call frame's dynamic extent — bound by `where(...).run(...)`, automatically reverted
on return, with no `set()` and no `remove()`.

**Q:** In probe (A), why does it print `bob` and not `alice`, given `later` was
created while `alice` was bound?
**A:** Lambdas capture *variables*, not *active bindings*. `later` closes over the
handle `USER`; `USER.get()` is resolved fresh each call against the running thread's
currently-active binding. It runs inside the `"bob"` extent on the same thread, so it
sees `bob`.

**Q:** Why does `StructuredTaskScope.fork` propagate the binding but
`Thread.ofVirtual().start()` does not?
**A:** `fork` is part of structured concurrency and snapshots the immutable binding
set at fork time into the child — that's the design contract. An ad-hoc thread is
unstructured; it starts with an empty binding set, so reads throw.

**Q:** What does `get()` return on an unbound `ScopedValue`?
**A:** Nothing — it throws `NoSuchElementException`. Use `isBound()` to check, or
`orElse(...)` for a default. Contrast `ThreadLocal.get()`, which returns `null` or
the `initialValue()`.

**Q:** In (C), can the parent catch the exception by wrapping `.join()` in
try/catch?
**A:** No. The exception is thrown on the *child* thread inside its run, after
`start()` already returned. It's handled by the child's uncaught-exception handler
(stderr by default) and dies there. `.join()` just waits for the child to finish and
returns normally.

**Q:** How do you "change" a scoped value for a sub-call?
**A:** You don't mutate it — you *rebind* by nesting another
`ScopedValue.where(KEY, newValue).run(...)`. The new binding lives only for that
callee's frame and reverts on return. There is no `set()`.

**Q:** Why is `ScopedValue` "virtual-thread-friendly" where `InheritableThreadLocal`
isn't?
**A:** Bindings are immutable and propagated as a cheap fork-time snapshot, with no
per-thread copy of mutable state and no `remove()`-or-leak lifecycle. Spinning up
millions of virtual threads under a structured scope shares bindings without the
copy cost and leak hazards of `InheritableThreadLocal`.

**Q:** Both `ScopedValue` and `StructuredTaskScope` are preview in 21 — what does
that mean for shipping this code?
**A:** They require compiling and running with `--enable-preview` against that exact
JDK; the API may change in later releases. You wouldn't ship it on a build that must
run without preview flags — which is exactly why this exercise is markdown-only.
