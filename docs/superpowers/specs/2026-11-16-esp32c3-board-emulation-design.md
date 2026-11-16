# ESP32-C3 Board Emulation — Design Spec

**Project:** neon-engine / Simulator — `jCPU.RiscV` package  
**Scope:** ESP32-C3 board emulation (memory map, UART, GPIO, timer peripherals, flat-binary loader) on top of the existing RISC-V RV32IM core  
**Date:** 2026-11-16  
**Status:** Draft

## Goal

Build a single-core ESP32-C3 board emulation on top of the existing RISC-V RV32IM core (`jCPU.RiscV.IsaSim`). The board loads a flat binary program into SRAM, maps the ESP32-C3 memory regions (SRAM, ROM, peripherals), and provides UART (console output), GPIO, and timer devices reachable via MMIO. Runs through a CLI entry point that loads the flat binary at boot and starts the emulation loop. Multi-core, Wi-Fi, Bluetooth, SPI, I2C, and the full ESP32-C3 peripheral set are out of scope.

## Scope (in)

The following items are in scope:

1. **`jCPU.RiscV.Esp32c3MemoryMap` (new)** — constants: SRAM base `0x3FC80000`, peripheral base `0x3FF40000`, UART/GPIO/timer register offsets; `Map<AddressRange, Peripheral>` of registered devices; `readByte`/`writeByte` (and wider) check device hits first, then fall back to SRAM/ROM backing arrays.
2. **`jCPU.RiscV.UartPeripheral` (new)** — MMIO device; writes print bytes to `System.out`; reads return queued host input or `0` (non-blocking); background daemon thread reads `System.in` and populates a `ConcurrentLinkedQueue<Byte>`.
3. **`jCPU.RiscV.GpioPeripheral` (new)** — MMIO device; output register (writing sets GPIO pin state), input register (reading returns current pin state, defaults to 0), direction/output-enable register.
4. **`jCPU.RiscV.TimerPeripheral` (new)** — MMIO device; free-running counter incremented each CPU cycle; compare register that asserts an interrupt flag on match.
5. **`jCPU.RiscV.Esp32c3Loader` (new)** — reads a flat binary file, writes bytes into SRAM starting at the SRAM base, wires peripherals into the memory map, resets CPU (PC = reset vector), runs the emulation loop; `main(String[])` CLI entry point `Esp32c3Loader <firmware.bin>`.
6. **`jCPU.RiscV.IsaSim` (extended)** — hooks so load/store route through `Esp32c3MemoryMap`; cycle counting (`clock()` increments each `step()`) so `TimerPeripheral` advances; `reset()` sets PC to the ESP32-C3 reset vector and clears registers. No new instruction decode — reuse existing RV32IM decoder.

## Non-goals

- Wi-Fi, Bluetooth, SPI, I2C, ADC, DAC, touch sensor, crypto accelerator, etc.
- RTC, sleep modes, watchdog.
- Second CPU core / APP_PROC vs PRO_CPU.
- ELF parsing — flat binary only.
- GUI beyond host console via UART.
- Changes to the `jCPU` framework (`iCPU`, `VirtualMemorySpace`) — all changes stay inside `jCPU.RiscV` to contain risk.

## Architecture

The simulator drives the CPU through `IsaSim`. Memory accesses route through the board memory map, which intercepts peripheral address ranges and forwards them to registered devices before falling back to SRAM/ROM.

```
Esp32c3Loader (CLI)
  └─ reads flat binary → byte[] into SRAM region @ 0x3FC80000
  └─ IsaSim.reset()          (PC = reset vector 0x40000000, registers cleared)
  └─ emulation loop:
        IsaSim.fetch()       → Esp32c3MemoryMap.readWord(PC)
        RiscV decoder        → decode instruction (RV32IM)
        IsaSim.execute()     → update GPRs / PC / clock
                                 memory accesses → Esp32c3MemoryMap
                                       → SRAM (ByteArrayRegion @ 0x3FC80000)
                                       → ROM (read-only @ 0x40000000)
                                       → Peripheral:
                                             UartPeripheral   @ 0x3FF40000 → System.out / input queue ← System.in
                                             GpioPeripheral   @ 0x3FF44000 → pin state registers
                                             TimerPeripheral  @ 0x3FF5F000 → counter + compare interrupt
```

Each responsibility has one home:
- `IsaSim`: instruction execution + architectural state (unchanged except hooks).
- `Esp32c3MemoryMap`: address decode + MMIO dispatch.
- `UartPeripheral` / `GpioPeripheral` / `TimerPeripheral`: device behavior.
- `Esp32c3Loader`: program loading + emulation bootstrap.

ESP32-C3 real addresses (consistent with the ESP32-C3 Technical Reference Manual):
- SRAM: `0x3FC80000`–`0x3FC88000` (32 KB internal SRAM)
- Peripheral base: `0x3FF40000`
  - UART0: `0x3FF40000`
  - GPIO: `0x3FF44000`
  - Timer Group 0: `0x3FF5F000`

These are configurable constants so they can be tuned.

## Components

