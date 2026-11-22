# Marvell Yukon 88E8055 NIC Emulation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a minimal-functionality emulated Marvell Yukon 88E8055 NIC to the x86 simulator, connected to the host network via pcap, so guest software can send/receive real Ethernet packets.

**Architecture:** New `X86EthernetNic` class implementing `jCPU.Peripheral`, registered at I/O ports `0x300–0x31F` in `CPU.java`. PCAP backend on a daemon thread for host RX/TX. No new IRQ system — interrupts exposed as a status register bit.

**Tech Stack:** Java 17, JUnit 4, Ant build. PCAP Java binding TBD (project has no pcap dependency yet — see Global Constraints).

## Global Constraints

- Keep `Simulator/build/built-jar.properties`, `assets/dist/assets.jar`, `ifOS/dist/ifOS.jar` untouched.
- Do not modify existing peripherals (VGA, UART, IDE, UHCI, PIT, floppy, BIOS/UEFI).
- Keep existing `Peripheral` interface unchanged (`read(int)` / `write(int, byte)`).
- NIC registers at I/O ports `0x300–0x31F` (no conflicts with existing port ranges).
- Minimal functional NIC: no memory-mapped registers, no descriptor rings, no new IRQ/PIC system.
- Vendor ID `0x11AB` (Marvell), Yukon 88E8055 device ID.
- Interrupts as register-status bits, not CPU interrupt lines.

## File Structure

- Create: `Simulator/src/jCPU/x86/X86EthernetNic.java` — NIC peripheral + pcap backend
- Create: `Simulator/test/jCPU/x86/X86EthernetNicTest.java` — unit tests
- Modify: `Simulator/src/jCPU/x86/CPU.java` — instantiate + register ports

---

### Task 1: Skeleton class + register map constants

**Files:**
- Create: `Simulator/src/jCPU/x86/X86EthernetNic.java`

**Interfaces:**
- Consumes: `jCPU.Peripheral` interface (`byte read(int port)`, `void write(int port, byte value)`).
- Produces: `public class X86EthernetNic implements Peripheral` with register-port constants, empty `read`/`write` stubs returning/doing nothing, and a `main`-free constructor taking a `String ifaceName` (may be `null`).

- [ ] **Step 1: Write the skeleton class**

```java
package jCPU.x86;

import jCPU.Peripheral;

/**
 * Minimal-functionality Marvell Yukon 88E8055 NIC emulation.
 *
 * Registers at I/O ports 0x300-0x31F via the existing Peripheral interface.
 * PCAP host connectivity is added in Task 2; this task only establishes the
 * register map and empty read/write stubs.
 */
public class X86EthernetNic implements Peripheral
{
    /* ---------------- register ports ---------------- */
    public static final int PORT_VENDOR_LO   = 0x300;
    public static final int PORT_VENDOR_HI   = 0x301;
    public static final int PORT_DEVICE_LO   = 0x302;
    public static final int PORT_DEVICE_HI   = 0x303;
    public static final int PORT_MAC_BASE    = 0x304;   // 6 bytes: 0x304-0x309
    public static final int PORT_TX_DATA     = 0x310;
    public static final int PORT_TX_LEN_LO   = 0x311;
    public static final int PORT_TX_LEN_HI   = 0x312;
    public static final int PORT_TX_CMD      = 0x313;
    public static final int PORT_TX_STATUS   = 0x314;
    public static final int PORT_RX_DATA     = 0x320;
    public static final int PORT_RX_LEN_LO   = 0x321;
    public static final int PORT_RX_LEN_HI   = 0x322;
    public static final int PORT_RX_CMD      = 0x323;
    public static final int PORT_RX_STATUS   = 0x324;
    public static final int PORT_INT_STATUS  = 0x330;

    /* ---------------- register values ---------------- */
    public static final short VENDOR_ID_MARVELL = 0x11AB;
    public static final short DEVICE_ID_YUKON_88E8055 = 0x4353;

    /* TX status bits */
    public static final byte TX_STATUS_READY   = 0x01;
    public static final byte TX_STATUS_SENDING = 0x02;
    public static final byte TX_STATUS_ERROR   = 0x04;

    /* RX status bits */
    public static final byte RX_STATUS_PACKET  = 0x01;

    /* Interrupt status bits */
    public static final byte INT_RX_PACKET  = 0x01;
    public static final byte INT_TX_COMPLETE = 0x02;

    /* TX commands */
    public static final byte TX_CMD_SEND = 0x01;

    /* RX commands */
    public static final byte RX_CMD_ACK = 0x01;

    private final String ifaceName;
    private final byte[] mac = new byte[6];

    public X86EthernetNic(String ifaceName) {
        this.ifaceName = ifaceName;
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

- [ ] **Step 2: Compile-check the new class**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
javac -nowarn -cp build/classes -d build/classes src/jCPU/x86/X86EthernetNic.java
```
Expected: `BUILD SUCCESSFUL` (javac prints nothing on success) and `build/classes/jCPU/x86/X86EthernetNic.class` exists.

