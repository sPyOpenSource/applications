# EP09 — Full Script: Classes & Objects (OOP Kickoff)

**Target length:** 13:00

---

## [0:00] HOOK
> "So far we've used int, String, arrays — types Java gave us. But what if you need a `Book`, a `Car`, a `Player`? Today you learn to design your own types. This is where Java becomes powerful."

## [0:40] CONCEPT
Visual: blueprint → house analogy.
> "A **class** is a blueprint. An **object** is the actual thing built from it. One blueprint, many houses."

```java
class Book {
    String title;      // field
    String author;
    int pages;

    void describe() {  // method
        System.out.println(title + " by " + author + " (" + pages + " pages)");
    }
}
```

## [3:00] LIVE CODING
1. Create class `Book`.
2. Add fields.
3. Add method `describe()`.
4. In `main`: `Book b = new Book();` then set fields, call describe.
5. Create a second book — show two independent objects.

```java
public class Main {
    public static void main(String[] args) {
        Book b1 = new Book();
        b1.title = "Clean Code";
        b1.author = "Robert C. Martin";
        b1.pages = 464;

        Book b2 = new Book();
        b2.title = "Effective Java";
        b2.author = "Joshua Bloch";
        b2.pages = 416;

        b1.describe();
        b2.describe();
    }
}
```

Explain: `new` allocates memory; `b1`, `b2` are references; fields default to null/0.

## [8:00] COMMON PITFALLS
- `NullPointerException` if you call method on null reference.
- Field name vs variable name.
- Class name must match file name (public class).

## [9:30] RECAP
- class = blueprint, object = instance.
- fields = data, methods = behavior.
- `new` creates object.

## [10:30] HOMEWORK
Create a `Car` class with `make`, `model`, `year` and a `start()` method that prints "Vroom!". Create two cars.

## [11:00] OUTRO
Next: constructors — how objects are born with their data already set.
