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
