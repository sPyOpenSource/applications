
package jCPU.JavaVM;

import java.util.function.Function;
import j51.intel.Code;
import jCPU.JavaVM.vm.AttributeInfo;

import jx.classfile.CodeData;
import jx.classfile.MethodData;
import jx.classfile.constantpool.ClassCPEntry;
import jx.classfile.constantpool.ConstantPool;
import jx.classfile.constantpool.MethodRefCPEntry;
import jx.classfile.constantpool.NameAndTypeCPEntry;
import jx.classfile.constantpool.StringCPEntry;

/**
 *
 * @author X. Wang
 */
public class ByteCode implements jx.zero.ByteCode {
    private Code code;
    public static ConstantPool cp;
    
    static Function<JVM, Integer> op_aload_0 = cpu -> cpu.handler.op_aload_0();
    static Function<JVM, Integer> op_bipush = cpu -> cpu.handler.op_bipush(cpu.opCode);
    static Function<JVM, Integer> op_dup = cpu -> cpu.handler.op_dup(), 
            op_get = cpu -> cpu.handler.op_get(cpu.opCode), 
            op_iadd = cpu -> cpu.handler.op_iadd(), 
            op_iconst_0 = cpu -> cpu.handler.op_iconst_0(),
            op_iconst_1 = cpu -> cpu.handler.op_iconst_1(), 
            op_iconst_2 = cpu -> cpu.handler.op_iconst_2(),
            op_iconst_3 = cpu -> cpu.handler.op_iconst_3(), 
            op_iconst_4 = cpu -> cpu.handler.op_iconst_4(), 
            op_iconst_5 = cpu -> cpu.handler.op_iconst_5(),
            op_dconst_1 = cpu -> cpu.handler.op_dconst_1(), 
            op_idiv = cpu -> cpu.handler.op_idiv(), 
            op_imul = cpu -> cpu.handler.op_imul(), 
            op_dadd = cpu -> cpu.handler.op_dadd(cp), 
            op_dmul = cpu -> cpu.handler.op_dmul(cp), 
            op_d2i = cpu -> cpu.handler.op_d2i();
    static Function<JVM, Integer> op_invokespecial = cpu -> cpu.op_invokespecial(), 
            op_invokevirtual = cpu -> cpu.op_invokevirtual(cp),
            op_invoke = cpu -> cpu.op_invoke(cp);
    static Function<JVM, Integer> op_iload = cpu -> cpu.handler.op_iload(cpu.opCode), 
            op_iload_1 = cpu -> cpu.handler.op_iload_1(), 
            op_iload_2 = cpu -> cpu.handler.op_iload_2(), 
            op_iload_3 = cpu -> cpu.handler.op_iload_3();
    static Function<JVM, Integer> op_istore = cpu -> cpu.handler.op_istore(cpu.opCode), 
            op_istore_1 = cpu -> cpu.handler.op_istore_1(), 
            op_istore_2 = cpu -> cpu.handler.op_istore_2(), 
            op_istore_3 = cpu -> cpu.handler.op_istore_3();
    static Function<JVM, Integer> op_isub = cpu -> cpu.handler.op_isub(), 
            op_ldc = cpu -> cpu.handler.op_ldc(cpu.opCode), 
            op_ldc2_w = cpu -> cpu.handler.op_ldc2_w(cpu.opCode),
            op_new = cpu -> cpu.handler.op_new(cpu.opCode), 
            op_irem = cpu -> cpu.handler.op_irem(), 
            op_sipush = cpu -> cpu.handler.op_sipush(cpu.opCode), 
            op_return = cpu -> cpu.handler.op_return();

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

    public MethodData getMethod() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public MethodRefCPEntry findMethodRef(int method_index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getUTF8String(int stringIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public StringCPEntry findStringRef(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public NameAndTypeCPEntry findNameAndType(int nameAndTypeIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ClassCPEntry findClassRef(int classIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /*
     * Simple Java Virtual Machine Implementation
     *
     * Copyright (C) 2014 Jim Huang <jserv.tw@gmail.com>
     * Copyright (C) 2013 Chun-Yu Wang <wicanr2@gmail.com>
     */

    public CodeData convertToCodeAttribute(AttributeInfo attr)
    {
        int info_p = 0;
        char[] tmp = new char[4];
        CodeData ca = new CodeData();
        //ca.attribute_name_index = attr.attribute_name_index;
        //ca.attribute_length = attr.attribute_length;
        tmp[0] = attr.info[info_p++];
        tmp[1] = attr.info[info_p++];
        int max_stack = tmp[0] << 8 | tmp[1];
        tmp[0] = attr.info[info_p++];
        tmp[1] = attr.info[info_p++];
        int max_locals = tmp[0] << 8 | tmp[1];
        tmp[0] = attr.info[info_p++];
        tmp[1] = attr.info[info_p++];
        tmp[2] = attr.info[info_p++];
        tmp[3] = attr.info[info_p++];
        int code_length = tmp[0] << 24 | tmp[1] << 16 | tmp[2] << 8 | tmp[3];
        byte[] code = new byte[code_length];
        System.arraycopy(code, 0, attr.info, info_p, code_length);
        //return new CodeData(max_stack, max_locals, code, null, null);
        return ca;
    }

    public static VmOpcode findOpCode(char op)
    {
        for (VmOpcode byteCode : byteCodes) {
            if (op == byteCode.getOpcode()) {
                return byteCode;
            }
        }
        return new VmOpcode(OPNAMES[op], op, OPNUMARGS[op] + 1, null);
    }

    void printCodeAttribute(CodeData ca, ConstantPool cp)
    {
       String name;// = getUTF8String(ca.attribute_name_index);
       //System.out.println("attribute name : " + name);
       //System.out.println("attribute length: " + ca.attribute_length);

       //System.out.println("max_stack: " + ca.max_stack);
       //System.out.println("max_locals: " + ca.max_locals);
       //System.out.println("code_length: " + ca.code_length);
       byte[] pc = ca.getBytecode();
       int i = 0;
       do {
           String opName = findOpCode((char)pc[0]).getDescription();
           if (opName == null) {
               //System.out.print("Unknow OpCode %02X\n", pc[0]);
               System.exit(1);
           }
           //System.out.print("%s \n", opName);
           i += findOpCode((char)pc[0]).getLength();
           //pc += tmp;
       } while (i < pc.length);
   }

    @Override
    public jx.zero.ByteCode getPrev() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public jx.zero.ByteCode getNext() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isTarget() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int mvCheckCount() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int svCheckCount() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getOpCode() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public jx.zero.ByteCode[] getTargets() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public jx.zero.ByteCode[] getSources() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void mvCheckCount(int i) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getAddress() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void svCheckCount(int i) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
