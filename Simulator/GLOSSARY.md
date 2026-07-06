# Simulation Glossary

Terms that appear across the armOS simulator suite. Ground truth is the code in `src/`.

## CPU simulation

**Fetch-Decode-Execute cycle**:
The three-step loop every simulator runs: (1) read an instruction word from memory at the program counter, (2) identify the instruction type from its opcode bits, (3) perform the operation and update state. The RISC-V `step()` in `IsaSim.java:66` is the clearest example.

**Single-cycle**:
A simulation model where every instruction completes in one tick of the loop — fetch, decode, execute, and writeback all happen in one call to `step()`. No pipelining. `IsaSim` is single-cycle.

**Opcode**:
The portion of an instruction word that selects which operation to perform. In RISC-V the bottom 7 bits (`instr & 0x7f`) are the opcode; in 8051 the first byte is the opcode. The codebase uses `MASK` constants (`LUI_MASK`, `JAL_MASK`, etc.) for the RISC-V opcode values.

**Funct3 / Funct7**:
Additional bit fields in RISC-V instructions that further qualify the opcode. For example, opcode `OP_MASK` (arithmetic) uses funct7 to separate ADD from SUB, and funct3 to separate ADD from AND.

**Register file**:
The bank of 32 general-purpose registers (`reg[0..31]`) shared by all RV32IM simulators. `x0` is hardwired to zero. The 8051 has a different register model (Accumulator, B, DPTR, PSW, etc.).

**Program counter (PC)**:
The address of the current instruction. After each step, the PC advances by 4 bytes (RISC-V), unless a branch/jump sets it. The code tracks this via a `pc` int and an `offsetPC` boolean flag.

**Instruction word**:
A 32-bit value read from memory, interpreted as an encoded instruction. The RISC-V `step()` reads it via `code.read32(pc)`. The 8051 fetches one byte at a time (variable-length instructions, 1–3 bytes).

## Memory model

**Code interface** (`j51.intel.Code`):
A Java interface with methods `read8`, `read16`, `read32`, `write`, `containsKey` — the contract every memory model implements. Both `jCPU.RiscV.Memory` and `j51.device.VolatileMemory` implement `Code`.

**HashMap-backed memory**:
The simplest memory strategy: a `HashMap<Integer, Byte>` maps addresses to byte values. Used by `Memory.java` (RISC-V). No size limit, sparse, slow for large simulations.

## Architecture layers in this codebase

**iCPU interface** (`src/jCPU/iCPU.java`):
The contract every CPU must implement: `step()`, `go()`, `getDecodeAt()`, register access, memory read/write. This is the seam between the CPU core and its peripherals.

**AbstractOpcode** (`src/jCPU/AbstractOpcode.java`):
Base class for 8051 instruction implementations. Contains the instruction decoder table and parameter-extraction helpers (DATA8, DATA16, BIT, DIRECT, etc.). Every `j51.intel.graph.inst/*.java` class extends this.

**MCS51** (`src/j51/intel/MCS51.java`):
The 8051 CPU core — translates incoming opcodes to `AbstractOpcode` instances via an array-based dispatch table (`opcodes[256]`). This is the 8051 equivalent of `IsaSim.step()`'s switch statement.

## SFR (Special Function Register)

**SFR (Special Function Register)**:
A byte-wide register in the 8051's direct-addressable space at 0x80–0xFF. Each SFR is wired to a specific hardware unit: ACC (accumulator), P0–P3 (IO ports), TCON/TMOD (timers), SCON/SBUF (serial), IE (interrupts). Peripherals in the simulator monitor SFR writes via listener callbacks.

**SfrWriteListener** (`src/j51/intel/SfrWriteListener.java`):
Interface peripherals implement to receive SFR write notifications. Single method: `sfrWrite(int register, int value)`. Register with `cpu.addSfrWriteListener(sfrAddress, this)`.

**SfrReadListener** (`src/j51/intel/SfrReadListener.java`):
Interface peripherals implement to service SFR read requests. Single method: `int sfrRead(int register)`. Register with `cpu.addSfrReadListener(sfrAddress, this)`.

**MemoryWriteListenerSfr** (`src/j51/intel/MemoryWriteListenerSfr.java`):
Adapter that converts the low-level `MemoryWriteListener.writeMemory(addr, newVal, oldVal)` to the simpler `SfrWriteListener.sfrWrite(reg, val)`. Registered internally by `addSfrWriteListener()`.

**MemoryReadListenerSfr** (`src/j51/intel/MemoryReadListenerSfr.java`):
Same adapter pattern for reads. Registered internally by `addSfrReadListener()`.

**VolatileMemory** (`src/j51/device/VolatileMemory.java`):
Array-backed memory implementation. Its `write()` method updates the byte then iterates `MemoryByte.mw[]` (write listener array). This is where the listener dispatch happens — the hot path between CPU instructions and peripheral callbacks.

**SfrPage** (`src/j51/intel/SfrPage.java`):
A 256-byte page of SFR memory (extends `VolatileMemory`). The 8051 may have multiple SFR pages (bank switching), accessed as `SfrRegister` objects. Default is page 0.

**SfrRegister** (`src/j51/intel/SfrRegister.java`):
A single SFR slot (extends `MemoryByte`). Holds the register value, its name, and a list of `InterruptSource` objects that trigger when this register is written. One SfrRegister exists for each of the 256 possible SFR addresses.

**SFR paging**:
An 8051 extension where multiple banks of SFRs exist, switched via a bank-select register. The CPU tracks `sfrCurrent` — the active `SfrPage`. Some derivatives use this to double the available SFR space.
