# SD Block Device Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an SD card block device controller at Pi 2 EMMC address `0x3F200000`, backed by a host SD image file, with SD command subset (CMD0, ACMD41, CMD16, CMD17, CMD24), DMA to RAM, and completion IRQ.

**Architecture:** `SdCard` (sector backing store) + `EmmcController` (MMIO device at `0x3F200000` with register set, SD command state machine, DMA buffer) registered in `VirtualMemorySpace`; IRQ asserted to `CPU` on completion; `Pi2Loader` accepts optional SD image path.

**Tech Stack:** Java, JUnit 4, existing `jCPU.arm` classes, javac/java directly (ant broken).

## Global Constraints

- Target: Raspberry Pi 2, ARMv7, A32 instruction set only; single-core.
- EMMC base: `0x3F200000`; RAM base: `0x00000000`.
- SD card image is a flat file of 512-byte sectors.
- SD command subset only: CMD0, ACMD41, CMD16, CMD17, CMD24. Other commands return error status; simulation continues.
- Unimplemented EMMC registers read as `0`, writes ignored, with a warning log.
- No multi-block, CSD/SCR, write-protect, card detection, filesystem, SPI mode.
- Follow existing `jCPU.arm` patterns; tests in `test/jCPU/arm/` mirroring `src/jCPU/arm/`.
- Build/test via javac + JUnit directly (ant is blocked by pre-existing ifOS/Zero dependency).
- `Simulator.jar` (supplies `j51.*`, `nl.lxtreme.arm.*`, `jCPU.GUI`) lives at `/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar`. The older `/Users/xuyi/Source/Java/applications/...` path no longer exists, and without the jar `-sourcepath src` drags in `jCPU.GUI` → `jCPU.MCS51` → the missing `j51.intel.graph` package.
- MMIO dispatch supports exact-address registration (offset 0) and range registration `registerPeripheral(base, size, peripheral)` (offset = `address - base`). The EMMC controller must use the range form.

## File Structure

Create:
- `Simulator/src/jCPU/arm/SdCard.java` — sector backing store over host image file.
- `Simulator/src/jCPU/arm/EmmcController.java` — MMIO device, register set, SD command state machine, DMA, IRQ.
- `Simulator/test/jCPU/arm/SdCardTest.java` — sector read/write + bounds.
- `Simulator/test/jCPU/arm/EmmcControllerRegisterTest.java` — register read/write + command decode.
- `Simulator/test/jCPU/arm/EmmcControllerCmdReadWriteTest.java` — CMD17/CMD24 round-trip through RAM.
- `Simulator/test/jCPU/arm/SdEndToEndTest.java` — Pi2Loader + SD image boot + round-trip.

Modify:
- `Simulator/src/jCPU/arm/Pi2MemoryMap.java` — add `EMMC_BASE = 0x3F200000`.
- `Simulator/src/jCPU/arm/CPU.java` — expose IRQ delivery for controller completion.
- `Simulator/src/jCPU/arm/Pi2Loader.java` — accept optional SD image path arg; wire controller.

---

### Task 1: Pi2MemoryMap EMMC_BASE constant

**Files:**
- Modify: `Simulator/src/jCPU/arm/Pi2MemoryMap.java`
- Test: `Simulator/test/jCPU/arm/Pi2MemoryMapTest.java` (existing; add one test method)

**Interfaces:**
- Consumes: nothing (pure constant).
- Produces: `public static final int EMMC_BASE = 0x3F200000;` on `jCPU.arm.Pi2MemoryMap`.

- [ ] **Step 1: Write the failing test**

Add to `Simulator/test/jCPU/arm/Pi2MemoryMapTest.java`:

```java
@Test
public void testEmmcBase() {
    assertEquals(0x3F200000, Pi2MemoryMap.EMMC_BASE);
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/Pi2MemoryMapTest.java`
Expected: `error: cannot find symbol ... variable EMMC_BASE` (exit 1)

- [ ] **Step 3: Write minimal implementation**

Add to `Simulator/src/jCPU/arm/Pi2MemoryMap.java`:

```java
    public static final int EMMC_BASE = 0x3F200000;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.Pi2MemoryMapTest`
