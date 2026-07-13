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
        java.util.Map<Integer, Byte> writeVals = new java.util.HashMap<>();
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
            writeVals.put(offset, value);
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
        assertEquals((byte)0x77, mock.writeVals.get(0));
    }

    @Test
    public void testMmioIntWrite() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x4000;
        virtualMem.registerPeripheral(addr, mock);
        
        // Little endian write of 0x12345678
        virtualMem.writeInt(addr, 0x12345678, false, false);
        
        assertTrue("Peripheral write should have been called", mock.writeCalled);
        assertEquals((byte)0x78, mock.writeVals.get(0));
        assertEquals((byte)0x56, mock.writeVals.get(1));
        assertEquals((byte)0x34, mock.writeVals.get(2));
        assertEquals((byte)0x12, mock.writeVals.get(3));
    }

    @Test
    public void testConcurrentMmioRegistration() throws InterruptedException {
        final int threadCount = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int addr = i * 0x1000;
            executor.submit(() -> {
                try {
                    virtualMem.registerPeripheral(addr, new MockPeripheral());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        // If ConcurrentHashMap is not used, this might fail if we were reading/writing simultaneously, 
        // but here we just register. To truly test thread-safety of the map, we should have concurrent read/write.
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
