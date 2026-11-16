package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class Pi2LoaderTest {
    @Test
    public void testLoadImagePopulatesRamAtAddressZero() throws Exception {
        byte[] image = new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        CPU cpu = Pi2Loader.loadImage(image);
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

        assertEquals((byte) 0xDE, vm.read(Pi2MemoryMap.RAM_BASE + 0));
        assertEquals((byte) 0xAD, vm.read(Pi2MemoryMap.RAM_BASE + 1));
        assertEquals((byte) 0xBE, vm.read(Pi2MemoryMap.RAM_BASE + 2));
        assertEquals((byte) 0xEF, vm.read(Pi2MemoryMap.RAM_BASE + 3));
    }
}
