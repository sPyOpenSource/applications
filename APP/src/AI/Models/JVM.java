/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI.Models;

import AI.Models.vm.VmByteCode;
import AI.Models.vm.VmCP;
import AI.Models.vm.VmStackFrame;
import AI.Models.vm.VmStackEntry;
import java.util.Arrays;
import java.util.function.Function;

/**
 *
 * @author X. Wang
 */
public class JVM {

    public JVM(int pc, VmStackFrame stack, VmCP cp) {
        this.pc = pc;
        this.stack = stack;
        this.cp = cp;
    }

    private ConstantMethodRef findMethodRef(int method_index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String getUTF8String(int stringIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean invoke_java_lang_library(String clsName, String method_name, String method_type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantStringRef findStringRef(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantNameAndType findNameAndType(int nameAndTypeIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantClassRef findClassRef(int classIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /*
     * Simple Java Virtual Machine Implementation
     *
     * Copyright (C) 2014 Jim Huang <jserv.tw@gmail.com>
     * Copyright (C) 2013 Chun-Yu Wang <wicanr2@gmail.com>
     */
    private class ConstantMethodRef{
        int nameAndTypeIndex, classIndex;
        public ConstantMethodRef(){

        }
    }

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

    private char[] opCode;
    private final VmStackFrame stack;
    private final VmCP cp;
    int pc;

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

    private class LocalVariables{
        int[] integer;
    }

    private class SimpleMethodPool{
        int method_used;
        MethodInfo[] method;
    }

private boolean run = true;
private LocalVariables localVariables;
private SimpleMethodPool simpleMethodPool;
 
public double get_double_parameter()
{
    double value;
    if (stack.isRef()) {
        int index = stack.popInt();
        value = cp.getDouble(index);
        //System.out.print("index %d\n", index);
        //System.out.print("get value from constant pool = %f\n", value);
    } else {
        value = stack.popDouble();
        //System.out.print("get value from stack = %f\n", value);
    }
    return value;
}

/* opcode implementation */

/* aload_0 */
public int op_aload_0()
{
    stack.pushInt(0);
    System.out.print("push 0 into stack\n");
    //opCode = opCode + 1;
    return 0;
}

/* bipush */
public int op_bipush()
{
    int value = opCode[pc + 1];
    stack.pushInt(value);
    //System.out.print("push a byte %d onto the stack \n", value);
    //opCode = opCode + 2;
    return 0;
}
 
int EntryToInt(VmStackEntry entry){
    return 0;
}

/* dup */
int op_dup()
{
    VmStackEntry entry = stack.popEntry();
    int value = EntryToInt(entry);
    if (entry.type == VmStackFrame.STACK_ENTRY_INT) {
        stack.pushInt(value);
        stack.pushInt(value);
    } else {
        stack.pushRef(value);
        stack.pushRef(value);
    }
    System.out.print("dup\n");
    //opCode = opCode + 1;
    return 0;
}

/* get */
int op_get()
{
    int field_index ;
     char[] tmp = new char[2];
    tmp[0] = opCode[pc + 1];
    tmp[1] = opCode[pc + 2];
    field_index = tmp[0] << 8 | tmp[1];
    //System.out.print("get %d\n", field_index);
    stack.pushRef(field_index);
    return 0;
}

/* iadd */
int op_iadd()
{
    int value1 = stack.popInt();
    int value2 = stack.popInt();
    int result = value1 + value2;
    //System.out.print("iadd: %d + %d = %d\n", value1, value2, result);
    stack.pushInt(result);
    return 0;
}

/* iconst_0 */
int op_iconst_0()
{
    stack.pushInt(0);
    System.out.print("iconst_0: push 0 into stack\n");
    return 0;
}

/* iconst_1 */
int op_iconst_1()
{
    stack.pushInt(1);
    System.out.print("iconst_1: push 1 into stack\n");
    return 0;
}

/* iconst_2 */
int op_iconst_2()
{
    stack.pushInt(2);
    System.out.print("iconst_2: push 1 into stack\n");
    return 0;
}

/* iconst_3 */
int op_iconst_3()
{
    stack.pushInt(3);
    System.out.print("iconst_3: push 1 into stack\n");
    return 0;
}

/* iconst_4 */
int op_iconst_4()
{
    stack.pushInt(4);
    System.out.print("iconst_4: push 1 into stack\n");
    return 0;
}

/* iconst_5 */
int op_iconst_5()
{
    stack.pushInt(5);
    System.out.print("iconst_5: push 5 into stack\n");
    return 0;
}

/* 0x0F dconst_1 */
int op_dconst_1()
{
    stack.pushDouble(1.0f);
    System.out.print("iconst_5: push 1.0f into stack\n");
    return 0;
}

/* idiv */
int op_idiv()
{
    int value2 = stack.popInt();
    int value1 = stack.popInt();
    int result = value1 / value2;
    //System.out.print("idiv: %d / %d = %d\n", value1, value2, result);
    stack.pushInt(result);
    return 0;
}

/* iload */
int op_iload(char[] opCode)
{
    int index = opCode[1];
    int value = localVariables.integer[index];
    //System.out.print("iload: load value from local variable %d(%d)\n", index, localVariables.integer[index]);
    stack.pushInt(value);
    return 0;
}

/* iload_1 */
int op_iload_1()
{
    int value = localVariables.integer[1];
    //System.out.print("iload_1: load value from local variable 1(%d)\n", localVariables.integer[1]);
    stack.pushInt(value);
    return 0;
}

/* iload_2 */
int op_iload_2()
{
    int value = localVariables.integer[2];
    //System.out.print("iload_2: load value from local variable 2(%d)\n", localVariables.integer[2]);
    stack.pushInt(value);
    return 0;
}

/* iload_3 */
int op_iload_3()
{
    int value = localVariables.integer[3];
    //System.out.print("iload_3: load value from local variable 3(%d)\n", localVariables.integer[3]);
    stack.pushInt(value);
    return 0;
}

/* imul */
int op_imul()
{
    int value1 = stack.popInt();
    int value2 = stack.popInt();
    int result = value1 * value2;
    //System.out.print("imul: %d * %d = %d\n", value1, value2, result);
    stack.pushInt(result);
    return 0;
}

/* 0x63 dadd */
int op_dadd()
{
    double value1 = get_double_parameter();
    double value2 = get_double_parameter();
    double result = value1 + value2;
    //System.out.print("dadd: %f + %f = %f\n", value1, value2, result);
    stack.pushDouble(result);
    return 0;
}

/* 0x6B dmul */
int op_dmul()
{
    double value1 = get_double_parameter();
    double value2 = get_double_parameter();
    double result = value1 * value2;
    //System.out.print("dmul: %f * %f = %f\n", value1, value2, result);
    stack.pushDouble(result);
    return 0;
}

/* 0x8e d2i */
int op_d2i()
{
    double value1 = stack.popDouble();
    int result = (int)value1;
    //System.out.print("d2i: %d <-- %f\n", result, value1);
    stack.pushInt(result);
    return 0;
}

/* irem */
int op_irem()
{
    int value1 = stack.popInt();
    int value2 = stack.popInt();
    int result = value2 % value1;
    //System.out.print("irem: %d mod %d = %d\n", value2, value1, result);
    stack.pushInt(result);
    return 0;
}

/* istore */
int op_istore()
{
    int value = stack.popInt();
    int index = opCode[pc + 1];
    //System.out.print("istore: store value into local variable %d(%d)\n", index, value);
    localVariables.integer[index] = value;
    return 0;
}

/* istore_1 */
int op_istore_1()
{
    int value = stack.popInt();
    //System.out.print("istore_1: store value into local variable 1(%d)\n", value);
    localVariables.integer[1] = value;
    return 0;
}

/* istore_2 */
int op_istore_2()
{
    int value = stack.popInt();
    //System.out.print("istore_2: store value into local variable 2(%d)\n", value);
    localVariables.integer[2] = value;
    return 0;
}

/* istore_3 */
int op_istore_3()
{
    int value = stack.popInt();
    //System.out.print("istore_3: store value into local variable 3(%d)\n", value);
    localVariables.integer[3] = value;
    return 0;
}

/* isub */
int op_isub()
{
    int value2 = stack.popInt();
    int value1 = stack.popInt();
    int result = value1 - value2;
    //System.out.print("isub : %d - %d = %d\n", value1, value2, result);
    stack.pushInt(result);
    return 0;
}

/* invokespecial */
int op_invokespecial(char[] opCode)
{
    int method_index;
    char []tmp = new char[2];
    tmp[0] = opCode[1];
    tmp[1] = opCode[2];
    method_index = tmp[0] << 8 | tmp[1];
    //System.out.print("call method_index %d\n", method_index);
    if (method_index < simpleMethodPool.method_used) {
        MethodInfo method = simpleMethodPool.method[method_index];
        executeMethod(method);
    }
    return 0;
}

private final String clzNamePrint = "java/io/PrintStream";
private final String clzNameStrBuilder = "java/lang/StringBuilder";
private final char[] stringBuilderBuffer = new char[1024];
private int stringBuilderUsed = 0;

/* 0xb8 invoke */
public int op_invoke()
{
    String method_name, clsName, method_type;
    int method_index = opCode[pc] << 8 | opCode[pc + 1];
    //System.out.print("invoke method_index %d\n", method_index);
    //System.out.print("simpleMethodPool.method_used = %d\n", simpleMethodPool.method_used);
    if (method_index < simpleMethodPool.method_used) {
        MethodInfo method = simpleMethodPool.method[method_index];
        method_name = getUTF8String(method.name_index);
        //System.out.print(" method name = %s\n", method_name);
    } else {
        ConstantMethodRef mRef = findMethodRef(method_index);
        if (mRef !=null) {
            ConstantClassRef clasz = findClassRef(mRef.classIndex);
            ConstantNameAndType nat = findNameAndType(mRef.nameAndTypeIndex);
            if (clasz == null || nat == null) return -1;
            clsName = getUTF8String(clasz.stringIndex);
            method_name = getUTF8String(nat.nameIndex);
            method_type = getUTF8String(nat.typeIndex);

           /* System.out.print("call class %s\n", clsName);
            System.out.print("call method %s\n", method_name);
            System.out.print("call method type %s\n", method_type);*/
	    boolean ret =
            invoke_java_lang_library(clsName, method_name, method_type);
            if (ret) {
                System.out.print("invoke java lang library successful\n");
            }
        }
    }

    return 0;
}

/* invokevirtual */
int op_invokevirtual()
{
    String clsName, utf8 = "";
    int len;
    int object_ref = opCode[pc] << 8 | opCode[pc + 1];
    //System.out.print("call object_ref %d\n", object_ref);
    ConstantMethodRef mRef = findMethodRef(object_ref);
    if (mRef != null) {
        ConstantClassRef clasz = findClassRef(mRef.classIndex);
        ConstantNameAndType nat = findNameAndType(mRef.nameAndTypeIndex);
        if (clasz == null || nat == null) return -1;
        clsName = getUTF8String(clasz.stringIndex);
        //System.out.print("call object ref class %s\n", clsName);
        if (clzNamePrint.equals(clsName)) {
            VmStackEntry entry = stack.popEntry();
            int index = EntryToInt(entry);
            //System.out.print("call Println with index = %d\n", index);
            if (entry.type == VmStackFrame.STACK_ENTRY_REF) {
                ConstantStringRef strRef = findStringRef(index);
                if (strRef != null) {
                    utf8 = getUTF8String(strRef.stringIndex);
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
            VmStackEntry entry = stack.popEntry();
            int index = EntryToInt(entry);
            //System.out.print("call StringBuilder with index = %d\n", index);
            if (entry.type == VmStackFrame.STACK_ENTRY_REF) {
                ConstantStringRef strRef = findStringRef(index);
                if (strRef != null) {
                    utf8 = getUTF8String(strRef.stringIndex);
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

/* ldc */
int op_ldc()
{
    int value = opCode[pc + 1];
    stack.pushRef(value);
    //System.out.print("ldc: push a constant index %d onto the stack \n", value);
    return 0;
}

/* 0x14 ldc2_w */
int op_ldc2_w()
{
    char index1 = opCode[pc + 1];
    char index2 = opCode[pc + 2];
    int index = (index1 << 8) | index2;
    stack.pushRef(index);
    //System.out.print("ldc2_w: push a constant index %d onto the stack \n", index);
    return 0;
}

/* 0x11 op_sipush */
int op_sipush()
{
    short value;
    char[] tmp = new char[2];
    tmp[0] = opCode[pc + 1];
    tmp[1] = opCode[pc + 2];
    value = (short) (tmp[0] << 8 | tmp[1]);
    //System.out.print("sipush value %d\n", value);
    stack.pushInt(value);
    return 0;
}

/* op_new */
int op_new()
{
    char []tmp = new char[2];
    tmp[0] = opCode[pc + 1];
    tmp[1] = opCode[pc + 2];
    int object_ref = tmp[0] << 8 | tmp[1];
    //System.out.print("new: new object_ref %d\n", object_ref);
    return 0;
}

/* return */
int op_return()
{
    System.out.print("return: \n");
    return -1;
}

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
    ca.code = Arrays.copyOfRange(attr.info, info_p, ca.code_length + 1);
    return 0;
}

int executeMethod(MethodInfo startup)
{
    CodeAttribute ca = new CodeAttribute();
    for (int j = 0 ; j < startup.attributes_count ; j++) {
        convertToCodeAttribute(ca, startup.attributes[j]);
        if ("Code".equals(getUTF8String(ca.attribute_name_index))) continue;
        System.out.println("----------------------------------------");
        System.out.println("code dump");
        printCodeAttribute(ca);
        System.out.println("----------------------------------------");
        if (!run)
            System.exit(1);
        VmByteCode bytecode;
        do {
            bytecode = new VmByteCode(ca.code[pc]);
            Function func = bytecode.findOpCode().func;
            if (func != null) {
                if((Integer)func.apply(new JVM(pc, stack, cp)) < 0) break;
            }
        } while (true);
    }
    return 0;
}

    void printCodeAttribute(CodeAttribute ca)
    {
        String name = getUTF8String(ca.attribute_name_index);
        System.out.println("attribute name : " + name);
        System.out.println("attribute length: " + ca.attribute_length);

        System.out.println("max_stack: " + ca.max_stack);
        System.out.println("max_locals: " + ca.max_locals);
        System.out.println("code_length: " + ca.code_length);
        int i = 0;
        VmByteCode bytecode;
        do {
            bytecode = new VmByteCode(ca.code[i]);
            String opName = bytecode.findOpCode().name;
            if (opName == null) {
                System.out.println("Unknow OpCode %02X" + ca.code[i]);
                System.exit(1);
            }
            System.out.println(opName);
            i += bytecode.findOpCode().offset;
        } while (i < ca.code_length);
    }
}
