
package name.bizna.jarm;

/**
 *
 * @author xuyi
 */
public interface ARMConstants {
    public static final int EXCEPTION_VECTOR_RESET = 0;
    public static final int EXCEPTION_VECTOR_UNDEFINED = 1;
    public static final int EXCEPTION_VECTOR_SUPERVISOR_CALL = 2;
    public static final int EXCEPTION_VECTOR_PREFETCH_ABORT = 3;
    public static final int EXCEPTION_VECTOR_DATA_ABORT = 4;
    /* vector #5 only used when Virtualization Extensions are present */
    public static final int EXCEPTION_VECTOR_IRQ = 6;
    public static final int EXCEPTION_VECTOR_FIQ = 7;
    /* APSR/CPSR bits (B1-1148) */
    public static final int CPSR_BIT_N = 31;
    public static final int CPSR_BIT_Z = 30;
    public static final int CPSR_BIT_C = 29;
    public static final int CPSR_BIT_V = 28;
    public static final int CPSR_MASK_CLEAR_CONDITIONS = ~0 >>> 4;
    public static final int CPSR_BIT_Q = 27;
    public static final int CPSR_SHIFT_ITLO = 25;
    public static final int CPSR_MASK_ITLO  = 3;
    public static final int CPSR_SHIFT_ITHI = 10;
    public static final int CPSR_MASK_ITHI  = 63;
    public static final int CPSR_POSTSHIFT_ITHI = 2;
    public static final int CPSR_BIT_J   = 24;
    public static final int CPSR_SHIFT_GE = 16;
    public static final int CPSR_MASK_GE = 15;
    public static final int CPSR_BIT_E = 9;
    public static final int CPSR_BIT_A = 8;
    public static final int CPSR_BIT_I = 7;
    public static final int CPSR_BIT_F = 6;
    public static final int CPSR_BIT_T = 5;
    public static final int CPSR_MASK_M = 0x1F;
    public static final int APSR_READ_MASK = 0xF80F0100;
    public static final int APSR_WRITE_MASK = 0xF80F0000;
    public static final int CPSR_USER_READ_MASK = 0xF8FFF3DF;
    public static final int CPSR_READ_MASK = 0xFFFFFFFF;
    /* the ARM ARM ARM says we should be able to write the E bit this way but also deprecates it
     * we'll let them write it because it costs us nothing
     * also! don't write the mode here, we'll handle that specially in code that writes CPSR */
    public static final int CPSR_WRITE_MASK = 0xF80F03C0;
    /* exception returns always restore the whole CPSR */
    public static final int SPSR_READ_MASK  = 0xFFFFFFFF;
    /* as above, don't write the mode here, we'll handle that specially in code */
    public static final int SPSR_WRITE_MASK = 0xFF0FFFEF;
}
