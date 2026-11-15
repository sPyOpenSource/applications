package jCPU.x86;

import static jCPU.x86.X86Core.*;

public class i8086 implements X86Core.iExecutor {
    private String lastDecode = "";

    /** True when the current instruction carries a 0x66 operand-size prefix. */
    private boolean op32 = false;

    /** True when the current instruction carries a 0x67 address-size prefix. */
    private boolean addr32 = false;

    /** Segment override for the current instruction, or -1 for the default. */
    private int segOverride = -1;

    @Override
    public int step(X86Core core) throws Exception {
        int pc = core.getPC();

        // Consume legacy prefix bytes so the real opcode and its operands are
        // decoded at the right offset.  The emulator executes 16-bit
        // operations, but 0x66 still changes the width of immediate operands
        // (e.g. "66 81 f5 imm32"), so remember it.  0x67 switches ModR/M
        // decoding to 32-bit addressing, and segment overrides select the
        // segment base used for memory operands.
        boolean wide = false;
        boolean address32 = false;
        int override = -1;
        OUTER:
        while (true) {
            int b = core.readMem8(pc) & 0xFF;
            switch (b) {
                case 0x66:
                    wide = true;
                    pc++;
                    break;
                case 0x67:
                    address32 = true;
                    pc++; // 32-bit addressing
                    break;
                case 0x26:
                    override = X86Core.SEG_ES;
                    pc++;
                    break;
                case 0x2E:
                    override = X86Core.SEG_CS;
                    pc++;
                    break;
                case 0x36:
                    override = X86Core.SEG_SS;
                    pc++;
                    break;
                case 0x3E:
                    override = X86Core.SEG_DS;
                    pc++;
                    break;
                case 0x64:
                    override = X86Core.SEG_FS;
                    pc++;
                    break;
                case 0x65:
                    override = X86Core.SEG_GS;
                    pc++;
                    break;
            // LOCK: single-threaded, no effect
                case 0xF0:
                    pc++;
                    break;
                default:
                    break OUTER;
            }
        }
        op32 = wide;
        addr32 = address32;
        segOverride = override;

        int opcode = core.readMem8(pc);
        lastDecode = "";
        int startPC = core.getPC();
        executeOpcode(core, opcode, pc);
        if (core.getPC() == startPC) {
            int len = instructionLen(core, opcode, pc);
            core.setPC(pc + len);
        }
        return 1;
    }

    @Override
    public String getDecodeAt(X86Core core, int pc) {
        int opcode = core.readMem8(pc);
        return String.format("%02X", opcode);
    }

    private int instructionLen(X86Core core, int opcode, int pc) {
        if (op32) {
            switch (opcode) {
                case 0x05: case 0x0D: case 0x15: case 0x1D:
                case 0x25: case 0x2D: case 0x35: case 0x3D:
                case 0xA9:
                    return 5; // AX/EAX, imm32
                case 0x68:
                    return 5; // PUSH imm32
                case 0x81: case 0xC7:
                    return modrmLen(core, pc) + 4;
                default:
                    if (opcode >= 0xB8 && opcode <= 0xBF) {
                        return 5; // MOV r32, imm32
                    }
                    break;
            }
        }
        if (addr32) {
            switch (opcode) {
                case 0xA0: case 0xA1: case 0xA2: case 0xA3:
                    return 5; // moffs32
                default:
                    break;
            }
        }
        switch (opcode) {
            case 0x00: case 0x02: case 0x08: case 0x0A:
            case 0x20: case 0x22: case 0x28: case 0x2A:
            case 0x30: case 0x32: case 0x38: case 0x3A:
            case 0x86: case 0x88: case 0x8A: case 0x8C: case 0x8E:
            case 0x84: case 0x8D: case 0x8F:
            case 0xD0: case 0xD1: case 0xD2: case 0xD3:
                return modrmLen(core, pc);
            case 0x01: case 0x03: case 0x09: case 0x0B:
            case 0x21: case 0x23: case 0x29: case 0x2B:
            case 0x31: case 0x33: case 0x39: case 0x3B:
            case 0x87: case 0x89: case 0x8B: case 0x85:
                return modrmLen(core, pc);
            case 0x04: case 0x0C: case 0x24: case 0x2C: case 0x34: case 0x3C:
            case 0xA8: case 0x6A:
            case 0xE0: case 0xE1: case 0xE2: case 0xE3:
            case 0xE4: case 0xE5: case 0xE6: case 0xE7:
                return 2;
            case 0x05: case 0x0D: case 0x25: case 0x2D: case 0x35: case 0x3D:
            case 0xA9: case 0x68:
                return 3;
            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
            case 0x50: case 0x51: case 0x52: case 0x53:
            case 0x54: case 0x55: case 0x56: case 0x57:
            case 0x58: case 0x59: case 0x5A: case 0x5B:
            case 0x5C: case 0x5D: case 0x5E: case 0x5F:
            case 0x60: case 0x61:
            case 0x06: case 0x07: case 0x0E:
            case 0x16: case 0x17: case 0x1E: case 0x1F:
            case 0x91: case 0x92: case 0x93: case 0x94:
            case 0x95: case 0x96: case 0x97:
            case 0x98: case 0x99: case 0x9C: case 0x9D: case 0x9E: case 0x9F:
            case 0xA4: case 0xA5: case 0xA6: case 0xA7:
            case 0xAA: case 0xAB: case 0xAC: case 0xAD:
            case 0xAE: case 0xAF:
            case 0x90: case 0xF4:
            case 0xF5: case 0xF8: case 0xF9: case 0xFA:
            case 0xFB: case 0xFC: case 0xFD:
            case 0xEC: case 0xED: case 0xEE: case 0xEF:
                return 1;
            case 0x70: case 0x71: case 0x72: case 0x73:
            case 0x74: case 0x75: case 0x76: case 0x77:
            case 0x78: case 0x79: case 0x7A: case 0x7B:
            case 0x7C: case 0x7D: case 0x7E: case 0x7F:
            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
            case 0xEB: case 0xCD:
            case 0x0F: case 0xF2: case 0xF3:
                return 2;
            case 0xA0: case 0xA1: case 0xA2: case 0xA3:
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
            case 0xC2: case 0xC3: case 0xCB:
            case 0xE8: case 0xE9:
                return 3;
            case 0xEA:
                return 5; // JMP FAR ptr16:16: EA offset16 segment16
            case 0xC6: return modrmLen(core, pc) + 1;
            case 0xC7: return modrmLen(core, pc) + 2;
            case 0xC0: case 0xC1: return modrmLen(core, pc) + 1;
            case 0x80: return modrmLen(core, pc) + 1;
            case 0x81: return modrmLen(core, pc) + 2;
            case 0x83: return modrmLen(core, pc) + 1;
            case 0xF6: {
                int r = (core.readMem8(pc + 1) >> 3) & 0x07;
                return modrmLen(core, pc) + ((r == 0 || r == 1) ? 1 : 0);
            }
            case 0xF7: {
                int r = (core.readMem8(pc + 1) >> 3) & 0x07;
                return modrmLen(core, pc) + ((r == 0 || r == 1) ? (op32 ? 4 : 2) : 0);
            }
            case 0xFE: case 0xFF: return modrmLen(core, pc);
            default: return 1;
        }
    }

    private int modrmLen(X86Core core, int pc) {
        return modrmLenAt(core, pc + 1);
    }

