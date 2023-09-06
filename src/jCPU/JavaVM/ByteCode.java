
package jCPU.JavaVM;

import j51.intel.Code;
import jCPU.JavaVM.vm.VmByteCode;
import jCPU.JavaVM.vm.VmCP;
import jCPU.JavaVM.vm.VmConstMethodRef;
import jCPU.JavaVM.vm.VmMethod;
import jCPU.JavaVM.vm.VmStackEntry;
import jCPU.JavaVM.vm.VmStackFrame;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author X. Wang
 */
public class ByteCode {
    Code code;

    ByteCode(Code code) {
        this.code = code;
    }

    public char[] getBytecode() {
        char[] bc = new char[code.getCodeSize()];
        for(int i = 0; i < bc.length; i++){
            bc[i] = (char)code.getCode(i, false);
        }
        return bc;
    }

    public VmCP getCP() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public VmMethod getMethod() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static int popInt(VmStackFrame stack) {
        return stack.pop().getInt();
    }

    public static double popDouble(VmStackFrame stack) {
        return stack.pop().getDouble();
    }

    public static void pushInt(VmStackFrame stack, int i) {
        stack.push(i);
    }
    
    public static void pushRef(VmStackFrame stack, int i) {
        stack.push(i);
    }
    
    public static void pushDouble(VmStackFrame stack, double i) {
        stack.push(i);
    }
    
    public static VmStackEntry popEntry(VmStackFrame stack){
        return stack.pop();
    }

