# SD Block Device (EMMC @ 0x3F200000) — Design Spec

**Project:** neon-engine / Simulator — `jCPU.arm` package (Raspberry Pi 2 bare-metal emulator)  
**Scope:** SD card block device controller at Pi 2 EMMC address `0x3F200000`, backed by a host SD image file; SD command subset (CMD0, ACMD41, CMD16, CMD17, CMD24) with DMA to RAM and completion IRQ  
**Date:** 2026-11-16  
**Status:** Implemented (2026-11-16) — see "Implementation notes" at the end

## Goal

Provide an SD block device for bare-metal Raspberry Pi 2 programs. Bare-metal code talks to an EMMC host controller at `0x3F200000` to initialize a card and read/write 512-byte sectors from a host-backed SD image file. The controller implements the SD commands Pi bare-metal SD drivers actually use, with DMA to system RAM and completion via IRQ. Unimplemented SD commands return an error status without halting simulation.

## Scope (in)

The following items are in scope:

1. **`SdCard` (new)** — raw sector backing store: reads/writes 512-byte sectors from a host image file; reports sector count (`fileSize / 512`); rejects out-of-range sector accesses.
2. **`EmmcController` (new)** — MMIO device at `0x3F200000`: EMMC register subset (`ARG1`, `ARG2`, `CMDCMD`, `CMDTMO`, `CMDRESP0`–`CMDRESP3`, `DATA`, `STATUS`, `INTERRUPT`, `IRQ_MASK`), SD command state machine, data buffer, DMA to/from system RAM via `VirtualMemorySpace`, completion IRQ line.
3. **`VirtualMemorySpace` (modified)** — add range-based MMIO registration so a device can own a register window; register `EmmcController` over `0x3F200000`–`0x3F200FFF`.
4. **`Pi2MemoryMap` (modified)** — add `EMMC_BASE = 0x3F200000`.
5. **`CPU` (unchanged)** — reuse the existing `setIRQ(Object)` / `clearIRQ(Object)` / `haveIRQ()` IRQ-source API.
6. **`Pi2Loader` (modified)** — accept optional second argument (SD image path); pass it through to `EmmcController` construction.
7. **SD command subset** — `CMD0` (GO_IDLE_STATE), `ACMD41` (SEND_OP_COND), `CMD16` (SET_BLOCKLEN), `CMD17` (READ_SINGLE_BLOCK), `CMD24` (WRITE_SINGLE_BLOCK). Other commands return an error status response; simulation continues.

## Non-goals

- Full EMMC register set — only the registers above are implemented; others read as `0`, writes ignored, with a warning log.
- Multi-block transfers, CSD/SCR reads, card type detection beyond "ready", write-protect, card detection/insertion, SDIO.
- SPI mode, UHS modes, hardware flow control, clock gating.
- Filesystem support (FAT, etc.) — raw sector access only.
- Multi-core SMP; GPU; USB; Ethernet; framebuffer.

## Architecture

The SD card image is a flat file of 512-byte sectors on the host. `SdCard` opens it and exposes sector read/write. `EmmcController` is an MMIO device registered over the `0x3F200000` page in `VirtualMemorySpace`. Bare-metal code programs `ARG1` (sector address) and `DATA` (the **RAM DMA address**), then writes `CMDCMD` (command index) to start the transfer, and polls `STATUS` until busy clears. On completion, the controller DMA-copies the 512-byte sector between the image and system RAM via `instructionReadWord`/`instructionWriteWord` on the CPU's memory space, then sets the `INTERRUPT` pending bit and raises the CPU IRQ line.

```
bare-metal code
   └─ VM write ARG1 = sector    -> EmmcController @ 0x3F200000
   └─ VM write DATA = RAM addr  -> EmmcController
   └─ VM write CMDCMD = 17      -> EmmcController state machine
                                  └─ SdCard.readSector(sector)
                                  └─ DMA: sector -> RAM (VirtualMemorySpace)
                                  └─ set INTERRUPT, assert IRQ -> CPU
   └─ VM read STATUS            -> busy cleared / error bit
   └─ VM read CMDRESP0          -> response
```

Each responsibility has one home:
- `SdCard`: image file I/O + sector bounds.
- `EmmcController`: register set, command decode, DMA, IRQ.
- `VirtualMemorySpace`: MMIO arbitration at `EMMC_BASE`.
- `CPU`: IRQ line delivery.
- `Pi2Loader`: image path plumbing + boot.

