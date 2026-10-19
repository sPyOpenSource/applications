# EP01 — Full Script: What is Java? Install JDK & Write Hello World

**Target length:** 12:00
**Companion code:** `ep01-hello-world/HelloWorld.java`

---

## [0:00] HOOK (30s)

> "Java runs on 3 billion devices. Your Android phone, your bank's servers, Minecraft. Today you'll write your first Java program — and by the end of this series, you'll build a real app. Let's go."

[Show quick montage: Android app, bank terminal, Minecraft, IntelliJ.]

## [0:30] INTRO (10s)

> "Welcome to Java from Zero to Hero. I'm [name]. Episode 1. Let's start."

[Title card: "Java #1 — What is Java?"]

## [0:45] WHAT IS JAVA (2 min)

Visual: diagram with three boxes: Source code (.java) → Compiler (javac) → Bytecode (.class) → JVM → Machine code.

Narration:
> "Java is a high-level, object-oriented programming language. You write code in `.java` files. The compiler `javac` turns it into **bytecode** — a `.class` file. That bytecode runs on the **Java Virtual Machine (JVM)**, which translates it to your computer's native instructions."

> "Three pieces to know:"
> - **JDK** (Java Development Kit) — tools to *develop*: compiler, debugger.
> - **JRE** (Java Runtime Environment) — to *run* Java apps.
> - **JVM** — the engine that executes bytecode.

> "Install the **JDK** — it includes everything."

> "Why Java? Write once, run anywhere. Huge ecosystem. Great for backend, Android, enterprise."

## [2:45] INSTALL JDK (2.5 min)

Screen: browser → adoptium.net (Temurin).

Steps (show on screen, narrate):
1. Go to adoptium.net → Temurin 21 (LTS).
2. Download for your OS (Windows .msi / macOS .pkg).
3. Run installer, keep defaults.
4. Verify: open terminal → `java -version` and `javac -version`.

> "If you see a version number, you're good. If 'command not found', your PATH needs fixing — link in description."

## [5:15] INSTALL INTELLIJ (2 min)

Screen: jetbrains.com/idea/download → Community Edition (free).

Steps:
1. Download & install.
2. Launch → "New Project".
3. Name: `HelloWorld`, Language: Java, JDK: Temurin 21, Build system: IntelliJ (no Maven yet).
4. Create.

> "IntelliJ is the most popular Java IDE. Community edition is free and enough for this whole series."

## [7:15] FIRST PROGRAM (2 min)

Screen: editor. Type slowly, explain each line.

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

Explain:
- `public class HelloWorld` — a class named HelloWorld; file must be `HelloWorld.java`.
- `public static void main(String[] args)` — entry point. JVM looks for this method to start.
- `System.out.println(...)` — print to console.
- Semicolon ends a statement.

Run with green play button. Show console output: `Hello, World!`

> "Change the text. Run again. You just programmed."

## [9:15] RECAP (1 min)

Bullet recap on screen:
- Java → bytecode → JVM → anywhere.
- JDK to develop, JRE to run.
- `main` method is the entry point.
- `System.out.println` prints.

## [10:15] HOMEWORK (30s)

> "Install JDK + IntelliJ on your machine. Create a project and print your name and today's date. Drop a screenshot in the comments — I'll reply to the first 50."

## [10:45] OUTRO (15s)

> "Next episode: variables and data types — how Java stores information. Subscribe so you don't miss it. See you Tuesday."

[End screen: subscribe button + next video card + socials.]

---

## Notes for recording
- Use 1920x1080, 60fps. Font size 18+ in editor.
- Zoom in on terminal commands.
- Keep energy high; smile on camera.
- Add captions (auto + manual review).

## Companion repo commit
`ep01-hello-world/HelloWorld.java` + `NOTES.md`.
