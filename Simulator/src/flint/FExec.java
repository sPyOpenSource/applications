package flint;

import jCPU.JVM.VmStackFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import jx.classfile.MethodData;

public class FExec extends ListNode {

    // --- Fields ---
    private List<Integer> stack;
    private int stackLength;
    private int[] code;
    private long[] locals;
    private int[] opcodes;
    private int lr;
    private int sp;
    private int startSp;
    private int peakSp;
    private JThread ownerThread;
    public Object excp;
    private int pc;
    private MethodData method;
    private boolean terminated;

    // --- Constants ---
    private static final int FLOAT_NAN = 0x7FC00000;
    private static final long DOUBLE_NAN = 0x7FF8000000000000L;
    private static final int END_OF_STACK = 4;
    private static final int INVALID_PC = 0xFFFFFFFF;

    // --- JVM Opcode Constants ---
    private static final int OP_NOP              = 0x00;
    private static final int OP_ACONST_NULL      = 0x01;
    private static final int OP_ICONST_M1        = 0x02;
    private static final int OP_ICONST_0         = 0x03;
    private static final int OP_ICONST_1         = 0x04;
    private static final int OP_ICONST_2         = 0x05;
    private static final int OP_ICONST_3         = 0x06;
    private static final int OP_ICONST_4         = 0x07;
    private static final int OP_ICONST_5         = 0x08;
    private static final int OP_LCONST_0         = 0x09;
    private static final int OP_LCONST_1         = 0x0A;
    private static final int OP_FCONST_0         = 0x0B;
    private static final int OP_FCONST_1         = 0x0C;
    private static final int OP_FCONST_2         = 0x0D;
    private static final int OP_DCONST_0         = 0x0E;
    private static final int OP_DCONST_1         = 0x0F;
    private static final int OP_BIPUSH           = 0x10;
    private static final int OP_SIPUSH           = 0x11;
    private static final int OP_LDC              = 0x12;
    private static final int OP_LDC_W            = 0x13;
    private static final int OP_LDC2_W           = 0x14;
    private static final int OP_ILOAD            = 0x15;
    private static final int OP_LLOAD            = 0x16;
    private static final int OP_FLOAD            = 0x17;
    private static final int OP_DLOAD            = 0x18;
    private static final int OP_ALOAD            = 0x19;
    private static final int OP_ILOAD_0          = 0x1A;
    private static final int OP_ILOAD_1          = 0x1B;
    private static final int OP_ILOAD_2          = 0x1C;
    private static final int OP_ILOAD_3          = 0x1D;
    private static final int OP_LLOAD_0          = 0x1E;
    private static final int OP_LLOAD_1          = 0x1F;
    private static final int OP_LLOAD_2          = 0x20;
    private static final int OP_LLOAD_3          = 0x21;
    private static final int OP_FLOAD_0          = 0x22;
    private static final int OP_FLOAD_1          = 0x23;
    private static final int OP_FLOAD_2          = 0x24;
    private static final int OP_FLOAD_3          = 0x25;
    private static final int OP_DLOAD_0          = 0x26;
    private static final int OP_DLOAD_1          = 0x27;
    private static final int OP_DLOAD_2          = 0x28;
    private static final int OP_DLOAD_3          = 0x29;
    private static final int OP_ALOAD_0          = 0x2A;
    private static final int OP_ALOAD_1          = 0x2B;
    private static final int OP_ALOAD_2          = 0x2C;
    private static final int OP_ALOAD_3          = 0x2D;
    private static final int OP_IALOAD           = 0x2E;
    private static final int OP_LALOAD           = 0x2F;
    private static final int OP_FALOAD           = 0x30;
    private static final int OP_DALOAD           = 0x31;
    private static final int OP_AALOAD           = 0x32;
    private static final int OP_BALOAD           = 0x33;
    private static final int OP_CALOAD           = 0x34;
    private static final int OP_SALOAD           = 0x35;
    private static final int OP_ISTORE          = 0x36;
    private static final int OP_LSTORE          = 0x37;
    private static final int OP_FSTORE          = 0x38;
    private static final int OP_DSTORE          = 0x39;
    private static final int OP_ASTORE          = 0x3A;
    private static final int OP_ISTORE_0        = 0x3B;
    private static final int OP_ISTORE_1        = 0x3C;
    private static final int OP_ISTORE_2        = 0x3D;
    private static final int OP_ISTORE_3        = 0x3E;
    private static final int OP_LSTORE_0        = 0x3F;
    private static final int OP_LSTORE_1        = 0x40;
    private static final int OP_LSTORE_2        = 0x41;
    private static final int OP_LSTORE_3        = 0x42;
    private static final int OP_FSTORE_0        = 0x43;
    private static final int OP_FSTORE_1        = 0x44;
    private static final int OP_FSTORE_2        = 0x45;
    private static final int OP_FSTORE_3        = 0x46;
    private static final int OP_DSTORE_0        = 0x47;
    private static final int OP_DSTORE_1        = 0x48;
    private static final int OP_DSTORE_2        = 0x49;
    private static final int OP_DSTORE_3        = 0x4A;
    private static final int OP_ASTORE_0        = 0x4B;
    private static final int OP_ASTORE_1        = 0x4C;
    private static final int OP_ASTORE_2        = 0x4D;
    private static final int OP_ASTORE_3        = 0x4E;
    private static final int OP_IASTORE          = 0x4F;
    private static final int OP_LASTORE          = 0x50;
    private static final int OP_FASTORE          = 0x51;
    private static final int OP_DASTORE          = 0x52;
    private static final int OP_AASTORE          = 0x53;
    private static final int OP_BASTORE          = 0x54;
    private static final int OP_CASTORE          = 0x55;
    private static final int OP_SASTORE          = 0x56;
    private static final int OP_POP              = 0x57;
    private static final int OP_POP2             = 0x58;
    private static final int OP_DUP              = 0x59;
    private static final int OP_DUP_X1           = 0x5A;
    private static final int OP_DUP_X2           = 0x5B;
    private static final int OP_DUP2             = 0x5C;
    private static final int OP_DUP2_X1          = 0x5D;
    private static final int OP_DUP2_X2          = 0x5E;
    private static final int OP_IADD             = 0x60;
    private static final int OP_LADD             = 0x61;
    private static final int OP_FADD             = 0x62;
    private static final int OP_DADD             = 0x63;
    private static final int OP_ISUB             = 0x64;
    private static final int OP_LSUB             = 0x65;
    private static final int OP_FSUB             = 0x66;
    private static final int OP_DSUB             = 0x67;
    private static final int OP_IMUL             = 0x68;
    private static final int OP_LMUL             = 0x69;
    private static final int OP_FMUL             = 0x6A;
    private static final int OP_DMUL             = 0x6B;
    private static final int OP_IDIV             = 0x6C;
    private static final int OP_LDIV             = 0x6D;
    private static final int OP_FDIV             = 0x6E;
    private static final int OP_DDIV             = 0x6F;
    private static final int OP_IREM             = 0x70;
    private static final int OP_LREM             = 0x71;
    private static final int OP_FREM             = 0x72;
    private static final int OP_DREM             = 0x73;
    private static final int OP_INEG             = 0x74;
    private static final int OP_LNEG             = 0x75;
    private static final int OP_FNEG             = 0x76;
    private static final int OP_DNEG             = 0x77;
    private static final int OP_ISHL             = 0x78;
    private static final int OP_LSHL             = 0x79;
    private static final int OP_ISHR             = 0x7A;
    private static final int OP_LSHR             = 0x7B;
    private static final int OP_IUSHR            = 0x7C;
    private static final int OP_LUSHR            = 0x7D;
    private static final int OP_IAND             = 0x7E;
    private static final int OP_LAND             = 0x7F;
    private static final int OP_IOR              = 0x80;
    private static final int OP_LOR              = 0x81;
    private static final int OP_IXOR             = 0x82;
    private static final int OP_LXOR             = 0x83;
    private static final int OP_IINC             = 0x84;
    private static final int OP_I2L              = 0x85;
    private static final int OP_I2F              = 0x86;
    private static final int OP_I2D              = 0x87;
    private static final int OP_L2I              = 0x88;
    private static final int OP_L2F              = 0x89;
    private static final int OP_L2D              = 0x8A;
    private static final int OP_F2I              = 0x8B;
    private static final int OP_F2L              = 0x8C;
    private static final int OP_F2D              = 0x8D;
    private static final int OP_D2I              = 0x8E;
    private static final int OP_D2L              = 0x8F;
    private static final int OP_D2F              = 0x90;
    private static final int OP_I2B              = 0x91;
    private static final int OP_I2C              = 0x92;
    private static final int OP_I2S              = 0x93;
    private static final int OP_LCMP             = 0x94;
    private static final int OP_FCMPL            = 0x95;
    private static final int OP_FCMPG            = 0x96;
    private static final int OP_DCMPL            = 0x97;
    private static final int OP_DCMPG            = 0x98;
    private static final int OP_IFEQ             = 0x99;
    private static final int OP_IFNE             = 0x9A;
    private static final int OP_IFLT             = 0x9B;
    private static final int OP_IFGE             = 0x9C;
    private static final int OP_IFGT             = 0x9D;
    private static final int OP_IFLE             = 0x9E;
    private static final int OP_IF_ICMPEQ        = 0x9F;
    private static final int OP_IF_ICMPNE        = 0xA0;
    private static final int OP_IF_ICMPLT        = 0xA1;
    private static final int OP_IF_ICMPGE        = 0xA2;
    private static final int OP_IF_ICMPGT        = 0xA3;
    private static final int OP_IF_ICMPLE        = 0xA4;
    private static final int OP_IF_ACMPEQ        = 0xA5;
    private static final int OP_IF_ACMPNE        = 0xA6;
    private static final int OP_GOTO             = 0xA7;
    private static final int OP_JSR              = 0xA8;
    private static final int OP_RET              = 0xA9;
    private static final int OP_TABLESWITCH      = 0xAA;
    private static final int OP_LOOKUPSWITCH     = 0xAB;
    private static final int OP_IRETURN          = 0xAC;
    private static final int OP_LRETURN          = 0xAD;
    private static final int OP_FRETURN          = 0xAE;
    private static final int OP_DRETURN          = 0xAF;
    private static final int OP_ARETURN          = 0xB0;
    private static final int OP_RETURN           = 0xB1;
    private static final int OP_GETSTATIC        = 0xB2;
    private static final int OP_PUTSTATIC        = 0xB3;
    private static final int OP_GETFIELD         = 0xB4;
    private static final int OP_PUTFIELD         = 0xB5;
    private static final int OP_INVOKEVIRTUAL    = 0xB6;
    private static final int OP_INVOKESPECIAL    = 0xB7;
    private static final int OP_INVOKESTATIC     = 0xB8;
    private static final int OP_INVOKEINTERFACE  = 0xB9;
    private static final int OP_INVOKEDYNAMIC    = 0xBA;
    private static final int OP_NEW              = 0xBB;
    private static final int OP_NEWARRAY         = 0xBC;
    private static final int OP_ANEWARRAY        = 0xBD;
    private static final int OP_ARRAYLENGTH      = 0xBE;
    private static final int OP_ATHROW           = 0xBF;
    private static final int OP_CHECKCAST        = 0xC0;
    private static final int OP_INSTANCEOF       = 0xC1;
    private static final int OP_MONITORENTER     = 0xC2;
    private static final int OP_MONITOREXIT      = 0xC3;
    private static final int OP_WIDE             = 0xC4;
    private static final int OP_MULTIANEWARRAY   = 0xC5;
    private static final int OP_IFNULL           = 0xC6;
    private static final int OP_IFNONNULL        = 0xC7;
    private static final int OP_GOTO_W           = 0xC8;
    private static final int OP_JSR_W            = 0xC9;
    private static final int OP_BREAKPOINT       = 0xCA;
    private static final int OP_EXIT             = 0xFF;

