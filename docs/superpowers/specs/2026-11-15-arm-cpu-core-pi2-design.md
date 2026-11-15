# ARM CPU Core (Raspberry Pi 2 / ARMv7) — Design Spec

**Project:** neon-engine / Simulator — `jCPU.arm` package  
**Scope:** ARMv7 processor core, board memory map, MMIO peripheral framework, UART console, and `kernel.img` loader for bare-metal Pi 2 emulation  
**Date:** 2026-11-15  
**Status:** Draft

## Goal

Build a single-core ARMv7 CPU core able to load and execute a bare-metal Raspberry Pi 2 `kernel.img`. The core fetches, decodes, and executes ARM/A32 instructions, maintains the register file and CPSR, routes memory accesses through an MMIO memory space so peripherals (UART) are reachable, and runs under a CLI entry point that loads `kernel.img` at boot and starts the emulation loop. Multi-core, GPU, SD card, USB, and framebuffer are out of scope for this sub-project.

## Scope (in)

The following items are in scope:

1. **`jCPU.arm.CPU` (ARMv7)** — register file (R0–R12, SP, LR, PC), CPSR with condition flags, processor modes (User, System, IRQ, FIQ, Supervisor, Abort, Undefined, Monitor), fetch/decode/execute loop for the ARM/A32 instruction subset.
2. **`jCPU.arm.iCPU`** — the CPU interface contract the rest of the simulator depends on; kept stable as the core evolves.
3. **`jCPU.arm.VirtualMemorySpace` / `PhysicalMemorySpace`** — memory interface and MMIO dispatcher; intercepts accesses to peripheral address ranges before falling back to RAM.
4. **`jCPU.arm.MemoryRegion` / `ByteArrayRegion` / `ByteBackedRegion`** — backing storage for RAM and device regions.
5. **`jCPU.arm.UartPeripheral`** — MMIO device mapped within the Pi 2 peripheral base; writes → `System.out`, reads → input queue populated by a background thread reading `System.in`.
6. **`jCPU.arm.Debugger`** — step/breakpoint support for observing execution.
7. **`Pi2MemoryMap` (new)** — constants: RAM base `0x00000000`, Pi 2 peripheral base `0x3F000000`, UART offset within that range.
8. **ARMv7 decoder / executor additions (new or extended)** — decode and execute the initial ARMv7 instruction subset (data processing, branches, load/store, multiply).
9. **`Pi2Loader` / CLI entry (new)** — loads `kernel.img` into RAM at `0x00000000`, resets the CPU (PC = 0, Supervisor mode), runs the emulation loop.

## Non-goals

- Multi-core / SMP (Pi 2 has 4 cores; single-core only for this sub-project).
- GPU / VideoCore, SD card, USB, Ethernet, framebuffer, timer, interrupt controller.
- Thumb/Thumb-2 instruction set (ARM/A32 only).
- ELF parsing — `kernel.img` is treated as a flat binary image loaded at `0x00000000`.
- OS / Linux booting — bare-metal programs only.
- Any GUI beyond host console output via UART.

## Architecture

The simulator drives the CPU through `iCPU`. The CPU's memory accesses go through `VirtualMemorySpace`, which acts as the MMIO dispatcher and forwards peripheral-range accesses to registered `Peripheral` devices (initially just `UartPeripheral`). RAM is a `ByteArrayRegion` starting at `0x00000000`.

```
Pi2Loader (CLI)
  └─ loads kernel.img → ByteArrayRegion (RAM @ 0x00000000)
  └─ CPU.reset()        (PC = 0, Supervisor mode)
  └─ emulation loop:
        CPU.fetch()     → VirtualMemorySpace.readWord(PC)
        ARMv7Decoder    → decode instruction
        CPU.execute()   → update GPRs / CPSR / PC
                            memory accesses → VirtualMemorySpace
                              → RAM (ByteArrayRegion)
                              → Peripheral (UartPeripheral @ 0x3Fxxxxxx)
                                → System.out / input queue ← System.in
```

Each responsibility has one home:
- `CPU`: instruction execution + architectural state.
- `VirtualMemorySpace`: MMIO dispatch + memory arbitration.
- `Peripheral` / `UartPeripheral`: device behavior.
- `Pi2Loader`: program loading + emulation bootstrap.

## Components

### `jCPU.arm.CPU` (extended)
- Register file: R0–R12, banked SP/LR/PC per mode, CPSR.
- Processor modes: User, System, IRQ, FIQ, Supervisor, Abort, Undefined, Monitor.
- Fetch/decode/execute loop for ARM/A32.
- `readPC()` returns `pc + 4` (ARM state); branches clear the Thumb bit since Thumb is out of scope.
- Exposes `iCPU` contract.

