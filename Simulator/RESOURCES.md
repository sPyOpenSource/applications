# Simulation Resources

## Knowledge

- [RISC-V Unprivileged Specification (Volume I, 2019)](https://riscv.org/technical/specifications/)
  The canonical spec for RV32I/RV64I base instruction set. The `IsaSim.java` and `RVInstruction.java` decode tables map almost one-to-one onto this spec. Use for: checking opcode encodings and instruction semantics.

- [8051 Instruction Set Manual — Intel](https://www.intel.com/content/dam/www/program/design/us/en/documents/8051-microcontroller-instruction-set.pdf)
  The original vendor documentation for the MCS-51 instruction set. The 94 `j51/intel/graph/inst/*.java` files implement exactly this. Use for: understanding the arithmetic, jump, and bit-manipulation instructions.

- [ARM Architecture Reference Manual (ARMv7-A)](https://developer.arm.com/documentation/ddi0406/latest/)
  Covers the ARM instruction set that both JARM and the lxtreme ARM9 emulator target. Use for: checking conditional execution, Thumb interworking, and coprocessor interface.

- [NASM Documentation](https://www.nasm.us/doc.php)
  The Netwide Assembler docs. The `src/nasm/` package parses and assembles NASM syntax. Use for: understanding the expression evaluator, instruction encoding rules.

- [Patterson & Hennessy — *Computer Organization and Design: RISC-V Edition*](https://www.elsevier.com/books/computer-organization-and-design-risc-v-edition/patterson/978-0-12-820331-6)
  The definitive textbook on single-cycle and multi-cycle processor design. The RISC-V simulator in this codebase is essentially a software implementation of the single-cycle datapath from Chapters 4–5. Use for: building the mental model of fetch→decode→execute→memory→writeback.

- [JVM Specification, §4.10 *Verification of class files*](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.10)
  The canonical rules for bytecode verification. The `jx.verifier` package implements exactly this abstract-interpretation discipline: merging operand stacks of equal depth, type joining at control-flow merges, and JSR/RET subroutines. Use for: the "what" behind the verifier; the code is the "how."

- [Golm, Felser, Wawersich & Kleinöder — *The JX Operating System* (USENIX ATC 2002)](https://www.usenix.org/conference/usenix-2002-annual-technical-conference/jx-operating-system)
  The research paper describing the JX OS model that armOS's JVM follows: Java bytecode verified statically and run natively in supervisor context, with bytecode verification — not hardware — as the trust boundary. Use for: understanding *why* the verifier exists and what it must prove.

## Wisdom (Communities)

- [r/EmuDev](https://reddit.com/r/EmuDev)
  Active subreddit for emulator and simulator developers. Use for: architecture-specific questions, debugging tricky instruction edge cases.

- [Stack Overflow — emulation tag](https://stackoverflow.com/questions/tagged/emulation)
  Good for specific technical questions about instruction decoding, memory mapping, and cycle accuracy.

## Gaps

- No known high-quality resource specifically about the ARM9/Thumb decode methodology used in `nl/lxtreme/arm/CPU.java`. The ARM ARM covers what, but not how to implement. The code itself is the best source for the "how."
- No single doc that ties the verifier's abstract-interpretation engine (`MethodVerifier`/`JVMState`) to how the WCET analysis builds and bounds its control-flow graphs. The `wcet/` package and `BCStackEffect.java` are the ground truth.
