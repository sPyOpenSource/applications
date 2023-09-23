
package jCPU.JavaVM;

import jCPU.JavaVM.vm.VmByteCode;
import jCPU.JavaVM.vm.VmCP;
import jCPU.JavaVM.vm.VmStackFrame;

/**
 *
 * @author X. Wang
 */
public class JVM extends j51.intel.MCS51{
    public VmStackFrame stack;
    public VmCP cp;
    public BytecodeVisitor handler;

    public ByteCode bytecode = new ByteCode(code);
    private BytecodeParser parser = new BytecodeParser(bytecode, handler);
    
    JVM(VmStackFrame stack, VmCP cp) {
        this.stack = stack;
        this.cp = cp;
    }
    
    public JVM(){
    }
    
    @Override
    public String getDecodeAt(int pc)
    {
        char ins = (char)code.getCode(pc, false);
        VmByteCode bc = bytecode.findOpCode(ins);
        if (bc != null){
            String result = bc.getDescription();
            if (result != null) return result;
        }
        return "NULL";
    }
    
    @Override
    public void go(int limit) throws Exception{
        while(true){
            bytecode.executeMethod(null, stack, cp);
        }
    }
}
