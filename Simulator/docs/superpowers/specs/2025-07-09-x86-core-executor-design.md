# x86 Core Executor — Design Spec

## Goal

Add real x86 binary execution to the armOS simulator suite. Build a fetch-decode-execute loop on raw x86 machine code, integrated with the existing disassembler and NASM evaluator. Keep the existing NASM AST path working alongside it.

## Architecture

One new shared CPU state class (`X86Core`) is the heart of the system. Both execution paths operate on it through the same register/flag/memory API.

```
                    ┌─────────────────────┐
                    │      X86Core        │  ← NEW
                    │  regs[16], eflags,  │
                    │  eip, esp, memory[] │
                    │  step()  go()       │
                    └─────────┬───────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼                               ▼
   ┌──────────────────┐           ┌──────────────────────┐
   │  NasmEval        │           │  X86BinaryExecutor   │  ← NEW
   │  (refactored)    │           │  (extends            │
   │  NASM AST →      │           │   jx.disass.x86)     │
   │  X86Core ops     │           │  raw bytes →         │
   └──────────────────┘           │  decode + execute    │
                                  └──────────────────────┘
```

- **`X86Core`** implements `jCPU.iCPU`, providing `setReg/getReg`, `setFlag/getFlag`, `push32/pop32`, `readMem/writeMem` as the primitive API. Both executors use only this API, never directly mutate state.
- **`X86BinaryExecutor`** extends `jx.disass.x86` to reuse all ModR/M, immediate, and operand decoding. Each opcode case in the existing `disasmInstr()` switch gains parallel execution logic that operates on the X86Core.
- **`NasmEval`** is refactored to use X86Core instead of owning its own register/memory/flag fields. Virtual registers (`@r0..@rN`) map to `core.regs[8..15]`.
- **`NasmVM`** wires the X86Core through both executors and picks the active one.
- **`jx.disass.x86`** is decoupled from `j51.intel.MCS51` (removing the architecturally wrong inheritance). Its `getDecodeAt()` returns text only; execution lives in the subclass.
- **`j51.test.x86`** is rewired to use X86Core + X86BinaryExecutor.

## Components

### X86Core — `src/jCPU/x86/X86Core.java`

Central CPU state machine. Implements `jCPU.iCPU`.

**State:**
- `int[] regs` — 16 slots. r0=EAX, r1=ECX, r2=EDX, r3=EBX, r4=ESP, r5=EBP, r6=ESI, r7=EDI. r8..r15 for NASM virtual registers.
- `int eip` — instruction pointer, auto-incremented by step().
- `int eflags` — bitmask: CF(0), PF(2), AF(4), ZF(6), SF(7), TF(8), IF(9), DF(10), OF(11). Parity computed via 256-entry lookup table. Adjust flag for BCD carry out of low nibble.
- `byte[] memory` — flat 32-bit address space, configurable size (default 64MB).
- Reference to active executor (`NasmEval` or `X86BinaryExecutor`).

**Methods:**
- `step()` — delegates to active executor.
- `go(int limit)` — run up to `limit` instructions.
- `getDecodeAt(int pc)` — snapshot of instruction text at pc for the GUI.
- `setReg(int idx, int val)` / `getReg(int idx)` — bounds-checked register access.
- `setFlag(int bit, boolean v)` / `getFlag(int bit)` — single-flag manipulation.
- `push32(int val)` — decrements ESP by 4, writes val to `memory[esp..esp+3]`.
- `pop32()` — reads `memory[esp..esp+3]`, increments ESP by 4, returns value.
- `readMem8(int addr)` / `readMem16(int addr)` / `readMem32(int addr)` — bounds-checked memory reads, little-endian.
- `writeMem8/16/32(int addr, int val)` — bounds-checked memory writes, little-endian.
- `setActiveExecutor(Executor)` — switch between NASM and binary mode.
- `reset()` — zeros regs/flags, sets ESP to top of memory, EIP to 0.

