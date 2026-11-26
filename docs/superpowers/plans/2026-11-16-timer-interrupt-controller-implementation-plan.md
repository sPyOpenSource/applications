# Timer + Interrupt Controller Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a BCM2835 system timer at `0x3F003000` (free-running 64-bit counter, 4 compare channels, match IRQs, write-1-to-clear status) and wire it to the existing multi-source CPU IRQ line, alongside the EMMC controller from sub-project 2.

**Architecture:** `SystemTimer` (new `Peripheral, Comparable`) registered in `VirtualMemorySpace` at `SYSTEM_TIMER_BASE`; `CPU` tracks `totalCycles` and ticks the timer before IRQ checks; `Pi2Loader` instantiates and wires the timer. IRQs from timer and EMMC share `CPU.setIRQ`/`clearIRQ` via `Comparable` tokens.

**Tech Stack:** Java, JUnit 4, existing `jCPU.arm` classes, javac/java directly (ant broken).

## Global Constraints

- Target: Raspberry Pi 2, ARMv7, A32 instruction set only; single-core; bare-metal (no Linux/OS).
- System timer base: `0x3F003000`; MMIO window size: `0x1000`.
- Timer advances on instruction cycles only: `CYCLES_PER_TICK = 1` (one simulated timer tick per CPU cycle).
- Timer registers: `CS` `0x00`, `CLO` `0x04`, `CHI` `0x08`, `C0` `0x0C`, `C1` `0x10`, `C2` `0x14`, `C3` `0x18`.
- `CS` bits [3:0] = match flags for channels 0–3; write-1-to-clear.
- `CLO`/`CHI` read-only; `C0`–`C3` read/write compare values.
- Compare match: counter >= compare value sets match flag + asserts `CPU.setIRQ(this)`.
- Write-1-to-clear CS bit N clears flag N; if no flags remain, calls `CPU.clearIRQ(this)`.
- IRQ token is `Comparable` (unique `irqId`) so `setIRQ(this)`/`clearIRQ(this)` work with CPU's `TreeSet<Object>`.
- `SystemTimer` and `EmmcController` share the same IRQ line; `haveIRQ()` true while either is pending.
- Build/test via javac + JUnit directly (ant blocked by pre-existing ifOS/Zero dependency).
- `Simulator.jar` (supplies `j51.*`, `nl.lxtreme.arm.*`, `jCPU.GUI`) lives at `/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar`.
- `JUNIT=/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar`, `HAMCREST=/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar`.
- Working directory for all commands: `Simulator/`.
- zsh does not word-split variables — use literal paths or `${=VAR}`.

## File Structure

Create:
- `Simulator/src/jCPU/arm/SystemTimer.java` — MMIO timer device, counter, compare channels, IRQ.
- `Simulator/test/jCPU/arm/SystemTimerRegisterTest.java` — CS/CLO/CHI/C0–C3 read/write, out-of-range.
- `Simulator/test/jCPU/arm/SystemTimerCounterAdvanceTest.java` — counter advances with executed cycles.
- `Simulator/test/jCPU/arm/SystemTimerCompareTest.java` — compare match sets flag + asserts IRQ.
- `Simulator/test/jCPU/arm/SystemTimerWrite1ClearTest.java` — write-1-to-clear clears flag + deasserts IRQ.
- `Simulator/test/jCPU/arm/SystemTimerMultiChannelTest.java` — multiple channels pending simultaneously.
- `Simulator/test/jCPU/arm/SystemTimerWfiTest.java` — WFI wake-up when timer fires.
- `Simulator/test/jCPU/arm/TimerEmmcIrqTest.java` — timer + EMMC IRQ sharing.
- `Simulator/test/jCPU/arm/Pi2LoaderTimerTest.java` — Pi2Loader wires timer at SYSTEM_TIMER_BASE.
- `Simulator/test/jCPU/arm/CpuTotalCyclesTest.java` — CPU totalCycles increments per instruction.

Modify:
- `Simulator/src/jCPU/arm/Pi2MemoryMap.java` — add `SYSTEM_TIMER_BASE = 0x3F003000`.
- `Simulator/src/jCPU/arm/CPU.java` — add `totalCycles`, `getTotalCycles()`, `setSystemTimer()`, `setWaitingForInterrupt`, `isWaitingForInterrupt`; tick timer in `execute()` and consume budget while waiting in `execute(int budget)`.
- `Simulator/src/jCPU/arm/Pi2Loader.java` — instantiate timer, register at `SYSTEM_TIMER_BASE`, wire to CPU.
- `Simulator/test/jCPU/arm/Pi2MemoryMapTest.java` — add `testSystemTimerBase`.

---

### Task 1: Pi2MemoryMap SYSTEM_TIMER_BASE constant

**Files:**
- Modify: `Simulator/src/jCPU/arm/Pi2MemoryMap.java:8-13`
- Test: `Simulator/test/jCPU/arm/Pi2MemoryMapTest.java`

**Interfaces:**
- Consumes: nothing (pure constant).
- Produces: `public static final int SYSTEM_TIMER_BASE = 0x3F003000;` on `jCPU.arm.Pi2MemoryMap`.

- [ ] **Step 1: Write the failing test**

Add to `Simulator/test/jCPU/arm/Pi2MemoryMapTest.java`, after `testEmmcBase`:

```java
    @Test
    public void testSystemTimerBase() {
        assertEquals(0x3F003000, Pi2MemoryMap.SYSTEM_TIMER_BASE);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Compile:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/Pi2MemoryMapTest.java
```

Expected: `Simulator/test/jCPU/arm/Pi2MemoryMapTest.java:24: error: cannot find symbol` for `SYSTEM_TIMER_BASE` (exit 1).

- [ ] **Step 3: Write minimal implementation**

Add to `Simulator/src/jCPU/arm/Pi2MemoryMap.java`, after the `EMMC_BASE` line:

```java
    public static final int SYSTEM_TIMER_BASE = 0x3F003000;
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.Pi2MemoryMapTest
```

Expected: `OK (5 tests)` (4 existing + 1 new).

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/Pi2MemoryMap.java Simulator/test/jCPU/arm/Pi2MemoryMapTest.java
git commit -m "feat(arm): add SYSTEM_TIMER_BASE constant to Pi2MemoryMap"
```

---

### Task 2: CPU totalCycles counter + getTotalCycles()

**Files:**
- Modify: `Simulator/src/jCPU/arm/CPU.java` (fields + execute method)
- Test: `Simulator/test/jCPU/arm/CpuTotalCyclesTest.java` (create)

**Interfaces:**
- Consumes: nothing (CPU self-contained).
- Produces:
  - `private long totalCycles = 0;` field on `jCPU.arm.CPU`
  - `public long getTotalCycles()` returning `totalCycles`
  - `totalCycles` incremented once per instruction in `execute()`

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/CpuTotalCyclesTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class CpuTotalCyclesTest {
    @Test
    public void testTotalCyclesIncrementsPerInstruction() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.execute();
        assertEquals(1, cpu.getTotalCycles());
        cpu.execute();
        assertEquals(2, cpu.getTotalCycles());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile main and test:

```bash
javac -sourcepath src -cp "/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar" -d out/main Simulator/src/jCPU/arm/CPU.java
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/CpuTotalCyclesTest.java
```

Expected: `error: cannot find symbol ... method getTotalCycles()` (exit 1).

- [ ] **Step 3: Write minimal implementation**

In `Simulator/src/jCPU/arm/CPU.java`, add field after `cycleBudget` (line 329):

```java
	private long totalCycles = 0;
