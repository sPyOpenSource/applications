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
 * @author spy
 */
public class VM extends j51.intel.MCS51{
public VmStackFrame stack;
public VmCP p;
public char[][] opCode;

    VM(char[][] pc, VmStackFrame stackFrame, VmCP p) {
        stack = stackFrame;
        opCode = pc;
        this.p = p;
    }
    
    public VM(){
        
    }
}
