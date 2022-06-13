/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jCPU.JavaVM.vm;

import jCPU.CPU;
import jCPU.Opcode;
import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 *
 * @author spy
 */
public class VmByteCode implements Opcode{
    public int opCode;
    public Function func;
    public int offset;
    public String name;
    
    public VmByteCode(String name, int opCode, int offset, Function func){
        this.opCode = opCode;
        this.name = name;
        this.offset = offset;
        this.func = func;
    }

    @Override
    public void exec(CPU cpu, int pc) throws Exception {
        func.apply(cpu);
    }

    @Override
    public int getLength() {
        return offset;
    }

    @Override
    public String decode(CPU cpu, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getCycle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getOpcode() {
        return opCode;
    }

    @Override
    public String getDescription() {
        return name;
    }
}
