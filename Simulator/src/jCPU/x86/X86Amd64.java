package jCPU.x86;

import static jCPU.x86.X86Core.*;

import java.math.BigInteger;

/**
 * Executor for x86-64 long mode and for 32-bit protected mode.
 *
 * Each step decodes one whole instruction (prefixes, REX, opcode, ModR/M,
 * SIB, displacement, immediate) while advancing {@link #ip}, so the length
 * of the instruction follows from the decode and RIP is set from it.  A
 * branch replaces {@link #ip} with its target.
 *
 * Registers are numbered 0..15 (RAX, RCX, RDX, RBX, RSP, RBP, RSI, RDI,
 * R8..R15) and operand sizes are given in bytes (1, 2, 4 or 8).
 *
 * The code segment decides how instructions are decoded: 64-bit code while
 * long mode is active and CS has the L bit, otherwise 32-bit code (CS has
 * the D bit).  In 32-bit code there is no REX prefix, addresses and the
 * stack are 32 bits wide, and segment bases are added to every memory
 * address.  16-bit code is not supported.
 */
public class X86Amd64 implements X86Core.iExecutor
{
    private static final long CF = 1L << FLAG_CF;
    private static final long PF = 1L << FLAG_PF;
    private static final long AF = 1L << FLAG_AF;
    private static final long ZF = 1L << FLAG_ZF;
    private static final long SF = 1L << FLAG_SF;
    private static final long IF = 1L << FLAG_IF;
    private static final long DF = 1L << FLAG_DF;
    private static final long OF = 1L << FLAG_OF;
    private static final long RF = 1L << FLAG_RF;
    private static final long STATUS = CF | PF | AF | ZF | SF | OF;

    private static final int RAX = 0, RCX = 1, RDX = 2, RBX = 3, RSP = 4, RBP = 5, RSI = 6, RDI = 7;

    private static final int ADD = 0, OR = 1, ADC = 2, SBB = 3, AND = 4, XOR = 6, CMP = 7;

    /* Longest possible x86 instruction. */
    private static final int MAX_INSN_BYTES = 15;

    /* ---------------- state of the instruction being executed ---------------- */

    private X86Core core;

    /* 64-bit code, or 32-bit code (protected or compatibility mode). */
    private boolean code64;
    /* Segment override prefix (SEG_ES..SEG_GS), or -1. */
    private int segOverride;

    /* Address of the instruction, including any prefix. */
    private long insnStart = 0;
    /* Address of the next byte to decode; after decoding, of the next instruction. */
    private long ip = 0;

    private boolean rexPresent;
    private int rexW, rexR, rexX, rexB;
    /* Operand size in bytes for instructions that follow the 66/REX.W rules. */
    private int opSize;
    /* Address size override (prefix 67): addresses are 32 bits wide. */
    private boolean addr32;
    /* Repeat prefix: 0, 0xF2 (REPNE) or 0xF3 (REP/REPE). */
    private int rep;

    /* ModR/M: reg and rm include the REX extension bits; ea is valid when mod != 3.
     * ea is the linear address, offset the address within its segment (what
     * LEA returns); they differ only for a segment with a base in 32-bit code. */
    private int mod, reg, rm;
    private long ea;
    private long offset;

    /* A data breakpoint the current instruction hit, reported once it has run, or -1. */
    private int dataBreakpoint;
    private long dataBreakpointAddr;
    private boolean dataBreakpointWrite;

    /* Addresses of the most recently executed instructions, as a ring buffer.
     * Reported when execution stops on an unknown instruction, to show how
     * the program got there. */
    private static final int HISTORY_SIZE = 16;
    private final long[] history = new long[HISTORY_SIZE];
    private long historyCount = 0;

    @Override
    public int step(X86Core core) throws Exception {
        this.core = core;
        long rip = core.getPC64();
        core.addTimestampCounter(1);
        if (core.firmwareTrap(rip)) {
            history[(int) (historyCount++ % HISTORY_SIZE)] = rip;
            return 1;
        }
        insnStart = ip = core.getPC64();
        rexPresent = false;
        rexW = rexR = rexX = rexB = 0;
        addr32 = false;
        rep = 0;
        segOverride = -1;
        boolean opSize16 = false;
        code64 = core.isLongMode() && core.isCodeLong();
        if (!code64 && !core.isCodeSize32()) {
            throw unknownOpcode("16-bit code; this executor runs 64-bit and 32-bit code");
        }
        dataBreakpoint = -1;
        if (core.hasBreakpoints() && (flags() & RF) == 0) {
            // An instruction breakpoint is a fault: deliver #DB before the instruction.
            int bp = core.breakpointHit(rip, 1, ACCESS_EXECUTE);
            if (bp >= 0) {
                deliverDebugException(bp, rip);
                core.setPC64(code64 ? ip : ip & 0xFFFFFFFFL);
                return 1;
            }
        }

        int opcode;
        for (;;) {
            opcode = fetch8();
            if (opcode == 0x66) {
                opSize16 = true;
            } else if (opcode == 0x67) {
                if (!code64) {
                    throw unknownOpcode("prefix 0x67 (16-bit addressing)");
                }
                addr32 = true;
            } else if (opcode == 0xF2 || opcode == 0xF3) {
                rep = opcode;
            } else if (opcode == 0xF0) {
                // LOCK: there is one CPU, so every instruction is already atomic.
            } else if (opcode == 0x26 || opcode == 0x2E || opcode == 0x36 || opcode == 0x3E) {
                // ES, CS, SS, DS; in 64-bit code their bases are always 0.
                segOverride = (opcode >>> 3) & 3;   // 26 -> ES, 2E -> CS, 36 -> SS, 3E -> DS
            } else if (opcode == 0x64 || opcode == 0x65) {
                if (code64) {
                    throw unknownOpcode(String.format("segment override 0x%02X (FS/GS)", opcode));
                }
                segOverride = opcode == 0x64 ? SEG_FS : SEG_GS;
            } else {
                break;
            }
            if (ip - insnStart >= MAX_INSN_BYTES) {
                throw unknownOpcode("instruction longer than 15 bytes");
            }
        }

        // A REX prefix counts only when it is the last byte before the opcode.
        // In 32-bit code these bytes are INC and DEC.
        if (code64 && (opcode & 0xF0) == 0x40) {
            rexPresent = true;
            rexW = (opcode >> 3) & 1;
            rexR = (opcode >> 2) & 1;
            rexX = (opcode >> 1) & 1;
            rexB = opcode & 1;
            opcode = fetch8();
        }
        opSize = rexW != 0 ? 8 : opSize16 ? 2 : 4;

        // Throws X86UnknownOpcodeException for anything that is not implemented,
        // leaving RIP on the offending instruction.
        if (opcode == 0x0F) {
            executeTwoByte(fetch8());
        } else {
            executeOneByte(opcode);
        }
        core.setPC64(code64 ? ip : ip & 0xFFFFFFFFL);
        history[(int) (historyCount++ % HISTORY_SIZE)] = insnStart;
        if (dataBreakpoint >= 0) {
            // A data breakpoint is a trap: the instruction has run.
            deliverDebugException(dataBreakpoint, ip);
            core.setPC64(code64 ? ip : ip & 0xFFFFFFFFL);
            return 1;
        }
        // RF suppresses an instruction breakpoint for one instruction only.
        // IRETD keeps the value it popped so the resumed instruction is not
        // reported again.
        if (opcode != 0xCF && (flags() & RF) != 0) {
            core.setRflags64(flags() & ~RF);
        }
        return 1;
    }

    /**
     * CPUID reports a Samsung R510-era Core 2 Duo identity.  Only features
     * this executor implements are advertised: TSC and CMOV in leaf 1, long
     * mode in leaf 0x80000001.  No FPU, SSE or MSRs.
     */
    private void cpuid(int leaf, int subleaf) {
        int a = 0, b = 0, c = 0, d = 0;
        switch (leaf) {
            case 0:                                 // max leaf, vendor "GenuineIntel"
                a = 1;
                b = 0x756E6547;                     // "Genu"
                d = 0x49656E69;                     // "ineI"
                c = 0x6C65746E;                     // "ntel"
                break;
            case 1:
                a = 0x00010676;                     // family 6, model 23, stepping 6
                d = (1 << 4) | (1 << 15);           // TSC, CMOV
                break;
            case 0x80000000:
                a = 0x80000001;
                break;
            case 0x80000001:
                d = 1 << 29;                        // LM
                break;
            default:
                break;
        }
        setReg(RAX, 4, a & 0xFFFFFFFFL);
        setReg(RBX, 4, b & 0xFFFFFFFFL);
        setReg(RCX, 4, c & 0xFFFFFFFFL);
        setReg(RDX, 4, d & 0xFFFFFFFFL);
    }