    private static final int CONST_INTEGER  = 3;
    private static final int CONST_FLOAT    = 4;
    private static final int CONST_DOUBLE   = 6;
    private static final int CONST_LONG     = 5;
    private static final int CONST_STRING   = 8;
    private static final int CONST_CLASS    = 7;
    private static final int CONST_METHOD_TYPE  = 16;
    private static final int CONST_METHOD_HANDLE = 15;

    private static final int NEW_BOOLEAN = 4;
    private static final int NEW_CHAR    = 5;
    private static final int NEW_FLOAT   = 6;
    private static final int NEW_DOUBLE  = 7;
    private static final int NEW_BYTE    = 8;
    private static final int NEW_SHORT   = 9;
    private static final int NEW_INT     = 10;
    private static final int NEW_LONG    = 11;

    private static final String[] PRIM_ARRAY_TYPE_NAMES = {
        "[Z", "[C", "[F", "[D", "[B", "[S", "[I", "[J"
    };

    // --- Constructor ---
    public FExec(JThread owner, int stackSize) {
        super();
        this.stackLength = stackSize / Integer.BYTES;
        this.stack = new ArrayList<>(stackLength);
        for (int i = 0; i < stackLength; i++) {
            stack.add(0);
        }
        this.code = null;
        this.locals = null;
        this.opcodes = null;
        this.lr = -1;
        this.sp = -1;
        this.startSp = -1;
        this.peakSp = -1;
        this.ownerThread = owner;
        this.excp = null;
        this.terminated = false;
    }

    // --- Stack Operations ---

    public void stackPushInt32(int value) {
        sp++;
        ensureStackSize(sp);
        stack.set(sp, value);
        peakSp = sp;
    }

    public void stackPushInt64(long value) {
        sp++;
        ensureStackSize(sp);
        stack.set(sp, (int) (value & 0xFFFFFFFFL));
        sp++;
        ensureStackSize(sp);
        stack.set(sp, (int) ((value >>> 32) & 0xFFFFFFFFL));
        peakSp = sp;
    }

    public void stackPushFloat(float value) {
        int intBits = Float.floatToIntBits(value);
        sp++;
        ensureStackSize(sp);
        stack.set(sp, intBits);
        peakSp = sp;
    }

    public void stackPushDouble(double value) {
        long longBits = Double.doubleToLongBits(value);
        sp++;
        ensureStackSize(sp);
        stack.set(sp, (int) (longBits & 0xFFFFFFFFL));
        sp++;
        ensureStackSize(sp);
        stack.set(sp, (int) ((longBits >>> 32) & 0xFFFFFFFFL));
        peakSp = sp;
    }

    public void stackPushObject(JObject obj) {
        sp++;
        ensureStackSize(sp);
        stack.set(sp, obj != null ? obj.hashCode() : 0);
        peakSp = sp;
        if (obj != null && (obj.getProtected() & 0x02) != 0) {
            Flint.clearProtLv2(obj);
        }
    }

    public int stackPopInt32() {
        int value = stack.get(sp);
        sp--;
        return value;
    }

    public long stackPopInt64() {
        int low = stack.get(sp);
        sp--;
        int high = stack.get(sp);
        sp--;
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }

    public float stackPopFloat() {
        int intBits = stack.get(sp);
        sp--;
        return Float.intBitsToFloat(intBits);
    }

    public double stackPopDouble() {
        int low = stack.get(sp);
        sp--;
        int high = stack.get(sp);
        sp--;
        long longBits = ((long) high << 32) | (low & 0xFFFFFFFFL);
        return Double.longBitsToDouble(longBits);
    }

    public JObject stackPopObject() {
        int objRef = stack.get(sp);
        sp--;
        return JObject.fromRef(objRef);
    }

    // --- Stack Trace ---

    public int getStackTrace(VmStackFrame stackTrace, int traceSp) {
        if (traceSp < END_OF_STACK) return -1;
        int tracePc = stack.get(traceSp - 2);
        MethodData traceMethod = (MethodData) (Object) stack.get(traceSp - 3);
        stackTrace.init(tracePc, stack.get(traceSp), traceMethod);
        return stack.get(traceSp);
    }

    public boolean getStackTrace(int index, VmStackFrame stackTrace, AtomicInteger isEndStack) {
        if (index == 0) {
            stackTrace.init(pc, startSp, method);
            if (isEndStack != null) isEndStack.set(startSp < END_OF_STACK ? 1 : 0);
            return true;
        } else {
            int traceSp = startSp;
            do {
                traceSp = getStackTrace(stackTrace, traceSp);
                if (traceSp < 0) return false;
                if (stackTrace.pc != INVALID_PC) index--;
            } while (stackTrace.pc == INVALID_PC || index > 0);
            if (isEndStack != null) isEndStack.set(traceSp < END_OF_STACK ? 1 : 0);
            return true;
        }
    }

    public boolean readLocal(int stackIndex, int localIndex, AtomicInteger value, AtomicInteger isObject) {
        VmStackFrame stackTrace = new VmStackFrame();
        if (!getStackTrace(stackIndex, stackTrace, null)) return false;
        int val = (int) stack.get(stackTrace.baseSp + 1 + localIndex);
        value.set(val);
        if (isObject != null) {
            isObject.set(Flint.isObject(val) ? 1 : 0);
        }
        return true;
    }

    public boolean readLocal(int stackIndex, int localIndex, AtomicLong value) {
        VmStackFrame stackTrace = new VmStackFrame();
        if (!getStackTrace(stackIndex, stackTrace, null)) return false;
        int low = stack.get(stackTrace.baseSp + 1 + localIndex);
        int high = stack.get(stackTrace.baseSp + 2 + localIndex);
        long val = ((long) high << 32) | (low & 0xFFFFFFFFL);
        value.set(val);
        return true;
    }