**Package-private static flag helpers in `X86Flags.java`:**
- `addAndFlags(int a, int b)` → `{result, eflags}` — computes 32-bit unsigned sum, sets CF (carry out of bit 31), PF (parity of low byte of result), AF (carry out of bit 3), ZF (result==0), SF (result bit 31), OF (signed overflow: signs of a and b same, result sign differs).
- `subAndFlags(int a, int b)` → `{result, eflags}` — computes a-b as a + ~b + 1, same flag rules with borrow semantics for CF.
- `adcAndFlags(int a, int b, boolean carryIn)` — add with carry-in from CF.
- `sbbAndFlags(int a, int b, boolean borrowIn)` — subtract with borrow-in.
- `logicalAndFlags(int result)` — sets ZF, SF, PF. CF=0, OF=0, AF undefined (set to 0).
- `incAndFlags(int a)` — flags like add but CF preserved.
- `decAndFlags(int a)` — flags like sub but CF preserved.
- `negAndFlags(int a)` — two's complement negation, CF set iff a != 0.
- `shiftAndFlags(int val, int count, int type)` — handles shl/shr/sar/rol/ror/rcl/rcr flag semantics per Intel manual: CF gets last bit shifted out, OF set for single-bit shifts if sign changes.

### X86BinaryExecutor — `src/jCPU/x86/X86BinaryExecutor.java`

Extends `jx.disass.x86` (the disassembler base class). Walks the same opcode switch but adds execution in parallel with text generation.

**Execution logic (core subset covered):**

| Opcode group | Execution |
|---|---|
| `MOV r/m, r` / `MOV r, r/m` / `MOV r/m, imm` | read source, write destination register/memory |
| `ADD/SUB/ADC/SBB/AND/OR/XOR/CMP/TEST` | compute via flag helpers, write result (CMP/TEST discard result, set flags only) |
| `INC/DEC` | preserve CF, set OF/ZF/SF/PF/AF |
| `NEG/NOT` | compute, set flags |
| `MUL/IMUL/DIV/IDIV` | use EAX/EDX implicitly, set CF/OF for IMUL |
| `SHL/SHR/SAR/ROL/ROR/RCL/RCR imm8,1,CL` | shift via flag helpers |
| `LEA` | compute effective address, write to register (no memory access, no flags) |
| `MOVZX/MOVSX` | zero/sign-extend byte/word to dword |
| `XCHG` | swap register/memory with register |
| `PUSH/POP/PUSHF/POPF/PUSHAD/POPAD` | stack ops on X86Core |
| `JMP/Jcc rel8/32` | conditional branch based on eflags bits |
| `CALL/RET` | push/pop EIP |
| `STC/CLC/CMC/STD/CLD/STI/CLI` | single-flag manipulation |
| `NOP` | no-op |
| `HLT` | stop execution |
| `CBW/CWDE/CDQ` | sign-extend AL→AX→EAX, EAX→EDX:EAX |

Unimplemented opcodes throw `UnsupportedOperationException` with the opcode byte and PC, logged as "unsupported opcode 0xNN at EIP=0xMMMM".

**`step()` flow:**
1. Set `codePosition` to `core.eip`.
2. Call `disasmInstr()` — this walks the opcode tree, produces `instruction` text AND calls execution methods on `core`.
3. `core.eip += codePosition - originalPC` (instruction length is known after decode).
4. Return `instruction` text for the GUI.

**NOTE:** The existing `disasmInstr()` uses `code[]` as a byte buffer. We'll feed it from `core.memory` at `core.eip`. The `fetch_byte()`, `fetch_word()`, `fetch_dword()` methods already walk `codePosition` and read bytes. We keep this mechanism — the fetch methods read from `core.memory` by delegation.

### NasmEval Refactor — `src/nasm/NasmEval.java`

All state fields become reads/writes through `X86Core`:

