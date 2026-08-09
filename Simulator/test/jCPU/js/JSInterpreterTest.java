package jCPU.js;

import org.junit.Test;
import static org.junit.Assert.*;

public class JSInterpreterTest {
    @Test public void printsArithmetic() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("print(1 + 2 * 3);");
        vm.go(-1);
        assertEquals("7\n", vm.readOut());
    }

    @Test public void runsVariablesAndLoop() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("var s = 0; for (var i=1; i<=5; i++){ s = s + i; } print(s);");
        vm.go(-1);
        assertEquals("15\n", vm.readOut());
    }

    @Test public void runsFunctionWithReturn() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("function add(a,b){ return a+b; } print(add(3,4));");
        vm.go(-1);
        assertEquals("7\n", vm.readOut());
    }

    @Test public void runsClosure() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("function make(c){ return function(n){ return n + c; }; } var f = make(10); print(f(5));");
        vm.go(-1);
        assertEquals("15\n", vm.readOut());
    }

    @Test public void stepsOneStatementPerCall() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("var a = 1; var b = 2; print(a + b);");
        int count = 0;
        while (vm.pc() >= 0) { vm.step(); count++; if (count > 100) break; }
        assertEquals("3\n", vm.readOut());
    }
}