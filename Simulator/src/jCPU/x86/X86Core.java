package jCPU.x86;

import jCPU.iCPU;
import jCPU.CallListener;
import jCPU.Peripheral;

import java.util.HashMap;
import java.util.Map;

public class X86Core implements iCPU
{
    /* ---------------- 32-bit register indices (legacy) ---------------- */
    public static final int REG_EAX = 0;
    public static final int REG_ECX = 1;
    public static final int REG_EDX = 2;
    public static final int REG_EBX = 3;
    public static final int REG_ESP = 4;
    public static final int REG_EBP = 5;
    public static final int REG_ESI = 6;
    public static final int REG_EDI = 7;

    /* ---------------- 64-bit register indices (long mode) ---------------- */
    public static final int REG_RAX = 8;
    public static final int REG_RCX = 9;
    public static final int REG_RDX = 10;
    public static final int REG_RBX = 11;
    public static final int REG_RSP = 12;
    public static final int REG_RBP = 13;
    public static final int REG_RSI = 14;
    public static final int REG_RDI = 15;
    public static final int REG_R8  = 16;
    public static final int REG_R9  = 17;
    public static final int REG_R10 = 18;
    public static final int REG_R11 = 19;
    public static final int REG_R12 = 20;
    public static final int REG_R13 = 21;
    public static final int REG_R14 = 22;
    public static final int REG_R15 = 23;

    public static final int REG_COUNT_32 = 8;
    public static final int REG_COUNT_64 = 24;

    /* Virtual register base (used by NASM assembler/evaluator). */
    public static final int REG_VIRTUAL_BASE = 100;
    public static final int REG_VIRTUAL_COUNT = 32;

    public static final int FLAG_CF = 0;
    public static final int FLAG_PF = 2;
    public static final int FLAG_AF = 4;
    public static final int FLAG_ZF = 6;
    public static final int FLAG_SF = 7;
    public static final int FLAG_DF = 10;
    public static final int FLAG_OF = 11;
    public static final int FLAG_IF = 9;

    private final int[] regs32 = new int[REG_COUNT_32];
    private final long[] regs64 = new long[REG_COUNT_64];
    private final int[] regsVirtual = new int[REG_VIRTUAL_COUNT];
    private int flags32 = 0;
    private long rflags64 = 0;
    private final int memSize;
    private final byte[] memory = new byte[64 * 1024 * 1024]; // 64MB direct-mapped memory
    private final Map<Integer, Peripheral> ioPorts = new HashMap<>();
    private iExecutor executor;
    private int pc32 = 0;
    private long rip64 = 0;
    private boolean longMode = false;
    private volatile boolean stopRequested;

    public interface iExecutor {
        int step(X86Core core) throws Exception;
        String getDecodeAt(X86Core core, int pc);
    }

    public X86Core(int memSize) {
        this.memSize = memSize;
        reset();
    }

    @Override
    public void reset() {
        for (int i = 0; i < REG_COUNT_32; i++) {
            regs32[i] = 0;
        }
        for (int i = 0; i < REG_COUNT_64; i++) {
            regs64[i] = 0;
        }
        for (int i = 0; i < REG_VIRTUAL_COUNT; i++) {
            regsVirtual[i] = 0;
        }
        flags32 = 0;
        rflags64 = 0;
        pc32 = 0;
        rip64 = 0;
        longMode = false;
        regs32[REG_ESP] = memSize;
        regs64[REG_RSP] = memSize;
    }

    /* ---------------- register access ---------------- */

    public void setReg(int reg, int value) {
        if (reg >= 0 && reg < REG_COUNT_32) {
            regs32[reg] = value;
        }
        if (longMode && reg >= REG_RAX && reg < REG_COUNT_64) {
            regs64[reg] = value & 0xFFFFFFFFL;
        }
        if (reg >= REG_VIRTUAL_BASE && reg < REG_VIRTUAL_BASE + REG_VIRTUAL_COUNT) {
            regsVirtual[reg - REG_VIRTUAL_BASE] = value;
        }
    }

    public int getReg(int reg) {
        if (reg >= 0 && reg < REG_COUNT_32) {
            return regs32[reg];
        }
        if (longMode && reg >= REG_RAX && reg < REG_COUNT_64) {
            return (int) (regs64[reg] & 0xFFFFFFFFL);
        }
        if (reg >= REG_VIRTUAL_BASE && reg < REG_VIRTUAL_BASE + REG_VIRTUAL_COUNT) {
            return regsVirtual[reg - REG_VIRTUAL_BASE];
        }
        return 0;
    }

    public void setReg64(int reg, long value) {
        if (reg >= REG_RAX && reg < REG_COUNT_64) {
            regs64[reg] = value;
        } else if (reg >= 0 && reg < REG_COUNT_32) {
            regs32[reg] = (int) value;
        }
    }