    /* The recorded instruction addresses, oldest first. */
    private long[] recentInstructions() {
        int n = (int) Math.min(historyCount, HISTORY_SIZE);
        long[] recent = new long[n];
        for (int i = 0; i < n; i++) {
            recent[i] = history[(int) ((historyCount - n + i) % HISTORY_SIZE)];
        }
        return recent;
    }

    /**
     * Stop execution on an instruction that is not implemented.  Skipping it
     * would mean guessing its length and silently continuing with a wrong
     * machine state, so the caller gets an exception instead.
     */
    private X86UnknownOpcodeException unknownOpcode(String what) {
        return new X86UnknownOpcodeException(core, insnStart,
                code64 ? what : what + " in 32-bit code", recentInstructions());
    }

    @Override
    public String getDecodeAt(X86Core core, int pc) {
        return String.format("%02X", core.readMem8(pc));
    }

    /* ---------------- instruction bytes ---------------- */

    private int fetch8() {
        return core.readMem8(ip++);
    }

    private int fetch16() {
        int v = core.readMem16(ip);
        ip += 2;
        return v;
    }

    private int fetch32() {
        int v = core.readMem32(ip);
        ip += 4;
        return v;
    }

    private long fetch64() {
        long v = core.readMem64(ip);
        ip += 8;
        return v;
    }

    /* Size of an immediate that follows the operand size, but is never 8 bytes. */
    private int immZBytes() {
        return opSize == 2 ? 2 : 4;
    }

    /* Immediate of immZBytes() bytes, sign-extended. */
    private long fetchImmZ() {
        return opSize == 2 ? (short) fetch16() : fetch32();
    }

    private long fetchImm8() {
        return (byte) fetch8();
    }

    /* ---------------- registers and memory ---------------- */

    private static long mask(int size) {
        return size == 8 ? -1L : (1L << (size * 8)) - 1;
    }

    private static long signBit(int size) {
        return 1L << (size * 8 - 1);
    }

    /* Sign-extend the low 'size' bytes of v. */
    private static long sx(long v, int size) {
        int shift = 64 - size * 8;
        return (v << shift) >> shift;
    }

    private long reg64(int r) {
        return core.getReg64(REG_RAX + r);
    }

    private void setReg64(int r, long v) {
        core.setReg64(REG_RAX + r, v);
    }

    /* Without a REX prefix, byte registers 4..7 are AH, CH, DH and BH. */
    private boolean isHighByte(int r, int size) {
        return size == 1 && !rexPresent && r >= 4 && r < 8;
    }

    private long getReg(int r, int size) {
        if (isHighByte(r, size)) {
            return (reg64(r - 4) >>> 8) & 0xFF;
        }
        return reg64(r) & mask(size);
    }

    /* A 4-byte write clears the upper half; 1 and 2-byte writes keep the rest. */
    private void setReg(int r, int size, long v) {
        if (isHighByte(r, size)) {
            setReg64(r - 4, (reg64(r - 4) & ~0xFF00L) | ((v & 0xFF) << 8));
        } else if (size >= 4) {
            setReg64(r, v & mask(size));
        } else {
            setReg64(r, (reg64(r) & ~mask(size)) | (v & mask(size)));
        }
    }

    /* Remember the first data breakpoint an access hits. */
    private void watch(long addr, int size, int access) {
        if (dataBreakpoint < 0) {
            int bp = core.breakpointHit(addr, size, access);
            if (bp >= 0) {
                dataBreakpoint = bp;
                dataBreakpointAddr = addr;
                dataBreakpointWrite = access == ACCESS_WRITE;
            }
        }
    }

    private long readMem(long addr, int size) {
        if (core.hasBreakpoints()) watch(addr, size, ACCESS_READ);
        switch (size) {
            case 1: return core.readMem8(addr);
            case 2: return core.readMem16(addr);
            case 4: return core.readMem32(addr) & 0xFFFFFFFFL;
            default: return core.readMem64(addr);
        }
    }

    private void writeMem(long addr, int size, long v) {
        if (core.hasBreakpoints()) watch(addr, size, ACCESS_WRITE);
        switch (size) {
            case 1: core.writeMem8(addr, (int) (v & 0xFF)); break;
            case 2: core.writeMem16(addr, (int) (v & 0xFFFF)); break;
            case 4: core.writeMem32(addr, (int) v); break;
            default: core.writeMem64(addr, v); break;
        }
    }

    /* Linear address of an offset in a segment.  Only 32-bit code uses segment bases. */
    private long linear(int seg, long off) {
        return code64 ? off : (core.getSegBase(seg) + off) & 0xFFFFFFFFL;
    }

    /* The segment of a data access: the override prefix, or the default. */
    private int dataSeg(int defaultSeg) {
        return segOverride >= 0 ? segOverride : defaultSeg;
    }

    /* Address size in bytes: 8, or 4 with prefix 67, in 64-bit code; 4 in 32-bit code. */
    private int addrSize() {
        return code64 && !addr32 ? 8 : 4;
    }

    /* The stack is 64 bits wide in 64-bit code; in 32-bit code SS is a 32-bit segment. */
    private void push(long v, int size) {
        if (code64) {
            long sp = reg64(RSP) - size;
            writeMem(sp, size, v);
            setReg64(RSP, sp);
        } else {
            long sp = (reg64(RSP) - size) & 0xFFFFFFFFL;
            writeMem(linear(SEG_SS, sp), size, v);
            setReg(RSP, 4, sp);
        }
    }

    private long pop(int size) {
        long sp = code64 ? reg64(RSP) : reg64(RSP) & 0xFFFFFFFFL;
        long v = readMem(linear(SEG_SS, sp), size);
        addStack(size);
        return v;
    }

    /* RSP += delta, or ESP in 32-bit code. */
    private void addStack(long delta) {
        if (code64) {
            setReg64(RSP, reg64(RSP) + delta);
        } else {
            setReg(RSP, 4, reg64(RSP) + delta);
        }
    }

    /* PUSH and POP use 8 bytes (4 in 32-bit code) unless prefix 66 selects 2. */
    private int stackSize() {
        return opSize == 2 ? 2 : code64 ? 8 : 4;
    }

    /*
     * Size of a near branch target and return address: 8 in 64-bit code, 4
     * in 32-bit code.  With prefix 66 a branch in 32-bit code uses a 16-bit
     * IP, which is not implemented.
     */
    private int nearSize() throws X86UnknownOpcodeException {
        if (!code64 && opSize == 2) {
            throw unknownOpcode("near branch with prefix 0x66 (16-bit IP)");
        }
        return code64 ? 8 : 4;
    }

    /* ---------------- ModR/M ---------------- */

    /**
     * Decode the ModR/M byte with its SIB byte and displacement.
     *
     * @param immBytes number of immediate bytes that follow, needed because
     *                 RIP-relative addresses count from the end of the instruction
     */
    private void decodeModRm(int immBytes) {
        int modrm = fetch8();
        mod = modrm >>> 6;
        reg = ((modrm >>> 3) & 7) | (rexR << 3);
        rm = modrm & 7;
        if (mod == 3) {
            rm |= rexB << 3;
            return;
        }

        long addr;
        boolean ripRelative = false;
        int defaultSeg = SEG_DS;                    // SS when the base is rSP or rBP
        if (rm == 4) {
            int sib = fetch8();
            int scale = sib >>> 6;
            int index = ((sib >>> 3) & 7) | (rexX << 3);
            int base = sib & 7;
            if (base == 5 && mod == 0) {
                addr = fetch32();                   // disp32, no base register
            } else {
                addr = reg64(base | (rexB << 3));
                if (base == RSP || base == RBP) defaultSeg = SEG_SS;
            }
            if (index != 4) {                       // index 4 without REX.X: no index
                addr += reg64(index) << scale;
            }
        } else if (rm == 5 && mod == 0) {
            addr = fetch32();                       // RIP-relative; absolute in 32-bit code
            ripRelative = code64;
        } else {
            addr = reg64(rm | (rexB << 3));
            if (rm == RBP) defaultSeg = SEG_SS;
        }
        if (mod == 1) {
            addr += (byte) fetch8();
        } else if (mod == 2) {
            addr += fetch32();
        }
        if (ripRelative) {
            addr += ip + immBytes;
        }
        offset = addrSize() == 4 ? addr & 0xFFFFFFFFL : addr;
        ea = linear(dataSeg(defaultSeg), offset);
    }

