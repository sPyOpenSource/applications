# SD Block Device (EMMC @ 0x3F200000) — Design Spec

**Project:** neon-engine / Simulator — `jCPU.arm` package (Raspberry Pi 2 bare-metal emulator)  
**Scope:** SD card block device controller at Pi 2 EMMC address `0x3F200000`, backed by a host SD image file; SD command subset (CMD0, ACMD41, CMD16, CMD17, CMD24) with DMA to RAM and completion IRQ  
**Date:** 2026-11-16  
**Status:** Draft

## Goal

Provide an SD block device for bare-metal Raspberry Pi 2 programs. Bare-metal code talks to an EMMC host controller at `0x3F200000` to initialize a card and read/write 512-byte sectors from a host-backed SD image file. The controller implements the SD commands Pi bare-metal SD drivers actually use, with DMA to system RAM and completion via IRQ. Unimplemented SD commands return an error status without halting simulation.

## Scope (in)

The following items are in scope:

1. **`SdCard` (new)** — raw sector backing store: reads/writes 512-byte sectors from a host image file; reports sector count (`fileSize / 512`); rejects out-of-range sector accesses.
2. **`EmmcController` (new)** — MMIO device at `0x3F200000`: EMMC register subset (`ARG1`, `ARG2`, `CMDCMD`, `CMDTMO`, `CMDRESP0`–`CMDRESP3`, `DATA`, `STATUS`, `INTERRUPT`, `IRQ_MASK`), SD command state machine, data buffer, DMA to/from system RAM via `VirtualMemorySpace`, completion IRQ line.
3. **`VirtualMemorySpace` (modified)** — register `EmmcController` over the `0x3F200000` address range using the existing MMIO dispatch API (`registerPeripheral`).
4. **`Pi2MemoryMap` (modified)** — add `EMMC_BASE = 0x3F200000`.
5. **`CPU` (modified)** — expose the IRQ line so `EmmcController` can assert a completion interrupt.
6. **`Pi2Loader` (modified)** — accept optional second argument (SD image path); pass it through to `EmmcController` construction.
7. **SD command subset** — `CMD0` (GO_IDLE_STATE), `ACMD41` (SEND_OP_COND), `CMD16` (SET_BLOCKLEN), `CMD17` (READ_SINGLE_BLOCK), `CMD24` (WRITE_SINGLE_BLOCK). Other commands return an error status response; simulation continues.

## Non-goals

- Full EMMC register set — only the registers above are implemented; others read as `0`, writes ignored, with a warning log.
- Multi-block transfers, CSD/SCR reads, card type detection beyond "ready", write-protect, card detection/insertion, SDIO.
- SPI mode, UHS modes, hardware flow control, clock gating.
- Filesystem support (FAT, etc.) — raw sector access only.
- Multi-core SMP; GPU; USB; Ethernet; framebuffer.

## Architecture

The SD card image is a flat file of 512-byte sectors on the host. `SdCard` opens it and exposes sector read/write. `EmmcController` is an MMIO device registered at `EMMC_BASE` in `VirtualMemorySpace`. Bare-metal code programs `ARG1` (sector address), `CMDCMD` (command index), and for writes programs `DATA` (sector contents), then polls `STATUS` until busy clears. On completion, the controller DMA-copies the 512-byte sector between its buffer and system RAM via `instructionReadWord`/`instructionWriteWord` on the CPU's memory space, then asserts IRQ.

```
bare-metal code
   └─ VM write ARG1 = sector   -> EmmcController @ 0x3F200000
   └─ VM write CMDCMD = 17     -> EmmcController state machine
                                  └─ SdCard.readSector(sector) -> buffer
                                  └─ DMA: buffer -> RAM (VirtualMemorySpace)
                                  └─ assert IRQ -> CPU
   └─ VM read STATUS           -> busy clears, IRQ pending
   └─ VM read CMDRESP0         -> response (success / error)
```

Each responsibility has one home:
- `SdCard`: image file I/O + sector bounds.
- `EmmcController`: register set, command decode, DMA, IRQ.
- `VirtualMemorySpace`: MMIO arbitration at `EMMC_BASE`.
- `CPU`: IRQ line delivery.
- `Pi2Loader`: image path plumbing + boot.

## Components

### `SdCard` (new)
- Constructor takes host image path; opens file (read/write), computes `sectorCount = fileSize / 512`.
- `byte[] readSector(int sector)` — validates bounds, reads 512 bytes; out-of-range returns error status.
- `void writeSector(int sector, byte[] data)` — validates bounds, writes 512 bytes, flushes.
- `int getSectorCount()` — total sectors.

