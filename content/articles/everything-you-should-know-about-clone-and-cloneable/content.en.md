`Cloneable` is a marker interface with no methods. It does not declare `clone()`. Instead, it changes how `protected Object.clone()` behaves: if a class implements `Cloneable`, then `super.clone()` returns a field-by-field copy; if it does not, a `CloneNotSupportedException` is thrown.

The mechanism is broken by design: it clashes with `final` fields, throws a pointless checked exception, needs casts, and makes a shallow copy by default. **For new code, prefer a copy constructor or a copy factory.**

---

## Why `Cloneable` is considered a design mistake

| A normal interface | `Cloneable` |
|---|---|
| Declares methods the class can do | Declares nothing |
| Says "what the class can do" | Changes how a protected superclass method behaves |
| The class is responsible for the implementation | The implementation is magic inside `Object` |

The key oddity: `Cloneable` does not give you a public `clone()`. Just writing `implements Cloneable` does **not** let anyone clone your object from outside — `clone()` is still `protected`. You also have to override it and make it `public`.

---

## The `clone()` contract

The spec for `Object.clone()` is loose:

```java
x.clone() != x                          // true
x.clone().getClass() == x.getClass()    // "usually" true, not required
x.clone().equals(x)                     // "usually" true, not required
```

Plus a convention: the object must come from `super.clone()`, not from a constructor.

**Why `super.clone()` and not `new`?** If class `B extends A`, and `A.clone()` returns `new A(...)`, then `B.clone()` gets an object of type `A` — and the cast to `B` fails with `ClassCastException`. The chain of `super.clone()` calls reaches `Object.clone()`, which creates an object of the **actual** class using a runtime mechanism.

---

## Case 1: only primitives and immutable fields

```java
public final class PhoneNumber implements Cloneable {
    private final int areaCode;
    private final int prefix;
    private final int lineNum;

    public PhoneNumber(int areaCode, int prefix, int lineNum) {
        this.areaCode = areaCode;
        this.prefix = prefix;
        this.lineNum = lineNum;
    }

    @Override
    public PhoneNumber clone() {            // covariant return type (Java 5+)
        try {
            return (PhoneNumber) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();     // cannot happen: we are Cloneable
        }
    }
}
```

Three points people ask about:

1. **Covariant return type** — we return `PhoneNumber`, not `Object`. The caller does not need a cast. Available since Java 5.
2. **The cast inside the method** — `super.clone()` is declared to return `Object`, so a cast is needed, but it is always safe.
3. **The `try/catch` around `CloneNotSupportedException`** — this is a checked exception that will never be thrown here. We wrap it in an `AssertionError`. Pure noise in the code — and that is an argument against `clone()`.

---

## Case 2: a mutable field — the shallow copy problem

This is the main trap. `super.clone()` copies fields **bit by bit**: for a reference field, it copies the reference, not the object.

### Broken version

```java
public class Stack implements Cloneable {
    private Object[] elements;
    private int size = 0;

    @Override
    public Stack clone() {
        try {
            return (Stack) super.clone();   // BUG: the array is shared!
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```

The original and the clone share one `elements` array. A `push` on one stack breaks the other — the invariant is gone.

### Correct version

```java
@Override
public Stack clone() {
    try {
        Stack result = (Stack) super.clone();
        result.elements = elements.clone();   // clone the array too
        return result;
    } catch (CloneNotSupportedException e) {
        throw new AssertionError();
    }
}
```

### A consequence: `clone()` does not work with `final`

You cannot make `elements` `final` — otherwise `result.elements = ...` will not compile. You have to give up immutability to support `clone()`. This is a structural flaw, not a style question.

---

## Case 3: a deep structure — recursive deep copy

If a field is an array of references to objects that themselves hold references, `array.clone()` is not enough: it copies the array but not the nodes inside.

```java
public class HashTable implements Cloneable {
    private Entry[] buckets;

    private static class Entry {
        final Object key;
        Object value;
        Entry next;

        Entry(Object key, Object value, Entry next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        // recursive copy of the linked list
        Entry deepCopy() {
            return new Entry(key, value,
                    next == null ? null : next.deepCopy());
        }
    }

    @Override
    public HashTable clone() {
        try {
            HashTable result = (HashTable) super.clone();
            result.buckets = new Entry[buckets.length];
            for (int i = 0; i < buckets.length; i++) {
                if (buckets[i] != null) {
                    result.buckets[i] = buckets[i].deepCopy();
                }
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```

**A point that marks a strong candidate:** the recursive `deepCopy()` uses one stack frame per list element and can cause a `StackOverflowError` on long buckets. The iterative version:

```java
Entry deepCopy() {
    Entry result = new Entry(key, value, next);
    for (Entry p = result; p.next != null; p = p.next) {
        p.next = new Entry(p.next.key, p.next.value, p.next.next);
    }
    return result;
}
```

One more detail: `result.buckets = new Entry[buckets.length]` creates a **new empty** array instead of cloning the old one — otherwise we would first copy someone else's references and then overwrite them anyway.

