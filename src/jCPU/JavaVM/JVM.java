/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jCPU.JavaVM;

import jCPU.JavaVM.vm.VmCP;
import jCPU.JavaVM.vm.VmStackFrame;

/**
 *
 * @author X. Wang
 */
public class JVM extends j51.intel.MCS51{
    public VmStackFrame stack;
    public VmCP cp;
    public char[] opCode;
    private ByteCode bytecode = new ByteCode(code);
    public BytecodeVisitor handler;
    private BytecodeParser parser = new BytecodeParser(bytecode, handler);
    
    JVM(char[] pc, VmStackFrame stack, VmCP cp) {
        this.stack = stack;
        opCode = pc;
        this.cp = cp;
    }
    
    public JVM(){
        
    }
    
    @Override
    public String getDecodeAt(int pc)
    {
        String result = bytecode.findOpCode((char)code(pc));
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
