package jCPU.common;

import jCPU.js.JSInterpreter;
import org.junit.Test;
import static org.junit.Assert.*;

public class SourceRenderTest {
    @Test
    public void decodeShowsSourceLineOfCurrentStatement() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("var a = 1;\nvar b = 2;\n");
        assertEquals("0\tvar a = 1;", vm.getDecodeAt(0));
        assertEquals("1\tvar b = 2;", vm.getDecodeAt(1));
    }
}