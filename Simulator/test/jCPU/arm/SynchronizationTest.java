package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

/** Exclusive load/store and SWP synchronization primitives used by Linux atomics. */
public class SynchronizationTest {

    private static CPU cpu() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.getMemorySpace().mapRegion(Pi2MemoryMap.RAM_BASE, new ByteArrayRegion(0x10000));
        return cpu;
    }

    @Test
    public void testLdrexStrexWord() throws Exception {
        CPU cpu = cpu();
        cpu.writeRegister(0, 0x10);            // base address
        cpu.writeRegister(1, 0xAA556677);      // value to store
        cpu.executeARM(0xE5801000);            // STR R1, [R0]   (seed memory)
        cpu.executeARM(0xE1903F9F);            // LDREX R3, [R0]  (exact faulting instruction)
        assertEquals(0xAA556677, cpu.r(3));
        cpu.writeRegister(1, 0x12345678);      // new value
        cpu.executeARM(0xE1802F91);            // STREX R2, R1, [R0]
        assertEquals(0, cpu.r(2));             // status: success
        assertEquals(0x12345678, cpu.r(1));
        cpu.executeARM(0xE5904000);            // LDR R4, [R0]
        assertEquals(0x12345678, cpu.r(4));    // store actually happened
    }

    @Test
    public void testLdrexStrexByte() throws Exception {
        CPU cpu = cpu();
        cpu.writeRegister(0, 0x20);
        cpu.writeRegister(1, 0x41);
        cpu.executeARM(0xE5C01000);            // STRB R1, [R0]
        cpu.executeARM(0xE1D03F9F);            // LDREXB R3, [R0]
        assertEquals(0x41, cpu.r(3));
        cpu.writeRegister(1, 0x7F);
        cpu.executeARM(0xE1C02F91);            // STREXB R2, R1, [R0]
        assertEquals(0, cpu.r(2));
        cpu.executeARM(0xE5D04000);            // LDRB R4, [R0]
        assertEquals(0x7F, cpu.r(4));
    }

    @Test
    public void testLdrexStrexHalfword() throws Exception {
        CPU cpu = cpu();
        cpu.writeRegister(0, 0x24);
        cpu.writeRegister(1, 0xBEEF);
        cpu.executeARM(0xE1C010B0);            // STRH R1, [R0]
        cpu.executeARM(0xE1F03F9F);            // LDREXH R3, [R0]
        assertEquals(0xBEEF, cpu.r(3));
        cpu.writeRegister(1, 0xCAFE);
        cpu.executeARM(0xE1E02F91);            // STREXH R2, R1, [R0]
        assertEquals(0, cpu.r(2));
        cpu.executeARM(0xE1D040B0);            // LDRH R4, [R0]
        assertEquals(0xCAFE, cpu.r(4));
    }

    @Test
    public void testLdrexStrexDual() throws Exception {
        CPU cpu = cpu();
        cpu.writeRegister(0, 0x30);
        cpu.writeRegister(4, 0x11223344);
        cpu.writeRegister(5, 0x55667788);
        cpu.executeARM(0xE5804000);            // STR R4, [R0]
        cpu.executeARM(0xE5805004);            // STR R5, [R0, #4]
        cpu.executeARM(0xE1B04F9F);            // LDREXD R4, R5, [R0]
        assertEquals(0x11223344, cpu.r(4));
        assertEquals(0x55667788, cpu.r(5));
        cpu.writeRegister(6, 0xAABBCCDD);
        cpu.writeRegister(7, 0x01020304);
        cpu.executeARM(0xE1A06F96);            // STREXD R2, R6, R7, [R0]
        assertEquals(0, cpu.r(2));
        cpu.executeARM(0xE5908000);            // LDR R8, [R0]
        cpu.executeARM(0xE5909004);            // LDR R9, [R0, #4]
        assertEquals(0xAABBCCDD, cpu.r(8));
        assertEquals(0x01020304, cpu.r(9));
    }

    @Test
    public void testSwpAndSwpb() throws Exception {
        CPU cpu = cpu();
        cpu.writeRegister(0, 0x40);
        cpu.writeRegister(1, 0x1234);
        cpu.executeARM(0xE5801000);            // STR R1, [R0]   (memory = 0x1234)
        cpu.writeRegister(1, 0xABCD);          // new value to swap in
        cpu.executeARM(0xE1003091);            // SWP R3, R1, [R0]
        assertEquals(0x1234, cpu.r(3));        // old memory value returned
        cpu.executeARM(0xE5904000);            // LDR R4, [R0]
        assertEquals(0xABCD, cpu.r(4));        // new value written

        cpu.writeRegister(1, 0x0000005A);      // byte value
        cpu.executeARM(0xE1403091);            // SWPB R3, R1, [R0]
        assertEquals(0xCD, cpu.r(3));          // old low byte (0xCD), zero-extended
        cpu.executeARM(0xE5904000);            // LDR R4, [R0]
        assertEquals(0xAB5A, cpu.r(4));        // byte swapped in, upper bytes unchanged
    }
}
