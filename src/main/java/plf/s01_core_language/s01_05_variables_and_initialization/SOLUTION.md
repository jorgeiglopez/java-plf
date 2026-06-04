# Solution — Confident and Wrong

## Ideal solution

The constructor captures:

```
snapshotInCtor == "legs=8 name=null weight=0"
```

And after construction completes:

```
legs=8 name=tarantula weight=5
```

The committed test asserts exactly this and is **green**:

```java
Construction.Spider s = new Construction.Spider();
assertEquals("legs=8 name=null weight=0", s.snapshotInCtor); // what the ctor saw
assertEquals(8, s.legs());
assertEquals("tarantula", s.name());
assertEquals(5, s.weight());                                  // after ctor
```

Three fields, three fates, observed inside `Animal()` before any `Spider`
initializer has run:

| field    | declaration               | constant expr? | value seen in ctor | why |
|----------|---------------------------|----------------|--------------------|-----|
| `legs`   | `final int legs = 8;`     | **yes**        | `8`                | inlined literal, no field read |
| `name`   | `String name = "tarantula";` | no          | `null`             | real reference field, still default |
| `weight` | `final int weight = computeWeight();` | **no** | `0`             | real field (method-call init), still default |

### Scale up solutions

1. **Make the ctor see `weight=5` while keeping `final` and the initializer.**
   Yes — but only by turning the initializer into a **constant expression**
   (JLS §15.29). Replace `computeWeight()` (a method call, not constant) with a
   constant: `final int weight = WEIGHT;` where `static final int WEIGHT = 5;`.
   A `final` field initialized with a constant expression is itself a compile-time
   constant, so the compiler inlines `5` everywhere `weight` is read and the
   constructor sees `5`. The moment the initializer is non-constant (a method
   call, `new`, a read of a non-`final` field), the field is a real field and the
   constructor sees `0`. The discriminator is **constant-ness**, not `final`.

2. **The honest fix.** Don't let a constructor call overridable methods. Options:
   - Make the methods (or the class) `final` so dispatch can't escape into a
     half-built subclass — then the superclass calls its own definitions.
   - Don't compute in the superclass constructor at all; pass the needed values
     in via constructor parameters (`super(legs, name, weight)`), so the
     subclass supplies fully-formed data before the superclass uses it.
   - Lazily compute on first use (after construction), not during construction.

   Rule of thumb: **never call an overridable method from a constructor**
   (Effective Java, Item 19). The object is not yet fully constructed.

