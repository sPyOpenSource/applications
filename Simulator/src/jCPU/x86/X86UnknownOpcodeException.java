package jCPU.x86;

/**
 * Thrown by the x86-64 executor when it reaches an instruction it does not
 * implement.  The program counter is left on that instruction, so the
 * debugger shows where execution stopped.
 */
public class X86UnknownOpcodeException extends Exception {
    public static final long serialVersionUID = 1;

    /* Longest possible x86 instruction. */
    private static final int MAX_INSN_BYTES = 15;

    /* Bytes shown for each earlier instruction; its length is not known here. */
    private static final int HISTORY_BYTES = 8;

    private final long rip;
    private final long[] recent;

    public X86UnknownOpcodeException(X86Core core, long rip, String what) {
        this(core, rip, what, new long[0]);
    }

    /**
     * @param recent addresses of the instructions executed before this one,
     *               oldest first
     */
    public X86UnknownOpcodeException(X86Core core, long rip, String what, long[] recent) {
        super(describe(core, rip, what, recent));
        this.rip = rip;
        this.recent = recent.clone();
    }

    /** Address of the instruction, including any prefix bytes. */
    public long getRip() {
        return rip;
    }

    /** Addresses of the instructions executed before this one, oldest first. */
    public long[] getRecentInstructions() {
        return recent.clone();
    }

    private static String describe(X86Core core, long rip, String what, long[] recent) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Unknown x86-64 instruction at RIP=0x%016X (%s), bytes:", rip, what));
        for (int i = 0; i < MAX_INSN_BYTES; i++) {
            sb.append(String.format(" %02X", core.readMem8(rip + i)));
        }
        String[] names = {
            "RAX", "RCX", "RDX", "RBX", "RSP", "RBP", "RSI", "RDI",
            "R8", "R9", "R10", "R11", "R12", "R13", "R14", "R15"
        };
        int[] regs = {
            X86Core.REG_RAX, X86Core.REG_RCX, X86Core.REG_RDX, X86Core.REG_RBX,
            X86Core.REG_RSP, X86Core.REG_RBP, X86Core.REG_RSI, X86Core.REG_RDI,
            X86Core.REG_R8, X86Core.REG_R9, X86Core.REG_R10, X86Core.REG_R11,
            X86Core.REG_R12, X86Core.REG_R13, X86Core.REG_R14, X86Core.REG_R15
        };
        for (int i = 0; i < regs.length; i++) {
            sb.append(i % 4 == 0 ? "\n  " : "  ");
            sb.append(String.format("%-3s=0x%016X", names[i], core.getReg64(regs[i])));
        }
        sb.append(String.format("\n  RFLAGS=0x%016X", core.getRflags64()));
        if (recent.length > 0) {
            sb.append(String.format("\n  Last %d instructions before this one, oldest first:", recent.length));
            for (long addr : recent) {
                sb.append(String.format("\n    RIP=0x%016X ", addr));
                for (int i = 0; i < HISTORY_BYTES; i++) {
                    sb.append(String.format(" %02X", core.readMem8(addr + i)));
                }
            }
        }
        return sb.toString();
    }
}
