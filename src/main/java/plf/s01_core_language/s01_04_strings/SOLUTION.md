# Solution — The Two Rows That Lie

## Ideal solution

The completed ledger (Java 21, Corretto, all verified):

| # | Code                                                  | `==`    | `.equals` |
|---|-------------------------------------------------------|---------|-----------|
| 1 | `final String s = "a";       (s + "b") == "ab"`       | `true`  | `true`    |
| 2 | `final String s = runtime(); (s + "b") == "ab"`       | `false` | `true`    |
| 3 | `String s = "a";             (s + "b") == "ab"`       | `false` | `true`    |

The only fix needed is the deliberately-wrong assertion in `row2`:

```java
@Test
void row2_finalButRuntimeInit_isInterned() {
    final String s = runtime();            // final, but NOT a constant variable
    assertFalse((s + "b") == "ab");        // FIXED: runtime concat -> fresh heap String
    assertTrue((s + "b").equals("ab"));    // contents equal (always)
}
```

(Swap the `assertTrue` for `assertFalse`, or assert `!= "ab"`. The point is the
identity is `false`; the contents are still `"ab"`.) `row1` is untouched.

### Scale up solutions (Optional)

1. **`intern()` round-trip:** `(s + "b").intern() == "ab"` → **`true`**.
   `intern()` returns the canonical pooled instance, which is the same object the
   `"ab"` literal already refers to. So even though the raw concat is a fresh
   object, interning it lands back on the pooled `"ab"`.
2. **`new String` identity:** `new String("x") == "x"` → **`false`** (`new`
   guarantees a fresh, un-pooled object); `new String("x").intern() == "x"` →
   **`true`** (intern hands back the pooled literal). `new String(literal)` is
   pure waste — it copies a pooled string onto the heap for no benefit.
3. **Text blocks:**
   ```java
   String a = """
           hi""";        // length 2, value "hi", does NOT end in '\n'
   String b = """
           hi
           """;          // length 3, value "hi\n", DOES end in '\n'
   ```
   The closing `"""` on its own line means the line before it ends with a
   newline that is *retained*; closing delimiter glued to the last content line
   means no trailing newline. Incidental leading whitespace (the common indent)
   is stripped either way, so `a.equals("hi")` is `true`.

## Why this works (and the gotcha)

The spine is one sentence: **`javac` interns what it can prove at compile time,
and only that.**

- **Row 1.** `final String s = "a";` makes `s` a *constant variable* — a `final`
  variable whose initializer is itself a constant expression (JLS 4.12.4).
  Therefore `s + "b"` is a *constant expression* (JLS 15.29) and the compiler
  folds it to the literal `"ab"` at compile time, placing it in the constant
  pool. `"ab"` (the literal on the right) is the same pooled instance. `==` is
  `true` because there is literally one object.
- **Row 2.** `final String s = runtime();` is `final`, but `runtime()` is a
  **method call**, which is *never* a constant expression. So `s` is **not** a
  constant variable, `s + "b"` is **not** a constant expression, and the compiler
  cannot fold it. At runtime, `s + "b"` compiles to `StringBuilder`/`StringConcatFactory`
  machinery that allocates a **brand-new heap `String`**. That fresh object is
  not in the pool, so `== "ab"` is `false`.
- **Row 3.** `String s = "a";` is not `final` at all, so identical reasoning:
  no constant variable, no folding, runtime allocation, `==` is `false`.

**The trap:** people memorize "`final` ⇒ folded." Wrong. `final` is *necessary
but not sufficient*. The real predicate is "is this a compile-time constant
expression?" Rows 2 and 3 agree precisely because they share the same
*underlying* reason — the operand is not a constant variable — even though only
one of them uses `final`. The keyword you just "learned" lied to you.

**Why the `.equals` column matters:** every row is `.equals "ab"`. The contents
are never in question — only *identity* is. A student who sees Row 2's `==`
fail and concludes "the strings are different" has learned the wrong lesson. The
column pins it: equality (value) ≠ identity (reference).

One more pin: Row 1 is `true` because **`javac` folds at compile time** (JLS
15.29), not because the JVM is interning generously at runtime. You cannot
reproduce or "disprove" it with reflection — the folding already happened in the
`.class` file's constant pool.

## Common mistakes

