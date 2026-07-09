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
}
