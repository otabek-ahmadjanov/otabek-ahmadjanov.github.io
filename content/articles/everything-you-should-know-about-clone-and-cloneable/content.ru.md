`Cloneable` - маркерный интерфейс без методов. Он не объявляет `clone()`, а меняет поведение `protected Object.clone()`: если класс реализует `Cloneable`, `super.clone()` возвращает пофайловую копию; если нет - бросается `CloneNotSupportedException`.

Механизм сломан по дизайну: конфликтует с `final` полями, бросает лишнее checked-исключение, требует приведения типов, делает shallow copy по умолчанию. **Для нового кода предпочтительны copy constructor и copy factory.**

---

## Почему `Cloneable` считают ошибкой дизайна

| Обычный интерфейс | `Cloneable` |
|---|---|
| Объявляет методы, которые класс умеет | Не объявляет ничего |
| Говорит "что класс может делать" | Меняет поведение protected-метода суперкласса |
| Реализация - обязанность класса | Реализация - магия внутри `Object` |

Ключевой парадокс: `Cloneable` не даёт публичного `clone()`. Просто написав `implements Cloneable`, вы **не** получаете возможность клонировать объект извне - `clone()` остаётся `protected`. Нужно ещё переопределить его как `public`.

---

## Контракт `clone()`

Спецификация `Object.clone()` формулирует его нестрого:

```java
x.clone() != x                          // true
x.clone().getClass() == x.getClass()    // "обычно" true, не обязательно
x.clone().equals(x)                     // "обычно" true, не обязательно
```

Плюс соглашение: объект должен создаваться через `super.clone()`, а не через конструктор.

**Почему `super.clone()`, а не `new`?** Если класс `B extends A`, и `A.clone()` вернёт `new A(...)`, то `B.clone()` получит объект типа `A` - и приведение к `B` упадёт с `ClassCastException`. Цепочка `super.clone()` доходит до `Object.clone()`, который создаёт объект **фактического** класса через рантайм-механизм.

---

## Случай 1: только примитивы и immutable-поля

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
    public PhoneNumber clone() {            // ковариантный возвращаемый тип (Java 5+)
        try {
            return (PhoneNumber) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();     // не может случиться: мы Cloneable
        }
    }
}
```

Три момента, которые спрашивают:

1. **Ковариантный возвращаемый тип** - возвращаем `PhoneNumber`, а не `Object`. Клиенту не нужен каст. Доступно с Java 5.
2. **Приведение внутри метода** - `super.clone()` объявлен как `Object`, поэтому каст нужен, но он всегда безопасен.
3. **`try/catch` вокруг `CloneNotSupportedException`** - это checked-исключение, которое здесь никогда не выбросится. Оборачиваем в `AssertionError`. Чистый шум в коде - и это аргумент против `clone()`.

---

## Случай 2: изменяемое поле - проблема shallow copy

Это главная ловушка. `super.clone()` копирует поля **побитово**: для ссылочного поля копируется ссылка, а не объект.

### Сломанная версия

```java
public class Stack implements Cloneable {
    private Object[] elements;
    private int size = 0;

