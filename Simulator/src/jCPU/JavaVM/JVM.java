
package jCPU.JavaVM;

import static jCPU.JavaVM.ByteCode.findOpCode;
import jCPU.JavaVM.vm.SimpleMethodPool;
import java.util.logging.Level;
import java.util.logging.Logger;

import jx.classfile.MethodData;
import jx.classfile.constantpool.ClassCPEntry;
import jx.classfile.constantpool.ConstantPool;
import jx.classfile.constantpool.MethodRefCPEntry;
import jx.classfile.constantpool.NameAndTypeCPEntry;
import jx.classfile.constantpool.StringCPEntry;
import jx.disass.Disassembler;
//import jx.verifier.Verifier;

/**
 *
 * @author X. Wang
 */
public class JVM extends j51.intel.MCS51 {
    public VmStackFrame stack;
    public BytecodeVisitor handler;
    public char[] opCode;
    
    private ByteCode bytecode;// = new ByteCode(code);
    private BytecodeParser parser;// = new BytecodeParser(bytecode, handler);
    private final boolean run = true;
    private String clzNamePrint = "java/io/PrintStream";
    private String clzNameStrBuilder = "java/lang/StringBuilder";
    private char[] stringBuilderBuffer = new char[1024];
    private int stringBuilderUsed = 0;
    private SimpleMethodPool simpleMethodPool;

    public JVM(VmStackFrame stack) {
        this.stack = stack;
    }
    
    public JVM(){
        stack = new VmStackFrame();
        //Verifier verifier = new Verifier();
    }
    
    @Override
    public String getDecodeAt(int pc)
    {
        char c = (char)code(pc);
        try {
            VmOpcode result = ByteCode.findOpCode(c);
            if (result != null) {
                opCode = new char[result.getLength()];
                int z = 0;
                for(int i = 0; i < result.getLength(); i++){
                    opCode[i] = (char)code(i + pc);
                    z = z << 8 | opCode[i];
                }
                return "     " + Disassembler.toHexInt(z) + result.getDescription();
            }
        } catch (Exception e){
        }
        return "     " + Disassembler.toHexInt(c) + "NULL";
    }
    
    @Override
    public void go(int limit) throws Exception{
        while(true){
            step();
        }
    }
    
