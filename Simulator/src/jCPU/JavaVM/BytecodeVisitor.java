/*
 * $Id$
 *
 * Copyright (C) 2003-2015 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package jCPU.JavaVM;

import jCPU.JavaVM.vm.LocalVariables;
import jx.classfile.MethodData;
import jx.classfile.constantpool.ClassCPEntry;
import jx.classfile.constantpool.ConstantPool;
import jx.classfile.constantpool.FieldRefCPEntry;
import jx.classfile.constantpool.InterfaceMethodRefCPEntry;
import jx.classfile.constantpool.MethodRefCPEntry;
import jx.classfile.constantpool.StringCPEntry;

/**
 * <description>
 *
 * @author epr
 */
public abstract class BytecodeVisitor {
    private VmStackFrame stack;
    private ConstantPool cp;
    private LocalVariables localVariables;

    public abstract void setParser(BytecodeParser parser);

    public abstract void startMethod(MethodData method);

    public abstract void endMethod();

    public abstract void startInstruction(int address);

    public abstract void endInstruction();

    public abstract void visit_nop();

    public abstract void visit_aconst_null();

    public abstract void visit_iconst(int value);

    public abstract void visit_lconst(long value);

    // -- 10 --
    public abstract void visit_fconst(float value);

    public abstract void visit_dconst(double value);

    /**
     * @deprecated
     */
    public final void visit_sipush(short value) {
    }

    public abstract void visit_ldc(StringCPEntry value);

    public abstract void visit_ldc(ClassCPEntry value);

    // -- 20 --
    public abstract void visit_iload(int index);

    public abstract void visit_lload(int index);

    public abstract void visit_fload(int index);

    public abstract void visit_dload(int index);

    public abstract void visit_aload(int index);

    // -- 30 --
    public abstract void visit_iaload();

    public abstract void visit_laload();

    public abstract void visit_faload();

    public abstract void visit_daload();

    // -- 50 --
    public abstract void visit_aaload();

    public abstract void visit_baload();

    public abstract void visit_caload();

    public abstract void visit_saload();

    public abstract void visit_istore(int index);

    public abstract void visit_lstore(int index);

    public abstract void visit_fstore(int index);

    public abstract void visit_dstore(int index);

    public abstract void visit_astore(int index);

    public abstract void visit_iastore();

    // -- 80 --
    public abstract void visit_lastore();

    public abstract void visit_fastore();

    public abstract void visit_dastore();

    public abstract void visit_aastore();

    public abstract void visit_bastore();

    public abstract void visit_castore();

    public abstract void visit_sastore();

    public abstract void visit_pop();

    public abstract void visit_pop2();

    public abstract void visit_dup();

    // -- 90 --
    public abstract void visit_dup_x1();

    public abstract void visit_dup_x2();

    public abstract void visit_dup2();

    public abstract void visit_dup2_x1();

    public abstract void visit_dup2_x2();

    public abstract void visit_swap();

    public abstract void visit_iadd();

    public abstract void visit_ladd();

    public abstract void visit_fadd();

    public abstract void visit_dadd();

    // -- 100 --
    public abstract void visit_isub();

    public abstract void visit_lsub();

    public abstract void visit_fsub();

    public abstract void visit_dsub();

    public abstract void visit_imul();

    public abstract void visit_lmul();

    public abstract void visit_fmul();

    public abstract void visit_dmul();

    public abstract void visit_idiv();

    public abstract void visit_ldiv();

    // -- 110 --
    public abstract void visit_fdiv();

    public abstract void visit_ddiv();

    public abstract void visit_irem();

    public abstract void visit_lrem();

    public abstract void visit_frem();

    public abstract void visit_drem();

    public abstract void visit_ineg();

    public abstract void visit_lneg();

    public abstract void visit_fneg();

    public abstract void visit_dneg();

    // -- 120 --
    public abstract void visit_ishl();

    public abstract void visit_lshl();

    public abstract void visit_ishr();

    public abstract void visit_lshr();

    public abstract void visit_iushr();

    public abstract void visit_lushr();

    public abstract void visit_iand();

    public abstract void visit_land();

    public abstract void visit_ior();

    public abstract void visit_lor();

    // -- 130 --
    public abstract void visit_ixor();

    public abstract void visit_lxor();

    public abstract void visit_iinc(int index, int incValue);

    public abstract void visit_i2l();

    public abstract void visit_i2f();

    public abstract void visit_i2d();

    public abstract void visit_l2i();

    public abstract void visit_l2f();

    public abstract void visit_l2d();

    public abstract void visit_f2i();

    // -- 140 --
    public abstract void visit_f2l();

    public abstract void visit_f2d();

    public abstract void visit_d2i();

    public abstract void visit_d2l();

    public abstract void visit_d2f();

    public abstract void visit_i2b();

    public abstract void visit_i2c();

    public abstract void visit_i2s();

    public abstract void visit_lcmp();

    public abstract void visit_fcmpl();

    // -- 150 --
    public abstract void visit_fcmpg();

    public abstract void visit_dcmpl();

    public abstract void visit_dcmpg();

    public abstract void visit_ifeq(int address);