    @Override
    public Stack clone() {
        try {
            return (Stack) super.clone();   // ОШИБКА: массив общий!
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```

Оригинал и клон делят один массив `elements`. `push` в одном стеке испортит другой - инвариант нарушен.

### Правильная версия

```java
@Override
public Stack clone() {
    try {
        Stack result = (Stack) super.clone();
        result.elements = elements.clone();   // рекурсивно клонируем массив
        return result;
    } catch (CloneNotSupportedException e) {
        throw new AssertionError();
    }
}
```

### Следствие: `clone()` несовместим с `final`

Полю `elements` нельзя поставить `final` - иначе `result.elements = ...` не скомпилируется. Приходится жертвовать иммутабельностью ради `clone()`. Это структурный изъян, не вопрос стиля.


---

## Случай 3: глубокая структура - рекурсивный deep copy

Если поле - массив ссылок на объекты, которые сами содержат ссылки, `array.clone()` не спасёт: он скопирует массив, но не узлы внутри.

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

        // рекурсивная копия связного списка
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

**Замечание, которое отличает сильного кандидата:** рекурсивный `deepCopy()` тратит один кадр стека на каждый элемент списка и может дать `StackOverflowError` на длинных бакетах. Итеративный вариант:

```java
Entry deepCopy() {
    Entry result = new Entry(key, value, next);
    for (Entry p = result; p.next != null; p = p.next) {
        p.next = new Entry(p.next.key, p.next.value, p.next.next);
    }
    return result;
}
```

Ещё один нюанс: `result.buckets = new Entry[buckets.length]` создаёт **новый пустой** массив, а не клонирует старый - иначе мы бы сначала скопировали чужие ссылки, а потом их перезаписали.

---

## Ещё подводные камни

### Не вызывайте переопределяемые методы из `clone()`

Если `clone()` вызовет нефинальный метод, подкласс отработает **до** того, как его собственное состояние восстановлено, - получится повреждённый клон, из-за динамической диспетчеризации. Все вспомогательные методы, вызываемые из `clone()`, должны быть `final` или `private`.

Та же логика, что и запрет на вызов переопределяемых методов из конструктора.

### Публичный `clone()` не должен объявлять `throws`

Хотя `Object.clone()` объявляет `CloneNotSupportedException`, ваш публичный `clone()` должен его проглатывать - иначе клиенты вынуждены писать бессмысленный `try/catch`.

### Наследование

Класс, спроектированный для наследования, не должен реализовывать `Cloneable`. Два варианта:

1. Повторить поведение `Object`: `protected clone()` с `throws CloneNotSupportedException`, дав подклассам свободу выбора.
2. Заблокировать вовсе:
   ```java
   @Override
   protected final Object clone() throws CloneNotSupportedException {
       throw new CloneNotSupportedException();
   }
   ```

### Потокобезопасность

Если класс потокобезопасен, его `clone()` тоже нужно синхронизировать. `Object.clone()` не синхронизирован.

### Пустой `clone()` для абстракций

Если `Cloneable` реализует абстрактный класс, вся тяжесть корректной реализации ложится на подклассы - они могут о ней не знать.

---

## Правильная альтернатива: copy constructor / copy factory

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

### Почему это лучше

| Проблема `clone()` | Copy constructor |
|---|---|
| Внеязыковой механизм создания объекта | Обычный конструктор |
| Конфликт с `final` полями | `final` работает нормально |
| Checked `CloneNotSupportedException` | Никаких лишних исключений |
| Требует приведения типов | Типобезопасен |
| Возвращает объект того же класса | Может принимать интерфейсный тип |

### Про последний пункт - козырь на интервью

Copy constructor может принимать **интерфейс**, а не конкретный класс. Это позволяет конвертировать реализацию:

```java
TreeSet<String> tree = new TreeSet<>(...);
HashSet<String> hash = new HashSet<>(tree);   // конверсия реализации
```

С `clone()` так нельзя: он всегда вернёт `TreeSet`.

Именно поэтому все коллекции в JDK предоставляют conversion constructor, принимающий `Collection`.

---

## Итоговое правило

1. Для **нового** класса - не реализуйте `Cloneable`. Используйте copy constructor / copy factory.
2. Реализуйте `Cloneable` только если **наследуетесь** от класса, который его уже требует, и нет разумной альтернативы.
3. Для **массивов** `clone()` - идиоматичный и предпочтительный способ копирования. Это единственное убедительное исключение.
   ```java
   int[] copy = original.clone();   // без каста: массивы имеют ковариантный clone()
   ```
4. Интерфейсы никогда не должны расширять `Cloneable`.

---

## Типичные вопросы на интервью

**В чём разница между shallow и deep copy?**
Shallow - копируются значения полей, включая ссылки; вложенные объекты общие. Deep - вложенные изменяемые объекты копируются рекурсивно. `Object.clone()` по умолчанию делает shallow.

**Почему `Cloneable` называют маркерным интерфейсом и что в нём необычного?**
Нет методов. Необычно то, что он не декларирует возможность, а меняет поведение метода суперкласса. Прецедент почти уникальный в JDK.

**Что будет, если вызвать `clone()`, не реализовав `Cloneable`?**
`CloneNotSupportedException` из `Object.clone()`.

**Почему `CloneNotSupportedException` - checked, хотя не должно?**
Историческое решение. Если класс реализует `Cloneable`, оно никогда не выбросится, поэтому checked-статус только добавляет шум. Bloch отмечает это как изъян API.

**Можно ли клонировать объект с `final` изменяемым полем?**
Корректно - нет: нельзя присвоить новую глубокую копию. Придётся убирать `final`.

**Почему `clone()` в `Object` объявлен `protected`?**
Чтобы не навязывать клонирование всем классам. Класс должен явно решить, поддерживать ли его, и повысить видимость до `public`.

**Что вернёт `super.clone()` в подклассе?**
Объект того класса, которым объект является на самом деле, - а не того, в чьём коде написан вызов.
Если у объекта класса `B` (наследника `A`) вызвать `clone()`, то даже когда `super.clone()` срабатывает внутри метода класса `A`, вернётся `B`. `Object.clone()` смотрит на `this.getClass()` - реальный тип объекта - и копирует все его поля, включая те, что объявлены в `B`. Поэтому каст к `B` безопасен.
Именно поэтому копию нельзя создавать конструктором: `new A(...)` внутри `A.clone()` всегда даст `A`, и наследник получит `ClassCastException`.


**Альтернативы `clone()` для deep copy?**
Copy constructor, copy factory, сериализация (медленно, требует `Serializable`), сторонние библиотеки. Для value-объектов - сделать класс immutable, тогда копирование вообще не нужно.

---

## Чек-лист "если всё-таки пишу `clone()`"

- `implements Cloneable`
- `public` override с ковариантным возвращаемым типом
- Первым делом - `super.clone()`, никогда не `new`
- `CloneNotSupportedException` пойман и завёрнут в `AssertionError`
- Каждое изменяемое ссылочное поле скопировано глубоко
- Изменяемые поля не помечены `final`
- Никаких вызовов переопределяемых методов
- Рекурсия проверена на глубину (или заменена итерацией)
- Синхронизация, если класс потокобезопасен
- `throws` в сигнатуре отсутствует