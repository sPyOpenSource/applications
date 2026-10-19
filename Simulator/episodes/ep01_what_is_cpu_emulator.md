# Episode 1 — "What Is a CPU Emulator? (Java Setup)"

## Metadata
- **Title:** Build an x86 CPU Emulator in Java — Ep.1: What Is a CPU Emulator?
- **Length target:** 7:00
- **Git tag:** `ep01`
- **Companion file:** `src/main/java/emulator/Cpu.java`

## Thumbnail concept
Split screen: left = a glowing physical CPU chip; right = Java code `class Cpu { ... }` on dark IDE. Big text: "CPU EMULATOR in JAVA". Subtext: "Ep.1 — Setup". Colors: deep navy + electric cyan.

## YouTube description
```
In Episode 1 we answer the big question: what IS a CPU emulator? Then we set up a Java project and write the empty skeleton of an x86 CPU emulator — registers, memory, and the main loop come next.

Repo (checkpoints per episode): https://github.com/<handle>/java-x86-emulator
Tag for this episode: ep01

Timestamps:
0:00 What is an emulator?
1:30 Real CPU vs emulated CPU
3:00 Project setup (Maven + IntelliJ)
4:45 The Cpu class skeleton
6:00 What's next + exercise

#java #emulator #x86 #beginner #programming
```

## Tags
java, x86 emulator, cpu emulator, java tutorial, beginner programming, emulation, retro computing, learn java

---

## Script (spoken ~1,020 words)

**[0:00 — HOOK + INTRO]**
(Host on camera, B-roll: old PC booting, then Java logo.)

"Hey everyone, welcome to a brand new series: **Build an x86 CPU Emulator in Java**. Over the next five episodes, we're going to write a tiny but real x86 CPU emulator — from an empty file all the way to running an actual program. And the best part? You only need basic Java to follow along. No assembly experience required.

By the end of today, you'll have a project set up and a `Cpu` class that compiles. Let's go."

**[0:45 — PLAIN ENGLISH: What is an emulator?]**
(Screen: definition card.)

"Plain English time. An **emulator** is a program that pretends to be another computer. It reads the same instructions a real CPU would read, and does the same math — but in software, on your machine. That's it. No magic."

**[1:15 — Real CPU vs emulated CPU]**
(Screen: side-by-side diagram.)

"A real CPU is a chip of silicon. It has registers — tiny fast storage slots — and memory, which is slower but bigger. It runs a loop forever: fetch an instruction, decode it, execute it. That's called the **fetch-decode-execute loop**, and it's the heartbeat of every computer ever made.

Our emulator will copy that exactly. We'll have registers, memory, and that same loop — all written in Java. We won't be fast. We won't run Windows. But we'll be *correct*, and that's what matters for learning."

**[2:30 — Project setup]**
(Screen recording: terminal.)

"Let's set up. I'm using Maven and IntelliJ, but any IDE works. Watch the terminal."

```bash
mvn archetype:generate -DgroupId=emulator -DartifactId=java-x86-emulator -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd java-x86-emulator
```

"Open the folder in your IDE. You'll see `src/main/java` and `src/test/java`. Delete the sample `App.java` — we don't need it. Create a new package called `emulator`."

**[3:45 — The Cpu class skeleton]**
(Screen: IDE, typing.)

"Now the fun part. Create `Cpu.java` in the `emulator` package. Here's our skeleton."

```java
package emulator;

public class Cpu {
    // Registers (filled in next episode)
    // Memory (filled in next episode)

    public Cpu() {
        // nothing yet
    }

    public void reset() {
        // reset state to power-on defaults
    }

    public void run() {
        // the fetch-decode-execute loop (coming soon)
    }
}
```

"Notice I'm sketching the *shape* first, before filling in details. This is a great habit: decide your API, then implement. `reset()` puts the CPU into a known starting state. `run()` is the heartbeat we talked about."

**[5:00 — Main entry point]**
```java
package emulator;

public class Main {
    public static void main(String[] args) {
        Cpu cpu = new Cpu();
        cpu.reset();
        cpu.run();
        System.out.println("CPU halted.");
    }
}
```

"Hit run. It should print 'CPU halted.' — because `run()` is empty for now. If you see that, your project is set up correctly."

**[5:45 — Recap + checkpoint]**
"Recap: an emulator pretends to be a CPU in software. Real CPUs loop fetch-decode-execute. We set up a Maven project and created a `Cpu` class with `reset()` and `run()`. Next episode we fill in the registers and memory — the CPU's 'scratch paper' and 'desk'.

Checkpoint: `git checkout ep01` in the repo."

**[6:20 — TRY IT exercise]**
"Try it before next week: add a `public int pc` field — that's the program counter, the address of the next instruction. Don't use it yet, just declare it. We'll wire it up in episode 3.

Subscribe, hit the bell, and I'll see you next episode. Bye!"

## Production notes
- Show the compile success green checkmark.
- Zoom on the `class Cpu` declaration for 3 seconds.
- B-roll: close-up of CPU socket, fan spinning.

## On-screen code (final state ep01)
```java
package emulator;

public class Cpu {
    public int pc; // program counter (preview)

    public Cpu() { }

    public void reset() { pc = 0; }

    public void run() { /* fetch-decode-execute: episode 3 */ }
}

public class Main {
    public static void main(String[] args) {
        Cpu cpu = new Cpu();
        cpu.reset();
        cpu.run();
        System.out.println("CPU halted.");
    }
}
```
