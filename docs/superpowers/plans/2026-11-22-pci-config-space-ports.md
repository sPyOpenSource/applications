# PCI Configuration Space Ports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the emulator's PCI configuration space ports (`0xCF8–0xCFC`) behave like a real PCI host bridge so guest software (jxcore's `PCIGod`) can probe the PCI bus without throwing `Error("no PCI Bus detected")`.

**Architecture:** New `X86PciConfigPeripheral` class implementing `jCPU.Peripheral`, registered at I/O ports `0xCF8–0xCFF` in `CPU.java`. Byte-oriented read/write; 32-bit `inl`/`outl` are already decomposed into consecutive byte accesses by the executors. CONFIG_ADDRESS is read/write; CONFIG_DATA returns `0xFFFFFFFF` (no device). No new IRQ/PIC system.

**Tech Stack:** Java 17, JUnit 4, Ant build (`Simulator/build.xml`).

## Global Constraints

- Keep `Simulator/build/built-jar.properties`, `assets/dist/assets.jar`, `ifOS/dist/ifOS.jar` untouched.
- Do not modify existing peripherals (VGA, UART, IDE, UHCI, Yukon NIC, PIT, floppy, BIOS/UEFI).
- Keep existing `Peripheral` interface unchanged (`read(int)` / `write(int, byte)`).
- PCI config ports at `0xCF8–0xCFF` (no conflicts with existing port ranges).
- CONFIG_DATA reads return `0xFFFFFFFF` (standard "no device" value).
- No new IRQ/PIC system; config-space access only.

## File Structure

- Create: `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java` — PCI config space peripheral
- Create: `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java` — unit tests
- Modify: `Simulator/src/jCPU/x86/CPU.java` — instantiate + register ports

---

### Task 1: Skeleton class + register port constants

**Files:**
- Create: `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java`

**Interfaces:**
- Consumes: `jCPU.Peripheral` interface (`byte read(int port)`, `void write(int port, byte value)`).
- Produces: `public class X86PciConfigPeripheral implements Peripheral` with port constants, empty `read`/`write` stubs, and a no-arg constructor.

- [ ] **Step 1: Write the skeleton class**

```java
package jCPU.x86;

import jCPU.Peripheral;

/**
 * PCI configuration space port emulation.
 *
 * Provides the CONFIG_ADDRESS (0xCF8) and CONFIG_DATA (0xCFC) ports used by
 * guest software to enumerate the PCI bus.  Multi-byte (32-bit) port access
 * is decomposed into consecutive byte reads/writes by the executors, so this
 * peripheral only needs byte-level read/write.
 */
public class X86PciConfigPeripheral implements Peripheral
{
    public static final int PORT_CONFIG_ADDRESS = 0xCF8; // 0xCF8-0xCFB
    public static final int PORT_CONFIG_DATA    = 0xCFC; // 0xCFC-0xCFF

    private final byte[] configAddress = new byte[4];

    public X86PciConfigPeripheral() {
    }

    @Override
    public byte read(int port) {
        return 0;
    }

    @Override
    public void write(int port, byte value) {
    }
}
```

