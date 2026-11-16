package jCPU.arm;

import static org.junit.Assert.assertEquals;

import jCPU.Peripheral;
import org.junit.Test;

/**
 * Verifies that MMIO peripherals registered over an address range receive
 * accesses anywhere inside that range, with the register offset passed through.
 */
public class VirtualMemorySpaceRangeTest {

    private static final class RecordingPeripheral implements Peripheral {
        final int[] reads = new int[256];
        final int[] writes = new int[256];
        int readCount, writeCount;
        byte nextRead = 0x5A;

        @Override
        public byte read(int offset) {
            reads[readCount++] = offset;
            return nextRead;
        }

        @Override
        public void write(int offset, byte value) {
            writes[writeCount++] = offset;
        }
    }

    @Test
    public void testWordAccessDispatchesWithRegisterOffset() throws Exception {
        VirtualMemorySpace vm = new VirtualMemorySpace(new PhysicalMemorySpace(), null);
        RecordingPeripheral p = new RecordingPeripheral();
        vm.registerPeripheral(0x3F200000, 0x1000, p);

        vm.writeInt(0x3F200018, 0x12345678, false, false);
        assertEquals(4, p.writeCount);
        assertEquals(0x18, p.writes[0]);
        assertEquals(0x19, p.writes[1]);
        assertEquals(0x1A, p.writes[2]);
        assertEquals(0x1B, p.writes[3]);

        int value = vm.readInt(0x3F200024, false, false);
        assertEquals(4, p.readCount);
        assertEquals(0x24, p.reads[0]);
        assertEquals(0x27, p.reads[3]);
        assertEquals(0x5A5A5A5A, value);
    }

    @Test
    public void testByteAccessUsesOffset() throws Exception {
        VirtualMemorySpace vm = new VirtualMemorySpace(new PhysicalMemorySpace(), null);
        RecordingPeripheral p = new RecordingPeripheral();
        vm.registerPeripheral(0x3F200000, 0x1000, p);

        vm.writeByte(0x3F200025, (byte) 0x77);
        assertEquals(1, p.writeCount);
        assertEquals(0x25, p.writes[0]);

        vm.readByte(0x3F200026);
        assertEquals(1, p.readCount);
        assertEquals(0x26, p.reads[0]);
    }

    @Test
    public void testExactRegistrationStillWins() throws Exception {
        VirtualMemorySpace vm = new VirtualMemorySpace(new PhysicalMemorySpace(), null);
        RecordingPeripheral exact = new RecordingPeripheral();
        RecordingPeripheral range = new RecordingPeripheral();
        vm.registerPeripheral(0x3F200000, exact);
        vm.registerPeripheral(0x3F200000, 0x1000, range);

        vm.writeByte(0x3F200000, (byte) 1);
        assertEquals(1, exact.writeCount);
        assertEquals(0, range.writeCount);
    }
}
