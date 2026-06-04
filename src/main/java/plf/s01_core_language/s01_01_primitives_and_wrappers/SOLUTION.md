# Solution — Silent Wrong vs. Loud Dead

## Ideal solution

Two lines, two failure *modes*, one root cause (autoboxing/unboxing). To turn
the test green, fix the one prediction that ships wrong:

```java
// In SilentUnboxingTest:
private static final String PREDICTED_SILENT_WRONG = null; // was "one"
```

The `assertThrows(NullPointerException.class, SilentUnboxing::loudDead)` line was
already correct and needs no change.

### Line 1 — silent wrong: returns `null`

```java
Map<Long, String> byId = new HashMap<>();
byId.put(1L, "one");
byId.get(1);   // -> null   (NOT "one")
```

`Map.get` is declared `get(Object key)`. The literal `1` is an `int`, so to pass
it as `Object` the compiler **autoboxes it to `Integer`**, not `Long`. The map
hashes/compares with `Integer.valueOf(1)`. Lookup walks the bucket and calls
`equals`; `Long.equals` starts with `if (obj instanceof Long)` — an `Integer` is
not a `Long`, so it returns `false`. No key matches → `get` returns `null`.
Compiles, runs, ships a quietly wrong answer.

### Line 2 — loud dead: throws `NullPointerException`

```java
Map<String, Integer> counts = new HashMap<>();
counts.put("seen", 3);
int n = counts.get("absent");   // -> NullPointerException
```

`get("absent")` returns `null` (an absent key). Assigning a reference into an
`int` triggers **unboxing**, i.e. an implicit `((Integer) null).intValue()`,
which is a `NullPointerException` (JLS 5.1.8). On Java 21 the *helpful* NPE
message is:

```
Cannot invoke "java.lang.Integer.intValue()" because the return value of "java.util.Map.get(Object)" is null
```

The contrast is the lesson: **silent wrong** (a bad value flows downstream) vs.
**loud dead** (it blows up here) — and *both* are unboxing you never wrote.

### Scale up solutions

1. **Cache boundary.** `127 == 127` → `true`; `1000 == 1000` → `false`.
   `Integer.valueOf` caches `-128..127` (JLS 5.1.7; `IntegerCache`, whose upper
   bound is tunable via `-XX:AutoBoxCacheMax`). In range, both refs point at the
   *same* cached object, so `==` (reference identity) is `true`. Outside the
   range, boxing mints fresh objects, so `==` is `false`. Use `.equals()` or
   compare the primitives.

2. **Cross-type `equals`.**
   `x == 1` → `true`: comparing a `Long` to an `int` literal **unboxes** `x` to
   `long`, then compares values — `1L == 1` is `true`.
   `x.equals(1)` → `false`: `equals(Object)` **boxes** `1` to `Integer`, and
   `Long.equals` requires `instanceof Long` first, so `Integer(1)` never equals
   `Long(1L)`. `==` unboxes; `equals` boxes — opposite directions, opposite
   answers.

3. **Comparator overflow.**
   `nums.sort((p, q) -> p - q)` on `[-2_000_000_000, 2_000_000_000]` yields
   `[2000000000, -2000000000]` — **reversed**. `(-2e9) - (2e9)` overflows `int`
   and wraps to a *positive* number, so the comparator claims the smaller value
   is "greater". `Integer::compare` (or `Long.compare(p, q)`, or
   `Comparator.naturalOrder()`) returns the correct `[-2000000000, 2000000000]`.
   Note the boxing is a red herring here: the same `int[]` lambda overflows
   identically. The real defect is `a - b` arithmetic; the wrappers just make it
   look like a "boxing problem". (Included here precisely to show what *isn't*
   the boxing trap.)

## Why this works (and the gotcha)

Autoboxing/unboxing is a *compiler* feature: it inserts `valueOf` and
`intValue()`/`longValue()` calls you never see in source. That invisibility is
the whole hazard.

- **`Map.get(Object)` boxes the argument to the literal's natural type.** `get(1)`
  boxes to `Integer` regardless of the map's key type, because generics are
  erased and `get` takes raw `Object`. There is **no compile error** warning you
  that `Integer` can never match a `Long` key — the type information needed to
  catch it was thrown away. That is why this ships.
- **Unboxing `null` is a guaranteed NPE.** Any time a wrapper that might be
  `null` lands in a primitive context (`int n = map.get(k)`, arithmetic, a
  `boolean` condition), you have a latent NPE.

The naive mental model — "`Integer` and `int` are interchangeable" — is exactly
what these two lines punish.