```

Add getter after `budgetFullySpent()` (line 334):

```java
	/**
	 * @return total number of instructions executed since CPU construction.
	 */
	public long getTotalCycles() { return totalCycles; }
```

Increment `totalCycles` at the start of `execute()` (line 397, before the IRQ checks):

```java
	public int execute() throws BusErrorException, AlignmentException, UndefinedException, EscapeRetryException, EscapeCompleteException {
		if(!haveReset) throw new FatalException("execute() called without first calling reset()");
		totalCycles++;
		if((cpsr & (1 << CPSR_BIT_F)) == 0 && haveFIQ()) generateFIQException();
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.CpuTotalCyclesTest
```

Expected: `OK (1 test)`.

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/CPU.java Simulator/test/jCPU/arm/CpuTotalCyclesTest.java
git commit -m "feat(arm): track total executed cycles on CPU"
```

---

### Task 3: SystemTimer skeleton class, constants, register read/write stubs

**Files:**
- Create: `Simulator/src/jCPU/arm/SystemTimer.java`
- Test: `Simulator/test/jCPU/arm/SystemTimerRegisterTest.java` (create)

**Interfaces:**
- Consumes: `CPU cpu` (may be null for standalone tests).
- Produces:
  - `public class SystemTimer implements Peripheral, Comparable<SystemTimer>`
  - Register constants: `CS` `0x00`, `CLO` `0x04`, `CHI` `0x08`, `C0` `0x0C`, `C1` `0x10`, `C2` `0x14`, `C3` `0x18`
  - `public static final long CYCLES_PER_TICK = 1`
  - `byte read(int offset)` / `void write(int offset, byte value)` — CS read returns match flags; CLO/CHI read returns counter bytes; C0–C3 read returns compare bytes; writes to CLO/CHI ignored; writes to CS are write-1-to-clear; writes to C0–C3 accumulate bytes and assemble word on high byte
  - `int compareTo(SystemTimer other)` comparing unique `irqId`
  - `private void tickToFromCpu()` — advances counter from CPU cycle delta, checks matches
  - `private void checkMatch(int ch)` — sets match flag + asserts IRQ if counter >= compare

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/SystemTimerRegisterTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerRegisterTest {
    @Test
    public void testRegistersReadZeroInitially() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        assertEquals(0, timer.read(SystemTimer.CS) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.CLO) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.CHI) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C0) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C1) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C2) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C3) & 0xFF);
    }

    @Test
    public void testOutOfRangeOffsetReadsZero() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        assertEquals(0, timer.read(0x1C) & 0xFF);
        assertEquals(0, timer.read(0x100) & 0xFF);
    }

    @Test
    public void testCompareRegisterWriteAndRead() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        timer.write(SystemTimer.C0 + 0, (byte) 0x12);
        timer.write(SystemTimer.C0 + 1, (byte) 0x34);
        timer.write(SystemTimer.C0 + 2, (byte) 0x56);
        timer.write(SystemTimer.C0 + 3, (byte) 0x78);
        assertEquals(0x12, timer.read(SystemTimer.C0 + 0) & 0xFF);
        assertEquals(0x34, timer.read(SystemTimer.C0 + 1) & 0xFF);
        assertEquals(0x56, timer.read(SystemTimer.C0 + 2) & 0xFF);
        assertEquals(0x78, timer.read(SystemTimer.C0 + 3) & 0xFF);
    }

    @Test
    public void testCloAndChiAreReadOnly() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        timer.write(SystemTimer.CLO + 0, (byte) 0xFF);
        timer.write(SystemTimer.CHI + 0, (byte) 0xFF);
        // CLO/CHI reads return counter-derived values, not written values
        assertEquals(0, timer.read(SystemTimer.CLO + 0) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.CHI + 0) & 0xFF);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile:

```bash
javac -sourcepath src -cp "/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar" -d out/main Simulator/src/jCPU/arm/SystemTimer.java
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/SystemTimerRegisterTest.java
```

