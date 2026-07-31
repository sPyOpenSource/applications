package jCPU.arm;

import jCPU.Peripheral;
import static org.junit.Assert.*;
import org.junit.*;

public class VirtualMemorySpaceTest {
    private PhysicalMemorySpace physicalMem;
    private VirtualMemorySpace virtualMem;

    @Before
    public void setUp() {
        physicalMem = new PhysicalMemorySpace();
        physicalMem.mapRegion(0x10000, new ByteArrayRegion(0x10000));
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
        assertEquals((Byte) (byte)0x77, mock.writeVals.get(0));
    }

    @Test
    public void testMmioIntWrite() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x4000;
        virtualMem.registerPeripheral(addr, mock);
        
        // Little endian write of 0x12345678
        virtualMem.writeInt(addr, 0x12345678, false, false);
        
        assertTrue("Peripheral write should have been called", mock.writeCalled);
        assertEquals((Byte) (byte)0x78, mock.writeVals.get(0));
        assertEquals((Byte) (byte)0x56, mock.writeVals.get(1));
        assertEquals((Byte) (byte)0x34, mock.writeVals.get(2));
        assertEquals((Byte) (byte)0x12, mock.writeVals.get(3));
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
    public void testMmioReadShort() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x5000;
        virtualMem.registerPeripheral(addr, mock);
        
        virtualMem.readShort(addr, false, false);
        
        assertEquals(addr, virtualMem.getLastAccessAddress());
        assertEquals(1, virtualMem.getLastAccessWidth());
        assertFalse(virtualMem.getLastAccessWasStore());
    }

    @Test
    public void testMmioWriteShort() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x6000;
        virtualMem.registerPeripheral(addr, mock);
        
        virtualMem.writeShort(addr, (short)0x1234, false, false);
        
        assertEquals(addr, virtualMem.getLastAccessAddress());
        assertEquals(1, virtualMem.getLastAccessWidth());
        assertTrue(virtualMem.getLastAccessWasStore());
    }

    @Test
    public void testMmioReadInt() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x7000;
        virtualMem.registerPeripheral(addr, mock);
        
        virtualMem.readInt(addr, false, false);
        
        assertEquals(addr, virtualMem.getLastAccessAddress());
        assertEquals(2, virtualMem.getLastAccessWidth());
        assertFalse(virtualMem.getLastAccessWasStore());
    }

    @Test
    public void testMmioWriteInt() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x8000;
        virtualMem.registerPeripheral(addr, mock);
        
        virtualMem.writeInt(addr, 0x12345678, false, false);
        
        assertEquals(addr, virtualMem.getLastAccessAddress());
        assertEquals(2, virtualMem.getLastAccessWidth());
        assertTrue(virtualMem.getLastAccessWasStore());
    }

    @Test
    public void testMmioReadLong() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0x9000;
        virtualMem.registerPeripheral(addr, mock);
        
        virtualMem.readLong(addr, false, false);
        
        assertEquals(addr, virtualMem.getLastAccessAddress());
        assertEquals(3, virtualMem.getLastAccessWidth());
        assertFalse(virtualMem.getLastAccessWasStore());
    }

    @Test
    public void testMmioWriteLong() throws Exception {
        MockPeripheral mock = new MockPeripheral();
        int addr = 0xA000;
        virtualMem.registerPeripheral(addr, mock);
        
        virtualMem.writeLong(addr, 0x123456789ABCDEFL, false, false);
        
        assertEquals(addr, virtualMem.getLastAccessAddress());
        assertEquals(3, virtualMem.getLastAccessWidth());
        assertTrue(virtualMem.getLastAccessWasStore());
    }

    @Test
    public void testPhysicalMemoryReadLongAccessTracking() throws Exception {
        int addr = 0x11000; // Not in MMIO map
        
        virtualMem.readLong(addr, false, false);
        
        assertEquals("Last access address should be the start of the long read", addr, virtualMem.getLastAccessAddress());
        assertEquals("Last access width should be 3 for long", 3, virtualMem.getLastAccessWidth());
        assertFalse(virtualMem.getLastAccessWasStore());
    }

    @Test
    public void testContainsKey() throws Exception {
        int registeredAddr = 0xB000;
        int unregisteredAddr = 0xC000;

        assertFalse("Address should not be registered yet", virtualMem.containsKey(registeredAddr));

        virtualMem.registerPeripheral(registeredAddr, new MockPeripheral());

        assertTrue("Address should be registered now", virtualMem.containsKey(registeredAddr));
        assertFalse("Other address should not be registered", virtualMem.containsKey(unregisteredAddr));
    }

    @Test
    public void testPhysicalMemoryWriteLongAccessTracking() throws Exception {
        int addr = 0x12000; // Not in MMIO map
        
        virtualMem.writeLong(addr, 0x123456789ABCDEFL, false, false);
        
        assertEquals("Last access address should be the start of the long write", addr, virtualMem.getLastAccessAddress());
        assertEquals("Last access width should be 3 for long", 3, virtualMem.getLastAccessWidth());
        assertTrue(virtualMem.getLastAccessWasStore());
    }
}
