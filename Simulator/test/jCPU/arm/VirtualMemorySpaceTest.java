package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.*;

public class VirtualMemorySpaceTest {
    private PhysicalMemorySpace physicalMem;
    private VirtualMemorySpace virtualMem;

    @Before
    public void setUp() {
        physicalMem = new PhysicalMemorySpace();
        virtualMem = new VirtualMemorySpace(physicalMem, null);
    }

    private static class MockPeripheral implements Peripheral {
        byte readVal = 0;
        byte writeVal = 0;
        boolean readCalled = false;
        boolean writeCalled = false;

        @Override
        public byte read(int offset) {
            readCalled = true;
            return readVal;
        }

        @Override
        public void write(int offset, byte value) {
            writeCalled = true;
            writeVal = value;
        }
    }

    @Test
    public void testMmioRead() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        mock.readVal = 0x42;
        int addr = 0x1000;
        
        virtualMem.registerPeripheral(addr, mock);
        
        byte result = virtualMem.readByte(addr);
        
        assertTrue("Peripheral read should have been called", mock.readCalled);
        assertEquals(0x42, result);
    }

    @Test
    public void testMmioWrite() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x2000;
        
        virtualMem.registerPeripheral(addr, mock);
        
        virtualMem.writeByte(addr, (byte) 0x77);
        
        assertTrue("Peripheral write should have been called", mock.writeCalled);
        assertEquals(0x77, mock.writeVal);
    }

    @Test
    public void testPhysicalMemoryRead() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int mmioAddr = 0x1000;
        int physAddr = 0x3000;
        
        virtualMem.registerPeripheral(mmioAddr, mock);
        
        // This should throw BusErrorException because nothing is mapped in physical memory
        try {
            virtualMem.readByte(physAddr);
            fail("Should have thrown BusErrorException");
        } catch (BusErrorException e) {
            // Expected
        }
        
        assertFalse("Peripheral read should NOT have been called", mock.readCalled);
    }
}