    public long getReg64(int reg) {
        if (reg >= REG_RAX && reg < REG_COUNT_64) {
            return regs64[reg];
        }
        if (reg >= 0 && reg < REG_COUNT_32) {
            return regs32[reg] & 0xFFFFFFFFL;
        }
        return 0;
    }

    public void setFlag(int flag, boolean value) {
        if (value) {
            flags32 |= (1 << flag);
            rflags64 |= (1L << flag);
        } else {
            flags32 &= ~(1 << flag);
            rflags64 &= ~(1L << flag);
        }
    }

    public boolean getFlag(int flag) {
        return (flags32 & (1 << flag)) != 0;
    }

    public long getRflags64() {
        return rflags64;
    }

    public void setRflags64(long rflags) {
        this.rflags64 = rflags;
        this.flags32 = (int) rflags;
    }

    /* ---------------- stack ---------------- */

    public void push32(int value) {
        regs32[REG_ESP] -= 4;
        writeMem32(regs32[REG_ESP], value);
        regs64[REG_RSP] = regs32[REG_ESP] & 0xFFFFFFFFL;
    }

    public int pop32() {
        int value = readMem32(regs32[REG_ESP]);
        regs32[REG_ESP] += 4;
        regs64[REG_RSP] = regs32[REG_ESP] & 0xFFFFFFFFL;
        return value;
    }

    public void push64(long value) {
        regs64[REG_RSP] -= 8;
        writeMem64(regs64[REG_RSP], value);
        regs32[REG_ESP] = (int) (regs64[REG_RSP] & 0xFFFFFFFFL);
    }

    public long pop64() {
        long value = readMem64(regs64[REG_RSP]);
        regs64[REG_RSP] += 8;
        regs32[REG_ESP] = (int) (regs64[REG_RSP] & 0xFFFFFFFFL);
        return value;
    }

    /* ---------------- memory access ---------------- */

    public int readMem8(int addr) {
        if (addr < 0 || addr >= memory.length) return 0;
        return memory[addr] & 0xFF;
    }

    public void writeMem8(int addr, int value) {
        if (addr >= 0 && addr < memory.length) {
            memory[addr] = (byte) value;
        }
    }

    public int readMem16(int addr) {
        int low = readMem8(addr);
        int high = readMem8(addr + 1);
        return (high << 8) | low;
    }

    public void writeMem16(int addr, int value) {
        writeMem8(addr, value & 0xFF);
        writeMem8(addr + 1, (value >> 8) & 0xFF);
    }

    public int readMem32(int addr) {
        int b0 = readMem8(addr);
        int b1 = readMem8(addr + 1);
        int b2 = readMem8(addr + 2);
        int b3 = readMem8(addr + 3);
        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    public void writeMem32(int addr, int value) {
        writeMem8(addr, value & 0xFF);
        writeMem8(addr + 1, (value >> 8) & 0xFF);
        writeMem8(addr + 2, (value >> 16) & 0xFF);
        writeMem8(addr + 3, (value >> 24) & 0xFF);
    }

    public long readMem64(long addr) {
        long b0 = readMem8((int) addr) & 0xFFL;
        long b1 = readMem8((int) (addr + 1)) & 0xFFL;
        long b2 = readMem8((int) (addr + 2)) & 0xFFL;
        long b3 = readMem8((int) (addr + 3)) & 0xFFL;
        long b4 = readMem8((int) (addr + 4)) & 0xFFL;
        long b5 = readMem8((int) (addr + 5)) & 0xFFL;
        long b6 = readMem8((int) (addr + 6)) & 0xFFL;
        long b7 = readMem8((int) (addr + 7)) & 0xFFL;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) |
               (b4 << 32) | (b5 << 40) | (b6 << 48) | (b7 << 56);
    }

    public void writeMem64(long addr, long value) {
        writeMem8((int) addr, (int) (value & 0xFF));
        writeMem8((int) (addr + 1), (int) ((value >> 8) & 0xFF));
        writeMem8((int) (addr + 2), (int) ((value >> 16) & 0xFF));
        writeMem8((int) (addr + 3), (int) ((value >> 24) & 0xFF));
        writeMem8((int) (addr + 4), (int) ((value >> 32) & 0xFF));
        writeMem8((int) (addr + 5), (int) ((value >> 40) & 0xFF));
        writeMem8((int) (addr + 6), (int) ((value >> 48) & 0xFF));
        writeMem8((int) (addr + 7), (int) ((value >> 56) & 0xFF));
    }

    public void registerPort(int port, Peripheral peripheral) {
        ioPorts.put(port, peripheral);
    }

    public Map<Integer, Peripheral> getIoPorts() {
        return ioPorts;
    }

    public byte readPort(int port) {
        Peripheral p = ioPorts.get(port);
        if (p != null) return p.read(port);
        return 0;
    }

