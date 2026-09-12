`Comparable<T>` is an interface with a single method, `int compareTo(T o)`. By implementing it, a class says that its instances have a **natural order**, and it gets `Arrays.sort()`, `Collections.sort()`, `TreeSet`, `TreeMap`, `binarySearch`, `Collections.max/min`, and stream sorting for free.

The contract requires antisymmetry, transitivity, and consistency for equal objects. Being consistent with `equals` is strongly recommended but not required. The method returns a **sign**, not the exact values -1/0/1.

---

## What implementing `Comparable` gives you

One line, `implements Comparable<T>`, plugs your class into the whole sorting infrastructure of the JDK:

```java
public class WordList {
    public static void main(String[] args) {
        Set<String> s = new TreeSet<>();
        Collections.addAll(s, args);
        System.out.println(s);   // sorted, no duplicates
    }
}
```

This works because `String` implements `Comparable`. Almost all value classes in the JDK, and **all** enums (through `Enum`, where the order is the order the constants are declared in), implement it too.

---

## The `compareTo` contract

It returns a negative number, zero, or a positive number if the object is less than, equal to, or greater than the argument. It throws `ClassCastException` if the types cannot be compared.

Let `sgn(x)` mean the sign function: -1, 0, or 1.

### Required conditions

**1. Antisymmetry**

```
sgn(x.compareTo(y)) == -sgn(y.compareTo(x))
```

This means: `x.compareTo(y)` throws an exception if and only if `y.compareTo(x)` throws one.

**2. Transitivity**

```
x.compareTo(y) > 0 && y.compareTo(z) > 0  ⟹  x.compareTo(z) > 0
```

**3. Consistency for equal objects**

```
x.compareTo(y) == 0  ⟹  sgn(x.compareTo(z)) == sgn(y.compareTo(z))  for any z
```

If two objects are equal in terms of order, they must compare the same way against everything else.

### Strongly recommended (but not required)

```
(x.compareTo(y) == 0) == x.equals(y)
```

---

## Being consistent with `equals` — the key point

You **may** break this rule, but then the class must be documented. The standard wording used in the JDK:

> Note: this class has a natural ordering that is inconsistent with equals.

### Why this matters

Collections fall into two groups based on what they use to decide whether two things are "the same":

| Collection | Uses |
|---|---|
| `HashSet`, `HashMap`, `ArrayList.contains` | `equals` |
| `TreeSet`, `TreeMap`, `Collections.binarySearch` | `compareTo` |

If these two mechanisms disagree, the same set of elements will give different results in different collections.

### The classic example — `BigDecimal`

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");

a.equals(b);        // false — equals takes the scale into account
a.compareTo(b);     // 0 — compareTo only compares the numeric value

Set<BigDecimal> hashSet = new HashSet<>();
hashSet.add(a);
hashSet.add(b);
hashSet.size();     // 2 — two different elements according to equals

Set<BigDecimal> treeSet = new TreeSet<>();
treeSet.add(a);
treeSet.add(b);
treeSet.size();     // 1 — the second element is "equal" to the first by compareTo
```

Strictly speaking, `TreeSet` does not break the `Set` contract — it is documented as working through `compareTo`. But for someone reading the code, it is a surprise.

---

## `compareTo` returns a **sign**, not -1/0/1

The contract only guarantees the sign of the result. The exact value is an implementation detail.

```java
"a".compareTo("d");   // -3, not -1 ('a'=97, 'd'=100)
```

`String.compareTo()` returns the difference between character codes. This gives you a practical rule:

```java
// BROKEN — fails on String and many other classes
if (a.compareTo(b) == -1) { ... }

// CORRECT
if (a.compareTo(b) < 0) { ... }
```

The other side of this: **your** implementation does not have to return -1/0/1 either — the right sign is enough. That is why `Integer.compare(x, y)` is a valid building block, even though it may return any value internally.

The contract itself is written using `sgn(...)` for exactly this reason: `-3` and `3` are a valid antisymmetric pair.

---

## The inheritance limitation

You cannot extend a concrete class with a new comparison component and keep the contract. If you add a field in a subclass and use it in `compareTo`, you break antisymmetry when you compare a subclass instance with a parent class instance.

This is the same problem `equals` has (Item 10).

**The fix is composition instead of inheritance.** Do not extend: keep an instance of the parent class as a field and add a view method that returns it. Then you are free to define any `compareTo` you want on the new class without breaking the original one's contract.

---

## How to write `compareTo`

### 1. Never use subtraction

```java
// BROKEN — overflow
static Comparator<Integer> bad = (a, b) -> a - b;
```

With `a = Integer.MAX_VALUE` and `b = -1`, the result overflows and becomes negative — the order flips. The trick is also wrong for floating point numbers (`NaN`, `-0.0`).

### 2. Use the static `compare` methods

Since Java 7, all boxed primitives have a safe `compare`:

```java
Integer.compare(x, y)
Long.compare(x, y)
Double.compare(x, y)     // handles NaN and -0.0 correctly
Boolean.compare(x, y)    // false < true
```

For a class with several fields, compare them **from the most important to the least**, stopping at the first difference:

```java
public int compareTo(PhoneNumber pn) {
    int result = Short.compare(areaCode, pn.areaCode);
    if (result == 0) {
        result = Short.compare(prefix, pn.prefix);
        if (result == 0) {
            result = Short.compare(lineNum, pn.lineNum);
        }
    }
    return result;
}
```

The order of the fields defines what the sorting means — it is part of the class's public contract, so document it.

### 3. Comparator construction methods (Java 8+)

Easier to read, at the cost of some performance:

```java
private static final Comparator<PhoneNumber> COMPARATOR =
    Comparator.comparingInt((PhoneNumber pn) -> pn.areaCode)
              .thenComparingInt(pn -> pn.prefix)
              .thenComparingInt(pn -> pn.lineNum);

