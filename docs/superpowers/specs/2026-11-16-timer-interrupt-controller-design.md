# Timer + Interrupt Controller — Design Spec

**Project:** neon-engine / Simulator — `jCPU.arm` package (Raspberry Pi 2 bare-metal emulator)  
**Scope:** BCM2835 system timer at `0x3F003000` with free-running 64-bit counter, 4 compare channels, match IRQs, and write-1-to-clear status; interrupt controller built on the existing per-source `CPU.setIRQ`/`clearIRQ` TreeSet IRQ line  
**Date:** 2026-11-16  
**Status:** Draft (pending review)

## Goal

Provide a programmable interval timer and interrupt delivery path for bare-metal Raspberry Pi 2 programs. Bare-metal code can read a free-running counter, set compare values on four channels, and receive IRQs when compare values are reached. IRQs from the timer and the EMMC controller (sub-project 2) share the existing multi-source IRQ line so software can wait on either device.

## Scope (in)

The following items are in scope:

1. **`SystemTimer` (new)** — MMIO device at `0x3F003000`: free-running 64-bit counter (`CLO`/`CHI`), control/status register (`CS` with per-channel match flags + write-1-to-clear), four compare registers (`C0`–`C3`), IRQ assertion on match.
2. **`Pi2MemoryMap` (modified)** — add `SYSTEM_TIMER_BASE = 0x3F003000`.
3. **`CPU` (additive change)** — track total executed cycles (`totalCycles`, incremented once per instruction in `execute()`); expose `getTotalCycles()`; optionally wire a `SystemTimer` and tick it before each IRQ check so WFI wake-up works with no MMIO in flight.
4. **`Pi2Loader` (modified)** — instantiate `SystemTimer`, register it at `SYSTEM_TIMER_BASE` (size `0x1000`), wire it to the CPU.
5. **IRQ line** — reuse the existing `CPU.setIRQ(Object)` / `clearIRQ(Object)` / `haveIRQ()` TreeSet-based multi-source IRQ line; `SystemTimer` implements `Comparable` (unique `irqId`) so its token works with the same pattern as `EmmcController`.

## Non-goals

- ARM generic timer via CP15 system registers (CNTPCT/CNTFRQ) — requires CPU coprocessor support, deferred to a later sub-project.
- BCM2835 local interrupt controller MMIO at `0x3F00B000+` — software polls device `STATUS`/`INTERRUPT` registers to learn the IRQ source; deferred if needed.
- FIQ support, IRQ priority, vectored interrupts, nested interrupts.
- Wall-clock / real-time timer advancement — timer advances on instruction cycles only.
- Multi-core SMP; GPU; USB; Ethernet; framebuffer; GPIO.

## Architecture

The timer is a pure-MMIO peripheral registered over the `0x3F003000` page in `VirtualMemorySpace`. The CPU tracks how many instruction cycles it has executed. The timer converts cycle delta into counter ticks (`CYCLES_PER_TICK = 1` by default: one simulated timer tick per CPU cycle) and advances its 64-bit free-running counter lazily — on MMIO access and before each IRQ check so that WFI-based waiting works even when no peripheral access is in flight.

```
bare-metal code
   └─ VM read CLO/CHI      -> SystemTimer @ 0x3F003000 (counter advances from CPU cycle delta)
   └─ VM write C0 = future -> SystemTimer compare channel 0
                               └─ counter reaches C0
                                   └─ CS match flag 0 set
                                   └─ CPU.setIRQ(this) -> IRQ line asserted
                                   └─ CPU takes IRQ exception (vector 0x18)
   └─ VM write CS bit0=1   -> write-1-to-clear match flag 0 -> CPU.clearIRQ(this)
   └─ VM read CS           -> match flags (which channel(s) fired)
```

### IRQ sharing with EMMC controller

Both `SystemTimer` and `EmmcController` are `Comparable` tokens in the CPU's `TreeSet<Object> irqs`. Asserting IRQ from either device adds its token; clearing removes its token. `haveIRQ()` is true while any token remains, so bare-metal software waiting on either device wakes correctly. Software determines the actual source by polling each device's status/interrupt register.

## Components

### `SystemTimer` (new)

- Implements `jCPU.Peripheral, Comparable<SystemTimer>`.
- IRQ token: unique `irqId` from an `AtomicLong` sequence; `compareTo` compares `irqId`. This mirrors `EmmcController` exactly.
- Constructor takes `CPU cpu` (may be null for standalone tests).
- Register offsets (from `0x3F003000`):
  - `CS` `0x00` — Control/Status. Bits [3:0] = match flags for channels 0–3. Write-1-to-clear: writing `1` to bit N clears match flag N and (if no flags remain) calls `cpu.clearIRQ(this)`.
  - `CLO` `0x04` — Counter lower 32 bits, read-only.
  - `CHI` `0x08` — Counter upper 32 bits, read-only.
  - `C0` `0x0C` — Compare channel 0, read/write.
  - `C1` `0x10` — Compare channel 1, read/write.
  - `C2` `0x14` — Compare channel 2, read/write.
  - `C3` `0x18` — Compare channel 3, read/write.
