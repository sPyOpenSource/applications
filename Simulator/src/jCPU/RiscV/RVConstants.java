
package jCPU.RiscV;

/**
 *
 * @author xuyi
 */
public class RVConstants  extends j51.intel.MCS51{
    /* Type of Functional Units */
    public static int FU_ALU = 0x0;
    public static int FU_MUL = 0x1;
    public static int FU_DIV = 0x2;
    public static int FU_FPU_ALU = 0x3;
    public static int FU_FPU_FMA = 0x4;
    public static int NUM_MAX_FU = 0x5;

    /* Extension C Quadrants */
    public static int C_QUADRANT0 = 0;
    public static int C_QUADRANT1 = 1;
    public static int C_QUADRANT2 = 2;

    /* Used for updating performance counters */
    public static int NUM_MAX_INS_TYPES = 17;
    public static int INS_TYPE_LOAD  = 0x0;
    public static int INS_TYPE_STORE = 0x1;
    public static int INS_TYPE_ATOMIC = 0x2;
    public static int INS_TYPE_SYSTEM = 0x3;
    public static int INS_TYPE_ARITMETIC = 0x4;
    public static int INS_TYPE_COND_BRANCH = 0x5;
    public static int INS_TYPE_JAL  = 0x6;
    public static int INS_TYPE_JALR = 0x7;
    public static int INS_TYPE_INT_MUL = 0x8;
    public static int INS_TYPE_INT_DIV = 0x9;
    public static int INS_TYPE_FP_LOAD = 0xa;
    public static int INS_TYPE_FP_STORE = 0xb;
    public static int INS_TYPE_FP_ADD = 0xc;
    public static int INS_TYPE_FP_MUL = 0xd;
    public static int INS_TYPE_FP_FMA = 0xe;
    public static int INS_TYPE_FP_DIV_SQRT = 0xf;
    public static int INS_TYPE_FP_MISC = 0x10;
    public static int INS_CLASS_INT   = 0x11;
    public static int INS_CLASS_FP    = 0x12;

    /* For exception handling during simulation */
    public static int SIM_ILLEGAL_OPCODE = 0x1;
    public static int SIM_COMPLEX_OPCODE = 0x2;
    public static int SIM_TIMEOUT_EXCEPTION = 0x3;
    public static int SIM_MMU_EXCEPTION = 0x4;

    /* Type of Branch instructions */
    public static int BRANCH_UNCOND = 0x0;
    public static int BRANCH_COND = 0x1;
    public static int BRANCH_FUNC_CALL = 0x2;
    public static int BRANCH_FUNC_RET = 0x3;
    
    /* Floating Point Instructions */
    public final int FLOAD_MASK  = 0x07;
    public final int FSTORE_MASK = 0x27;
    public final int FMADD_MASK  = 0x43;
    public final int FMSUB_MASK  = 0x47;
    public final int FNMSUB_MASK = 0x4B;
    public final int FNMADD_MASK = 0x4F;
    public final int F_ARITHMETIC_MASK = 0x53;

    /* Major Opcodes */
    public static final int OP_IMM_MASK = 0x13;
    public static final int OP_IMM_32_MASK = 0x1b;
    public static final int OP_MASK = 0x33;
    public static final int OP_MASK_32 = 0x3b;
    public static final int LUI_MASK = 0x37;
    public static final int AUIPC_MASK = 0x17;
    public static final int JAL_MASK = 0x6f;
    public static final int JALR_MASK = 0x67;
    public static final int BRANCH_MASK = 0x63;
    public static final int LOAD_MASK = 0x3;
    public static final int STORE_MASK = 0x23;
    public static final int FENCE_MASK = 0xf;
    public static final int CSR_MASK = 0x73;
    public static final int ATOMIC_MASK = 0x2F;
}
