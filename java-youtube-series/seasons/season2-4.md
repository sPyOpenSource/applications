# Season 2 — Object-Oriented Programming (Episodes 9–16)

## EP09 — Classes & Objects: Blueprint → Reality
- **Goal:** Define a class with fields/methods; instantiate with `new`.
- **Key points:** class, object, instance, field, method, reference; `new`.
- **Code:**
  ```java
  class Book {
      String title;
      String author;
      int pages;
      void describe() { System.out.println(title + " by " + author); }
  }
  Book b = new Book();
  b.title = "Clean Code";
  ```
- **Homework:** Create a `Car` class with make/model/year and a `start()` method.

## EP10 — Constructors & the `this` Keyword
- **Goal:** Use constructors to initialize objects; understand `this`.
- **Key points:** default constructor, parameterized, constructor overloading, `this` (field vs param), chaining `this(...)`.
- **Code:** `Student(name, grade)` with validation.
- **Homework:** `Rectangle` with width/height and area().

## EP11 — Encapsulation: Hide & Protect Data
- **Goal:** Make fields private; expose via getters/setters with validation.
- **Key points:** `private`, `public`, getter/setter, validation, read-only fields.
- **Code:** `BankAccount` with deposit/withdraw, balance never negative.
- **Homework:** `Password` class with strength check.

## EP12 — Inheritance: Reuse & Extend
- **Goal:** Use `extends` and `super` to build hierarchies.
- **Key points:** IS-A, `extends`, `super()`, method inheritance, `protected`.
- **Code:** `Animal` → `Dog`, `Cat` with `speak()`.
- **Homework:** `Shape` → `Circle`, `Rectangle`.

## EP13 — Polymorphism: One Interface, Many Forms
- **Goal:** Override methods; use parent reference to child object.
- **Key points:** method overriding, `@Override`, dynamic dispatch, upcasting, downcasting.
- **Code:** `Shape[] shapes` → call `area()` polymorphically.
- **Homework:** `Payment` → `CardPayment`, `CashPayment`.

## EP14 — Abstraction: Abstract Classes
- **Goal:** Define abstract classes and template methods.
- **Key points:** `abstract` class/method, cannot instantiate, concrete subclasses, template method pattern.
- **Code:** `Vehicle` abstract with `start()` abstract and `stop()` concrete.
- **Homework:** `Employee` abstract with `calculateSalary()`.

## EP15 — Interfaces: Contracts in Java
- **Goal:** Define and implement interfaces; understand multiple inheritance.
- **Key points:** `interface`, `implements`, abstract methods, `default`/`static` methods, functional interfaces preview.
- **Code:** `Flyable`, `Swimmable`; `Duck implements both`.
- **Homework:** `Playable` interface for `Guitar`, `Piano`.

## EP16 — Static, Final & Enums
- **Goal:** Class-level members, constants, enumerations.
- **Key points:** `static` fields/methods, `static` block, `final`, `enum`, `valueOf`, switch on enum.
- **Code:** `Counter` with static count; `DayOfWeek` enum.
- **Homework:** `Planet` enum with mass/radius.

# Season 3 — Advanced Java (Episodes 17–24)

## EP17 — Exception Handling: Try, Catch, Finally
- **Goal:** Handle errors gracefully; create custom exceptions.
- **Key points:** try/catch/finally, multiple catch, try-with-resources, checked vs unchecked, `throw`/`throws`, custom exception.
- **Code:** division with catch; `InsufficientFundsException`.
- **Homework:** Validate age input, throw on invalid.

## EP18 — Collections: ArrayList & LinkedList
- **Goal:** Use List API; choose implementation.
- **Key points:** `List`, `ArrayList`, `LinkedList`, add/remove/get, iteration, generics preview.
- **Code:** todo list in-memory CRUD.
- **Homework:** contact list with search.

## EP19 — Collections: HashMap & HashSet
- **Goal:** Use Map and Set; understand hashing.
- **Key points:** `Map<K,V>`, `HashMap`, `Set`, `HashSet`, put/get/containsKey, iteration over entrySet.
- **Code:** word frequency counter.
- **Homework:** phonebook (name→number).

## EP20 — Generics: Type-Safe Reusable Code
- **Goal:** Write generic classes/methods; bounded types.
- **Key points:** `<T>`, generic class/method, type inference, bounded `<T extends Number>`, wildcards `?`.
- **Code:** `Box<T>`, generic `max(T a, T b)`.
- **Homework:** `Pair<K,V>` class.

## EP21 — Lambda Expressions
- **Goal:** Write lambdas and functional interfaces.
- **Key points:** functional interface, `@FunctionalInterface`, lambda syntax, method references `::`.
- **Code:** sort list of strings; filter with Predicate.
- **Homework:** calculator using lambdas.

## EP22 — Streams API: Process Data Declaratively
- **Goal:** Use streams to filter/map/reduce/collect.
- **Key points:** `stream()`, `filter`, `map`, `sorted`, `distinct`, `limit`, `reduce`, `collect`, `Collectors`.
- **Code:** filter evens, sum, group by length.
- **Homework:** top-3 longest words.

## EP23 — File I/O: Read & Write Files
- **Goal:** Read/write text files safely.
- **Key points:** `Files`, `Path`, `BufferedReader`/`Writer`, try-with-resources, CSV parse, `Files.readAllLines`.
- **Code:** read CSV students, write report.
- **Homework:** append log entries to file.

## EP24 — Multithreading Basics
- **Goal:** Create and run threads; understand concurrency.
- **Key points:** `Thread`, `Runnable`, `start()` vs `run()`, `sleep`, `join`, basic race condition awareness.
- **Code:** two threads printing; counter with sleep.
- **Homework:** download simulator (print progress).

# Season 4 — Capstone Project (Episodes 25–30)

## EP25 — Project Kickoff: Task Manager CLI
- **Goal:** Define requirements and design for a CLI task manager.
- **Key points:** user stories, features (add/list/complete/delete/save/load), package structure.
- **Deliverable:** `README` + feature list + package layout.

## EP26 — Modeling: Task, User, TaskManager
- **Goal:** Implement domain classes with OOP principles.
- **Key points:** encapsulation, enums for status/priority, composition.
- **Code:** `Task`, `TaskManager`, `Status` enum.

## EP27 — Persistence: Save/Load Tasks (Files)
- **Goal:** Persist tasks to CSV; load on startup.
- **Key points:** file format, parsing, error handling, backup.
- **Code:** `TaskRepository` with save/load.

## EP28 — Menu System & User Interaction
- **Goal:** Build interactive CLI menu loop.
- **Key points:** Scanner loop, switch menu, input validation, graceful exit.
- **Code:** `App` main loop.

## EP29 — Refactor, Clean Code & Testing
- **Goal:** Refactor for readability; add JUnit tests.
- **Key points:** extract methods, naming, single responsibility, JUnit 5 basics, assertions.
- **Code:** 3 unit tests for TaskManager.

## EP30 — Wrap-Up + Next Steps (Spring Boot Roadmap)
- **Goal:** Celebrate; outline next learning path.
- **Key points:** recap skills, Spring Boot, Maven/Gradle, Git, portfolio tips, community.
- **Deliverable:** roadmap graphic + resource list.