    /* The /digit of a group opcode: the reg field without the REX.R bit. */
    private int group() {
        return reg & 7;
    }

    /* The /digit of the ModR/M byte that is about to be decoded. */
    private int peekGroup() {
        return (core.readMem8(ip) >>> 3) & 7;
    }

    private long readRm(int size) {
        return mod == 3 ? getReg(rm, size) : readMem(ea, size);
    }

    private void writeRm(int size, long v) {
        if (mod == 3) {
            setReg(rm, size, v);
        } else {
            writeMem(ea, size, v);
        }
    }

    /* ---------------- flags ---------------- */

    private long flags() {
        return core.getRflags64();
    }

    private void setFlags(long mask, long value) {
        core.setRflags64((flags() & ~mask) | (value & mask));
    }

    private boolean flag(long f) {
        return (flags() & f) != 0;
    }

    /* SF, ZF and PF for a result. */
    private static long szp(long res, int size) {
        long f = 0;
        if ((res & mask(size)) == 0) f |= ZF;
        if ((res & signBit(size)) != 0) f |= SF;
        if ((Integer.bitCount((int) (res & 0xFF)) & 1) == 0) f |= PF;
        return f;
    }

    private long add(long a, long b, long carryIn, int size, long changed) {
        long res = a + b + carryIn;
        long f = szp(res, size);
        if ((((a & b) | ((a | b) & ~res)) & signBit(size)) != 0) f |= CF;
        if ((((a ^ res) & (b ^ res)) & signBit(size)) != 0) f |= OF;
        if (((a ^ b ^ res) & 0x10) != 0) f |= AF;
        setFlags(changed, f);
        return res & mask(size);
    }

    private long sub(long a, long b, long borrowIn, int size, long changed) {
        long res = a - b - borrowIn;
        long f = szp(res, size);
        if ((((~a & b) | ((~a | b) & res)) & signBit(size)) != 0) f |= CF;
        if ((((a ^ b) & (a ^ res)) & signBit(size)) != 0) f |= OF;
        if (((a ^ b ^ res) & 0x10) != 0) f |= AF;
        setFlags(changed, f);
        return res & mask(size);
    }

    private long logic(long res, int size) {
        setFlags(STATUS, szp(res, size));           // CF, OF and AF are cleared
        return res & mask(size);
    }

    /* The eight operations that share the encodings 00..3F and 80..83. */
    private long alu(int op, long a, long b, int size) {
        long carry = flag(CF) ? 1 : 0;
        switch (op) {
            case ADD: return add(a, b, 0, size, STATUS);
            case OR:  return logic(a | b, size);
            case ADC: return add(a, b, carry, size, STATUS);
            case SBB: return sub(a, b, carry, size, STATUS);
            case AND: return logic(a & b, size);
            case XOR: return logic(a ^ b, size);
            default:  return sub(a, b, 0, size, STATUS);    // SUB and CMP
        }
    }

    private boolean condition(int cc) {
        long f = flags();
        boolean r;
        switch (cc >> 1) {
            case 0: r = (f & OF) != 0; break;
            case 1: r = (f & CF) != 0; break;
            case 2: r = (f & ZF) != 0; break;
            case 3: r = (f & (CF | ZF)) != 0; break;
            case 4: r = (f & SF) != 0; break;
            case 5: r = (f & PF) != 0; break;
            case 6: r = ((f & SF) != 0) != ((f & OF) != 0); break;
            default: r = (f & ZF) != 0 || ((f & SF) != 0) != ((f & OF) != 0); break;
        }
        return (cc & 1) != 0 ? !r : r;
    }

    /* ---------------- one-byte opcodes ---------------- */