- **"It's `final`, so it's folded."** → `final` only triggers folding when the
  initializer is itself constant. `runtime()` (a method call) breaks it; so does
  `final String s = args[0];`.
- **Switching `==` to `.equals` to make the test pass.** → That erases the entire
  lesson. The contents were always equal; the exercise is about identity.
- **Believing the `==` result is JVM/run-dependent.** → It is decided by `javac`
  at compile time and baked into the constant pool. Deterministic.
- **`new String("literal")`.** → Always a fresh heap object; never `==` the
  literal. A pure allocation with no upside.
- **Thinking a `final` *method parameter* would fold.** → A parameter is
  initialized at call time, so it is never a constant variable (JLS 4.12.4),
  regardless of `final`.

## Interviewer Q&A

**Q:** `"a" + "b" == "ab"` — true or false, and why?
**A:** `true`. Both operands are constant expressions, so `javac` folds the
concatenation to the literal `"ab"` at compile time and interns it; the
right-hand `"ab"` is the same pooled instance.

**Q:** Make the left side `final String s = "a"; s + "b" == "ab"`. Still true?
Now change the initializer to a method call. Still true?
**A:** With `s = "a"`, still `true` — `s` is a *constant variable* (final +
constant initializer), so the expression is still a compile-time constant. With
`s = someMethod()`, it becomes `false`: a method call is never a constant
expression, so `s` is not a constant variable, the concat happens at runtime, and
it produces a fresh heap `String`.

**Q:** So is the rule "`final` means folded"?
**A:** No. `final` is necessary but not sufficient. The real rule is JLS 15.29:
the whole expression must be a *constant expression*, which requires every
variable operand to be a *constant variable* (JLS 4.12.4 — final **and**
initialized by a constant expression). Parameters, fields read from non-constant
initializers, and method results all break it.

**Q:** What does `String.intern()` do, and when would you call it?
**A:** It returns the canonical pooled instance for that string's value, adding
it to the pool if absent. Two `.equals` strings always `intern()` to the same
`==` object. You rarely need it; it can help deduplicate a flood of equal,
runtime-built strings, but the pool lives in heap (since Java 7) and over-interning
can bloat it. Prefer `.equals` over engineering identity.

**Q:** A `String` is immutable. What actually backs it, and how is that enforced?
**A:** Internally a `private final byte[] value` plus a `byte coder`. The array
reference is `final` and never escapes, the class is `final`, and every "mutating"
method returns a new `String`. Immutability is what makes safe sharing, the string
pool, and caching `hashCode` sound.

**Q:** What are *compact strings* (JEP 254), and how do they relate to that
backing array?
**A:** Since Java 9, `String` stores `byte[]` instead of `char[]`. If every
character fits in Latin-1 (one byte), `coder == LATIN1 (0)` and each char is one
byte. The moment a character needs more (e.g. a non-Latin-1 codepoint), the whole
string switches to `coder == UTF16 (1)` with two bytes per unit. So an all-ASCII
string uses ~half the memory of the old `char[]` representation; a single emoji
flips the entire string to UTF-16.

**Q:** Why is `s += x` in a loop a performance bug, and what's the fix?
**A:** Each `+=` compiles to a fresh `StringBuilder`/concat that copies the whole
accumulated string, giving O(n²) work and O(n) garbage. The fix is one
`StringBuilder` (or `StringBuffer` if you truly need synchronization) reused
across the loop, appending in place — O(n). Note: a *single* `a + b + c`
expression is fine; javac fuses it into one concat. It is the *repeated*
concatenation across statements/iterations that hurts.

**Q:** `StringBuilder` vs `StringBuffer` — which and when?
**A:** `StringBuilder` almost always. `StringBuffer`'s methods are
`synchronized`, which costs you on every call; it's a legacy class useful only
when a single buffer is genuinely shared and mutated across threads — rare, and
usually better solved by confining the builder to one thread and publishing the
finished immutable `String`.

**Q:** In a text block, what's the difference between closing `"""` on its own
line versus glued to the last content line?
**A:** On its own line, the final content line keeps a trailing `\n`; glued to the
content, there's no trailing newline. Incidental leading whitespace (the common
indentation determined by the least-indented line, including the closing
delimiter's position) is stripped in both cases. Use `\` at end of line to
suppress a newline, or `\s` to preserve trailing spaces.
