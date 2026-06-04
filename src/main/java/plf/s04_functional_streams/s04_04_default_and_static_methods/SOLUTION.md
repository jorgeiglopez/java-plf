# Solution — Who Wins?

## Ideal solution

### Task 1 — concrete superclass wins
`new MyService().format()` returns **`"[parent]"`**, not `"[default]"`.

```java
String predictedConcrete = "[parent]";
```

The `Logger` default never runs. A class method inherited from a superclass takes
priority over any interface default of the same signature — silently, at compile
time, with no warning.

### Task 2 — resolve the diamond with `Interface.super`
```java
static final class Merged implements Left, Right {
    @Override
    public String id() {
        return Left.super.id();
    }
}
```

`Left.super.id()` invokes the default declared in `Left` specifically. `super.id()`
alone is ambiguous (which super-interface?) and won't compile; the qualifier is
mandatory. To return `Right`'s value instead you'd write `Right.super.id()`.

### Task 3 — the abstract case does NOT compile
```text
error: AbstractYields is not abstract and does not override abstract method format() in AbstractBase
```
`AbstractYields` is rejected. The `Logger` default does **not** step in to satisfy
the abstract method inherited from `AbstractBase`. This is the same "class wins"
rule, just inverted: a method declared in the class hierarchy — even an *abstract*
one — outranks an interface default, so the default is never even a candidate to
fill the hole. The class is left with an unimplemented abstract method and fails
to compile. The fix is to override `format()` explicitly in `AbstractYields`
(optionally delegating with `Logger.super.format()`).

## Why this works (and the gotcha)

JLS §9.4.1 resolution, in priority order:

1. **A concrete method inherited from a superclass** — wins over any default.
2. **The most specific default** — if a sub-interface overrides a super-interface's
   default, the sub-interface one wins.
3. **Unrelated defaults from two interfaces** — *compile error*; you must override
   and disambiguate, typically with `X.super.method()`.

The trap is the belief that "default methods override" anything. They don't.
A default is a **fallback the interface supplies only when the implementing class
hierarchy supplies nothing concrete.** The class hierarchy always has first claim:

- A concrete superclass method shadows the default **silently** (`[parent]`) — the
  dangerous case, because nothing flags it.
- An abstract superclass method does **not** get filled by the default — it forces
  a **loud** compile error, because an abstract declaration in the hierarchy still
  outranks the default and leaves an unsatisfied obligation.

So "who wins?" has one answer with two faces: the class hierarchy wins, whether it
wins quietly (concrete) or by refusing the default (abstract).

### A sharper edge: access modifiers can break the override relationship
A `default` method is implicitly `public`. If the superclass method is
*package-private* and lives in a **different package** from the interface, it does
not stand in an override relationship with the default at all — different package,
not accessible, so no override. That is why the scaffold's `ConcreteBase.format()`
is declared `public`: a package-private `format()` would fail with *"attempting to
assign weaker access privileges; was public"* because it cannot legally override
the public default. Access modifiers silently change method resolution — the
real-world bug that ships at 2am.

## Common mistakes
- "The default runs because that's what defaults are for." → A concrete superclass
  method shadows it; you get `[parent]`.
- Writing `super.id()` to resolve the diamond → won't compile; ambiguous. Must be
  `Left.super.id()`.
- Assuming the default auto-satisfies an inherited *abstract* method → compile
  error; the default is not a candidate.
- Making the overriding/colliding class method package-private to match a default →
  "weaker access privileges" error; defaults are `public`.
- Expecting to call a `static` interface method through an instance or subclass →
  static interface methods are **not inherited**; only `Interface.staticMethod()`
  works.

## Interviewer Q&A

**Q:** Why were default methods added in Java 8?
**A:** Interface evolution — so the JDK could add methods like
`Collection.stream()` or `Iterable.forEach()` to existing interfaces without
breaking every implementor. A default provides a body so old implementations still
compile and run.

**Q:** A method exists as both a concrete superclass method and an interface
default. Which runs, and is there a warning?
**A:** The superclass method wins, silently, with no warning. The "class wins" rule:
a method inherited from a class outranks any interface default of the same
signature.

**Q:** Two interfaces declare the same default method and a class implements both.
What happens?
**A:** Compile error — "inherits unrelated defaults." The class must override the
method and pick a winner, usually via `InterfaceName.super.method()`.

**Q:** Why `Left.super.id()` and not plain `super.id()`?
**A:** With multiple super-interfaces, plain `super` is ambiguous. The qualified
form names exactly which super-interface's default to invoke. It is the only
syntax where `super` carries a type qualifier.

**Q:** A class extends an abstract class whose abstract method matches an interface
default it also implements. Does the default satisfy the abstract method?
**A:** No. It does not compile — "not abstract and does not override abstract
method." The abstract declaration in the class hierarchy outranks the default, so
the default isn't a candidate; the class is left with an unimplemented method.

**Q:** Are static interface methods inherited by implementing classes or
sub-interfaces?
**A:** No. Static interface methods belong only to the declaring interface; you
call them as `Interface.method()`. You cannot call them through an implementing
class, a subtype, or an instance.

**Q:** Can a sub-interface turn a default method back into an abstract one?
**A:** Yes — re-declare it abstract: `interface D extends A { String id(); }`
re-abstracts `A`'s default, forcing concrete implementors to provide a body.

**Q:** You make a colliding class method package-private and now get "attempting to
assign weaker access privileges." Why?
**A:** Default methods are implicitly `public`. A class method that "overrides" or
satisfies it must be at least as accessible; package-private is weaker, so it's
rejected. Relatedly, a package-private superclass method in a *different* package
doesn't override the default at all — no override relationship across the package
boundary.