    // --- Helper Methods ---

    private void ensureStackSize(int index) {
        while (stack.size() <= index) {
            stack.add(0);
        }
    }

    private static int readInt16(int[] array, int offset) {
        return (short) ((array[offset] << 8) | (array[offset + 1] & 0xFF));
    }

    private static int readInt32(int[] array, int offset) {
        return (array[offset] << 24)
             | ((array[offset + 1] & 0xFF) << 16)
             | ((array[offset + 2] & 0xFF) << 8)
             | (array[offset + 3] & 0xFF);
    }

    // --- Stub Methods (to be implemented) ---

    private void invokeStaticCtor(Object loader) {
        // TODO: invoke static constructor for the class loader
    }

    private void invokeVirtual(Object constMethod) {
        // TODO: invoke virtual method
    }

    private void invokeSpecial(Object constMethod) {
        // TODO: invoke special method (constructor, private, super)
    }

    private void invokeStatic(Object constMethod) {
        // TODO: invoke static method
    }

    private void invokeInterface(Object interfaceMethod, int count) {
        // TODO: invoke interface method
    }

    private boolean lockObject(JObject obj) {
        // TODO: attempt to lock object for synchronized block
        return true;
    }

    private void unlockObject(JObject obj) {
        // TODO: unlock object for synchronized block
    }

    private boolean hasTerminateRequest() {
        return terminated;
    }

    public void restoreContext() {
        // TODO: restore caller's stack frame
    }

    // --- Exception Handling ---

    private void exceptionHandler() {
        FDbg dbg = Flint.getDebugger();

        while (true) {
            int tracePc = pc;
            int traceStartSp = startSp;
            MethodData traceMethod = method;
            Object obj = excp;

            // Fatal error check (low bit set indicates non-throwable error)
            if (obj != null && (System.identityHashCode(obj) & 0x01) != 0) return;

            if (dbg != null && dbg.exceptionIsEnabled()) {
                dbg.caughtException(this);
            }

            boolean reenter = false;
            int exceptionLength = traceMethod.getExceptionLength();
            for (int i = 0; i < exceptionLength; i++) {
                ExceptionTable exception = traceMethod.getException(i);
                if (exception.startPc <= tracePc && tracePc < exception.endPc) {
                    boolean isMatch = false;
                    if (exception.catchType == 0) {
                        isMatch = true;
                    } else {
                        JClass catchType = traceMethod.loader.getConstClass(this, exception.catchType);
                        if (catchType == null) {
                            while (startSp > traceStartSp) restoreContext();
                            code = this.code;
                            sp = startSp + traceMethod.getMaxLocals();
                            pc = exception.handlerPc;
                            reenter = true;
                            break;
                        }
                        isMatch = Flint.isInstanceof(this, obj, catchType);
                        if (!isMatch && excp != obj) {
                            while (startSp > traceStartSp) restoreContext();
                            code = this.code;
                            sp = startSp + traceMethod.getMaxLocals();
                            pc = exception.handlerPc;
                            reenter = true;
                            break;
                        }
                    }
                    if (isMatch) {
                        while (startSp > traceStartSp) restoreContext();
                        code = this.code;
                        sp = startSp + traceMethod.getMaxLocals();
                        pc = exception.handlerPc;
                        stackPushObject((JObject) obj);
                        excp = null;
                        return;
                    }
                }
            }
            if (reenter) continue;

            // No handler found in this frame, walk up the call stack
            if (traceStartSp < END_OF_STACK) {
                if (dbg != null && !dbg.exceptionIsEnabled()) {
                    dbg.caughtException(this);
                }
                return;
            }
            traceMethod = (MethodData) stack.get(traceStartSp - 3);
            tracePc = stack.get(traceStartSp - 2);
            traceStartSp = stack.get(traceStartSp);
            if (tracePc == INVALID_PC) return;
        }
    }

    // --- Main Execution Loop ---