### `EmmcController` (new)
- Register offsets (from `0x3F200000`): `ARG1` `0x00`, `ARG2` `0x04`, `CMDCMD` `0x08`, `CMDTMO` `0x0C`, `CMDRESP0` `0x10`, `CMDRESP1` `0x14`, `CMDRESP2` `0x18`, `CMDRESP3` `0x1C`, `DATA` `0x20`, `STATUS` `0x24`, `INTERRUPT` `0x28`, `IRQ_MASK` `0x2C`.
- `byte read(int offset)` / `void write(int offset, byte value)` — implements `Peripheral`; multi-byte word accesses build on byte ops (existing MMIO width support reused).
- SD command state machine: decode `CMDCMD` into command index + ACMD flag; handle CMD0 / ACMD41 / CMD16 / CMD17 / CMD24; set `STATUS` busy during transfer; clear on completion.
- DMA: on CMD17 completion, copy buffer → RAM via CPU `instructionWriteWord`; on CMD24 completion, copy RAM → buffer via `instructionReadWord`, then `SdCard.writeSector`.
- Completion IRQ: set `INTERRUPT` bit; if `IRQ_MASK` enables, assert CPU IRQ line.

### `VirtualMemorySpace` (modified)
- `registerPeripheral(0x3F200000, emmcController)` — existing API, no signature change.

### `Pi2MemoryMap` (modified)
- Add `public static final int EMMC_BASE = 0x3F200000;`.

### `CPU` (modified)
- Expose IRQ delivery: `EmmcController` asserts IRQ via a callback/interface the CPU implements (e.g., `setIrq(boolean)` or `raiseIrq()`). Exact wiring decided at implementation; existing exception/interrupt infrastructure (`generateIRQException`) reused.

### `Pi2Loader` (modified)
- `main(String[])`: `args[0]` = `kernel.img` (required), `args[1]` = optional SD image path.
- Construct `SdCard` + `EmmcController` when SD image provided; register controller in `VirtualMemorySpace`.

### SD command subset
| Command | Name | Behavior |
|---------|------|----------|
| CMD0 | GO_IDLE_STATE | Card idle; R1 response; no data. |
| ACMD41 | SEND_OP_COND | Card "ready" (always true — no physical card); R1 response. |
| CMD16 | SET_BLOCKLEN | Block length 512; acknowledged. |
| CMD17 | READ_SINGLE_BLOCK | Read sector from image → DMA to RAM → IRQ. |
| CMD24 | WRITE_SINGLE_BLOCK | DMA from RAM → write sector to image → IRQ. |
| other | — | Error status response; no state change. |

## Data flow

1. **Boot:** `Pi2Loader` loads `kernel.img` at `0x00000000`; if a second arg is given, opens it as the SD image via `SdCard`, builds `EmmcController`, registers it at `0x3F200000` in `VirtualMemorySpace`.
2. **Card init:** Bare-metal code writes `CMD0` then `ACMD41` → controller returns R1 responses via `CMDRESP0`.
3. **Read sector:** Code writes sector number to `ARG1`, issues `CMD17` → controller reads sector from image into buffer, DMA-copies 512 bytes to system RAM via `instructionWriteWord`, sets `STATUS` complete, asserts IRQ.
4. **Write sector:** Code writes sector to `ARG1`, programs sector bytes via `DATA` register, issues `CMD24` → controller DMA-copies RAM → buffer via `instructionReadWord`, writes sector to image, sets `STATUS` complete, asserts IRQ.
5. **Sync:** Code polls `STATUS` / `INTERRUPT` until busy clears and IRQ pending.

## Error handling

- **Sector out of range** (sector ≥ sectorCount) → `STATUS` error bit set, IRQ asserted, no data transferred outside image bounds.
- **Image file missing/unreadable** → `EmmcController`/`SdCard` construction throws; `Pi2Loader` prints a clear message and exits non-zero.
- **Unsupported SD command** → response with error status bit set; simulation continues (no halt).
- **DMA overrun / unaligned transfer** → `STATUS` error bit; transfer aborted.
- **Controller busy when new command issued** → command ignored / status bit set (to be pinned down at implementation).

## Testing Plan

1. **Unit:** `SdCard` sector read/write with bounds checking — sector ≥ sector count returns error, valid sectors round-trip.
2. **Unit:** `EmmcController` register reads/writes; `CMDCMD` decodes command index; unknown command sets error status.
3. **Integration:** Write a known sector via `CMD24` (program `DATA`, issue command), then read it back via `CMD17` — verify bytes round-trip through RAM and persist to the image file.
4. **CLI:** `Pi2Loader kernel.img sd.img` boots; `EmmcController` initializes without exception; no crash on `CMD0`/`ACMD41` sequence.

## Acceptance criteria

- Bare-metal code can initialize the "card" via `CMD0`/`ACMD41` and read `CMDRESP0` without simulation halt.
- A 512-byte sector written via `CMD24` and read back via `CMD17` returns identical bytes; changes persist in the host image file.
- Out-of-range sector access sets `STATUS` error bit and does not corrupt memory.
- Unknown SD commands return error status; simulation continues.
- `Pi2Loader` accepts an optional SD image path and starts the controller without exception.
- No regressions in existing `jCPU.arm` behavior (existing UART, memory, CPU tests unchanged).
