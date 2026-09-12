Если вы когда-нибудь заглядывали в исходники JDK, вы видели странную пару: интерфейс `Collection` и рядом абстрактный класс `AbstractCollection`. Интерфейс `List` - и `AbstractList`. `Map` - и `AbstractMap`.

Зачем два типа на одну сущность? Почему нельзя было обойтись чем-то одним?

Ответ - паттерн, который Джошуа Блох в *Effective Java* называет **скелетной реализацией** (skeletal implementation). Разберём его на живом примере: сначала увидим боль, потом придём к решению и посмотрим на ограничения.

## Наблюдение, с которого всё начинается

Возьмём небольшой интерфейс - хранилище заметок:

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

Семь методов. Любой класс с `implements Notes` обязан реализовать все семь.

Но присмотритесь к списку. Не все семь пунктов равноценны.

**Настоящих операций здесь три.** Чтобы реализовать `add`, `all` и `deleteAt`, нужно принять решение: *где физически лежат данные*. В `ArrayList`? В файле? В SQLite? Без этого решения написать их невозможно.

**Остальные четыре - производные.** Они выражаются через первые три и не требуют никаких новых знаний о хранилище:

| Производная операция | Выражается как |
|---|---|
| `count()` | `all().size()` |
| `isEmpty()` | `count() == 0` |
| `last()` | `all().get(count() - 1)` |
| `deleteAll()` | `deleteAt()` в цикле |

Обратите внимание на важную деталь: `count()` реализуется одинаково **для любого** хранилища. Берём `all()`, считаем длину. Данные в памяти, в файле, в сети - код не меняется.

Это и есть наблюдение, из которого растёт весь паттерн: методы интерфейса делятся на **примитивные** (требуют решения о реализации) и **производные** (выводятся из примитивных механически).

## Боль: что происходит без паттерна

Напишем две реализации.

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
        /* дописать строку в файл */ 
    }
    
    @Override public List<String> all() { 
        /* прочитать файл */ 
    }

    @Override public void deleteAt(int i) { 
        /* перезаписать файл */ 
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
        /* цикл deleteAt */ 
    }
}
```

Первая тройка методов различается по существу - так и должно быть, это разные хранилища.

А вот последняя четвёрка - дублирование. `return all().size()` будет написано столько раз, сколько у нас реализаций. Появится `SqliteNotes` - напишем в третий раз.

### Почему это не "просто некрасиво"

Копипаста опасна не только в плане эстетики. Посмотрите на `last()`:

```java
@Override public String last() {
    return all().get(count() - 1);   // список пуст => IndexOutOfBoundsException
}
```

Кейс с `IndexOutOfBoundException` не обработан. И он не обработан **в каждой копии независимо**. Автор `MemoryNotes` мог добавить проверку, автор `FileNotes` - забыть. Баг найден в одном классе, исправлен в одном классе, в остальных остался жить.

Каждая новая реализация - это новая возможность повторить ту же ошибку. Причём ошибку в коде, который к сути реализации не имеет никакого отношения.

## Решение: скелетная реализация

Идея прямая: реализовать производные операции **один раз**, а примитивные оставить абстрактными.

```java
/**
 * Скелетная реализация {@link Notes}.
 * Реализует производные операции через три примитива:
 * add(), all(), deleteAt().
 *
 * Внутренние вызовы (self-use) - читать перед переопределением:
 *   count()     => all()
 *   isEmpty()   => count()
 *   last()      => isEmpty(), all(), count()
 *   deleteAll() => count(), deleteAt()
 */
public abstract class AbstractNotes implements Notes {

    // add(), all(), deleteAt() остаются абстрактными:
    // решение о хранилище принимает подкласс

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
        // идём с конца: удаление сдвигает индексы
        for (int i = count() - 1; i >= 0; i--) {
            deleteAt(i);
        }
    }
}
```

Класс объявлен `abstract` - создать его нельзя, он намеренно неполный. Отсюда и название: скелет есть, реализации нет.

Теперь конкретные классы:

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

    // count() и deleteAll() переопределены не потому, что скелет неверен,
    // а потому, что у ArrayList они O(1) против O(n) у скелета
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
        /* дописать строку */ 
    }

    @Override 
    public List<String> all() { 
        /* прочитать файл */ 
    }

    @Override 
    public void deleteAt(int i) { 
        /* перезаписать файл */ 
    }
}
```

`FileNotes` - три метода вместо семи. При этом `count()`, `isEmpty()`, `last()`, `deleteAll()` полностью работоспособны, включая проверку на пустоту в `last()`, написанную и протестированную один раз.