    void exec(boolean initOpcodeLabels) {
        FDbg dbg = Flint.getDebugger();
        if (initOpcodeLabels) opcodes = opcodeLabels;
        int[] code = this.code;

        // Check if the class needs static initialization
        if (method.loader.getStaticInitStatus() == StaticInitStatus.UNINITIALIZED) {
            invokeStaticCtor(method.loader);
            if (excp != null) { exceptionHandler(); return; }
            code = this.code;
        }

        mainLoop:
        while (true) {
            int opcode = opcodes[code[pc]];

            switch (opcode) {

            // --- Debug ---
            case OP_NOP:
                pc++;
                break;

            // --- Constants ---
            case OP_ICONST_M1:
                stackPushInt32(-1);
                pc++;
                break;

            case OP_ICONST_0:
            case OP_ACONST_NULL:
                stackPushInt32(0);
                pc++;
                break;

            case OP_ICONST_1:
                stackPushInt32(1);
                pc++;
                break;

            case OP_ICONST_2:
                stackPushInt32(2);
                pc++;
                break;

            case OP_ICONST_3:
                stackPushInt32(3);
                pc++;
                break;

            case OP_ICONST_4:
                stackPushInt32(4);
                pc++;
                break;

            case OP_ICONST_5:
                stackPushInt32(5);
                pc++;
                break;

            case OP_LCONST_0:
                stackPushInt64(0L);
                pc++;
                break;

            case OP_LCONST_1:
                stackPushInt64(1L);
                pc++;
                break;

            case OP_FCONST_0:
                stackPushFloat(0.0f);
                pc++;
                break;

            case OP_FCONST_1:
                stackPushFloat(1.0f);
                pc++;
                break;

            case OP_FCONST_2:
                stackPushFloat(2.0f);
                pc++;
                break;

            case OP_DCONST_0:
                stackPushDouble(0.0);
                pc++;
                break;

            case OP_DCONST_1:
                stackPushDouble(1.0);
                pc++;
                break;

            case OP_BIPUSH:
                stackPushInt32((byte) code[pc + 1]);
                pc += 2;
                break;

            case OP_SIPUSH:
                stackPushInt32(readInt16(code, pc + 1));
                pc += 3;
                break;

            // --- Load Constants from Pool ---
            case OP_LDC: {
                int poolIndex = code[pc + 1];
                ClassLoader loader = method.loader;
                switch (loader.getConstPoolTag(poolIndex)) {
                    case CONST_INTEGER:
                        stackPushInt32(loader.getConstInteger(poolIndex));
                        pc += 2;
                        break;
                    case CONST_FLOAT:
                        stackPushFloat(loader.getConstFloat(poolIndex));
                        pc += 2;
                        break;
                    case CONST_STRING: {
                        JString str = loader.getConstString(this, poolIndex);
                        if (str == null) { exceptionHandler(); continue mainLoop; }
                        stackPushObject(str);
                        pc += 2;
                        break;
                    }
                    case CONST_CLASS: {
                        JClass cls = loader.getConstClass(this, poolIndex);
                        if (cls == null) { exceptionHandler(); continue mainLoop; }
                        stackPushObject(cls);
                        pc += 2;
                        break;
                    }
                    case CONST_METHOD_TYPE:
                        // TODO: not yet implemented
                        pc += 2;
                        break;
                    case CONST_METHOD_HANDLE:
                        // TODO: not yet implemented
                        pc += 2;
                        break;
                    default: {
                        JClass excpCls = Flint.findClass(this, "java/lang/ClassFormatError");
                        throwNew(excpCls, "Constant pool tag value (%d) is invalid in class %s",
                                loader.getConstPoolTag(poolIndex), loader.getName());
                        return;
                    }
                }
                break;
            }

            case OP_LDC_W: {
                int poolIndex = readInt16(code, pc + 1);
                ClassLoader loader = method.loader;
                switch (loader.getConstPoolTag(poolIndex)) {
                    case CONST_INTEGER:
                        stackPushInt32(loader.getConstInteger(poolIndex));
                        pc += 3;
                        break;
                    case CONST_FLOAT:
                        stackPushFloat(loader.getConstFloat(poolIndex));
                        pc += 3;
                        break;
                    case CONST_STRING: {
                        JString str = loader.getConstString(this, poolIndex);
                        if (str == null) { exceptionHandler(); continue mainLoop; }
                        stackPushObject(str);
                        pc += 3;
                        break;
                    }
                    case CONST_CLASS: {
                        JClass cls = loader.getConstClass(this, poolIndex);
                        if (cls == null) { exceptionHandler(); continue mainLoop; }
                        stackPushObject(cls);
                        pc += 3;
                        break;
                    }
                    case CONST_METHOD_TYPE:
                        pc += 3;
                        break;
                    case CONST_METHOD_HANDLE:
                        pc += 3;
                        break;
                    default: {
                        JClass excpCls = Flint.findClass(this, "java/lang/ClassFormatError");
                        throwNew(excpCls, "Constant pool tag value (%d) is invalid in class %s",
                                loader.getConstPoolTag(poolIndex), loader.getName());
                        return;
                    }
                }
                break;
            }

            case OP_LDC2_W: {
                int poolIndex = readInt16(code, pc + 1);
                ClassLoader loader = method.loader;
                switch (loader.getConstPoolTag(poolIndex)) {
                    case CONST_LONG:
                        stackPushInt64(loader.getConstLong(poolIndex));
                        pc += 3;
                        break;
                    case CONST_DOUBLE:
                        stackPushDouble(loader.getConstDouble(poolIndex));
                        pc += 3;
                        break;
                    default: {
                        JClass excpCls = Flint.findClass(this, "java/lang/ClassFormatError");
                        throwNew(excpCls, "Constant pool tag value (%d) is invalid in class %s",
                                loader.getConstPoolTag(poolIndex), loader.getName());
                        return;
                    }
                }
                break;
            }

            // --- Local Variable Loads ---
            case OP_ILOAD:
            case OP_FLOAD:
                stackPushInt32((int) locals[code[pc + 1]]);
                pc += 2;
                break;

            case OP_ILOAD_0:
            case OP_FLOAD_0:
                stackPushInt32((int) locals[0]);
                pc++;
                break;

            case OP_ILOAD_1:
            case OP_FLOAD_1:
                stackPushInt32((int) locals[1]);
                pc++;
                break;

            case OP_ILOAD_2:
            case OP_FLOAD_2:
                stackPushInt32((int) locals[2]);
                pc++;
                break;

            case OP_ILOAD_3:
            case OP_FLOAD_3:
                stackPushInt32((int) locals[3]);
                pc++;
                break;

            case OP_LLOAD:
            case OP_DLOAD:
                stackPushInt64(locals[code[pc + 1]]);
                pc += 2;
                break;

            case OP_LLOAD_0:
            case OP_DLOAD_0:
                stackPushInt64(locals[0]);
                pc++;
                break;

            case OP_LLOAD_1:
            case OP_DLOAD_1:
                stackPushInt64(locals[1]);
                pc++;
                break;

            case OP_LLOAD_2:
            case OP_DLOAD_2:
                stackPushInt64(locals[2]);
                pc++;
                break;

            case OP_LLOAD_3:
            case OP_DLOAD_3:
                stackPushInt64(locals[3]);
                pc++;
                break;

            case OP_ALOAD:
                stackPushObject(JObject.fromRef((int) locals[code[pc + 1]]));
                pc += 2;
                break;

            case OP_ALOAD_0:
                stackPushObject(JObject.fromRef((int) locals[0]));
                pc++;
                break;

            case OP_ALOAD_1:
                stackPushObject(JObject.fromRef((int) locals[1]));
                pc++;
                break;

            case OP_ALOAD_2:
                stackPushObject(JObject.fromRef((int) locals[2]));
                pc++;
                break;

            case OP_ALOAD_3:
                stackPushObject(JObject.fromRef((int) locals[3]));
                pc++;
                break;

            // --- Array Loads ---
            case OP_IALOAD:
            case OP_FALOAD: {
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot load from null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= (obj.size / Integer.BYTES)) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size / Integer.BYTES);
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt32(obj.getIntElement(index));
                pc++;
                break;
            }

            case OP_LALOAD:
            case OP_DALOAD: {
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot load from null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= (obj.size / Long.BYTES)) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size / Long.BYTES);
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt64(obj.getLongElement(index));
                pc++;
                break;
            }

            case OP_AALOAD: {
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot load from null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= (obj.size / Integer.BYTES)) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size / Integer.BYTES);
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushObject(obj.getObjElement(index));
                pc++;
                break;
            }

            case OP_BALOAD: {
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot load from null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= obj.size) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size);
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt32(obj.getByteElement(index));
                pc++;
                break;
            }

            case OP_CALOAD:
            case OP_SALOAD: {
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot load from null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= (obj.size / Short.BYTES)) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size / Short.BYTES);
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt32(obj.getShortElement(index));
                pc++;
                break;
            }

            // --- Local Variable Stores ---
            case OP_ISTORE:
            case OP_FSTORE: {
                int index = code[pc + 1];
                locals[index] = stackPopInt32();
                pc += 2;
                break;
            }

            case OP_LSTORE:
            case OP_DSTORE: {
                int index = code[pc + 1];
                locals[index] = stackPopInt64();
                pc += 2;
                break;
            }

            case OP_ASTORE: {
                int index = code[pc + 1];
                locals[index] = stackPopInt32();
                pc += 2;
                break;
            }

            case OP_ISTORE_0:
            case OP_FSTORE_0:
                locals[0] = stackPopInt32();
                pc++;
                break;

            case OP_ISTORE_1:
            case OP_FSTORE_1:
                locals[1] = stackPopInt32();
                pc++;
                break;

            case OP_ISTORE_2:
            case OP_FSTORE_2:
                locals[2] = stackPopInt32();
                pc++;
                break;

            case OP_ISTORE_3:
            case OP_FSTORE_3:
                locals[3] = stackPopInt32();
                pc++;
                break;

            case OP_LSTORE_0:
            case OP_DSTORE_0:
                locals[0] = stackPopInt64();
                pc++;
                break;

            case OP_LSTORE_1:
            case OP_DSTORE_1:
                locals[1] = stackPopInt64();
                pc++;
                break;

            case OP_LSTORE_2:
            case OP_DSTORE_2:
                locals[2] = stackPopInt64();
                pc++;
                break;

            case OP_LSTORE_3:
            case OP_DSTORE_3:
                locals[3] = stackPopInt64();
                pc++;
                break;

            case OP_ASTORE_0:
                locals[0] = stackPopInt32();
                pc++;
                break;

            case OP_ASTORE_1:
                locals[1] = stackPopInt32();
                pc++;
                break;

            case OP_ASTORE_2:
                locals[2] = stackPopInt32();
                pc++;
                break;

            case OP_ASTORE_3:
                locals[3] = stackPopInt32();
                pc++;
                break;

            // --- Array Stores ---
            case OP_IASTORE:
            case OP_FASTORE:
            case OP_AASTORE: {
                int value = stackPopInt32();
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot store to null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= (obj.size / Integer.BYTES)) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size / Integer.BYTES);
                    exceptionHandler();
                    continue mainLoop;
                }
                obj.setIntElement(index, value);
                pc++;
                break;
            }

            case OP_LASTORE:
            case OP_DASTORE: {
                long value = stackPopInt64();
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot store to null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= (obj.size / Long.BYTES)) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size / Long.BYTES);
                    exceptionHandler();
                    continue mainLoop;
                }
                obj.setLongElement(index, value);
                pc++;
                break;
            }

            case OP_BASTORE: {
                int value = stackPopInt32();
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot store to null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= obj.size) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size);
                    exceptionHandler();
                    continue mainLoop;
                }
                obj.setByteElement(index, (byte) value);
                pc++;
                break;
            }

            case OP_CASTORE:
            case OP_SASTORE: {
                int value = stackPopInt32();
                int index = stackPopInt32();
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot store to null array object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (index < 0 || index >= (obj.size / Short.BYTES)) {
                    JClass excpCls = Flint.findClass(this, "java/lang/ArrayIndexOutOfBoundsException");
                    throwNew(excpCls, "Index %d out of bounds for length %d", index, obj.size / Short.BYTES);
                    exceptionHandler();
                    continue mainLoop;
                }
                obj.setShortElement(index, (short) value);
                pc++;
                break;
            }

            // --- Stack Manipulation ---
            case OP_POP:
                stackPopInt32();
                pc++;
                break;

            case OP_POP2:
                stackPopInt64();
                pc++;
                break;

            case OP_DUP: {
                int value = stack.get(sp);
                stackPushInt32(value);
                pc++;
                break;
            }

            case OP_DUP_X1: {
                int value2 = stack.get(sp - 1);
                int value1 = stack.get(sp);
                stack.set(sp + 1, value1);
                stack.set(sp, value2);
                stack.set(sp - 1, value1);
                pc++;
                break;
            }

            case OP_DUP_X2: {
                int value3 = stack.get(sp - 2);
                int value2 = stack.get(sp - 1);
                int value1 = stack.get(sp);
                stack.set(sp + 1, value1);
                stack.set(sp, value2);
                stack.set(sp - 1, value3);
                stack.set(sp - 2, value1);
                pc++;
                break;
            }

            case OP_DUP2: {
                int value2 = stack.get(sp - 1);
                int value1 = stack.get(sp);
                stackPushInt32(value2);
                stackPushInt32(value1);
                pc++;
                break;
            }

            case OP_DUP2_X1: {
                int value3 = stack.get(sp - 2);
                int value2 = stack.get(sp - 1);
                int value1 = stack.get(sp);
                stack.set(sp + 1, value2);
                stack.set(sp, value1);
                stack.set(sp - 1, value3);
                stack.set(sp - 2, value1);
                stack.set(sp - 3, value2);
                pc++;
                break;
            }

            case OP_DUP2_X2: {
                int value4 = stack.get(sp - 3);
                int value3 = stack.get(sp - 2);
                int value2 = stack.get(sp - 1);
                int value1 = stack.get(sp);
                stack.set(sp + 1, value2);
                stack.set(sp, value1);
                stack.set(sp - 1, value3);
                stack.set(sp - 2, value4);
                stack.set(sp - 3, value1);
                stack.set(sp - 4, value2);
                pc++;
                break;
            }

            // --- Arithmetic ---
            case OP_IADD: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                stackPushInt32(value1 + value2);
                pc++;
                break;
            }

            case OP_LADD: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                stackPushInt64(value1 + value2);
                pc++;
                break;
            }

            case OP_FADD: {
                float value2 = stackPopFloat();
                float value1 = stackPopFloat();
                stackPushFloat(value1 + value2);
                pc++;
                break;
            }

            case OP_DADD: {
                double value2 = stackPopDouble();
                double value1 = stackPopDouble();
                stackPushDouble(value1 + value2);
                pc++;
                break;
            }

            case OP_ISUB: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                stackPushInt32(value1 - value2);
                pc++;
                break;
            }

            case OP_LSUB: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                stackPushInt64(value1 - value2);
                pc++;
                break;
            }

            case OP_FSUB: {
                float value2 = stackPopFloat();
                float value1 = stackPopFloat();
                stackPushFloat(value1 - value2);
                pc++;
                break;
            }

            case OP_DSUB: {
                double value2 = stackPopDouble();
                double value1 = stackPopDouble();
                stackPushDouble(value1 - value2);
                pc++;
                break;
            }

            case OP_IMUL: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                stackPushInt32(value1 * value2);
                pc++;
                break;
            }

            case OP_LMUL: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                stackPushInt64(value1 * value2);
                pc++;
                break;
            }

            case OP_FMUL: {
                float value2 = stackPopFloat();
                float value1 = stackPopFloat();
                stackPushFloat(value1 * value2);
                pc++;
                break;
            }

            case OP_DMUL: {
                double value2 = stackPopDouble();
                double value1 = stackPopDouble();
                stackPushDouble(value1 * value2);
                pc++;
                break;
            }

            case OP_IDIV: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                if (value2 == 0) {
                    throwNew(Flint.findClass(this, "java/lang/ArithmeticException"), "Divided by zero");
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt32(value1 / value2);
                pc++;
                break;
            }

            case OP_LDIV: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                if (value2 == 0) {
                    throwNew(Flint.findClass(this, "java/lang/ArithmeticException"), "Divided by zero");
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt64(value1 / value2);
                pc++;
                break;
            }

            case OP_FDIV: {
                float value2 = stackPopFloat();
                float value1 = stackPopFloat();
                stackPushFloat(value1 / value2);
                pc++;
                break;
            }

            case OP_DDIV: {
                double value2 = stackPopDouble();
                double value1 = stackPopDouble();
                stackPushDouble(value1 / value2);
                pc++;
                break;
            }

            case OP_IREM: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                if (value2 == 0) {
                    throwNew(Flint.findClass(this, "java/lang/ArithmeticException"), "Divided by zero");
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt32(value1 % value2);
                pc++;
                break;
            }

            case OP_LREM: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                if (value2 == 0) {
                    throwNew(Flint.findClass(this, "java/lang/ArithmeticException"), "Divided by zero");
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt64(value1 % value2);
                pc++;
                break;
            }

            case OP_FREM: {
                float value2 = stackPopFloat();
                float value1 = stackPopFloat();
                int temp = (int) (value1 / value2);
                stackPushFloat(value1 - (temp * value2));
                pc++;
                break;
            }

            case OP_DREM: {
                double value2 = stackPopDouble();
                double value1 = stackPopDouble();
                long temp = (long) (value1 / value2);
                stackPushDouble(value1 - (temp * value2));
                pc++;
                break;
            }

            case OP_INEG:
                stack.set(sp, -stack.get(sp));
                pc++;
                break;

            case OP_LNEG:
                stackPushInt64(-stackPopInt64());
                pc++;
                break;

            case OP_FNEG:
                stackPushFloat(-stackPopFloat());
                pc++;
                break;

            case OP_DNEG:
                stackPushDouble(-stackPopDouble());
                pc++;
                break;

            // --- Bitwise / Shift ---
            case OP_ISHL: {
                int position = stackPopInt32();
                int value = stackPopInt32();
                stackPushInt32(value << position);
                pc++;
                break;
            }

            case OP_LSHL: {
                int position = stackPopInt32();
                long value = stackPopInt64();
                stackPushInt64(value << position);
                pc++;
                break;
            }

            case OP_ISHR: {
                int position = stackPopInt32();
                int value = stackPopInt32();
                stackPushInt32(value >> position);
                pc++;
                break;
            }

            case OP_LSHR: {
                int position = stackPopInt32();
                long value = stackPopInt64();
                stackPushInt64(value >> position);
                pc++;
                break;
            }

            case OP_IUSHR: {
                int position = stackPopInt32();
                int value = stackPopInt32();
                stackPushInt32(value >>> position);
                pc++;
                break;
            }

            case OP_LUSHR: {
                int position = stackPopInt32();
                long value = stackPopInt64();
                stackPushInt64(value >>> position);
                pc++;
                break;
            }

            case OP_IAND: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                stackPushInt32(value1 & value2);
                pc++;
                break;
            }

            case OP_LAND: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                stackPushInt64(value1 & value2);
                pc++;
                break;
            }

            case OP_IOR: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                stackPushInt32(value1 | value2);
                pc++;
                break;
            }

            case OP_LOR: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                stackPushInt64(value1 | value2);
                pc++;
                break;
            }

            case OP_IXOR: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                stackPushInt32(value1 ^ value2);
                pc++;
                break;
            }

            case OP_LXOR: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                stackPushInt64(value1 ^ value2);
                pc++;
                break;
            }

            case OP_IINC:
                locals[code[pc + 1]] += (byte) code[pc + 2];
                pc += 3;
                break;

            // --- Type Conversions ---
            case OP_I2L:
                stackPushInt64(stackPopInt32());
                pc++;
                break;

            case OP_I2F: {
                int value = stackPopInt32();
                stackPushFloat((float) value);
                pc++;
                break;
            }

            case OP_I2D:
                stackPushDouble(stackPopInt32());
                pc++;
                break;

            case OP_L2I:
                stackPushInt32((int) stackPopInt64());
                pc++;
                break;

            case OP_L2F:
                stackPushFloat(stackPopInt64());
                pc++;
                break;

            case OP_L2D:
                stackPushDouble(stackPopInt64());
                pc++;
                break;

            case OP_F2I: {
                float value = stackPopFloat();
                stackPushInt32((int) value);
                pc++;
                break;
            }

            case OP_F2L:
                stackPushInt64((long) stackPopFloat());
                pc++;
                break;

            case OP_F2D:
                stackPushDouble(stackPopFloat());
                pc++;
                break;

            case OP_D2I:
                stackPushInt32((int) stackPopDouble());
                pc++;
                break;

            case OP_D2L:
                stackPushInt64((long) stackPopDouble());
                pc++;
                break;

            case OP_D2F:
                stackPushFloat((float) stackPopDouble());
                pc++;
                break;

            case OP_I2B:
                stack.set(sp, (int) ((byte) stack.get(sp).intValue()));
                pc++;
                break;

            case OP_I2C:
            case OP_I2S:
                stack.set(sp, (int) ((short) stack.get(sp).intValue()));
                pc++;
                break;

            // --- Comparisons ---
            case OP_LCMP: {
                long value2 = stackPopInt64();
                long value1 = stackPopInt64();
                stackPushInt32((value1 == value2) ? 0 : ((value1 < value2) ? -1 : 1));
                pc++;
                break;
            }

            case OP_FCMPL:
            case OP_FCMPG: {
                float value2 = stackPopFloat();
                float value1 = stackPopFloat();
                if (Float.isNaN(value1) || Float.isNaN(value2)) {
                    stackPushInt32((opcode == OP_FCMPL) ? -1 : 1);
                } else if (value1 > value2) {
                    stackPushInt32(1);
                } else if (value1 == value2) {
                    stackPushInt32(0);
                } else {
                    stackPushInt32(-1);
                }
                pc++;
                break;
            }

            case OP_DCMPL:
            case OP_DCMPG: {
                double value2 = stackPopDouble();
                double value1 = stackPopDouble();
                if (Double.isNaN(value1) || Double.isNaN(value2)) {
                    stackPushInt32((opcode == OP_DCMPL) ? -1 : 1);
                } else if (value1 > value2) {
                    stackPushInt32(1);
                } else if (value1 == value2) {
                    stackPushInt32(0);
                } else {
                    stackPushInt32(-1);
                }
                pc++;
                break;
            }

            // --- Branches ---
            case OP_IFEQ:
            case OP_IFNULL:
                pc += (stackPopInt32() == 0) ? readInt16(code, pc + 1) : 3;
                break;

            case OP_IFNE:
            case OP_IFNONNULL:
                pc += (stackPopInt32() != 0) ? readInt16(code, pc + 1) : 3;
                break;

            case OP_IFLT:
                pc += (stackPopInt32() < 0) ? readInt16(code, pc + 1) : 3;
                break;

            case OP_IFGE:
                pc += (stackPopInt32() >= 0) ? readInt16(code, pc + 1) : 3;
                break;

            case OP_IFGT:
                pc += (stackPopInt32() > 0) ? readInt16(code, pc + 1) : 3;
                break;

            case OP_IFLE:
                pc += (stackPopInt32() <= 0) ? readInt16(code, pc + 1) : 3;
                break;

            case OP_IF_ICMPEQ:
            case OP_IF_ACMPEQ: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                pc += (value1 == value2) ? readInt16(code, pc + 1) : 3;
                break;
            }

            case OP_IF_ICMPNE:
            case OP_IF_ACMPNE: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                pc += (value1 != value2) ? readInt16(code, pc + 1) : 3;
                break;
            }

            case OP_IF_ICMPLT: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                pc += (value1 < value2) ? readInt16(code, pc + 1) : 3;
                break;
            }

            case OP_IF_ICMPGE: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                pc += (value1 >= value2) ? readInt16(code, pc + 1) : 3;
                break;
            }

            case OP_IF_ICMPGT: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                pc += (value1 > value2) ? readInt16(code, pc + 1) : 3;
                break;
            }

            case OP_IF_ICMPLE: {
                int value2 = stackPopInt32();
                int value1 = stackPopInt32();
                pc += (value1 <= value2) ? readInt16(code, pc + 1) : 3;
                break;
            }

            case OP_GOTO:
                pc += readInt16(code, pc + 1);
                break;

            case OP_GOTO_W:
                pc += readInt32(code, pc + 1);
                break;

            case OP_JSR:
                stackPushInt32(pc + 3);
                pc += readInt16(code, pc + 1);
                break;

            case OP_JSR_W:
                stackPushInt32(pc + 5);
                pc += readInt32(code, pc + 1);
                break;

            case OP_RET:
                pc = (int) locals[code[pc + 1]];
                break;

            // --- Switch ---
            case OP_TABLESWITCH: {
                int index = stackPopInt32();
                int padding = (4 - ((pc + 1) % 4)) % 4;
                int base = pc + padding + 1;
                int defaultOffset = readInt32(code, base);
                int low = readInt32(code, base + 4);
                int high = readInt32(code, base + 8);
                if (index < low || index > high) {
                    pc += defaultOffset;
                } else {
                    int caseOffset = readInt32(code, base + 12 + (index - low) * 4);
                    pc += caseOffset;
                }
                break;
            }

            case OP_LOOKUPSWITCH: {
                int key = stackPopInt32();
                int padding = (4 - ((pc + 1) % 4)) % 4;
                int base = pc + padding + 1;
                int defaultOffset = readInt32(code, base);
                int npairs = readInt32(code, base + 4);
                int pairOffset = base + 8;
                boolean found = false;
                for (int i = 0; i < npairs; i++) {
                    int matchValue = readInt32(code, pairOffset);
                    if (key == matchValue) {
                        pc += readInt32(code, pairOffset + 4);
                        found = true;
                        break;
                    }
                    pairOffset += 8;
                }
                if (!found) {
                    pc += defaultOffset;
                }
                break;
            }

            // --- Returns ---
            case OP_IRETURN:
            case OP_FRETURN: {
                int retVal = stackPopInt32();
                restoreContext();
                code = this.code;
                stackPushInt32(retVal);
                pc = lr;
                break;
            }

            case OP_LRETURN:
            case OP_DRETURN: {
                long retVal = stackPopInt64();
                restoreContext();
                code = this.code;
                stackPushInt64(retVal);
                pc = lr;
                break;
            }

            case OP_ARETURN: {
                int retVal = stackPopInt32();
                restoreContext();
                code = this.code;
                stackPushObject(JObject.fromRef(retVal));
                pc = lr;
                break;
            }

            case OP_RETURN: {
                restoreContext();
                code = this.code;
                peakSp = sp;
                pc = lr;
                break;
            }

            // --- Field Access ---
            case OP_GETSTATIC: {
                ConstField constField = method.loader.getConstField(this, readInt16(code, pc + 1));
                if (constField == null) { exceptionHandler(); continue mainLoop; }
                ClassLoader clsLoader = constField.loader;
                if (clsLoader == null) {
                    clsLoader = Flint.findLoader(this, constField.className);
                    if (clsLoader == null) { exceptionHandler(); continue mainLoop; }
                    constField.loader = clsLoader;
                }
                StaticInitStatus initStatus = clsLoader.getStaticInitStatus();
                if (initStatus == StaticInitStatus.INITIALIZED
                        || (initStatus == StaticInitStatus.INITIALIZING
                            && clsLoader.monitorOwnId == System.identityHashCode(this))) {
                    FieldValue fieldValue = clsLoader.getStaticField(this, constField);
                    if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                    switch (constField.nameAndType.desc.charAt(0)) {
                        case 'J':
                        case 'D':
                            stackPushInt64(fieldValue.getInt64());
                            break;
                        case 'L':
                        case '[':
                            stackPushObject(fieldValue.getObj());
                            break;
                        default:
                            stackPushInt32(fieldValue.getInt32());
                            break;
                    }
                    pc += 3;
                } else if (initStatus == StaticInitStatus.UNINITIALIZED) {
                    invokeStaticCtor(clsLoader);
                    if (excp != null) { exceptionHandler(); continue mainLoop; }
                    code = this.code;
                    continue mainLoop;
                }
                FlintAPI.Thread.yield();
                if (hasTerminateRequest()) return;
                continue mainLoop;
            }

            case OP_PUTSTATIC: {
                ConstField constField = method.loader.getConstField(this, readInt16(code, pc + 1));
                if (constField == null) { exceptionHandler(); continue mainLoop; }
                ClassLoader clsLoader = constField.loader;
                if (clsLoader == null) {
                    clsLoader = Flint.findLoader(this, constField.className);
                    if (clsLoader == null) { exceptionHandler(); continue mainLoop; }
                    constField.loader = clsLoader;
                }
                StaticInitStatus initStatus = clsLoader.getStaticInitStatus();
                if (initStatus == StaticInitStatus.INITIALIZED
                        || (initStatus == StaticInitStatus.INITIALIZING
                            && clsLoader.monitorOwnId == System.identityHashCode(this))) {
                    FieldValue fieldValue = clsLoader.getStaticField(this, constField);
                    if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                    switch (constField.nameAndType.desc.charAt(0)) {
                        case 'Z':
                        case 'B':
                            fieldValue.setInt32((byte) stackPopInt32());
                            break;
                        case 'C':
                        case 'S':
                            fieldValue.setInt32((short) stackPopInt32());
                            break;
                        case 'J':
                        case 'D':
                            fieldValue.setInt64(stackPopInt64());
                            break;
                        case 'L':
                        case '[':
                            fieldValue.setObj(stackPopObject());
                            break;
                        default:
                            fieldValue.setInt32(stackPopInt32());
                            break;
                    }
                    pc += 3;
                } else if (initStatus == StaticInitStatus.UNINITIALIZED) {
                    invokeStaticCtor(clsLoader);
                    if (excp != null) { exceptionHandler(); continue mainLoop; }
                    code = this.code;
                    continue mainLoop;
                }
                FlintAPI.Thread.yield();
                if (hasTerminateRequest()) return;
                continue mainLoop;
            }

            case OP_GETFIELD: {
                ConstField constField = method.loader.getConstField(this, readInt16(code, pc + 1));
                if (constField == null) { exceptionHandler(); continue mainLoop; }
                JObject obj = stackPopObject();
                if (obj == null) {
                    JClass excpCls = Flint.findClass(this, "java/lang/NullPointerException");
                    throwNew(excpCls, "Cannot access field %s.%s from null object",
                            constField.className, constField.nameAndType.name);
                    exceptionHandler();
                    continue mainLoop;
                }
                FieldValue fieldValue = obj.getField(this, constField);
                if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                switch (constField.nameAndType.desc.charAt(0)) {
                    case 'J':
                    case 'D':
                        stackPushInt64(fieldValue.getInt64());
                        break;
                    case 'L':
                    case '[':
                        stackPushObject(fieldValue.getObj());
                        break;
                    default:
                        stackPushInt32(fieldValue.getInt32());
                        break;
                }
                pc += 3;
                break;
            }

            case OP_PUTFIELD: {
                ConstField constField = method.loader.getConstField(this, readInt16(code, pc + 1));
                if (constField == null) { exceptionHandler(); continue mainLoop; }
                switch (constField.nameAndType.desc.charAt(0)) {
                    case 'Z':
                    case 'B': {
                        int value = stackPopInt32();
                        JObject obj = stackPopObject();
                        if (obj == null) {
                            JClass excpCls = Flint.findClass(this, "java/lang/NullPointerException");
                            throwNew(excpCls, "Cannot access field %s.%s from null object",
                                    constField.className, constField.nameAndType.name);
                            exceptionHandler();
                            continue mainLoop;
                        }
                        FieldValue fieldValue = obj.getField(this, constField);
                        if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                        fieldValue.setInt32((byte) value);
                        break;
                    }
                    case 'C':
                    case 'S': {
                        int value = stackPopInt32();
                        JObject obj = stackPopObject();
                        if (obj == null) {
                            JClass excpCls = Flint.findClass(this, "java/lang/NullPointerException");
                            throwNew(excpCls, "Cannot access field %s.%s from null object",
                                    constField.className, constField.nameAndType.name);
                            exceptionHandler();
                            continue mainLoop;
                        }
                        FieldValue fieldValue = obj.getField(this, constField);
                        if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                        fieldValue.setInt32((short) value);
                        break;
                    }
                    case 'J':
                    case 'D': {
                        long value = stackPopInt64();
                        JObject obj = stackPopObject();
                        if (obj == null) {
                            JClass excpCls = Flint.findClass(this, "java/lang/NullPointerException");
                            throwNew(excpCls, "Cannot access field %s.%s from null object",
                                    constField.className, constField.nameAndType.name);
                            exceptionHandler();
                            continue mainLoop;
                        }
                        FieldValue fieldValue = obj.getField(this, constField);
                        if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                        fieldValue.setInt64(value);
                        break;
                    }
                    case 'L':
                    case '[': {
                        JObject value = stackPopObject();
                        JObject obj = stackPopObject();
                        if (obj == null) {
                            JClass excpCls = Flint.findClass(this, "java/lang/NullPointerException");
                            throwNew(excpCls, "Cannot access field %s.%s from null object",
                                    constField.className, constField.nameAndType.name);
                            exceptionHandler();
                            continue mainLoop;
                        }
                        FieldValue fieldValue = obj.getField(this, constField);
                        if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                        fieldValue.setObj(value);
                        break;
                    }
                    default: {
                        int value = stackPopInt32();
                        JObject obj = stackPopObject();
                        if (obj == null) {
                            JClass excpCls = Flint.findClass(this, "java/lang/NullPointerException");
                            throwNew(excpCls, "Cannot access field %s.%s from null object",
                                    constField.className, constField.nameAndType.name);
                            exceptionHandler();
                            continue mainLoop;
                        }
                        FieldValue fieldValue = obj.getField(this, constField);
                        if (fieldValue == null) { exceptionHandler(); continue mainLoop; }
                        fieldValue.setInt32(value);
                        break;
                    }
                }
                pc += 3;
                break;
            }

            // --- Method Invocation ---
            case OP_INVOKEVIRTUAL: {
                ConstMethod constMethod = method.loader.getConstMethod(this, readInt16(code, pc + 1));
                if (constMethod == null) { exceptionHandler(); continue mainLoop; }
                invokeVirtual(constMethod);
                if (excp != null) { exceptionHandler(); continue mainLoop; }
                code = this.code;
                break;
            }

            case OP_INVOKESPECIAL: {
                ConstMethod constMethod = method.loader.getConstMethod(this, readInt16(code, pc + 1));
                if (constMethod == null) { exceptionHandler(); continue mainLoop; }
                invokeSpecial(constMethod);
                if (excp != null) { exceptionHandler(); continue mainLoop; }
                code = this.code;
                break;
            }

            case OP_INVOKESTATIC: {
                ConstMethod constMethod = method.loader.getConstMethod(this, readInt16(code, pc + 1));
                if (constMethod == null) { exceptionHandler(); continue mainLoop; }
                invokeStatic(constMethod);
                if (excp != null) { exceptionHandler(); continue mainLoop; }
                code = this.code;
                break;
            }

            case OP_INVOKEINTERFACE: {
                Object interfaceMethod = method.loader.getConstInterfaceMethod(this, readInt16(code, pc + 1));
                if (interfaceMethod == null) { exceptionHandler(); continue mainLoop; }
                int count = code[pc + 3];
                invokeInterface(interfaceMethod, count);
                if (excp != null) { exceptionHandler(); continue mainLoop; }
                code = this.code;
                break;
            }

            case OP_INVOKEDYNAMIC: {
                JClass excpCls = Flint.findClass(this, "java/lang/UnsupportedOperationException");
                throwNew(excpCls, "Invokedynamic instructions are not supported");
                exceptionHandler();
                continue mainLoop;
            }

            // --- Object Creation ---
            case OP_NEW: {
                JClass cls = method.loader.getConstClass(this, readInt16(code, pc + 1));
                JObject obj = Flint.newObject(this, cls);
                if (obj == null) { exceptionHandler(); continue mainLoop; }
                stackPushObject(obj);
                pc += 3;
                break;
            }

            case OP_NEWARRAY: {
                int count = stackPopInt32();
                if (count < 0) {
                    throwNew(Flint.findClass(this, "java/lang/NegativeArraySizeException"),
                            "Size of the array is a negative number");
                    exceptionHandler();
                    continue mainLoop;
                }
                int atype = code[pc + 1];
                JClass arrayCls = Flint.findClass(this, PRIM_ARRAY_TYPE_NAMES[atype - NEW_BOOLEAN]);
                JObject obj = Flint.newArray(this, arrayCls, count);
                if (obj == null) { exceptionHandler(); continue mainLoop; }
                obj.clearData();
                stackPushObject(obj);
                pc += 2;
                break;
            }

            case OP_ANEWARRAY: {
                int count = stackPopInt32();
                if (count < 0) {
                    throwNew(Flint.findClass(this, "java/lang/NegativeArraySizeException"),
                            "Size of the array is a negative number");
                    exceptionHandler();
                    continue mainLoop;
                }
                JClass cls = method.loader.getConstClass(this, readInt16(code, pc + 1));
                if (cls == null) { exceptionHandler(); continue mainLoop; }
                cls = Flint.findClassOfArray(this, cls.getTypeName(), 1);
                JObject array = Flint.newArray(this, cls, count);
                if (array == null) { exceptionHandler(); continue mainLoop; }
                array.clearData();
                stackPushObject(array);
                pc += 3;
                break;
            }

            case OP_ARRAYLENGTH: {
                JObject obj = stackPopObject();
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot read the array length from null object");
                    exceptionHandler();
                    continue mainLoop;
                }
                stackPushInt32(obj.size / obj.type.componentSize());
                pc++;
                break;
            }

            // --- Exception Throwing ---
            case OP_ATHROW: {
                excp = stackPopObject();
                if (excp == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot throw exception by null object");
                }
                exceptionHandler();
                continue mainLoop;
            }

            // --- Type Checking ---
            case OP_CHECKCAST: {
                JObject obj = JObject.fromRef(stack.get(sp));
                if (obj != null) {
                    JClass catchType = method.loader.getConstClass(this, readInt16(code, pc + 1));
                    boolean isIns = Flint.isInstanceof(this, obj, catchType);
                    if (!isIns) {
                        if (excp == null) {
                            JClass excpCls = Flint.findClass(this, "java/lang/ClassCastException");
                            throwNew(excpCls, "Class %s cannot be cast to class %s",
                                    obj.getTypeName(), catchType.getTypeName());
                        }
                        exceptionHandler();
                        continue mainLoop;
                    }
                }
                pc += 3;
                break;
            }

            case OP_INSTANCEOF: {
                JObject obj = stackPopObject();
                JClass type = method.loader.getConstClass(this, readInt16(code, pc + 1));
                boolean isIns = Flint.isInstanceof(this, obj, type);
                if (isIns) {
                    stackPushInt32(1);
                } else {
                    if (excp != null) { exceptionHandler(); continue mainLoop; }
                    stackPushInt32(0);
                }
                pc += 3;
                break;
            }

            // --- Synchronization ---
            case OP_MONITORENTER: {
                JObject obj = JObject.fromRef(stack.get(sp));
                if (obj == null) {
                    throwNew(Flint.findClass(this, "java/lang/NullPointerException"),
                            "Cannot enter synchronized block by null object");
                    exceptionHandler();
                    continue mainLoop;
                }
                if (!lockObject(obj)) {
                    if (excp != null) { exceptionHandler(); continue mainLoop; }
                    FlintAPI.Thread.yield();
                    continue mainLoop;
                }
                stackPopObject();
                pc++;
                break;
            }

            case OP_MONITOREXIT: {
                JObject obj = stackPopObject();
                unlockObject(obj);
                pc++;
                break;
            }

            // --- Wide Prefix ---
            case OP_WIDE: {
                int subOpcode = code[pc + 1];
                switch (subOpcode) {
                    case OP_IINC: {
                        int index = readInt16(code, pc + 2);
                        locals[index] += (short) readInt16(code, pc + 4);
                        pc += 6;
                        break;
                    }
                    case OP_ALOAD: {
                        int index = readInt16(code, pc + 2);
                        stackPushObject(JObject.fromRef((int) locals[index]));
                        pc += 4;
                        break;
                    }
                    case OP_FLOAD:
                    case OP_ILOAD: {
                        int index = readInt16(code, pc + 2);
                        stackPushInt32((int) locals[index]);
                        pc += 4;
                        break;
                    }
                    case OP_LLOAD:
                    case OP_DLOAD: {
                        int index = readInt16(code, pc + 2);
                        stackPushInt64(locals[index]);
                        pc += 4;
                        break;
                    }
                    case OP_ASTORE: {
                        int index = readInt16(code, pc + 2);
                        locals[index] = stackPopInt32();
                        pc += 4;
                        break;
                    }
                    case OP_FSTORE:
                    case OP_ISTORE: {
                        int index = readInt16(code, pc + 2);
                        locals[index] = stackPopInt32();
                        pc += 4;
                        break;
                    }
                    case OP_LSTORE:
                    case OP_DSTORE: {
                        int index = readInt16(code, pc + 2);
                        locals[index] = stackPopInt64();
                        pc += 4;
                        break;
                    }
                    case OP_RET: {
                        int index = readInt16(code, pc + 2);
                        pc = (int) locals[index];
                        break;
                    }
                    default: {
                        throwNew(Flint.findClass(this, "java/lang/ClassFormatError"),
                                "Invalid wide opcode %d", subOpcode);
                        return;
                    }
                }
                break;
            }

            // --- Multi-dimensional Array ---
            case OP_MULTIANEWARRAY: {
                JClass cls = method.loader.getConstClass(this, readInt16(code, pc + 1));
                int dimensions = code[pc + 3];
                for (int i = 0; i < dimensions; i++) {
                    if (stack.get(sp - dimensions + 1 + i) < 0) {
                        throwNew(Flint.findClass(this, "java/lang/NegativeArraySizeException"),
                                "Size of the array is a negative number");
                        exceptionHandler();
                        continue mainLoop;
                    }
                }
                JObject array = Flint.newMultiArray(this, cls,
                        stack.get(sp - dimensions + 1), dimensions);
                if (array == null) { exceptionHandler(); continue mainLoop; }
                sp -= dimensions;
                stackPushObject(array);
                pc += 4;
                break;
            }

            // --- Breakpoint ---
            case OP_BREAKPOINT: {
                if (dbg == null) {
                    pc++;
                    break;
                }
                dbg.hitBreakpoint(this);
                if (hasTerminateRequest()) return;
                int op = code[pc];
                if (op == OP_BREAKPOINT) {
                    op = dbg.getSavedOpcode(pc, method);
                    if (op == OP_NOP) {
                        op = code[pc];
                    } else if (op == OP_BREAKPOINT) {
                        pc++;
                        continue mainLoop;
                    }
                }
                // Re-dispatch with the restored opcode
                // In a real implementation, this would jump to the handler for op
                // For now, set opcodes temporarily and continue
                continue mainLoop;
            }

            case OP_EXIT:
                return;

            default: {
                throwNew(Flint.findClass(this, "java/lang/ClassFormatError"),
                        "Invalid opcode %d", code[pc]);
                return;
            }

            } // end switch
        } // end while
    }

    // --- Task Runner ---

    void runTask(FExec exec) {
        exec.exec(true);
        if (exec.excp != null) {
            if (exec.excp instanceof JThrowable) {
                JThrowable throwable = (JThrowable) exec.excp;
                String str = throwable.getDetailMessage();
                if (str != null) {
                    System.out.print(throwable.getClass().getName());
                    System.out.print(": ");
                    System.out.println(str);
                } else {
                    System.out.println(throwable.getClass().getName());
                }
            } else {
                // Fatal error - print the error message
                System.out.println(exec.excp);
            }
            Flint.terminateRequest();
        }
        while (exec.startSp > END_OF_STACK) exec.restoreContext();
        exec.peakSp = -1;
        Flint.freeExecution(exec);
        FlintAPI.Thread.terminate(0);
    }

    // --- Public Accessors ---

    public void setCode(int[] code) { this.code = code; }
    public int[] getCode() { return code; }
    public void setLocals(long[] locals) { this.locals = locals; }
    public long[] getLocals() { return locals; }
    public void setMethod(MethodData method) { this.method = method; }
    public MethodData getMethod() { return method; }
    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }
    public int getSp() { return sp; }
    public int getStartSp() { return startSp; }
    public void setStartSp(int startSp) { this.startSp = startSp; }
    public int getLr() { return lr; }
    public void setLr(int lr) { this.lr = lr; }
    public int getPeakSp() { return peakSp; }
}