    /** Length of a ModR/M operand whose ModR/M byte is at {@code at}. */
    private int modrmLenAt(X86Core core, int at) {
        int modrm = core.readMem8(at);
        int mod = (modrm >> 6) & 0x03;
        int rm = modrm & 0x07;
        if (addr32) {
            if (mod == 0 && rm == 5) return 6; // disp32
            if (mod == 1) return 3;            // disp8
            if (mod == 2) return 6;            // disp32
            return 2;
        }
        if (mod == 0 && rm == 6) return 4;
        if (mod == 1) return 3;
        if (mod == 2) return 4;
        return 2;
    }

    private void executeOpcode(X86Core core, int opcode, int pc) throws Exception {
        switch (opcode) {
            case 0x00: modrm_arith(core, pc, false, false, OP_ADD); break;
            case 0x01: modrm_arith(core, pc, true, false, OP_ADD); break;
            case 0x02: modrm_arith(core, pc, false, true, OP_ADD); break;
            case 0x03: modrm_arith(core, pc, true, true, OP_ADD); break;

            case 0x08: modrm_arith(core, pc, false, false, OP_OR); break;
            case 0x09: modrm_arith(core, pc, true, false, OP_OR); break;
            case 0x0A: modrm_arith(core, pc, false, true, OP_OR); break;
            case 0x0B: modrm_arith(core, pc, true, true, OP_OR); break;

            case 0x20: modrm_arith(core, pc, false, false, OP_AND); break;
            case 0x21: modrm_arith(core, pc, true, false, OP_AND); break;
            case 0x22: modrm_arith(core, pc, false, true, OP_AND); break;
            case 0x23: modrm_arith(core, pc, true, true, OP_AND); break;

            case 0x28: modrm_arith(core, pc, false, false, OP_SUB); break;
            case 0x29: modrm_arith(core, pc, true, false, OP_SUB); break;
            case 0x2A: modrm_arith(core, pc, false, true, OP_SUB); break;
            case 0x2B: modrm_arith(core, pc, true, true, OP_SUB); break;

            case 0x30: modrm_arith(core, pc, false, false, OP_XOR); break;
            case 0x31: modrm_arith(core, pc, true, false, OP_XOR); break;
            case 0x32: modrm_arith(core, pc, false, true, OP_XOR); break;
            case 0x33: modrm_arith(core, pc, true, true, OP_XOR); break;

            case 0x38: modrm_cmp(core, pc, false, false); break;
            case 0x39: modrm_cmp(core, pc, true, false); break;
            case 0x3A: modrm_cmp(core, pc, false, true); break;
            case 0x3B: modrm_cmp(core, pc, true, true); break;

            case 0x04: aluImm(core, pc, false, OP_ADD); break;
            case 0x05: aluImm(core, pc, true, OP_ADD); break;
            case 0x0C: aluImm(core, pc, false, OP_OR); break;
            case 0x0D: aluImm(core, pc, true, OP_OR); break;
            case 0x24: aluImm(core, pc, false, OP_AND); break;
            case 0x25: aluImm(core, pc, true, OP_AND); break;
            case 0x2C: aluImm(core, pc, false, OP_SUB); break;
            case 0x2D: aluImm(core, pc, true, OP_SUB); break;
            case 0x34: aluImm(core, pc, false, OP_XOR); break;
            case 0x35: aluImm(core, pc, true, OP_XOR); break;
            case 0x3C: aluImm(core, pc, false, OP_CMP); break;
            case 0x3D: aluImm(core, pc, true, OP_CMP); break;

            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
                { int r = opcode & 0x07; int old = core.getReg(r);
                  int v = op32 ? old + 1 : ((old & 0xFFFF) + 1) & 0xFFFF;
                  core.setReg(r, op32 ? v : (core.getReg(r) & ~0xFFFF) | v);
                  setFlagsIncDec(core, old, v, !op32, true); } break;
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
                { int r = opcode & 0x07; int old = core.getReg(r);
                  int v = op32 ? old - 1 : ((old & 0xFFFF) - 1) & 0xFFFF;
                  core.setReg(r, op32 ? v : (core.getReg(r) & ~0xFFFF) | v);
                  setFlagsIncDec(core, old, v, !op32, false); } break;
            case 0x50: case 0x51: case 0x52: case 0x53:
            case 0x54: case 0x55: case 0x56: case 0x57:
                core.push32(core.getReg(opcode & 0x07) & 0xFFFF); break;
            case 0x58: case 0x59: case 0x5A: case 0x5B:
            case 0x5C: case 0x5D: case 0x5E: case 0x5F:
                { int r = opcode & 0x07; int v = core.pop32() & 0xFFFF;
                  core.setReg(r, (core.getReg(r) & ~0xFFFF) | v); } break;

            case 0x68: core.push32(op32 ? core.readMem32(pc + 1) : core.readMem16(pc + 1)); break;
            case 0x6A: core.push32((byte) core.readMem8(pc + 1)); break;

            case 0x91: case 0x92: case 0x93: case 0x94:
            case 0x95: case 0x96: case 0x97:
                { int r = opcode & 0x07;
                  int tmp = core.getReg(REG_EAX) & 0xFFFF;
                  core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (core.getReg(r) & 0xFFFF));
                  core.setReg(r, (core.getReg(r) & ~0xFFFF) | tmp); } break;

            case 0x70: case 0x71: case 0x72: case 0x73:
            case 0x74: case 0x75: case 0x76: case 0x77:
            case 0x78: case 0x79: case 0x7A: case 0x7B:
            case 0x7C: case 0x7D: case 0x7E: case 0x7F:
                jcc(core, opcode, pc); break;

            case 0x86: modrm_xchg(core, pc, false); break;
            case 0x87: modrm_xchg(core, pc, true); break;
            case 0x88: modrm_mov_to_rm(core, pc, false); break;
            case 0x89: modrm_mov_to_rm(core, pc, true); break;
            case 0x8A: modrm_mov_to_reg(core, pc, false); break;
            case 0x8B: modrm_mov_to_reg(core, pc, true); break;
            case 0x8C: modrm_mov_sreg_to_rm(core, pc); break;
            case 0x8D: modrm_lea(core, pc); break;
            case 0x8E: modrm_mov_rm_to_sreg(core, pc); break;
            case 0x8F: modrm_pop(core, pc); break;

            case 0x06: core.push32(core.getSegSelector(X86Core.SEG_ES)); break;
            case 0x07: loadSeg(core, X86Core.SEG_ES, core.pop32() & 0xFFFF); break;
            case 0x0E: core.push32(core.getSegSelector(X86Core.SEG_CS)); break;
            case 0x16: core.push32(core.getSegSelector(X86Core.SEG_SS)); break;
            case 0x17: loadSeg(core, X86Core.SEG_SS, core.pop32() & 0xFFFF); break;
            case 0x1E: core.push32(core.getSegSelector(X86Core.SEG_DS)); break;
            case 0x1F: loadSeg(core, X86Core.SEG_DS, core.pop32() & 0xFFFF); break;

            case 0x84: modrm_test(core, pc, false); break;
            case 0x85: modrm_test(core, pc, true); break;

            case 0x90: break;

            case 0x60: pusha(core); break;
            case 0x61: popa(core); break;

            case 0x98: cbw(core); break;
            case 0x99: cwd(core); break;
            case 0x9C: core.push32(core.getFlags() & 0xFFFF); break;
            case 0x9D: core.setFlags(core.pop32() & 0xFFFF); break;
            case 0x9E: core.setFlags((core.getFlags() & ~0xD5) | (getReg8(core, 4) & 0xD5)); break;
            case 0x9F: setReg8(core, 4, (core.getFlags() & 0xD5) | 0x02); break;

            case 0xA0: {
                int addr = dataAddress(core, sourceSegment(),
                        addr32 ? core.readMem32(pc + 1) : core.readMem16(pc + 1));
                core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (core.readMem8(addr) & 0xFF));
            } break;
            case 0xA1: {
                int addr = dataAddress(core, sourceSegment(),
                        addr32 ? core.readMem32(pc + 1) : core.readMem16(pc + 1));
                if (op32) {
                    core.setReg(REG_EAX, core.readMem32(addr));
                } else {
                    core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | core.readMem16(addr));
                }
            } break;
            case 0xA2: {
                int addr = dataAddress(core, sourceSegment(),
                        addr32 ? core.readMem32(pc + 1) : core.readMem16(pc + 1));
                core.writeMem8(addr, core.getReg(REG_EAX) & 0xFF);
            } break;
            case 0xA3: {
                int addr = dataAddress(core, sourceSegment(),
                        addr32 ? core.readMem32(pc + 1) : core.readMem16(pc + 1));
                if (op32) {
                    core.writeMem32(addr, core.getReg(REG_EAX));
                } else {
                    core.writeMem16(addr, core.getReg(REG_EAX));
                }
            } break;

            case 0xA4: movs(core, false); break;
            case 0xA5: movs(core, true); break;
            case 0xA6: cmps(core, false); break;
            case 0xA7: cmps(core, true); break;
            case 0xA8: { int v = core.readMem8(pc + 1) & 0xFF;
                         setFlagsLogic(core, getReg8(core, 0) & v, false); } break;
            case 0xA9: { int v = op32 ? core.readMem32(pc + 1) : core.readMem16(pc + 1);
                         setFlagsLogic(core, core.getReg(REG_EAX) & v, true); } break;
            case 0xAA: stos(core, false); break;
            case 0xAB: stos(core, true); break;
            case 0xAC: lods(core, false); break;
            case 0xAD: lods(core, true); break;
            case 0xAE: scas(core, false); break;
            case 0xAF: scas(core, true); break;

            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
                { int r = opcode & 0x07; int v = core.readMem8(pc + 1) & 0xFF; setReg8(core, r, v); } break;
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                { int r = opcode & 0x07;
                  core.setReg(r, op32 ? core.readMem32(pc + 1) : core.readMem16(pc + 1)); } break;

            case 0xC2: { int popCount = core.readMem16(pc + 1); core.setReg(REG_ESP, core.getReg(REG_ESP) + 2 + popCount); } break;
            case 0xC0: shiftImm(core, pc, false); break;
            case 0xC1: shiftImm(core, pc, true); break;
            case 0xC3: ret_near(core, pc); break;
            case 0xC6: modrm_mov_imm(core, pc, false); break;
            case 0xC7: modrm_mov_imm(core, pc, true); break;
            case 0xCB: ret_near(core, pc); break;
            case 0xCD: {
                int vector = core.readMem8(pc + 1) & 0xFF;
                core.handleInt(vector);
                break;
            }

            case 0xD0: shiftOp(core, pc, false, 1); break;
            case 0xD1: shiftOp(core, pc, true, 1); break;
            case 0xD2: shiftOp(core, pc, false, -1); break;
            case 0xD3: shiftOp(core, pc, true, -1); break;

            case 0xE0: case 0xE1: case 0xE2: case 0xE3:
                loopRel8(core, pc, opcode); break;
            case 0xE4: { int port = core.readMem8(pc + 1) & 0xFF;
                         setReg8(core, 0, core.readPort(port) & 0xFF); } break;
            case 0xE5: { int port = core.readMem8(pc + 1) & 0xFF;
                         int al = core.readPort(port) & 0xFF;
                         int ah = core.readPort(port + 1) & 0xFF;
                         core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (ah << 8) | al); } break;
            case 0xE6: { int port = core.readMem8(pc + 1) & 0xFF;
                         core.writePort(port, (byte) (core.getReg(REG_EAX) & 0xFF)); } break;
            case 0xE7: { int port = core.readMem8(pc + 1) & 0xFF;
                         core.writePort(port, (byte) (core.getReg(REG_EAX) & 0xFF));
                         core.writePort(port + 1, (byte) ((core.getReg(REG_EAX) >> 8) & 0xFF)); } break;

            case 0xE8: call_rel16(core, pc); break;
            case 0xE9: jmp_rel16(core, pc); break;
            case 0xEB: jmp_rel8(core, pc); break;
            case 0xEA: { int offset = core.readMem16(pc + 1); int segment = core.readMem16(pc + 3); core.setPC((segment << 4) | offset); } break;

            case 0xEC: { int dx = core.getReg(REG_EDX) & 0xFFFF; core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (core.readPort(dx) & 0xFF)); } break;
            case 0xED: { int dx = core.getReg(REG_EDX) & 0xFFFF; int al = core.readPort(dx) & 0xFF; int ah = core.readPort(dx + 1) & 0xFF; core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (ah << 8) | al); } break;
            case 0xEE: { int dx = core.getReg(REG_EDX) & 0xFFFF; core.writePort(dx, (byte)(core.getReg(REG_EAX) & 0xFF)); } break;
            case 0xEF: { int dx = core.getReg(REG_EDX) & 0xFFFF; core.writePort(dx, (byte)(core.getReg(REG_EAX) & 0xFF)); core.writePort(dx + 1, (byte)((core.getReg(REG_EAX) >> 8) & 0xFF)); } break;

            case 0x0F: twoByteOpcode(core, pc); break;

            case 0xF2: case 0xF3: repPrefix(core, pc, opcode == 0xF2); break;
            case 0xF5: core.setFlag(FLAG_CF, !core.getFlag(FLAG_CF)); break;
            case 0xF8: core.setFlag(FLAG_CF, false); break;
            case 0xF9: core.setFlag(FLAG_CF, true); break;
            case 0xFA: core.setFlag(FLAG_IF, false); break;
            case 0xFB: core.setFlag(FLAG_IF, true); break;
            case 0xFC: core.setFlag(FLAG_DF, false); break;
            case 0xFD: core.setFlag(FLAG_DF, true); break;

            case 0xF4: break;

            case 0xF6: modrm_grp3(core, pc, false); break;
            case 0xF7: modrm_grp3(core, pc, true); break;

            case 0xFE: modrm_grp4(core, pc); break;
            case 0xFF: modrm_grp5(core, pc); break;

            case 0x80: modrm_imm_arith(core, pc, false, false); break;
            case 0x81: modrm_imm_arith(core, pc, true, false); break;
            case 0x83: modrm_imm_arith(core, pc, true, true); break;
        }
    }

    private static final int OP_ADD = 0, OP_OR = 1, OP_ADC = 2, OP_SBB = 3,
                             OP_AND = 4, OP_SUB = 5, OP_XOR = 6, OP_CMP = 7;

    /** Value mask for the current operand size (8, 16 or 32 bits). */
    private int opMask(boolean word) {
        if (!word) return 0xFF;
        return op32 ? 0xFFFFFFFF : 0xFFFF;
    }

    /** Sign-bit mask for the current operand size. */
    private int opSign(boolean word) {
        if (!word) return 0x80;
        return op32 ? 0x80000000 : 0x8000;
    }

    /** Number of bits for the current operand size. */
    private int opBits(boolean word) {
        if (!word) return 8;
        return op32 ? 32 : 16;
    }

    private void modrm_arith(X86Core core, int pc, boolean word, boolean dirToReg, int kind) {
        ModRm m = decodeModRm(core, pc, word);
        int destVal = dirToReg ? m.regVal : m.rmVal;
        int srcVal = dirToReg ? m.rmVal : m.regVal;
        int mask = opMask(word);
        if (kind == OP_CMP) {
            setFlagsSub(core, destVal, srcVal, destVal - srcVal, word);
            return;
        }
        int r = binOp(kind, destVal, srcVal) & mask;
        switch (kind) {
            case OP_ADC: case OP_SBB:
            case OP_ADD: setFlagsAdd(core, destVal, srcVal, r, word); break;
            case OP_SUB: setFlagsSub(core, destVal, srcVal, r, word); break;
            default: setFlagsLogic(core, r, word); break;
        }
        writeResult(core, m, dirToReg, r, word);
    }

    private int binOp(int kind, int a, int b) {
        switch (kind) {
            case OP_OR: return a | b;
            case OP_ADC: return a + b;
            case OP_SBB: return a - b;
            case OP_AND: return a & b;
            case OP_SUB: case OP_CMP: return a - b;
            case OP_XOR: return a ^ b;
            default: return a + b;
        }
    }

    /** Zero-extended value for the current operand size. */
    private long unsignedOf(int value, boolean word) {
        if (!word) return value & 0xFFL;
        return op32 ? (value & 0xFFFFFFFFL) : (value & 0xFFFFL);
    }

    /** Set flags for an ADD/ADC result. */
    private void setFlagsAdd(X86Core core, int a, int b, int result, boolean word) {
        int mask = opMask(word);
        int sign = opSign(word);
        long max = unsignedOf(-1, word);
        long sum = unsignedOf(a, word) + unsignedOf(b, word);
        result &= mask;
        core.setFlag(FLAG_CF, sum > max);
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & sign) != 0);
        core.setFlag(FLAG_PF, (Integer.bitCount(result & 0xFF) & 1) == 0);
        boolean overflow = ((a ^ result) & (b ^ result) & sign) != 0;
        core.setFlag(FLAG_OF, overflow);
    }

    /** Set flags for a SUB/CMP result. */
    private void setFlagsSub(X86Core core, int a, int b, int result, boolean word) {
        int mask = opMask(word);
        int sign = opSign(word);
        result &= mask;
        core.setFlag(FLAG_CF, unsignedOf(a, word) < unsignedOf(b, word));
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & sign) != 0);
        core.setFlag(FLAG_PF, (Integer.bitCount(result & 0xFF) & 1) == 0);
        boolean overflow = ((a ^ b) & (a ^ result) & sign) != 0;
        core.setFlag(FLAG_OF, overflow);
    }

    /** Set flags for AND/OR/XOR/TEST results (CF and OF are cleared). */
    private void setFlagsLogic(X86Core core, int result, boolean word) {
        int mask = opMask(word);
        int sign = opSign(word);
        result &= mask;
        core.setFlag(FLAG_CF, false);
        core.setFlag(FLAG_OF, false);
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & sign) != 0);
        core.setFlag(FLAG_PF, (Integer.bitCount(result & 0xFF) & 1) == 0);
    }

    /** Set flags after a modulo operation such as MUL/DIV (only ZF/SF/PF defined). */
    private void setFlagsResult(X86Core core, int result, boolean word) {
        setFlagsLogic(core, result, word);
    }

    private void modrm_cmp(X86Core core, int pc, boolean word, boolean dirToReg) {
        ModRm m = decodeModRm(core, pc, word);
        int destVal = dirToReg ? m.regVal : m.rmVal;
        int srcVal = dirToReg ? m.rmVal : m.regVal;
        setFlagsSub(core, destVal, srcVal, destVal - srcVal, word);
    }

    private void modrm_mov_to_rm(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc, word);
        writeVal(core, m, false, m.regVal, word);
    }

    private void modrm_mov_to_reg(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc, word);
        if (word) {
            if (op32) {
                core.setReg(REG_EAX + m.regField, m.rmVal);
            } else {
                core.setReg(REG_EAX + m.regField, m.rmVal & 0xFFFF);
            }
        } else {
            setReg8(core, m.regField, m.rmVal & 0xFF);
        }
    }

    private void modrm_mov_imm(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc, word);
        int imm;
        if (word && op32) {
            imm = core.readMem32(pc + m.len);
        } else {
            imm = word ? core.readMem16(pc + m.len) : (core.readMem8(pc + m.len) & 0xFF);
        }
        writeVal(core, m, false, imm, word);
    }

    private void modrm_xchg(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc, word);
        int tmp = m.regVal;
        if (word) {
            if (op32) {
                core.setReg(REG_EAX + m.regField, m.rmVal);
            } else {
                core.setReg(REG_EAX + m.regField, m.rmVal & 0xFFFF);
            }
        } else {
            setReg8(core, m.regField, m.rmVal & 0xFF);
        }
        writeVal(core, m, false, tmp, word);
    }

    private void modrm_ignore(X86Core core, int pc, boolean word) {
        decodeModRm(core, pc, word);
    }

    /** MOV r/m16, Sreg (0x8C). */
    private void modrm_mov_sreg_to_rm(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc, true);
        int sel = (m.regField < X86Core.SEG_COUNT) ? core.getSegSelector(m.regField) : 0;
        writeVal(core, m, false, sel, true);
    }

    /** MOV Sreg, r/m16 (0x8E). */
    private void modrm_mov_rm_to_sreg(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc, true);
        if (m.regField < X86Core.SEG_COUNT) {
            loadSeg(core, m.regField, m.rmVal & 0xFFFF);
        }
    }

    /**
     * Load a segment register, updating its cached linear base.  While the
     * CPU is in protected mode the base comes from the GDT descriptor; back in
     * real mode the base is simply {@code selector << 4}.  The cached base is
     * retained when leaving protected mode, which supports unreal mode.
     */
    private void loadSeg(X86Core core, int seg, int selector) {
        selector &= 0xFFFF;
        core.setSegSelector(seg, selector);
        int base;
        if (core.isProtectedMode() && selector != 0) {
            int desc = core.getGdtBase() + (selector & 0xFFF8);
            int b2 = core.readMem8(desc + 2);
            int b3 = core.readMem8(desc + 3);
            int b4 = core.readMem8(desc + 4);
            int b7 = core.readMem8(desc + 7);
            base = b2 | (b3 << 8) | (b4 << 16) | (b7 << 24);
        } else {
            base = selector << 4;
        }
        core.setSegBase(seg, base);
    }

    private void modrm_imm_arith(X86Core core, int pc, boolean word, boolean signedImm) {
        ModRm m = decodeModRm(core, pc, word);
        int regField = m.regField;
        int imm;
        if (signedImm) {
            imm = (byte)core.readMem8(pc + m.len);
        } else if (word && op32) {
            imm = core.readMem32(pc + m.len); // 32-bit immediate, truncated to 16 bits
        } else {
            imm = word ? core.readMem16(pc + m.len) : (core.readMem8(pc + m.len) & 0xFF);
        }
        int mask = opMask(word);
        if (regField == OP_CMP) {
            setFlagsSub(core, m.rmVal, imm, m.rmVal - imm, word);
            return;
        }
        int result = binOp(regField, m.rmVal, imm) & mask;
        switch (regField) {
            case OP_ADD: case OP_ADC: setFlagsAdd(core, m.rmVal, imm, result, word); break;
            case OP_SUB: case OP_SBB: setFlagsSub(core, m.rmVal, imm, result, word); break;
            default: setFlagsLogic(core, result, word); break;
        }
        writeVal(core, m, false, result, word);
    }

    private void modrm_grp3(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc, word);
        int mask = opMask(word);
        switch (m.regField) {
            case 0: case 1: { // TEST r/m, imm
                int imm;
                if (word && op32) {
                    imm = core.readMem32(pc + m.len);
                } else {
                    imm = word ? core.readMem16(pc + m.len)
                               : (core.readMem8(pc + m.len) & 0xFF);
                }
                setFlagsLogic(core, (m.rmVal & imm) & mask, word);
                break;
            }
            case 2: // NOT
                writeVal(core, m, false, ~m.rmVal, word);
                break;
            case 3: { // NEG
                int v = (-m.rmVal) & mask;
                setFlagsSub(core, 0, m.rmVal, v, word);
                writeVal(core, m, false, v, word);
                break;
            }
            case 4: case 5: { // MUL / IMUL (one-operand form)
                long a = word ? unsignedOf(core.getReg(REG_EAX), word)
                              : (core.getReg(REG_EAX) & 0xFFL);
                long b = word ? unsignedOf(m.rmVal, word)
                              : (m.rmVal & 0xFFL);
                long product = a * b;
                long max = unsignedOf(-1, word);
                if (word && op32) {
                    core.setReg(REG_EAX, (int) product);
                    core.setReg(REG_EDX, (int) (product >>> 32));
                } else if (word) {
                    core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (int) (product & 0xFFFF));
                    core.setReg(REG_EDX, (core.getReg(REG_EDX) & ~0xFFFF) | (int) ((product >>> 16) & 0xFFFF));
                } else {
                    core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (int) (product & 0xFFFF));
                }
                core.setFlag(FLAG_CF, product > max);
                core.setFlag(FLAG_OF, product > max);
                break;
            }
            default: { // DIV / IDIV
                if (m.rmVal == 0) {
                    core.setFlag(FLAG_CF, true);
                    break;
                }
                long dividend;
                if (!word) {
                    dividend = core.getReg(REG_EAX) & 0xFFFF;
                } else if (op32) {
                    dividend = (unsignedOf(core.getReg(REG_EDX), word) << 32)
                             | unsignedOf(core.getReg(REG_EAX), word);
                } else {
                    dividend = ((core.getReg(REG_EDX) & 0xFFFFL) << 16) | (core.getReg(REG_EAX) & 0xFFFFL);
                }
                long divisor = unsignedOf(m.rmVal, word);
                if (divisor == 0) {
                    core.setFlag(FLAG_CF, true);
                    break;
                }
                long quot = dividend / divisor;
                long rem = dividend % divisor;
                if (word && op32) {
                    core.setReg(REG_EAX, (int) quot);
                    core.setReg(REG_EDX, (int) rem);
                } else if (word) {
                    core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (int) (quot & 0xFFFF));
                    core.setReg(REG_EDX, (core.getReg(REG_EDX) & ~0xFFFF) | (int) (rem & 0xFFFF));
                } else {
                    core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (int) (quot & 0xFF));
                    core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF00) | ((int) (rem & 0xFF) << 8));
                }
                core.setFlag(FLAG_CF, false);
                break;
            }
        }
    }


    private void modrm_grp4(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc, false);
        int r = m.regField;
        if (r == 0) {
            int v = (m.rmVal + 1) & 0xFF;
            setFlagsIncDec(core, m.rmVal, v, false, true);
            writeVal(core, m, false, v, false);
        } else if (r == 1) {
            int v = (m.rmVal - 1) & 0xFF;
            setFlagsIncDec(core, m.rmVal, v, false, false);
            writeVal(core, m, false, v, false);
        }
    }

    private void modrm_grp5(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc, true);
        int r = m.regField;
        switch (r) {
            case 0: {
                int v = m.rmVal + 1;
                setFlagsIncDec(core, m.rmVal, v, true, true);
                writeVal(core, m, false, v, true);
                break;
            }
            case 1: {
                int v = m.rmVal - 1;
                setFlagsIncDec(core, m.rmVal, v, true, false);
                writeVal(core, m, false, v, true);
                break;
            }
            case 2:
                core.push32(m.rmVal);
                break;
            case 3:
                core.setReg(REG_EAX + m.rmField, core.pop32());
                break;
            case 4:
                core.push32(core.getPC() + m.len);
                core.setPC(m.effAddr);
                break;
            case 5:
                core.setPC(m.effAddr);
                break;
            case 6:
                core.push32(core.getPC() + m.len);
                core.setPC(m.effAddr);
                break;
        }
    }

    private void jcc(X86Core core, int opcode, int pc) {
        int rel = (byte)core.readMem8(pc + 1);
        boolean take = false;
        switch (opcode) {
            case 0x70: take = core.getFlag(FLAG_OF); break;
            case 0x71: take = !core.getFlag(FLAG_OF); break;
            case 0x72: take = core.getFlag(FLAG_CF); break;
            case 0x73: take = !core.getFlag(FLAG_CF); break;
            case 0x74: take = core.getFlag(FLAG_ZF); break;
            case 0x75: take = !core.getFlag(FLAG_ZF); break;
            case 0x76: take = core.getFlag(FLAG_CF) || core.getFlag(FLAG_ZF); break;
            case 0x77: take = !core.getFlag(FLAG_CF) && !core.getFlag(FLAG_ZF); break;
            case 0x78: take = core.getFlag(FLAG_SF); break;
            case 0x79: take = !core.getFlag(FLAG_SF); break;
            case 0x7A: take = core.getFlag(FLAG_PF); break;
            case 0x7B: take = !core.getFlag(FLAG_PF); break;
            case 0x7C: take = core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF); break;
            case 0x7D: take = core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF); break;
            case 0x7E: take = core.getFlag(FLAG_ZF) || (core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF)); break;
            case 0x7F: take = !core.getFlag(FLAG_ZF) && (core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF)); break;
        }
        if (take) {
            core.setPC(pc + 2 + rel);
        }
    }

    private void ret_near(X86Core core, int pc) {
        int newPC = core.pop32();
        core.setPC(newPC);
    }

    private void call_rel16(X86Core core, int pc) {
        int rel = (short) core.readMem16(pc + 1); // sign-extended 16-bit displacement
        core.push32(pc + 3);
        core.setPC(pc + 3 + rel);
    }

    private void jmp_rel16(X86Core core, int pc) {
        int rel = (short) core.readMem16(pc + 1); // sign-extended 16-bit displacement
        core.setPC(pc + 3 + rel);
    }

    private void jmp_rel8(X86Core core, int pc) {
        int rel = (byte)core.readMem8(pc + 1);
        core.setPC(pc + 2 + rel);
    }

    private ModRm decodeModRm(X86Core core, int pc, boolean word) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 0x03;
        int regField = (modrm >> 3) & 0x07;
        int rmField = modrm & 0x07;
        int dispSize = 0;
        int disp = 0;
        int sib = -1;
        if (addr32) {
            if (mod == 1) {
                dispSize = 1;
                disp = (byte)core.readMem8(pc + 2);
            } else if (mod == 2) {
                dispSize = 4;
                disp = core.readMem32(pc + 2);
            } else if (mod == 0 && rmField == 5) {
                dispSize = 4;
                disp = core.readMem32(pc + 2);
            }
            if (rmField == 4) {
                sib = core.readMem8(pc + 2 + dispSize);
            }
        } else {
            if (mod == 1) {
                dispSize = 1;
                disp = (byte)core.readMem8(pc + 2);
            } else if (mod == 2) {
                dispSize = 2;
                disp = core.readMem16(pc + 2);
            } else if (mod == 0 && rmField == 6) {
                dispSize = 2;
                disp = core.readMem16(pc + 2);
            }
        }
        int addr = 0;
        int offset = 0;
        int defaultSeg = X86Core.SEG_DS;
        if (mod != 3) {
            if (addr32) {
                offset = modrm_addr32(core, rmField, sib, disp, mod);
                defaultSeg = defaultSeg32(rmField, sib, mod);
                addr = core.segmentAddress32(segOverride >= 0 ? segOverride : defaultSeg, offset);
            } else {
                offset = modrm_addr(core, rmField, disp, mod);
                defaultSeg = defaultSeg16(rmField, mod);
                addr = core.segmentAddress(segOverride >= 0 ? segOverride : defaultSeg, offset);
            }
        }
        int rmVal;
        if (mod == 3) {
            rmVal = word ? (op32 ? core.getReg(REG_EAX + rmField)
                                  : (core.getReg(REG_EAX + rmField) & 0xFFFF))
                         : getReg8(core, rmField);
        } else if (word && op32) {
            rmVal = core.readMem32(addr);
        } else {
            rmVal = word ? (core.readMem16(addr) & 0xFFFF)
                         : (core.readMem8(addr) & 0xFF);
        }
        int regVal = word ? (op32 ? core.getReg(REG_EAX + regField)
                                  : (core.getReg(REG_EAX + regField) & 0xFFFF))
                          : getReg8(core, regField);
        ModRm m = new ModRm();
        m.mod = mod; m.regField = regField; m.rmField = rmField;
        m.dispSize = dispSize; m.disp = disp; m.effAddr = addr;
        m.offset = offset;
        m.rmVal = rmVal; m.regVal = regVal;
        m.len = 2 + dispSize + (sib >= 0 ? 1 : 0);
        return m;
    }

    /** Default segment for a 16-bit ModR/M memory operand. */
    private int defaultSeg16(int rm, int mod) {
        if (rm == 2 || rm == 3 || (rm == 6 && mod != 0)) return X86Core.SEG_SS;
        return X86Core.SEG_DS;
    }

    /** Default segment for a 32-bit ModR/M memory operand. */
    private int defaultSeg32(int rm, int sib, int mod) {
        int base = rm;
        if (rm == 4 && sib >= 0) base = sib & 0x07;
        if (base == 4) return X86Core.SEG_SS;                       // ESP
        if (base == 5 && !(mod == 0 && rm == 5)) return X86Core.SEG_SS; // EBP
        return X86Core.SEG_DS;
    }

    /**
     * Read an 8-bit register using the x86 encoding: AL, CL, DL, BL for
     * indexes 0-3 and AH, CH, DH, BH for indexes 4-7.
     */
    private int getReg8(X86Core core, int r) {
        int v = core.getReg(REG_EAX + (r & 0x03));
        return (r < 4) ? (v & 0xFF) : ((v >> 8) & 0xFF);
    }

    /** Write an 8-bit register using the x86 encoding (see {@link #getReg8}). */
    private void setReg8(X86Core core, int r, int value) {
        int reg = REG_EAX + (r & 0x03);
        int v = core.getReg(reg);
        if (r < 4) {
            v = (v & ~0xFF) | (value & 0xFF);
        } else {
            v = (v & ~0xFF00) | ((value & 0xFF) << 8);
        }
        core.setReg(reg, v);
    }

    private void writeVal(X86Core core, ModRm m, boolean noReg, int val, boolean word) {
        if (m.mod == 3) {
            if (word) {
                if (op32) {
                    core.setReg(REG_EAX + m.rmField, val);
                } else {
                    core.setReg(REG_EAX + m.rmField, val & 0xFFFF);
                }
            } else {
                setReg8(core, m.rmField, val & 0xFF);
            }
        } else {
            if (word) {
                if (op32) {
                    core.writeMem32(m.effAddr, val);
                } else {
                    core.writeMem16(m.effAddr, val);
                }
            } else {
                core.writeMem8(m.effAddr, val & 0xFF);
            }
        }
    }

    private void writeResult(X86Core core, ModRm m, boolean dirToReg, int result, boolean word) {
        if (dirToReg) {
            if (word) {
                if (op32) {
                    core.setReg(REG_EAX + m.regField, result);
                } else {
                    core.setReg(REG_EAX + m.regField, result & 0xFFFF);
                }
            } else {
                setReg8(core, m.regField, result & 0xFF);
            }
        } else {
            writeVal(core, m, false, result, word);
        }
    }

    /* ---------------- additional instruction helpers ---------------- */

    private void setFlagsIncDec(X86Core core, int old, int result, boolean word, boolean inc) {
        int mask = opMask(word);
        int sign = opSign(word);
        result &= mask;
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & sign) != 0);
        core.setFlag(FLAG_PF, (Integer.bitCount(result & 0xFF) & 1) == 0);
        boolean overflow = inc ? ((old & sign) != 0 && (result & sign) == 0)
                               : ((old & sign) == 0 && (result & sign) != 0);
        core.setFlag(FLAG_OF, overflow);
    }

    /** AL/AX immediate arithmetic (opcodes 0x04/0x05, 0x0C/0x0D, ...). */
    private void aluImm(X86Core core, int pc, boolean word, int kind) {
        int imm;
        if (word && op32) {
            imm = core.readMem32(pc + 1);
        } else {
            imm = word ? core.readMem16(pc + 1) : (core.readMem8(pc + 1) & 0xFF);
        }
        int a = word ? (op32 ? core.getReg(REG_EAX) : core.getReg(REG_EAX) & 0xFFFF)
                     : getReg8(core, 0);
        int mask = opMask(word);
        if (kind == OP_CMP) {
            setFlagsSub(core, a, imm, a - imm, word);
            return;
        }
        int r = binOp(kind, a, imm) & mask;
        switch (kind) {
            case OP_ADD: case OP_ADC: setFlagsAdd(core, a, imm, r, word); break;
            case OP_SUB: case OP_SBB: setFlagsSub(core, a, imm, r, word); break;
            default: setFlagsLogic(core, r, word); break;
        }
        if (word) {
            if (op32) {
                core.setReg(REG_EAX, r);
            } else {
                core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | r);
            }
        } else {
            setReg8(core, 0, r);
        }
    }

    private void modrm_test(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc, word);
        int mask = word ? 0xFFFF : 0xFF;
        setFlagsLogic(core, (m.rmVal & m.regVal) & mask, word);
    }

    private void modrm_lea(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc, true);
        if (m.mod == 3) {
            return;
        }
        int r = REG_EAX + m.regField;
        int value = addr32 ? m.offset : (m.offset & 0xFFFF);
        if (op32) {
            core.setReg(r, value);
        } else {
            core.setReg(r, (core.getReg(r) & ~0xFFFF) | (value & 0xFFFF));
        }
    }

    private void modrm_pop(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc, true);
        int v = core.pop32();
        if (m.mod == 3) {
            int r = REG_EAX + m.rmField;
            if (op32) {
                core.setReg(r, v);
            } else {
                core.setReg(r, (core.getReg(r) & ~0xFFFF) | (v & 0xFFFF));
            }
        } else {
            if (op32) {
                core.writeMem32(m.effAddr, v);
            } else {
                core.writeMem16(m.effAddr, v & 0xFFFF);
            }
        }
    }

    private void pusha(X86Core core) {
        int sp = core.getReg(REG_ESP);
        for (int r = 0; r < 8; r++) {
            int v = (r == 4) ? sp : core.getReg(r); // PUSHA saves the original SP
            core.push32(v & 0xFFFF);
        }
    }

    private void popa(X86Core core) {
        for (int r = 7; r >= 0; r--) {
            int v = core.pop32() & 0xFFFF;
            if (r == 4) {
                continue; // the saved SP value is discarded
            }
            core.setReg(r, (core.getReg(r) & ~0xFFFF) | v);
        }
    }

    private void cbw(X86Core core) {
        int al = core.getReg(REG_EAX) & 0xFF;
        setReg8(core, 4, (al & 0x80) != 0 ? 0xFF : 0x00);
    }

    private void cwd(X86Core core) {
        int ax = core.getReg(REG_EAX) & 0xFFFF;
        int dx = (ax & 0x8000) != 0 ? 0xFFFF : 0x0000;
        core.setReg(REG_EDX, (core.getReg(REG_EDX) & ~0xFFFF) | dx);
    }

    private int stringStep(X86Core core, boolean word) {
        int size = op32 ? 4 : (word ? 2 : 1);
        return (core.getFlag(FLAG_DF) ? -1 : 1) * size;
    }

    private void addReg16(X86Core core, int reg, int delta) {
        int v = ((core.getReg(reg) & 0xFFFF) + delta) & 0xFFFF;
        core.setReg(reg, (core.getReg(reg) & ~0xFFFF) | v);
    }

    private int indexValue(X86Core core, int reg) {
        return addr32 ? core.getReg(reg) : (core.getReg(reg) & 0xFFFF);
    }

    private void addIndex(X86Core core, int reg, int delta) {
        if (addr32) {
            core.setReg(reg, core.getReg(reg) + delta);
        } else {
            addReg16(core, reg, delta);
        }
    }

    private int dataAddress(X86Core core, int seg, int offset) {
        return addr32 ? core.segmentAddress32(seg, offset)
                      : core.segmentAddress(seg, offset);
    }

    private int sourceSegment() {
        return segOverride >= 0 ? segOverride : X86Core.SEG_DS;
    }

    private int readOperand(X86Core core, int addr, boolean word) {
        if (op32) return core.readMem32(addr);
        return word ? core.readMem16(addr) : (core.readMem8(addr) & 0xFF);
    }

    private void writeOperand(X86Core core, int addr, int value, boolean word) {
        if (op32) {
            core.writeMem32(addr, value);
        } else if (word) {
            core.writeMem16(addr, value);
        } else {
            core.writeMem8(addr, value & 0xFF);
        }
    }

    private void movs(X86Core core, boolean word) {
        int si = dataAddress(core, sourceSegment(), indexValue(core, REG_ESI));
        int di = dataAddress(core, X86Core.SEG_ES, indexValue(core, REG_EDI));
        writeOperand(core, di, readOperand(core, si, word), word);
        int step = stringStep(core, word);
        addIndex(core, REG_ESI, step);
        addIndex(core, REG_EDI, step);
    }

    private void stos(X86Core core, boolean word) {
        int di = dataAddress(core, X86Core.SEG_ES, indexValue(core, REG_EDI));
        int value = op32 ? core.getReg(REG_EAX) : (word ? core.getReg(REG_EAX) & 0xFFFF
                                                        : core.getReg(REG_EAX) & 0xFF);
        writeOperand(core, di, value, word);
        addIndex(core, REG_EDI, stringStep(core, word));
    }

    private void lods(X86Core core, boolean word) {
        int si = dataAddress(core, sourceSegment(), indexValue(core, REG_ESI));
        int v = readOperand(core, si, word);
        if (op32) {
            core.setReg(REG_EAX, v);
        } else if (word) {
            core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | v);
        } else {
            setReg8(core, 0, v & 0xFF);
        }
        addIndex(core, REG_ESI, stringStep(core, word));
    }

    private void cmps(X86Core core, boolean word) {
        int si = dataAddress(core, sourceSegment(), indexValue(core, REG_ESI));
        int di = dataAddress(core, X86Core.SEG_ES, indexValue(core, REG_EDI));
        int a = readOperand(core, si, word);
        int b = readOperand(core, di, word);
        setFlagsSub(core, a, b, a - b, word);
        int step = stringStep(core, word);
        addIndex(core, REG_ESI, step);
        addIndex(core, REG_EDI, step);
    }

    private void scas(X86Core core, boolean word) {
        int di = dataAddress(core, X86Core.SEG_ES, indexValue(core, REG_EDI));
        int a = op32 ? core.getReg(REG_EAX) : (word ? core.getReg(REG_EAX) & 0xFFFF
                                                    : core.getReg(REG_EAX) & 0xFF);
        int b = readOperand(core, di, word);
        setFlagsSub(core, a, b, a - b, word);
        addIndex(core, REG_EDI, stringStep(core, word));
    }

    /**
     * Handle the REP/REPNE prefixes.  Repeats the following string instruction
     * CX times (or until ZF matches for CMPS/SCAS); any other following
     * instruction simply executes once without the prefix.
     */
    private void repPrefix(X86Core core, int pc, boolean repne) {
        int op = core.readMem8(pc + 1) & 0xFF;
        boolean compares = (op == 0xA6 || op == 0xA7 || op == 0xAE || op == 0xAF);

        if (op != 0xA4 && op != 0xA5 && op != 0xAA && op != 0xAB
                && op != 0xAC && op != 0xAD && !compares) {
            core.setPC(pc + 1); // prefix alone: run the next instruction
            return;
        }

        int cx = core.getReg(REG_ECX) & 0xFFFF;
        while (cx != 0) {
            switch (op) {
                case 0xA4: movs(core, false); break;
                case 0xA5: movs(core, true); break;
                case 0xA6: cmps(core, false); break;
                case 0xA7: cmps(core, true); break;
                case 0xAA: stos(core, false); break;
                case 0xAB: stos(core, true); break;
                case 0xAC: lods(core, false); break;
                case 0xAD: lods(core, true); break;
                case 0xAE: scas(core, false); break;
                case 0xAF: scas(core, true); break;
                default: break;
            }
            cx--;
            core.setReg(REG_ECX, (core.getReg(REG_ECX) & ~0xFFFF) | cx);
            if (compares) {
                boolean zf = core.getFlag(FLAG_ZF);
                if (repne ? zf : !zf) {
                    break;
                }
            }
        }
        core.setPC(pc + 2);
    }

    private void loopRel8(X86Core core, int pc, int opcode) {
        int rel = (byte) core.readMem8(pc + 1);
        int cx = core.getReg(REG_ECX) & 0xFFFF;
        boolean take;
        switch (opcode) {
            case 0xE0: // LOOPNE
                cx = (cx - 1) & 0xFFFF;
                core.setReg(REG_ECX, (core.getReg(REG_ECX) & ~0xFFFF) | cx);
                take = cx != 0 && !core.getFlag(FLAG_ZF);
                break;
            case 0xE1: // LOOPE
                cx = (cx - 1) & 0xFFFF;
                core.setReg(REG_ECX, (core.getReg(REG_ECX) & ~0xFFFF) | cx);
                take = cx != 0 && core.getFlag(FLAG_ZF);
                break;
            case 0xE2: // LOOP
                cx = (cx - 1) & 0xFFFF;
                core.setReg(REG_ECX, (core.getReg(REG_ECX) & ~0xFFFF) | cx);
                take = cx != 0;
                break;
            default: // 0xE3 JCXZ
                take = cx == 0;
                break;
        }
        if (take) {
            core.setPC(pc + 2 + rel);
        }
    }

    private void shiftOp(X86Core core, int pc, boolean word, int count) {
        ModRm m = decodeModRm(core, pc, word);
        int n = count < 0 ? (core.getReg(REG_ECX) & 0xFF) : count;
        doShift(core, m, n, word);
    }

    private void shiftImm(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc, word);
        int n = core.readMem8(pc + m.len) & 0xFF;
        doShift(core, m, n, word);
    }

    private void doShift(X86Core core, ModRm m, int n, boolean word) {
        int mask = opMask(word);
        int bits = opBits(word);
        int value = m.rmVal & mask;
        n &= 31;
        if (n == 0) {
            writeVal(core, m, false, value, word);
            return;
        }
        int result;
        switch (m.regField) {
            case 0: // ROL
                n %= bits;
                result = n == 0 ? value : ((value << n) | (value >>> (bits - n))) & mask;
                core.setFlag(FLAG_CF, (result & 1) != 0);
                break;
            case 1: // ROR
                n %= bits;
                result = n == 0 ? value : ((value >>> n) | (value << (bits - n))) & mask;
                core.setFlag(FLAG_CF, (result & (1 << (bits - 1))) != 0);
                break;
            case 5: // SHR
                core.setFlag(FLAG_CF, ((value >>> (n - 1)) & 1) != 0);
                result = (value >>> n) & mask;
                break;
            case 7: { // SAR
                int sv = bits == 32 ? value
                       : (bits == 16 ? (short) value : (byte) value);
                core.setFlag(FLAG_CF, ((sv >> (n - 1)) & 1) != 0);
                result = (sv >> n) & mask;
                break;
            }
            default: // RCL/RCR approximated, SHL/SAL
                core.setFlag(FLAG_CF, ((value >>> (bits - 1)) & 1) != 0);
                result = (value << n) & mask;
                break;
        }
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & (1 << (bits - 1))) != 0);
        core.setFlag(FLAG_PF, (Integer.bitCount(result & 0xFF) & 1) == 0);
        core.setFlag(FLAG_OF, false);
        writeVal(core, m, false, result, word);
    }

    /**
     * Handle 0x0F two-byte opcodes.  Besides the usual 386 system
     * instructions, the CR0 access and LGDT instructions are modelled so
     * that real-mode boot loaders using the "unreal mode" trick (temporarily
     * entering protected mode to load a flat segment descriptor, then
     * returning to real mode) behave as intended.
     */
    private void twoByteOpcode(X86Core core, int pc) {
        int op = core.readMem8(pc + 1) & 0xFF;
        switch (op) {
            case 0x01: { // SGDT/SIDT/LGDT/LIDT/SMSW/LMSW
                int modrm = core.readMem8(pc + 2);
                int mod = (modrm >> 6) & 0x03;
                int reg = (modrm >> 3) & 0x07;
                int rm = modrm & 0x07;
                if ((reg == 2 || reg == 3) && mod == 0 && rm == 6) {
                    int operand = core.readMem16(pc + 3);
                    int addr = core.segmentAddress(X86Core.SEG_DS, operand);
                    if (reg == 2) {
                        int limit = core.readMem16(addr);
                        int base = core.readMem32(addr + 2);
                        core.setGdt(base, limit);
                    }
                }
                core.setPC(pc + 1 + modrmLenAt(core, pc + 2));
                break;
            }
            case 0x20: { // MOV r32, CRn
                int modrm = core.readMem8(pc + 2);
                int cr = (modrm >> 3) & 0x07;
                int r = modrm & 0x07;
                int value = (cr == 0 && core.isProtectedMode()) ? 1 : 0;
                if (op32) {
                    core.setReg(r, value);
                } else {
                    core.setReg(r, (core.getReg(r) & ~0xFFFF) | (value & 0xFFFF));
                }
                core.setPC(pc + 3);
                break;
            }
            case 0x22: { // MOV CRn, r32
                int modrm = core.readMem8(pc + 2);
                int cr = (modrm >> 3) & 0x07;
                int r = modrm & 0x07;
                if (cr == 0) {
                    core.setProtectedMode((core.getReg(r) & 1) != 0);
                }
                core.setPC(pc + 3);
                break;
            }
            case 0x21: case 0x23: // MOV to/from DRn
                core.setPC(pc + 3);
                break;
            case 0xA0: core.push32(core.getSegSelector(X86Core.SEG_FS)); core.setPC(pc + 2); break;
            case 0xA1: loadSeg(core, X86Core.SEG_FS, core.pop32() & 0xFFFF); core.setPC(pc + 2); break;
            case 0xA8: core.push32(core.getSegSelector(X86Core.SEG_GS)); core.setPC(pc + 2); break;
            case 0xA9: loadSeg(core, X86Core.SEG_GS, core.pop32() & 0xFFFF); core.setPC(pc + 2); break;
            case 0x31: // RDTSC
                core.setReg(REG_EAX, (int) (System.nanoTime() & 0xFFFFFFFFL));
                core.setReg(REG_EDX, (int) ((System.nanoTime() >>> 32) & 0xFFFFFFFFL));
                core.setPC(pc + 2);
                break;
            default:
                core.setPC(pc + 2);
                break;
        }
    }

    private int modrm_addr(X86Core core, int rm, int disp, int mod) {
        if (mod == 0 && rm == 6) return disp;
        switch (rm) {
            case 0: return core.getReg(REG_EBX) + core.getReg(REG_ESI) + disp;
            case 1: return core.getReg(REG_EBX) + core.getReg(REG_EDI) + disp;
            case 2: return core.getReg(REG_EBP) + core.getReg(REG_ESI) + disp;
            case 3: return core.getReg(REG_EBP) + core.getReg(REG_EDI) + disp;
            case 4: return core.getReg(REG_ESI) + disp;
            case 5: return core.getReg(REG_EDI) + disp;
            case 6: return (mod == 0) ? disp : (core.getReg(REG_EBP) + disp);
            case 7: return core.getReg(REG_EBX) + disp;
        }
        return disp;
    }

    /** Effective offset for a 32-bit ModR/M memory operand. */
    private int modrm_addr32(X86Core core, int rm, int sib, int disp, int mod) {
        if (rm != 4) {
            if (mod == 0 && rm == 5) return disp; // [disp32]
            return core.getReg(REG_EAX + rm) + disp;
        }
        int scale = (sib >> 6) & 0x03;
        int index = (sib >> 3) & 0x07;
        int base = sib & 0x07;
        int addr = disp;
        if (!(mod == 0 && base == 5)) {
            addr += core.getReg(REG_EAX + base);
        }
        if (index != 4) {
            addr += core.getReg(REG_EAX + index) << scale;
        }
        return addr;
    }

    private static class ModRm {
        int mod, regField, rmField, dispSize, disp, effAddr, offset;
        int rmVal, regVal, len;
    }
}
