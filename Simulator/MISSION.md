# Mission: Understanding the armOS Simulator Suite

## Why

You maintain armOS, which contains four processor simulators (RISC-V, 8051, ARM, x86 NASM) alongside a Java VM interpreter. To extend, debug, and port this code with confidence, you need a mental model of how each simulator works — how they fetch-decode-execute, model memory, handle interrupts, and interface with peripherals. Surface-level reading of files hasn't built that model.

## Success looks like

- You can trace a RISC-V instruction through the full fetch→decode→execute→writeback cycle in `IsaSim.java`
- You can explain how the 8051 opcode dispatch table (`MCS51.java` → 94 instruction classes) works without opening the file
- You can compare the ARM CPU in JARM vs the lxtreme ARM9 and say which parts are shared and which diverge
- You can add a new instruction to any of the simulators and know exactly which files to touch

## Constraints

- This is the actual codebase — lessons are grounded in real files, not toy examples
- You know Java well; no time wasted on language basics
- Lessons should be scannable in 10–15 minutes

## Out of scope

- Writing a new simulator from scratch
- Formal verification or pipelining theory
- Performance optimisation of the existing code