    public abstract void visit_ifne(int address);

    public abstract void visit_iflt(int address);

    public abstract void visit_ifge(int address);

    public abstract void visit_ifgt(int address);

    public abstract void visit_ifle(int address);

    public abstract void visit_if_icmpeq(int address);

    // -- 160 --
    public abstract void visit_if_icmpne(int address);

    public abstract void visit_if_icmplt(int address);

    public abstract void visit_if_icmpge(int address);

    public abstract void visit_if_icmpgt(int address);

    public abstract void visit_if_icmple(int address);

    public abstract void visit_if_acmpeq(int address);

    public abstract void visit_if_acmpne(int address);

    public abstract void visit_goto(int address);

    public abstract void visit_jsr(int address);

    public abstract void visit_ret(int index);

    // -- 170 --
    public abstract void visit_tableswitch(int defValue, int lowValue, int highValue, int[] addresses);

    public abstract void visit_lookupswitch(int defValue, int[] matchValues, int[] addresses);

    public abstract void visit_ireturn();

    public abstract void visit_lreturn();

    public abstract void visit_freturn();

    public abstract void visit_dreturn();

    public abstract void visit_areturn();

    public abstract void visit_return();

    public abstract void visit_getstatic(FieldRefCPEntry fieldRef);

    public abstract void visit_putstatic(FieldRefCPEntry fieldRef);

    // -- 180 --
    public abstract void visit_getfield(FieldRefCPEntry fieldRef);

    public abstract void visit_putfield(FieldRefCPEntry fieldRef);

    public abstract void visit_invokevirtual(MethodRefCPEntry methodRef);

    public abstract void visit_invokespecial(MethodRefCPEntry methodRef);

    public abstract void visit_invokestatic(MethodRefCPEntry methodRef);

    public abstract void visit_invokeinterface(InterfaceMethodRefCPEntry methodRef, int count);

    public abstract void visit_new(ClassCPEntry clazz);

    public abstract void visit_newarray(int type);

    public abstract void visit_anewarray(ClassCPEntry clazz);

    // -- 190 --
    public abstract void visit_arraylength();

    public abstract void visit_athrow();

    public abstract void visit_checkcast(ClassCPEntry clazz);

    public abstract void visit_instanceof(ClassCPEntry clazz);

    public abstract void visit_monitorenter();

    public abstract void visit_monitorexit();

    public abstract void visit_multianewarray(ClassCPEntry clazz, int dimensions);

    public abstract void visit_ifnull(int address);

    public abstract void visit_ifnonnull(int address);
    
    /* ldc */
    int op_ldc(char[] opCode)
    {
        int value = opCode[1];
        stack.push(value);
        //System.out.print("ldc: push a constant index %d onto the stack \n", value);
        return 0;
    }

    /* 0x14 ldc2_w */
    int op_ldc2_w(char[] opCode)
    {
        char index1 = opCode[1];
        char index2 = opCode[2];
        int index = (index1 << 8) | index2;
        stack.push(index);
        //System.out.print("ldc2_w: push a constant index %d onto the stack \n", index);
        return 0;
    }

    /* 0x11 op_sipush */
    int op_sipush(char[] opCode)
    {
        short value;
        char[] tmp=new char[2];
        tmp[0] = opCode[1];
        tmp[1] = opCode[2];
        value =(short) (tmp[0] << 8 | tmp[1]);
        //System.out.print("sipush value %d\n", value);
        stack.push(value);
        return 0;
    }

