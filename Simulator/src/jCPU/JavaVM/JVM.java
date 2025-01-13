
package jCPU.JavaVM;

import static jCPU.JavaVM.ByteCode.findOpCode;
import jCPU.JavaVM.vm.VmCP;
import jCPU.JavaVM.vm.VmOpcode;
import jCPU.JavaVM.vm.VmStackFrame;
import jx.disass.Disassembler;
//import jx.verifier.Verifier;

/**
 *
 * @author X. Wang
 */
public class JVM extends j51.intel.MCS51 {
    public VmStackFrame stack;
    public VmCP cp;
    public BytecodeVisitor handler;

    private ByteCode bytecode;// = new ByteCode(code);
    private BytecodeParser parser;// = new BytecodeParser(bytecode, handler);
    private final boolean run = true;

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
        char c = (char)code(pc);
        //bytecode = new ByteCode(code);
        try {
            String result = ByteCode.findOpCode(c).getDescription();
            if (result != null) return Disassembler.toHexInt(c) + result;
        } catch (Exception e){
        }
        return Disassembler.toHexInt(c) + "NULL";
    }
    
    @Override
    public void go(int limit) throws Exception{
        while(true){
            executeMethod(null);
        }
    }
    
    int executeMethod(ByteCode.MethodInfo startup)
    {
        int i = 0;
        ByteCode.CodeAttribute ca = null;
        for (int j = 0 ; j < startup.attributes_count ; j++) {
            bytecode.convertToCodeAttribute(ca, startup.attributes[j]);
            String name = bytecode.getUTF8String(cp, ca.attribute_name_index);
            if (!"Code".equals(name)) continue;
            System.out.print("----------------------------------------\n");
            System.out.print("code dump\n");
            bytecode.printCodeAttribute(ca, cp);
            System.out.print("----------------------------------------\n");
            char[] pc = ca.code;
            if (!run){
                System.exit(1);
            }
            do {
                VmOpcode func = findOpCode(pc[i]);
                if (func != null) {
                    //if ((Integer)func.exec(new JVM(stack, cp), i) < 0) break;
                }
                i += findOpCode(pc[i]).getLength();
            } while (true);
        }
        return 0;
    }
    
    /* invokespecial */
    int op_invokespecial(char[] opCode)
    {
        int method_index;
        char[] tmp = new char[2];
        tmp[0] = opCode[1];
        tmp[1] = opCode[2];
        method_index = tmp[0] << 8 | tmp[1];
        // System.out.print("call method_index %d\n", method_index);
        if (method_index < BytecodeVisitor.simpleMethodPool.method_used) {
            ByteCode.MethodInfo method = BytecodeVisitor.simpleMethodPool.method[method_index];
            executeMethod(method);
        }
        return 0;
    }

    @Override
    public int getLengthAt(int pc){
        char c = (char)code(pc);
        //bytecode = new ByteCode(code);
        try {
            return ByteCode.findOpCode(c).getLength();
        } catch (Exception e){
            return 1;
        }
    }
}
