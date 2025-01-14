
package jCPU.JavaVM;

import jCPU.Opcode;
import java.util.function.Function;
import jCPU.iCPU;

/**
 *
 * @author spy
 */
public class VmOpcode implements Opcode {
    private final int opCode;
    private final Function func;
    private final int offset;
    private final String name;
    
    public VmOpcode(String name, int opCode, int offset, Function func){
        this.opCode = opCode;
        this.name = name;
        this.offset = offset;
        this.func = func;
    }

    @Override
    public void exec(iCPU cpu, int pc) throws Exception {
        func.apply(cpu);
    }

    @Override
    public int getLength() {
        return offset;
    }

    @Override
    public String decode(iCPU cpu, int pc) {
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
