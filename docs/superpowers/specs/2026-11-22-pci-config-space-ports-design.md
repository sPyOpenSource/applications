# PCI Configuration Space Ports Emulation — Design

## Goal

Make the emulator's PCI configuration space ports (`0xCF8–0xCFC`) behave like a real PCI host bridge so guest software (e.g., jxcore's `PCIGod`) can probe the PCI bus without throwing `Error("no PCI Bus detected")`.

## Problem

jxcore's `PCIGod` (`/Users/xuyi/Source/OS/x86OS/jx/libs/pci_pc/jx/devices/pci/PCIGod.java`) probes PCI by:

1. Reading port `0xCF8` (CONFIG_ADDRESS) — expects bit 31 = 0
2. Writing `0x80000000` to port `0xCF8` (enable bit + register 0)
3. Writing `0x00` to port `0xCFB` (byte 3 of CONFIG_ADDRESS)
4. Reading port `0xCF8` back — expects the value just written (PCI mode 1 detected)

The emulator registers no peripheral at ports `0xCF8–0xCFC`, so every read returns `0` and every write is silently dropped. PCIGod reads `0` back instead of its written value, concludes PCI mode 1 is unsupported, falls through its CONF1 fallback (also returns `0`), and throws `Error("no PCI Bus detected")`.

## Decisions

- **New peripheral class:** `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java` implementing `jCPU.Peripheral` (`read(int)` / `write(int, byte)`). Matches existing peripheral pattern (VGA, UART, IDE are all separate classes).
- **Port range:** `0xCF8–0xCFC` (CONFIG_ADDRESS at 0xCF8, CONFIG_DATA at 0xCFC; 0xCF9–0xCFB are intermediate bytes of the 32-bit CONFIG_ADDRESS).
- **Byte-oriented:** Multi-byte `inl`/`outl` are already decomposed into consecutive byte reads/writes by `X86Amd64.portIn()`/`portOut()` and the executors. The peripheral stores 4 bytes at 0xCF8–0xCFB representing the 32-bit config address.
- **CONFIG_DATA reads return `0xFFFFFFFF`:** Standard PCI "no device present" value. PCIGod's `lookForDevices()` checks `id != INVALID_ID (0xFFFFFFFF)`, so this means "no devices found" and PCIGod's probe completes successfully without enumerating fake devices.
- **No new IRQ/PIC system:** This is config-space access only; no device interrupts or DMA.
- **Integration:** Instantiated and port-registered in `CPU.java` constructor, alongside VGA, UART, IDE, UHCI, and the Yukon NIC.

## Component changes

### New file: `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java`

```java
package jCPU.x86;

import jCPU.Peripheral;

/**
 * PCI configuration space port emulation.
 *
 * Provides the CONFIG_ADDRESS (0xCF8) and CONFIG_DATA (0xCFC) ports used by
 * guest software to enumerate the PCI bus.  Multi-byte (32-bit) port access
 * is decomposed into consecutive byte reads/writes by the executors
 * (X86Amd64.portIn/portOut, X86BinaryExecutor, i8086), so this peripheral
 * only needs byte-level read/write.
 *
 * CONFIG_ADDRESS is a read/write 32-bit register stored at ports 0xCF8-0xCFB
 * (little-endian).  CONFIG_DATA reads return 0xFFFFFFFF (no device present),
 * which is the standard PCI value for an unpopulated slot.
 */
public class X86PciConfigPeripheral implements Peripheral
{
    public static final int PORT_CONFIG_ADDRESS = 0xCF8; // 0xCF8-0xCFB (4 bytes)
    public static final int PORT_CONFIG_DATA    = 0xCFC; // 0xCFC-0xCFF (4 bytes)

    private final byte[] configAddress = new byte[4]; // little-endian 32-bit value

    public X86PciConfigPeripheral() {
    }

    @Override
    public byte read(int port) {
        int offset = port - PORT_CONFIG_ADDRESS;
        if (offset >= 0 && offset < 4) {
            return configAddress[offset];
        }
        offset = port - PORT_CONFIG_DATA;
        if (offset >= 0 && offset < 4) {
            return (byte) 0xFF; // no device: all-1s
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
            // CONFIG_DATA writes target the device selected by CONFIG_ADDRESS;
            // with no devices present, drops are silent.
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
}
```

### Modified file: `Simulator/src/jCPU/x86/CPU.java`

In the constructor, after the Yukon NIC registration block (ports `0x300–0x31F`) and before the UEFI firmware block:

```java
	// PCI configuration space ports (CONFIG_ADDRESS 0xCF8, CONFIG_DATA 0xCFC)
	pciConfig = new X86PciConfigPeripheral();
	for (int port = 0xCF8; port <= 0xCFF; port++) {
		core.registerPort(port, pciConfig);
	}
```

Add field `private X86PciConfigPeripheral pciConfig;` near the other peripheral fields.

## Data flow

**Guest writes CONFIG_ADDRESS:**
1. Guest writes 4 bytes to ports `0xCF8–0xCFB` (little-endian, via `outl_p`)
2. Peripheral stores bytes in `configAddress[]`
3. Guest reads ports `0xCF8–0xCFB` back → returns stored bytes → PCIGod sees its written value → PCI mode 1 detected

**Guest reads CONFIG_DATA:**
1. Guest reads 4 bytes from ports `0xCFC–0xCFF` → each returns `0xFF` → assembled value `0xFFFFFFFF`
2. PCIGod's `lookForDevices()` sees `INVALID_ID` → no devices found → probe completes normally

## Error handling

| Condition | Behavior |
|-----------|----------|
| Unknown port read | Return 0 |
| Unknown port write | Ignore |
| CONFIG_DATA write to nonexistent device | Silently dropped (no devices present) |

All errors are handled gracefully; no exceptions are thrown from the peripheral.

## Testing

**Unit test: `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java`**
- `testConfigAddressReadWrite` — write 4 bytes to 0xCF8–0xCFB, read back, verify round-trip
- `testConfigDataReadsAllOnes` — read 0xCFC–0xCFF, verify each byte is 0xFF
- `testProbePciSequence` — simulate PCIGod's probe: read 0xCF8 (0), write 0x80000000, write 0x00 to 0xCFB, read 0xCF8 (expect 0x80000000 assembled)
- `testUnknownPorts` — reads/writes outside 0xCF8–0xCFF return 0 / no-op

**Boot test:** Run jxcore disk image with `UefiBootRunner`, verify PCIGod's "Success reading PCI configuration port" appears and no "no PCI Bus detected" error.

## Out of scope

- Actual PCI devices behind the config space (no device enumeration results)
- PCI interrupts, MSI, or PIC integration
- CONFIG_DATA writes having any effect (no devices present)
- Memory-mapped PCI configuration (MCFG)

## Files

- Create: `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java`
- Create: `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java`
- Modify: `Simulator/src/jCPU/x86/CPU.java`