// --- Placeholder Types (to be implemented fully) ---

class ListNode {
}

class JThread {
}

class StaticInitStatus {
    static final StaticInitStatus UNINITIALIZED = new StaticInitStatus();
    static final StaticInitStatus INITIALIZING = new StaticInitStatus();
    static final StaticInitStatus INITIALIZED = new StaticInitStatus();
}

class ConstField {
    String className;
    NameAndType nameAndType;
    ClassLoader loader;
}

class NameAndType {
    String name;
    String desc;
}

class ConstMethod {
}

class ConstInterfaceMethod {
}

class FieldValue {
    int getInt32() { return 0; }
    long getInt64() { return 0; }
    JObject getObj() { return null; }
    void setInt32(int value) {}
    void setInt64(long value) {}
    void setObj(JObject value) {}
}

class JString extends JObject {
    JString(int size, JClass type) { super(size, type); }
}

class ExceptionTable {
    int startPc;
    int endPc;
    int handlerPc;
    int catchType;
}

// Extensions to existing classes (would need to be added to those files)
// JObject extensions for array element access:
//   int getIntElement(int index)
//   void setIntElement(int index, int value)
//   long getLongElement(int index)
//   void setLongElement(int index, long value)
//   byte getByteElement(int index)
//   void setByteElement(int index, byte value)
//   short getShortElement(int index)
//   void setShortElement(int index, short value)
//   JObject getObjElement(int index)