    public void writePort(int port, byte value) {
        Peripheral p = ioPorts.get(port);
        if (p != null) p.write(port, value);
    }

    @Override
    public int step() throws Exception {
        if (executor != null) {
            return executor.step(this);
        }
        return 0;
    }

    @Override
    public void go(int limit) throws Exception {
        stopRequested = false;
        if (limit < 0) {
            while (!stopRequested) {
                step();
            }
        } else {
            for (int i = 0; i < limit && !stopRequested; i++) {
                if (step() == 0) break;
            }
        }
    }

    @Override
    public void stopSimulation() {
        stopRequested = true;
    }

    @Override
    public String getDecodeAt(int pc) {
        if (executor != null) {
            return executor.getDecodeAt(this, pc);
        }
        return "";
    }

    public void setActiveExecutor(iExecutor executor) {
        this.executor = executor;
    }

    /* ---------------- PC / RIP ---------------- */

    @Override
    public int code(int i) {
        return readMem8(pc32 + i);
    }

    public long code64(long i) {
        return readMem8((int) (rip64 + i)) & 0xFFL;
    }

    @Override
    public String getCodeName(int i) {
        return "";
    }

    @Override
    public String getBitName(int code) {
        return "";
    }

    @Override
    public String getDirectName(int r) {
        return "";
    }

    @Override
    public int acc() {
        return regs32[REG_EAX];
    }

    @Override
    public int r(int i) {
        return getReg(i);
    }

    @Override
    public int code16(int i) {
        return readMem16(pc32 + i);
    }

    @Override
    public CallListener getCallListener(int address) {
        return null;
    }

    @Override
    public void pushw(int i) throws Exception {
        push32(i);
    }

    @Override
    public void pc(int address) {
        this.pc32 = address;
        this.rip64 = address & 0xFFFFFFFFL;
    }

    public void rip(long address) {
        this.rip64 = address;
        this.pc32 = (int) address;
    }

    @Override
    public void acc(int i) {
        regs32[REG_EAX] = i;
    }

    @Override
    public void idata(int add, int acc) {
        writeMem32(add, acc);
    }

    @Override
    public int getDirectCODE(int i) {
        return readMem8(i);
    }

    @Override
    public int getDirect(int add) {
        return readMem8(add);
    }

    @Override
    public void setDirect(int add, int i) {
        writeMem8(add, i);
    }

    @Override
    public boolean getBit(int code) {
        return getFlag(code);
    }

    @Override
    public boolean cy() {
        return getFlag(FLAG_CF);
    }

    @Override
    public void cy(boolean b) {
        setFlag(FLAG_CF, b);
    }

    @Override
    public int idata(int r) {
        return readMem32(r);
    }

    @Override
    public void setBit(int code, boolean b) {
        setFlag(code, b);
    }

    @Override
    public boolean ac() {
        return getFlag(FLAG_AF);
    }

    @Override
    public void r(int r, int tmp) {
        setReg(r, tmp);
    }

    @Override
    public int b() {
        return regs32[REG_EBX];
    }

    @Override
    public void b(int i) {
        regs32[REG_EBX] = i;
    }

    @Override
    public void ov(boolean b) {
        setFlag(FLAG_OF, b);
    }

    @Override
    public int dptr() {
        return 0;
    }

    @Override
    public void dptr(int i) {
    }

    @Override
    public boolean getBitCODE(int i) {
        return false;
    }

    @Override
    public int popw() throws Exception {
        return pop32();
    }

    @Override
    public void eoi() {
    }

    @Override
    public void xdata(int offset, int acc) {
    }

    @Override
    public int pop() throws Exception {
        return pop32();
    }

    @Override
    public int xdata(int dptr) {
        return 0;
    }

    @Override
    public void push(int directCODE) throws Exception {
        push32(directCODE);
    }

    @Override
    public void ac(boolean op) {
        setFlag(FLAG_AF, op);
    }

    @Override
    public int sfr(int add) {
        return 0;
    }

    @Override
    public int getSfrXdataHi() {
        return 0;
    }

    public int getPC32() {
        return pc32;
    }

    public int getPC() {
        return pc32;
    }

    public long getPC64() {
        return rip64;
    }

    public void setPC(int pc) {
        this.pc32 = pc;
        this.rip64 = pc & 0xFFFFFFFFL;
    }

    public void setPC64(long pc) {
        this.rip64 = pc;
        this.pc32 = (int) pc;
    }

    public boolean isLongMode() {
        return longMode;
    }

    public void setLongMode(boolean longMode) {
        this.longMode = longMode;
    }

    public int getFlags() {
        return flags32;
    }

    public void setFlags(int flags) {
        this.flags32 = flags;
        this.rflags64 = flags & 0xFFFFFFFFL;
    }

    public int getMemSize() {
        return memSize;
    }

    public byte[] getMemory() {
        return memory;
    }
}
