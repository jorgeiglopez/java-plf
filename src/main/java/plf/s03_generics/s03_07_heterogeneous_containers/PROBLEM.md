# Typesafe Heterogeneous Containers — Who poisoned the slot?

**Concept:** a `Class<T>` token (EJ33) cannot distinguish generic instantiations, so any caller with map access can silently corrupt a "typed" slot — and the crash lands far from the crime.
**Difficulty:** ★★★ senior
**Est. time:** ~15 min

## Background
`TypesafeMap` is the textbook *typesafe heterogeneous container*: `put(Class<T>, T)` /
`get(Class<T>)`, backed by a `Map<Class<?>, Object>`. It round-trips `String`, `Integer`,
your own types — the compiler is happy and `type.cast(...)` guards every read. It feels
airtight. Then someone keys a slot by `List.class`. Suddenly the container will hand you back
a list whose elements are the wrong type, the `get()` won't complain, and the explosion goes
off three method calls later in code you didn't write. Can you predict where, and name who
actually pulled the trigger?

## Task
1. Read `Demo.java`, `TypesafeMap.java`, and the call to `NameProcessor.totalLength(...)`.
   Treat `NameProcessor` as a **black box** first — predict from the call site alone.
2. **Before running anything**, write into `prediction.txt`:
   - what `Demo.main` prints, line by line — or the exact exception **type** and the
     **method call** where it is thrown (file + line is even better).
3. Now open `NameProcessor`. In **one sentence** in `prediction.txt`, name the exact line that
   broke the type contract and say *why* `Class<List<String>>` and `Class<List<Integer>>` are
   indistinguishable to the container at runtime. Your sentence must contain the word
   **erasure** or the token **`List.class`**.
4. Run the program and the test; confirm your prediction.

## Constraints
- Java 21, no external libraries.
- Do **not** "fix" `TypesafeMap` — this exercise is about *seeing* the corruption, not curing
  it. (The cure — a Gafter-gadget `TypeRef<T>` — is the next sub-topic.)
- Do not modify the test file. Commit your `prediction.txt` answer before you run anything.

## How to verify
```
mvn -q -Dtest=TypesafeMapCorruptionTest test
```
The shipped test `slotStaysAListOfStringsAfterProcessing` **starts red**. Read the failure
message: which type was found where a `String` was expected, and in which call did it surface?
That location — not `get()` — is where the lie becomes visible. Then run `Demo.main` and check
your predicted output/exception against reality.

<details><summary>Hint (open only if stuck)</summary>
`get(List.class)` calls `List.class.cast(value)`. `List.class` is a single, raw <code>Class</code>
object — there is no `List&lt;String&gt;.class`. So `cast` only checks "is this a `List`?",
never "a `List` of <em>what</em>?". The bad element slips through `get()` untouched and only
trips a compiler-inserted cast at the point you actually read an element as a `String`.
</details>
