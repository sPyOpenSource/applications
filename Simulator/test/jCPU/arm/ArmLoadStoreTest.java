package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class ArmLoadStoreTest {
    @Test
    public void testStrThenLdr() throws Exception {
        CPU cpu = new CPU();
        cpu.reset(); // sets mode (needed by isPrivileged in load/store)
        cpu.getMemorySpace().mapRegion(Pi2MemoryMap.RAM_BASE, new ByteArrayRegion(0x10000));
        cpu.executeARM(0xE3A00010); // MOV R0, #16
        cpu.writeRegister(1, 0xAA556677);
        cpu.executeARM(0xE5801000); // STR R1, [R0]
        cpu.executeARM(0xE5902000); // LDR R2, [R0]
        assertEquals(0xAA556677, cpu.r(2));
    }

    @Test
    public void testStrbThenLdrb() throws Exception {
        CPU cpu = new CPU();
        cpu.reset(); // sets mode (needed by isPrivileged in load/store)
        cpu.getMemorySpace().mapRegion(Pi2MemoryMap.RAM_BASE, new ByteArrayRegion(0x10000));
        cpu.executeARM(0xE3A00020); // MOV R0, #32
        cpu.writeRegister(1, 0x41); // 'A'
        cpu.executeARM(0xE5C01000); // STRB R1, [R0]
        cpu.executeARM(0xE5D02000); // LDRB R2, [R0]
        assertEquals((byte) 'A', (byte) cpu.r(2));
    }

    @Test
    public void testStrdRegisterForm() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.getMemorySpace().mapRegion(Pi2MemoryMap.RAM_BASE, new ByteArrayRegion(0x10000));
        // STRD R0, [R3, R12] : store R0 at [R3+R12], R1 at [R3+R12+4]
        cpu.writeRegister(0, 0x11111111);
        cpu.writeRegister(1, 0x22222222);
        cpu.writeRegister(3, 0x1000);
        cpu.writeRegister(12, 0);
        cpu.executeARM(0xE18300FC); // STRD (register, A8-688)
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();
        assertEquals(0x11111111, vm.readInt(0x1000, false, false));
        assertEquals(0x22222222, vm.readInt(0x1004, false, false));
    }

    @Test
    public void testStrdRegisterFormWithOffset() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.getMemorySpace().mapRegion(Pi2MemoryMap.RAM_BASE, new ByteArrayRegion(0x10000));
        cpu.writeRegister(0, 0xAAAAAAAA);
        cpu.writeRegister(1, 0xBBBBBBBB);
        cpu.writeRegister(3, 0x2000);
        cpu.writeRegister(12, 8); // offset 8
        cpu.executeARM(0xE18300FC); // STRD R0, [R3, R12]
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();
        assertEquals(0xAAAAAAAA, vm.readInt(0x2008, false, false));
        assertEquals(0xBBBBBBBB, vm.readInt(0x200C, false, false));
    }
}