### `jCPU.arm.iCPU` (unchanged)
- Stable interface; no new methods required for this sub-project beyond what already exists.

### `jCPU.arm.VirtualMemorySpace` / `PhysicalMemorySpace`
- Maintains `Map<AddressRange, Peripheral>` of registered devices.
- Intercepts `readByte`/`writeByte` (and wider accesses) to check for device hits before falling back to `PhysicalMemorySpace`.
- Existing MMIO support (all widths, thread-safe registry) is reused as-is.

### `jCPU.arm.MemoryRegion` / `ByteArrayRegion` / `ByteBackedRegion`
- RAM backing store; no changes required beyond instantiation at the Pi 2 RAM base.

### `jCPU.arm.UartPeripheral` (existing, reused)
- `byte read(int offset)` — returns queued input byte or `0` if none available (non-blocking).
- `void write(int offset, byte value)` → `System.out.print((char) value)`.
- Background daemon thread reads `System.in` and populates a `ConcurrentLinkedQueue<Byte>`.

### `jCPU.arm.Debugger` (existing)
- Step / breakpoint hooks; no changes required for this sub-project.

### `Pi2MemoryMap` (new)
- `RAM_BASE = 0x00000000`
- `PERIPHERAL_BASE = 0x3F000000` (Pi 2)
- `UART_OFFSET` within the peripheral range (default UART address configurable).

### ARMv7 decoder / executor (new or extended)
- Initial ARM/A32 subset: data-processing (MOV, ADD, SUB, AND, ORR, EOR, CMP, etc.), branches (B, BL, BX), load/store (LDR, STR, LDRB, STRB, LDRH, STRH), multiply (MUL, MLA).
- Decodes condition codes from CPSR; updates N/Z/C/V flags on flag-setting forms.

### `Pi2Loader` / CLI entry (new)
- Reads `kernel.img` as a flat binary; writes bytes into RAM starting at `0x00000000`.
- CPU reset: `PC = 0`, CPSR → Supervisor mode, other registers cleared.
- Runs the fetch/decode/execute loop until halt or exception.
- Reports exceptions with PC context.

## Data flow

1. **Boot:** `Pi2Loader` reads `kernel.img` → writes bytes into a `ByteArrayRegion` (RAM) starting at `0x00000000`.
2. **Reset:** CPU sets `PC = 0`, CPSR → Supervisor mode, registers cleared.
3. **Execute loop:** CPU fetches 4 bytes from `VirtualMemorySpace.readWord(PC)`, decodes via the ARMv7 decoder, executes (updating GPRs/CPSR/PC), repeats.
4. **MMIO:** If an access falls in `[0x3F000000, ...)`, `VirtualMemorySpace` dispatches to `UartPeripheral` instead of RAM.
5. **Console:** `UartPeripheral.write` → `System.out.print`; a background thread fills an input queue from `System.in` for reads.

## Error handling

- `BusErrorException` — invalid/unmapped memory access (carries address and access type); propagated to the CLI and reported.
- `UndefinedException` / `UnimplementedInstructionException` — an instruction has no decoder; simulation stops with a message including PC and opcode.
- `AlignmentException` — unaligned word/halfword access where the architecture forbids it.
- `FatalException` — unrecoverable CPU state; halts emulation.
- All exceptions carry PC context so the user can locate the failing instruction.

## Testing

1. **Unit:** `CPU` register file and CPSR flag updates for a few instructions in isolation (no memory).
2. **Unit:** `VirtualMemorySpace` with a registered `UartPeripheral` — verify writes to the UART address print to console and reads return queued input.
3. **Integration:** Assemble a tiny ARMv7 program (move a value, `STRB` to UART address, `B .` to hang), load it, run, and verify console output and final register state.
4. **End-to-end:** Load a real `kernel.img` that prints text via UART and halts; verify output appears on the host console.

## Acceptance criteria

- A bare-metal ARMv7 `kernel.img` loads at `0x00000000` and executes, producing expected console output via the UART at the Pi 2 peripheral base.
- `STRB` to the UART address prints the byte to `System.out`; reads return queued host input or `0`.
- Instruction decode covers the initial ARM/A32 subset; undefined instructions halt with a clear PC-referenced message.
- Memory accesses to unmapped addresses raise `BusErrorException` with address context.
- No regressions in existing `jCPU.arm` behavior; existing `UartPeripheral`, `VirtualMemorySpace`, and `Debugger` continue to work unchanged.
