package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86CoreTest {
    @Test
    public void testRegisterConstantsAndAccess() {
        X86Core core = new X86Core(64 * 1024 * 1024);
        assertEquals(0, X86Core.REG_EAX);
        assertEquals(1, X86Core.REG_ECX);
        assertEquals(2, X86Core.REG_EDX);
        assertEquals(3, X86Core.REG_EBX);
        assertEquals(4, X86Core.REG_ESP);
        assertEquals(5, X86Core.REG_EBP);
        assertEquals(6, X86Core.REG_ESI);
        assertEquals(7, X86Core.REG_EDI);
        assertEquals(8, X86Core.REG_VIRTUAL_BASE);
        
        core.setReg(X86Core.REG_EAX, 0x12345678);
        assertEquals(0x12345678, core.getReg(X86Core.REG_EAX));
        core.setReg(X86Core.REG_VIRTUAL_BASE + 3, 0xDEADBEEF);
        assertEquals(0xDEADBEEF, core.getReg(X86Core.REG_VIRTUAL_BASE + 3));
    }

    @Test
    public void test32BitRegisterIsLowHalfOf64BitRegister() {
        X86Core core = new X86Core(64 * 1024);
        core.setLongMode(true);
        core.setReg64(X86Core.REG_RAX, 0x1122334455667788L);
        assertEquals(0x55667788, core.getReg(X86Core.REG_EAX));

        core.setReg(X86Core.REG_EAX, 0x80000001);
        assertEquals("a 32-bit write clears the upper half",
                0x80000001L, core.getReg64(X86Core.REG_RAX));

        core.setReg64(X86Core.REG_RDI, -1L);
        assertEquals(-1, core.getReg(X86Core.REG_EDI));
        assertEquals("R8..R15 have no 32-bit alias in the legacy array",
                0, core.getReg64(X86Core.REG_R8));
    }

    @Test
    public void testStackPointerViewsStayInStep() {
        X86Core core = new X86Core(64 * 1024);
        core.setLongMode(true);
        core.setReg64(X86Core.REG_RSP, 0x8000);
        core.push64(0x1122334455667788L);
        assertEquals(0x7FF8, core.getReg(X86Core.REG_ESP));
        assertEquals(0x1122334455667788L, core.readMem64(0x7FF8L));
        assertEquals(0x1122334455667788L, core.pop64());
        assertEquals(0x8000L, core.getReg64(X86Core.REG_RSP));
    }

    @Test
    public void testAddressAbove4GbDoesNotAliasLowMemory() {
        X86Core core = new X86Core(64 * 1024);
        core.writeMem8(0x1000, 0xAB);
        assertEquals(0xAB, core.readMem8(0x1000L));
        assertEquals(0, core.readMem8(0x100001000L));
        core.writeMem8(0x100001000L, 0xCD);
        assertEquals("a write above 4 GB must not land at 0x1000", 0xAB, core.readMem8(0x1000));
        assertEquals(0, core.readMem64(0xFFFFFFFFFFFFFFF8L));
    }
}
