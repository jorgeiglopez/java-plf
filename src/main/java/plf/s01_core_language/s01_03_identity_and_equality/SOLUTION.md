# Solution — One Method, Two Ops, Half Visible

## Ideal solution

The production method `IdentityLab.f` is already correct — the exercise is to fix the
*predictions* in `HalfVisibleCallTest`. The corrected constants:

```java
static final String  EXPECTED_A_AFTER     = "A!";  // op 1 mutated the shared object
static final String  EXPECTED_B_AFTER     = "B";   // op 2 rebound a COPY; b untouched
static final boolean EXPECTED_A_IS_ORIG_A = true;  // a never moved; same object, mutated
static final boolean EXPECTED_B_IS_ORIG_B = true;  // b never moved; the copy y was rebound
```

Running `IdentityLab.main` prints exactly:

```
1) a            = "A!"
2) b            = "B"
3) a == origA   -> true
4) b == origB   -> true
5) a.equals(b)  -> false
```

The full diagnostic table — the model *is* the diff between these two rows:

| operation in `f`     | effect on caller | identity of caller's var |
|----------------------|------------------|--------------------------|
| `x.append("!")`      | visible (`a` → `"A!"`) | **same ref** (`a == origA` true) — object mutated in place |
| `y = x`              | invisible (`b` stays `"B"`) | **same ref** (`b == origB` true) — only the *copy* `y` was rebound |

Inside `f`, after `y = x`, `x == y` is `true` — but that identity lives only in the
method frame and never escapes. The caller's `a` and `b` are two independent variables;
neither one moved.

### Scale up solutions (Optional)

**1. Integer cache.**
```
Integer p = 127, q = 127;  p == q   ->  true    // cached: -128..127 share instances
Integer r = 128, s = 128;  r == s   ->  false   // 128 is outside the cache: two objects
r.equals(s)                         ->  true    // equals compares value, not identity
```
`Integer.valueOf` (which autoboxing calls) caches boxed values in `[-128, 127]` (JLS 5.1.7,
`Integer.IntegerCache`). Inside the range you get the *same* object, so `==` is true. Outside,
each box is a fresh object, so `==` is false even though the values are equal. `equals` always
compares value. Lesson: `==` on wrappers tests *identity* — the variable can be a copy pointing
at a cached instance, or at a brand-new box.

**2. String constant folding vs runtime concat.**
```
String z = "b";
"ab" == "a" + "b"          ->  true    // both compile-time constants -> same interned literal
"ab" == ("a" + z)          ->  false   // z isn't constant: runtime concat -> NEW String object
"ab" == ("a" + z).intern() ->  true    // intern() returns the pooled instance
```
`"a" + "b"` is a *compile-time constant expression* (JLS 15.29), folded to `"ab"` and interned,
so it is the very same object as the literal `"ab"`. `"a" + z` is computed at runtime (`z` is not
a constant variable) and produces a brand-new `String` on the heap; `==` against the pooled
literal is false. `.intern()` hands back the canonical pooled instance, so `==` is true again —
while `.equals` would have been true in every case.

**3. The single rule.** *The copy is the variable, not the object.* `==` compares the two
copies (the references / the boxed identities); `equals` compares the things they point at.
Mutation reaches the shared object; reassignment only re-points a copy.

## Why this works (and the gotcha)

Java is **always pass-by-value**. For an object argument, the value copied into the parameter
is the **reference**, not the object. So a parameter is a fresh local variable that *happens to
point* where the argument pointed.

- `x.append("!")` follows the reference to the **shared** `StringBuilder` and mutates it. The
  caller sees `a` become `"A!"` — and crucially `a == origA` is still `true`: same object,
  changed in place. Mutation is visible because the object is shared.
- `y = x` reassigns the **local copy** `y`. The caller's `b` is a different variable entirely;
  it never moves, so `b == origB` stays `true` and `b` stays `"B"`. Reassignment is invisible
  because the variable is a copy.

The trap is the **asymmetry**: the exact same call both changes something the caller sees and
fails to change something the caller might expect. A "pass-by-reference" believer predicts
all-or-nothing and gets line 1 right and line 2 wrong — diagnostic gold. The `x`/`y` params vs
`a`/`b` args make the lie literal: `x` is *not* `a`, it is a copy that points where `a` points.