    /* op_new */
    int op_new(char[] opCode)
    {
        char []tmp = new char[2];
        tmp[0] = opCode[1];
        tmp[1] = opCode[2];
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

    /* opcode implementation */

    /* aload_0 */
    int op_aload_0()
    {
        stack.push(0);
        System.out.print("push 0 into stack\n");
        return 0;
    }

    /* bipush */
    int op_bipush(char[] opCode)
    {
        int value = opCode[1];
        stack.push(value);
        //System.out.print("push a byte %d onto the stack \n", value);
        return 0;
    }

    /* dup */
    int op_dup()
    {
        VmStackEntry entry = stack.pop();
        int value = entry.getInt();
        if (entry.type == VmStackFrame.STACK_ENTRY_INT) {
            stack.push(value);
            stack.push(value);
        } else {
            stack.push(value);
            stack.push(value);
        }
        System.out.print("dup\n");
        return 0;
    }

    /* get */
    int op_get(char[] opCode)
    {
        int field_index;
        char[] tmp = new char[2];
        tmp[0] = opCode[1];
        tmp[1] = opCode[2];
        field_index = tmp[0] << 8 | tmp[1];
        //System.out.print("get %d\n", field_index);
        stack.push(field_index);
        return 0;
    }

    /* iadd */
    int op_iadd()
    {
        int value1 = stack.pop().getInt();
        int value2 = stack.pop().getInt();
        int result = value1 + value2;
        //System.out.print("iadd: %d + %d = %d\n", value1, value2, result);
        stack.push(result);
        return 0;
    }

    /* iconst_0 */
    int op_iconst_0()
    {
        stack.push(0);
        System.out.print("iconst_0: push 0 into stack\n");
        return 0;
    }

    /* iconst_1 */
    int op_iconst_1()
    {
        stack.push(1);
        System.out.print("iconst_1: push 1 into stack\n");
        return 0;
    }

    /* iconst_2 */
    int op_iconst_2()
    {
        stack.push(2);
        System.out.print("iconst_2: push 1 into stack\n");
        return 0;
    }

    /* iconst_3 */
    int op_iconst_3()
    {
        stack.push(3);
        System.out.print("iconst_3: push 1 into stack\n");
        return 0;
    }

    /* iconst_4 */
    int op_iconst_4()
    {
        stack.push(4);
        System.out.print("iconst_4: push 1 into stack\n");
        return 0;
    }

    /* iconst_5 */
    int op_iconst_5()
    {
        stack.push(5);
        System.out.print("iconst_5: push 5 into stack\n");
        return 0;
    }

    /* 0x0F dconst_1 */
    int op_dconst_1()
    {
        stack.push(1.0f);
        System.out.print("iconst_5: push 1.0f into stack\n");
        return 0;
    }

    /* idiv */
    int op_idiv()
    {
        int value2 = stack.pop().getInt();
        int value1 = stack.pop().getInt();
        int result = value1 / value2;
        //System.out.print("idiv: %d / %d = %d\n", value1, value2, result);
        stack.push(result);
        return 0;
    }

    /* iload */
    int op_iload(char[] opCode)
    {
        int index = opCode[1];
        int value = localVariables.integer[index];
        //System.out.print("iload: load value from local variable %d(%d)\n", index, localVariables.integer[index]);
        stack.push(value);
        return 0;
    }

    /* iload_1 */
    int op_iload_1()
    {
        int value = localVariables.integer[1];
        //System.out.print("iload_1: load value from local variable 1(%d)\n", localVariables.integer[1]);
        stack.push(value);
        return 0;
    }

    /* iload_2 */
    int op_iload_2()
    {
        int value = localVariables.integer[2];
        //System.out.print("iload_2: load value from local variable 2(%d)\n", localVariables.integer[2]);
        stack.push(value);
        return 0;
    }

    /* iload_3 */
    int op_iload_3()
    {
        int value = localVariables.integer[3];
        //System.out.print("iload_3: load value from local variable 3(%d)\n", localVariables.integer[3]);
        stack.push(value);
        return 0;
    }

    /* imul */
    int op_imul()
    {
        int value1 = stack.pop().getInt();
        int value2 = stack.pop().getInt();
        int result = value1 * value2;
        // System.out.print("imul: %d * %d = %d\n", value1, value2, result);
        stack.push(result);
        return 0;
    }

    /* 0x63 dadd */
    int op_dadd()
    {
        double value1 = stack.get_double_parameter(cp);
        double value2 = stack.get_double_parameter(cp);
        double result = value1 + value2;
        //System.out.print("dadd: %f + %f = %f\n", value1, value2, result);
        stack.push(result);
        return 0;
    }

    /* 0x6B dmul */
    int op_dmul()
    {
        double value1 = stack.get_double_parameter(cp);
        double value2 = stack.get_double_parameter(cp);
        double result = value1 * value2;
        // System.out.print("dmul: %f * %f = %f\n", value1, value2, result);
        stack.push(result);
        return 0;
    }

    /* 0x8e d2i */
    int op_d2i()
    {
        double value1 = stack.pop().getDouble();
        int result = (int)value1;
        // System.out.print("d2i: %d <-- %f\n", result, value1);
        stack.push(result);
        return 0;
    }

    /* irem */
    int op_irem()
    {
        int value1 = stack.pop().getInt();
        int value2 = stack.pop().getInt();
        int result = value2 % value1;
        // System.out.print("irem: %d mod %d = %d\n", value2, value1, result);
        stack.push(result);
        return 0;
    }

    /* istore */
    int op_istore(char[] opCode)
    {
        int value = stack.pop().getInt();
        int index = opCode[1];
        // System.out.print("istore: store value into local variable %d(%d)\n", index, value);
        localVariables.integer[index] = value;
        return 0;
    }

    /* istore_1 */
    int op_istore_1()
    {
        int value = stack.pop().getInt();
        // System.out.print("istore_1: store value into local variable 1(%d)\n", value);
        localVariables.integer[1] = value;
        return 0;
    }

    /* istore_2 */
    int op_istore_2()
    {
        int value = stack.pop().getInt();
        // System.out.print("istore_2: store value into local variable 2(%d)\n", value);
        localVariables.integer[2] = value;
        return 0;
    }

    /* istore_3 */
    int op_istore_3()
    {
        int value = stack.pop().getInt();
        // System.out.print("istore_3: store value into local variable 3(%d)\n", value);
        localVariables.integer[3] = value;
        return 0;
    }

    /* isub */
    int op_isub()
    {
        int value2 = stack.pop().getInt();
        int value1 = stack.pop().getInt();
        int result = value1 - value2;
        // System.out.print("isub : %d - %d = %d\n", value1, value2, result);
        stack.push(result);
        return 0;
    }
}
