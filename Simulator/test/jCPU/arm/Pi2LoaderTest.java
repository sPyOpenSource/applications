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

    @Test
    public void testLoadImageLargerThan1MbMapsWholeImage() throws Exception {
        byte[] image = new byte[0x567700]; // size of the kernel7.img from the bug report
        for (int i = 0; i < image.length; i++) image[i] = (byte) (i * 31 + 7);

        CPU cpu = Pi2Loader.loadImage(image);
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

        int addr = 0x5676c0;
        for (int i = 0; i < 8; i++)
            assertEquals("byte at 0x" + Integer.toHexString(addr + i), image[addr + i],
                    vm.read(Pi2MemoryMap.RAM_BASE + addr + i));

        // RAM must also have some headroom beyond the image for kernel data/stack
        int scratch = image.length + 0x100;
        vm.writeInt(Pi2MemoryMap.RAM_BASE + scratch, 0x12345678, false, false);
        assertEquals(0x12345678, vm.readInt(Pi2MemoryMap.RAM_BASE + scratch, false, false));
    }

    @Test
    public void testRamCoversKernelRuntimeWritesFarBeyondImage() throws Exception {
        CPU cpu = Pi2Loader.loadImage(new byte[] {1, 2, 3, 4});
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

        int addr = 0x1314420; // address from the kernel boot bug report
        vm.writeInt(addr, (int) 0xDEADBEEFL, false, false);

        assertEquals((int) 0xDEADBEEFL, vm.readInt(addr, false, false));
    }

    @Test
    public void testKernelVirtualAddressAliasesPhysicalRam() throws Exception {
        byte[] image = new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        CPU cpu = Pi2Loader.loadImage(image);
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

        // 2G/2G split: kernel virtual 0x80000000 aliases physical 0x0
        assertEquals((byte) 0xDE, vm.read(0x80000000));

        // A write through the virtual alias must be visible physically and vice versa
        vm.writeInt(0x80C00000, 0xCAFEBABE, false, false);
        assertEquals(0xCAFEBABE, vm.readInt(0x00C00000, false, false));
        vm.writeInt(0x9000, 0x12345678, false, false);
        assertEquals(0x12345678, vm.readInt(0x80009000, false, false));

        // 3G/1G split alias must also work
        assertEquals((byte) 0xDE, vm.read(0xC0000000));
    }

    @Test
    public void testHighVectorPageIsMappedAndWritable() throws Exception {
        CPU cpu = Pi2Loader.loadImage(new byte[] {1, 2, 3, 4});
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

        int vectorAddr = 0xFFFF000C; // prefetch-abort vector with V set
        vm.writeInt(vectorAddr, 0xE59FF018, false, false); // ldr pc, [pc, #0x18]
        assertEquals(0xE59FF018, vm.readInt(vectorAddr, false, false));
    }
}