On equality: **`StringBuilder` does not override `equals`**, so it inherits `Object.equals`,
which is reference identity. That is why `a.equals(b)` is `false` here and why `==` and `equals`
give the *same* answer — there is no value-equality to muddy the trap. (Contrast with `String`,
which *does* override `equals`: there `==` can be false while `equals` is true.)

## Common mistakes
- **"Java passes objects by reference."** No. It passes a *copy of the reference* by value.
  Mutation is visible (shared object); reassignment is not (copied variable).
- **Predicting `b` becomes `"A!"` (or `"B"` swapped to the other object).** `y = x` rebinds a
  copy that dies at `return`; the caller's `b` is untouched.
- **Predicting `b == origB` is `false`.** `b` never moved — *the parameter* `y` moved. The most
  common inversion; if you wrote `false`, you still believe the param *is* the argument.
- **Reaching for `a.equals(b)` expecting value equality.** `StringBuilder` has no `equals`
  override; it is identity, so `false`. Use `.toString().contentEquals(...)`/`equals` on the
  text if you want value comparison.
- **Assuming all equal `Integer`s are `==`.** Only `[-128, 127]` are cached; `128 == 128` on
  boxed `Integer` is `false`.

## Interviewer Q&A

**Q:** Is Java pass-by-value or pass-by-reference?
**A:** Always pass-by-value. For objects, the value copied is the *reference*. The method gets a
copy of the reference, so it can mutate the shared object but cannot rebind the caller's variable.

**Q:** In `f`, why does `a` become `"A!"` but `b` stays `"B"` after `x.append("!")` and `y = x`?
**A:** `x` and `a` are different variables pointing at the same object, so mutating through `x`
is visible as `a`. `y = x` only re-points the local copy `y`; the caller's `b` is a separate
variable that was never reassigned, so it stays `"B"`.

**Q:** After the call, what are `a == origA` and `b == origB`, and what do they prove?
**A:** Both `true`. `a` was mutated in place but is the same object (so `a == origA`). `b` never
moved — the rebind happened to the copied parameter, not to `b` (so `b == origB`). Together they
show: mutate → same ref; rebind → copy moved, caller untouched.

**Q:** Why is `a.equals(b)` `false` here, and would `==` give a different answer?
**A:** `StringBuilder` doesn't override `equals`, so `equals` is inherited identity from
`Object`. `a` and `b` are different objects, so both `a == b` and `a.equals(b)` are `false` —
same answer. There is no value-equality for `StringBuilder`.

**Q:** Contrast that with `String`. When can `==` and `equals` disagree?
**A:** `String` overrides `equals` for value comparison. Two `String`s with equal content but
different identities (e.g. `new String("x")` vs `"x"`, or a runtime concat vs a literal) give
`==` false but `equals` true. Rule: `==` is identity; `equals` is whatever the class defines
(value, for `String`).

**Q:** `Integer a = 127, b = 127; a == b` is `true`, but at `128` it's `false`. Why?
**A:** Autoboxing calls `Integer.valueOf`, which caches `[-128, 127]` (JLS 5.1.7). In range you
get the same cached instance (`==` true); outside, fresh objects (`==` false). `equals` is `true`
in both cases. Never use `==` to compare wrapper values.

**Q:** Why is `"ab" == "a" + "b"` true but `"ab" == ("a" + s)` false when `String s = "b"`?
**A:** `"a" + "b"` is a compile-time constant (JLS 15.29), folded and interned, so it's the same
pooled object as the literal `"ab"`. `"a" + s` is computed at runtime (`s` is not a constant
variable), producing a new heap `String`; `==` against the pooled literal is false. `.intern()`
returns the pooled instance, making `==` true again.

**Q:** How would you actually swap two object references a method receives?
**A:** You can't, in Java — reassigning a parameter never affects the caller's variable. Return
the swapped pair (e.g. a record/array), wrap them in a holder/`AtomicReference` the method
mutates, or swap fields of a shared container. The reference itself is passed by value.

**Q:** Inside `f`, after `y = x`, is `x == y`? Does that mean anything to the caller?
**A:** Yes, `x == y` is `true` inside the frame — both name the same object after the rebind. But
that identity is local to the method frame; it never escapes. The caller's `a` and `b` are
unaffected by it.
