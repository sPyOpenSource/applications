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
}
