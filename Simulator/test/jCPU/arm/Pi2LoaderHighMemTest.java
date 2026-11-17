package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

/** Pi2Loader must map enough RAM that kernel virtual addresses in the 3G
 *  alias range (PAGE_OFFSET_3G + phys) resolve to physical RAM. */
public class Pi2LoaderHighMemTest {

    @Test
    public void testThreeGAliasCoversHighKernelVirtualAddress() throws Exception {
        CPU cpu = Pi2Loader.loadImage(new byte[] {0, 0, 0, 0});
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();
        /* 0xffebecf0 is a kernel virtual address the boot process writes to.
         * Through the 3G alias it maps to physical 0x3febecf0, which must be
         * backed by RAM. */
        vm.writeInt(0xFFECECF0, 0xDEADBEEF, false, false);
        assertEquals(0xDEADBEEF, vm.readInt(0xFFECECF0, false, false));
    }
}
