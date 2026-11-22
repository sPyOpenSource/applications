# PCI Configuration Space Ports Emulation — Design

## Goal

Make the emulator's PCI configuration space ports (`0xCF8–0xCFC`) behave like a real PCI host bridge so guest software (jxcore's `PCIGod`) can probe the PCI bus without throwing `Error("no PCI Bus detected")`.

## Problem

jxcore's `PCIGod` (`/Users/xuyi/Source/OS/x86OS/jx/libs/pci_pc/jx/devices/pci/PCIGod.java`) probes PCI by:

1. Reading port `0xCF8` (CONFIG_ADDRESS) — expects bit 31 = 0
2. Writing `0x80000000` to port `0xCF8` (enable bit + register 0), then writing `0` to byte 3 (`0xCFB`) — clears the enable bit
3. Writing `0xff000001` (CONF1_ENABLE_CHK1) to `0xCF8` and reading back — expects `(value & 0x80000001) == 0x80000000` (CONF1 check)
4. If the CONF1 check passes, `lookForDevices()` reads the vendor/device ID (register 0) of each device at bus 0, functions 0; it needs at least one ID **≠ `0xFFFFFFFF`** (INVALID_ID)

The emulator registers no peripheral at ports `0xCF8–0xCFC`, so every read returns `0` and every write is silently dropped. Both the CONF1 check and `lookForDevices()` fail, `probePCI()` returns false, and the constructor throws `Error("no PCI Bus detected")`.

## Decisions (corrected)

- **New peripheral class:** `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java` implementing `jCPU.Peripheral` (`read(int)` / `write(int, byte)`). Matches existing peripheral pattern (VGA, UART, IDE are all separate classes).
- **Port range:** `0xCF8–0xCFF` (CONFIG_ADDRESS at `0xCF8–0xCFB`, CONFIG_DATA at `0xCFC–0xCFF`).
- **Byte-oriented:** Multi-byte `inl`/`outl` are already decomposed into consecutive byte reads/writes by `X86Amd64.portIn()`/`portOut()` and the executors. Config address is stored as 4 little-endian bytes at `0xCF8–0xCFB`.
- **CONFIG_ADDRESS reads mask bits 1:0 to zero:** Real PCI hardware forces the low 2 bits of the config address register to 0 (config cycles are 4-byte aligned). This is essential: PCIGod writes `0xff000001` and expects to read back `0xff000000`, so `(readback & 0x80000001) == 0x80000000`. Without masking, the CONF1 check fails.
- **CONFIG_DATA returns a minimal fake device:** When CONFIG_ADDRESS bit 31 (enable) is set and it selects bus 0 / device 0 / function 0 / register 0 (the vendor/device ID register), return the Marvell Yukon ID `0x435311AB` (device `0x4353` << 16 | vendor `0x11AB`). This makes `lookForDevices()` succeed. For all other addresses/registers, return `0xFFFFFFFF` (standard "no device" value). When bit 31 is clear, also return `0xFFFFFFFF`.
- **Why a fake ID is necessary:** Returning `0xFFFFFFFF` everywhere makes `lookForDevices()` fail and the boot error persists. A single fake device is the minimum needed for PCI enumeration to succeed.
- **Fake device stays invisible to drivers:** For the fake device, all other config registers (header type, class code, etc.) return `0xFFFFFFFF`. `PCIGod.scanBus()` reads class code `0xFFFFFF` → mainclass `0xFF` → `continue`, so no `PCIDevice` is added to the device list. No guest driver claims the device.
- **No new IRQ/PIC system:** Config-space access only; no device interrupts or DMA.
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
 * is decomposed into consecutive byte reads/writes by the executors, so this
 * peripheral only needs byte-level read/write.
 *
 * CONFIG_ADDRESS is a read/write 32-bit register stored at ports 0xCF8-0xCFB
 * (little-endian).  Bits 1:0 are forced to 0 on read (real PCI hardware
 * requires 4-byte-aligned config cycles).
 *
 * CONFIG_DATA reads are decoded from CONFIG_ADDRESS.  When bit 31 (enable)
 * is set and the address selects bus 0 / device 0 / function 0 / register 0,
 * a minimal fake device ID (Marvell 0x11AB / Yukon 88E8055 0x4353) is
 * returned so guest PCI enumeration finds a device.  All other addresses
 * return 0xFFFFFFFF ("no device").  The fake device's other registers are
 * also 0xFFFFFFFF so no guest driver claims it.
 */
public class X86PciConfigPeripheral implements Peripheral
{
    public static final int PORT_CONFIG_ADDRESS = 0xCF8; // 0xCF8-0xCFB (4 bytes)
    public static final int PORT_CONFIG_DATA    = 0xCFC; // 0xCFC-0xCFF (4 bytes)

    /* Fake device: Marvell vendor 0x11AB, Yukon 88E8055 device 0x4353.
     * Value at config register 0 is (device << 16) | vendor. */
    public static final int FAKE_VENDOR_ID  = 0x11AB;
    public static final int FAKE_DEVICE_ID  = 0x4353;
    public static final int FAKE_DEVVEND    = (FAKE_DEVICE_ID << 16) | FAKE_VENDOR_ID; // 0x435311AB