| Old field | New |
|---|---|
| `int eax` | `core.getReg(X86Core.REG_EAX)` / `core.setReg(X86Core.REG_EAX, v)` |
| `int ebx` | `core.getReg(X86Core.REG_EBX)` / `core.setReg(X86Core.REG_EBX, v)` |
| `int ecx` | `core.getReg(X86Core.REG_ECX)` / `core.setReg(X86Core.REG_ECX, v)` |
| `int edx` | `core.getReg(X86Core.REG_EDX)` / `core.setReg(X86Core.REG_EDX, v)` |
| `int ebp` | `core.getReg(X86Core.REG_EBP)` / `core.setReg(X86Core.REG_EBP, v)` |
| `int[] registers` | `core.getReg(8 + idx)` / `core.setReg(8 + idx, v)` |
| `boolean ZF` | `core.setFlag(FLAG_ZF, v)` / `core.getFlag(FLAG_ZF)` |
| `boolean SF` | `core.setFlag(FLAG_SF, v)` / `core.getFlag(FLAG_SF)` |
| `Memory memory` | `core.push32/pop32/readMem32/writeMem32` |
| `int eip` | `core.eip` for read, `core.eip += 1` for advance |

Other registers (`ESI`, `EDI`) added to `X86Core` constants; `NasmEval` uses them for NASM programs that reference `esi`/`edi`.

**Bug fix in LoadNasm:** `caseAAndInst` currently creates `new Or(...)` instead of `new And(...)`. Fixed. New `And` NasmInst class added to `src/nasm/inst/`.

### NasmVM — `src/nasm/NasmVM.java`

- Constructor creates `X86Core` and `NasmEval(core, code, stackSize, verboseLevel)`.
- `step()` calls `core.step()` which delegates to the active executor (NasmEval in this mode).
- `getDecodeAt()` returns `core.getDecodeAt(pc)`.

### jx.disass.x86 — decoupling

- Remove `extends j51.intel.MCS51`. This inheritance was solely for GUI display integration. The class now provides only decode functionality.
- Remove all `Visitor` interface stubs (`insertByte`, `insertConst`, `pushl`, `movl`, `addl`, etc.) — these were part of a JX compiler backend interface, not the disassembler's concern. Keep only the decoding methods.
- Make `getDecodeAt()` and `getLengthAt()` work purely from the `code` byte buffer (already the case).
- `getCurrentIP()` returns `codePosition`.

### j51.test.x86 — rewiring

- Constructs `X86Core`, then `X86BinaryExecutor(core)`.
- LCD peripheral (`G128x64`) registered on `X86Core` via memory-mapped IO at a configurable address range, not through 8051 SFR hooks (since there are no SFRs in x86).

## Instruction Coverage

Core x86-64 subset, flat 32-bit memory model:

**Data transfer:** mov, xchg, lea, movzx/sx (byte→dword, word→dword), cbw/cwde, cdq
**Arithmetic:** add, sub, adc, sbb, inc, dec, neg, cmp, mul, imul, div, idiv
**Logic:** and, or, xor, not, test
**Shifts:** shl/sal, shr, sar, rol, ror, rcl, rcr (imm8, 1, CL variants)
**Stack:** push, pop, pushf, popf, pushad, popad
**Control:** jmp, call, ret, je/jne/jl/jle/jg/jge/jb/jnb/jbe/ja/jo/jno/js/jns/jp/jnp
**Flags:** stc, clc, cmc, std, cld
**Other:** nop, hlt, int (sw interrupt to stop execution)

## Register & Flag Model

**Registers:** EAX, ECX, EDX, EBX, ESP, EBP, ESI, EDI — 32-bit. Plus virtual r8..r15.

**EFLAGS bits implemented:**
- CF (bit 0) — carry/borrow from arithmetic and shifts
- PF (bit 2) — parity of low 8 bits of result (even parity=1)
- AF (bit 4) — auxiliary carry from bit 3 to bit 4 (for BCD)
- ZF (bit 6) — result is zero
- SF (bit 7) — result is negative (bit 31 set)
- OF (bit 11) — signed overflow
- DF (bit 10) — direction flag (for string ops, set/cleared by std/cld)

TF (bit 8), IF (bit 9) accepted but no behavior implemented (no interrupts, no single-step trap).

## Error Handling

