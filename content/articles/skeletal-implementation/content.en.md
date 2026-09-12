If you have ever looked into the JDK sources, you have seen a strange pair: the interface `Collection` and, right next to it, the abstract class `AbstractCollection`. The interface `List` - and `AbstractList`. `Map` - and `AbstractMap`.

Why two types for one thing? Why was one not enough?

The answer is a pattern that Joshua Bloch calls a **skeletal implementation** in *Effective Java*. Let's look at it on a live example: first we will see the pain, then we will come to the solution and look at the limits.

## The observation that starts it all

Take a small interface - a store for notes:

```java
public interface Notes {

    void add(String text);
    List<String> all();
    void deleteAt(int index);

    int count();
    boolean isEmpty();
    String last();
    void deleteAll();
}
```

Seven methods. Any class with `implements Notes` must implement all seven.

But look closely at the list. Not all seven are equal.

**There are only three real operations here.** To implement `add`, `all` and `deleteAt` you must make a decision: *where the data physically lives*. In an `ArrayList`? In a file? In SQLite? Without that decision you cannot write them.

**The other four are derived.** They can be expressed through the first three and need no new knowledge about the storage:

| Derived operation | Expressed as |
|---|---|
| `count()` | `all().size()` |
| `isEmpty()` | `count() == 0` |
| `last()` | `all().get(count() - 1)` |
| `deleteAll()` | `deleteAt()` in a loop |

Note one important detail: `count()` is implemented the same way **for any** storage. Take `all()`, count the length. Data in memory, in a file, over the network - the code does not change.

This is the observation the whole pattern grows from: the methods of an interface split into **primitive** ones (they need a decision about the implementation) and **derived** ones (they follow from the primitive ones mechanically).

## The pain: what happens without the pattern

Let's write two implementations.

```java
public class MemoryNotes implements Notes {
    private final List<String> list = new ArrayList<>();

    @Override public void add(String text) { 
        list.add(text); 
    }

    @Override public List<String> all() {
        return List.copyOf(list); 
    }

    @Override public void deleteAt(int i) { 
        list.remove(i); 
    }

    @Override public int count() { 
        return list.size(); 
    }

    @Override public boolean isEmpty() { 
        return list.isEmpty(); 
    }

    @Override public String last() { 
        return list.get(list.size() - 1); 
    }

    @Override public void deleteAll() {
        list.clear(); 
    }
}
```

```java
public class FileNotes implements Notes {
    private final Path file;

    @Override public void add(String text) { 
        /* append a line to the file */ 
    }
    
    @Override public List<String> all() { 
        /* read the file */ 
    }

    @Override public void deleteAt(int i) { 
        /* rewrite the file */ 
    }

    @Override public int count() { 
        return all().size(); 
    }

    @Override public boolean isEmpty() { 
        return count() == 0; 
    }

    @Override public String last() { 
        return all().get(count() - 1); 
    }

    @Override public void deleteAll() { 
        /* a loop of deleteAt */ 
    }
}
```

The first three methods differ in essence - and that is how it should be, these are different storages.

But the last four are duplication. `return all().size()` will be written as many times as we have implementations. A `SqliteNotes` shows up - we write it a third time.

### Why this is not "just ugly"

Copy-paste is dangerous not only for beauty. Look at `last()`:

```java
@Override public String last() {
    return all().get(count() - 1);   // the list is empty => IndexOutOfBoundsException
}
```

The `IndexOutOfBoundException` case is not handled. And it is not handled **in every copy separately**. The author of `MemoryNotes` could add a check, the author of `FileNotes` could forget it. The bug is found in one class, fixed in one class, and keeps living in the rest.

Every new implementation is a new chance to repeat the same mistake. And it is a mistake in code that has nothing to do with the essence of the implementation.

## The solution: a skeletal implementation

The idea is direct: implement the derived operations **once**, and leave the primitive ones abstract.

```java
/**
 * Skeletal implementation of {@link Notes}.
 * Implements the derived operations through three primitives:
 * add(), all(), deleteAt().
 *
 * Internal calls (self-use) - read before overriding:
 *   count()     => all()
 *   isEmpty()   => count()
 *   last()      => isEmpty(), all(), count()
 *   deleteAll() => count(), deleteAt()
 */
public abstract class AbstractNotes implements Notes {

    // add(), all(), deleteAt() stay abstract:
    // the subclass makes the decision about the storage

    @Override
    public int count() {
        return all().size();
    }

    @Override
    public boolean isEmpty() {
        return count() == 0;
    }

    @Override
    public String last() {
        if (isEmpty()) {
            throw new NoSuchElementException("notes are empty");
        }
        return all().get(count() - 1);
    }

    @Override
    public void deleteAll() {
        // go from the end: removing shifts the indexes
        for (int i = count() - 1; i >= 0; i--) {
            deleteAt(i);
        }
    }
}
```

The class is declared `abstract` - you cannot create it, it is incomplete on purpose. That is where the name comes from: there is a skeleton, but no implementation.

Now the concrete classes:

```java
public final class MemoryNotes extends AbstractNotes {
    private final List<String> list = new ArrayList<>();

    @Override 
    public void add(String text) { 
        list.add(text); 
    }
    
    @Override 
    public List<String> all() { 
        return List.copyOf(list); 
    }

    @Override 
    public void deleteAt(int i) { 
        list.remove(i); 
    }

    // count() and deleteAll() are overridden not because the skeleton is wrong,
    // but because on an ArrayList they are O(1) against O(n) in the skeleton
    @Override 
    public int count() { 
        return list.size();
    }

    @Override 
    public void deleteAll() { 
        list.clear(); 
    }
}
```

```java
public final class FileNotes extends AbstractNotes {
    private final Path file;

    public FileNotes(Path file) { 
        this.file = file; 
    }

    @Override 
    public void add(String text) { 
        /* append a line */ 
    }

    @Override 
    public List<String> all() { 
        /* read the file */ 
    }

    @Override 
    public void deleteAt(int i) { 
        /* rewrite the file */ 
    }
}
```

`FileNotes` - three methods instead of seven. And `count()`, `isEmpty()`, `last()`, `deleteAll()` work fully, including the empty check in `last()`, written and tested once.

The author of the skeleton did not know that someone would store notes in a file. And he did not need to know it - he relied only on the contract of the three primitives.

## The key question: why keep the interface then?

If we have `AbstractNotes`, why keep the interface `Notes` at all? Let's leave only the abstract class.

The answer is in the main property of the pattern: **the interface defines the type, the skeleton stays optional help**.

Client code always works with the interface:

```java
void render(Notes notes) { ... }
```

And now imagine a developer whose class already extends something:

```java
public class SyncedNotes extends AbstractCustomRepository { ... }
```

In Java the single `extends` slot is taken. If the skeleton were required, this code could not be written. With an interface there is a way out: `implements Notes` and seven methods by hand. Boring - but possible.

This is exactly why the JDK has the pair `Collection` / `AbstractCollection` and not one abstract class. Extending `AbstractCollection` is a convenience, implementing `Collection` directly is a right.

There is also a middle option - **simulated multiple inheritance**: the skeleton is used through a private inner class, and the outer class forwards the calls to it.

```java
public class SyncedNotes extends AbstractCustomRepository implements Notes {

    private final Notes impl = new AbstractNotes() {
        @Override public void add(String text) { /* ... */ }
        @Override public List<String> all()    { /* ... */ }
        @Override public void deleteAt(int i)  { /* ... */ }
    };

    @Override public void add(String text) { impl.add(text); }
    @Override public List<String> all()    { return impl.all(); }
    @Override public int count()           { return impl.count(); }
    // the rest is forwarded the same way
}
```

The inheritance slot is given to the framework, and the functionality of the skeleton is still available.

## What else the pattern gives

**An implementation of `equals`, `hashCode`, `toString`.** An interface cannot provide them - this is a ban at the language level. An abstract class can, by expressing them through the primitives. This is one of the reasons why `AbstractList` did not disappear after default methods appeared: it gives all lists a correct `equals` and `hashCode`.

## Limits you need to know about

The pattern is not free. Four things are worth keeping in mind.

### 1. It is still inheritance

The skeleton does not remove the fragile base class problem, it only limits it. Inside, `last()` calls `isEmpty()`, and `isEmpty()` calls `count()`. If a subclass overrides `count()` carelessly, then `last()` breaks, even though the subclass never touched it.

That is where the self-use block in the javadoc from the example above comes from. This is not documentation written "for order": without it a subclass breaks in unpredictable ways. And this block becomes a part of the public contract forever - if you change `isEmpty()` from `count() == 0` to `all().isEmpty()`, you will break the subclasses that overrode only `count()`.

### 2. Derived implementations are sometimes not optimal

`count()` through `all().size()` in `FileNotes` means reading the whole file for one number.

The skeleton has to be universal, and universal is almost always slower than specialized. The real risk is that the skeleton creates an **illusion of completeness**, and the implementing classes are happy about the three methods and do not notice that they got O(n) where the storage could give O(1). So the documentation of the skeleton should list separately the methods that are worth overriding for performance.

### 3. Two artifacts must be kept in sync

You added a method to the interface - decide whether it is primitive or derived, and add it to the skeleton. You added a new **abstract** method to the skeleton - you broke all existing subclasses. The pattern makes it easier for the interface to evolve, but the pattern itself evolves worse than the interface.

## A skeleton or default methods?

Since Java 8 the derived operations can be put right into the interface:

```java
public interface Notes {
    void add(String text);
    List<String> all();
    void deleteAt(int index);

    default int count()       { return all().size(); }
    default boolean isEmpty() { return count() == 0; }
}
```

No separate class is needed, the `extends` slot is free. But default methods have hard limits - because of them skeletons are not outdated:

- **No state.** An interface has no fields. You cannot cache `count`, you cannot init it lazily, you cannot keep a counter.
- **You cannot implement `equals` / `hashCode` / `toString`.** A direct ban of the language.
- **You cannot declare a `protected` helper.** Default methods are public and get into the API of the interface, whether you want it or not.
- **No constructor,** which means there is no place to check the invariants.

A practical rule:

> Derived operations **without state** => default methods in the interface.
> Everything that needs fields, `Object` methods, `protected` helpers or a constructor => a skeletal class.

Often both are used at once: the trivial things in the interface, the heavy ones in the skeleton.