@Override
public int compareTo(PhoneNumber pn) {
    return COMPARATOR.compare(this, pn);
}
```

**Two things people ask about:**

1. **The explicit type in the first lambda** — `(PhoneNumber pn)`. Java's type inference cannot manage without this hint, because at the point where `comparingInt` is parsed the target type is not known yet. In the following `thenComparingInt` calls the type is already known, so no annotation is needed.
2. **The comparator lives in a static final field** — so the chain is built once, not on every `compareTo` call.

---

## `Comparable` vs `Comparator`

| | `Comparable` | `Comparator` |
|---|---|---|
| Where it lives | Inside the class | Outside |
| Method | `compareTo(T o)` | `compare(T a, T b)` |
| How many orders | Exactly one | As many as you like |
| Functional interface | Technically yes, but not used for lambdas | Yes, a typical one |
| When to use | There is an obvious natural order | Alternative or context-dependent sorting |

**How to choose:** if the natural order is not obvious, do not implement `Comparable`. For a `Person`, ordering "by name" is no more justified than "by age"; it is better to provide named comparators:

```java
public static final Comparator<Person> BY_NAME = Comparator.comparing(Person::getName);
public static final Comparator<Person> BY_AGE  = Comparator.comparingInt(Person::getAge);
```

You also need a `Comparator` when:
- the class is not yours and you cannot change it;
- you need an order that goes against the natural one;
- the order depends on context (locale, user settings).

---

## Practical notes

**No type check needed.** Unlike `equals(Object)`, `compareTo(T)` is typed with a generic — the compiler checks the type for you. You do not need an explicit `instanceof` at the start of the method. If an argument of the wrong type does get through (raw types), a `ClassCastException` is the correct behaviour.

**`null` is not handled.** `compareTo(null)` should throw a `NullPointerException` — this follows naturally from the method reading the argument's fields. No explicit check is needed.

**Object fields** should be compared by delegating to their own `compareTo`, not by writing your own logic.

**Reverse order** — use `Comparator.reverseOrder()` or `cmp.reversed()`. Do not flip the sign by hand (`-compareTo(...)`): for `Integer.MIN_VALUE` the unary minus gives back the same `Integer.MIN_VALUE`, so the sign does not change.

**Compatibility with sorting.** `Arrays.sort` and `Collections.sort` use TimSort, which checks transitivity and may throw `IllegalArgumentException: Comparison method violates its general contract!` on a broken implementation. This is a common production bug — usually caused by subtraction with overflow.

---

## Common interview questions

**What is a natural order?**
The order defined by the class itself through `compareTo`. It is used by default in `Arrays.sort`, `TreeSet`, `TreeMap`, and others when no `Comparator` is passed explicitly.

**How is `Comparable` different from `Comparator`?**
In short: one built-in order versus any number of external ones.

**Does `compareTo` have to be consistent with `equals`?**
No, but it is strongly recommended. Otherwise the class behaves differently in hash-based and tree-based collections, and you have to document that. `BigDecimal` is the classic example of a violation.

**Why is `a - b` in a comparator a bug?**
Integer overflow. `Integer.MAX_VALUE - (-1)` gives a negative number and the order flips. Use `Integer.compare`.

**What does `compareTo` return?**
A negative number, zero, or a positive number. The exact value is not guaranteed — only compare it against zero.

**Can `compareTo` throw an exception?**
Yes: `ClassCastException` for an incomparable type, `NullPointerException` for `null`. Both are correct behaviour under the contract.

**What happens if `compareTo` breaks transitivity?**
Sorting can give a wrong result or throw `IllegalArgumentException` (TimSort detects some violations). `TreeMap` can lose elements.

**Do enums implement `Comparable`?**
Yes, through `java.lang.Enum`. The order is by `ordinal()`, that is, the order the constants are declared in. The method is `final` and cannot be overridden.

**Can you extend a class and add a field to `compareTo`?**
Not correctly — antisymmetry breaks. Use composition.

---

## Checklist: "I am writing `compareTo`"

- `implements Comparable<MyClass>` — with the type parameter, not raw
- Fields compared from the most important to the least
- Uses `Integer.compare` / `Double.compare` and friends, not subtraction
- Returns a sign; the result is never compared against -1 or 1
- Antisymmetry and transitivity checked
- Decided whether the order is consistent with `equals`; if not, documented it
- The field order is described in the javadoc
- No `instanceof` and no `null` checks — the generic and the NPE handle it
- If the class can be extended, thought through the inheritance limitation