    private void executeOneByte(int opcode) throws Exception {
        // INC r, DEC r: 40..4F, in 32-bit code only (in 64-bit code they are REX)
        if (!code64 && (opcode & 0xF0) == 0x40) {
            int r = opcode & 7;
            setReg(r, opSize, incDec(getReg(r, opSize), opSize, opcode >= 0x48));
            return;
        }

        // ADD, OR, ADC, SBB, AND, SUB, XOR, CMP: 00..3F, six forms each
        if (opcode < 0x40 && (opcode & 7) < 6) {
            int op = opcode >>> 3;
            int form = opcode & 7;
            int size = (form & 1) == 0 ? 1 : opSize;
            if (form < 2) {                         // r/m, r
                decodeModRm(0);
                long res = alu(op, readRm(size), getReg(reg, size), size);
                if (op != CMP) writeRm(size, res);
            } else if (form < 4) {                  // r, r/m
                decodeModRm(0);
                long res = alu(op, getReg(reg, size), readRm(size), size);
                if (op != CMP) setReg(reg, size, res);
            } else {                                // AL/eAX/RAX, imm
                long imm = form == 4 ? fetchImm8() : fetchImmZ();
                long res = alu(op, getReg(RAX, size), imm, size);
                if (op != CMP) setReg(RAX, size, res);
            }
            return;
        }

        // PUSH r: 50..57, POP r: 58..5F
        if ((opcode & 0xF8) == 0x50) {
            push(getReg((opcode & 7) | (rexB << 3), stackSize()), stackSize());
            return;
        }
        if ((opcode & 0xF8) == 0x58) {
            setReg((opcode & 7) | (rexB << 3), stackSize(), pop(stackSize()));
            return;
        }

        // Jcc rel8: 70..7F
        if ((opcode & 0xF0) == 0x70) {
            nearSize();
            long rel = fetchImm8();
            if (condition(opcode & 0xF)) ip += rel;
            return;
        }

        // XCHG rAX, r: 91..97, and 90 with REX.B.  Plain 90 is NOP.
        if ((opcode & 0xF8) == 0x90) {
            int r = (opcode & 7) | (rexB << 3);
            if (r != RAX) {
                long tmp = getReg(RAX, opSize);
                setReg(RAX, opSize, getReg(r, opSize));
                setReg(r, opSize, tmp);
            }
            return;
        }

        // MOV r8, imm8: B0..B7
        if ((opcode & 0xF8) == 0xB0) {
            setReg((opcode & 7) | (rexB << 3), 1, fetch8());
            return;
        }

        // MOV r, imm: B8..BF.  The only instruction with a 64-bit immediate.
        if ((opcode & 0xF8) == 0xB8) {
            long imm = opSize == 8 ? fetch64() : opSize == 4 ? fetch32() : fetch16();
            setReg((opcode & 7) | (rexB << 3), opSize, imm);
            return;
        }

        switch (opcode) {
            case 0x06: case 0x0E: case 0x16: case 0x1E:     // PUSH ES, CS, SS, DS (32-bit code)
                if (code64) break;
                push(core.getSegSelector(opcode >>> 3), stackSize());
                return;
            case 0x07: case 0x17: case 0x1F:                // POP ES, SS, DS (32-bit code)
                if (code64) break;
                loadSegment(opcode >>> 3, (int) pop(stackSize()) & 0xFFFF);
                return;
            case 0x8C: {                            // MOV r/m, Sreg
                int seg = peekGroup();
                if (seg >= SEG_COUNT) break;
                decodeModRm(0);
                writeRm(mod == 3 ? opSize : 2, core.getSegSelector(seg));
                return;
            }
            case 0x8E: {                            // MOV Sreg, r/m
                int seg = peekGroup();
                if (seg >= SEG_COUNT || seg == SEG_CS) break;
                decodeModRm(0);
                loadSegment(seg, (int) readRm(2));
                return;
            }
            case 0x9A: {                            // CALL ptr16:32 (32-bit code)
                if (code64) break;
                long target = farOffsetImm();
                int selector = fetch16();
                farCall(selector, target);
                return;
            }
            case 0xEA: {                            // JMP ptr16:32 (32-bit code)
                if (code64) break;
                long target = farOffsetImm();
                farJump(fetch16(), target);
                return;
            }
            case 0xCA:                              // RETF imm16
            case 0xCB: {                            // RETF
                int extra = opcode == 0xCA ? fetch16() : 0;
                int size = farSize();
                long target = pop(size);
                int selector = (int) pop(size) & 0xFFFF;
                addStack(extra);
                farJump(selector, target);
                return;
            }
            case 0x60: {                            // PUSHA/PUSHAD (32-bit code)
                if (code64) break;
                long sp = getReg(RSP, opSize);
                for (int r = RAX; r <= RDI; r++) {
                    push(r == RSP ? sp : getReg(r, opSize), opSize);
                }
                return;
            }
            case 0x61:                              // POPA/POPAD (32-bit code); the saved rSP is skipped
                if (code64) break;
                for (int r = RDI; r >= RAX; r--) {
                    long v = pop(opSize);
                    if (r != RSP) setReg(r, opSize, v);
                }
                return;
            case 0x63: {                            // MOVSXD r, r/m32 (ARPL in 32-bit code)
                if (!code64) break;
                decodeModRm(0);
                if (opSize == 8) {
                    setReg(reg, 8, sx(readRm(4), 4));
                } else {
                    setReg(reg, opSize, readRm(opSize));
                }
                return;
            }
            case 0x68:                              // PUSH imm32
                push(fetchImmZ(), stackSize());
                return;
            case 0x6A:                              // PUSH imm8
                push(fetchImm8(), stackSize());
                return;
            case 0x69:                              // IMUL r, r/m, imm32
            case 0x6B: {                            // IMUL r, r/m, imm8
                decodeModRm(opcode == 0x69 ? immZBytes() : 1);
                long a = readRm(opSize);
                long imm = opcode == 0x69 ? fetchImmZ() : fetchImm8();
                setReg(reg, opSize, imul(a, imm, opSize));
                return;
            }
            case 0x6C: case 0x6D: case 0x6E: case 0x6F:     // INS, OUTS
                stringOp(opcode);
                return;

            case 0x80:                              // group 1: r/m8, imm8
            case 0x81:                              // group 1: r/m, imm32
            case 0x82:                              // the same as 80, 32-bit code only
            case 0x83: {                            // group 1: r/m, imm8
                if (opcode == 0x82 && code64) break;
                int size = opcode == 0x80 || opcode == 0x82 ? 1 : opSize;
                decodeModRm(opcode == 0x81 ? immZBytes() : 1);
                long imm = opcode == 0x81 ? fetchImmZ() : fetchImm8();
                long res = alu(group(), readRm(size), imm, size);
                if (group() != CMP) writeRm(size, res);
                return;
            }
            case 0x84:                              // TEST r/m8, r8
            case 0x85: {                            // TEST r/m, r
                int size = opcode == 0x84 ? 1 : opSize;
                decodeModRm(0);
                logic(readRm(size) & getReg(reg, size), size);
                return;
            }
            case 0x86:                              // XCHG r/m8, r8
            case 0x87: {                            // XCHG r/m, r
                int size = opcode == 0x86 ? 1 : opSize;
                decodeModRm(0);
                long tmp = readRm(size);
                writeRm(size, getReg(reg, size));
                setReg(reg, size, tmp);
                return;
            }
            case 0x88:                              // MOV r/m8, r8
            case 0x89: {                            // MOV r/m, r
                int size = opcode == 0x88 ? 1 : opSize;
                decodeModRm(0);
                writeRm(size, getReg(reg, size));
                return;
            }
            case 0x8A:                              // MOV r8, r/m8
            case 0x8B: {                            // MOV r, r/m
                int size = opcode == 0x8A ? 1 : opSize;
                decodeModRm(0);
                setReg(reg, size, readRm(size));
                return;
            }
            case 0x8D: {                            // LEA r, m
                decodeModRm(0);
                if (mod == 3) break;
                setReg(reg, opSize, offset);
                return;
            }
            case 0x8F: {                            // POP r/m
                if (peekGroup() != 0) break;
                // The address is computed after RSP has been incremented.
                long v = pop(stackSize());
                decodeModRm(0);
                writeRm(stackSize(), v);
                return;
            }

            case 0x98:                              // CBW, CWDE, CDQE
                setReg(RAX, opSize, sx(getReg(RAX, opSize / 2), opSize / 2));
                return;
            case 0x99:                              // CWD, CDQ, CQO
                setReg(RDX, opSize, (getReg(RAX, opSize) & signBit(opSize)) != 0 ? -1 : 0);
                return;
            case 0x9C:                              // PUSHFQ
                push(flags(), stackSize());
                return;
            case 0x9D:                              // POPFQ
                core.setRflags64(pop(stackSize()) | 2); // bit 1 is always set
                return;
            case 0x9E:                              // SAHF
                setFlags(CF | PF | AF | ZF | SF, reg64(RAX) >>> 8);
                return;
            case 0x9F:                              // LAHF
                setReg64(RAX, (reg64(RAX) & ~0xFF00L)
                        | (((flags() & (CF | PF | AF | ZF | SF)) | 2) << 8));
                return;

            case 0xA0: case 0xA1: case 0xA2: case 0xA3: {   // MOV AL/rAX <-> absolute address
                int size = (opcode & 1) == 0 ? 1 : opSize;
                long addr = addrSize() == 4 ? fetch32() & 0xFFFFFFFFL : fetch64();
                addr = linear(dataSeg(SEG_DS), addr);
                if (opcode < 0xA2) {
                    setReg(RAX, size, readMem(addr, size));
                } else {
                    writeMem(addr, size, getReg(RAX, size));
                }
                return;
            }
            case 0xA4: case 0xA5: case 0xA6: case 0xA7:     // MOVS, CMPS
            case 0xAA: case 0xAB: case 0xAC: case 0xAD:     // STOS, LODS
            case 0xAE: case 0xAF:                           // SCAS
                stringOp(opcode);
                return;
            case 0xA8:                              // TEST AL, imm8
                logic(getReg(RAX, 1) & fetchImm8(), 1);
                return;
            case 0xA9:                              // TEST rAX, imm32
                logic(getReg(RAX, opSize) & fetchImmZ(), opSize);
                return;

            case 0xC0: case 0xC1:                   // shift group: r/m, imm8
            case 0xD0: case 0xD1:                   // shift group: r/m, 1
            case 0xD2: case 0xD3: {                 // shift group: r/m, CL
                int size = (opcode & 1) == 0 ? 1 : opSize;
                decodeModRm(opcode < 0xD0 ? 1 : 0);
                int count = opcode < 0xD0 ? fetch8() : opcode < 0xD2 ? 1 : (int) getReg(RCX, 1);
                writeRm(size, shift(group(), readRm(size), count, size));
                return;
            }
            case 0xC2: {                            // RET imm16
                int extra = fetch16();
                ip = pop(nearSize());
                addStack(extra);
                return;
            }
            case 0xC3:                              // RET
                ip = pop(nearSize());
                return;
            case 0xC6:                              // MOV r/m8, imm8
            case 0xC7: {                            // MOV r/m, imm32
                if (peekGroup() != 0) break;
                int size = opcode == 0xC6 ? 1 : opSize;
                decodeModRm(opcode == 0xC6 ? 1 : immZBytes());
                writeRm(size, opcode == 0xC6 ? fetchImm8() : fetchImmZ());
                return;
            }
            case 0xC9:                              // LEAVE
                if (code64) {
                    setReg64(RSP, reg64(RBP));
                    setReg64(RBP, pop(8));
                } else {
                    setReg(RSP, 4, reg64(RBP));
                    setReg(RBP, stackSize(), pop(stackSize()));
                }
                return;
            case 0xCC:                              // INT3
                core.handleInt(3);
                return;
            case 0xCD:                              // INT imm8
                core.handleInt(fetch8());
                return;
            case 0xCF: {                            // IRETD
                if (code64) break;
                farSize();
                long sp = reg64(RSP) & 0xFFFFFFFFL;
                long target = core.readMem32(linear(SEG_SS, sp)) & 0xFFFFFFFFL;
                int selector = core.readMem32(linear(SEG_SS, sp + 4)) & 0xFFFF;
                long savedFlags = core.readMem32(linear(SEG_SS, sp + 8)) & 0xFFFFFFFFL;
                long d = codeDescriptor(selector);
                addStack(12);
                core.setRflags64(savedFlags | 2);   // bit 1 is always set
                enterCode(selector, d, target);
                return;
            }

            case 0xE0: case 0xE1: case 0xE2: {      // LOOPNE, LOOPE, LOOP
                nearSize();
                long rel = fetchImm8();
                int size = addrSize();
                long count = (getReg(RCX, size) - 1) & mask(size);
                setReg(RCX, size, count);
                boolean taken = count != 0
                        && (opcode == 0xE2 || flag(ZF) == (opcode == 0xE1));
                if (taken) ip += rel;
                return;
            }
            case 0xE3: {                            // JRCXZ, JECXZ
                nearSize();
                long rel = fetchImm8();
                if (getReg(RCX, addrSize()) == 0) ip += rel;
                return;
            }
            case 0xE4: case 0xE5: {                 // IN AL/eAX, imm8
                int size = opcode == 0xE4 ? 1 : immZBytes();
                setReg(RAX, size, portIn(fetch8(), size));
                return;
            }
            case 0xE6: case 0xE7: {                 // OUT imm8, AL/eAX
                int size = opcode == 0xE6 ? 1 : immZBytes();
                portOut(fetch8(), size, getReg(RAX, size));
                return;
            }
            case 0xEC: case 0xED: {                 // IN AL/eAX, DX
                int size = opcode == 0xEC ? 1 : immZBytes();
                setReg(RAX, size, portIn((int) getReg(RDX, 2), size));
                return;
            }
            case 0xEE: case 0xEF: {                 // OUT DX, AL/eAX
                int size = opcode == 0xEE ? 1 : immZBytes();
                portOut((int) getReg(RDX, 2), size, getReg(RAX, size));
                return;
            }
            case 0xE8: {                            // CALL rel32
                int size = nearSize();
                long rel = fetch32();
                push(ip, size);
                ip += rel;
                return;
            }
            case 0xE9: {                            // JMP rel32
                nearSize();
                long rel = fetch32();
                ip += rel;
                return;
            }
            case 0xEB: {                            // JMP rel8
                nearSize();
                long rel = fetchImm8();
                ip += rel;
                return;
            }

            case 0xF4:                              // HLT: there are no interrupts to wait for
                return;
            case 0xF5:                              // CMC
                setFlags(CF, ~flags());
                return;
            case 0xF8:                              // CLC
                setFlags(CF, 0);
                return;
            case 0xF9:                              // STC
                setFlags(CF, CF);
                return;
            case 0xFA:                              // CLI
                setFlags(IF, 0);
                return;
            case 0xFB:                              // STI
                setFlags(IF, IF);
                return;
            case 0xFC:                              // CLD
                setFlags(DF, 0);
                return;
            case 0xFD:                              // STD
                setFlags(DF, DF);
                return;

            case 0xF6:                              // group 3: r/m8
            case 0xF7: {                            // group 3: r/m
                int size = opcode == 0xF6 ? 1 : opSize;
                int immBytes = peekGroup() >= 2 ? 0 : size == 1 ? 1 : immZBytes();
                decodeModRm(immBytes);
                group3(size);
                return;
            }
            case 0xFE: {                            // group 4: INC, DEC r/m8
                if (peekGroup() > 1) break;
                decodeModRm(0);
                writeRm(1, incDec(readRm(1), 1, group() == 1));
                return;
            }
            case 0xFF: {                            // group 5
                int sub = peekGroup();
                boolean far = sub == 3 || sub == 5;
                if (sub == 7 || far && (core.readMem8(ip) >>> 6) == 3) break;
                decodeModRm(0);
                if (far) {                          // far CALL, far JMP through m16:32 / m16:64
                    int size = farSize();
                    long target = readMem(ea, size);
                    int selector = (int) readMem(linear(dataSeg(SEG_DS), offset + size), 2);
                    if (sub == 3) {
                        farCall(selector, target);
                    } else {
                        farJump(selector, target);
                    }
                    return;
                }
                switch (sub) {
                    case 0: case 1:                 // INC, DEC r/m
                        writeRm(opSize, incDec(readRm(opSize), opSize, sub == 1));
                        break;
                    case 2: {                       // CALL r/m64 (r/m32 in 32-bit code)
                        int size = nearSize();
                        long target = readRm(size);
                        push(ip, size);
                        ip = target;
                        break;
                    }
                    case 4:                         // JMP r/m64 (r/m32 in 32-bit code)
                        ip = readRm(nearSize());
                        break;
                    default:                        // PUSH r/m
                        push(readRm(stackSize()), stackSize());
                        break;
                }
                return;
            }
        }

        String name = String.format("opcode 0x%02X", opcode);
        if (opcode == 0x8F || opcode == 0xC6 || opcode == 0xC7 || opcode == 0xFE || opcode == 0xFF
                || opcode == 0x8C || opcode == 0x8E) {
            name += " /" + peekGroup();             // the /digit selects the instruction
        }
        throw unknownOpcode(name);
    }