---

## More pitfalls

### Do not call overridable methods from `clone()`

If `clone()` calls a non-final method, the subclass runs **before** its own state has been fixed up, and you get a broken clone — because of dynamic dispatch. Any helper method called from `clone()` should be `final` or `private`.

This is the same rule as "do not call overridable methods from a constructor".

### A public `clone()` should not declare `throws`

Even though `Object.clone()` declares `CloneNotSupportedException`, your public `clone()` should swallow it — otherwise callers have to write a pointless `try/catch`.

### Inheritance

A class designed for inheritance should not implement `Cloneable`. Two options:

1. Mimic `Object`: a `protected clone()` that declares `throws CloneNotSupportedException`, leaving the choice to subclasses.
2. Block it completely:
   ```java
   @Override
   protected final Object clone() throws CloneNotSupportedException {
       throw new CloneNotSupportedException();
   }
   ```

### Thread safety

If a class is thread-safe, its `clone()` must be synchronized too. `Object.clone()` is not synchronized.

### An empty `clone()` for abstract classes

If an abstract class implements `Cloneable`, the whole burden of getting it right falls on the subclasses — and they may not even know about it.

---

## The right alternative: copy constructor / copy factory

```java
// Copy constructor (conversion constructor)
public Yum(Yum yum) {
    this.field = yum.field;
    this.list  = new ArrayList<>(yum.list);
}

// Copy factory (conversion factory)
public static Yum newInstance(Yum yum) {
    return new Yum(yum);
}
```

### Why this is better

| Problem with `clone()` | Copy constructor |
|---|---|
| Object creation outside the language | An ordinary constructor |
| Clashes with `final` fields | `final` works fine |
| Checked `CloneNotSupportedException` | No extra exceptions |
| Needs casts | Type-safe |
| Returns an object of the same class | Can accept an interface type |

### About that last point — the trump card in an interview

A copy constructor can take an **interface**, not a concrete class. This lets you convert one implementation into another:

```java
TreeSet<String> tree = new TreeSet<>(...);
HashSet<String> hash = new HashSet<>(tree);   // implementation conversion
```

You cannot do this with `clone()`: it always returns a `TreeSet`.

This is exactly why every collection in the JDK provides a conversion constructor that takes a `Collection`.

---

## The bottom line

1. For a **new** class — do not implement `Cloneable`. Use a copy constructor or a copy factory.
2. Implement `Cloneable` only if you **extend** a class that already requires it and there is no reasonable alternative.
3. For **arrays**, `clone()` is the idiomatic and preferred way to copy. This is the one convincing exception.
   ```java
   int[] copy = original.clone();   // no cast: arrays have a covariant clone()
   ```
4. Interfaces should never extend `Cloneable`.

---

## Common interview questions

**What is the difference between a shallow and a deep copy?**
Shallow — field values are copied, including references; nested objects are shared. Deep — nested mutable objects are copied recursively. `Object.clone()` does a shallow copy by default.

**Why is `Cloneable` called a marker interface, and what is unusual about it?**
It has no methods. What is unusual is that it does not declare a capability — it changes the behaviour of a superclass method. There is almost no other case like it in the JDK.

**What happens if you call `clone()` without implementing `Cloneable`?**
`Object.clone()` throws `CloneNotSupportedException`.

**Why is `CloneNotSupportedException` checked when it should not be?**
A historical decision. If a class implements `Cloneable`, it will never be thrown, so being checked only adds noise. Bloch calls this an API flaw.

**Can you clone an object that has a `final` mutable field?**
Not properly: you cannot assign a new deep copy to it. You have to drop the `final`.

**Why is `clone()` declared `protected` in `Object`?**
So that cloning is not forced on every class. A class must explicitly decide whether to support it and widen the visibility to `public`.

**What does `super.clone()` return in a subclass?**
An object of the class the object actually is — not the class whose code contains the call.
If you call `clone()` on an object of class `B` (a subclass of `A`), then even though `super.clone()` runs inside a method of class `A`, you get back a `B`. `Object.clone()` looks at `this.getClass()` — the real type of the object — and copies all of its fields, including the ones declared in `B`. That is why the cast to `B` is safe.
And that is exactly why you must not create the copy with a constructor: `new A(...)` inside `A.clone()` always gives an `A`, and the subclass gets a `ClassCastException`.

**What are the alternatives to `clone()` for a deep copy?**
A copy constructor, a copy factory, serialization (slow, requires `Serializable`), or third-party libraries. For value objects — make the class immutable, and then you do not need to copy at all.

---

## Checklist: "if I really am writing `clone()`"

- `implements Cloneable`
- A `public` override with a covariant return type
- Start with `super.clone()`, never `new`
- `CloneNotSupportedException` caught and wrapped in an `AssertionError`
- Every mutable reference field copied deeply
- Mutable fields are not marked `final`
- No calls to overridable methods
- Recursion depth checked (or replaced with iteration)
- Synchronization added if the class is thread-safe
- No `throws` in the signature