- [ ] **Step 3: Commit**

```bash
git add Simulator/src/jCPU/x86/X86EthernetNic.java
git commit -m "x86: add Yukon NIC skeleton class"
```

---

### Task 2: Register read/write logic (vendor ID, MAC, TX/RX status, interrupts)

**Files:**
- Modify: `Simulator/src/jCPU/x86/X86EthernetNic.java`
- Test: `Simulator/test/jCPU/x86/X86EthernetNicTest.java`

**Interfaces:**
- Consumes: `X86EthernetNic` class from Task 1, port constants, `Peripheral` interface.
- Produces: Working `read`/`write` for vendor ID, device ID, MAC, TX/RX status/command, interrupt status. Internal state fields for MAC, TX buffer/length/status, RX queue/length/status, interrupt status.

- [ ] **Step 1: Write the failing unit test**

Create `Simulator/test/jCPU/x86/X86EthernetNicTest.java`:

```java
package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86EthernetNicTest
{
    @Test
    public void testVendorIdReadsBackMarvell() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0xAB, nic.read(0x300));
        assertEquals((byte) 0x11, nic.read(0x301));
    }

    @Test
    public void testDeviceIdReadsBackYukon88E8055() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0x53, nic.read(0x302));
        assertEquals((byte) 0x43, nic.read(0x303));
    }

    @Test
    public void testMacAddressReadWrite() {
        X86EthernetNic nic = new X86EthernetNic(null);
        byte[] mac = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
        for (int i = 0; i < 6; i++) {
            nic.write(0x304 + i, mac[i]);
        }
        for (int i = 0; i < 6; i++) {
            assertEquals(mac[i], nic.read(0x304 + i));
        }
    }

    @Test
    public void testTransmitStatusStartsReady() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0x01, nic.read(0x314));
    }

    @Test
    public void testInterruptStatusStartsZero() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0x00, nic.read(0x330));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
ant -q test-single -Dtest.includes=jCPU/x86/X86EthernetNicTest.java -Djavac.includes=jCPU/x86/X86EthernetNicTest.java
```
Expected: tests FAIL (class has no register logic yet; all reads return 0).

- [ ] **Step 3: Implement register logic**

Replace `read`/`write` in `X86EthernetNic.java` with:

```java
    /* TX/RX buffer state */
    private final byte[] txBuffer = new byte[2048];
    private int txLength = 0;
    private byte txStatus = TX_STATUS_READY;

    private final byte[] rxBuffer = new byte[2048];
    private int rxLength = 0;
    private byte rxStatus = 0;

    private byte interruptStatus = 0;

    @Override
    public byte read(int port) {
        int offset = port - PORT_MAC_BASE;
        if (offset >= 0 && offset < 6) {
            return mac[offset];
        }
        switch (port) {
            case PORT_VENDOR_LO: return (byte) (VENDOR_ID_MARVELL & 0xFF);
            case PORT_VENDOR_HI: return (byte) ((VENDOR_ID_MARVELL >> 8) & 0xFF);
            case PORT_DEVICE_LO: return (byte) (DEVICE_ID_YUKON_88E8055 & 0xFF);
            case PORT_DEVICE_HI: return (byte) ((DEVICE_ID_YUKON_88E8055 >> 8) & 0xFF);
            case PORT_TX_LEN_LO: return (byte) (txLength & 0xFF);
            case PORT_TX_LEN_HI: return (byte) ((txLength >> 8) & 0xFF);
            case PORT_TX_STATUS: return txStatus;
            case PORT_RX_LEN_LO: return (byte) (rxLength & 0xFF);
            case PORT_RX_LEN_HI: return (byte) ((rxLength >> 8) & 0xFF);
            case PORT_RX_STATUS: return rxStatus;
            case PORT_INT_STATUS: return interruptStatus;
            default: return 0;
        }
    }

    @Override
    public void write(int port, byte value) {
        int offset = port - PORT_MAC_BASE;
        if (offset >= 0 && offset < 6) {
            mac[offset] = value;
            return;
        }
        switch (port) {
            case PORT_TX_DATA:
                if (txLength < txBuffer.length) {
                    txBuffer[txLength++] = value;
                }
                break;
            case PORT_TX_LEN_LO:
                txLength = (txLength & 0xFF00) | (value & 0xFF);
                break;
            case PORT_TX_LEN_HI:
                txLength = (txLength & 0x00FF) | ((value & 0xFF) << 8);
                break;
            case PORT_TX_CMD:
                if (value == TX_CMD_SEND) {
                    txStatus = TX_STATUS_SENDING;
                    interruptStatus |= INT_TX_COMPLETE;
                }
                break;
            case PORT_RX_CMD:
                if (value == RX_CMD_ACK) {
                    rxLength = 0;
                    rxStatus = 0;
                    interruptStatus &= ~INT_RX_PACKET;
                }
                break;
            case PORT_INT_STATUS:
                interruptStatus &= (byte) ~value;
                break;
            default:
                break;
        }
    }

    /** Enqueue received bytes from the PCAP thread (Task 4). */
    public void enqueueRx(byte[] packet, int len) {
        if (len > rxBuffer.length) len = rxBuffer.length;
        System.arraycopy(packet, 0, rxBuffer, 0, len);
        rxLength = len;
        rxStatus = RX_STATUS_PACKET;
        interruptStatus |= INT_RX_PACKET;
    }

    /** Read one byte from the current RX packet (guest reads PORT_RX_DATA). */
    public byte readRxData(int index) {
        if (index < 0 || index >= rxLength) return 0;
        return rxBuffer[index];
    }
```

Remove the old empty `read`/`write` stubs and add the new fields/methods.

- [ ] **Step 4: Run test to verify it passes**

Same command as Step 2. Expected: BUILD SUCCESSFUL, all 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/x86/X86EthernetNic.java Simulator/test/jCPU/x86/X86EthernetNicTest.java
git commit -m "x86: Yukon NIC register read/write logic"
```

---

### Task 3: Wire NIC into CPU.java port registration

**Files:**
- Modify: `Simulator/src/jCPU/x86/CPU.java`

**Interfaces:**
- Consumes: `X86EthernetNic` class (Task 2).
- Produces: `CPU` constructor instantiates `X86EthernetNic(null)` and registers ports `0x300–0x31F`.

- [ ] **Step 1: Add NIC instantiation and port registration**

In `CPU.java` constructor, after the UHCI registration block and before the UEFI firmware block, add:

```java
		// Marvell Yukon 88E8055 NIC at I/O ports 0x300-0x31F
		nic = new X86EthernetNic(null);
		for (int port = 0x300; port <= 0x31F; port++) {
			core.registerPort(port, nic);
		}