Expected: `OK (4 tests)` (3 existing + 1 new)

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/Pi2MemoryMap.java Simulator/test/jCPU/arm/Pi2MemoryMapTest.java
git commit -m "feat(arm): add EMMC_BASE constant to Pi2MemoryMap"
```

---

### Task 2: SdCard backing store

**Files:**
- Create: `Simulator/src/jCPU/arm/SdCard.java`
- Test: `Simulator/test/jCPU/arm/SdCardTest.java` (new)

**Interfaces:**
- Consumes: host image file path (String).
- Produces: `class jCPU.arm.SdCard` with:
  - `public SdCard(String imagePath) throws IOException`
  - `public int getSectorCount()` — returns `fileSize / 512`
  - `public byte[] readSector(int sector)` — returns 512 bytes; throws `IllegalArgumentException` if out of range
  - `public void writeSector(int sector, byte[] data)` — writes 512 bytes; throws `IllegalArgumentException` if out of range or data length != 512

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/SdCardTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;

public class SdCardTest {
    @Test
    public void testSectorCount() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[1024]); // 2 sectors
        try (SdCard card = new SdCard(tmp.toString())) {
            assertEquals(2, card.getSectorCount());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testReadWriteRoundTrip() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[1024]);
        try (SdCard card = new SdCard(tmp.toString())) {
            byte[] data = new byte[512];
            for (int i = 0; i < 512; i++) data[i] = (byte) (i * 3 + 1);
            card.writeSector(0, data);
            byte[] back = card.readSector(0);
            assertArrayEquals(data, back);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadOutOfRange() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[512]);
        try (SdCard card = new SdCard(tmp.toString())) {
            card.readSector(1);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:junit:hamcrest" Simulator/test/jCPU/arm/SdCardTest.java`
Expected: `error: cannot find symbol: class SdCard` (exit 1)

- [ ] **Step 3: Write minimal implementation**

Create `Simulator/src/jCPU/arm/SdCard.java`:

```java
package jCPU.arm;

import java.io.*;
import java.nio.file.*;

public class SdCard implements AutoCloseable {
    public static final int SECTOR_SIZE = 512;
    private final RandomAccessFile file;
    private final int sectorCount;

    public SdCard(String imagePath) throws IOException {
        file = new RandomAccessFile(imagePath, "rw");
        sectorCount = (int) (file.length() / SECTOR_SIZE);
    }

    public int getSectorCount() {
        return sectorCount;
    }

    public byte[] readSector(int sector) {
        if (sector < 0 || sector >= sectorCount) {
            throw new IllegalArgumentException("sector out of range: " + sector);
        }
        byte[] buf = new byte[SECTOR_SIZE];
        try {
            file.seek((long) sector * SECTOR_SIZE);
            int read = file.read(buf);
            if (read != SECTOR_SIZE) throw new IOException("short read");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buf;
    }

    public void writeSector(int sector, byte[] data) {
        if (sector < 0 || sector >= sectorCount) {
            throw new IllegalArgumentException("sector out of range: " + sector);
        }
        if (data.length != SECTOR_SIZE) {
            throw new IllegalArgumentException("data must be 512 bytes");
        }
        try {
            file.seek((long) sector * SECTOR_SIZE);
            file.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.SdCardTest`
Expected: `OK (3 tests)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/SdCard.java Simulator/test/jCPU/arm/SdCardTest.java
git commit -m "feat(arm): SdCard sector backing store with bounds checking"
```

---

### Task 3: EmmcController skeleton + register map

**Files:**
- Create: `Simulator/src/jCPU/arm/EmmcController.java`
- Test: `Simulator/test/jCPU/arm/EmmcControllerRegisterTest.java` (new)