    private long incDec(long v, int size, boolean dec) {
        // CF is not affected.
        return dec ? sub(v, 1, 0, size, STATUS & ~CF) : add(v, 1, 0, size, STATUS & ~CF);
    }

    /* ---------------- shifts and rotates ---------------- */

    /* ROL, ROR, RCL, RCR, SHL, SHR, SAL, SAR, selected by the /digit. */
    private long shift(int op, long val, int count, int size) {
        int bits = size * 8;
        long m = mask(size);
        long top = signBit(size);
        val &= m;
        count &= size == 8 ? 0x3F : 0x1F;
        if (count == 0) {
            return val;                             // flags are not affected
        }
        long cf = flag(CF) ? 1 : 0;
        long res = val;
        for (int i = 0; i < count; i++) {
            long msb = (res >>> (bits - 1)) & 1;
            long lsb = res & 1;
            switch (op) {
                case 0: res = ((res << 1) | msb) & m; cf = msb; break;                 // ROL
                case 1: res = (res >>> 1) | (lsb << (bits - 1)); cf = lsb; break;      // ROR
                case 2: res = ((res << 1) | cf) & m; cf = msb; break;                  // RCL
                case 3: res = (res >>> 1) | (cf << (bits - 1)); cf = lsb; break;       // RCR
                case 5: res >>>= 1; cf = lsb; break;                                   // SHR
                case 7: res = (res >>> 1) | (res & top); cf = lsb; break;              // SAR
                default: res = (res << 1) & m; cf = msb; break;                        // SHL, SAL
            }
        }
        // OF is defined for a count of 1 only; this is its value in that case.
        long msb = (res >>> (bits - 1)) & 1;
        long of;
        switch (op) {
            case 0: case 2: case 4: case 6: of = msb ^ cf; break;
            case 1: case 3: of = msb ^ ((res >>> (bits - 2)) & 1); break;
            case 5: of = (val >>> (bits - 1)) & 1; break;
            default: of = 0; break;
        }
        long f = (cf != 0 ? CF : 0) | (of != 0 ? OF : 0);
        if (op < 4) {
            setFlags(CF | OF, f);                   // rotates leave SF, ZF and PF alone
        } else {
            setFlags(STATUS & ~AF, f | szp(res, size));
        }
        return res;
    }

    /* ---------------- multiply and divide ---------------- */

    /* Signed multiply truncated to 'size' bytes; CF and OF report a lost upper half. */
    private long imul(long a, long b, int size) {
        long res;
        boolean overflow;
        if (size == 8) {
            res = a * b;
            overflow = Math.multiplyHigh(a, b) != (res >> 63);
        } else {
            long full = sx(a, size) * sx(b, size);
            res = full & mask(size);
            overflow = sx(res, size) != full;
        }
        setFlags(CF | OF, overflow ? CF | OF : 0);
        return res;
    }

