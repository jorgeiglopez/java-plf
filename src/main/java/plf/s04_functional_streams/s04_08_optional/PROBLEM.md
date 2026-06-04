# Optional — The fallback that always runs

**Concept:** `Optional` doesn't defer and doesn't fail loudly — `orElse(x)` evaluates `x` *now*, every call, and `map` quietly turns `null` into `empty()`. The cost lands away from the code that looks wrong.
**Difficulty:** ★★★ senior
**Est. time:** ~20 min

## Background
`UserLookup.emailOrAudit` looks airtight: when the email is present, return it; otherwise fall back to `audit(...)`, which logs a "missing email" event and returns `""`. A reviewer reads it and nods.

Then the audit table fills with rows for users who *have* an email. Worse, on a record whose `id` is `null`, the call throws `NullPointerException` on the *happy path* — for a user who is present and has an email.

How can a fallback fire for a value that isn't missing? And why does the second method, `middleNameOf`, refuse to compile when you reach for the obvious `map`?

## Task
Work in `UserLookup.java`.

1. **Fix the eager fallback (`emailOrAudit`).** `audit("missing:" + id)` is built and invoked on *every* call because `orElse` takes a value, and arguments are evaluated before the method runs. Refactor so `audit(...)` runs **only** when the email is genuinely absent or blank. The red test `presentEmailMustNotTouchAuditLog` must turn green, and the other `emailOrAudit` tests must stay green.

2. **Implement `middleNameOf`** to return `Optional<String>` for a given id. `findById` gives you `Optional<User>`, and `User::middleName` *itself* returns `Optional<String>`. Pick the operation that yields `Optional<String>`, not `Optional<Optional<String>>`. The target type is load-bearing — let it tell you which one.

## Constraints
- Java 21, no external libraries.
- Do **not** modify the test file.
- Do **not** change `audit`'s signature or the `User` record shape.
- Do not unwrap with `get()` / `isPresent()` then `get()`.

## Scale up (Optional)
3. The `User` record carries `Optional<String> middleName` as a **field/component**. Name two concrete reasons EJ55 says this is wrong, and what runtime exception appears if `User` were made `Serializable`. State what the field's type *should* be and how absence would then be modeled.

## How to verify
`mvn -q -Dtest=UserLookupTest test` — all five tests green.

<details><summary>Hint (open only if stuck)</summary>
Two methods take a *value* and run it eagerly: `orElse` and `orElseThrow(x)`. Two take a *supplier* and run it only when needed: `orElseGet(() -> ...)` and `orElseThrow(() -> ...)`. For Task 2, when your mapper already returns an `Optional`, `map` nests it; the un-nesting operation is `flatMap`.
</details>