**Interfaces:**
- Consumes: `SdCard` instance; `Pi2MemoryMap.EMMC_BASE`.
- Produces: `class jCPU.arm.EmmcController implements jCPU.Peripheral` with:
  - `public EmmcController(SdCard card)`
  - `public byte read(int offset)` — returns register value by offset; unimplemented offsets return 0
  - `public void write(int offset, byte value)` — stores register byte; unimplemented offsets log warning
  - `public int getStatus()` — for test inspection
  - Register offsets (from EMMC_BASE): `ARG1=0x00`, `ARG2=0x04`, `CMDCMD=0x08`, `CMDTMO=0x0C`, `CMDRESP0=0x10`, `CMDRESP1=0x14`, `CMDRESP2=0x18`, `CMDRESP3=0x1C`, `DATA=0x20`, `STATUS=0x24`, `INTERRUPT=0x28`, `IRQ_MASK=0x2C` (as `public static final int` constants)

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/EmmcControllerRegisterTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerRegisterTest {
    @Test
    public void testReadUnimplementedReturnsZero() {
        SdCard card = null; // controller constructed without functional card for register test
        EmmcController ctl = new EmmcController(card);
        assertEquals(0, ctl.read(EmmcController.CMDRESP0) & 0xFF);
    }

    @Test
    public void testWriteAndReadArg1() {
        SdCard card = null;
        EmmcController ctl = new EmmcController(card);
        ctl.write(EmmcController.ARG1 + 0, (byte) 0x12);
        ctl.write(EmmcController.ARG1 + 1, (byte) 0x34);
        ctl.write(EmmcController.ARG1 + 2, (byte) 0x56);
        ctl.write(EmmcController.ARG1 + 3, (byte) 0x78);
        int arg = ctl.read(EmmcController.ARG1 + 0) & 0xFF;
        arg |= (ctl.read(EmmcController.ARG1 + 1) & 0xFF) << 8;
        arg |= (ctl.read(EmmcController.ARG1 + 2) & 0xFF) << 16;
        arg |= (ctl.read(EmmcController.ARG1 + 3) & 0xFF) << 24;
        assertEquals(0x78563412, arg);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:junit:hamcrest" Simulator/test/jCPU/arm/EmmcControllerRegisterTest.java`
Expected: `error: cannot find symbol: class EmmcController` (exit 1)

- [ ] **Step 3: Write minimal implementation**

Create `Simulator/src/jCPU/arm/EmmcController.java`:

```java
package jCPU.arm;

import jCPU.Peripheral;

public class EmmcController implements Peripheral {
    public static final int ARG1 = 0x00;
    public static final int ARG2 = 0x04;
    public static final int CMDCMD = 0x08;
    public static final int CMDTMO = 0x0C;
    public static final int CMDRESP0 = 0x10;
    public static final int CMDRESP1 = 0x14;
    public static final int CMDRESP2 = 0x18;
    public static final int CMDRESP3 = 0x1C;
    public static final int DATA = 0x20;
    public static final int STATUS = 0x24;
    public static final int INTERRUPT = 0x28;
    public static final int IRQ_MASK = 0x2C;

    public static final int STATUS_BUSY = 1 << 0;
    public static final int STATUS_ERROR = 1 << 1;

    private final SdCard card;
    private final byte[] regs = new byte[0x30];
    private int status;

    public EmmcController(SdCard card) {
        this.card = card;
    }

    @Override
    public byte read(int offset) {
        if (offset < 0 || offset >= regs.length) return 0;
        return regs[offset];
    }

    @Override
    public void write(int offset, byte value) {
        if (offset < 0 || offset >= regs.length) return;
        regs[offset] = value;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.EmmcControllerRegisterTest`
Expected: `OK (2 tests)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/EmmcController.java Simulator/test/jCPU/arm/EmmcControllerRegisterTest.java
git commit -m "feat(arm): EmmcController skeleton + register map"
```

---

### Task 4: EmmcController SD command decode (CMD0/ACMD41/CMD16)

**Files:**
- Modify: `Simulator/src/jCPU/arm/EmmcController.java`
- Test: `Simulator/test/jCPU/arm/EmmcControllerCmdTest.java` (new)

**Interfaces:**
- Consumes: `SdCard` (may be null for command-decode tests).
- Produces: `EmmcController` decodes `CMDCMD` writes:
  - command index = bits [5:0] of the CMDCMD word
  - ACMD flag = bit 6
  - On CMD0: sets `CMDRESP0` byte 0 = 0x01 (card idle), clears busy
  - On ACMD41: sets `CMDRESP0` byte 0 = 0x00 (ready), clears busy
  - On CMD16: clears busy (block length acknowledged)
  - Other commands: sets `STATUS` error bit
  - Writing `CMDCMD` sets `STATUS` busy bit during processing, clears on completion

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/EmmcControllerCmdTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerCmdTest {
    @Test
    public void testCmd0SetsIdleResponse() {
        EmmcController ctl = new EmmcController(null);
        // write CMDCMD = 0 (CMD0), little-endian 4 bytes
        ctl.write(EmmcController.CMDCMD + 0, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 1, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 2, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 3, (byte) 0x00);
        // response byte 0 of CMDRESP0 should be 0x01 (idle)
        assertEquals(0x01, ctl.read(EmmcController.CMDRESP0) & 0xFF);
        // busy bit cleared
        assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);
    }

    @Test
    public void testAcmd41SetsReadyResponse() {
        EmmcController ctl = new EmmcController(null);
        ctl.write(EmmcController.CMDCMD + 0, (byte) 0x29); // ACMD41 index
        ctl.write(EmmcController.CMDCMD + 1, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 2, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 3, (byte) 0x00);
        assertEquals(0x00, ctl.read(EmmcController.CMDRESP0) & 0xFF);
        assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);
    }

    @Test
    public void testUnknownCommandSetsError() {
        EmmcController ctl = new EmmcController(null);
        ctl.write(EmmcController.CMDCMD + 0, (byte) 0x30); // CMD48 (unsupported)
        ctl.write(EmmcController.CMDCMD + 1, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 2, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 3, (byte) 0x00);
        assertTrue((ctl.getStatus() & EmmcController.STATUS_ERROR) != 0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:junit:hamcrest" Simulator/test/jCPU/arm/EmmcControllerCmdTest.java`
Expected: compiles but `CMDRESP0` read returns 0 instead of 0x01/0x00 → assertion failures (exit 1)

- [ ] **Step 3: Write minimal implementation**

Modify `Simulator/src/jCPU/arm/EmmcController.java`:

1. Add command constants and response/decoding logic:

```java
    public static final int CMD_GO_IDLE_STATE = 0;
    public static final int CMD_SEND_OP_COND = 41; // ACMD41 = index 41 with bit 6 (0x40) set
    public static final int CMD_SET_BLOCKLEN = 16;
    public static final int CMD_READ_SINGLE = 17;
    public static final int CMD_WRITE_SINGLE = 24;
```

2. In `write`, when `offset == CMDCMD` and it's the last byte (offset+3), decode the full 32-bit command word and process:

```java
    @Override
    public void write(int offset, byte value) {
        if (offset < 0 || offset >= regs.length) return;
        regs[offset] = value;
        if (offset >= CMDCMD && offset <= CMDCMD + 3) {
            // only act when the full word has been written (lowest address last is simplest: act on CMDCMD+0)
            if (offset == CMDCMD) {
                // defer: process when high byte written instead
            }
        }
    }
```

Simpler: process the command when `CMDCMD + 3` (highest byte) is written, assembling the little-endian word:

```java
        if (offset == CMDCMD + 3) {
            int cmdWord = (regs[CMDCMD] & 0xFF)
                        | ((regs[CMDCMD + 1] & 0xFF) << 8)
                        | ((regs[CMDCMD + 2] & 0xFF) << 16)
                        | ((regs[CMDCMD + 3] & 0xFF) << 24);
            processCommand(cmdWord);
        }
```

3. Add `processCommand`:

```java
    private void processCommand(int cmdWord) {
        setStatus(STATUS_BUSY);
        int cmdIndex = cmdWord & 0x3F;
        boolean isAcmd = (cmdWord & 0x40) != 0;
        // clear response
        regs[CMDRESP0] = 0;
        regs[CMDRESP1] = 0;
        regs[CMDRESP2] = 0;
        regs[CMDRESP3] = 0;
        if (isAcmd) {
            if (cmdIndex == CMD_SEND_OP_COND) {
                regs[CMDRESP0] = 0x00; // ready
            } else {
                setStatus(STATUS_ERROR);
            }
        } else {
            switch (cmdIndex) {
                case CMD_GO_IDLE_STATE:
                    regs[CMDRESP0] = 0x01; // idle
                    break;
                case CMD_SEND_OP_COND:
                    // CMD41 sent without the APP_CMD bit also means SEND_OP_COND
                    regs[CMDRESP0] = 0x00; // ready
                    break;
                case CMD_SET_BLOCKLEN:
                    break; // acknowledged
                case CMD_READ_SINGLE:
                case CMD_WRITE_SINGLE:
                    // handled in Tasks 5/6; for now mark busy then clear
                    break;
                default:
                    setStatus(STATUS_ERROR);
                    break;
            }
        }
        setStatus(getStatus() & ~STATUS_BUSY);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.EmmcControllerCmdTest`
Expected: `OK (3 tests)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/EmmcController.java Simulator/test/jCPU/arm/EmmcControllerCmdTest.java
git commit -m "feat(arm): EmmcController SD command decode (CMD0/ACMD41/CMD16)"
```

---

### Task 4.5: VirtualMemorySpace MMIO range registration

**Why:** the existing MMIO dispatch is exact-address only (`mmioMap.get(address)`), so a device registered at `0x3F200000` never sees accesses to `+0x08`, `+0x24`, etc. Without this, bare-metal code cannot reach any EMMC register other than the first.

**Files:**
- Modify: `Simulator/src/jCPU/arm/VirtualMemorySpace.java`
- Test: `Simulator/test/jCPU/arm/VirtualMemorySpaceRangeTest.java` (new)

**Interfaces:**
- Consumes: nothing new.
- Produces: `public void registerPeripheral(int address, int size, Peripheral p)` on `jCPU.arm.VirtualMemorySpace`. Accesses in `[address, address + size)` are routed to `p` with `offset = address - address`. Exact-address registrations keep precedence and receive offset `0`.

**Implementation shape:**
- Add a private `MmioRange { base, end, Peripheral p }` record class.
- Keep `mmioMap` (exact) untouched; add `mmioRanges` (`CopyOnWriteArrayList`).
- Add `lookupPeripheral(int address)` and `peripheralOffset(int address)` helpers.
- In `readByte`/`writeByte`/`readShort`/`writeShort`/`readInt`/`writeInt`/`readLong`/`writeLong`, replace `Peripheral p = mmioMap.get(address);` with the helpers and pass `po + i` instead of `i` to the peripheral.

**Status:** DONE — commit `fbcc8ee6`, test `OK (3 tests)`.

---

### Task 5: EmmcController CMD17 read sector + DMA to RAM

**Files:**
- Modify: `Simulator/src/jCPU/arm/EmmcController.java`
- Modify: `Simulator/src/jCPU/arm/CPU.java` — expose `instructionWriteWord` access (already public) and IRQ hook (deferred to Task 7; Task 5 just needs DMA to CPU memory space).
- Test: `Simulator/test/jCPU/arm/EmmcControllerCmdReadTest.java` (new)

**Interfaces:**
- Consumes: `CPU` (for `instructionWriteWord(int, int)` to DMA into RAM), `SdCard`.
- Produces: `EmmcController` handles `CMD_READ_SINGLE`:
  - Reads `ARG1` as sector number
  - Reads 512 bytes from `SdCard.readSector(sector)` into internal buffer
  - DMA-copies 512 bytes to system RAM starting at `DATA` register value (interpreted as RAM address) via `cpu.instructionWriteWord(addr, word)` little-endian
  - Sets `STATUS` complete, clears busy, asserts IRQ pending (IRQ delivery wired in Task 7)

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/EmmcControllerCmdReadTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerCmdReadTest {
    @Test
    public void testReadSectorDmaToRam() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        // NOTE: CPU() already maps 1 MB of RAM at 0x0 — do not map it again.

        SdCard card = new SdCard(tmpImageWithSector(7, 0xDEADBEEF));
        EmmcController ctl = new EmmcController(card);
        cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);

        // program sector 7 in ARG1
        writeWord(ctl, EmmcController.ARG1, 7);
        // program DATA = RAM destination address
        writeWord(ctl, EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x1000);
        // issue CMD17
        writeWord(ctl, EmmcController.CMDCMD, 17);

        // wait until busy clears
        for (int i = 0; i < 10000 && (ctl.getStatus() & EmmcController.STATUS_BUSY) != 0; i++) {}

        assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);
        // verify first word of sector in RAM
        int val = cpu.instructionReadWord(Pi2MemoryMap.RAM_BASE + 0x1000, false);
        assertEquals(0xDEADBEEF, val);
    }

    private void writeWord(EmmcController ctl, int offset, int value) {
        ctl.write(offset + 0, (byte) (value & 0xFF));
        ctl.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        ctl.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        ctl.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }

    private SdCard tmpImageWithSector(int sector, int pattern) throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("sd", ".img");
        byte[] img = new byte[(sector + 1) * 512];
        // write the pattern little-endian so the first RAM word reads back as `pattern`
        img[sector * 512 + 0] = (byte) (pattern & 0xFF);
        img[sector * 512 + 1] = (byte) ((pattern >> 8) & 0xFF);
        img[sector * 512 + 2] = (byte) ((pattern >> 16) & 0xFF);
        img[sector * 512 + 3] = (byte) ((pattern >> 24) & 0xFF);
        java.nio.file.Files.write(tmp, img);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }));
        return new SdCard(tmp.toString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:junit:hamcrest" Simulator/test/jCPU/arm/EmmcControllerCmdReadTest.java`
Expected: CMD17 doesn't DMA → RAM word remains 0 → assertion failure (exit 1)

- [ ] **Step 3: Write minimal implementation**

Modify `Simulator/src/jCPU/arm/EmmcController.java`:
1. Add field `private final CPU cpu;` and constructor `public EmmcController(SdCard card, CPU cpu)`. Keep the existing 1-arg constructor as `public EmmcController(SdCard card) { this(card, null); }` so Task 3/4 tests still compile.
2. In `processCommand`, case `CMD_READ_SINGLE`:
   - read `arg1 = readWord(ARG1)` (sector number) and `addr = readWord(DATA)` (RAM DMA destination)
   - if `card == null || cpu == null` → set `STATUS_ERROR` and stop
   - `byte[] buffer = card.readSector(arg1);` (catch `IllegalArgumentException` → `STATUS_ERROR`)
   - for `i` in 0..127: build `word = (buffer[i*4] & 0xFF) | ((buffer[i*4+1] & 0xFF) << 8) | ((buffer[i*4+2] & 0xFF) << 16) | ((buffer[i*4+3] & 0xFF) << 24)` and `cpu.instructionWriteWord(addr + i*4, word, false);`
   - `instructionWriteWord` throws checked exceptions (`BusErrorException`, `AlignmentException`, `EscapeRetryException`, `EscapeCompleteException`) — wrap the DMA loop in `try { ... } catch (Exception e) { setStatus(STATUS_ERROR); return; }`
   - set interrupt pending bit (Task 7 wires delivery)
3. Add `private int readWord(int offset)` assembling 4 bytes little-endian from `regs` (reuse in Tasks 5/6).

**Important:** `CPU()` already maps 1 MB of RAM at `0x0`; never call `mapRegion` for RAM in tests.

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.EmmcControllerCmdReadTest`
Expected: `OK (1 test)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/EmmcController.java Simulator/test/jCPU/arm/EmmcControllerCmdReadTest.java
git commit -m "feat(arm): EmmcController CMD17 read sector + DMA to RAM"
```

---

### Task 6: EmmcController CMD24 write sector + DMA from RAM

**Files:**
- Modify: `Simulator/src/jCPU/arm/EmmcController.java`
- Test: `Simulator/test/jCPU/arm/EmmcControllerCmdWriteTest.java` (new)

**Interfaces:**
- Consumes: `CPU` (for `instructionReadWord` to DMA from RAM), `SdCard`.
- Produces: `EmmcController` handles `CMD_WRITE_SINGLE`:
  - Reads `ARG1` as sector number
  - DMA-copies 512 bytes from RAM (address in `DATA` register) via `cpu.instructionReadWord(addr + i*4, false)`
  - Writes sector to `SdCard.writeSector(sector, buffer)`
  - Sets `STATUS` complete, clears busy, asserts IRQ pending

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/EmmcControllerCmdWriteTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;
import java.nio.file.*;

public class EmmcControllerCmdWriteTest {
    @Test
    public void testWriteSectorPersistsToImage() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        // NOTE: CPU() already maps 1 MB of RAM at 0x0 — do not map it again.

        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[1024]);
        try (SdCard card = new SdCard(tmp.toString())) {
            EmmcController ctl = new EmmcController(card, cpu);
            cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);

            // fill RAM at 0x1000: little-endian bytes A5 A5 00 00
            for (int i = 0; i < 128; i++) {
                cpu.instructionWriteWord(Pi2MemoryMap.RAM_BASE + 0x1000 + i * 4, 0x0000A5A5, false);
            }
            writeWord(ctl, EmmcController.ARG1, 0);
            writeWord(ctl, EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x1000);
            writeWord(ctl, EmmcController.CMDCMD, 24); // CMD24

            for (int i = 0; i < 10000 && (ctl.getStatus() & EmmcController.STATUS_BUSY) != 0; i++) {}
            assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);

            // verify persisted in image file
            byte[] img = Files.readAllBytes(tmp);
            assertEquals(0xA5, img[0] & 0xFF);
            assertEquals(0xA5, img[1] & 0xFF);
            assertEquals(0x00, img[2] & 0xFF);
            assertEquals(0x00, img[3] & 0xFF);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private void writeWord(EmmcController ctl, int offset, int value) {
        ctl.write(offset + 0, (byte) (value & 0xFF));
        ctl.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        ctl.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        ctl.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:junit:hamcrest" Simulator/test/jCPU/arm/EmmcControllerCmdWriteTest.java`
Expected: CMD24 doesn't DMA/write → image file bytes stay 0 → assertion failure (exit 1)

- [ ] **Step 3: Write minimal implementation**

Modify `Simulator/src/jCPU/arm/EmmcController.java`:
1. Add case `CMD_WRITE_SINGLE` in `processCommand`:
   - get sector from `readWord(ARG1)`
   - get source address from `readWord(DATA)`
   - `byte[] buffer = new byte[512];` copy from RAM little-endian via `cpu.instructionReadWord(addr + i*4, false)`
   - `card.writeSector(sector, buffer)`
   - set status complete (clear busy), set interrupt pending

2. Add `readWord(int offset)` helper assembling 4 bytes little-endian from `regs`.

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.EmmcControllerCmdWriteTest`
Expected: `OK (1 test)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/EmmcController.java Simulator/test/jCPU/arm/EmmcControllerCmdWriteTest.java
git commit -m "feat(arm): EmmcController CMD24 write sector + DMA from RAM"
```

---

### Task 7: EmmcController completion IRQ + CPU IRQ wiring

**Files:**
- Modify: `Simulator/src/jCPU/arm/EmmcController.java`
- Test: `Simulator/test/jCPU/arm/EmmcControllerIrqTest.java` (new)

**Interfaces:**
- Consumes: existing `CPU.setIRQ(Object who)`, `CPU.clearIRQ(Object who)`, `CPU.haveIRQ()` — already implemented, no `CPU.java` change needed.
- Produces: `EmmcController` sets `INTERRUPT` bit 0 (`IRQ_COMPLETE`) on command completion and calls `cpu.setIRQ(this)`. Writing `1` to the `INTERRUPT` register clears the pending bit and calls `cpu.clearIRQ(this)`.

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/EmmcControllerIrqTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerIrqTest {
    @Test
    public void testCommandCompletionSetsInterrupt() {
        CPU cpu = new CPU();
        cpu.reset();
        EmmcController ctl = new EmmcController(null, cpu);
        writeWord(ctl, EmmcController.CMDCMD, 0); // CMD0 completes
        assertTrue((ctl.read(EmmcController.INTERRUPT) & 0x01) != 0);
        assertTrue(cpu.haveIRQ());
        // STATUS is readable through MMIO and reports no busy/error after completion
        assertEquals(0, ctl.read(EmmcController.STATUS) & (EmmcController.STATUS_BUSY | EmmcController.STATUS_ERROR));
    }

    @Test
    public void testUnsupportedCommandExposesErrorInStatus() {
        CPU cpu = new CPU();
        cpu.reset();
        EmmcController ctl = new EmmcController(null, cpu);
        writeWord(ctl, EmmcController.CMDCMD, 48); // CMD48 unsupported
        assertTrue((ctl.read(EmmcController.STATUS) & EmmcController.STATUS_ERROR) != 0);
        assertTrue(cpu.haveIRQ());
    }

    @Test
    public void testWritingInterruptClearsIrq() {
        CPU cpu = new CPU();
        cpu.reset();
        EmmcController ctl = new EmmcController(null, cpu);
        writeWord(ctl, EmmcController.CMDCMD, 0);
        ctl.write(EmmcController.INTERRUPT, (byte) 0x01); // write-1-to-clear
        assertEquals(0, ctl.read(EmmcController.INTERRUPT) & 0x01);
        assertFalse(cpu.haveIRQ());
    }

    private void writeWord(EmmcController ctl, int offset, int value) {
        ctl.write(offset + 0, (byte) (value & 0xFF));
        ctl.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        ctl.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        ctl.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:$SIMPATH:$JUNIT:$HAMCREST" test/jCPU/arm/EmmcControllerIrqTest.java`
Expected: `INTERRUPT` bit not set after command → assertion failure (exit 1)

- [ ] **Step 3: Write minimal implementation**

Modify `Simulator/src/jCPU/arm/EmmcController.java` only:
1. `public static final int IRQ_COMPLETE = 1;`
2. `read(...)`: for offsets in `[STATUS, STATUS + 3]` return `(status >>> (8 * (offset - STATUS))) & 0xFF` instead of the raw `regs` byte; all other offsets keep reading `regs`. (Busy/error must be visible through MMIO.)
3. In `write(...)`, special-case `offset == INTERRUPT`: `if ((value & IRQ_COMPLETE) != 0) { regs[INTERRUPT] &= ~IRQ_COMPLETE; if (cpu != null) cpu.clearIRQ(this); }` and return (do not store the written value).
4. At the end of `processCommand` (after clearing busy): `regs[INTERRUPT] |= IRQ_COMPLETE; if (cpu != null) cpu.setIRQ(this);`
5. Signal completion on both success and error paths (so bare-metal code is never stuck waiting).

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:$SIMPATH:$JUNIT:$HAMCREST" org.junit.runner.JUnitCore jCPU.arm.EmmcControllerIrqTest`
Expected: `OK (3 tests)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/EmmcController.java Simulator/test/jCPU/arm/EmmcControllerIrqTest.java
git commit -m "feat(arm): EmmcController completion IRQ wired to CPU"
```

**Note (discovered during implementation):** `CPU.setIRQ`/`clearIRQ` store sources in a `TreeSet<Object>`, so the token must be `Comparable`. `EmmcController` implements `Comparable<EmmcController>` with a unique per-instance id, and always passes `this` to both calls.

---

### Task 8: Pi2Loader SD image arg + wire controller into VirtualMemorySpace

**Files:**
- Modify: `Simulator/src/jCPU/arm/Pi2Loader.java`
- Modify: `Simulator/src/jCPU/arm/CPU.java` (register controller in constructor's VirtualMemorySpace, or via loader)
- Test: `Simulator/test/jCPU/arm/Pi2LoaderSdTest.java` (new)

**Interfaces:**
- Consumes: `args[1]` optional SD image path.
- Produces: `Pi2Loader.main` opens SD image via `SdCard`, builds `EmmcController(card, cpu)`, registers it at `Pi2MemoryMap.EMMC_BASE` in `cpu.getVirtualMemorySpace()` before `cpu.reset()`.

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/Pi2LoaderSdTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.file.*;

public class Pi2LoaderSdTest {
    @Test
    public void testLoaderAcceptsSdImage() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[512]);
        try {
            CPU cpu = Pi2Loader.loadImageWithSd(new byte[] {(byte)0xE3,(byte)0xA0,(byte)0x00,(byte)0x42}, tmp.toString());
            // CMD0 through MMIO must reach the controller registered over the EMMC range
            cpu.getVirtualMemorySpace().writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, 0, false, false);
            int resp = cpu.getVirtualMemorySpace().readInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDRESP0, false, false);
            assertEquals(0x01, resp & 0xFF);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:junit:hamcrest" Simulator/test/jCPU/arm/Pi2LoaderSdTest.java`
Expected: `error: cannot find symbol: method loadImageWithSd` (exit 1)

- [ ] **Step 3: Write minimal implementation**

Modify `Simulator/src/jCPU/arm/Pi2Loader.java`:
1. Add `public static CPU loadImageWithSd(byte[] image, String sdPath) throws IOException`:
   - `CPU cpu = loadImage(image);`
   - `SdCard card = new SdCard(sdPath);`
   - `EmmcController ctl = new EmmcController(card, cpu);`
   - `cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);`
   - return `cpu`
2. Update `main(String[])`: if `args.length >= 2`, call `loadImageWithSd(image, args[1])`; else `loadImage(image)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.Pi2LoaderSdTest`
Expected: `OK (1 test)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/Pi2Loader.java Simulator/test/jCPU/arm/Pi2LoaderSdTest.java
git commit -m "feat(arm): Pi2Loader SD image arg + wire EMMC controller"
```

---

### Task 9: End-to-end integration test (CMD24 write + CMD17 read round-trip)

**Files:**
- Create: `Simulator/test/jCPU/arm/SdEndToEndTest.java`

**Interfaces:**
- Consumes: `Pi2Loader`, `CPU`, `SdCard`, `EmmcController`.
- Produces: test that boots `Pi2Loader` with a small kernel + SD image, issues CMD24 to write a sector, then CMD17 to read it back, verifies bytes round-trip through RAM and persist to the image file.

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/SdEndToEndTest.java`:

```java
package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.file.*;

public class SdEndToEndTest {
    @Test
    public void testWriteThenReadRoundTrip() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[1024]);
        try {
            // Controller wired directly at EMMC_BASE (loader-level wiring covered by Task 8)
            CPU cpu = new CPU();
            cpu.reset();
            SdCard card = new SdCard(tmp.toString());
            EmmcController ctl = new EmmcController(card, cpu);
            cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);

            // write sector 3 via CMD24 from RAM at 0x2000
            byte[] sector = new byte[512];
            for (int i = 0; i < 512; i++) sector[i] = (byte) (i ^ 0x5A);
            for (int i = 0; i < 128; i++) {
                int w = sector[i*4] & 0xFF | (sector[i*4+1] & 0xFF) << 8
                    | (sector[i*4+2] & 0xFF) << 16 | (sector[i*4+3] & 0xFF) << 24;
                cpu.instructionWriteWord(Pi2MemoryMap.RAM_BASE + 0x2000 + i*4, w, false);
            }
            writeWord(ctl, EmmcController.ARG1, 3);
            writeWord(ctl, EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x2000);
            writeWord(ctl, EmmcController.CMDCMD, 24);
            waitNotBusy(ctl);

            // read sector 3 via CMD17 to RAM at 0x3000
            writeWord(ctl, EmmcController.ARG1, 3);
            writeWord(ctl, EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x3000);
            writeWord(ctl, EmmcController.CMDCMD, 17);
            waitNotBusy(ctl);

            // verify RAM contents match original sector
            for (int i = 0; i < 128; i++) {
                int w = cpu.instructionReadWord(Pi2MemoryMap.RAM_BASE + 0x3000 + i*4, false);
                byte[] exp = new byte[4];
                exp[0] = (byte) (w & 0xFF); exp[1] = (byte) ((w>>8) & 0xFF);
                exp[2] = (byte) ((w>>16) & 0xFF); exp[3] = (byte) ((w>>24) & 0xFF);
                assertArrayEquals(new byte[]{sector[i*4], sector[i*4+1], sector[i*4+2], sector[i*4+3]}, exp);
            }

            // verify persisted in image file
            byte[] img = Files.readAllBytes(tmp);
            assertArrayEquals(sector, java.util.Arrays.copyOfRange(img, 3*512, 3*512+512));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private void writeWord(EmmcController ctl, int offset, int value) {
        ctl.write(offset + 0, (byte) (value & 0xFF));
        ctl.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        ctl.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        ctl.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }

    private void waitNotBusy(EmmcController ctl) {
        for (int i = 0; i < 100000 && (ctl.getStatus() & EmmcController.STATUS_BUSY) != 0; i++) {}
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile: `javac -d out/test -cp "out/main:junit:hamcrest" Simulator/test/jCPU/arm/SdEndToEndTest.java`
Expected: compiles but round-trip fails if CMD24/CMD17 DMA broken → assertion failure (exit 1)

- [ ] **Step 3: Write minimal implementation**

No new production code (Tasks 5–7 supply CMD17/CMD24/DMA/IRQ). If assertions fail, fix `EmmcController` DMA copy loops (little-endian word boundaries) until green.

- [ ] **Step 4: Run test to verify it passes**

Run: `java -cp "out/main:out/test:junit:hamcrest" org.junit.runner.JUnitCore jCPU.arm.SdEndToEndTest`
Expected: `OK (1 test)`

- [ ] **Step 5: Commit**

```bash
git add Simulator/test/jCPU/arm/SdEndToEndTest.java
git commit -m "test(arm): end-to-end SD CMD24 write + CMD17 read round-trip"
```

---

## Definition of Done

- All 9 tasks committed independently with passing test cycles.
- `javac` compiles all `src/jCPU/arm/*` sources used by the plan cleanly.
- All `test/jCPU/arm/*` tests (new + existing) pass when run via JUnitCore.
- Bare-metal code can initialize the SD "card" via CMD0/ACMD41, read/write 512-byte sectors via CMD17/CMD24 with DMA to RAM, and observe completion via IRQ.
- Sector writes persist to the host image file; out-of-range sectors set STATUS error without corrupting memory.
- Unknown SD commands return error status; simulation continues.
- `Pi2Loader` accepts an optional SD image path and starts the controller without exception.
- No regressions in existing `jCPU.arm` tests (UART, memory map, CPU data-processing, branch, load/store, multiply).

## Acceptance criteria (from spec)

- CMD0/ACMD41 init sequence returns responses via CMDRESP0 without simulation halt.
- A 512-byte sector written via CMD24 and read back via CMD17 returns identical bytes; changes persist in the host image file.
- Out-of-range sector access sets STATUS error bit and does not corrupt memory.
- Unknown SD commands return error status; simulation continues.
- Pi2Loader accepts an optional SD image path and starts the controller without exception.

Plan complete and saved to `docs/superpowers/plans/2026-11-16-sd-block-device-implementation-plan.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints for review

**Which approach?**