### A subtlety the author had to get right

Line 1 detonates **only** because the map is declared `Map<Long, String>` while
`get` takes `Object`. If the API exposed a *typed* `get(Long key)`, then
`get(1)` would not compile (or would widen/box to `Long`), and the trap would
die. The silent miss exists precisely because `Map.get` is untyped.

## Common mistakes

- **Predicting `"one"` for line 1.** Tempting: `1` *equals* `1L` numerically. But
  the lookup compares `Integer` to `Long`, and cross-type `equals` is `false`.
  Result is `null`, not `"one"`.
- **Expecting `0` (or a default) for line 2.** There is no default; unboxing
  `null` throws. `map.getOrDefault("absent", 0)` is the fix.
- **"It's a `==` / Integer-cache bug."** No `==` appears. The cache is a separate
  (scale-up) gotcha; conflating them means you do not actually understand either.
- **Blaming boxing for the comparator overflow.** The defect is `int`
  subtraction overflow; strip the wrappers and the bug is identical.
- **Storing the key as the wrong type, then "fixing" with a cast.** Casting `1`
  to `(long)` in the `get` works, but the durable fix is to look up with a value
  of the key's type (`get(1L)`) or to type the API.

## Interviewer Q&A

**Q:** `Map<Long,String> m = new HashMap<>(); m.put(1L,"x"); m.get(1)` — what does
`get(1)` return, and why?
**A:** `null`. `Map.get` takes `Object`, so `1` autoboxes to `Integer`, not
`Long`. `Long.equals` checks `instanceof Long` first, so `Integer(1)` never
equals the `Long(1L)` key; no match, `null`.

**Q:** Why doesn't the compiler stop you, given `Long` keys can never equal an
`Integer`?
**A:** `get` is `get(Object)` — generics are erased and the parameter is raw
`Object`, so any object is accepted at compile time. The type info that could
flag the mismatch is gone by runtime. (IDEs may warn via
`SuspiciousMethodCalls`, but `javac` won't.)

**Q:** `int n = map.get(absentKey)` — what happens and what's the exact
mechanism?
**A:** `NullPointerException`. `get` returns `null`; assigning a reference to an
`int` unboxes it via `intValue()`, and calling `intValue()` on `null` throws
(JLS 5.1.8). Java 21's helpful NPE names `Integer.intValue()` and the
`Map.get(Object)` return.

**Q:** Where else does null-unboxing bite besides assignment?
**A:** Any primitive context: arithmetic (`map.get(k) + 1`), comparisons
(`if (flag)` where `flag` is a `Boolean`), ternaries that mix a wrapper and a
primitive branch, and `switch` on a wrapper. All can NPE on a `null` wrapper.

**Q:** Explain the `Integer` cache and why `Integer a=127,b=127; a==b` is `true`
but `1000==1000` is `false`.
**A:** `Integer.valueOf` caches instances for `-128..127` (JLS 5.1.7). In range,
both refs are the *same* cached object so `==` (identity) is `true`; outside,
each box is a new object so `==` is `false`. The upper bound is tunable with
`-XX:AutoBoxCacheMax`, which is exactly why you must never use `==` on wrappers.

**Q:** `Long x = 1L;` — why is `x == 1` true but `x.equals(1)` false?
**A:** `==` between a `Long` and an `int` unboxes `x` to `long` and compares
values: `1L == 1` is `true`. `equals(Object)` boxes `1` to `Integer`, and
`Long.equals` requires `instanceof Long`, so it's `false`. `==` unboxes; `equals`
boxes — opposite directions.

**Q:** Why is `(a, b) -> a - b` a buggy `Comparator<Integer>`, and is that a
boxing bug?
**A:** It's an `int` overflow bug, not a boxing bug. `a - b` can overflow (e.g.
`-2e9 - 2e9` wraps positive), violating the comparator contract and producing a
wrong/garbage order. Use `Integer.compare(a, b)`. Removing the wrappers doesn't
fix it — that's the tell it isn't about boxing.

**Q:** Give the senior rules of thumb to avoid this whole family of bugs.
**A:** (1) Never `==` on wrappers — use `.equals()` or compare primitives.
(2) Never unbox a value that can be `null`; prefer `getOrDefault`/`Optional` or
an explicit null check. (3) Look maps up with a key of the *declared* key type.
(4) Use `Integer.compare`/`Long.compare`, never subtraction, in comparators.
(5) In hot paths, prefer primitive specializations (`IntStream`, primitive
arrays) to avoid boxing churn entirely.
