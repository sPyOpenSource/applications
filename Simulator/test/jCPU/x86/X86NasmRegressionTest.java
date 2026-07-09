package jCPU.x86;

import nasm.NasmVM;
import nasm.NasmEval;
import org.junit.Test;
import static org.junit.Assert.*;

public class X86NasmRegressionTest {
    @Test
    public void testIncr1ProducesCorrectOutput() throws Exception {
        String testFile = "/Users/xuyi/Source/Java/nasm/test2024/nasm-ref/incr1.nasm";
        NasmVM vm = new NasmVM();
        vm.init(testFile);
        X86Core core = vm.getCore();
        NasmEval eval = vm.getEval();

        while (!eval.isStopped() && core.getPC() < vm.getCode().sectionText.size()) {
            eval.execute();
        }

        assertEquals(1, core.getReg(X86Core.REG_EAX));
        assertEquals(0, core.getReg(X86Core.REG_EBX));
        assertTrue(eval.isStopped());
    }
}