- `byte read(int offset)` / `void write(int offset, byte value)` — implements `Peripheral`; multi-byte words assembled from byte accesses by `VirtualMemorySpace`.
- `public static final long CYCLES_PER_TICK = 1` — simulated timer ticks per CPU cycle.
- Counter logic: `counter = totalCycles / CYCLES_PER_TICK`, advanced lazily via `tickTo(long targetCycles)`. Reads of `CLO`/`CHI` and the match check both call `tickTo(cpu.getTotalCycles())` so the counter is always current relative to executed instructions.
- Compare match: when `counter >= compare[N]`, set CS bit N and call `cpu.setIRQ(this)`. A channel stays matched (flag set) until software write-1-clears it; repeated matches do not re-assert while the flag is already set (flag stays set, IRQ stays asserted).
- On each `tickTo`, after advancing the counter, re-check all four channels against their compare values and set flags / assert IRQ for any newly-matched channel whose flag is not already set.
- Write-1-to-clear CS bit N: clear flag N; recompute whether any flag remains set; if none, call `cpu.clearIRQ(this)`. This correctly handles the case where two channels are pending and only one is acknowledged.

### `Pi2MemoryMap` (modified)

- Add `public static final int SYSTEM_TIMER_BASE = 0x3F003000;`

### `CPU` (additive change)

- Add `private long totalCycles = 0;` — incremented once per instruction in `execute()`.
- Add `public long getTotalCycles()` — returns `totalCycles`.
- Add `private SystemTimer systemTimer = null;` + `public void setSystemTimer(SystemTimer t)`.
- In `execute(int budget)`, before the existing IRQ check (`if((cpsr & (1 << CPSR_BIT_I)) == 0 && haveIRQ())`), call `systemTimer.tickTo(totalCycles)` if `systemTimer != null`. This guarantees WFI wake-up works even when no MMIO access occurs between instructions.

### `Pi2Loader` (modified)

- In `loadImageWithSd` (and/or `main` path): create `SystemTimer`, register at `SYSTEM_TIMER_BASE` (size `0x1000`) via `registerPeripheral(base, size, p)`, wire to CPU via `cpu.setSystemTimer(timer)`.

## Data flow

1. **Boot:** `Pi2Loader` loads `kernel.img` at `0x0`; creates `SystemTimer`, registers it at `0x3F003000` (size `0x1000`), wires it to the CPU.
2. **Read counter:** Software reads `CLO`/`CHI` → timer advances counter from `CPU.getTotalCycles()` delta → returns current counter value.
3. **Set compare:** Software writes `C0`–`C3` to future counter values.
4. **Match fires:** Counter reaches a compare value → CS match flag set → `CPU.setIRQ(this)` → CPU takes IRQ exception on next instruction (if IRQs unmasked in CPSR).
5. **Acknowledge:** Software writes CS bit N = `1` → clears match flag N → if no flags remain, `CPU.clearIRQ(this)` → IRQ line deasserts.

## Error handling

- **Registers outside `0x00`–`0x18`** within the `0x1000` window: reads return `0`, writes ignored with a warning log (consistent with `EmmcController`).
- **Null CPU:** `SystemTimer` constructed without a CPU works for register unit tests; `tickTo` and IRQ assertion are no-ops when `cpu == null`.
- **Compare value already passed:** match flag sets immediately on next tick; IRQ asserted on next IRQ check (software must handle "already-expired" compares).
- **Counter wrap:** 64-bit counter wraps naturally; compare logic uses unsigned `>=` semantics so wraps are handled correctly.

## Testing Plan

1. **Unit:** `SystemTimerRegisterTest` — CS/CLO/CHI/C0–C3 read/write; out-of-range offsets return 0 / ignored with warning.
2. **Unit:** `SystemTimerCounterAdvanceTest` — counter advances with executed CPU cycles; `CYCLES_PER_TICK = 1` yields one tick per instruction.
3. **Unit:** `SystemTimerCompareTest` — setting `C0 = counter + N`, executing N instructions, observing CS match flag 0 and `haveIRQ() == true`.
4. **Unit:** `SystemTimerWrite1ClearTest` — write-1-to-clear CS bit clears flag and deasserts IRQ.
5. **Unit:** `SystemTimerMultiChannelTest` — channels 0 and 1 both pending; clearing channel 0 does NOT deassert IRQ until channel 1 is also cleared (multi-source correctness).
6. **Unit:** `SystemTimerWfiTest` — WFI (`waitingForInterrupt`) wakes when timer compare fires with no MMIO access between instructions.
7. **Integration:** `TimerEmmcIrqTest` — `SystemTimer` and `EmmcController` assert IRQ simultaneously; `haveIRQ()` reflects both; clearing one leaves the other asserted.
8. **Loader:** `Pi2LoaderTimerTest` — `Pi2Loader` wires the timer at `SYSTEM_TIMER_BASE` and it is reachable through MMIO.

## Acceptance criteria

- Bare-metal code can read `CLO`/`CHI` and observe the counter advancing with executed cycles.
- Setting `C0` to a future value and executing enough cycles sets CS match flag 0 and asserts the CPU IRQ line.
- Write-1-to-clear on CS bit N clears flag N; IRQ deasserts only when no match flags remain.
- Multiple compare channels can be pending simultaneously; acknowledging one does not clear IRQ for another.
- `SystemTimer` and `EmmcController` can assert IRQ at the same time; `haveIRQ()` is true while either is pending.
- `Pi2Loader` wires the timer at `0x3F003000` without exception; timer reachable through MMIO.
- No regressions in existing `jCPU.arm` behavior (existing UART, EMMC, memory, CPU tests unchanged).
