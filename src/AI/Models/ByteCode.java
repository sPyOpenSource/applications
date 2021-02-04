/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI.Models;

import AI.Models.vm.VmByteCode;
import AI.Models.vm.VmCP;
import AI.Models.vm.VmStackFrame;
import java.util.function.Function;

/**
 *
 * @author X. Wang
 */
public class ByteCode {
    
/* Stack Frame */
int STACK_ENTRY_NONE        =0;
int STACK_ENTRY_INT         =1;
int STACK_ENTRY_REF         =2;
int STACK_ENTRY_LONG        =3;
int STACK_ENTRY_DOUBLE     = 4;
int STACK_ENTRY_FLOAT       =5;

    private  boolean is_ref_entry(VmStackFrame stack) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private  int popInt(VmStackFrame stack) {
        return (Integer)stack.pop();
    }

    private  double get_double_from_constant_pool(VmCP p, int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private  double popDouble(VmStackFrame stack) {
        return (Double)stack.pop();
    }

    private  void pushInt(VmStackFrame stack, int i) {
        stack.push(i);
    }
    
    private  void pushRef(VmStackFrame stack, int i) {
        stack.push(i);
    }
    
    private  void pushDouble(VmStackFrame stack, double i) {
        stack.push(i);
    }
    
     StackEntry popEntry(VmStackFrame stack){
        return (StackEntry)stack.pop();
    }

    private ConstantMethodRef findMethodRef(VmCP p, int method_index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void getUTF8String(VmCP p, int stringIndex, int i, char[] clsName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean invoke_java_lang_library(VmStackFrame stack, VmCP p, char[] clsName, char[] method_name, char[] method_type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private int strcmp(String clzNamePrint, char[] clsName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantStringRef findStringRef(VmCP p, int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private int strlen(char[] utf8) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void memset(char[] stringBuilderBuffer, int i, int i0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void memcpy(char[] code, char c, int code_length) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private int memcmp(char[] name, String code, int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantNameAndType findNameAndType(VmCP p, int nameAndTypeIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ConstantClassRef findClassRef(VmCP p, int classIndex) {
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

VmStackFrame stackFrame;
VmCP simpleConstantPool;

private  class CodeAttribute{
    public char[][] code;
    int code_length, attribute_name_index, max_stack, max_locals, attribute_length;
    public CodeAttribute(){
        
    }
}

private  class MethodInfo{
    int attributes_count;
        private AttributeInfo[] attributes;
        private int name_index;
    public MethodInfo(){
        
    }
    }

private  class AttributeInfo{
    int attribute_name_index, attribute_length;
    char[]info;
    public AttributeInfo(){
        
    }
}

private class LocalVariables{
    int[]integer;
    
}
private class SimpleMethodPool{
    int method_used;
    MethodInfo[] method;
    
}

 int run = 1;
 LocalVariables localVariables;
 SimpleMethodPool simpleMethodPool;
 
public  double get_double_parameter(VmStackFrame stack, VmCP p)
{
    double value = 0.0f;
    if (is_ref_entry(stack)) {
        int index = popInt(stack);
        value = get_double_from_constant_pool(p, index);
        //System.out.print("index %d\n", index);
        //System.out.print("get value from constant pool = %f\n", value);
    } else {
        value = popDouble(stack);
        //System.out.print("get value from stack = %f\n", value);
    }
    return value;
}

/* opcode implementation */

/* aload_0 */
 int op_aload_0(char[][] opCode, VmStackFrame stack, VmCP p)
{
    pushInt(stack, 0);
    System.out.print("push 0 into stack\n");
    //opCode = opCode + 1;
    return 0;
}

/* bipush */
 int op_bipush( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = opCode[0][1];
    pushInt(stack, value);
    //System.out.print("push a byte %d onto the stack \n", value);
    //opCode = opCode + 2;
    return 0;
}
 int EntryToInt(StackEntry entry){
    return 0;
}
/* dup */
 int op_dup(char opCode, VmStackFrame stack, VmCP p)
{
    StackEntry entry = popEntry(stack);
    int value = 0;
    value = EntryToInt(entry);
    if (entry.type == STACK_ENTRY_INT) {
        pushInt(stack, value);
        pushInt(stack, value);
    } else {
        pushRef(stack, value);
        pushRef(stack, value);
    }
    System.out.print("dup\n");
    //opCode = opCode + 1;
    return 0;
}

/* get */
 int op_get( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int field_index ;
     char[] tmp=new char[2];
    tmp[0] = opCode[0][1];
    tmp[1] = opCode[0][2];
    field_index = tmp[0] << 8 | tmp[1];
    //System.out.print("get %d\n", field_index);
    pushRef(stack, field_index);
    return 0;
}

/* iadd */
 int op_iadd(char opCode, VmStackFrame stack, VmCP p)
{
    int value1 = popInt(stack);
    int value2 = popInt(stack);
    int result = 0;
    result = value1 + value2;
    //System.out.print("iadd: %d + %d = %d\n", value1, value2, result);
    pushInt(stack, result);
    return 0;
}

/* iconst_0 */
 int op_iconst_0( char opCode, VmStackFrame stack, VmCP p)
{
    pushInt(stack, 0);
    System.out.print("iconst_0: push 0 into stack\n");
    return 0;
}

/* iconst_1 */
 int op_iconst_1( char opCode, VmStackFrame stack, VmCP p)
{
    pushInt(stack, 1);
    System.out.print("iconst_1: push 1 into stack\n");
    return 0;
}

/* iconst_2 */
 int op_iconst_2( char opCode, VmStackFrame stack, VmCP p)
{
    pushInt(stack, 2);
    System.out.print("iconst_2: push 1 into stack\n");
    return 0;
}

/* iconst_3 */
 int op_iconst_3( char opCode, VmStackFrame stack, VmCP p)
{
    pushInt(stack, 3);
    System.out.print("iconst_3: push 1 into stack\n");
    return 0;
}

/* iconst_4 */
 int op_iconst_4( char opCode, VmStackFrame stack, VmCP p)
{
    pushInt(stack, 4);
    System.out.print("iconst_4: push 1 into stack\n");
    return 0;
}

/* iconst_5 */
 int op_iconst_5( char opCode, VmStackFrame stack, VmCP p)
{
    pushInt(stack, 5);
    System.out.print("iconst_5: push 5 into stack\n");
    return 0;
}

/* 0x0F dconst_1 */
 int op_dconst_1( char[][] opCode, VmStackFrame stack, VmCP p)
{
    pushDouble(stack, 1.0f);
    System.out.print("iconst_5: push 1.0f into stack\n");
    return 0;
}

/* idiv */
 int op_idiv( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value2 = popInt(stack);
    int value1 = popInt(stack);
    int result = 0;
    result = value1 / value2;
    //System.out.print("idiv: %d / %d = %d\n", value1, value2, result);
    pushInt(stack, result);
    return 0;
}

/* iload */
 int op_iload( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int index = opCode[0][1];
    int value = localVariables.integer[index];
    //System.out.print("iload: load value from local variable %d(%d)\n", index, localVariables.integer[index]);
    pushInt(stack, value);
    return 0;
}

/* iload_1 */
 int op_iload_1( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = localVariables.integer[1];
    //System.out.print("iload_1: load value from local variable 1(%d)\n", localVariables.integer[1]);
    pushInt(stack, value);
    return 0;
}

/* iload_2 */
 int op_iload_2( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = localVariables.integer[2];
    //System.out.print("iload_2: load value from local variable 2(%d)\n", localVariables.integer[2]);
    pushInt(stack, value);
    return 0;
}

/* iload_3 */
 int op_iload_3( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = localVariables.integer[3];
    //System.out.print("iload_3: load value from local variable 3(%d)\n", localVariables.integer[3]);
    pushInt(stack, value);
    return 0;
}

/* imul */
 int op_imul( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value1 = popInt(stack);
    int value2 = popInt(stack);
    int result = 0;
    result = value1 * value2;
   // System.out.print("imul: %d * %d = %d\n", value1, value2, result);
    pushInt(stack, result);
    return 0;
}

/* 0x63 dadd */
 int op_dadd( char[][] opCode, VmStackFrame stack, VmCP p)
{
    double value1 = get_double_parameter(stack, p);
    double value2 = get_double_parameter(stack, p);
    double result = 0;
    result = value1 + value2;
    //System.out.print("dadd: %f + %f = %f\n", value1, value2, result);
    pushDouble(stack, result);
    return 0;
}

/* 0x6B dmul */
 int op_dmul( char[][] opCode, VmStackFrame stack, VmCP p)
{
    double value1 = get_double_parameter(stack, p);
    double value2 = get_double_parameter(stack, p);
    double result = 0;
    result = value1 * value2;
 //   System.out.print("dmul: %f * %f = %f\n", value1, value2, result);
    pushDouble(stack, result);
    return 0;
}

/* 0x8e d2i */
 int op_d2i( char[][] opCode, VmStackFrame stack, VmCP p)
{
    double value1 = popDouble(stack);
    int result = 0;
    result = (int)value1;
  //  System.out.print("d2i: %d <-- %f\n", result, value1);
    pushInt(stack, result);
    return 0;
}

/* irem */
 int op_irem( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value1 = popInt(stack);
    int value2 = popInt(stack);
    int result = 0;
    result = value2 % value1;
 //   System.out.print("irem: %d mod %d = %d\n", value2, value1, result);
    pushInt(stack, result);
    return 0;
}

/* istore */
 int op_istore( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = popInt(stack);
    int index = opCode[0][1];
  //  System.out.print("istore: store value into local variable %d(%d)\n", index, value);
    localVariables.integer[index] = value;
    return 0;
}

/* istore_1 */
 int op_istore_1( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = popInt(stack);
  //  System.out.print("istore_1: store value into local variable 1(%d)\n", value);
    localVariables.integer[1] = value;
    return 0;
}

/* istore_2 */
 int op_istore_2( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = popInt(stack);
  //  System.out.print("istore_2: store value into local variable 2(%d)\n", value);
    localVariables.integer[2] = value;
    return 0;
}

/* istore_3 */
 int op_istore_3( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value = popInt(stack);
  //  System.out.print("istore_3: store value into local variable 3(%d)\n", value);
    localVariables.integer[3] = value;
    return 0;
}

/* isub */
 int op_isub( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int value2 = popInt(stack);
    int value1 = popInt(stack);
    int result = 0;
    result = value1 - value2;
  //  System.out.print("isub : %d - %d = %d\n", value1, value2, result);
    pushInt(stack, result);
    return 0;
}

/* invokespecial */
 int op_invokespecial( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int method_index;
     char []tmp=new char[2];
    tmp[0] = opCode[0][1];
    tmp[1] = opCode[0][2];
    method_index = tmp[0] << 8 | tmp[1];
   // System.out.print("call method_index %d\n", method_index);
    if (method_index < simpleMethodPool.method_used) {
        MethodInfo method = simpleMethodPool.method[method_index];
        executeMethod(method, stackFrame, simpleConstantPool);
    }
    return 0;
}

 String clzNamePrint = "java/io/PrintStream";
 String clzNameStrBuilder = "java/lang/StringBuilder";
 char[] stringBuilderBuffer = new char[1024];
 int stringBuilderUsed = 0;

/* 0xb8 invoke */
 int op_invoke( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int method_index ;
     char[] tmp=new char[2];
    char[] method_name=new char[255];
    char[] clsName=new char[255];
    char[] method_type=new char[255];
    tmp[0] = opCode[0][1];
    tmp[1] = opCode[0][2];
    method_index = tmp[0] << 8 | tmp[1];
   // System.out.print("invoke method_index %d\n", method_index);
    //System.out.print("simpleMethodPool.method_used = %d\n", simpleMethodPool.method_used);
    if (method_index < simpleMethodPool.method_used) {
        MethodInfo method = simpleMethodPool.method[method_index];
        memset(method_name, 0, 255);
        getUTF8String(p, method.name_index, 255, method_name);
        //System.out.print(" method name = %s\n", method_name);
    } else {
        ConstantMethodRef mRef = findMethodRef(p, method_index);
        if (mRef !=null) {
            ConstantClassRef clasz = findClassRef(p, mRef.classIndex);
            ConstantNameAndType nat = findNameAndType(p, mRef.nameAndTypeIndex);
            if (clasz == null || nat == null) return -1;
            getUTF8String(p, clasz.stringIndex, 255, clsName);
            getUTF8String(p, nat.nameIndex, 255, method_name);
            getUTF8String(p, nat.typeIndex, 255, method_type);

           /* System.out.print("call class %s\n", clsName);
            System.out.print("call method %s\n", method_name);
            System.out.print("call method type %s\n", method_type);*/
	    boolean ret =
            invoke_java_lang_library(stack, p,
                                     clsName, method_name, method_type);
            if (ret) {
                System.out.print("invoke java lang library successful\n");
            }
        }
    }

    return 0;
}

/* invokevirtual */
 int op_invokevirtual( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int object_ref;
     char []tmp =new char[2];
    char[] clsName=new char[255];
    char[] utf8=new char[255];
    int len = 0;
    tmp[0] = opCode[0][1];
    tmp[1] = opCode[0][2];
    object_ref = tmp[0] << 8 | tmp[1];
    //System.out.print("call object_ref %d\n", object_ref);
    ConstantMethodRef mRef = findMethodRef(p, object_ref);
    if (mRef != null) {
        ConstantClassRef clasz = findClassRef(p, mRef.classIndex);
        ConstantNameAndType nat = findNameAndType(p, mRef.nameAndTypeIndex);
        if (clasz == null || nat == null) return -1;
        getUTF8String(p, clasz.stringIndex, 255, clsName);
        //System.out.print("call object ref class %s\n", clsName);
        if (strcmp(clzNamePrint, clsName) == 0) {
            StackEntry entry = popEntry(stack);
            int index = EntryToInt(entry);
            //System.out.print("call Println with index = %d\n", index);
            if (entry.type == STACK_ENTRY_REF) {
                ConstantStringRef strRef = findStringRef(p, index);
                if (strRef != null) {
                    getUTF8String(p, strRef.stringIndex, 255, utf8);
                    len = strlen(utf8);
                    //memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                    stringBuilderUsed += len;
                    stringBuilderBuffer[stringBuilderUsed] = 0;
                }
            } else if (entry.type == STACK_ENTRY_INT) {
                //System.out.print(utf8, "%d", index);
                len = strlen(utf8);
               // memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                stringBuilderUsed += len;
                stringBuilderBuffer[stringBuilderUsed] = 0;
            }
            // System.out.print out the result
            //System.out.print("%s\n", stringBuilderBuffer);
            memset(stringBuilderBuffer, 0, 1024);
            stringBuilderUsed = 0;
        } else if (strcmp(clzNameStrBuilder, clsName) == 0) {
            StackEntry entry = popEntry(stack);
            int index = EntryToInt(entry);
            //System.out.print("call StringBuilder with index = %d\n", index);
            if (entry.type == STACK_ENTRY_REF) {
                ConstantStringRef strRef = findStringRef(p, index);
                if (strRef != null) {
                    getUTF8String(p, strRef.stringIndex, 255, utf8);
                    len = strlen(utf8);
                   // memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                    stringBuilderUsed += len;
                }
            } else if (entry.type == STACK_ENTRY_INT) {
                //System.out.print(utf8, "%d", index);
                len = strlen(utf8);
            //    memcpy(stringBuilderBuffer + stringBuilderUsed, utf8, len);
                stringBuilderUsed += len;
                //System.out.print("%s\n", stringBuilderBuffer);
            }

        }
    }
    return 0;
}

/* ldc */
 int op_ldc(char [][]opCode, VmStackFrame stack, VmCP p)
{
    int value = opCode[0][1];
    pushRef(stack, value);
    //System.out.print("ldc: push a constant index %d onto the stack \n", value);
    return 0;
}

/* 0x14 ldc2_w */
 int op_ldc2_w( char[][] opCode, VmStackFrame stack, VmCP p)
{
     char index1 = opCode[0][1];
     char index2 = opCode[0][2];
    int index = (index1 << 8) | index2;
    pushRef(stack, index);
    //System.out.print("ldc2_w: push a constant index %d onto the stack \n", index);
    return 0;
}

/* 0x11 op_sipush */
 int op_sipush( char[][] opCode, VmStackFrame stack, VmCP p)
{
    short value;
     char[] tmp=new char[2];
    tmp[0] = opCode[0][1];
    tmp[1] = opCode[0][2];
    value =(short) (tmp[0] << 8 | tmp[1]);
    //System.out.print("sipush value %d\n", value);
    pushInt(stack, value);
    return 0;
}

/* op_new */
 int op_new( char[][] opCode, VmStackFrame stack, VmCP p)
{
    int object_ref;
     char []tmp=new char[2];
    tmp[0] = opCode[0][1];
    tmp[1] = opCode[0][2];
    object_ref = tmp[0] << 8 | tmp[1];
    //System.out.print("new: new object_ref %d\n", object_ref);
    return 0;
}

/* return */
 int op_return( char[][] opCode, VmStackFrame stack, VmCP p)
{
    System.out.print("return: \n");
    return -1;
}
 
Function<CPU, Integer> op_aload_0 = cpu -> op_aload_0(cpu.opCode, cpu.stack, cpu.p);
Function<CPU, Integer> op_bipush = cpu -> op_bipush(cpu.opCode, cpu.stack, cpu.p);
Function<CPU, Integer> op_dup, op_get, op_iadd, op_iconst_0;
Function<CPU, Integer> op_iconst_1, op_iconst_2;
Function<CPU, Integer> op_iconst_3, op_iconst_4, op_iconst_5;
Function<CPU, Integer> op_dconst_1, op_idiv, op_imul, op_dadd, op_dmul, op_d2i;
Function<CPU, Integer> op_invokespecial, op_invokevirtual;
Function<CPU, Integer> op_invoke = cpu -> op_invoke(cpu.opCode, cpu.stack, cpu.p);
Function<CPU, Integer> op_iload, op_iload_1, op_iload_2, op_iload_3;
Function<CPU, Integer> op_istore, op_istore_1, op_istore_2, op_istore_3;
Function<CPU, Integer> op_isub, op_ldc, op_ldc2_w;
Function<CPU, Integer> op_new, op_irem, op_sipush, op_return;

    private  class StackEntry {
        int type;
        
        public StackEntry() {
        }
    }

 VmByteCode byteCodes[] = {
    new VmByteCode("aload_0"         , 0x2A, 1,  op_aload_0          ),
    new VmByteCode( "bipush"          , 0x10, 2,  op_bipush           ),
    new VmByteCode( "dup"             , 0x59, 1,  op_dup              ),
    new VmByteCode( "get"       , 0xB2, 3,  op_get        ),
    new VmByteCode( "iadd"            , 0x60, 1,  op_iadd             ),
    new VmByteCode( "iconst_0"        , 0x03, 1,  op_iconst_0         ),
    new VmByteCode( "iconst_1"        , 0x04, 1,  op_iconst_1         ),
    new VmByteCode("iconst_2"        , 0x05, 1,  op_iconst_2         ),
    new VmByteCode( "iconst_3"        , 0x06, 1,  op_iconst_3         ),
    new VmByteCode( "iconst_4"        , 0x07, 1,  op_iconst_4         ),
    new VmByteCode( "iconst_5"        , 0x08, 1,  op_iconst_5         ),
    new VmByteCode( "dconst_1"        , 0x0F, 1,  op_dconst_1         ),
    new VmByteCode( "idiv"            , 0x6C, 1,  op_idiv             ),
    new VmByteCode( "imul"            , 0x68, 1,  op_imul             ),
    new VmByteCode( "dadd"            , 0x63, 1,  op_dadd             ),
    new VmByteCode( "dmul"            , 0x6B, 1,  op_dmul             ),
    new VmByteCode( "d2i"             , 0x8e, 1,  op_d2i              ),
    new VmByteCode( "invokespecial"   , 0xB7, 3,  op_invokespecial    ),
    new VmByteCode( "invokevirtual"   , 0xB6, 3,  op_invokevirtual    ),
    new VmByteCode( "invoke"    , 0xB8, 3,  op_invoke     ),
    new VmByteCode( "iload"           , 0x15, 2,  op_iload            ),
    new VmByteCode( "iload_1"         , 0x1B, 1,  op_iload_1          ),
    new VmByteCode( "iload_2"         , 0x1C, 1,  op_iload_2          ),
    new VmByteCode( "iload_3"         , 0x1D, 1,  op_iload_3          ),
    new VmByteCode( "istore"          , 0x36, 2,  op_istore           ),
    new VmByteCode( "istore_1"        , 0x3C, 1,  op_istore_1         ),
    new VmByteCode( "istore_2"        , 0x3D, 1,  op_istore_2         ),
    new VmByteCode( "istore_3"        , 0x3E, 1,  op_istore_3         ),
    new VmByteCode( "isub"            , 0x64, 1,  op_isub             ),
    new VmByteCode( "ldc"             , 0x12, 2,  op_ldc              ),
    new VmByteCode( "ldc2_w"          , 0x14, 3,  op_ldc2_w           ),
    new VmByteCode( "new"             , 0xBB, 3,  op_new              ),
    new VmByteCode( "irem"            , 0x70, 1,  op_irem             ),
    new VmByteCode( "sipush"          , 0x11, 3,  op_sipush           ),
    new VmByteCode( "return"          , 0xB1, 1,  op_return           )
};

 Function findOpCodeFunc( char op)
{
    int i;
    for (i = 0; i < byteCodes.length; i++)
        if (op == byteCodes[i].opCode)
            return byteCodes[i].func;
    return null;
}

 int findOpCodeOffset( char op)
{
    int i;
    for (i = 0; i < byteCodes.length ; i++)
        if (op == byteCodes[i].opCode)
            return byteCodes[i].offset;
    return 0;
}

 int convertToCodeAttribute(CodeAttribute ca, AttributeInfo attr)
{
    int info_p = 0;
     char []tmp=new char[4];
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
    //ca.code = ( char ) malloc(sizeof( char)  ca.code_length);
    //memcpy(ca.code, attr.info[info_p], ca.code_length);
    return 0;
}

int executeMethod(MethodInfo startup, VmStackFrame stack, VmCP p)
{
    int i = 0;
    int j = 0;
    char []name=new char[255];
    CodeAttribute ca =null;
    //memset(ca, 0 , sizeof(CodeAttribute));
    for (j = 0 ; j < startup.attributes_count ; j++) {
        convertToCodeAttribute(ca, startup.attributes[j]);
        getUTF8String(p, ca.attribute_name_index, 255, name);
        if (memcmp(name, "Code", 4) != 0) continue;
        System.out.print("----------------------------------------\n");
        System.out.print("code dump\n");
        printCodeAttribute(ca, p);
        System.out.print("----------------------------------------\n");
         char[][] pc = ca.code;
        if (run == 0)
            System.exit(1);
        do {
            Function func = findOpCodeFunc(pc[0][0]);
            if (func != null) {
                i = (Integer)func.apply(new CPU(pc , stackFrame, p));
            }
            if (i < 0) break;
        } while (true);
    }
    return 0;
}

 String findOpCode( char op)
{
    int i;
    for (i = 0; i < byteCodes.length ; i++)
        if (op == byteCodes[i].opCode)
            return byteCodes[i].name;
    return null;
}

    void printCodeAttribute(CodeAttribute ca, VmCP p)
   {
       int i = 0;
       int tmp = 0;
       char []name=new char[255];
       getUTF8String(p, ca.attribute_name_index, 255, name);
       //System.out.print("attribute name : %s\n", name);
       //System.out.print("attribute length: %d\n", ca.attribute_length);

   //    System.out.print("max_stack: %d\n", ca.max_stack);
     //  System.out.print("max_locals: %d\n", ca.max_locals);
      // System.out.print("code_length: %d\n", ca.code_length);
        char[][] pc = ca.code;
       i = 0;
       do {
           String opName = findOpCode(pc[0][0]);
           if (opName == null) {
               //System.out.print("Unknow OpCode %02X\n", pc[0]);
               System.exit(1);
           }
           //System.out.print("%s \n", opName);
           tmp = findOpCodeOffset(pc[0][0]);
           //pc += tmp;
           i += tmp;
       } while (i < ca.code_length);
   }
}
