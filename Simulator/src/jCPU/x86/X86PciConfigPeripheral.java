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
    public static final int PORT_CONFIG_ADDRESS = 0xCF8; // 0xCF8-0xCFB
    public static final int PORT_CONFIG_DATA    = 0xCFC; // 0xCFC-0xCFF

    private static final int CONFIG_ENABLE = 0x80000000;
    private static final int NO_DEVICE     = 0xFFFFFFFF;
    public static final int FAKE_VENDOR_ID  = 0x11AB;
    public static final int FAKE_DEVICE_ID  = 0x4353;
    public static final int FAKE_DEVVEND    = (FAKE_DEVICE_ID << 16) | FAKE_VENDOR_ID; // 0x435311AB

    private final byte[] configAddress = new byte[4];

    public X86PciConfigPeripheral() {
    }

    @Override
    public byte read(int port) {
        int offset = port - PORT_CONFIG_ADDRESS;
        if (offset >= 0 && offset < 4) {
            if (offset == 0) {
                return (byte) (configAddress[0] & 0xFC); // bits 1:0 hardwired to 0
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
        // CONFIG_DATA writes are ignored (no writable devices).
    }

    /** Return the assembled 32-bit CONFIG_ADDRESS value, with bits 1:0 masked to 0. */
    public int getConfigAddress() {
        return (configAddress[0] & 0xFC)
             | ((configAddress[1] & 0xFF) << 8)
             | ((configAddress[2] & 0xFF) << 16)
             | ((configAddress[3] & 0xFF) << 24);
    }

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