    private static long unsignedMultiplyHigh(long a, long b) {
        return Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a);
    }

    private static BigInteger unsigned(long v) {
        return new BigInteger(Long.toUnsignedString(v));
    }

    /* Group 3: TEST, NOT, NEG, MUL, IMUL, DIV, IDIV on the decoded r/m operand. */
    private void group3(int size) {
        long v = readRm(size);
        switch (group()) {
            case 0: case 1:                         // TEST r/m, imm
                logic(v & (size == 1 ? fetchImm8() : fetchImmZ()), size);
                return;
            case 2:                                 // NOT: no flags
                writeRm(size, ~v);
                return;
            case 3:                                 // NEG
                writeRm(size, sub(0, v, 0, size, STATUS));
                return;
            case 4: case 5:
                multiply(v, size, group() == 5);
                return;
            default:
                divide(v, size, group() == 7);
                return;
        }
    }

    /* MUL/IMUL: AX = AL * v, or rDX:rAX = rAX * v. */
    private void multiply(long v, int size, boolean signed) {
        long a = getReg(RAX, size);
        long lo, hi;
        boolean overflow;
        if (size == 8) {
            lo = a * v;
            hi = signed ? Math.multiplyHigh(a, v) : unsignedMultiplyHigh(a, v);
            overflow = signed ? hi != (lo >> 63) : hi != 0;
        } else {
            long full = signed ? sx(a, size) * sx(v, size) : a * v;
            lo = full & mask(size);
            hi = (full >> (size * 8)) & mask(size);
            overflow = signed ? sx(lo, size) != full : hi != 0;
        }
        if (size == 1) {
            setReg(RAX, 2, (hi << 8) | lo);
        } else {
            setReg(RAX, size, lo);
            setReg(RDX, size, hi);
        }
        setFlags(CF | OF, overflow ? CF | OF : 0);
    }

    /* DIV/IDIV: divide AX by v, or rDX:rAX by v. */
    private void divide(long v, int size, boolean signed) {
        int bits = size * 8;
        long hi = size == 1 ? getReg(RAX, 2) >>> 8 : getReg(RDX, size);
        long lo = getReg(RAX, size);

        BigInteger dividend = (signed ? BigInteger.valueOf(sx(hi, size)) : unsigned(hi))
                .shiftLeft(bits).add(unsigned(lo));
        BigInteger divisor = signed ? BigInteger.valueOf(sx(v, size)) : unsigned(v);
        if (divisor.signum() == 0) {
            throw divideError();
        }
        BigInteger[] qr = dividend.divideAndRemainder(divisor);
        BigInteger limit = BigInteger.ONE.shiftLeft(signed ? bits - 1 : bits);
        BigInteger min = signed ? limit.negate() : BigInteger.ZERO;
        if (qr[0].compareTo(min) < 0 || qr[0].compareTo(limit) >= 0) {
            throw divideError();
        }
        long q = qr[0].longValue();
        long r = qr[1].longValue();
        if (size == 1) {
            setReg(RAX, 2, ((r & 0xFF) << 8) | (q & 0xFF));
        } else {
            setReg(RAX, size, q);
            setReg(RDX, size, r);
        }
    }

    private ArithmeticException divideError() {
        return new ArithmeticException(String.format(
                "x86-64 divide error at RIP=0x%016X (division by zero or quotient too large)", insnStart));
    }

    /* ---------------- string instructions and ports ---------------- */

    private long portIn(int port, int size) {
        long v = 0;
        for (int i = 0; i < size; i++) {
            v |= (core.readPort(port + i) & 0xFFL) << (i * 8);
        }
        return v;
    }

    private void portOut(int port, int size, long v) {
        for (int i = 0; i < size; i++) {
            core.writePort(port + i, (byte) (v >>> (i * 8)));
        }
    }

    /* MOVS, CMPS, STOS, LODS, SCAS, INS and OUTS, with an optional repeat prefix. */
    private void stringOp(int opcode) {
        int size = (opcode & 1) == 0 ? 1 : opSize;
        if (opcode < 0x70 && size == 8) size = 4;   // INS and OUTS have no 64-bit form
        int addrSize = addrSize();
        // The source is DS:rSI (or an override), the destination always ES:rDI.
        int srcSeg = dataSeg(SEG_DS);
        long delta = flag(DF) ? -size : size;
        boolean compares = (opcode & 0xFE) == 0xA6 || (opcode & 0xFE) == 0xAE;

        if (rep != 0 && getReg(RCX, addrSize) == 0) {
            return;
        }
        for (;;) {
            long si = getReg(RSI, addrSize);
            long di = getReg(RDI, addrSize);
            long src = linear(srcSeg, si);
            long dst = linear(SEG_ES, di);
            boolean usesSi = false, usesDi = false;
            switch (opcode & 0xFE) {
                case 0xA4:                          // MOVS
                    writeMem(dst, size, readMem(src, size));
                    usesSi = usesDi = true;
                    break;
                case 0xA6:                          // CMPS
                    sub(readMem(src, size), readMem(dst, size), 0, size, STATUS);
                    usesSi = usesDi = true;
                    break;
                case 0xAA:                          // STOS
                    writeMem(dst, size, getReg(RAX, size));
                    usesDi = true;
                    break;
                case 0xAC:                          // LODS
                    setReg(RAX, size, readMem(src, size));
                    usesSi = true;
                    break;
                case 0xAE:                          // SCAS
                    sub(getReg(RAX, size), readMem(dst, size), 0, size, STATUS);
                    usesDi = true;
                    break;
                case 0x6C:                          // INS
                    writeMem(dst, size, portIn((int) getReg(RDX, 2), size));
                    usesDi = true;
                    break;
                default:                            // OUTS
                    portOut((int) getReg(RDX, 2), size, readMem(src, size));
                    usesSi = true;
                    break;
            }
            if (usesSi) setReg(RSI, addrSize, si + delta);
            if (usesDi) setReg(RDI, addrSize, di + delta);

            if (rep == 0) {
                return;
            }
            long count = (getReg(RCX, addrSize) - 1) & mask(addrSize);
            setReg(RCX, addrSize, count);
            if (count == 0) {
                return;
            }
            // REPE stops when the operands differ, REPNE when they are equal.
            if (compares && flag(ZF) != (rep == 0xF3)) {
                return;
            }
        }
    }

    /* ---------------- two-byte opcodes (0F xx) ---------------- */

    private void executeTwoByte(int op2) throws Exception {
        // CMOVcc r, r/m: 0F 40..4F
        if ((op2 & 0xF0) == 0x40) {
            decodeModRm(0);
            long v = readRm(opSize);
            // A 32-bit CMOV clears the upper half even when it does not move.
            setReg(reg, opSize, condition(op2 & 0xF) ? v : getReg(reg, opSize));
            return;
        }
        // Jcc rel32: 0F 80..8F
        if ((op2 & 0xF0) == 0x80) {
            nearSize();
            long rel = fetch32();
            if (condition(op2 & 0xF)) ip += rel;
            return;
        }
        // SETcc r/m8: 0F 90..9F
        if ((op2 & 0xF0) == 0x90) {
            decodeModRm(0);
            writeRm(1, condition(op2 & 0xF) ? 1 : 0);
            return;
        }
        // BSWAP r: 0F C8..CF
        if ((op2 & 0xF8) == 0xC8) {
            int r = (op2 & 7) | (rexB << 3);
            if (opSize == 8) {
                setReg(r, 8, Long.reverseBytes(reg64(r)));
            } else {
                setReg(r, 4, Integer.reverseBytes((int) reg64(r)));
            }
            return;
        }
        // Multi-byte NOP and prefetch hints: 0F 18..1F
        if ((op2 & 0xF8) == 0x18) {
            decodeModRm(0);
            return;
        }

        switch (op2) {
            case 0x01: {                            // group 7: SGDT, SIDT, LGDT, LIDT, INVLPG
                int sub = peekGroup();
                if ((core.readMem8(ip) >>> 6) == 3 || sub == 4 || sub == 5 || sub == 6) break;
                decodeModRm(0);
                descriptorTable(sub);
                return;
            }
            case 0x20:                              // MOV r, CRn
            case 0x22: {                            // MOV CRn, r
                // The r/m operand is always a register, whatever the mod field says.
                int modrm = fetch8();
                int cr = ((modrm >>> 3) & 7) | (rexR << 3);
                int r = (modrm & 7) | (rexB << 3);
                int size = code64 ? 8 : 4;
                if (op2 == 0x20) {
                    setReg(r, size, readCr(cr));
                } else {
                    writeCr(cr, getReg(r, size));
                }
                return;
            }
            case 0x21:                              // MOV r, DRn
            case 0x23: {                            // MOV DRn, r
                int modrm = fetch8();
                int dr = ((modrm >>> 3) & 7) | (rexR << 3);
                int r = (modrm & 7) | (rexB << 3);
                if (dr > 7) {
                    throw unknownOpcode("MOV with DR" + dr);
                }
                if (dr == 4 || dr == 5) {
                    dr += 2;                        // DR6 and DR7, as with CR4.DE clear
                }
                int size = code64 ? 8 : 4;
                if (op2 == 0x21) {
                    setReg(r, size, core.getDebugReg(dr));
                } else {
                    core.setDebugReg(dr, getReg(r, size));
                }
                return;
            }
            case 0x30:                              // WRMSR
                writeMsr((int) getReg(RCX, 4), (getReg(RDX, 4) << 32) | getReg(RAX, 4));
                return;
            case 0x32: {                            // RDMSR
                long v = readMsr((int) getReg(RCX, 4));
                setReg(RAX, 4, v & 0xFFFFFFFFL);
                setReg(RDX, 4, v >>> 32);
                return;
            }
            case 0xA0: case 0xA8:                   // PUSH FS, PUSH GS
                push(core.getSegSelector(op2 == 0xA0 ? SEG_FS : SEG_GS), stackSize());
                return;
            case 0xA1: case 0xA9:                   // POP FS, POP GS
                if (code64) break;                  // the FS/GS base would come from the descriptor
                loadSegment(op2 == 0xA1 ? SEG_FS : SEG_GS, (int) pop(stackSize()) & 0xFFFF);
                return;
            case 0x05:                              // SYSCALL: there is no kernel entry to call
            case 0x07:                              // SYSRET
                return;
            case 0xA2:                              // CPUID
                cpuid((int) getReg(RAX, 4), (int) getReg(RCX, 4));
                return;

            case 0x31: {                            // RDTSC
                long tsc = core.getTimestampCounter();
                setReg(RAX, 4, tsc & 0xFFFFFFFFL);
                setReg(RDX, 4, tsc >>> 32);
                return;
            }

            case 0xA3: case 0xAB: case 0xB3: case 0xBB: {   // BT, BTS, BTR, BTC r/m, r
                decodeModRm(0);
                bitTest((op2 >>> 3) & 3, sx(getReg(reg, opSize), opSize), false);
                return;
            }
            case 0xBA: {                            // BT, BTS, BTR, BTC r/m, imm8
                if (peekGroup() < 4) break;
                decodeModRm(1);
                bitTest(group() & 3, fetch8(), true);
                return;
            }
            case 0xAF: {                            // IMUL r, r/m
                decodeModRm(0);
                setReg(reg, opSize, imul(getReg(reg, opSize), readRm(opSize), opSize));
                return;
            }
            case 0xB0:                              // CMPXCHG r/m8, r8
            case 0xB1: {                            // CMPXCHG r/m, r
                int size = op2 == 0xB0 ? 1 : opSize;
                decodeModRm(0);
                long dest = readRm(size);
                long acc = getReg(RAX, size);
                sub(acc, dest, 0, size, STATUS);
                if (acc == dest) {
                    writeRm(size, getReg(reg, size));
                } else {
                    setReg(RAX, size, dest);        // the destination is not written
                }
                return;
            }
            case 0xB6: case 0xB7:                   // MOVZX r, r/m8 / r/m16
            case 0xBE: case 0xBF: {                 // MOVSX r, r/m8 / r/m16
                int srcSize = (op2 & 1) == 0 ? 1 : 2;
                decodeModRm(0);
                long v = readRm(srcSize);
                setReg(reg, opSize, op2 >= 0xBE ? sx(v, srcSize) : v);
                return;
            }
            case 0xBC:                              // BSF r, r/m
            case 0xBD: {                            // BSR r, r/m
                if (rep == 0xF3) break;             // TZCNT, LZCNT
                decodeModRm(0);
                long v = readRm(opSize);
                if (v == 0) {
                    setFlags(ZF, ZF);               // the destination is left unchanged
                } else {
                    setFlags(ZF, 0);
                    setReg(reg, opSize, op2 == 0xBC ? Long.numberOfTrailingZeros(v)
                                                    : 63 - Long.numberOfLeadingZeros(v));
                }
                return;
            }
            case 0xC0:                              // XADD r/m8, r8
            case 0xC1: {                            // XADD r/m, r
                int size = op2 == 0xC0 ? 1 : opSize;
                decodeModRm(0);
                long dest = readRm(size);
                long sum = add(dest, getReg(reg, size), 0, size, STATUS);
                setReg(reg, size, dest);
                writeRm(size, sum);
                return;
            }
        }

        String name = String.format("opcode 0x0F 0x%02X", op2);
        throw unknownOpcode(op2 == 0xBA || op2 == 0x01 ? name + " /" + peekGroup() : name);
    }

    /* ---------------- system instructions ---------------- */

    /*
     * The CPU always runs at privilege level 0.  Segment limits are not
     * checked, and paging is not translated: long mode relies on the
     * identity mapping UEFI firmware sets up, and turning paging on outside
     * long mode stops with an error.  So do LDT selectors, other privilege
     * levels and MSRs other than EFER.
     */

    /* SGDT (0), SIDT (1), LGDT (2), LIDT (3), INVLPG (7) on the decoded memory operand. */
    private void descriptorTable(int sub) throws X86UnknownOpcodeException {
        int baseSize = code64 ? 8 : 4;
        long baseAddr = linear(dataSeg(SEG_DS), offset + 2);
        switch (sub) {
            case 0:
            case 1: {
                boolean gdt = sub == 0;
                writeMem(ea, 2, gdt ? core.getGdtLimit() : core.getIdtLimit());
                writeMem(baseAddr, baseSize, gdt ? core.getGdtBase64() : core.getIdtBase());
                return;
            }
            case 2:
            case 3: {
                int limit = (int) readMem(ea, 2);
                long base = readMem(baseAddr, baseSize);
                if (!code64 && opSize == 2) {
                    base &= 0xFFFFFF;               // a 16-bit operand loads a 24-bit base
                }
                if (sub == 2) {
                    core.setGdt(base, limit);
                } else {
                    core.setIdt(base, limit);
                }
                return;
            }
            default:                                // INVLPG: there is no TLB
                return;
        }
    }

    private long readCr(int cr) throws X86UnknownOpcodeException {
        switch (cr) {
            case 0: return core.getCr0();
            case 2: return core.getCr2();
            case 3: return core.getCr3();
            case 4: return core.getCr4();
            default: throw unknownOpcode("MOV from CR" + cr);
        }
    }

    /*
     * Writing CR0 and CR4 follows the rules for entering and leaving long
     * mode: setting CR0.PG with EFER.LME (and CR4.PAE) activates it, clearing
     * CR0.PG deactivates it.
     */
    private void writeCr(int cr, long v) throws X86UnknownOpcodeException {
        long efer = core.getEfer();
        switch (cr) {
            case 0: {
                v = (v | 0x10) & 0xFFFFFFFFL;       // ET is always 1
                boolean paging = (v & X86Core.CR0_PG) != 0;
                boolean wasPaging = (core.getCr0() & X86Core.CR0_PG) != 0;
                if ((v & X86Core.CR0_PE) == 0) {
                    throw unknownOpcode("MOV CR0 that leaves protected mode");
                }
                if (paging && !wasPaging) {
                    if ((efer & X86Core.EFER_LME) == 0) {
                        throw unknownOpcode("MOV CR0 that turns on paging outside long mode");
                    }
                    if ((core.getCr4() & X86Core.CR4_PAE) == 0) {
                        throw unknownOpcode("MOV CR0 that enters long mode without CR4.PAE");
                    }
                    efer |= X86Core.EFER_LMA;
                } else if (!paging && wasPaging) {
                    if (code64) {
                        throw unknownOpcode("MOV CR0 that turns off paging in 64-bit code");
                    }
                    efer &= ~X86Core.EFER_LMA;
                }
                core.setCr0(v);
                core.setEfer(efer);
                return;
            }
            case 2:
                core.setCr2(v);
                return;
            case 3:
                core.setCr3(v);
                return;
            case 4:
                if ((efer & X86Core.EFER_LMA) != 0 && (v & X86Core.CR4_PAE) == 0) {
                    throw unknownOpcode("MOV CR4 that clears PAE in long mode");
                }
                core.setCr4(v);
                return;
            default:
                throw unknownOpcode("MOV to CR" + cr);
        }
    }

    private long readMsr(int msr) throws X86UnknownOpcodeException {
        if (msr == X86Core.MSR_EFER) {
            return core.getEfer();
        }
        throw unknownOpcode(String.format("RDMSR 0x%X", msr));
    }

    private void writeMsr(int msr, long v) throws X86UnknownOpcodeException {
        if (msr != X86Core.MSR_EFER) {
            throw unknownOpcode(String.format("WRMSR 0x%X", msr));
        }
        long old = core.getEfer();
        if ((core.getCr0() & X86Core.CR0_PG) != 0 && ((old ^ v) & X86Core.EFER_LME) != 0) {
            throw unknownOpcode("WRMSR that changes EFER.LME while paging is on");
        }
        // LMA cannot be written; it follows CR0.PG.
        core.setEfer((v & ~X86Core.EFER_LMA) | (old & X86Core.EFER_LMA));
    }

    /* The 8-byte GDT descriptor of a selector. */
    private long descriptor(int selector, String what) throws X86UnknownOpcodeException {
        if ((selector & 4) != 0) {
            throw unknownOpcode(String.format("%s: selector 0x%X is in the LDT", what, selector));
        }
        int index = selector & ~7;
        if (index + 7 > core.getGdtLimit()) {
            throw unknownOpcode(String.format("%s: selector 0x%X is outside the GDT", what, selector));
        }
        long d = core.readMem64(core.getGdtBase64() + index);
        if ((d & (1L << 47)) == 0) {
            throw unknownOpcode(String.format("%s: segment 0x%X is not present", what, selector));
        }
        if ((d & (1L << 44)) == 0) {
            throw unknownOpcode(String.format("%s: selector 0x%X is a system segment", what, selector));
        }
        return d;
    }

    private static int descriptorBase(long d) {
        return (int) (((d >>> 16) & 0xFFFFFF) | ((d >>> 56) << 24));
    }

    /* Load a data segment register (not CS) from the GDT. */
    private void loadSegment(int seg, int selector) throws X86UnknownOpcodeException {
        String what = "load " + "ECSDFG".charAt(seg) + "S";
        int base = 0;
        if ((selector & ~3) == 0) {
            // The null selector; only SS outside 64-bit code needs a real segment.
            if (seg == SEG_SS && !code64) {
                throw unknownOpcode(what + ": null selector");
            }
        } else {
            if ((selector & 3) != 0) {
                throw unknownOpcode(String.format("%s: selector 0x%X has privilege level %d",
                        what, selector, selector & 3));
            }
            long d = descriptor(selector, what);
            boolean code = (d & (1L << 43)) != 0;
            boolean writableOrReadable = (d & (1L << 41)) != 0;
            if (seg == SEG_SS ? code || !writableOrReadable : code && !writableOrReadable) {
                throw unknownOpcode(String.format("%s: segment 0x%X cannot be used here", what, selector));
            }
            // The D/B bit of an SS descriptor selects a 16- or 32-bit stack
            // pointer on real hardware, but Intel's MOV-to-SS checks do not
            // require it to be set in 32-bit code.  GRUB's relocator loads a
            // 16-bit data segment into SS just before leaving long mode.
            base = descriptorBase(d);
        }
        core.setSegSelector(seg, selector);
        core.setSegBase(seg, base);
    }

    /* Size of a far pointer's offset: 4, or 8 with REX.W.  16-bit offsets are not implemented. */
    private int farSize() throws X86UnknownOpcodeException {
        if (opSize == 2) {
            throw unknownOpcode("far branch with a 16-bit offset");
        }
        return opSize;
    }

    /* The offset of an immediate far pointer (ptr16:32). */
    private long farOffsetImm() throws X86UnknownOpcodeException {
        farSize();
        return fetch32() & 0xFFFFFFFFL;
    }

    private void farCall(int selector, long target) throws X86UnknownOpcodeException {
        int size = farSize();
        long returnTo = ip;
        int cs = core.getSegSelector(SEG_CS);
        // The target is checked before anything is pushed.
        long d = codeDescriptor(selector);
        push(cs, size);
        push(returnTo, size);
        enterCode(selector, d, target);
    }

    private void farJump(int selector, long target) throws X86UnknownOpcodeException {
        enterCode(selector, codeDescriptor(selector), target);
    }

    private long codeDescriptor(int selector) throws X86UnknownOpcodeException {
        String what = "far branch";
        if ((selector & 3) != 0) {
            throw unknownOpcode(String.format("%s: selector 0x%X has privilege level %d",
                    what, selector, selector & 3));
        }
        if ((selector & ~3) == 0) {
            throw unknownOpcode(what + ": null selector");
        }
        long d = descriptor(selector, what);
        if ((d & (1L << 43)) == 0) {
            throw unknownOpcode(String.format("%s: segment 0x%X is not a code segment", what, selector));
        }
        if (descriptorBase(d) != 0) {
            throw unknownOpcode(String.format("%s: code segment 0x%X has a base", what, selector));
        }
        return d;
    }

    /*
     * Continue at target in the code segment.  Its L and D bits decide
     * whether the next instruction is 64-bit, 32-bit or (not supported)
     * 16-bit code.
     */
    private void enterCode(int selector, long d, long target) {
        boolean longSegment = core.isLongMode() && (d & (1L << 53)) != 0;
        core.setSegSelector(SEG_CS, selector);
        core.setSegBase(SEG_CS, 0);
        core.setCodeLong(longSegment);
        core.setCodeSize32(!longSegment && (d & (1L << 54)) != 0);
        ip = longSegment ? target : target & 0xFFFFFFFFL;
        code64 = longSegment;
    }

    /**
     * Deliver vector 1 (#DB) through the 32-bit interrupt gate in the IDT.
     * Instruction breakpoints are faults, so the return address is the
     * faulting instruction; data breakpoints are traps, so it is the next
     * instruction.  RF is set in the saved flags to keep IRETD from reporting
     * the same instruction breakpoint again.
     */
    private void deliverDebugException(int bp, long returnIp) throws Exception {
        if (!core.isProtectedMode() || code64) {
            throw unknownOpcode("debug exception outside 32-bit protected mode is not emulated");
        }
        long idtBase = core.getIdtBase();
        if (idtBase == 0 || 1 * 8 + 7 > core.getIdtLimit()) {
            throw unknownOpcode("debug exception: IDT does not cover vector 1");
        }
        long gate = core.readMem64(idtBase + 1 * 8L);
        if ((gate & (1L << 47)) == 0) {
            throw unknownOpcode("debug exception: gate for vector 1 is not present");
        }
        int type = (int) ((gate >>> 40) & 0xFF);
        if ((type & 0x1F) != 0x0E) {
            throw unknownOpcode("debug exception: gate for vector 1 is not a 32-bit interrupt gate");
        }
        int selector = (int) ((gate >>> 16) & 0xFFFF);
        long target = (gate & 0xFFFFL) | (((gate >>> 48) & 0xFFFFL) << 16);
        long d = codeDescriptor(selector);

        long savedIp = returnIp & 0xFFFFFFFFL;
        long savedCs = core.getSegSelector(SEG_CS) & 0xFFFFL;
        long savedFlags = flags() | RF;
        long sp = (reg64(RSP) - 12) & 0xFFFFFFFFL;
        core.writeMem32(linear(SEG_SS, sp), (int) savedIp);
        core.writeMem32(linear(SEG_SS, sp + 4), (int) savedCs);
        core.writeMem32(linear(SEG_SS, sp + 8), (int) savedFlags);
        setReg(RSP, 4, sp);

        core.setDebugReg(6, (core.getDebugReg(6) & ~0xFL) | (1L << bp));
        core.setFlag(FLAG_IF, false);               // an interrupt gate clears IF
        enterCode(selector, d, target);
    }

    /**
     * BT (0), BTS (1), BTR (2) and BTC (3) on the decoded r/m operand.  With a
     * register bit offset and a memory operand, the offset can reach outside
     * the addressed word.
     */
    private void bitTest(int op, long offset, boolean immediate) {
        int bits = opSize * 8;
        if (mod != 3 && !immediate) {
            ea += Math.floorDiv(offset, (long) bits) * opSize;
        }
        long bit = 1L << Math.floorMod(offset, (long) bits);
        long v = readRm(opSize);
        setFlags(CF, (v & bit) != 0 ? CF : 0);
        switch (op) {
            case 1: writeRm(opSize, v | bit); break;
            case 2: writeRm(opSize, v & ~bit); break;
            case 3: writeRm(opSize, v ^ bit); break;
            default: break;
        }
    }
}
