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

## Wisdom (Communities)

- [r/EmuDev](https://reddit.com/r/EmuDev)
  Active subreddit for emulator and simulator developers. Use for: architecture-specific questions, debugging tricky instruction edge cases.

- [Stack Overflow — emulation tag](https://stackoverflow.com/questions/tagged/emulation)
  Good for specific technical questions about instruction decoding, memory mapping, and cycle accuracy.

## Gaps

- No known high-quality resource specifically about the ARM9/Thumb decode methodology used in `nl/lxtreme/arm/CPU.java`. The ARM ARM covers what, but not how to implement. The code itself is the best source for the "how."