    private VmConstMethodRef findMethodRef(VmCP cp, int method_index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String getUTF8String(VmCP cp, int stringIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean invokeLibrary(VmStackFrame stack, VmCP cp, String clsName, String method_name, String method_type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantStringRef findStringRef(VmCP cp, int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantNameAndType findNameAndType(VmCP cp, int nameAndTypeIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantClassRef findClassRef(VmCP cp, int classIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
/*
 * Simple Java Virtual Machine Implementation
 *
 * Copyright (C) 2014 Jim Huang <jserv.tw@gmail.com>
 * Copyright (C) 2013 Chun-Yu Wang <wicanr2@gmail.com>
 */
 
private class ConstantNameAndType{
    int nameIndex, typeIndex;
    public ConstantNameAndType(){
        
    }
}
 
private class ConstantStringRef{
    private int stringIndex;
    public ConstantStringRef(){
        
    }
}
 
private class ConstantClassRef{
    int stringIndex, classIndex;
    public ConstantClassRef(){
        
    }
}

private class CodeAttribute{
    public char[] code;
    int code_length, attribute_name_index, max_stack, max_locals, attribute_length;
    public CodeAttribute(){
        
    }
}

private class MethodInfo{
    int attributes_count;
    private AttributeInfo[] attributes;
    private int name_index;
    public MethodInfo(){
        
    }
}

private class AttributeInfo{
    int attribute_name_index, attribute_length;
    char[] info;
    public AttributeInfo(){
        
    }
}

public class LocalVariables{
    int[] integer;
}

public class SimpleMethodPool{
    int method_used;
    MethodInfo[] method;
}

private boolean run = true;

/* invokespecial */
int op_invokespecial(char[] opCode, VmStackFrame stack, VmCP cp)
{
    int method_index;
    char[] tmp = new char[2];
    tmp[0] = opCode[1];
    tmp[1] = opCode[2];
    method_index = tmp[0] << 8 | tmp[1];
    // System.out.print("call method_index %d\n", method_index);
    if (method_index < BytecodeVisitor.simpleMethodPool.method_used) {
        MethodInfo method = BytecodeVisitor.simpleMethodPool.method[method_index];
        executeMethod(method, stack, cp);
    }
    return 0;
}

String clzNamePrint = "java/io/PrintStream";
String clzNameStrBuilder = "java/lang/StringBuilder";
char[] stringBuilderBuffer = new char[1024];
int stringBuilderUsed = 0;

/* 0xb8 invoke */
int op_invoke(char[] opCode, VmStackFrame stack, VmCP cp)
{
    int method_index ;
    char[] tmp = new char[2];
    String method_name, clsName, method_type;
    tmp[0] = opCode[1];
    tmp[1] = opCode[2];
    method_index = tmp[0] << 8 | tmp[1];
    // System.out.print("invoke method_index %d\n", method_index);
    // System.out.print("simpleMethodPool.method_used = %d\n", simpleMethodPool.method_used);
    if (method_index < BytecodeVisitor.simpleMethodPool.method_used) {
        MethodInfo method = BytecodeVisitor.simpleMethodPool.method[method_index];
        method_name = getUTF8String(cp, method.name_index);
        //System.out.print(" method name = %s\n", method_name);
    } else {
        VmConstMethodRef mRef = findMethodRef(cp, method_index);
        if (mRef !=null) {
            ConstantClassRef clasz = findClassRef(cp, mRef.classIndex);
            ConstantNameAndType nat = findNameAndType(cp, mRef.nameAndTypeIndex);
            if (clasz == null || nat == null) return -1;
            clsName = getUTF8String(cp, clasz.stringIndex);
            method_name = getUTF8String(cp, nat.nameIndex);
            method_type = getUTF8String(cp, nat.typeIndex);

            /* System.out.print("call class %s\n", clsName);
            System.out.print("call method %s\n", method_name);
            System.out.print("call method type %s\n", method_type);*/
            boolean ret = invokeLibrary(stack, cp, clsName, method_name, method_type);
            if (ret) {
                System.out.print("invoke java lang library successful\n");
            }
        }
    }

    return 0;
}

/* invokevirtual */
int op_invokevirtual(char[] opCode, VmStackFrame stack, VmCP cp)
{
    int object_ref;
    char[] tmp = new char[2];
    String utf8 = "";
    int len;
    tmp[0] = opCode[1];
    tmp[1] = opCode[2];
    object_ref = tmp[0] << 8 | tmp[1];
    //System.out.print("call object_ref %d\n", object_ref);
    VmConstMethodRef mRef = findMethodRef(cp, object_ref);
    if (mRef != null) {
        ConstantClassRef clasz = findClassRef(cp, mRef.classIndex);
        ConstantNameAndType nat = findNameAndType(cp, mRef.nameAndTypeIndex);
        if (clasz == null || nat == null) return -1;
        String clsName = getUTF8String(cp, clasz.stringIndex);
        //System.out.print("call object ref class %s\n", clsName);
        if (clzNamePrint.equals(clsName)) {
            VmStackEntry entry = popEntry(stack);
            int index = entry.getInt();
            //System.out.print("call Println with index = %d\n", index);
            if (entry.type == VmStackFrame.STACK_ENTRY_REF) {
                ConstantStringRef strRef = findStringRef(cp, index);
                if (strRef != null) {
                    utf8 = getUTF8String(cp, strRef.stringIndex);
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
            VmStackEntry entry = popEntry(stack);
            int index = entry.getInt();
            //System.out.print("call StringBuilder with index = %d\n", index);
            if (entry.type == VmStackFrame.STACK_ENTRY_REF) {
                ConstantStringRef strRef = findStringRef(cp, index);
                if (strRef != null) {
                    utf8 = getUTF8String(cp, strRef.stringIndex);
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

Function<JVM, Integer> op_aload_0 = cpu -> cpu.handler.op_aload_0();
Function<JVM, Integer> op_bipush = cpu -> cpu.handler.op_bipush(cpu.bytecode.getBytecode());
Function<JVM, Integer> op_dup, op_get, op_iadd, op_iconst_0;
Function<JVM, Integer> op_iconst_1, op_iconst_2;
Function<JVM, Integer> op_iconst_3, op_iconst_4, op_iconst_5;
Function<JVM, Integer> op_dconst_1, op_idiv, op_imul, op_dadd, op_dmul, op_d2i;
Function<JVM, Integer> op_invokespecial, op_invokevirtual;
Function<JVM, Integer> op_invoke = cpu -> op_invoke(cpu.bytecode.getBytecode(), cpu.stack, cpu.cp);
Function<JVM, Integer> op_iload, op_iload_1, op_iload_2, op_iload_3;
Function<JVM, Integer> op_istore, op_istore_1, op_istore_2, op_istore_3;
Function<JVM, Integer> op_isub, op_ldc, op_ldc2_w;
Function<JVM, Integer> op_new, op_irem, op_sipush, op_return;

public VmByteCode byteCodes[] = {
    new VmByteCode( "aload_0"       , 0x2A, 1, op_aload_0       ),
    new VmByteCode( "bipush"        , 0x10, 2, op_bipush        ),
    new VmByteCode( "dup"           , 0x59, 1, op_dup           ),
    new VmByteCode( "get"           , 0xB2, 3, op_get           ),
    new VmByteCode( "iadd"          , 0x60, 1, op_iadd          ),
    new VmByteCode( "iconst_0"      , 0x03, 1, op_iconst_0      ),
    new VmByteCode( "iconst_1"      , 0x04, 1, op_iconst_1      ),
    new VmByteCode( "iconst_2"      , 0x05, 1, op_iconst_2      ),
    new VmByteCode( "iconst_3"      , 0x06, 1, op_iconst_3      ),
    new VmByteCode( "iconst_4"      , 0x07, 1, op_iconst_4      ),
    new VmByteCode( "iconst_5"      , 0x08, 1, op_iconst_5      ),
    new VmByteCode( "dconst_1"      , 0x0F, 1, op_dconst_1      ),
    new VmByteCode( "idiv"          , 0x6C, 1, op_idiv          ),
    new VmByteCode( "imul"          , 0x68, 1, op_imul          ),
    new VmByteCode( "dadd"          , 0x63, 1, op_dadd          ),
    new VmByteCode( "dmul"          , 0x6B, 1, op_dmul          ),
    new VmByteCode( "d2i"           , 0x8e, 1, op_d2i           ),
    new VmByteCode( "invokespecial" , 0xB7, 3, op_invokespecial ),
    new VmByteCode( "invokevirtual" , 0xB6, 3, op_invokevirtual ),
    new VmByteCode( "invoke"        , 0xB8, 3, op_invoke        ),
    new VmByteCode( "iload"         , 0x15, 2, op_iload         ),
    new VmByteCode( "iload_1"       , 0x1B, 1, op_iload_1       ),
    new VmByteCode( "iload_2"       , 0x1C, 1, op_iload_2       ),
    new VmByteCode( "iload_3"       , 0x1D, 1, op_iload_3       ),
    new VmByteCode( "istore"        , 0x36, 2, op_istore        ),
    new VmByteCode( "istore_1"      , 0x3C, 1, op_istore_1      ),
    new VmByteCode( "istore_2"      , 0x3D, 1, op_istore_2      ),
    new VmByteCode( "istore_3"      , 0x3E, 1, op_istore_3      ),
    new VmByteCode( "isub"          , 0x64, 1, op_isub          ),
    new VmByteCode( "ldc"           , 0x12, 2, op_ldc           ),
    new VmByteCode( "ldc2_w"        , 0x14, 3, op_ldc2_w        ),
    new VmByteCode( "new"           , 0xBB, 3, op_new           ),
    new VmByteCode( "irem"          , 0x70, 1, op_irem          ),
    new VmByteCode( "sipush"        , 0x11, 3, op_sipush        ),
    new VmByteCode( "return"        , 0xB1, 1, op_return        )
};

int convertToCodeAttribute(CodeAttribute ca, AttributeInfo attr)
{
    int info_p = 0;
    char[] tmp = new char[4];
    ca.attribute_name_index = attr.attribute_name_index;
    ca.attribute_length = attr.attribute_length;
    tmp[0] = attr.info[info_p++];
    tmp[1] = attr.info[info_p++];
    ca.max_stack = tmp[0] << 8 | tmp[1];
    tmp[0] = attr.info[info_p++];
    tmp[1] = attr.info[info_p++];
    ca.max_locals = tmp[0] << 8 | tmp[1];
    tmp[0] = attr.info[info_p++];
    tmp[1] = attr.info[info_p++];
    tmp[2] = attr.info[info_p++];
    tmp[3] = attr.info[info_p++];
    ca.code_length = tmp[0] << 24 | tmp[1] << 16 | tmp[2] << 8 | tmp[3];
    ca.code = new char[ca.code_length];
    //memcpy(ca.code, attr.info[info_p], ca.code_length);
    return 0;
}

int executeMethod(MethodInfo startup, VmStackFrame stack, VmCP cp)
{
    int i = 0;
    CodeAttribute ca = null;
    for (int j = 0 ; j < startup.attributes_count ; j++) {
        convertToCodeAttribute(ca, startup.attributes[j]);
        String name = getUTF8String(cp, ca.attribute_name_index);
        if (!"Code".equals(name)) continue;
        System.out.print("----------------------------------------\n");
        System.out.print("code dump\n");
        printCodeAttribute(ca, cp);
        System.out.print("----------------------------------------\n");
        char[] pc = ca.code;
        if (!run){
            System.exit(1);
        }
        do {
            VmByteCode func = findOpCode(pc[i]);
            if (func != null) {
                try {
                    if (func.exec(new JVM(stack, cp), i) < 0) break;
                } catch (Exception ex) {
                    Logger.getLogger(ByteCode.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            i += findOpCode(pc[i]).getLength();
        } while (true);
    }
    return 0;
}

    public VmByteCode findOpCode(char op)
    {
        for (VmByteCode byteCode : byteCodes) {
            if (op == byteCode.getOpcode()) {
                return byteCode;
            }
        }
        return null;
    }

    void printCodeAttribute(CodeAttribute ca, VmCP cp)
    {
       String name = getUTF8String(cp, ca.attribute_name_index);
       System.out.println("attribute name : " + name);
       System.out.println("attribute length: " + ca.attribute_length);

       System.out.println("max_stack: " + ca.max_stack);
       System.out.println("max_locals: " + ca.max_locals);
       System.out.println("code_length: " + ca.code_length);
       char[] pc = ca.code;
       int i = 0;
       do {
           String opName = findOpCode(pc[0]).getDescription();
           if (opName == null) {
               //System.out.print("Unknow OpCode %02X\n", pc[0]);
               System.exit(1);
           }
           //System.out.print("%s \n", opName);
           i += findOpCode(pc[0]).getLength();
           //pc += tmp;
       } while (i < ca.code_length);
   }
}
