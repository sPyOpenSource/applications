/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jCPU.JavaVM;

import jCPU.JavaVM.vm.VmCP;
import jCPU.JavaVM.vm.VmStackFrame;
//import jx.verifier.Verifier;

/**
 *
 * @author X. Wang
 */
public class JVM extends j51.intel.MCS51 {
    public VmStackFrame stack;
    public VmCP cp;
    public BytecodeVisitor handler;

    private ByteCode bytecode = new ByteCode(code);
    private BytecodeParser parser = new BytecodeParser(bytecode, handler);
    
    JVM(VmStackFrame stack, VmCP cp) {
        this.stack = stack;
        this.cp = cp;
    }
    
    public JVM(){
        //Verifier verifier = new Verifier();
    }
    
    @Override
    public String getDecodeAt(int pc)
    {
        String result = bytecode.findOpCode((char)code(pc)).getDescription();
        if (result != null) return "     " + result;
        return "     NULL";
    }
    
    @Override
    public void go(int limit) throws Exception{
        while(true){
            bytecode.executeMethod(null, stack, cp);
        }
    }
}