- [ ] **Step 2: Compile-check**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
javac -nowarn -cp build/classes -d build/classes src/jCPU/x86/X86PciConfigPeripheral.java
```
Expected: no errors; `build/classes/jCPU/x86/X86PciConfigPeripheral.class` created.

- [ ] **Step 3: Commit**

```bash
git add Simulator/src/jCPU/x86/X86PciConfigPeripheral.java
git commit -m "x86: add PCI config space peripheral skeleton"
```

---

### Task 2: Register read/write logic (CONFIG_ADDRESS read/write, CONFIG_DATA returns 0xFF)

**Files:**
- Modify: `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java`
- Create: `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java`

**Interfaces:**
- Consumes: `X86PciConfigPeripheral` class from Task 1, port constants, `Peripheral` interface.
- Produces: Working `read`/`write` for CONFIG_ADDRESS (0xCF8–0xCFB) and CONFIG_DATA (0xCFC–0xCFF returns `0xFF`). Internal state field for config address bytes.

- [ ] **Step 1: Write the failing unit test**

Create `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java`:

```java
package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86PciConfigPeripheralTest
{
    @Test
    public void testConfigAddressReadWriteRoundTrip() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        // write 0x80000000 little-endian to 0xCF8-0xCFB
        pci.write(0xCF8, (byte) 0x00);
        pci.write(0xCF9, (byte) 0x00);
        pci.write(0xCFA, (byte) 0x00);
        pci.write(0xCFB, (byte) 0x80);
        assertEquals((byte) 0x00, pci.read(0xCF8));
        assertEquals((byte) 0x00, pci.read(0xCF9));
        assertEquals((byte) 0x00, pci.read(0xCFA));
        assertEquals((byte) 0x80, pci.read(0xCFB));
    }

    @Test
    public void testConfigDataReadsAllOnes() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        assertEquals((byte) 0xFF, pci.read(0xCFC));
        assertEquals((byte) 0xFF, pci.read(0xCFD));
        assertEquals((byte) 0xFF, pci.read(0xCFE));
        assertEquals((byte) 0xFF, pci.read(0xCFF));
    }

    @Test
    public void testConfigDataWriteIsIgnored() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        pci.write(0xCFC, (byte) 0x12);
        assertEquals((byte) 0xFF, pci.read(0xCFC));
    }

    @Test
    public void testUnknownPortsReturnZero() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        assertEquals((byte) 0x00, pci.read(0xCF7));
        assertEquals((byte) 0x00, pci.read(0xD00));
        pci.write(0xCF7, (byte) 0x42);
        pci.write(0xD00, (byte) 0x42);
    }

    @Test
    public void testGetConfigAddressAssemblesLittleEndian() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        pci.write(0xCF8, (byte) 0x78);
        pci.write(0xCF9, (byte) 0x56);
        pci.write(0xCFA, (byte) 0x34);
        pci.write(0xCFB, (byte) 0x12);
        assertEquals(0x12345678, pci.getConfigAddress());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
