package jCPU.RiscV;

/*
 * RISC-V Instruction Set Simulator
 * 
 * A simple single-cycle simulator of the RV32IM instruction set architecture.
 * 
 * @author Hans Jakob Damsgaard (hansjakobdamsgaard@gmail.com)
 */

import static jCPU.RiscV.RVConstants.AUIPC_MASK;
import static jCPU.RiscV.RVConstants.BRANCH_MASK;
import static jCPU.RiscV.RVConstants.CSR_MASK;
import static jCPU.RiscV.RVConstants.FENCE_MASK;
import static jCPU.RiscV.RVConstants.JALR_MASK;
import static jCPU.RiscV.RVConstants.JAL_MASK;
import static jCPU.RiscV.RVConstants.LOAD_MASK;
import static jCPU.RiscV.RVConstants.LUI_MASK;
import static jCPU.RiscV.RVConstants.OP_IMM_32_MASK;
import static jCPU.RiscV.RVConstants.OP_IMM_MASK;
import static jCPU.RiscV.RVConstants.OP_MASK;
import static jCPU.RiscV.RVConstants.OP_MASK_32;
import static jCPU.RiscV.RVConstants.STORE_MASK;
import jCPU.iCPU;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CPU implements iCPU {
    // Insert path to binary file containing RISC-V instructions
    public final static String FILEPATH = "tests/task3/loop.bin";

    // Initial value of the program counter (default is zero)
    public final static int INITIAL_PC = 0;

    // Initial value of the stack pointer (default is 2^31 - 1)
    public final static int INITIAL_SP = Integer.MAX_VALUE;

    // Activate/deactivate debugging prints (default is true)
    public final static boolean DEBUGGING = true;

    // Safety valve: maximum steps when go() is called with no explicit limit
    // (<= 0). Prevents the simulator from running forever on code that lacks
    // an ecall exit or loops within mapped memory.
    public final static int MAX_STEPS = 10_000_000;

    // Static variables used throughout the simulator
    static int reg[] = new int[32]; // 32 registers

    // A single memory for instructions and data allowing "byte addressing"
    // using different integer key values (pc and sp counting in bytes)
    //public static Memory ram = new Memory();
    boolean offsetPC = false, breakProgram = false; // For determining next pc value
    int cc = 0; // Clock cycle counter
    private boolean running = false;
    private Decoder decoder = new Decoder();
    protected int pc;
    protected Memory code;
    Esp32c3MemoryMap memoryMap;
    TimerPeripheral timer;
    // Control and Status Registers (CSR). Key = CSR address, value = register contents.
    private final Map<Integer, Integer> csrRegisters = new HashMap<>();

    public CPU() {
        //System.out.println("RISC-V!");
        code = new Memory();
        //code = new Memory();
        /*try {
            code.readBinary(FILEPATH, INITIAL_PC); // Read instructions into memory
        } catch (IOException ex) {
            Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        reg[2] = INITIAL_SP; // Reset stack pointer
        pc = INITIAL_PC;
    }

    /**
     * Set the ESP32-C3 memory map (Task 5). Memory routing of fetch/load/store
     * through this map is deferred to Task 6.
     *
     * @param memoryMap the board memory map; must not be null
     * @throws IllegalArgumentException if {@code memoryMap} is null
     */
    public void setMemoryMap(Esp32c3MemoryMap memoryMap) {
        if (memoryMap == null) {
            throw new IllegalArgumentException("memoryMap must not be null");
        }
        this.memoryMap = memoryMap;
    }

    /**
     * Set the timer peripheral. {@link #step()} advances it once per cycle.
     * @param timer
     */
    public void setTimer(TimerPeripheral timer) {
        this.timer = timer;
    }

    /**
     * Reset the simulator: PC = RESET_VECTOR, all registers cleared, clock = 0.
     *
     * <p><b>Implementation note:</b> the register file {@code reg[]} is
     * {@code static} and therefore shared across all {@link CPU}
     * instances. {@link #reset()} clears it for every instance, not just
     * this one. Per-instance register state is a known future improvement.
     */
    @Override
    public void reset() {
        pc = Esp32c3MemoryMap.RESET_VECTOR;
        for (int i = 0; i < reg.length; i++) {
            reg[i] = 0;
        }
        cc = 0;
        running = true;
    }

    /**
     * Get the current cycle count (clock).
     * @return 
     */
    public long getClock() {
        return cc;
    }

    /**
     * Get the current program counter.
     * @return 
     */
    @Override
    public int pc() {
        return pc;
    }

    /**
     * Get the current cycle count (clock). Overrides iCPU.clock() which
     * defaults to 0; here it returns the real cycle counter `cc`.
     * @return 
     */
    @Override
    public long clock() {
        return cc;
    }

    /**
     * Read a code byte at {@code addr} for the GUI code/memory panel.
     * Returns 0 for unmapped addresses.
     * @param addr code memory address
     * @return the byte at {@code addr}, or 0 if unmapped
     */
    @Override
    public int code(int addr) {
        try {
            return code.read(addr);
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * Write a code byte at {@code addr} for the GUI code/memory panel.
     * @param addr code memory address
     * @param value byte value to store
     */
    @Override
    public void code(int addr, int value) {
        code.write(addr, value);
    }

    /**
     * Get the size of the code memory for the GUI code/memory panel.
     * @return number of code bytes currently mapped
     */
    @Override
    public int getCodeSize() {
        return code.getSize();
    }

    /**
     * Get the number of Control and Status Registers (CSRs) currently defined.
     * @return number of CSRs
     */
    @Override
    public int csrSize() {
        return csrRegisters.size();
    }

    /**
     * Get the name of the CSR at the given index.
     * @param index zero-based index into the sorted CSR list
     * @return CSR name like "csr 0x300" (mtvec)
     */
    @Override
    public String getCsrName(int index) {
        List<Integer> keys = new ArrayList<>(csrRegisters.keySet());
        java.util.Collections.sort(keys);
        if (index >= 0 && index < keys.size()) {
            int csrAddr = keys.get(index);
            return "csr 0x" + Integer.toHexString(csrAddr) + " (" + csrName(csrAddr) + ")";
        }
        return "";
    }

    /**
     * Get the value of the CSR at the given index.
     * @param index zero-based index into the sorted CSR list
     * @return CSR value
     */
    @Override
    public int csr(int index) {
        List<Integer> keys = new ArrayList<>(csrRegisters.keySet());
        java.util.Collections.sort(keys);
        if (index >= 0 && index < keys.size()) {
            return csrRegisters.get(keys.get(index));
        }
        return 0;
    }

    /**
     * Return a mnemonic name for a standard RISC-V CSR address, or the address
     * in hex if not recognized.
     */
    private String csrName(int addr) {
        switch (addr) {
            case 0x000: return "ustatus";
            case 0x001: return "uisa";
            case 0x002: return "utvec";
            case 0x003: return "uscratch";
            case 0x004: return "uepc";
            case 0x005: return "ucause";
            case 0x006: return "utval";
            case 0x007: return "uip";
            case 0x040: return "uie";
            case 0x100: return "sstatus";
            case 0x102: return "stvec";
            case 0x104: return "sscratch";
            case 0x105: return "sepc";
            case 0x106: return "scause";
            case 0x107: return "stval";
            case 0x108: return "sip";
            case 0x144: return "sie";
            case 0x300: return "mstatus";
            case 0x301: return "mtvec";
            case 0x302: return "mtdeleg";
            case 0x303: return "mideleg";
            case 0x304: return "mie";
            case 0x305: return "mtimer";
            case 0x306: return "mtimecmp";
            case 0x340: return "mscratch";
            case 0x341: return "mepc";
            case 0x342: return "mcause";
            case 0x343: return "mtval";
            case 0x344: return "mip";
            case 0x345: return "mtinst";
            case 0x346: return "mtval2";
            case 0x7A0: return "marchid";
            case 0x7A1: return "mimpid";
            case 0x7A2: return "mhartid";
            case 0xF11: return "mvendorid";
            case 0x30A: return "menvcfg";
            case 0x30B: return "mcountinhibit";
            case 0x320: return "mcountinhibit";
            case 0x321: return "mhpmevent3";
            case 0x322: return "mhpmevent4";
            case 0x780: return "mcycle";
            case 0x781: return "minstret";
            case 0x782: return "mhpmcounter3";
            case 0x783: return "mhpmcounter4";
            case 0xB00: return "mcycleh";
            case 0xB01: return "minstreth";
            case 0xB02: return "mhpmcounter3h";
            case 0xB03: return "mhpmcounter4h";
            case 0xC00: return "mstatus";
            case 0xC01: return "mtvec";
            case 0xC02: return "mtdeleg";
            case 0xC03: return "mideleg";
            case 0xC04: return "mie";
            case 0xC05: return "mtime";
            case 0xC06: return "mtimecmp";
            case 0xC40: return "mscratch";
            case 0xC41: return "mepc";
            case 0xC42: return "mcause";
            case 0xC43: return "mtval";
            case 0xC44: return "mip";
            case 0xC45: return "mtinst";
            case 0xC46: return "mtval2";
            case 0xFC0: return "mcycle";
            case 0xFC1: return "minstret";
            case 0xFC2: return "mhpmcounter3";
            case 0xFC3: return "mhpmcounter4";
            default: return "0x" + Integer.toHexString(addr);
        }
    }

    /**
     * Read a byte, routing through the ESP32-C3 memory map when set
     * (Task 6), else falling back to the raw code Memory.
     * @param addr
     * @return 
     * @throws jCPU.RiscV.BusErrorException
     */
    public int readByte(int addr) throws BusErrorException {
        if (memoryMap != null) {
            return memoryMap.readByte(addr);
        }
        return code.read(addr);
    }

    @Override
    public void go(int limit) throws Exception{
        int steps = 0;
        while(true){
            // Stop cleanly at the end of the loaded program instead of throwing a
            // NullPointerException when the PC runs past mapped code.
            if (!code.containsKey(pc) || !code.containsKey(pc + 1) ||
                !code.containsKey(pc + 2) || !code.containsKey(pc + 3)) {
                dumpRegisters("end of program");
                return;
            }
            // RV32I instructions are 4 bytes; a misaligned PC cannot be fetched.
            if (pc % 4 != 0) {
                throw new BusErrorException(pc, "instruction fetch");
            }
            int effectiveLimit = limit > 0 ? limit : MAX_STEPS;
            if (steps >= effectiveLimit) {
                dumpRegisters("step limit reached");
                return;
            }
            RVInstruction ins = new RVInstruction(code.read16(pc));
            if (DEBUGGING) {
                int instr = code.read32(pc);
                System.out.println(cc + " PC=0x" + Integer.toHexString(pc) +
                                   " ins=0x" + Integer.toHexString(instr) +
                                   " opcode=0x" + Integer.toHexString(instr & 0x7f));
            }
            decoder.decode_riscv_binary(ins);
            step();
            steps++;
        }
    }

    /**
     * Dump the current register file and program counter to stdout.
     * Used for debugging when execution stops cleanly.
     *
     * @param reason a short description of why execution stopped
     */
    public void dumpRegisters(String reason) {
        System.out.println("Register dump (" + reason + "):");
        System.out.println("pc = 0x" + Integer.toHexString(pc) + " (" + pc + ")");
        for (int i = 0; i < reg.length; i++) {
            System.out.println("x" + i + " : 0x" + Integer.toHexString(reg[i]) + " (" + reg[i] + ")");
        }
    }

    @Override
    public int step() throws BusErrorException {
            // Combine four bytes to produce a single instruction
            int instr;
            if (memoryMap != null) {
                instr = memoryMap.read32(pc);
            } else {
                instr = code.read32(pc);
            }

            // Retrieve the least significant seven bits of the instruction
            // indicating the type of instruction
            int opcode = instr & 0x7f;

            // Execute the instruction based on its type
            switch (opcode) {
            case LUI_MASK:
                    if (DEBUGGING) {
                            System.out.println(cc + " LUI ");
                    }
                    loadUpperImmediate(instr);
                    break;

            case AUIPC_MASK:
                    if (DEBUGGING) {
                            System.out.println(cc + " AUIPC ");
                    }
                    addUpperImmediatePC(instr);
                    break;

            case JAL_MASK:
                    if (DEBUGGING) {
                            System.out.println(cc + " JAL ");
                    }
                    jumpAndLink(instr);
                    offsetPC = true;
                    break;

            case JALR_MASK:
                    if (DEBUGGING) {
                            System.out.println(cc + " JALR ");
                    }
                    jumpAndLinkRegister(instr);
                    offsetPC = true;
                    break;

            case BRANCH_MASK:
                    if (DEBUGGING) {
                            System.out.println(cc + " Branch ");
                    }
                    offsetPC = branchInstruction(instr);
                    break;

            case LOAD_MASK:
                    if (DEBUGGING) {
                            System.out.println(cc + " Load ");
                    }
                    loadInstruction(instr);
                    break;

            case STORE_MASK:
                    if (DEBUGGING) {
                            System.out.println(cc + " Store ");
                    }
                    storeInstruction(instr);
                    break;

            case OP_IMM_MASK: // Immediate instructions
                    if (DEBUGGING) {
                            System.out.println(cc + " Immediate ");
                    }
                    immediateInstruction(instr);
                    break;

            case OP_MASK: // Arithmetic
                    if (DEBUGGING) {
                            System.out.println(cc + " Arithmetic ");
                    }
                    arithmeticInstruction(instr);
                    break;

            case FENCE_MASK: // Fence (no-op)
                    break;

            case CSR_MASK: // Ecalls and CSR instructions
                    {
                        int funct3 = (instr >> 12) & 0x7;
                        int csr = (instr >> 20) & 0xFFF;
                        int rd = (instr >> 7) & 0x1F;
                        int rs1 = (instr >> 15) & 0x1F;
                        int zimm = (instr >> 15) & 0x1F; // CSR immediate (zero-extended)
                        int csrVal = csrRegisters.getOrDefault(csr, 0);
                        switch (funct3) {
                        case 0x0: // ECALL / EBREAK / CSRRW(read-only)
                            if (csr == 0) {
                                // ECALL or EBREAK (imm[11:0] distinguishes them)
                                breakProgram = ecallInstruction();
                            } else {
                                // CSRRW with rs1=0: read-only CSR access
                                reg[rd] = csrVal;
                            }
                            break;
                        case 0x1: // CSRRW (Atomic Read/Write CSR)
                            reg[rd] = csrVal;
                            if (rs1 != 0) {
                                csrRegisters.put(csr, reg[rs1]);
                            }
                            break;
                        case 0x2: // CSRRS (Atomic Read and Set bits in CSR)
                            reg[rd] = csrVal;
                            if (rs1 != 0) {
                                csrRegisters.put(csr, csrVal | reg[rs1]);
                            }
                            break;
                        case 0x3: // CSRRC (Atomic Read and Clear bits in CSR)
                            reg[rd] = csrVal;
                            if (rs1 != 0) {
                                csrRegisters.put(csr, csrVal & ~reg[rs1]);
                            }
                            break;
                        case 0x5: // CSRRWI (Atomic Read/Write CSR immediate)
                            reg[rd] = csrVal;
                            csrRegisters.put(csr, zimm);
                            break;
                        case 0x6: // CSRRSI (Atomic Read and Set bits in CSR immediate)
                            reg[rd] = csrVal;
                            csrRegisters.put(csr, csrVal | zimm);
                            break;
                        case 0x7: // CSRRCI (Atomic Read and Clear bits in CSR immediate)
                            reg[rd] = csrVal;
                            csrRegisters.put(csr, csrVal & ~zimm);
                            break;
                        default:
                            if (DEBUGGING) {
                                System.out.println("CSR funct3 " + funct3 + " not yet implemented");
                            }
                            break;
                        }
                    }
                    break;

            default:
                    if (DEBUGGING) {
                            System.out.println("Opcode " + opcode + " not yet implemented");
                    }
                    break;
            }

            if (!offsetPC) {
                    pc += 4; // Update program counter
            }
            offsetPC = false; // Reset the pc offset flag
            reg[0] = 0; // Resetting the x0 register

            // Ecall or Ebreak has been encountered
            if (breakProgram || !code.containsKey(pc)) {
                    if (DEBUGGING) {
                            System.out.println("Ecall or end of program encountered");
                    }
                    //printBinary(FILEPATH);
                    if (DEBUGGING) {
                            //printFile(FILEPATH);
                    }
                    return 1;
            }
            cc++;
            if (timer != null) {
                timer.tick();
            }
            return 1;
    }

    /**
     * Read a byte through the memory map when set, else from raw code Memory.
     * Throws {@link BusErrorException} on unmapped addresses.
     */
    private int readMemByte(int addr) throws BusErrorException {
        if (memoryMap != null) {
            return memoryMap.readByte(addr);
        }
        return code.read(addr);
    }

    /**
     * Write a byte through the memory map when set, else to raw code Memory.
     * Throws {@link BusErrorException} on unmapped addresses.
     */
    private void writeMemByte(int addr, int value) throws BusErrorException {
        if (memoryMap != null) {
            memoryMap.writeByte(addr, (byte) value);
        } else {
            code.write(addr, value);
        }
    }

    private int readMem16(int addr) throws BusErrorException {
        return ((readMemByte(addr + 1) & 0xFF) << 8) | (readMemByte(addr) & 0xFF);
    }

    private int readMem32(int addr) throws BusErrorException {
        return ((readMemByte(addr + 3) & 0xFF) << 24) |
               ((readMemByte(addr + 2) & 0xFF) << 16) |
               ((readMemByte(addr + 1) & 0xFF) << 8) |
               (readMemByte(addr) & 0xFF);
    }

    private void writeMem16(int addr, int value) throws BusErrorException {
        writeMemByte(addr, value);
        writeMemByte(addr + 1, value >> 8);
    }

    private void writeMem32(int addr, int value) throws BusErrorException {
        writeMemByte(addr, value);
        writeMemByte(addr + 1, value >> 8);
        writeMemByte(addr + 2, value >> 16);
        writeMemByte(addr + 3, value >> 24);
    }
    
    /**
     * Disassemble the instruction at {@code pc} without modifying any simulator
     * state (no register writes, no PC advancement, no memory side effects).
     *
     * <p>Supports both 32-bit RV32I instructions and 16-bit RVC compressed
     * instructions. If the address is unmapped or the encoding is unknown, a
     * descriptive string is returned instead of throwing.</p>
     *
     * @param pc byte address of the instruction to disassemble
     * @return a human-readable disassembly line
     */
    /**
     * Get the length of the instruction at {@code pc} for the GUI disassembly panel.
     * RISC-V instructions are 4 bytes (or 2 bytes for RVC compressed). Returns 1 for
     * unmapped addresses so the decoder advances instead of looping forever.
     * @param pc byte address of the instruction
     * @return instruction length in bytes (2, 4, or 1 for unmapped)
     */
    @Override
    public int getLengthAt(int pc) {
        // Cannot fetch even 2 bytes from an unmapped address
        if (!code.containsKey(pc) || !code.containsKey(pc + 1)) {
            return 1;
        }

        int cInstr = code.read16(pc);

        // RVC compressed instruction: lowest two bits are not "11"
        if ((cInstr & 0x3) != 0x3) {
            return 2;
        }

        // 32-bit instruction
        return 4;
    }

    @Override
    public String getDecodeAt(int pc) {
        // Cannot fetch even 2 bytes from an unmapped address
        if (!code.containsKey(pc) || !code.containsKey(pc + 1)) {
            return String.format("     0x%08x: <unmapped>", pc);
        }

        int cInstr = code.read16(pc);

        // RVC compressed instruction: lowest two bits are not "11"
        if ((cInstr & 0x3) != 0x3) {
            return String.format("     0x%08x: %04x  %s", pc, cInstr,
                    decodeCompressed(cInstr, pc));
        }

        // 32-bit instruction: need all 4 bytes
        if (!code.containsKey(pc + 2) || !code.containsKey(pc + 3)) {
            return String.format("     0x%08x: <unmapped>", pc);
        }

        int instr = code.read32(pc);
        return String.format("     0x%08x: %08x  %s", pc, instr,
                decode32Bit(instr, pc));
    }

    /* ------------------------------------------------------------------ */
    /* Read-only 32-bit RV32I disassembly                                 */
    /* ------------------------------------------------------------------ */

    private String decode32Bit(int instr, int pc) {
        int opcode = instr & 0x7f;
        int rd = (instr >> 7) & 0x1f;
        int rs1 = (instr >> 15) & 0x1f;
        int rs2 = (instr >> 20) & 0x1f;
        int funct3 = (instr >> 12) & 0x7;
        int funct7 = (instr >> 25);

        switch (opcode) {
        case LUI_MASK: {
            int imm = instr & 0xfffff000;
            return String.format("lui x%d, 0x%x", rd, imm);
        }
        case AUIPC_MASK: {
            int imm = instr & 0xfffff000;
            return String.format("auipc x%d, 0x%x", rd, imm);
        }
        case JAL_MASK: {
            int imm = ((instr >> (31 - 20)) & (1 << 20))
                    | ((instr >> (21 - 1)) & 0x7fe)
                    | ((instr >> (20 - 11)) & (1 << 11))
                    | (instr & 0xff000);
            imm = (imm << 11) >> 11; // sign-extend 20 bits
            return String.format("jal x%d, 0x%x (pc + %d -> 0x%x)",
                    rd, imm, imm, pc + imm);
        }
        case JALR_MASK: {
            int imm = (instr >> 20);
            imm = (imm << 20) >> 20; // sign-extend 12 bits
            return String.format("jalr x%d, %d(x%d)", rd, imm, rs1);
        }
        case BRANCH_MASK: {
            int imm = ((instr >> (31 - 12)) & (1 << 12))
                    | ((instr >> (25 - 5)) & 0x7e0)
                    | ((instr >> (8 - 1)) & 0x1e)
                    | ((instr << (11 - 7)) & (1 << 11));
            imm = (imm << 19) >> 19; // sign-extend 12 bits
            String mnemonic;
            switch (funct3) {
            case 0x0: mnemonic = "beq"; break;
            case 0x1: mnemonic = "bne"; break;
            case 0x4: mnemonic = "blt"; break;
            case 0x5: mnemonic = "bge"; break;
            case 0x6: mnemonic = "bltu"; break;
            case 0x7: mnemonic = "bgeu"; break;
            default: mnemonic = "branch?"; break;
            }
            return String.format("%s x%d, x%d, 0x%x (pc + %d -> 0x%x)",
                    mnemonic, rs1, rs2, imm, imm, pc + imm);
        }
        case LOAD_MASK: {
            int imm = (instr >> 20);
            imm = (imm << 20) >> 20; // sign-extend 12 bits
            String mnemonic;
            switch (funct3) {
            case 0x0: mnemonic = "lb"; break;
            case 0x1: mnemonic = "lh"; break;
            case 0x2: mnemonic = "lw"; break;
            case 0x4: mnemonic = "lbu"; break;
            case 0x5: mnemonic = "lhu"; break;
            case 0x6: mnemonic = "lwu"; break;
            default: mnemonic = "load?"; break;
            }
            return String.format("%s x%d, %d(x%d)", mnemonic, rd, imm, rs1);
        }
        case STORE_MASK: {
            int imm = ((instr >> 7) & 0x1f) | ((instr >> 25) << 5);
            imm = (imm << 20) >> 20; // sign-extend 12 bits
            String mnemonic;
            switch (funct3) {
            case 0x0: mnemonic = "sb"; break;
            case 0x1: mnemonic = "sh"; break;
            case 0x2: mnemonic = "sw"; break;
            default: mnemonic = "store?"; break;
            }
            return String.format("%s x%d, %d(x%d)", mnemonic, rs2, imm, rs1);
        }
        case OP_IMM_MASK:
        case OP_IMM_32_MASK: {
            int imm = (instr >> 20);
            imm = (imm << 20) >> 20; // sign-extend 12 bits
            String mnemonic;
            switch (funct3) {
            case 0x0: mnemonic = "addi"; break;
            case 0x2: mnemonic = "slti"; break;
            case 0x3: mnemonic = "sltiu"; break;
            case 0x4: mnemonic = "xori"; break;
            case 0x6: mnemonic = "ori"; break;
            case 0x7: mnemonic = "andi"; break;
            case 0x1:
                if (funct7 == 0x00) mnemonic = "slli";
                else mnemonic = "slli?";
                break;
            case 0x5:
                if (funct7 == 0x00) mnemonic = "srli";
                else if (funct7 == 0x20) mnemonic = "srai";
                else mnemonic = "sr?";
                break;
            default: mnemonic = "op-imm?"; break;
            }
            if (funct3 == 0x1 || funct3 == 0x5) {
                int shamt = (instr >> 20) & 0x1f;
                return String.format("%s x%d, x%d, %d", mnemonic, rd, rs1, shamt);
            }
            return String.format("%s x%d, x%d, %d", mnemonic, rd, rs1, imm);
        }
        case OP_MASK:
        case OP_MASK_32: {
            String mnemonic;
            switch (funct7) {
            case 0x01: // MUL/DIV extensions
                switch (funct3) {
                case 0x0: mnemonic = "mul"; break;
                case 0x1: mnemonic = "mulh"; break;
                case 0x2: mnemonic = "mulhsu"; break;
                case 0x3: mnemonic = "mulhu"; break;
                case 0x4: mnemonic = "div"; break;
                case 0x5: mnemonic = "divu"; break;
                case 0x6: mnemonic = "rem"; break;
                case 0x7: mnemonic = "remu"; break;
                default: mnemonic = "muldiv?"; break;
                }
                break;
            default: // ADD/SUB and logic
                switch (funct3) {
                case 0x0: mnemonic = (funct7 == 0x00) ? "add" : "sub"; break;
                case 0x1: mnemonic = "sll"; break;
                case 0x2: mnemonic = "slt"; break;
                case 0x3: mnemonic = "sltu"; break;
                case 0x4: mnemonic = "xor"; break;
                case 0x5: mnemonic = (funct7 == 0x00) ? "srl" : "sra"; break;
                case 0x6: mnemonic = "or"; break;
                case 0x7: mnemonic = "and"; break;
                default: mnemonic = "op?"; break;
                }
                break;
            }
            return String.format("%s x%d, x%d, x%d", mnemonic, rd, rs1, rs2);
        }
        case FENCE_MASK:
            return "fence";
        case CSR_MASK: {
            int csr = (instr >> 20) & 0xFFF;
            int zimm = rs1; // CSR immediate is in rs1 field
            switch (funct3) {
            case 0x0:
                if (csr == 0) return "ecall";
                return String.format("csrrw x%d, 0x%x", rd, csr);
            case 0x1:
                return String.format("csrrw x%d, 0x%x, x%d", rd, csr, rs1);
            case 0x2:
                return String.format("csrrs x%d, 0x%x, x%d", rd, csr, rs1);
            case 0x3:
                return String.format("csrrc x%d, 0x%x, x%d", rd, csr, rs1);
            case 0x5:
                return String.format("csrrwi x%d, 0x%x, %d", rd, csr, zimm);
            case 0x6:
                return String.format("csrrsi x%d, 0x%x, %d", rd, csr, zimm);
            case 0x7:
                return String.format("csrrci x%d, 0x%x, %d", rd, csr, zimm);
            default:
                return String.format("csr? x%d, 0x%x", rd, csr);
            }
        }
        default:
            return String.format("<opcode 0x%02x not implemented>", opcode);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Read-only RVC (compressed 16-bit) disassembly                      */
    /* ------------------------------------------------------------------ */

    private String decodeCompressed(int cInstr, int pc) {
        int funct3 = (cInstr >> 13) & 0x7;
        int quad = cInstr & 0x3;

        switch (quad) {
        case 0: // Quadrant 0
            switch (funct3) {
            case 0x0: // c.addi4spn
                {
                    int imm = ((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1e)
                            | ((cInstr >> 6) & 0x4) | ((cInstr >> 5) & 0x8);
                    if (imm == 0) return "c.illegal";
                    return String.format("c.addi4spn sp, %d", imm);
                }
            case 0x1: // c.fld (not RV32I core)
            case 0x3: // c.ld
                return "c.fld/c.ld (FP/64-bit)";
            case 0x2: // c.lw
                {
                    int imm = ((cInstr >> 7) & 0x3c) | ((cInstr >> 5) & 0x40)
                            | ((cInstr >> 2) & 0x4);
                    int rs1 = ((cInstr >> 7) & 0x7) | 0x8;
                    return String.format("c.lw x%d, %d(x%d)", rs1, imm, rs1);
                }
            case 0x5: // c.fsd
            case 0x6: // c.sw
                {
                    int imm = ((cInstr >> 7) & 0x3c) | ((cInstr >> 5) & 0x40)
                            | ((cInstr >> 2) & 0x4);
                    int rs2 = ((cInstr >> 7) & 0x7) | 0x8;
                    int rs1 = ((cInstr >> 7) & 0x7) | 0x8;
                    return String.format("c.sw x%d, %d(x%d)", rs2, imm, rs1);
                }
            case 0x7: // c.sd
                return "c.sd (64-bit)";
            default:
                return "c.unknown q0";
            }
        case 1: // Quadrant 1
            switch (funct3) {
            case 0x0: // c.addi / c.nop
                {
                    int rd = (cInstr >> 7) & 0x1f;
                    int imm = (((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f));
                    imm = (imm << 26) >> 26; // sign-extend 6 bits
                    if (rd == 0 && imm == 0) return "c.nop";
                    return String.format("c.addi x%d, x%d, %d", rd, rd, imm);
                }
            case 0x1: // c.addiw
                {
                    int rd = (cInstr >> 7) & 0x1f;
                    int imm = (((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f));
                    imm = (imm << 26) >> 26;
                    return String.format("c.addiw x%d, x%d, %d", rd, rd, imm);
                }
            case 0x2: // c.li
                {
                    int rd = (cInstr >> 7) & 0x1f;
                    int imm = (((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f));
                    imm = (imm << 26) >> 26;
                    return String.format("c.li x%d, %d", rd, imm);
                }
            case 0x3:
                {
                    int rd = (cInstr >> 7) & 0x1f;
                    if (rd == 2) {
                        int imm = (((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f)
                                | ((cInstr >> 6) & 0x40) | ((cInstr >> 3) & 0x38)
                                | ((cInstr >> 2) & 0x40));
                        imm = (imm << 22) >> 22;
                        if (imm == 0) return "c.illegal";
                        return String.format("c.addi16sp sp, %d", imm);
                    } else if (rd != 0) {
                        int imm = (((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f)
                                | ((cInstr >> 6) & 0x100) >> 2);
                        imm = (imm << 22) >> 22;
                        return String.format("c.lui x%d, %d", rd, imm);
                    }
                    return "c.unknown";
                }
            case 0x4: // c.srli/c.srai/c.andi/c.sub/c.xor/c.or/c.and/c.subw/c.addw
                {
                    int funct4 = (cInstr >> 10) & 0x3;
                    int rd = ((cInstr >> 7) & 0x7) | 0x8;
                    int rs1 = rd;
                    switch (funct4) {
                    case 0x0: // c.srli / c.srai
                        {
                            int imm = ((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f);
                            int funct5 = (cInstr >> 5) & 0x2;
                            String m = (funct5 == 0x0) ? "c.srli" : "c.srai";
                            return String.format("%s x%d, x%d, %d", m, rd, rd, imm);
                        }
                    case 0x1: // c.srai
                        return "c.srai";
                    case 0x2: // c.andi
                        {
                            int imm = (((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f));
                            imm = (imm << 26) >> 26;
                            return String.format("c.andi x%d, x%d, %d", rd, rd, imm);
                        }
                    case 0x3: // c.sub/c.xor/c.or/c.and/c.subw/c.addw
                        {
                            int rs2 = ((cInstr >> 2) & 0x7) | 0x8;
                            int funct5 = ((cInstr >> 5) & 0x3) | ((cInstr >> 10) & 0x4);
                            String m;
                            switch (funct5) {
                            case 0x0: m = "c.sub"; break;
                            case 0x1: m = "c.xor"; break;
                            case 0x2: m = "c.or"; break;
                            case 0x3: m = "c.and"; break;
                            case 0x4: m = "c.subw"; break;
                            case 0x5: m = "c.addw"; break;
                            default: m = "c.alu?"; break;
                            }
                            return String.format("%s x%d, x%d, x%d", m, rd, rd, rs2);
                        }
                    default:
                        return "c.unknown q1.4";
                    }
                }
            case 0x5: // c.j
                {
                    int imm = (((cInstr >> 7) & 0x800) >> 3) | (((cInstr >> 4) & 0x700) >> 2)
                            | (((cInstr >> 8) & 0x400) >> 1) | (((cInstr >> 6) & 0x200))
                            | (((cInstr >> 5) & 0x100)) | (((cInstr >> 3) & 0x80))
                            | (((cInstr >> 1) & 0x40));
                    imm = (imm << 22) >> 22;
                    return String.format("c.j 0x%x (pc + %d -> 0x%x)", imm, imm, pc + imm);
                }
            case 0x6: // c.beqz
                {
                    int rs1 = ((cInstr >> 7) & 0x7) | 0x8;
                    int imm = (((cInstr >> 7) & 0x100) >> 1) | (((cInstr >> 3) & 0x18))
                            | (((cInstr >> 5) & 0x60) >> 1) | (((cInstr >> 1) & 0x4));
                    imm = (imm << 25) >> 25;
                    return String.format("c.beqz x%d, 0x%x (pc + %d -> 0x%x)",
                            rs1, imm, imm, pc + imm);
                }
            case 0x7: // c.bnez
                {
                    int rs1 = ((cInstr >> 7) & 0x7) | 0x8;
                    int imm = (((cInstr >> 7) & 0x100) >> 1) | (((cInstr >> 3) & 0x18))
                            | (((cInstr >> 5) & 0x60) >> 1) | (((cInstr >> 1) & 0x4));
                    imm = (imm << 25) >> 25;
                    return String.format("c.bnez x%d, 0x%x (pc + %d -> 0x%x)",
                            rs1, imm, imm, pc + imm);
                }
            default:
                return "c.unknown q1";
            }
        case 2: // Quadrant 2
            switch (funct3) {
            case 0x0: // c.slli
                {
                    int rd = (cInstr >> 7) & 0x1f;
                    int imm = ((cInstr >> 7) & 0x20) | ((cInstr >> 2) & 0x1f);
                    return String.format("c.slli x%d, x%d, %d", rd, rd, imm);
                }
            case 0x1: // c.fldsp (FP/64-bit)
                return "c.fldsp (FP)";
            case 0x2: // c.lwsp
                {
                    int rd = (cInstr >> 7) & 0x1f;
                    int imm = ((cInstr >> 7) & 0x3c) | ((cInstr >> 2) & 0x60);
                    return String.format("c.lwsp x%d, %d(sp)", rd, imm);
                }
            case 0x3: // c.ldsp
                return "c.ldsp (64-bit)";
            case 0x4:
                {
                    int rd = (cInstr >> 7) & 0x1f;
                    int rs2 = (cInstr >> 2) & 0x1f;
                    if (((cInstr >> 12) & 0x1) == 0) {
                        if (rs2 == 0) {
                            if (rd == 0) return "c.illegal";
                            return String.format("c.jr x%d", rd);
                        } else {
                            return String.format("c.mv x%d, x%d", rd, rs2);
                        }
                    } else {
                        if (rs2 == 0) {
                            if (rd == 0) return "c.ebreak";
                            return String.format("c.jalr x%d, x%d", 1, rd);
                        } else {
                            return String.format("c.add x%d, x%d, x%d", rd, rd, rs2);
                        }
                    }
                }
            case 0x5: // c.fsdsp (FP)
                return "c.fsdsp (FP)";
            case 0x6: // c.swsp
                {
                    int rs2 = (cInstr >> 2) & 0x1f;
                    int imm = ((cInstr >> 7) & 0x1c) | ((cInstr >> 2) & 0x60);
                    return String.format("c.swsp x%d, %d(sp)", rs2, imm);
                }
            case 0x7: // c.sdsp
                return "c.sdsp (64-bit)";
            default:
                return "c.unknown q2";
            }
        default:
            return "c.illegal";
        }
    }

	private String loadUpperImmediate(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = instr & 0xFFFFF000;
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", imm = " + imm);
		}
		reg[rd] = imm; // LUI stores the immediate in the top 20 bits of register rd
                return "rd = " + rd + ", imm = " + imm;
	}

	private String addUpperImmediatePC(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = instr & 0xFFFFF000;
		if (DEBUGGING) {
                    System.out.println("rd = " + rd + ", imm = " + imm);
		}
		reg[rd] = pc + imm;
                return "rd = " + rd + ", imm = " + imm;
	}

	private String jumpAndLink(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = (((instr >> 21) & 0x3FF) << 1) | (((instr >> 20) & 0x1) << 11) | 
				  (((instr >> 12) & 0xFF) << 12) | ((instr >> 31) << 20);
		if ((instr >> 31) == 1) {
			imm |= 0xFFF00000; // Sign-extension if necessary
		}
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", imm = " + imm);
		}
		reg[rd] = pc + 4; // Store return address
		pc += imm; // Jump target address
		if (pc % 4 != 0) {
			System.out.println("Instruction fetch exception; pc not multiple of 4 bytes");
		}
                return "rd = " + rd + ", imm = " + imm;
	}

	private String jumpAndLinkRegister(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int rs1 = (instr >> 15) & 0x1F;
		int imm = (instr >> 20);
		if ((imm >> 11) == 1) {
			imm |= 0xFFFFF000; // Sign-extension if necessary
		}
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", rs1 = " + rs1 + ", imm = " + imm);
		}
		reg[rd] = pc + 4; // Store return address
		pc = (reg[rs1] + imm) & 0xFFFFFFFC; // Jump target address is 4-byte aligned
		if (pc % 4 != 0) {
			System.out.println("Instruction fetch exception; pc not multiple of 4 bytes");
		}
                return "rd = " + rd + ", rs1 = " + rs1 + ", imm = " + imm;
	}

	private boolean branchInstruction(int instr) {
		// General information
		int rs1 = (instr >> 15) & 0x1F;
		int rs2 = (instr >> 20) & 0x1F;
		int imm = (((instr >> 8) & 0xF) << 1) | (((instr >> 25) & 0x3F) << 5) | 
				  (((instr >> 7) & 0x1) << 11) | ((instr >> 31) << 12);
		if ((imm >> 11) == 1) {
			imm |= 0xFFFFE000; // Sign-extension if necessary
		}
		if (DEBUGGING) {
			System.out.println("rs1 = " + rs1 + ", rs2 = " + rs2 + ", imm = " + imm);
		}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		switch (funct3) {
                    case 0x0: // BEQ
			if (reg[rs1] == reg[rs2]) {
				pc += imm;
				return true;
			}
			break;
                    case 0x1: // BNE
			if (reg[rs1] != reg[rs2]) {
				pc += imm;
				return true;
			}
			break;
                    case 0x4: // BLT
			if (reg[rs1] < reg[rs2]) {
				pc += imm;
				return true;
			}
			break;
                    case 0x5: // BGE
			if (reg[rs1] >= reg[rs2]) {
				pc += imm;
				return true;
			}
			break;
                    case 0x6: // BLTU
			if (((long) reg[rs1] & 0xFFFFFFFFL) < ((long) reg[rs2] & 0xFFFFFFFFL)) {
				pc += imm;
				return true;
			}
			break;
                    case 0x7: // BGEU
			if (((long) reg[rs1] & 0xFFFFFFFFL) >= ((long) reg[rs2] & 0xFFFFFFFFL)) {
				pc += imm;
				return true;
			}
			break;
		}
		if (pc % 4 != 0) {
			System.out.println("Instruction fetch exception; pc not multiple of 4 bytes");
		}
		return false;
	}

	private String loadInstruction(int instr) throws BusErrorException {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int rs1 = (instr >> 15) & 0x1F;
		int imm = (instr >> 20);
		if ((instr >> 31) == 1) {
			imm |= 0xFFFFF000; // Sign-extension if necessary
		}
		int memAddr = reg[rs1] + imm;
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", rs1 = " + rs1 + ", imm = " + imm + ", memAddr = " + memAddr);
		}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		// Initializing an integer to read data into
		int memValue = 0;
		switch (funct3) {
		case 0x0: // LB
                    if(running)
			memValue = readMemByte(memAddr);
			if ((memValue >> 7) == 1) {
				memValue |= 0xFFFFFF00; // Sign-extension if necessary
			}
			break;
		case 0x1: // LH
                    if(running)
			memValue = readMem16(memAddr);
			if ((memValue >> 15) == 1) {
				memValue |= 0xFFFF0000; // Sign-extension if necessary
			}
			break;
		case 0x2: // LW
                    if(running)
			memValue = readMem16(memAddr);
			break;
		case 0x4: // LBU
                    if(running)
			memValue = readMemByte(memAddr);
			break;
		case 0x5: // LHU
			memValue = readMem16(memAddr);
			break;
		case 0x6: // LWU (load word unsigned, zero-extended)
                    if(running)
			memValue = readMem32(memAddr);
			break;
		}
		reg[rd] = memValue;
                return "rd = " + rd + ", rs1 = " + rs1 + ", imm = " + imm + ", memAddr = " + memAddr;
	}
	
	private String storeInstruction(int instr) throws BusErrorException {
		// General information
		int rs1 = (instr >> 15) & 0x1F;
		int rs2 = (instr >> 20) & 0x1F;
		int imm = ((instr >> 7) & 0x1F) | ((instr >> 25) << 5);
		if ((instr >> 31) == 1) {
			imm |= 0xFFFFF000; // Sign-extension if necessary
		}
		int memAddr = reg[rs1] + imm;
		if (DEBUGGING) {
			System.out.println("rs1 = " + rs1 + ", rs2 = " + rs2 + ", imm = " + imm + ", memAddr = " + memAddr);
		}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		// Value to store to memory
		int memValue = reg[rs2];
		switch (funct3) {
                    case 0x0: // SB
			memValue &= 0x000000FF;
                        if(running)
                            writeMemByte(memAddr, memValue);
			break;
                    case 0x1: // SH
			memValue &= 0x0000FFFF;
			writeMem16(memAddr, memValue);
			break;
                    case 0x2: // SW
                        if(running)
                            writeMem16(memAddr, memValue);
			break;
		}
                return "rs1 = " + rs1 + ", rs2 = " + rs2 + ", imm = " + imm + ", memAddr = " + memAddr;
	}

	private String immediateInstruction(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int rs1 = (instr >> 15) & 0x1F;
		int imm = (instr >> 20);
		if ((imm >> 11) == 1) {
			imm |= 0xFFFFF000; // Sign-extension if necessary
		}
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", rs1 = " + rs1 + ", imm = " + imm);
		}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		switch (funct3) {
                    case 0x0: // ADDI
			reg[rd] = reg[rs1] + imm;
			break;
                    case 0x2: // SLTI (signed comparison)
			if (reg[rs1] < imm) {
				reg[rd] = 1;
			} else {
				reg[rd] = 0;
			}
			break;
                    case 0x3: // SLTIU (unsigned comparison)
			if (((long) reg[rs1] & 0xFFFFFFFFL) < ((long) imm & 0xFFFFFFFFL)) {
				reg[rd] = 1;
			} else {
				reg[rd] = 0;
			}
			break;
                    case 0x4: // XORI
			reg[rd] = reg[rs1] ^ imm;
			break;
                    case 0x6: // ORI
			reg[rd] = reg[rs1] | imm;
			break;
                    case 0x7: // ANDI
			reg[rd] = reg[rs1] & imm;
			break;
                    default:
			// Additional information
			int funct7 = (instr >> 25);
			int shamt = (instr >> 20) & 0x1F;
			if (DEBUGGING) {
				System.out.println("funct3 = " + funct3 + ", funct7 = " + funct7 + ", shamt = " + shamt);
			}
			if (funct3 == 0x1 && funct7 == 0x00) { // SLLI
				reg[rd] = (reg[rs1] << shamt);
				break;
			} else if (funct3 == 0x5 && funct7 == 0x00) { // SRLI
				reg[rd] = (reg[rs1] >>> shamt);
				break;
			} else { // SRAI
				reg[rd] = (reg[rs1] >> shamt);
				break;
			}
		}
                return "rd = " + rd + ", rs1 = " + rs1 + ", imm = " + imm;
	}

	private String arithmeticInstruction(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int rs1 = (instr >> 15) & 0x1F;
		int rs2 = (instr >> 20) & 0x1F;
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", rs1 = " + rs1 + ", rs2 = " + rs2);
		}
		// Determining the type of instruction
		int funct3 = (instr >> 12) & 0x7;
		int funct7 = (instr >> 25);
		if (DEBUGGING) {
			System.out.println("funct3 = " + funct3 + ", funct7 = " + funct7);
		}
		switch (funct7) {
                    case 0x1: // MUL, DIV and so on
			long regrs1 = reg[rs1], regrs2 = reg[rs2];
			switch (funct3) {
                            case 0x0: // MUL
				reg[rd] = reg[rs1] * reg[rs2];
				break;
                            case 0x1: // MULH
				reg[rd] = (int) ((regrs1 * regrs2) >> 32) & 0xFFFFFFFF;
				break;
                            case 0x2: // MULHSU
				regrs2 &= 0xFFFFFFFFL;
				reg[rd] = (int) ((regrs1 * regrs2) >> 32) & 0xFFFFFFFF;
				break;
                            case 0x3: // MULHU
				regrs1 &= 0xFFFFFFFFL;
				regrs2 &= 0xFFFFFFFFL;
				reg[rd] = (int) ((regrs1 * regrs2) >> 32) & 0xFFFFFFFF;
				break;
                            case 0x4: // DIV
				if (reg[rs2] == 0) { // Division by zero
					reg[rd] = -1;
				} else {
					reg[rd] = reg[rs1] / reg[rs2];
				}
				break;
                            case 0x5: // DIVU
				if (reg[rs2] == 0) { // Division by zero
					reg[rd] = -1;
				} else {
					regrs1 &= 0xFFFFFFFFL;
					regrs2 &= 0xFFFFFFFFL;
					reg[rd] = (int) (regrs1 / regrs2);
				}
				break;
                            case 0x6: // REM
				if (reg[rs2] == 0) { // Remainder by zero
					reg[rd] = reg[rs1];
				} else {
					reg[rd] = reg[rs1] % reg[rs2];
				}
				break;
                            case 0x7: // REMU
				if (reg[rs2] == 0) { // Remainder by zero
					reg[rd] = reg[rs1];
				} else {
					regrs1 &= 0xFFFFFFFFL;
					regrs2 &= 0xFFFFFFFFL;
					reg[rd] = (int) (regrs1 % regrs2);
				}
				break;
			}
			break;
                    default: // ADD, SUB and so on
			switch (funct3) {
                            case 0x0: // ADD or SUB
				if (funct7 == 0x00) { // ADD
					reg[rd] = reg[rs1] + reg[rs2];
				} else { // SUB
					reg[rd] = reg[rs1] - reg[rs2];
				}
				break;
                            case 0x1: // SLL
				reg[rd] = (reg[rs1] << (reg[rs2] & 0x1F));
				break;
                            case 0x2: // SLT (signed comparison)
				if (reg[rs1] < reg[rs2]) {
					reg[rd] = 1;
				} else {
					reg[rd] = 0;
				}
				break;
                            case 0x3: // SLTU (unsigned comparison)
				if (rs1 == 0 && reg[rs2] != 0) { // See manual page 15
					reg[rd] = 1;
				} else {
					if (((long) reg[rs1] & 0xFFFFFFFFL) < ((long) reg[rs2] & 0xFFFFFFFFL)) {
						reg[rd] = 1;
					} else {
						reg[rd] = 0;
					}
				}
				break;
                            case 0x4: // XOR
				reg[rd] = reg[rs1] ^ reg[rs2];
				break;
                            case 0x5: // SRL or SRA
				if (funct7 == 0x00) { // SRL
					reg[rd] = (reg[rs1] >>> (reg[rs2] & 0x1F));
				} else { // SRA
					reg[rd] = (reg[rs1] >> (reg[rs2] & 0x1F));
				}
				break;
                            case 0x6: // OR
				reg[rd] = reg[rs1] | reg[rs2];
				break;
                            case 0x7: // AND
				reg[rd] = reg[rs1] & reg[rs2];
				break;
			}
			break;
		}
                return "rd = " + rd + ", rs1 = " + rs1 + ", rs2 = " + rs2;
	}

	private boolean ecallInstruction() {
		switch (reg[10]) {
                    case 1: // print_int
			System.out.println(reg[11]);
			return false;
                    case 4: // print_string
			return false;
                    case 9: // sbrk
			return false;
                    case 10: // exit
			return true;
                    case 11: // print_character
			System.out.println((char) reg[11]);
			return false;
                    default: // exit2
			reg[11] = 0;
			return true;
		}
	}

	public static void printBinary(String filePath) throws IOException {
		FileOutputStream fileStream = null;
		String filePathLocal = filePath.substring(0, filePath.lastIndexOf(".")) + "_reg.res";
		try {
			fileStream = new FileOutputStream(filePathLocal);
			for (int i = 0; i < reg.length; i++) {
				fileStream.write(intToByteArray(reg[i]));
				System.out.println("x" + i + " : " + reg[i]);
			}
		} catch (IOException e) {
			 Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, e);
		} finally {
			if (fileStream != null) {
				fileStream.close();
			}
		}
		System.out.println("Binary register content is in file " + filePathLocal);
	}

	// From https://javadeveloperzone.com/java-basic/java-convert-int-to-byte-array/#2_int_to_byte_array 
	public static byte[] intToByteArray(int value) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(Integer.reverseBytes(value));
		return buffer.array();
	}

	public static void printFile(String filePath) throws IOException {
		PrintWriter writer = null;
		FileInputStream fileStream = null;
		DataInputStream dataStream = null;
		String filePathLocal = filePath.substring(0, filePath.lastIndexOf(".")) + "_reg.txt";
		if (DEBUGGING) {
			System.out.println("Printing register content to .txt");
		}
		try {
			writer = new PrintWriter(filePathLocal, "UTF-8");
			writer.println("Post-execution register content");
			for (int i = 0; i < reg.length; i++) {
				writer.println("x" + i + " : " + reg[i]);
			}
			fileStream = new FileInputStream(filePath.substring(0, filePath.indexOf(".")) + ".res");
			dataStream = new DataInputStream(fileStream);
			writer.println("\nExpected post-execution register content");
			for (int i = 0; i < reg.length; i++) {
				writer.println("x" + i + " : " + Integer.reverseBytes(dataStream.readInt()));
			}
		} catch (IOException e) {
			 Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, e);
		} finally {
			if (writer != null) {
				writer.close();
			}
			if (dataStream != null) {
				dataStream.close();
			}
			if (fileStream != null) {
				fileStream.close();
			}
		}
		if (DEBUGGING) {
			System.out.println("Finished printing register content to .txt");
		}
		System.out.println("Register content is in file " + filePathLocal);
	}
}