    @Override
    public int step()
    {
        if (!run){
            System.exit(1);
        }
        VmOpcode func = findOpCode((char)code(pc));
        if (func != null) {
            try {
                opCode = new char[func.getLength()];
                int z = 0;
                for(int i = 0; i < func.getLength(); i++){
                    opCode[i] = (char)code(i + pc);
                    z = z << 8 | opCode[i];
                }
                func.exec(new JVM(stack), pc);
            } catch (Exception ex) {
                Logger.getLogger(JVM.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        pc += findOpCode((char)code(pc)).getLength();
        return 0;
    }
    
    private boolean invokeLibrary(ConstantPool cp, String clsName, String method_name, String method_type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /* invokespecial */
    int op_invokespecial()
    {
        int method_index;
        char[] tmp = new char[2];
        tmp[0] = opCode[1];
        tmp[1] = opCode[2];
        method_index = tmp[0] << 8 | tmp[1];
        // System.out.print("call method_index %d\n", method_index);
        if (method_index < simpleMethodPool.method_used) {
            MethodData method = simpleMethodPool.method[method_index];
            //executeMethod(method);
        }
        return 0;
    }
    
    /* 0xb8 invoke */
    int op_invoke(ConstantPool cp)
    {
        int method_index ;
        char[] tmp = new char[2];
        String method_name, clsName, method_type;
        tmp[0] = opCode[1];
        tmp[1] = opCode[2];
        method_index = tmp[0] << 8 | tmp[1];
        // System.out.print("invoke method_index %d\n", method_index);
        // System.out.print("simpleMethodPool.method_used = %d\n", simpleMethodPool.method_used);
        if (method_index < simpleMethodPool.method_used) {
            MethodData method = simpleMethodPool.method[method_index];
            method_name = method.getName();
            System.out.printf(" method name = %s\n", method_name);
        } else {
            MethodRefCPEntry mRef = bytecode.findMethodRef(method_index);
            if (mRef !=null) {
                ClassCPEntry clasz = bytecode.findClassRef(mRef.getClassIndex());
                NameAndTypeCPEntry nat = bytecode.findNameAndType(mRef.getNameAndTypeIndex());
                if (clasz == null || nat == null) return -1;
                clsName = bytecode.getUTF8String(clasz.getCPIndex());
                method_name = nat.getName();
                method_type = nat.getTypeString();

                /* System.out.print("call class %s\n", clsName);
                System.out.print("call method %s\n", method_name);
                System.out.print("call method type %s\n", method_type);*/
                boolean ret = invokeLibrary(cp, clsName, method_name, method_type);
                if (ret) {
                    System.out.print("invoke java lang library successful\n");
                }
            }
        }

        return 0;
    }

    /* invokevirtual */
    int op_invokevirtual(ConstantPool cp)
    {
        int object_ref;
        char[] tmp = new char[2];
        String utf8 = "";
        int len;
        tmp[0] = opCode[1];
        tmp[1] = opCode[2];
        object_ref = tmp[0] << 8 | tmp[1];
        //System.out.print("call object_ref %d\n", object_ref);
        MethodRefCPEntry mRef = bytecode.findMethodRef(object_ref);
        if (mRef != null) {
            ClassCPEntry clasz = bytecode.findClassRef(mRef.getClassIndex());
            NameAndTypeCPEntry nat = bytecode.findNameAndType(mRef.getNameAndTypeIndex());
            if (clasz == null || nat == null) return -1;
            String clsName = bytecode.getUTF8String(clasz.getCPIndex());
            //System.out.print("call object ref class %s\n", clsName);
            if (clzNamePrint.equals(clsName)) {
                VmStackEntry entry = stack.pop();
                int index = entry.getInt();
                //System.out.print("call Println with index = %d\n", index);
                if (entry.type == VmStackFrame.STACK_ENTRY_REF) {
                    StringCPEntry strRef = bytecode.findStringRef(index);
                    if (strRef != null) {
                        utf8 = bytecode.getUTF8String(strRef.getCPIndex());
                        len = utf8.length();
                        //memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                        stringBuilderUsed += len;
                        stringBuilderBuffer[stringBuilderUsed] = 0;
                    }
                } else if (entry.type == VmStackFrame.STACK_ENTRY_INT) {
                    //System.out.print(utf8, "%d", index);
                    len = utf8.length();
                    //memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                    stringBuilderUsed += len;
                    stringBuilderBuffer[stringBuilderUsed] = 0;
                }
                //System.out.print out the result
                //System.out.print("%s\n", stringBuilderBuffer);
                stringBuilderUsed = 0;
            } else if (clzNameStrBuilder.equals(clsName)) {
                VmStackEntry entry = stack.pop();
                int index = entry.getInt();
                //System.out.print("call StringBuilder with index = %d\n", index);
                if (entry.type == VmStackFrame.STACK_ENTRY_REF) {
                    StringCPEntry strRef = bytecode.findStringRef(index);
                    if (strRef != null) {
                        utf8 = bytecode.getUTF8String(strRef.getCPIndex());
                        len = utf8.length();
                        //memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                        stringBuilderUsed += len;
                    }
                } else if (entry.type == VmStackFrame.STACK_ENTRY_INT) {
                    //System.out.print(utf8, "%d", index);
                    len = utf8.length();
                    //memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                    stringBuilderUsed += len;
                    //System.out.print("%s\n", stringBuilderBuffer);
                }

            }
        }
        return 0;
    }

    @Override
    public int getLengthAt(int pc){
        char c = (char)code(pc);
        try {
            return ByteCode.findOpCode(c).getLength();
        } catch (Exception e){
            return 1;
        }
    }
}
