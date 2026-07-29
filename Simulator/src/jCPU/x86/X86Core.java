package jCPU.x86;

import jCPU.iCPU;
import jCPU.CallListener;
import jCPU.Peripheral;

import java.util.HashMap;
import java.util.Map;

public class X86Core implements iCPU {
    public static final int REG_EAX = 0;
    public static final int REG_ECX = 1;
    public static final int REG_EDX = 2;
    public static final int REG_EBX = 3;
    public static final int REG_ESP = 4;
    public static final int REG_EBP = 5;
    public static final int REG_ESI = 6;
    public static final int REG_EDI = 7;
    public static final int REG_VIRTUAL_BASE = 8;
    public static final int REG_COUNT = 16;

    public static final int FLAG_CF = 0;
    public static final int FLAG_PF = 2;
    public static final int FLAG_AF = 4;
    public static final int FLAG_ZF = 6;
    public static final int FLAG_SF = 7;
    public static final int FLAG_DF = 10;
    public static final int FLAG_OF = 11;

    private final int[] regs = new int[REG_COUNT];
    private int flags = 0;
    private final int memSize;
    private final Map<Integer, Byte> memory = new HashMap<>();
    private final Map<Integer, Peripheral> ioPorts = new HashMap<>();
    private iExecutor executor;
    private int pc = 0;

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
        for (int i = 0; i < REG_COUNT; i++) {
            regs[i] = 0;
        }
        flags = 0;
        pc = 0;
        regs[REG_ESP] = memSize;
    }

    public void setReg(int reg, int value) {
        if (reg >= 0 && reg < REG_COUNT) {
            regs[reg] = value;
        }
    }

    public int getReg(int reg) {
        if (reg >= 0 && reg < REG_COUNT) {
            return regs[reg];
        }
        return 0;
    }

    public void setFlag(int flag, boolean value) {
        if (value) {
            flags |= (1 << flag);
        } else {
            flags &= ~(1 << flag);
        }
    }

    public boolean getFlag(int flag) {
        return (flags & (1 << flag)) != 0;
    }

    public void push32(int value) {
        regs[REG_ESP] -= 4;
        writeMem32(regs[REG_ESP], value);
    }

    public int pop32() {
        int value = readMem32(regs[REG_ESP]);
        regs[REG_ESP] += 4;
        return value;
    }

    public int readMem8(int addr) {
        Byte b = memory.get(addr);
        return b != null ? b & 0xFF : 0;
    }

    public void writeMem8(int addr, int value) {
        memory.put(addr, (byte) value);
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

    public void registerPort(int port, Peripheral peripheral) {
        ioPorts.put(port, peripheral);
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
        for (int i = 0; i < limit; i++) {
            if (step() == 0) break;
        }
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

    @Override
    public int code(int i) {
        return readMem8(pc + i);
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
        return regs[REG_EAX];
    }

    @Override
    public int r(int i) {
        return getReg(i);
    }

    @Override
    public int code16(int i) {
        return readMem16(pc + i);
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
        this.pc = address;
    }

    @Override
    public void acc(int i) {
        regs[REG_EAX] = i;
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
        return regs[REG_EBX];
    }

    @Override
    public void b(int i) {
        regs[REG_EBX] = i;
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

    public int getPC() {
        return pc;
    }

    public void setPC(int pc) {
        this.pc = pc;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getMemSize() {
        return memSize;
    }

    public Map<Integer, Byte> getMemory() {
        return memory;
    }
}