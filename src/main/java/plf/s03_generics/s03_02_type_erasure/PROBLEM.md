# Type Erasure — The cast that fires 14 frames away

**Concept:** an unbounded `(T)` cast erases to `(Object)` — a runtime no-op — so the
real `checkcast` is inserted at the *caller's* assignment, not where you wrote the cast.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background

Here is a method that compiles cleanly, passes every test you throw at it in
isolation, and looks airtight:

```java
public static <T> T firstElement(List<T> list) {
    return (T) list.get(0);
}
```

A teammate reports a `ClassCastException` from this method "in production." You
open it, see the `(T)` cast, and reach for a fix *at the cast*. Stop. The cast
isn't where the exception happens — and there is no fix that lives inside this
method. Your job is to prove why, then recover as much runtime safety as the type
system will let you.

## Task

Work in `ErasedAccess.java` and the provided `ErasedAccessTest.java`.

1. **Diagnose (no edits).** Read `firstElement`. The test `erasedCastFailsAtCaller`
   pollutes a `List<Integer>` into a `String` variable and asserts the
   `ClassCastException`'s *top stack frame* is the caller, not `firstElement`.
   Run it. Before you do, write down: which frame is on top, and why does
   `firstElement` itself never throw? Confirm your prediction against the green bar.

2. **Repair (implement Task 2).** Implement `firstElementSafe(List<T> list,
   Class<T> clazz)` so the failure moves *into* the method — i.e. the caller gets
   a stack frame pointing at the real culprit. One line. You may not change
   `firstElement`.

3. **Prove.** Turn `tokenCastFailsInsideMethod` and `tokenCastSucceedsOnMatch`
   green. The first asserts `firstElementSafe` appears in the CCE's stack trace;
   the second asserts a matching type passes through untouched.

Then answer in one sentence: *why does the `Class<T>` token restore the check that
erasure deleted?*

## Constraints

- Java 21, no external libraries.
- Do **not** modify `firstElement` or the test file.
- `firstElementSafe` must be implemented with the token — no `instanceof`, no
  manual `if`-checks, no catching and rethrowing.

## Scale up (optional)

- **Recursion of the token.** Could you write `firstElementSafe` for a
  `List<List<String>>` and have it reject a polluted `List<List<Integer>>` at the
  method boundary? Sketch the signature you'd need and explain why
  `Class<List<String>>` is not a thing you can hand it. (This is why
  `@SuppressWarnings("unchecked")` exists in the JDK source.)
- **`javap` proof.** Compile `ErasedAccess` and run
  `javap -c plf.s03_generics.s03_02_type_erasure.ErasedAccess`. Find the
  `checkcast` in `firstElement`'s bytecode — or rather, confirm there isn't one,
  then find where it *does* appear by disassembling the test class.

## How to verify

```
mvn -q -Dtest=ErasedAccessTest test
```

All three tests green. The diagnostic test should already be green before you
touch anything; the two `firstElementSafe` tests start red.

<details><summary>Hint (open only if stuck)</summary>
`Class<T>` has a one-method tool that performs a *checked* cast at runtime using
the reified `Class` object you handed it. Unlike `(T)`, that object survives
erasure — it is a real value, not a compile-time fiction.
</details>