## Components

### `SdCard` (new)
- Constructor takes host image path; opens the file read/write, computes `sectorCount = fileSize / 512`.
- `byte[] readSector(int sector)` — validates bounds, returns 512 bytes; out-of-range or short read throws.
- `void writeSector(int sector, byte[] data)` — validates bounds and a 512-byte buffer, writes the sector.
- `int getSectorCount()` — total sectors.
- `close()` — closes the backing file (`AutoCloseable`).

### `EmmcController` (new)
- Register offsets (from `0x3F200000`): `ARG1` `0x00`, `ARG2` `0x04`, `CMDCMD` `0x08`, `CMDTMO` `0x0C`, `CMDRESP0` `0x10`, `CMDRESP1` `0x14`, `CMDRESP2` `0x18`, `CMDRESP3` `0x1C`, `DATA` `0x20`, `STATUS` `0x24`, `INTERRUPT` `0x28`, `IRQ_MASK` `0x2C`.
- `byte read(int offset)` / `void write(int offset, byte value)` — implements `jCPU.Peripheral`; multi-byte word accesses are assembled from byte accesses by `VirtualMemorySpace`. `DATA` is the 32-bit **RAM DMA address**, not a data FIFO. `STATUS` reads return the live status word (`BUSY` bit 0, `ERROR` bit 1). Writes to `INTERRUPT` are write-1-to-clear.
- Command execution: the little-endian `CMDCMD` word is decoded when its high byte (`CMDCMD + 3`) is written. Command index = bits [5:0], ACMD flag = bit 6.
- Transfers are performed synchronously inside the `CMDCMD` write: `STATUS_BUSY` is set on entry and cleared before returning, so bare-metal polling always observes completion.
- DMA: CMD17 copies the sector from `SdCard` to RAM at `DATA`; CMD24 copies 512 bytes from RAM at `DATA` into the image via `SdCard.writeSector`.
- Completion: `INTERRUPT.IRQ_COMPLETE` (bit 0) is set on success **and** error; the CPU IRQ line is asserted via `CPU.setIRQ(this)` unless `IRQ_MASK` is non-zero and masks bit 0. Clearing the pending bit calls `CPU.clearIRQ(this)`.
- `EmmcController` implements `Comparable` because `CPU.setIRQ`/`clearIRQ` keep sources in a `TreeSet`.

### `VirtualMemorySpace` (modified)
- New overload `registerPeripheral(int address, int size, Peripheral p)`. Accesses in `[address, address + size)` are routed to `p` with `offset = address - base`; exact-address registrations still take precedence and receive offset `0`.

### `Pi2MemoryMap` (modified)
- Add `public static final int EMMC_BASE = 0x3F200000;`.

### `CPU` (unchanged)
- Existing `setIRQ(Object)`, `clearIRQ(Object)`, `haveIRQ()` and the IRQ exception vector already provide the interrupt line.

### `Pi2Loader` (modified)
- `main(String[])`: `args[0]` = `kernel.img` (required), `args[1]` = optional SD image path; usage `Pi2Loader <kernel.img> [sd.img]`.
- `loadImageWithSd(byte[] image, String sdPath)`: opens `SdCard`, builds `EmmcController(card, cpu)`, registers it at `EMMC_BASE` (size `0x1000`).

### SD command subset
| Command | Name | Behavior |
|---------|------|----------|
| CMD0 | GO_IDLE_STATE | Card idle; `CMDRESP0` byte 0 = `0x01`; no data. |
| ACMD41 | SEND_OP_COND | Card "ready" (always true — no physical card); `CMDRESP0` = `0x00`. Command index 41 without the ACMD bit is treated the same. |
| CMD16 | SET_BLOCKLEN | Block length 512; acknowledged. |
| CMD17 | READ_SINGLE_BLOCK | Read sector from image → DMA to RAM at `DATA` → IRQ. |
| CMD24 | WRITE_SINGLE_BLOCK | DMA from RAM at `DATA` → write sector to image → IRQ. |
| other | — | `STATUS` error bit set; completion IRQ still raised; no state change. |

## Data flow

