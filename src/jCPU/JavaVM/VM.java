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
public class VM extends j51.intel.MCS51{
    public VmStackFrame stack;
    public VmCP p;
    public char[][] opCode;
    private ByteCode bytecode = new ByteCode();

    VM(char[][] pc, VmStackFrame stackFrame, VmCP p) {
        stack = stackFrame;
        opCode = pc;
        this.p = p;
    }
    
    public VM(){
        
    }
    
    @Override
    public String getDecodeAt(int pc)
    {
        String result = bytecode.findOpCode((char)code(pc));
        if(result != null) return "     " + result;
        return "     NULL";
    }
    
    @Override
    public void go(int limit) throws Exception{
        while(true){
            
        }
    }
}
