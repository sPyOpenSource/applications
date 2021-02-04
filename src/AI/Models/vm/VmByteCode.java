/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI.Models.vm;

import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 *
 * @author spy
 */
public class VmByteCode {
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
    public ByteBuffer getBytecode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public VmCP getCP() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public VmMethod getMethod() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
