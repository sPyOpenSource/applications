# RISC-V simulator entry point: `IsaSim.java`

The user wants to understand the armOS simulator suite by tracing real code. We established `IsaSim.java` as the anchored entry point for the first session. The single-cycle fetch→decode→execute→writeback pattern in `step()` is the model that carries across all four simulators in the codebase.

First lesson (0001) traces an ADD instruction through the entire `step()` method. The user can now read an RISC-V instruction word, identify its opcode/funct3/funct7/rd/rs1/rs2 fields, and follow which method handles it.

**Implications:** Next lesson should contrast this with the 8051 emulator's dispatch model — `MCS51.java` with its 256-entry opcode table and `AbstractOpcode` hierarchy — to show how the same fetch-decode-execute pattern takes a different shape when instructions are variable-length.