```

Add field declaration near the other peripheral fields:
```java
	private X86EthernetNic nic;
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
git commit -m "x86: wire Yukon NIC into CPU port map"
```

---

### Task 4: PCAP backend for host RX/TX

**Files:**
- Modify: `Simulator/src/jCPU/x86/X86EthernetNic.java`

**Interfaces:**
- Consumes: `X86EthernetNic` from Task 2, `enqueueRx(byte[], int)` method.
- Produces: PCAP handle opened in constructor; daemon thread reads host packets and calls `enqueueRx`; TX send injects via pcap when `PORT_TX_CMD` receives `TX_CMD_SEND`.

- [ ] **Step 1: Add pcap imports and handle field**

Add to `X86EthernetNic.java`:

```java
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import java.util.concurrent.ConcurrentLinkedQueue;
```

Add fields:
```java
    private PcapHandle handle;
    private final boolean pcapAvailable;
```

- [ ] **Step 2: Open pcap handle in constructor**

```java
    public X86EthernetNic(String ifaceName) {
        this.ifaceName = ifaceName;
        boolean available = false;
        if (ifaceName != null && !ifaceName.isEmpty()) {
            try {
                handle = Pcaps.openLive(ifaceName, 2048, PromiscuousMode.PROMISCUOUS, 1000);
                available = true;
            } catch (Exception e) {
                System.err.println("NIC: cannot open pcap interface " + ifaceName + ": " + e.getMessage());
                handle = null;
            }
        }
        this.pcapAvailable = available;
        // start RX daemon thread
        Thread rxThread = new Thread(this::rxLoop);
        rxThread.setDaemon(true);
        rxThread.start();
    }
```

- [ ] **Step 3: Implement rxLoop and TX injection**

Add methods:
```java
    private void rxLoop() {
        try {
            while (pcapAvailable && handle != null) {
                Packet packet = handle.getNextPacketEx();
                if (packet == null) continue;
                byte[] raw = packet.getPayload().toByteArray(); // simplified
                enqueueRx(raw, raw.length);
            }
        } catch (Exception e) {
            System.err.println("NIC: pcap RX loop error: " + e.getMessage());
        }
    }
```

In `write` for `PORT_TX_CMD` with `TX_CMD_SEND`, add pcap injection before setting interrupt:
```java
                if (value == TX_CMD_SEND && pcapAvailable && handle != null) {
                    try {
                        handle.sendPacket(Packet.Builder...); // build Ethernet frame from txBuffer
                        txStatus = TX_STATUS_READY;
                    } catch (Exception e) {
                        txStatus = TX_STATUS_ERROR;
                        System.err.println("NIC: TX send error: " + e.getMessage());
                    }
                    interruptStatus |= INT_TX_COMPLETE;
                    break;
                }
```

**Note:** Exact pcap4j API usage depends on the JAR version available. Implementer must check the pcap4j JAR on the classpath before writing the final API calls.

- [ ] **Step 4: Compile-check**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
ant -q compile
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add Simulator/src/jCPU/x86/X86EthernetNic.java
git commit -m "x86: Yukon NIC pcap backend"
```

---

### Task 5: Unit tests for TX/RX cycles + pcap-free verification

**Files:**
- Test: `Simulator/test/jCPU/x86/X86EthernetNicTest.java`

**Interfaces:**
- Consumes: `X86EthernetNic` (Tasks 2-4), `enqueueRx`, `readRxData`.
- Produces: Additional unit tests exercising full TX/RX cycles without pcap.

- [ ] **Step 1: Add TX cycle test**

```java
    @Test
    public void testTransmitCycleSetsInterrupt() {
        X86EthernetNic nic = new X86EthernetNic(null);
        nic.write(0x310, (byte) 0xDE);
        nic.write(0x310, (byte) 0xAD);
        nic.write(0x310, (byte) 0xBE);
        nic.write(0x310, (byte) 0xEF);
        nic.write(0x311, (byte) 0x04);
        nic.write(0x312, (byte) 0x00);
        nic.write(0x313, (byte) 0x01); // TX_CMD_SEND
        assertEquals((byte) (0x02 | 0x04), nic.read(0x314)); // SENDING | ERROR (no pcap)
        assertEquals((byte) 0x02, nic.read(0x330) & 0x02);    // INT_TX_COMPLETE
    }
```

