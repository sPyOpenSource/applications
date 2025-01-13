
package jCPU.JavaVM;

import jCPU.JavaVM.vm.VmOpcode;
import jCPU.JavaVM.vm.VmCP;
import jCPU.JavaVM.vm.VmConstMethodRef;
import jCPU.JavaVM.vm.VmMethod;

import java.util.function.Function;
import j51.intel.Code;

/**
 *
 * @author X. Wang
 */
public class ByteCode {
    private Code code;
    private VmCP cp;
    
    static Function<JVM, Integer> op_aload_0 = cpu -> cpu.handler.op_aload_0();
    static Function<JVM, Integer> op_bipush;// = cpu -> cpu.handler.op_bipush(cpu.opCode);
    static Function<JVM, Integer> op_dup, op_get, op_iadd, op_iconst_0;
    static Function<JVM, Integer> op_iconst_1, op_iconst_2;
    static Function<JVM, Integer> op_iconst_3, op_iconst_4, op_iconst_5;
    static Function<JVM, Integer> op_dconst_1, op_idiv, op_imul, op_dadd, op_dmul, op_d2i;
    static Function<JVM, Integer> op_invokespecial, op_invokevirtual;
    static Function<JVM, Integer> op_invoke;// = cpu -> op_invoke(cpu.opCode, cpu.stack, cpu.cp);
    static Function<JVM, Integer> op_iload, op_iload_1, op_iload_2, op_iload_3;
    static Function<JVM, Integer> op_istore, op_istore_1, op_istore_2, op_istore_3;
    static Function<JVM, Integer> op_isub, op_ldc, op_ldc2_w;
    static Function<JVM, Integer> op_new, op_irem, op_sipush, op_return;

    public static VmOpcode byteCodes[] = {
        new VmOpcode( "bipush"        , 0x10, 2, op_bipush        ),
        new VmOpcode( "sipush"        , 0x11, 3, op_sipush        ),
        new VmOpcode( "get"           , 0xB2, 3, op_get           ),
        new VmOpcode( "iconst_0"      , 0x03, 1, op_iconst_0      ),
        new VmOpcode( "iconst_1"      , 0x04, 1, op_iconst_1      ),
        new VmOpcode( "iconst_2"      , 0x05, 1, op_iconst_2      ),
        new VmOpcode( "iconst_3"      , 0x06, 1, op_iconst_3      ),
        new VmOpcode( "iconst_4"      , 0x07, 1, op_iconst_4      ),
        new VmOpcode( "iconst_5"      , 0x08, 1, op_iconst_5      ),
        new VmOpcode( "dconst_1"      , 0x0F, 1, op_dconst_1      ),
        new VmOpcode( "isub"          , 0x64, 1, op_isub          ),
        new VmOpcode( "iadd"          , 0x60, 1, op_iadd          ),
        new VmOpcode( "idiv"          , 0x6C, 1, op_idiv          ),
        new VmOpcode( "imul"          , 0x68, 1, op_imul          ),
        new VmOpcode( "irem"          , 0x70, 1, op_irem          ),
        new VmOpcode( "dadd"          , 0x63, 1, op_dadd          ),
        new VmOpcode( "dmul"          , 0x6B, 1, op_dmul          ),
        new VmOpcode( "d2i"           , 0x8e, 1, op_d2i           ),
        new VmOpcode( "invokespecial" , 0xB7, 3, op_invokespecial ),
        new VmOpcode( "invokevirtual" , 0xB6, 3, op_invokevirtual ),
        new VmOpcode( "invokestatic"  , 0xB8, 3, op_invoke        ),
        new VmOpcode( "aload_0"       , 0x2A, 1, op_aload_0       ),
        new VmOpcode( "iload"         , 0x15, 2, op_iload         ),
        new VmOpcode( "iload_1"       , 0x1B, 1, op_iload_1       ),
        new VmOpcode( "iload_2"       , 0x1C, 1, op_iload_2       ),
        new VmOpcode( "iload_3"       , 0x1D, 1, op_iload_3       ),
        new VmOpcode( "istore"        , 0x36, 2, op_istore        ),
        new VmOpcode( "istore_1"      , 0x3C, 1, op_istore_1      ),
        new VmOpcode( "istore_2"      , 0x3D, 1, op_istore_2      ),
        new VmOpcode( "istore_3"      , 0x3E, 1, op_istore_3      ),
        new VmOpcode( "ldc"           , 0x12, 2, op_ldc           ),
        new VmOpcode( "ldc2_w"        , 0x14, 3, op_ldc2_w        ),
        new VmOpcode( "new"           , 0xBB, 3, op_new           ),
        new VmOpcode( "dup"           , 0x59, 1, op_dup           ),
        new VmOpcode( "return"        , 0xB1, 1, op_return        )
    };

    public ByteCode(Code code) {
        this.code = code;
    }

    public Code getBytecode() {
        return code;
    }

    public VmCP getCP() {
        return cp;
    }

    public VmMethod getMethod() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public VmConstMethodRef findMethodRef(int method_index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getUTF8String(int stringIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ConstantStringRef findStringRef(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ConstantNameAndType findNameAndType(int nameAndTypeIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ConstantClassRef findClassRef(int classIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /*
     * Simple Java Virtual Machine Implementation
     *
     * Copyright (C) 2014 Jim Huang <jserv.tw@gmail.com>
     * Copyright (C) 2013 Chun-Yu Wang <wicanr2@gmail.com>
     */
    
    public class ConstantNameAndType{
        int nameIndex, typeIndex;
        public ConstantNameAndType(){

        }
    }

    public class ConstantStringRef{
        public int stringIndex;
        public ConstantStringRef(){

        }
    }

    public class ConstantClassRef{
        int stringIndex, classIndex;
        public ConstantClassRef(){

        }
    }

    public class CodeAttribute{
        public char[] code;
        int code_length, attribute_name_index, max_stack, max_locals, attribute_length;
        public CodeAttribute(){

        }
    }

    public class MethodInfo{
        int attributes_count;
        public AttributeInfo[] attributes;
        public int name_index;
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

    public CodeAttribute convertToCodeAttribute(AttributeInfo attr)
    {
        int info_p = 0;
        char[] tmp = new char[4];
        CodeAttribute ca = new CodeAttribute();
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
        return ca;
    }

    public static VmOpcode findOpCode(char op)
    {
        for (VmOpcode byteCode : byteCodes) {
            if (op == byteCode.getOpcode()) {
                return byteCode;
            }
        }
        return null;
    }

    void printCodeAttribute(CodeAttribute ca, VmCP cp)
    {
       String name = getUTF8String(ca.attribute_name_index);
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
