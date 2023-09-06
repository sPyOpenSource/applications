/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jCPU.JavaVM.vm;

import jCPU.CPU;
import jCPU.Opcode;
import java.util.function.Function;

/**
 *
 * @author spy
 */
public class VmByteCode implements Opcode{
    private final int opCode;
    private final Function func;
    private final int offset;
    private final String name;
    
    public VmByteCode(String name, int opCode, int offset, Function func){
        this.opCode = opCode;
        this.name = name;
        this.offset = offset;
        this.func = func;
    }

    @Override
    public int exec(CPU cpu, int pc) throws Exception {
        return (Integer)func.apply(cpu);
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