- [ ] **Step 2: Add RX cycle test**

```java
    @Test
    public void testReceiveCycle() {
        X86EthernetNic nic = new X86EthernetNic(null);
        byte[] packet = {0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF, 0x00};
        nic.enqueueRx(packet, packet.length);
        assertEquals((byte) 0x01, nic.read(0x324));           // RX_STATUS_PACKET
        assertEquals((byte) 0x07, nic.read(0x321));           // length low
        assertEquals((byte) 0x00, nic.read(0x322));           // length high
        assertEquals((byte) 0xAA, nic.readRxData(0));
        assertEquals((byte) 0xFF, nic.readRxData(5));
        nic.write(0x323, (byte) 0x01);                        // RX_CMD_ACK
        assertEquals((byte) 0x00, nic.read(0x324));           // status cleared
        assertEquals((byte) 0x00, nic.read(0x330) & 0x01);    // INT_RX_PACKET cleared
    }
```

- [ ] **Step 3: Run tests**

Same command as Task 2 Step 2. Expected: all tests PASS (including prior 5).

- [ ] **Step 4: Commit**

```bash
git add Simulator/test/jCPU/x86/X86EthernetNicTest.java
git commit -m "x86: Yukon NIC unit tests for TX/RX cycles"
```

---

### Task 6: Boot verification + final commit

**Files:**
- Verify: `Simulator/build/classes` (already compiled), jxcore disk image at `/private/var/folders/ws/9cwgq9ps1f9cdq1f4zfby4z40000gn/T/opencode/iso6-mbr.img`

**Interfaces:**
- Consumes: Full simulator build with NIC wired in.
- Produces: Verified boot log showing NIC registers readable from guest; no new failures vs baseline.

- [ ] **Step 1: Run full x86 unit suite**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
java -Xmx2g -cp "build/classes:build/test/classes:/Users/xuyi/Source/Java/nasm/dist/nasm.jar:/Applications/Apache NetBeans.app/Contents/Resources/netbeans/platform/modules/ext/junit-4.13.2.jar:/Applications/Apache NetBeans.app/Contents/Resources/netbeans/platform/modules/ext/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore $(cd build/test/classes && find jCPU/x86 -name '*Test.class' ! -name '*$*' | sed 's#/#.#g;s#\.class$##' | sort)
```
Expected: `Tests run: N, Failures: 1` — sole failure is pre-existing `X86CoreTest.testRegisterConstantsAndAccess`.

- [ ] **Step 2: Boot jxcore with NIC present**

Run from `Simulator/`:
```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
java -Xmx4g -cp "build/classes:build/test/classes" jCPU.x86.boot.UefiBootRunner /private/var/folders/ws/9cwgq9ps1f9cdq1f4zfby4z40000gn/T/opencode/iso6-mbr.img 8000000000 </dev/null > /tmp/iso6-nic-boot.log 2>&1
grep -a -n 'Terminate DomainZero initial thread.\|panic\|PANIC' /tmp/iso6-nic-boot.log
```
Expected: contains `Terminate DomainZero initial thread.`, no panic lines. (Without a host pcap interface, `null` was passed so NIC is present but non-functional — boot behavior unchanged from baseline.)

- [ ] **Step 3: Final review of diff**

```bash
git -c core.whitespace=cr-at-eol diff --check && echo CLEAN
git status --short
```
Expected: only `Simulator/src/jCPU/x86/X86EthernetNic.java`, `Simulator/test/jCPU/x86/X86EthernetNicTest.java`, `Simulator/src/jCPU/x86/CPU.java` changed; generated artifacts untouched.

- [ ] **Step 4: Commit all**

```bash
git add Simulator/src/jCPU/x86/X86EthernetNic.java Simulator/test/jCPU/x86/X86EthernetNicTest.java Simulator/src/jCPU/x86/CPU.java
git commit -m "x86: add Marvell Yukon 88E8055 NIC emulation (minimal)"
```