    private static final int CONFIG_ENABLE = 0x80000000;
    private static final int NO_DEVICE     = 0xFFFFFFFF;

    private final byte[] configAddress = new byte[4]; // little-endian 32-bit value

    public X86PciConfigPeripheral() {
    }

    @Override
    public byte read(int port) {
        int offset = port - PORT_CONFIG_ADDRESS;
        if (offset >= 0 && offset < 4) {
            // Bits 1:0 are hardwired to 0 on real PCI hardware.
            if (offset == 0) {
                return (byte) (configAddress[0] & 0xFC);
            }
            return configAddress[offset];
        }
        offset = port - PORT_CONFIG_DATA;
        if (offset >= 0 && offset < 4) {
            int value = readConfigData();
            return (byte) (value >>> (offset * 8));
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
            // CONFIG_DATA writes target a device; with no writable devices, ignore.
            return;
        }
    }

    /** Return the assembled 32-bit CONFIG_ADDRESS value, with bits 1:0 masked to 0. */
    public int getConfigAddress() {
        return (configAddress[0] & 0xFC)
             | ((configAddress[1] & 0xFF) << 8)
             | ((configAddress[2] & 0xFF) << 16)
             | ((configAddress[3] & 0xFF) << 24);
    }

    /** Decode CONFIG_ADDRESS and return the 32-bit config data value. */
    private int readConfigData() {
        int address = getConfigAddress();
        if ((address & CONFIG_ENABLE) == 0) {
            return NO_DEVICE;
        }
        int bus      = (address >>> 16) & 0xFF;
        int device   = (address >>> 11) & 0x1F;
        int function = (address >>> 8)  & 0x07;
        int register = address & 0xFC;
        if (bus == 0 && device == 0 && function == 0 && register == 0) {
            return FAKE_DEVVEND;
        }
        return NO_DEVICE;
    }
}
```

### Modified file: `Simulator/src/jCPU/x86/CPU.java`

In the constructor, after the Yukon NIC registration block and before the UEFI firmware block:

```java
	// PCI configuration space ports (CONFIG_ADDRESS 0xCF8, CONFIG_DATA 0xCFC)
	pciConfig = new X86PciConfigPeripheral();
	for (int port = 0xCF8; port <= 0xCFF; port++) {
		core.registerPort(port, pciConfig);
	}
```

Add field `private X86PciConfigPeripheral pciConfig;` near the other peripheral fields.

## Data flow

**Guest writes CONFIG_ADDRESS:** four bytes to `0xCF8–0xCFB` (little-endian, via `outl_p`). Peripheral stores bytes. Reads back return stored bytes with bits 1:0 masked to 0.

**CONF1 probe:** PCIGod writes `0xff000001`, reads back `0xff000000`, checks `(x & 0x80000001) == 0x80000000` → passes.

**Device detection:** PCIGod writes `0x80000000` (bus0/dev0/func0/reg0), reads CONFIG_DATA → `0x435311AB` ≠ INVALID_ID → `lookForDevices()` returns true → `probePCI()` succeeds.

**scanBus:** reads the fake device's class code (`0xFFFFFF`) → mainclass `0xFF` → `continue`; no device added. Boot proceeds without error.

## Error handling

| Condition | Behavior |
|-----------|----------|
| Unknown port read | Return 0 |
| Unknown port write | Ignore |
| CONFIG_DATA read with enable bit clear | Return `0xFFFFFFFF` |
| CONFIG_DATA read for any device except fake | Return `0xFFFFFFFF` |
| CONFIG_DATA write | Silently ignored |

No exceptions are thrown from the peripheral.

## Testing

**Unit test: `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java`**
- `testConfigAddressReadWriteRoundTrip` — write 4 bytes to 0xCF8–0xCFB, read back, verify round-trip
- `testConfigAddressLowBitsMasked` — write 0xff000001, verify getConfigAddress() == 0xff000000
- `testConfigDataReadsFakeDevVendorAtDevice0` — address bus0/dev0/func0/reg0 with enable → `0x435311AB`
- `testConfigDataNoDeviceElsewhere` — enable bit clear, or other device/register → `0xFFFFFFFF`
- `testConfigDataWriteIsIgnored`
- `testUnknownPortsReturnZero`
- `testPcigodProbeSequence` — simulate full probe: CONF1 check + lookForDevices

**Boot test:** Run jxcore disk image with `UefiBootRunner`, verify "Success reading PCI configuration port" appears and no `Error`/`no PCI Bus detected`.

## Out of scope

- Additional PCI devices behind config space (only one minimal fake device)
- PCI interrupts, MSI, or PIC integration
- CONFIG_DATA writes having any effect (no writable devices)
- Memory-mapped PCI configuration (MCFG)
- Exposing the Yukon NIC as a PCI device (it remains at I/O ports 0x300–0x330)

## Files

- Create: `Simulator/src/jCPU/x86/X86PciConfigPeripheral.java`
- Create: `Simulator/test/jCPU/x86/X86PciConfigPeripheralTest.java`
- Modify: `Simulator/src/jCPU/x86/CPU.java`