- **Invalid opcode:** throw `UnsupportedOperationException` with opcode + PC.
- **Div-by-zero:** throw `ArithmeticException` with register dump. (Optionally, a flag-controlled mode sets result=0 per x86 #DE behavior.)
- **Memory bounds:** `IndexOutOfBoundsException` for access beyond configured memSize.
- **Stack underflow:** pop when ESP > initial stack top throws `StackUnderflowException`.
- **Stack overflow:** push when ESP < 4096 (guard page) logs warning, optionally caps ESP.
- **Unaligned access:** silent in flat mode. A `strictAlign` flag in X86Core can enable checks.

## Testing Plan

Tests in `test/jCPU/x86/`:

1. **`X86FlagsTest.java`** — unit tests per flag helper:
   - `addAndFlags`: test CF (FFFFFFFF+1), ZF (0+0), SF (results with bit 31), OF (7FFFFFFF+1, 80000000+FFFFFFFF), AF (0F+01), PF (results with various low-byte parity).
   - `subAndFlags`: similar coverage with borrow semantics.
   - `incAndFlags`: verify CF preserved, OF on 7FFFFFFF+1.
   - `negAndFlags`: verify CF=1 unless operand=0.
   - `shiftAndFlags`: verify CF gets shifted-out bit, OF for single-bit shifts.
   - `logicalAndFlags`: CF=0, OF=0 confirmed.

2. **`X86BinaryTest.java`** — integration tests on pre-assembled binaries:
   - `mov eax, 5; mov ebx, 3; add eax, ebx` → eax=8, ZF=0.
   - `sub eax, 8` → eax=0, ZF=1.
   - `push ecx; pop edx` → edx equals original ecx.
   - `cmp eax, ebx; je` → conditional branch works.
   - All tests assert register dumps against expected .res files (matching RISC-V pattern).

3. **`X86RoundTripTest.java`** — NASM mode vs binary mode:
   - Load .nasm file, run via NasmEval, capture final state.
   - Assemble same .nasm to binary (via external nasm), run via X86BinaryExecutor, capture final state.
   - Assert register/flag/memory match.

4. **`X86NasmRegressionTest.java`** — existing NASM programs:
   - Run all existing `.nasm` test files, verify output unchanged from pre-refactor baseline.

5. **Disassembler consistency:** for random byte sequences, confirm `getLengthAt()` matches instruction length consumed by executor `step()`.

## File Changes

### New
| File | Purpose |
|---|---|
| `src/jCPU/x86/X86Core.java` | Shared CPU state, step/go |
| `src/jCPU/x86/X86BinaryExecutor.java` | Decode + execute raw bytes |
| `src/jCPU/x86/X86Flags.java` | Flag computation helpers |
| `src/nasm/inst/And.java` | New And instruction class (was missing, bug) |
| `test/jCPU/x86/X86FlagsTest.java` | Flag unit tests |
| `test/jCPU/x86/X86BinaryTest.java` | Binary execution integration tests |
| `test/jCPU/x86/X86RoundTripTest.java` | NASM vs binary consistency |

### Modified
| File | Change |
|---|---|
| `src/nasm/NasmEval.java` | Fields replaced with X86Core API calls |
| `src/nasm/NasmVM.java` | Constructs X86Core, wires NasmEval to it |
| `src/nasm/LoadNasm.java` | Fix caseAAndInst: Or → And |
| `src/jx/disass/x86.java` | Drop MCS51 inheritance. Clean up unused Visitor stubs. |
| `src/j51/test/x86.java` | Wire to X86Core + X86BinaryExecutor |
| `j51.conf` | Update line 4 to `jCPU.x86.X86BinaryExecutor` |

### Unchanged
All 8051, ARM, RISC-V, JavaVM, peripheral, GUI files. `build.xml`. All `nasm/inst/*` except new And. `nasm/expr/*`. `jx/disass/Disassembler.java` (inherits from base).

## Success Criteria

1. Raw x86 binary programs execute instruction-by-instruction with correct register/flag/memory state.
2. All 40+ core instructions produce correct results and flags, verified by per-instruction unit tests.
3. Existing `.nasm` test files execute identically before and after refactor.
4. NASM and binary execution produce identical state for the same program.
5. Plugs into existing framework: appears in `j51.conf`, supports `getDecodeAt()`, implements `iCPU`.
6. Every flag bit has dedicated correctness tests.
7. Existing disassembly capability is preserved — text output unchanged.