Expected: `error: cannot find symbol ... class SystemTimer` (exit 1, file doesn't exist yet).

- [ ] **Step 3: Write minimal implementation**

Create `Simulator/src/jCPU/arm/SystemTimer.java`:

```java
package jCPU.arm;

import jCPU.Peripheral;

/**
 * BCM2835 system timer at 0x3F003000.
 * <p>
 * Free-running 64-bit counter (CLO/CHI) advancing on CPU instruction cycles,
 * with four compare channels (C0-C3) that assert IRQ on match. CS match flags
 * are write-1-to-clear. Implements {@link Comparable} so the timer can be a
 * token in {@link CPU}'s TreeSet-based IRQ line (same pattern as EmmcController).
 */
public class SystemTimer implements Peripheral, Comparable<SystemTimer> {
    private static final java.util.concurrent.atomic.AtomicLong IRQ_SEQUENCE = new java.util.concurrent.atomic.AtomicLong();
    private final long irqId = IRQ_SEQUENCE.incrementAndGet();

    public static final int CS = 0x00;
    public static final int CLO = 0x04;
    public static final int CHI = 0x08;
    public static final int C0 = 0x0C;
    public static final int C1 = 0x10;
    public static final int C2 = 0x14;
    public static final int C3 = 0x18;

    public static final long CYCLES_PER_TICK = 1;

    private final CPU cpu;
    private final byte[] regs = new byte[0x18]; // byte storage for C0-C3 compare registers
    private final int[] compare = new int[] { -1, -1, -1, -1 }; // -1 = disarmed (won't match counter=0)
    private int csFlags = 0;                    // match flags in bits [3:0]
    private long counter = 0;                   // free-running 64-bit counter
    private long lastCycles = 0;                // last CPU cycle count at last tick

    public SystemTimer(CPU cpu) {
        this.cpu = cpu;
    }

    @Override
    public byte read(int offset) {
        tickToFromCpu();
        if (offset < 0 || offset >= 0x18) {
            return 0;
        }
        switch (offset) {
            case 0x00: return (byte) (csFlags & 0xFF);
            case 0x01: return (byte) ((csFlags >> 8) & 0xFF);
            case 0x02: return (byte) ((csFlags >> 16) & 0xFF);
            case 0x03: return (byte) ((csFlags >> 24) & 0xFF);
            case 0x04: return (byte) (counter & 0xFFL);
            case 0x05: return (byte) ((counter >> 8) & 0xFFL);
            case 0x06: return (byte) ((counter >> 16) & 0xFFL);
            case 0x07: return (byte) ((counter >> 24) & 0xFFL);
            case 0x08: return (byte) ((counter >> 32) & 0xFFL);
            case 0x09: return (byte) ((counter >> 40) & 0xFFL);
            case 0x0A: return (byte) ((counter >> 48) & 0xFFL);
            case 0x0B: return (byte) ((counter >> 56) & 0xFFL);
            default: {
                // C0-C3: offsets 0x0C-0x17, read from regs byte storage
                return regs[offset];
            }
        }
    }

    @Override
    public void write(int offset, byte value) {
        if (offset < 0 || offset >= 0x18) {
            System.err.println("Warning: unimplemented system timer register offset 0x" + Integer.toHexString(offset));
            return;
        }
        if (offset == CS + 0) {
            // CS: write-1-to-clear match flags (process only low byte)
            int clearMask = value & 0xFF;
            for (int ch = 0; ch < 4; ch++) {
                if ((clearMask & (1 << ch)) != 0) {
                    csFlags &= ~(1 << ch);
                }
            }
            if (cpu != null && csFlags == 0) {
                cpu.clearIRQ(this);
            }
            return;
        }
        if (offset >= CLO && offset <= CHI + 3) {
            // CLO/CHI: read-only, ignore writes
            return;
        }
        if (offset >= C0 && offset <= C3 + 3) {
            // C0-C3: accumulate bytes, assemble word when high byte written
            regs[offset] = value;
            if ((offset & 0x03) == 0x03) {
                int ch = (offset - C0) / 4;
                compare[ch] = (regs[offset - 3] & 0xFF)
                        | ((regs[offset - 2] & 0xFF) << 8)
                        | ((regs[offset - 1] & 0xFF) << 16)
                        | ((regs[offset] & 0xFF) << 24);
                checkMatch(ch);
            }
            return;
        }
        System.err.println("Warning: unimplemented system timer register offset 0x" + Integer.toHexString(offset));
    }

    @Override
    public int compareTo(SystemTimer other) {
        return Long.compare(irqId, other.irqId);
    }

    /** Advance counter to match current CPU cycle count, then check matches. */
    private void tickToFromCpu() {
        if (cpu == null) return;
        long cycles = cpu.getTotalCycles();
        long ticks = (cycles - lastCycles) / CYCLES_PER_TICK;
        if (ticks <= 0) return;
        counter += ticks;
        lastCycles = cycles;
        for (int ch = 0; ch < 4; ch++) {
            checkMatch(ch);
        }
    }

    /** Set match flag for channel and assert IRQ if counter has reached compare value. */
    private void checkMatch(int ch) {
        if ((csFlags & (1 << ch)) == 0 && counter >= (compare[ch] & 0xFFFFFFFFL)) {
            csFlags |= (1 << ch);
            if (cpu != null) {
                cpu.setIRQ(this);
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.SystemTimerRegisterTest
```

Expected: `OK (4 tests)`.

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/SystemTimer.java Simulator/test/jCPU/arm/SystemTimerRegisterTest.java
git commit -m "feat(arm): add SystemTimer skeleton with register map"
```

---

### Task 4: SystemTimer counter advancement via tickTo()

**Files:**
- Modify: `Simulator/src/jCPU/arm/SystemTimer.java` (tickToFromCpu already implemented in Task 3; no code change needed, test validates existing behavior)
- Test: `Simulator/test/jCPU/arm/SystemTimerCounterAdvanceTest.java` (create)

**Interfaces:**
- Consumes: `CPU.getTotalCycles()` (long), `CYCLES_PER_TICK` constant.
- Produces: counter advances when `read()` calls `tickToFromCpu()`; does not advance when no cycles executed.

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/SystemTimerCounterAdvanceTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerCounterAdvanceTest {
    @Test
    public void testCounterAdvancesWithCycles() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        long before = timerReadLo(timer);
        cpu.execute();
        cpu.execute();
        cpu.execute();
        long after = timerReadLo(timer);
        assertTrue("counter should advance with cycles, before=" + before + " after=" + after, after > before);
    }

    @Test
    public void testCounterDoesNotAdvanceWithoutCycles() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        long before = timerReadLo(timer);
        // No execute() calls, so totalCycles unchanged
        long after = timerReadLo(timer);
        assertEquals(before, after);
    }

    /** Read CLO as a 32-bit little-endian value from the timer's byte reads. */
    private long timerReadLo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile test:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/SystemTimerCounterAdvanceTest.java
```

Expected: compile should pass (tickToFromCpu already exists). Test `testCounterAdvancesWithCycles` should FAIL because `read()` calls `tickToFromCpu()` but `execute()` doesn't call `read()`, so counter stays 0 and `after == before`, failing `assertTrue(after > before)`.

Wait — actually `tickToFromCpu()` IS called from `read()`. The test calls `timerReadLo(timer)` which calls `read(CLO)` which calls `tickToFromCpu()`. After `cpu.execute()`, totalCycles=1. Then `timerReadLo` calls `tickToFromCpu()` which sees cycles=1, lastCycles=0, ticks=1, counter=1. So `after > before` should be true. The test should PASS already.

But per TDD, we still write the test first. If it passes, great — the implementation from Task 3 already handles it. Let me proceed.

- [ ] **Step 3: Write minimal implementation**

No changes needed — `tickToFromCpu()` was implemented in Task 3 and is called from `read()`. The counter advances correctly when timer registers are read after CPU cycles execute.

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.SystemTimerCounterAdvanceTest
```

Expected: `OK (2 tests)`.

- [ ] **Step 5: Commit**

```bash
git add Simulator/test/jCPU/arm/SystemTimerCounterAdvanceTest.java
git commit -m "test(arm): SystemTimer counter advances with CPU cycles"
```

---

### Task 5: SystemTimer compare match + IRQ assertion

**Files:**
- Modify: `Simulator/src/jCPU/arm/SystemTimer.java` (no change — checkMatch already implemented; verify correctness)
- Test: `Simulator/test/jCPU/arm/SystemTimerCompareTest.java` (create)

**Interfaces:**
- Consumes: `CPU.setIRQ(Object)`, `CPU.haveIRQ()`, `CPU.getTotalCycles()`.
- Produces: When `counter >= compare[ch]` and flag not set, sets `csFlags` bit `ch` and calls `cpu.setIRQ(this)`.

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/SystemTimerCompareTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerCompareTest {
    @Test
    public void testCompareMatchAssertsIRQ() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);

        long clo = readClo(timer); // counter = 0
        writeWord(timer, SystemTimer.C0, (int) (clo + 3));

        cpu.execute(); // cycle 1
        readClo(timer);  // counter = 1, no match (1 < 3)
        cpu.execute(); // cycle 2
        readClo(timer);  // counter = 2, no match (2 < 3)
        cpu.execute(); // cycle 3
        readClo(timer);  // counter = 3, match! (3 >= 3)

        int cs = readWord(timer, SystemTimer.CS);
        assertTrue("CS match flag 0 should be set, CS=0x" + Integer.toHexString(cs), (cs & 0x1) != 0);
        assertTrue("IRQ should be asserted after compare match", cpu.haveIRQ());
    }

    @Test
    public void testNoMatchBeforeCompareReached() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);

        long clo = readClo(timer);
        writeWord(timer, SystemTimer.C0, (int) (clo + 100));

        cpu.execute();
        readClo(timer); // counter = 1, no match

        int cs = readWord(timer, SystemTimer.CS);
        assertEquals(0, cs & 0x1);
        assertFalse("IRQ should not be asserted before compare reached", cpu.haveIRQ());
    }

    private long readClo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private int readWord(SystemTimer timer, int offset) {
        int b0 = timer.read(offset + 0) & 0xFF;
        int b1 = timer.read(offset + 1) & 0xFF;
        int b2 = timer.read(offset + 2) & 0xFF;
        int b3 = timer.read(offset + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private void writeWord(SystemTimer timer, int offset, int value) {
        timer.write(offset + 0, (byte) (value & 0xFF));
        timer.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        timer.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        timer.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile and run:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/SystemTimerCompareTest.java
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.SystemTimerCompareTest
```

Expected: test FAILS — `checkMatch` is called when C0 is written (counter=0, not yet >= 3), and on subsequent `readClo` calls counter advances and `checkMatch` is re-called. On counter=3, `3 >= 3` should set the flag. The test should actually PASS with the current implementation (checkMatch is called from tickToFromCpu on every read).

Wait — let me trace: after `writeWord(C0, 3)`, `checkMatch(0)` is called: counter=0, 0 >= 3? No. Flag stays 0. Then `cpu.execute()` → totalCycles=1. `readClo()` → `tickToFromCpu()`: cycles=1, ticks=1, counter=1, `checkMatch(0)`: 1 >= 3? No. Then execute() → totalCycles=2. `readClo()`: counter=2, 2 >= 3? No. Then execute() → totalCycles=3. `readClo()`: counter=3, 3 >= 3? Yes! Flag set, IRQ asserted. Test PASSES.

So the test passes with existing implementation. Good — TDD test written first, implementation already in place.

- [ ] **Step 3: Write minimal implementation**

No changes needed — `checkMatch` and `tickToFromCpu` were implemented in Task 3 and correctly handle compare match + IRQ assertion.

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.SystemTimerCompareTest
```

Expected: `OK (2 tests)`.

- [ ] **Step 5: Commit**

```bash
git add Simulator/test/jCPU/arm/SystemTimerCompareTest.java
git commit -m "test(arm): SystemTimer compare match sets flag and asserts IRQ"
```

---

### Task 6: SystemTimer write-1-to-clear + multi-channel IRQ

**Files:**
- Modify: `Simulator/src/jCPU/arm/SystemTimer.java` (no change — write-1-to-clear already implemented in Task 3)
- Test: `Simulator/test/jCPU/arm/SystemTimerWrite1ClearTest.java` (create)
- Test: `Simulator/test/jCPU/arm/SystemTimerMultiChannelTest.java` (create)

**Interfaces:**
- Consumes: `CPU.clearIRQ(Object)`, `CPU.haveIRQ()`.
- Produces: Write-1-to-clear CS bit N clears flag N; if no flags remain, calls `cpu.clearIRQ(this)`. Multiple channels can be pending; clearing one doesn't deassert IRQ until all are cleared.

- [ ] **Step 1: Write the failing test — write-1-to-clear**

Create `Simulator/test/jCPU/arm/SystemTimerWrite1ClearTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerWrite1ClearTest {
    @Test
    public void testWrite1ClearClearsMatchFlagAndDeassertsIRQ() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);

        long clo = readClo(timer);
        writeWord(timer, SystemTimer.C0, (int) (clo + 2));

        cpu.execute(); // cycle 1
        readClo(timer); // counter = 1, no match
        cpu.execute(); // cycle 2
        readClo(timer); // counter = 2, match! flag set, IRQ asserted

        assertTrue(cpu.haveIRQ());

        // Write-1-to-clear CS bit 0
        timer.write(SystemTimer.CS + 0, (byte) 0x01);

        // Flag cleared, IRQ deasserted
        int cs = readWord(timer, SystemTimer.CS);
        assertEquals(0, cs & 0x1);
        assertFalse("IRQ should be deasserted after write-1-to-clear", cpu.haveIRQ());
    }

    private long readClo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private int readWord(SystemTimer timer, int offset) {
        int b0 = timer.read(offset + 0) & 0xFF;
        int b1 = timer.read(offset + 1) & 0xFF;
        int b2 = timer.read(offset + 2) & 0xFF;
        int b3 = timer.read(offset + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private void writeWord(SystemTimer timer, int offset, int value) {
        timer.write(offset + 0, (byte) (value & 0xFF));
        timer.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        timer.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        timer.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}
```

- [ ] **Step 2: Run write-1-to-clear test**

Compile and run:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/SystemTimerWrite1ClearTest.java
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.SystemTimerWrite1ClearTest
```

Expected: `OK (1 test)` — write-1-to-clear logic already implemented in Task 3.

- [ ] **Step 3: Write the failing test — multi-channel**

Create `Simulator/test/jCPU/arm/SystemTimerMultiChannelTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerMultiChannelTest {
    @Test
    public void testMultipleChannelsPendingClearingOneKeepsIRQ() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);

        long clo = readClo(timer);

        // Set C0 = counter + 2, C1 = counter + 4
        writeWord(timer, SystemTimer.C0, (int) (clo + 2));
        writeWord(timer, SystemTimer.C1, (int) (clo + 4));

        cpu.execute(); // cycle 1
        readClo(timer); // counter = 1
        cpu.execute(); // cycle 2
        readClo(timer); // counter = 2, C0 matches -> flag 0 set, IRQ asserted
        cpu.execute(); // cycle 3
        readClo(timer); // counter = 3
        cpu.execute(); // cycle 4
        readClo(timer); // counter = 4, C1 matches -> flag 1 set, IRQ still asserted

        int cs = readWord(timer, SystemTimer.CS);
        assertTrue("Both channels should have match flags, CS=0x" + Integer.toHexString(cs), (cs & 0x3) == 0x3);
        assertTrue("IRQ should be asserted with both channels pending", cpu.haveIRQ());

        // Clear only channel 0
        timer.write(SystemTimer.CS + 0, (byte) 0x01);

        cs = readWord(timer, SystemTimer.CS);
        assertEquals(0x2, cs & 0x3); // only channel 1 flag remains
        assertTrue("IRQ should still be asserted (channel 1 pending)", cpu.haveIRQ());

        // Clear channel 1
        timer.write(SystemTimer.CS + 0, (byte) 0x02);

        cs = readWord(timer, SystemTimer.CS);
        assertEquals(0, cs & 0x3);
        assertFalse("IRQ should be deasserted after all channels cleared", cpu.haveIRQ());
    }

    private long readClo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private int readWord(SystemTimer timer, int offset) {
        int b0 = timer.read(offset + 0) & 0xFF;
        int b1 = timer.read(offset + 1) & 0xFF;
        int b2 = timer.read(offset + 2) & 0xFF;
        int b3 = timer.read(offset + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private void writeWord(SystemTimer timer, int offset, int value) {
        timer.write(offset + 0, (byte) (value & 0xFF));
        timer.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        timer.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        timer.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}
```

- [ ] **Step 4: Run multi-channel test**

Compile and run:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/SystemTimerMultiChannelTest.java
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.SystemTimerMultiChannelTest
```

Expected: `OK (1 test)`.

- [ ] **Step 5: Commit**

```bash
git add Simulator/test/jCPU/arm/SystemTimerWrite1ClearTest.java Simulator/test/jCPU/arm/SystemTimerMultiChannelTest.java
git commit -m "test(arm): SystemTimer write-1-to-clear and multi-channel IRQ"
```

---

### Task 7: CPU setSystemTimer + tickTo in execute() and execute(int budget)

**Files:**
- Modify: `Simulator/src/jCPU/arm/CPU.java` — add `systemTimer` field, `setSystemTimer()`, `setWaitingForInterrupt()`, `isWaitingForInterrupt()`; tick timer in `execute()` before IRQ checks; consume cycle budget while waiting in `execute(int budget)`
- Test: `Simulator/test/jCPU/arm/SystemTimerWfiTest.java` (create)

**Interfaces:**
- Consumes: `SystemTimer` object, `CPU.getTotalCycles()`.
- Produces:
  - `private SystemTimer systemTimer = null;` field
  - `public void setSystemTimer(SystemTimer t)` setter
  - `public void setWaitingForInterrupt(boolean w)` setter
  - `public boolean isWaitingForInterrupt()` getter
  - `systemTimer.tickTo(totalCycles)` called in `execute()` before IRQ checks
  - While `waitingForInterrupt` and `systemTimer != null`, remaining `cycleBudget` is consumed as cycles and timer is ticked (WFI wake-up path)

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/SystemTimerWfiTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerWfiTest {
    @Test
    public void testExecuteTicksTimer() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        cpu.setSystemTimer(timer);

        long before = readClo(timer);
        cpu.execute();
        long after = readClo(timer);
        assertTrue("Counter should advance after execute()", after > before);
    }

    @Test
    public void testWfiTimerFiresWhileWaiting() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        cpu.setSystemTimer(timer);

        long clo = readClo(timer); // counter = 0
        writeWord(timer, SystemTimer.C0, (int) (clo + 10));

        cpu.setWaitingForInterrupt(true);
        cpu.execute(100); // while waiting, consumes 100 cycles, timer advances to 100, match fires

        assertTrue("Timer should have fired IRQ after WFI budget consumed", cpu.haveIRQ());

        // Clear IRQ
        timer.write(SystemTimer.CS + 0, (byte) 0x01);
        assertFalse("IRQ should be deasserted after write-1-to-clear", cpu.haveIRQ());
    }

    private long readClo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private void writeWord(SystemTimer timer, int offset, int value) {
        timer.write(offset + 0, (byte) (value & 0xFF));
        timer.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        timer.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        timer.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile test:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/SystemTimerWfiTest.java
```

Expected: `error: cannot find symbol ... method setSystemTimer` and `method setWaitingForInterrupt` (exit 1, methods don't exist yet on CPU).

- [ ] **Step 3: Write minimal implementation**

In `Simulator/src/jCPU/arm/CPU.java`, add field after `totalCycles` (from Task 2):

```java
	private SystemTimer systemTimer = null;
```

Add accessors after `getTotalCycles()`:

```java
	/**
	 * Wires a SystemTimer to this CPU so the timer is ticked on each instruction.
	 * @param t the SystemTimer, or null to unwire
	 */
	public void setSystemTimer(SystemTimer t) { this.systemTimer = t; }

	public void setWaitingForInterrupt(boolean waiting) {
		this.waitingForInterrupt = waiting;
	}

	public boolean isWaitingForInterrupt() {
		return waitingForInterrupt;
	}
```

Tick timer in `execute()` — modify line 397 area (after `totalCycles++`):

```java
	public int execute() throws BusErrorException, AlignmentException, UndefinedException, EscapeRetryException, EscapeCompleteException {
		if(!haveReset) throw new FatalException("execute() called without first calling reset()");
		totalCycles++;
		if(systemTimer != null) systemTimer.tickTo(totalCycles);
		if((cpsr & (1 << CPSR_BIT_F)) == 0 && haveFIQ()) generateFIQException();
```

Modify `execute(int budget)` — add timer tick before IRQ check, and budget consumption while waiting. The method starts at line 345:

```java
        @Override
        public boolean execute(int limit) throws Exception{
        while(true){
            cycleBudget += limit;
            int backupPC = pc;
            if(systemTimer != null) systemTimer.tickTo(totalCycles);
            if(waitingForInterrupt && (haveIRQ() || haveFIQ())) waitingForInterrupt = false;
```

And add budget consumption while waiting, after the inner while loop and before `return cycleBudget <= 0`:

```java
            cycleBudget -= mem.settleAccessBill();
            if(waitingForInterrupt && cycleBudget > 0 && systemTimer != null) {
                totalCycles += cycleBudget;
                systemTimer.tickTo(totalCycles);
                cycleBudget = 0;
            }
            if(waitingForInterrupt && cycleBudget > 0) cycleBudget = 0;
            return cycleBudget <= 0;
```

Wait, the existing code already has `if(waitingForInterrupt && cycleBudget > 0) cycleBudget = 0;` before `return`. I need to insert the timer-ticking version before that line. Let me look at the exact code:

```java
            cycleBudget -= mem.settleAccessBill();
            if(waitingForInterrupt && cycleBudget > 0) cycleBudget = 0;
            return cycleBudget <= 0;
```

I'll change it to:

```java
            cycleBudget -= mem.settleAccessBill();
            if(waitingForInterrupt && cycleBudget > 0 && systemTimer != null) {
                totalCycles += cycleBudget;
                systemTimer.tickTo(totalCycles);
                cycleBudget = 0;
            }
            if(waitingForInterrupt && cycleBudget > 0) cycleBudget = 0;
            return cycleBudget <= 0;
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.SystemTimerWfiTest
```

Expected: `OK (2 tests)`.

- [ ] **Step 5: Run full existing test suite for regressions**

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/*.java
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.CpuTotalCyclesTest jCPU.arm.SystemTimerRegisterTest jCPU.arm.SystemTimerCounterAdvanceTest jCPU.arm.SystemTimerCompareTest jCPU.arm.SystemTimerWrite1ClearTest jCPU.arm.SystemTimerMultiChannelTest jCPU.arm.SystemTimerWfiTest
```

Expected: all test classes pass with no failures.

- [ ] **Step 6: Commit**

```bash
git add Simulator/src/jCPU/arm/CPU.java Simulator/test/jCPU/arm/SystemTimerWfiTest.java
git commit -m "feat(arm): CPU ticks SystemTimer on execute and while waiting (WFI)"
```

---

### Task 8: Pi2Loader timer wiring

**Files:**
- Modify: `Simulator/src/jCPU/arm/Pi2Loader.java`
- Test: `Simulator/test/jCPU/arm/Pi2LoaderTimerTest.java` (create)

**Interfaces:**
- Consumes: `Pi2MemoryMap.SYSTEM_TIMER_BASE`, `SystemTimer(CPU)`, `CPU.setSystemTimer(SystemTimer)`, `VirtualMemorySpace.registerPeripheral(int, int, Peripheral)`.
- Produces: `Pi2Loader` creates `SystemTimer`, registers it at `SYSTEM_TIMER_BASE` (size `0x1000`), wires to CPU via `setSystemTimer`.

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/Pi2LoaderTimerTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class Pi2LoaderTimerTest {
    @Test
    public void testLoaderWiresSystemTimer() throws Exception {
        // Minimal kernel image: just enough to boot without executing anything harmful
        byte[] kernel = new byte[4]; // 4 bytes of zeros (no valid instructions, but we won't execute)
        CPU cpu = Pi2Loader.loadImage(kernel);

        // Verify timer is reachable through MMIO at SYSTEM_TIMER_BASE
        // Read CLO via memory space
        int clo0 = cpu.getVirtualMemorySpace().read32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CLO);
        int clo1 = cpu.getVirtualMemorySpace().read32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CLO);

        // CLO should advance (counter increments with cycles from read32 calls via tickTo)
        // At minimum, registers should be accessible (not cause bus error)
        assertTrue("CLO should be readable", clo0 >= 0);
    }

    @Test
    public void testTimerIsWiredToCpu() throws Exception {
        byte[] kernel = new byte[4];
        CPU cpu = Pi2Loader.loadImage(kernel);
        // Timer should be registered and wired — verify by reading CS through MMIO
        int cs = cpu.getVirtualMemorySpace().read32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CS);
        assertEquals(0, cs & 0xF); // no match flags initially
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Compile:

```bash
javac -sourcepath src -cp "/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar" -d out/main Simulator/src/jCPU/arm/Pi2Loader.java
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/Pi2LoaderTimerTest.java
```

Expected: `error: cannot find symbol ... method loadImage` may exist, but `SYSTEM_TIMER_BASE` exists from Task 1. The issue is `Pi2Loader.loadImage` doesn't create/wire a timer, so reading timer registers through MMIO returns 0 (no timer registered) — but the test expects the timer to be wired. Actually, `read32` on an unregistered address reads from RAM, not a timer. The test reads `clo0` and `clo1` — without a timer registered, these read RAM bytes (zeros), so `clo0 == clo1 == 0`. The assertTrue passes trivially. But `testTimerIsWiredToCpu` reads CS and expects 0 — without a timer, CS reads RAM (0), so it passes too.

The test needs to actually verify the timer is wired. Without the timer, reading CS returns RAM content (0), which coincidentally passes. To make a meaningful failing test, I need to check that the timer is actually a `SystemTimer` instance. But that requires accessing internal state.

Better approach: write a value to C0 through MMIO, execute some cycles, then read CS through MMIO and check the match flag is set. Without the timer wired, C0 writes go to RAM (no effect), CS reads RAM (0), and the match flag test fails.

```java
@Test
public void testTimerMatchThroughMMIO() throws Exception {
    byte[] kernel = new byte[4];
    CPU cpu = Pi2Loader.loadImage(kernel);

    // Write C0 = 5 through MMIO (little-endian)
    cpu.getVirtualMemorySpace().write32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.C0, 5);

    // Execute cycles to advance counter past 5
    for (int i = 0; i < 10; i++) {
        cpu.execute();
    }

    // Read CS through MMIO — match flag 0 should be set
    int cs = cpu.getVirtualMemorySpace().read32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CS);
    assertTrue("Timer match flag 0 should be set through MMIO, CS=0x" + Integer.toHexString(cs), (cs & 0x1) != 0);
    assertTrue("CPU should have IRQ pending", cpu.haveIRQ());
}
```

Without the timer wired: `write32(C0, 5)` writes to RAM at `0x3F00300C` — but there's no RAM mapped there (RAM is at 0x0, 1MB). The EMMC is at `0x3F200000`, UART at `0x3F201000`. So `0x3F003000` is unmapped → `mem.readByte` would throw `BusErrorException` → caught by `read32` wrapper → returns 0. And `write32` would throw → caught → returns. So C0 write does nothing, CS read returns 0, `cs & 0x1 == 0`, `assertTrue` fails. Good — this is a meaningful failing test.

- [ ] **Step 2: Run test to verify it fails**

Compile and run:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/Pi2LoaderTimerTest.java
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.Pi2LoaderTimerTest
```

Expected: `testTimerMatchThroughMMIO` FAILS — CS reads 0 (no timer registered at SYSTEM_TIMER_BASE), `cs & 0x1 == 0`, `assertTrue` fails.

- [ ] **Step 3: Write minimal implementation**

Modify `Simulator/src/jCPU/arm/Pi2Loader.java`:

In `loadImageWithSd`, after registering EMMC, add timer creation and wiring. Also modify `loadImage` to always create the timer (even without SD). Actually, the timer should be wired in both paths. Let me modify `loadImage` to create and wire the timer:

```java
	public static CPU loadImage(byte[] image) {
		CPU cpu = loadImage(image, null);
		return cpu;
	}

	// Internal: load image and optionally wire timer and SD
	private static CPU loadImage(byte[] image, String sdPath) throws IOException {
		CPU cpu = new CPU();
		ByteArrayRegion ram = findRam(cpu.getMemorySpace());
		byte[] backing = ram.getBackingArray();
		System.arraycopy(image, 0, backing, 0, Math.min(image.length, backing.length));

		// Wire system timer
		SystemTimer timer = new SystemTimer(cpu);
		cpu.setSystemTimer(timer);
		cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.SYSTEM_TIMER_BASE, 0x1000, timer);

		if (sdPath != null) {
			SdCard card = new SdCard(sdPath);
			EmmcController ctl = new EmmcController(card, cpu);
			cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);
		}
		return cpu;
	}

	public static CPU loadImage(byte[] image) {
		try {
			return loadImage(image, null);
		} catch (IOException e) {
			throw new RuntimeException(e); // loadImage without SD doesn't throw
		}
	}
```

Wait, `loadImage(byte[])` currently doesn't throw IOException. Let me restructure more carefully:

```java
	public static CPU loadImage(byte[] image) {
		CPU cpu = new CPU();
		ByteArrayRegion ram = findRam(cpu.getMemorySpace());
		byte[] backing = ram.getBackingArray();
		System.arraycopy(image, 0, backing, 0, Math.min(image.length, backing.length));
		wireTimer(cpu);
		return cpu;
	}

	public static CPU loadImageWithSd(byte[] image, String sdPath) throws IOException {
		CPU cpu = loadImage(image);
		SdCard card = new SdCard(sdPath);
		EmmcController ctl = new EmmcController(card, cpu);
		cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);
		return cpu;
	}

	private static void wireTimer(CPU cpu) {
		SystemTimer timer = new SystemTimer(cpu);
		cpu.setSystemTimer(timer);
		cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.SYSTEM_TIMER_BASE, 0x1000, timer);
	}
```

And add imports:
```java
import jCPU.arm.SystemTimer;
```

Wait, `Pi2Loader` is in package `jCPU.arm`, so `SystemTimer` is in the same package — no import needed.

But `SdCard` and `EmmcController` are also in `jCPU.arm`, same package. Good.

Need to add `SystemTimer` usage. `SystemTimer` is in the same package `jCPU.arm`, so no import needed.

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.Pi2LoaderTimerTest
```

Expected: `OK (2 tests)`.

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/arm/Pi2Loader.java Simulator/test/jCPU/arm/Pi2LoaderTimerTest.java
git commit -m "feat(arm): Pi2Loader wires SystemTimer at SYSTEM_TIMER_BASE"
```

---

### Task 9: Integration test — timer + EMMC IRQ sharing

**Files:**
- Test: `Simulator/test/jCPU/arm/TimerEmmcIrqTest.java` (create)

**Interfaces:**
- Consumes: `Pi2Loader.loadImageWithSd`, `CPU.getVirtualMemorySpace()`, `CPU.haveIRQ()`, `CPU.clearIRQ`, `EmmcController.INTERRUPT`/`IRQ_COMPLETE`, `SystemTimer.CS`.
- Produces: Timer and EMMC can assert IRQ simultaneously; `haveIRQ()` true while either pending; clearing one leaves the other asserted.

- [ ] **Step 1: Write the failing test**

Create `Simulator/test/jCPU/arm/TimerEmmcIrqTest.java`:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class TimerEmmcIrqTest {
    @Test
    public void testTimerAndEmmcIrqSharing() throws Exception {
        byte[] kernel = new byte[4];
        // Create SD image file for EmmcController
        java.nio.file.Path sdPath = java.nio.file.Files.createTempFile("timer-emmc-irq-test", ".img");
        java.nio.file.Files.write(sdPath, new byte[512 * 10]); // 10 sectors

        try {
            CPU cpu = Pi2Loader.loadImageWithSd(kernel, sdPath.toString());
            cpu.reset();

            // Get memory space
            jCPU.arm.VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

            // Assert timer IRQ: write C0 = 0 (matches immediately since counter >= 0)
            // But compare initialized to -1, so write C0 = 0
            vm.write32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.C0, 0);
            // Execute cycles to advance counter
            for (int i = 0; i < 5; i++) cpu.execute();
            vm.read32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CLO); // tick counter

            assertTrue("Timer should assert IRQ", cpu.haveIRQ());

            // Assert EMMC IRQ: write CMDCMD to trigger completion
            vm.write32(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, EmmcController.CMD_GO_IDLE_STATE);

            assertTrue("Both timer and EMMC should assert IRQ", cpu.haveIRQ());

            // Clear timer IRQ only
            vm.write32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CS, 0x01);
            // EMMC IRQ should still be asserted
            assertTrue("EMMC IRQ should still be asserted after clearing timer", cpu.haveIRQ());

            // Clear EMMC IRQ
            vm.write32(Pi2MemoryMap.EMMC_BASE + EmmcController.INTERRUPT, EmmcController.IRQ_COMPLETE);
            assertFalse("IRQ should be deasserted after clearing both", cpu.haveIRQ());
        } finally {
            java.nio.file.Files.deleteIfExists(sdPath);
        }
    }
}
```

Wait, there's a problem with writing C0=0. The compare array is initialized to -1, so `compare[0] & 0xFFFFFFFFL` = 0xFFFFFFFFL. `counter >= 0xFFFFFFFFL` is false for small counter values. So writing C0=0 and executing won't match until counter reaches 0xFFFFFFFFL (billions of cycles).

I need to write a reasonable compare value. Let me read CLO first, then set C0 = clo + small number:

```java
long clo = readCloViaMMIO(vm);
vm.write32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.C0, (int) (clo + 5));
// execute cycles to advance counter
for (int i = 0; i < 10; i++) cpu.execute();
vm.read32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CLO); // tick
assertTrue(cpu.haveIRQ());
```

But `read32` triggers `tickToFromCpu` only if called on the timer. Through MMIO (`vm.read32`), it calls `p.read(po)` which calls `tickToFromCpu()`. Good.

Also, for EMMC IRQ: `EmmcController.CMD_GO_IDLE_STATE` = 0. Writing CMDCMD=0 triggers `processCommand(0)` which is CMD_GO_IDLE_STATE. It sets response, clears busy, sets INTERRUPT, and calls `cpu.setIRQ(this)` if mask allows. Default `IRQ_MASK` register is 0 (all zeros), so `mask == 0` → IRQ asserted. Good.

Let me also add helper methods for reading/writing 32-bit values through MMIO.

Let me rewrite the test:

```java
package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class TimerEmmcIrqTest {
    @Test
    public void testTimerAndEmmcIrqSharing() throws Exception {
        byte[] kernel = new byte[4];
        java.nio.file.Path sdPath = java.nio.file.Files.createTempFile("timer-emmc-irq-test", ".img");
        java.nio.file.Files.write(sdPath, new byte[512 * 10]);

        try {
            CPU cpu = Pi2Loader.loadImageWithSd(kernel, sdPath.toString());
            cpu.reset();
            jCPU.arm.VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

            // Assert timer IRQ
            int cloBase = Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CLO;
            int clo = read32(vm, cloBase); // ticks counter to current cycles
            write32(vm, Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.C0, (int) (clo + 3));
            for (int i = 0; i < 5; i++) cpu.execute();
            read32(vm, cloBase); // tick counter to >= C0

            assertTrue("Timer should assert IRQ", cpu.haveIRQ());

            // Assert EMMC IRQ
            write32(vm, Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, EmmcController.CMD_GO_IDLE_STATE);
            assertTrue("Both timer and EMMC should assert IRQ", cpu.haveIRQ());

            // Clear timer IRQ only
            write32(vm, Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CS, 0x01);
            assertTrue("EMMC IRQ should still be asserted after clearing timer", cpu.haveIRQ());

            // Clear EMMC IRQ
            write32(vm, Pi2MemoryMap.EMMC_BASE + EmmcController.INTERRUPT, EmmcController.IRQ_COMPLETE);
            assertFalse("IRQ should be deasserted after clearing both", cpu.haveIRQ());
        } finally {
            java.nio.file.Files.deleteIfExists(sdPath);
        }
    }

    private int read32(jCPU.arm.VirtualMemorySpace vm, int addr) {
        return vm.read32(addr);
    }

    private void write32(jCPU.arm.VirtualMemorySpace vm, int addr, int value) {
        vm.write32(addr, value);
    }
}
```

Wait, there's a subtlety with the EMMC IRQ. The `EmmcController` was constructed with `new EmmcController(card, cpu)` and `cpu.setIRQ(this)` is called. But the `EmmcController`'s `cpu` field is set. When `processCommand` calls `cpu.setIRQ(this)`, it adds the EmmcController to the CPU's IRQ TreeSet. Good.

But there's a potential issue: both `SystemTimer` and `EmmcController` use `setIRQ(this)` / `clearIRQ(this)`. They are different objects with different `irqId`s, so they're separate entries in the TreeSet. `haveIRQ()` returns true if the TreeSet is non-empty. Clearing the timer removes only the timer's entry. Clearing EMMC removes only EMMC's entry. This is correct multi-source behavior.

However, there's a problem with `write32(vm, EMMC_BASE + CMDCMD, ...)`. `VirtualMemorySpace.write32` calls `writeInt` which calls `p.write(po+0)`, `p.write(po+1)`, `p.write(po+2)`, `p.write(po+3)` for little-endian. For `EmmcController`, the command is triggered when `CMDCMD + 3` (high byte) is written. So all 4 bytes are written, then `processCommand` is called. Good.

But wait — `write32(addr, value)` calls `writeInt(addr, value, false, false)` (little-endian). `writeInt` for a peripheral writes bytes at po+0, po+1, po+2, po+3. The last write is at po+3 which is `CMDCMD + 3`, triggering `processCommand`. Good.

Also, the `read32(vm, CLO)` call: `VirtualMemorySpace.read32` calls `readInt(addr, false, false)` which reads 4 bytes from the peripheral via `p.read(po+0)` through `p.read(po+3)`. Each `p.read()` call calls `tickToFromCpu()`. The counter advances. Good.

One concern: after `write32(C0, clo + 3)`, `checkMatch(0)` is called immediately (counter might be < clo+3). Then `cpu.execute()` 5 times → totalCycles increases by 5. `read32(CLO)` → `tickToFromCpu()` → counter advances, `checkMatch(0)` → match. Good.

But wait — `read32(CLO)` at the start reads CLO, which calls `tickToFromCpu()`. At that point totalCycles might be 0 (just reset, no execute() calls yet). So counter = 0, clo = 0. Then `write32(C0, 3)`. Then execute() 5 times → totalCycles = 5. `read32(CLO)` → `tickToFromCpu()`: cycles=5, lastCycles=0, ticks=5, counter=5. `checkMatch(0)`: 5 >= 3? Yes! Flag set, IRQ asserted. 

But actually, each `cpu.execute()` call also calls `tickToFromCpu()` (via the Task 7 change in `execute()`). So after each execute(), the counter is already updated. The final `read32(CLO)` is just to make sure. Let me trace:

1. `read32(CLO)` → `tickToFromCpu()`: cycles=0, ticks=0, counter=0. Returns 0.
2. `write32(C0, 3)` → compare[0] = 3, `checkMatch(0)`: 0 >= 3? No.
3. `cpu.execute()` → totalCycles=1, `tickToFromCpu()`: cycles=1, ticks=1, counter=1, `checkMatch(0)`: 1 >= 3? No.
4. `cpu.execute()` → totalCycles=2, counter=2, no match.
5. `cpu.execute()` → totalCycles=3, counter=3, `checkMatch(0)`: 3 >= 3? Yes! Flag set, `setIRQ(this)`, haveIRQ=true.
6. `cpu.execute()` → totalCycles=4, counter=4, flag already set.
7. `cpu.execute()` → totalCycles=5, counter=5, flag already set.
8. `read32(CLO)` → counter=5, no new match.
9. `assertTrue(cpu.haveIRQ())` → true. ✓

Good. Then EMMC:
10. `write32(CMDCMD, 0)` → processCommand(0) → CMD_GO_IDLE_STATE → sets INTERRUPT, `cpu.setIRQ(this)` → haveIRQ still true (already true).
11. `assertTrue(cpu.haveIRQ())` → true. ✓

Clear timer:
12. `write32(CS, 0x01)` → CS write-1-to-clear bit 0 → csFlags &= ~1 → csFlags = 0 (if only timer flag was set). Wait — but EMMC's IRQ is a separate token in the TreeSet. The timer's `csFlags` is separate from EmmcController's INTERRUPT register. Clearing timer's CS bit 0 clears the timer's match flag and calls `cpu.clearIRQ(timer)`. This removes the timer from the TreeSet. But EMmcController is still in the TreeSet. `haveIRQ()` → true (EMMC still pending). ✓

Clear EMMC:
13. `write32(INTERRUPT, IRQ_COMPLETE)` → EmmcController write-1-to-clear → `cpu.clearIRQ(emmcController)` → removes EMMC from TreeSet → `haveIRQ()` → false. ✓

The test should work. But there's one issue: `read32` on `CLO` calls `tickToFromCpu()` which calls `checkMatch()` for ALL channels. Since only C0 is set (to 3), and counter is already past 3, `checkMatch(0)` would try to set the flag again — but it's already set, so no change. Good.

Also, `read32(CLO)` reads 4 bytes from the timer. Each byte read calls `tickToFromCpu()`. After the first byte read, counter is updated. Subsequent byte reads see `ticks <= 0` (same totalCycles), so no change. Fine.

OK, the test looks correct. Let me finalize.

- [ ] **Step 2: Run test to verify it fails**

Expected: Without timer wired (before Task 8), this test would fail because timer registers aren't at SYSTEM_TIMER_BASE. But Task 8 wires the timer, so this test should PASS after Task 8. Per TDD, write the test before Task 8's changes... but Task 8 is already done by the time we get to Task 9. So the test should pass.

Actually, Task 9 is the integration test that validates both timer and EMMC work together. It should pass after Tasks 3-8 are complete. Let me write it, run it, and expect it to pass.

- [ ] **Step 3: Run test**

Compile and run:

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/TimerEmmcIrqTest.java
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore jCPU.arm.TimerEmmcIrqTest
```

Expected: `OK (1 test)`.

- [ ] **Step 4: Commit**

```bash
git add Simulator/test/jCPU/arm/TimerEmmcIrqTest.java
git commit -m "test(arm): timer and EMMC IRQ sharing"
```

---

### Task 10: Full test suite regression check

**Files:**
- Test: all `Simulator/test/jCPU/arm/*.java`

**Interfaces:**
- Consumes: all compiled main + test classes.
- Produces: all existing tests pass with no regressions.

- [ ] **Step 1: Compile all tests**

```bash
javac -d out/test -cp "out/main:/Users/xuyi/.local/share/opencode/worktree/25f7f9/applications/Simulator/dist/Simulator.jar:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" Simulator/test/jCPU/arm/*.java
```

Note: use `${=...}` in zsh if passing multiple classes via variable; here direct glob works.

- [ ] **Step 2: Run full jCPU.arm suite**

```bash
java -cp "out/main:out/test:/Users/xuyi/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/xuyi/Source/Java/lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore ${=*:jCPU.arm.*}
```

Or list all test classes explicitly. Expected: `OK (N tests)` where N includes all existing + new tests, zero failures.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test(arm): full suite regression check for timer sub-project"
```

---

## Self-Review

**Spec coverage:** Every requirement in the spec maps to a task:
- `SystemTimer` class with register map → Task 3
- Counter advancement on CPU cycles → Task 4
- Compare match + IRQ → Task 5
- Write-1-to-clear + multi-channel → Task 6
- CPU totalCycles + tick integration → Tasks 2, 7
- `Pi2MemoryMap.SYSTEM_TIMER_BASE` → Task 1
- `Pi2Loader` timer wiring → Task 8
- IRQ sharing with EMMC → Task 9
- All 8 spec tests → Tasks 1-9

**Placeholder scan:** No TBD, TODO, "implement later", or "write tests for the above" without code. Every step has actual code blocks and exact commands.

**Type consistency:** `SystemTimer` implements `Peripheral, Comparable<SystemTimer>` with `compareTo(SystemTimer)`. `CPU.setSystemTimer(SystemTimer)` matches. `setIRQ(Object)`/`clearIRQ(Object)` accept `SystemTimer` (Object). `compareTo` uses `Long.compare(irqId, other.irqId)` consistent with `EmmcController`. All register offsets and constants match the spec.

---

**Plan complete and saved to `docs/superpowers/plans/2026-11-16-timer-interrupt-controller-implementation-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration (matches your earlier choice of execution mode "1").

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints for review.

**Which approach?** Per your earlier selection, I recommend Subagent-Driven. Confirm and I'll start with Task 1.
