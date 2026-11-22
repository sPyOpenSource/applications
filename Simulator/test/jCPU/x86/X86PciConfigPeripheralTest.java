package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86PciConfigPeripheralTest
{
    @Test
    public void testConfigAddressReadWriteRoundTrip() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
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
    public void testConfigAddressLowBitsMasked() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        pci.write(0xCF8, (byte) 0x01);
        pci.write(0xCF9, (byte) 0x00);
        pci.write(0xCFA, (byte) 0x00);
        pci.write(0xCFB, (byte) 0xFF);
        assertEquals(0xFF000000, pci.getConfigAddress());
        assertEquals((byte) 0x00, pci.read(0xCF8));
    }

    @Test
    public void testConfigDataReadsFakeDevVendorAtDevice0() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        pci.write(0xCF8, (byte) 0x00);
        pci.write(0xCF9, (byte) 0x00);
        pci.write(0xCFA, (byte) 0x00);
        pci.write(0xCFB, (byte) 0x80);
        assertEquals((byte) 0xAB, pci.read(0xCFC));
        assertEquals((byte) 0x11, pci.read(0xCFD));
        assertEquals((byte) 0x53, pci.read(0xCFE));
        assertEquals((byte) 0x43, pci.read(0xCFF));
    }

    @Test
    public void testConfigDataNoDeviceElsewhere() {
        X86PciConfigPeripheral pci = new X86PciConfigPeripheral();
        assertEquals(0xFFFFFFFF, readConfigDataDword(pci));
        pci.write(0xCF8, (byte) 0x00);
        pci.write(0xCF9, (byte) 0x08);
        pci.write(0xCFA, (byte) 0x00);
        pci.write(0xCFB, (byte) 0x80);
        assertEquals(0xFFFFFFFF, readConfigDataDword(pci));
        pci.write(0xCF8, (byte) 0x04);
        pci.write(0xCF9, (byte) 0x00);
        assertEquals(0xFFFFFFFF, readConfigDataDword(pci));
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

    private static int readConfigDataDword(X86PciConfigPeripheral pci) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= (pci.read(0xCFC + i) & 0xFF) << (i * 8);
        }
        return value;
    }
}