1. **Boot:** `Pi2Loader` loads `kernel.img` at `0x00000000`; if a second arg is given, opens it as the SD image via `SdCard`, builds `EmmcController`, registers it at `0x3F200000` in `VirtualMemorySpace`.
2. **Card init:** Bare-metal code writes `CMD0` then `ACMD41` → controller returns R1 responses via `CMDRESP0`.
3. **Read sector:** Code writes sector number to `ARG1`, the RAM destination to `DATA`, issues `CMD17` → controller reads the sector from the image, DMA-copies 512 bytes to RAM, clears `STATUS` busy, sets `INTERRUPT`, asserts IRQ.
4. **Write sector:** Code writes sector number to `ARG1`, stages 512 bytes in RAM, writes that RAM address to `DATA`, issues `CMD24` → controller DMA-copies RAM → image, clears busy, sets `INTERRUPT`, asserts IRQ.
5. **Sync:** Code polls `STATUS` (busy/error) and/or waits for IRQ, then write-1-clears `INTERRUPT`.

## Error handling

- **Sector out of range** (sector < 0 or ≥ sectorCount) → `STATUS` error bit set, completion IRQ raised, no data transferred outside image bounds.
- **Image file missing/unreadable** → `SdCard` construction throws `IOException`; `Pi2Loader` reports the failure and exits non-zero.
- **Unsupported SD command** → `STATUS` error bit set; simulation continues (no halt).
- **DMA fault / unaligned transfer** → DMA exception caught, `STATUS` error bit set, transfer aborted.
- **Command issued while busy** → transfers are synchronous, so bare-metal code cannot observe a busy window between commands; `STATUS_BUSY` is only set during the `CMDCMD` write itself.

## Testing Plan

1. **Unit:** `SdCardTest` — sector count, read/write round-trip, out-of-range rejection.
2. **Unit:** `EmmcControllerRegisterTest`, `EmmcControllerCmdTest` — register read/write, command decode, CMD0/41/16 responses, unknown-command error.
3. **Unit:** `EmmcControllerCmdReadTest`, `EmmcControllerCmdWriteTest` — CMD17/CMD24 DMA round-trip and image persistence.
4. **Unit:** `EmmcControllerIrqTest` — completion IRQ, `STATUS` visibility, write-1-to-clear.
5. **Unit:** `VirtualMemorySpaceRangeTest` — range MMIO dispatch and exact-registration precedence.
6. **Loader:** `Pi2LoaderSdTest` — `Pi2Loader` accepts an SD image and the controller is reachable through MMIO.
7. **Integration:** `SdEndToEndTest` — boot via `Pi2Loader.loadImageWithSd`, write sector 3 via CMD24 through MMIO, read it back via CMD17, verify RAM and the host image file, assert/clear IRQ.

## Implementation notes (2026-11-16)

Commits on the detached HEAD of `neon-engine`:

- `0c850d4a` `EMMC_BASE` constant
- `aedf4d21` `SdCard` backing store
- `31f46503` `EmmcController` register map
- `cdf05f7a` SD command decode (CMD0/ACMD41/CMD16)
- `fbcc8ee6` `VirtualMemorySpace` MMIO ranges (added during implementation — exact-address dispatch alone could not reach EMMC registers)
- `d803eadb` CMD17 read + DMA
- `4848be55` CMD24 write + DMA
- `47f576fb` completion IRQ + live `STATUS` reads
- `337065a6` `Pi2Loader` SD arg
- `7b0418ef` end-to-end round-trip test

Verification: `javac` + JUnit 4 (`ant` is broken by a pre-existing `ifOS`/Zero dependency); full `jCPU.arm` suite `OK (54 tests)`.

Known deviations from the original draft, recorded deliberately:
- MMIO required new range registration (`registerPeripheral(base, size, p)`); the pre-existing API was exact-address only.
- `DATA` holds a RAM DMA address rather than sector bytes (no FIFO needed for the single-block subset).
- The completion IRQ reuses `CPU.setIRQ`/`clearIRQ`; no `CPU.java` change was necessary, but the IRQ token must be `Comparable` (`TreeSet`), so `EmmcController` implements `Comparable`.

## Acceptance criteria

- Bare-metal code can initialize the "card" via `CMD0`/`ACMD41` and read `CMDRESP0` without simulation halt.
- A 512-byte sector written via `CMD24` and read back via `CMD17` returns identical bytes; changes persist in the host image file.
- Out-of-range sector access sets `STATUS` error bit and does not corrupt memory.
- Unknown SD commands return error status; simulation continues.
- `Pi2Loader` accepts an optional SD image path and starts the controller without exception.
- No regressions in existing `jCPU.arm` behavior (existing UART, memory, CPU tests unchanged).