ant -q test-single -Dtest.includes=jCPU/x86/X86PciConfigPeripheralTest.java -Djavac.includes=jCPU/x86/X86PciConfigPeripheralTest.java
```
Expected: tests FAIL (read returns 0 for everything; CONFIG_DATA doesn't return 0xFF).

- [ ] **Step 3: Implement register logic**

Replace `read`/`write` stubs and add `getConfigAddress()`:

```java
    @Override
    public byte read(int port) {
        int offset = port - PORT_CONFIG_ADDRESS;
        if (offset >= 0 && offset < 4) {
            return configAddress[offset];
        }
        offset = port - PORT_CONFIG_DATA;
        if (offset >= 0 && offset < 4) {
            return (byte) 0xFF;
        }
        return 0;
    }

    @Override
    public void write(int port, byte value) {
        int offset = port - PORT_CONFIG_ADDRESS;
        if (offset >= 0 && offset < 4) {
            configAddress[offset] = value;
            return;
        }
        offset = port - PORT_CONFIG_DATA;
        if (offset >= 0 && offset < 4) {
            return;
        }
    }

    /** Return the assembled 32-bit CONFIG_ADDRESS value (little-endian). */
    public int getConfigAddress() {
        return (configAddress[0] & 0xFF)
             | ((configAddress[1] & 0xFF) << 8)
             | ((configAddress[2] & 0xFF) << 16)
             | ((configAddress[3] & 0xFF) << 24);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Same command as Step 2. Expected: BUILD SUCCESSFUL, all 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/x86/X86PciConfigPeripheral.java Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java
git commit -m "x86: PCI config space read/write logic"
```

---

### Task 3: Wire PCI config peripheral into CPU.java port registration

**Files:**
- Modify: `Simulator/src/jCPU/x86/CPU.java`

**Interfaces:**
- Consumes: `X86PciConfigPeripheral` class from Task 2.
- Produces: `CPU` constructor instantiates `X86PciConfigPeripheral()` and registers ports `0xCF8–0xCFF`.

- [ ] **Step 1: Add field and port registration**

In `CPU.java`, add field near other peripherals:
```java
	private X86PciConfigPeripheral pciConfig;
```

In constructor, after the Yukon NIC registration block and before the UEFI firmware block:
```java
		// PCI configuration space ports (CONFIG_ADDRESS 0xCF8, CONFIG_DATA 0xCFC)
		pciConfig = new X86PciConfigPeripheral();
		for (int port = 0xCF8; port <= 0xCFF; port++) {
			core.registerPort(port, pciConfig);
		}
```

- [ ] **Step 2: Compile-check**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
ant -q compile
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add Simulator/src/jCPU/x86/CPU.java
git commit -m "x86: wire PCI config space peripheral into CPU port map"
```

---

### Task 4: PCIGod probe simulation test + boot verification

**Files:**
- Test: `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java` (add probe simulation test)

**Interfaces:**
- Consumes: `X86PciConfigPeripheral` from Task 2, wired into `CPU` from Task 3.
- Produces: Additional unit test simulating PCIGod's probe sequence; boot verification of jxcore disk image.

- [ ] **Step 1: Add PCIGod probe simulation test**

Add to `X86PciConfigPeripheralTest.java`:

```java
    @Test
    public void testPcigodProbeSequence() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        // PCIGod probePCI():
        //   int old = ports.inl_p(CONFIG_ADDRESS);  expect bit31 == 0
        assertEquals(0, pci.getConfigAddress() & 0x80000000);
        //   ports.outl_p(CONFIG_ADDRESS, ECD_MASK);  // 0x80000000
        pci.write(0xCF8, (byte) 0x00);
        pci.write(0xCF9, (byte) 0x00);
        pci.write(0xCFA, (byte) 0x00);
        pci.write(0xCFB, (byte) 0x80);
        assertEquals(0x80000000, pci.getConfigAddress());
        //   ports.outb_p(CONFIG_ADDRESS + 3, 0);  // already 0
        assertEquals((byte) 0x00, pci.read(0xCFB));
        //   int mode1res = ports.inl_p(CONFIG_ADDRESS);  expect == written value
        assertEquals(0x80000000, pci.getConfigAddress());
        // CONFIG_DATA reads return all-ones (no device)
        assertEquals((byte) 0xFF, pci.read(0xCFC));
    }
```

- [ ] **Step 2: Run unit tests**

Same command as Task 2 Step 4. Expected: BUILD SUCCESSFUL, all 6 tests PASS.

- [ ] **Step 3: Commit test addition**

```bash
git add Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java
git commit -m "x86: add PCIGod probe simulation test"
```

- [ ] **Step 4: Boot jxcore with PCI config ports present**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
java -Xmx4g -cp "build/classes:build/test/classes" jCPU.x86.boot.UefiBootRunner /private/var/folders/ws/9cwgq9ps1f9cdq1f4zfby4z40000gn/T/opencode/iso6-mbr.img 8000000000 </dev/null > /tmp/iso6-pci-boot.log 2>&1
grep -a -n 'Success reading PCI configuration port\|no PCI Bus detected\|panic\|PANIC' /tmp/iso6-pci-boot.log
```
Expected: contains `Success reading PCI configuration port`; no `no PCI Bus detected`; no panic lines.

- [ ] **Step 5: Full x86 suite**

Same JUnit command as Task 3. Expected: `Tests run: N, Failures: 1` — sole failure is pre-existing `X86CoreTest.testRegisterConstantsAndAccess`.

- [ ] **Step 6: Final diff review**

```bash
git -c core.whitespace=cr-at-eol diff --check && echo CLEAN
git status --short
```
Expected: only the 3 NIC/PCI source files changed; generated artifacts untouched.

- [ ] **Step 7: Commit (if needed)**

No new commit expected — all source already committed in Tasks 1–3. Report commit hashes.