### `jCPU.RiscV.IsaSim` (extended)
- RV32IM fetch/decode/execute loop, register file (x0–x31), PC, memory — already implemented.
- Add hooks: `readWord`/`writeWord` route through `Esp32c3MemoryMap` so peripheral addresses are intercepted.
- Add cycle counting (`clock()` increments each `step()`) so `TimerPeripheral` can advance.
- Add `reset()` to set PC to the ESP32-C3 reset vector and clear registers.
- No new instruction decode — reuse existing RV32IM decoder.

### `jCPU.RiscV.Esp32c3MemoryMap` (new)
- `Map<AddressRange, Peripheral>` of registered devices.
- `readByte`/`writeByte` (and wider) check device hits first, then fall back to `PhysicalMemorySpace`-style SRAM/ROM backing arrays.
- Constants for SRAM base `0x3FC80000`, peripheral base `0x3FF40000`, UART/GPIO/Timer offsets.
- SRAM: `ByteArrayRegion` at `0x3FC80000`; ROM (read-only) at `0x40000000`.

### `jCPU.RiscV.UartPeripheral` (new)
- `byte read(int offset)` — returns queued input byte or `0` if none available (non-blocking).
- `void write(int offset, byte value)` → `System.out.print((char) value)`.
- Background daemon thread reads `System.in` and populates a `ConcurrentLinkedQueue<Byte>`.

### `jCPU.RiscV.GpioPeripheral` (new)
- Output register: writing sets GPIO pin state.
- Input register: reading returns current pin state (defaults to 0; can be set programmatically).
- Direction/output-enable register.

### `jCPU.RiscV.TimerPeripheral` (new)
- Free-running 32/64-bit counter incremented each CPU cycle.
- Compare register: when counter == compare, assert an interrupt flag.
- Interrupt line wired to CPU interrupt input (ISA Sim interrupt hook).

### `jCPU.RiscV.Esp32c3Loader` (new)
- Reads a flat binary file; writes bytes into SRAM starting at SRAM base.
- `CPU.reset()` (PC = reset vector), wires peripherals into the memory map.
- Runs the fetch/decode/execute loop until halt/exception.
- `main(String[])` CLI: `Esp32c3Loader <firmware.bin>`.

## Data flow

1. **Boot:** `Esp32c3Loader` reads the flat binary → writes bytes into the SRAM `ByteArrayRegion` starting at `0x3FC80000`.
2. **Reset:** `IsaSim.reset()` sets `PC` to the reset vector (`0x40000000` for ESP32-C3) and clears registers.
3. **Execute loop:** `IsaSim` fetches a word from `Esp32c3MemoryMap.readWord(PC)`, decodes via the RV32IM decoder, executes (updating GPRs/PC), repeats.
4. **MMIO:** If a load/store address falls in `[0x3FF40000, 0x3FF5FFFF]`, `Esp32c3MemoryMap` dispatches to `UartPeripheral`/`GpioPeripheral`/`TimerPeripheral` instead of SRAM.
5. **Console:** `UartPeripheral.write` → `System.out.print`; a background thread fills an input queue from `System.in` for reads.
6. **Timer:** `TimerPeripheral` counter increments each `IsaSim.step()` cycle; on compare match it asserts an interrupt flag.

## Error handling

- `BusErrorException` — invalid/unmapped memory access (carries address and access type); propagated to the CLI and reported.
- `UnimplementedInstructionException` — an RV32IM instruction has no decoder; simulation stops with a message including PC and opcode.
- `AlignmentException` — unaligned word/halfword access where RISC-V forbids it.
- `IllegalInstructionException` — a valid opcode but reserved/illegal encoding; halts with PC context.
- All exceptions carry PC context so the user can locate the failing instruction.
- UART reads are non-blocking (return `0` when no input) — no blocking I/O hangs the emulation loop.

## Testing

1. **Unit:** `Esp32c3MemoryMap` with registered `UartPeripheral` — verify writes to the UART address print to console and reads return queued input.
2. **Unit:** `GpioPeripheral` — verify output register write is reflected in input register read, and direction register behaves.
3. **Unit:** `TimerPeripheral` — verify counter increments per cycle and compare match asserts the interrupt flag.
4. **Integration:** Assemble a tiny RV32IM program (load a value, `SB` to UART address to print a char, `B .`/jump to self to halt), load it, run, and verify console output and final register state.
5. **End-to-end:** Load a real flat-binary firmware that prints text via UART and halts; verify output appears on the host console.
6. **Regression:** Existing `jCPU.RiscV.IsaSim`/RV32IM behavior continues to work unchanged.

## Acceptance criteria

- A flat-binary RV32IM firmware loads at `0x3FC80000` and executes, producing expected console output via UART at `0x3FF40000`.
- `SB` to the UART address prints the byte to `System.out`; reads return queued host input or `0`.
- `GpioPeripheral` output register write is reflected in input register read.
- `TimerPeripheral` counter advances per cycle and asserts interrupt on compare match.
- Instruction decode covers the RV32IM subset; undefined instructions halt with a clear PC-referenced message.
- Memory accesses to unmapped addresses raise `BusErrorException` with address context.
- No regressions in existing `jCPU.RiscV` behavior.