Автор скелета не знал, что кто-то станет хранить заметки в файле. Ему и не нужно было знать - он опирался только на контракт трёх примитивов.

## Ключевой вопрос: зачем тогда интерфейс?

Eсли есть `AbstractNotes`, зачем вообще держать интерфейс `Notes`?  Оставим один абстрактный класс.

Ответ - в главном свойстве паттерна: **интерфейс задаёт тип, скелет остаётся необязательной помощью**.

Клиентский код всегда работает с интерфейсом:

```java
void render(Notes notes) { ... }
```

А теперь представьте разработчика, чей класс уже кого-то расширяет:

```java
public class SyncedNotes extends AbstractCustomRepository { ... }
```

В Java единственный слот `extends` занят. Если бы скелет был обязательным, этот код было бы не написать. С интерфейсом выход есть: `implements Notes` и семь методов вручную. Скучно - но возможно.

Именно поэтому в JDK пара `Collection` / `AbstractCollection`, а не один абстрактный класс. Наследоваться от `AbstractCollection` - удобство, реализовать `Collection` напрямую - право.

Есть и промежуточный вариант - **имитация множественного наследования**: скелет используется через приватный внутренний класс, а внешний класс пересылает в него вызовы.

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
    // остальное пересылается так же
}
```

Слот наследования отдан фреймворку, функциональность скелета при этом доступна.

## Что ещё даёт паттерн

**Реализация `equals`, `hashCode`, `toString`.** Интерфейс не может их предоставить - это запрет на уровне языка. Абстрактный класс может, выразив их через примитивы. Это одна из причин, по которой `AbstractList` не исчез после появления default-методов: он даёт всем спискам корректные `equals` и `hashCode`.

## Ограничения, о которых нужно знать

Паттерн не бесплатный. Четыре вещи стоит держать в голове.

### 1. Это по-прежнему наследование

Скелет не отменяет проблему хрупкого базового класса, а лишь ограничивает её. `last()` внутри вызывает `isEmpty()`, `isEmpty()` вызывает `count()`. Если подкласс переопределит `count()` неаккуратно, то сломается `last()`, которого он не касался.

Отсюда блок self-use в javadoc из примера выше. Это не документация "для порядка": без неё подкласс ломается непредсказуемо. И этот блок становится частью публичного контракта навсегда - поменяв `isEmpty()` с `count() == 0` на `all().isEmpty()`, вы сломаете подклассы, переопределившие только `count()`.

### 2. Производные реализации иногда неоптимальны

`count()` через `all().size()` в `FileNotes` - это чтение всего файла ради одного числа.

Скелет обязан быть универсальным, а универсальное почти всегда медленнее специализированного. Реальный риск в том, что скелет создаёт **иллюзию завершённости**, а реализующие классы радуются трём методам и не замечают, что получили O(n) там, где хранилище давало O(1). Поэтому в документации скелета стоит отдельно перечислять методы, которые имеет смысл переопределить ради производительности.

### 3. Два артефакта нужно держать в синхроне

Добавили метод в интерфейс - решите, примитив он или производный, и допишите в скелет. Добавили новый **абстрактный** метод в скелет - сломали все существующие подклассы. Паттерн облегчает эволюцию интерфейса, но сам эволюционирует хуже, чем интерфейс.

## Скелет или default-методы?

С Java 8 производные операции можно положить прямо в интерфейс:

```java
public interface Notes {
    void add(String text);
    List<String> all();
    void deleteAt(int index);

    default int count()       { return all().size(); }
    default boolean isEmpty() { return count() == 0; }
}
```

Отдельный класс не нужен, слот `extends` свободен. Но у default-методов есть жёсткие границы - из-за них скелеты не устарели:

- **Нет состояния.** У интерфейса нет полей. Нельзя закешировать `count`, нельзя лениво инициализировать, нельзя хранить счётчик.
- **Нельзя реализовать `equals` / `hashCode` / `toString`.** Прямой запрет языка.
- **Нельзя объявить `protected`-хелпер.** Default-методы публичны и попадают в API интерфейса, хотите вы этого или нет.
- **Нет конструктора,** а значит, нет места для проверки инвариантов.

Практическое правило:

> Производные операции **без состояния** => default-методы в интерфейсе.
> Всё, где нужны поля, `Object`-методы, `protected`-хелперы или конструктор => скелетный класс.

Часто применяют оба сразу: тривиальное в интерфейс, тяжёлое в скелет.