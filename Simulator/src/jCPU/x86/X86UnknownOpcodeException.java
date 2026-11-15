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

    private final long rip;

    public X86UnknownOpcodeException(X86Core core, long rip, String what) {
        super(describe(core, rip, what));
        this.rip = rip;
    }

    /** Address of the instruction, including any prefix bytes. */
    public long getRip() {
        return rip;
    }

    private static String describe(X86Core core, long rip, String what) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Unknown x86-64 instruction at RIP=0x%016X (%s), bytes:", rip, what));
        for (int i = 0; i < MAX_INSN_BYTES; i++) {
            sb.append(String.format(" %02X", core.readMem8((int) rip + i)));
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
        return sb.toString();
    }
}
