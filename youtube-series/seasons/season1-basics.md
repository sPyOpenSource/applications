# Season 1 — Java Basics (Episodes 1–8)

## EP01 — What is Java? Install JDK & Write Hello World
- **Length:** 12 min
- **Goal:** Student has JDK + IntelliJ running and runs Hello World.
- **Timestamps:**
  - 0:00 Hook
  - 0:45 What is Java (JVM/JDK/JRE diagram)
  - 2:30 Install JDK (Temurin/Oracle) on Win/Mac
  - 5:00 Install IntelliJ IDEA Community
  - 7:30 First project: Hello World
  - 9:30 How it runs: compile → bytecode → JVM
  - 11:00 Recap + homework
- **Key points:** Java is compiled to bytecode, runs on JVM → "write once, run anywhere"; `public static void main(String[] args)`; `System.out.println`.
- **Code:**
  ```java
  public class HelloWorld {
      public static void main(String[] args) {
          System.out.println("Hello, World!");
      }
  }
  ```
- **Common pitfalls:** PATH not set; wrong JDK version; file name ≠ class name.
- **Homework:** Install everything, change message to your name, run it.

## EP02 — Variables & Primitive Data Types
- **Length:** 11 min
- **Goal:** Declare and use all 8 primitives + String.
- **Timestamps:** Hook 0:00 · Theory 0:40 · Live code 3:00 · Naming rules 8:30 · Recap 10:00
- **Key points:** `byte/short/int/long`, `float/double`, `char`, `boolean`; `String` is reference; naming camelCase; constants `final`.
- **Code:**
  ```java
  int age = 25;
  double price = 19.99;
  boolean isJavaFun = true;
  char grade = 'A';
  String name = "Alex";
  final double PI = 3.14159;
  ```
- **Homework:** Declare variables for a person (name, age, height, isStudent), print a sentence.

## EP03 — Operators & Expressions
- **Length:** 10 min
- **Goal:** Use arithmetic, comparison, logical, assignment operators.
- **Key points:** `+ - * / %`, `++ --`, `== != < > <= >=`, `&& || !`, ternary `?:`, precedence.
- **Code:** simple calculator reading two ints.
- **Homework:** BMI calculator.

## EP04 — Strings & User Input
- **Length:** 12 min
- **Goal:** Use String methods and read input with Scanner.
- **Key points:** `length()`, `charAt()`, `substring()`, `toUpperCase()`, `equals()` vs `==`; `Scanner` nextLine/nextInt; close scanner.
- **Code:**
  ```java
  Scanner sc = new Scanner(System.in);
  System.out.print("Name: ");
  String name = sc.nextLine();
  System.out.println("Hello " + name.toUpperCase());
  ```
- **Homework:** Ask name + age, greet, compute birth year.

## EP05 — Control Flow: if/else & switch
- **Length:** 12 min
- **Goal:** Branch logic with if/else, switch, ternary.
- **Key points:** if/else if/else; switch on int/String/enum; fall-through; ternary.
- **Code:** grade calculator A-F; day-of-week with switch.
- **Homework:** Simple ATM menu (balance/deposit/withdraw).

## EP06 — Loops: for, while, do-while
- **Length:** 12 min
- **Goal:** Iterate with all three loops; use break/continue.
- **Key points:** for (init;cond;update); while; do-while (runs once); enhanced for; break/continue.
- **Code:** multiplication table; guess-the-number game.
- **Homework:** FizzBuzz 1–100.

## EP07 — Methods: Reusable Code Blocks
- **Length:** 13 min
- **Goal:** Define and call methods; understand params & return; overloading.
- **Key points:** method signature, return type, void, parameters, overloading, scope.
- **Code:** `celsiusToFahrenheit`, `isPrime`.
- **Homework:** Temperature converter with menu.

## EP08 — Arrays: Store Multiple Values
- **Length:** 12 min
- **Goal:** Create, index, iterate arrays; find max/min/sum.
- **Key points:** declaration, `new int[n]`, length, index 0-based, ArrayIndexOutOfBounds, enhanced for.
- **Code:** find max, reverse array, linear search.
- **Homework:** Sort an array (bubble sort) manually.