3. **Varargs corner.**
   ```
   f()            -> "len=0"     // empty array, NOT null
   f(1, 2)        -> "len=2"
   f((int[])null) -> "null"      // you explicitly passed the array reference as null
   ```
   `f()` synthesizes a **zero-length array**; the parameter is never `null` from
   an empty call. You only get `null` by explicitly handing it a `null` array
   reference (the cast disambiguates "the whole array is null" from "one null
   element"). `f(null)` here is actually ambiguous-looking but resolves to the
   array being null too; the cast makes the intent explicit and avoids a warning.

4. **Overload phase.** `g(1, 2)` resolves to **`g(int a, int b)`**, the
   fixed-arity overload. Overload resolution runs in phases (JLS §15.12.2):
   phase 1 (no boxing, no varargs), phase 2 (boxing), phase 3 (varargs) — and
   stops at the first phase that finds an applicable method. A fixed-arity match
   wins in phase 1, so adding a varargs overload **never** silently hijacks an
   existing exact call. Varargs is the last resort.

5. **Static forward reference (won't compile — markdown only).**
   ```java
   static int a = b;     // error: illegal forward reference
   static int b = 1;
   ```
   exact javac (Java 21) message:
   ```
   error: illegal forward reference
   ```
   The JLS (§8.3.3) forbids a static initializer from reading, *by simple name*,
   a static field declared textually later in the same class. Route the read
   through a method and the restriction lifts — but the method runs during class
   init while `b` is still its default:
   ```java
   static int a = readB();        // compiles
   static int readB() { return b; }
   static int b = 1;
   // class init runs top-to-bottom: a = readB() = 0 (b not assigned yet), then b = 1
   // => a == 0, b == 1
   ```

## Why this works (and the gotcha)

Object creation follows JLS §12.5 strictly:

1. Allocate the instance; **default-initialize every field** (`0`/`0.0`/`false`/`null`).
2. Invoke the constructor, whose first act is an explicit or implicit `super(...)`.
3. The superclass constructor body runs **to completion** — including any
   methods it calls. Those calls use **virtual dispatch**, so they land in the
   most-derived override, i.e. `Spider.legs()/name()/weight()`.
4. **Only after `super()` returns** do the subclass's field initializers and
   constructor body run.

So at step 3 the `Spider` instance exists but its initializers (`= "tarantula"`,
`= computeWeight()`) have not executed. Real fields therefore still hold their
defaults: `name` is `null`, `weight` is `0`.

The trap: **`final` does not protect you.** Devs read `final int weight =
computeWeight()` and assume "final means it always has its value." It doesn't —
`final` means *assigned exactly once*, and that assignment hasn't happened yet
during `super()`. The proof that the issue is *timing, not `final`-magic* is
`legs`: it is **also** `final`, yet reads the correct `8`. The difference is that
`final int legs = 8` is a **constant expression** (JLS §15.29: `final` variable
initialized with a constant expression). The compiler treats it as a compile-time
constant and **inlines the literal `8`** at every use site — `legs()` doesn't
read a field at all, so there is nothing to be uninitialized. `weight`'s
initializer is a method call, which is *not* constant, so `weight` stays a real,
default-`0` field during construction.

One virtual call. Two `final` fields. One reads `8`, the other reads `0`. The
keyword is the same; the constant-ness is not.

## Common mistakes

- **"`final` means it always has its value."** → No. `final` = assigned once;
  before that assignment the field is its default. Mid-`super()` it hasn't been
  assigned.
- **"Both `final` fields behave the same."** → `legs` is inlined (constant
  expression), `weight` is a real field. They diverge to `8` vs `0`.
- **Predicting `null`/`0` for `legs` too.** → It's a constant, so it's `8`, even
  in the constructor. This is the part that humbles people who "got" the trap.
- **Predicting `f()` gives `null`.** → It gives a zero-length array (`len=0`).
- **Assuming a new varargs overload changes `g(1,2)`.** → Fixed-arity wins in an
  earlier resolution phase; varargs is last resort.
- **"The forward-reference rule is about runtime."** → It's a *compile-time*
  rule about reading a later-declared static by simple name in an initializer.

## Interviewer Q&A

**Q:** Walk me through what happens, step by step, when `new Spider()` runs.
**A:** Allocate and zero all instance fields; call `Spider()` which implicitly
calls `super()` → `Animal()`; `Animal()` virtually dispatches to `Spider`'s
`legs()/name()/weight()` while `Spider`'s initializers haven't run, so it reads
inlined `8`, default `null`, default `0`; `super()` returns; then `Spider`'s
field initializers (`name="tarantula"`, `weight=computeWeight()=5`) and ctor body
run. (JLS §12.5.)

**Q:** Both `legs` and `weight` are `final`. Why does the constructor see `8` for
one and `0` for the other?
**A:** `final int legs = 8` is a constant expression (JLS §15.29), so the
compiler inlines the literal `8` at every read — `legs()` never touches a field.
`final int weight = computeWeight()` has a non-constant (method-call) initializer,
so `weight` is a genuine field that is still at its default `0` until the
subclass initializers run after `super()`.

**Q:** So is the `final` keyword relevant to the timing at all?
**A:** No. `final` means assigned exactly once; it says nothing about *when*.
The real discriminator is whether the initializer is a *compile-time constant
expression*. A non-final field with a constant initializer is still a real field
(reads default during `super()`); a `final` field with a constant initializer is
inlined. Constant-ness, not finality, decides.

**Q:** How would you fix this so the superclass constructor never sees a
half-initialized object?
**A:** Don't call overridable methods from a constructor (Effective Java Item 19).
Either make the methods/class `final` so dispatch can't escape, or pass the
values in via `super(...)` parameters so the data is fully formed before the
superclass uses it, or defer the computation until after construction.

**Q:** What's the difference between definite assignment and what we saw here?
**A:** Definite assignment is a *compile-time* check that a (blank) `final` is
assigned exactly once on every code path before use — it's satisfied here, the
code compiles. The `0`/`null` we observed is a *runtime* timing artifact:
default initialization precedes the subclass initializers. Definite assignment
guarantees "assigned once," not "assigned before the superclass constructor
reads it through a virtual call."

**Q:** `f()` on `static String f(int... xs)` — does `xs` come in as `null` or an
empty array?
**A:** An empty (zero-length) array. The compiler synthesizes `new int[0]`. You
only get `null` by explicitly passing a `null` array reference, e.g.
`f((int[]) null)`. So a guard like `if (xs == null)` never triggers on a plain
empty call.

**Q:** If I add `g(int... xs)` next to an existing `g(int a, int b)`, does
`g(1, 2)` change behavior?
**A:** No. Overload resolution tries phases in order — strict (no boxing, no
varargs) first, then boxing, then varargs — and stops at the first applicable
match. `g(int, int)` matches in the strict phase, so the varargs overload is
never reached for `g(1, 2)`. Varargs is always the last resort.

**Q:** Why won't `static int a = b; static int b = 1;` compile, and how does the
order change if you read `b` through a method?
**A:** JLS §8.3.3 forbids a static initializer from referencing, by simple name,
a static field declared later in the same class — `illegal forward reference`.
Reading through a method (`static int a = readB();`) sidesteps the *compile-time*
rule, but at *runtime* class initialization runs top-to-bottom: `a` is assigned
`readB()` while `b` is still `0`, so `a == 0` and `b == 1`. Same lesson as the
constructor: initializers run in order, and reads before assignment see defaults.