// Flint extensions:
//   static ClassLoader findLoader(FExec ctx, String className)
//   static JClass findClassOfArray(FExec ctx, String typeName, int dimensions)
//   static JObject newObject(FExec ctx, JClass cls)
//   static JObject newArray(FExec ctx, JClass cls, int count)
//   static JObject newMultiArray(FExec ctx, JClass cls, int count, int dimensions)
//   static boolean isInstanceof(FExec ctx, JObject obj, JClass type)
//   static void terminateRequest()
//   static void freeExecution(FExec exec)
//   static void println(String msg)

// ClassLoader extensions:
//   int getConstPoolTag(int index)
//   int getConstInteger(int index)
//   float getConstFloat(int index)
//   long getConstLong(int index)
//   double getConstDouble(int index)
//   JString getConstString(FExec ctx, int index)
//   JClass getConstClass(FExec ctx, int index)
//   ConstField getConstField(FExec ctx, int index)
//   ConstMethod getConstMethod(FExec ctx, int index)
//   Object getConstInterfaceMethod(FExec ctx, int index)
//   StaticInitStatus getStaticInitStatus()
//   FieldValue getStaticField(FExec ctx, ConstField field)
//   String getName()

// FDbg extensions:
//   boolean exceptionIsEnabled()
//   void caughtException(FExec exec)
//   boolean checkStop(FExec exec)
//   void hitBreakpoint(FExec exec)
//   int getSavedOpcode(int pc, MethodData method)

// FlintAPI extensions:
//   static class Thread {
//       static void yield()
//       static void terminate(int code)
//   }
