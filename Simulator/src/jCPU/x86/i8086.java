package jCPU.x86;

import static jCPU.x86.X86Core.*;

public class i8086 implements X86Core.iExecutor {
    private String lastDecode = "";

    @Override
    public int step(X86Core core) throws Exception {
        int pc = core.getPC();
        int opcode = core.readMem8(pc);
        lastDecode = "";
        int oldPC = pc;
        executeOpcode(core, opcode, pc);
        if (core.getPC() == oldPC) {
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
        switch (opcode) {
            case 0x00: case 0x02: case 0x08: case 0x0A:
            case 0x20: case 0x22: case 0x28: case 0x2A:
            case 0x30: case 0x32: case 0x38: case 0x3A:
            case 0x86: case 0x88: case 0x8A: case 0x8C: case 0x8E:
                return modrmLen(core, pc);
            case 0x01: case 0x03: case 0x09: case 0x0B:
            case 0x21: case 0x23: case 0x29: case 0x2B:
            case 0x31: case 0x33: case 0x39: case 0x3B:
            case 0x87: case 0x89: case 0x8B:
                return modrmLen(core, pc);
            case 0x04: case 0x0C: case 0x24: case 0x2C: case 0x34: case 0x3C: return 2;
            case 0x05: case 0x0D: case 0x25: case 0x2D: case 0x35: case 0x3D: return 3;
            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
            case 0x50: case 0x51: case 0x52: case 0x53:
            case 0x54: case 0x55: case 0x56: case 0x57:
            case 0x58: case 0x59: case 0x5A: case 0x5B:
            case 0x5C: case 0x5D: case 0x5E: case 0x5F:
            case 0x90: case 0xF4:
            case 0xEC: case 0xED: case 0xEE: case 0xEF:
                return 1;
            case 0x70: case 0x71: case 0x72: case 0x73:
            case 0x74: case 0x75: case 0x76: case 0x77:
            case 0x78: case 0x79: case 0x7A: case 0x7B:
            case 0x7C: case 0x7D: case 0x7E: case 0x7F:
            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
            case 0xEB: case 0xCD:
                return 2;
            case 0xA0: case 0xA1: case 0xA2: case 0xA3:
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
            case 0xC2: case 0xC3: case 0xCB:
            case 0xE8: case 0xE9:
                return 3;
            case 0xC6: return modrmLen(core, pc) + 1;
            case 0xC7: return modrmLen(core, pc) + 2;
            case 0x80: return modrmLen(core, pc) + 1;
            case 0x81: return modrmLen(core, pc) + 2;
            case 0x83: return modrmLen(core, pc) + 1;
            case 0xFE: case 0xFF: return modrmLen(core, pc);
            default: return 1;
        }
    }

    private int modrmLen(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 0x03;
        int rm = modrm & 0x07;
        if (mod == 0 && rm == 6) return 4;
        if (mod == 1) return 3;
        if (mod == 2) return 4;
        return 2;
    }

    private void executeOpcode(X86Core core, int opcode, int pc) throws Exception {
        switch (opcode) {
            case 0x00: modrm_arith(core, pc, false, false, (a, b) -> a + b); break;
            case 0x01: modrm_arith(core, pc, true, false, (a, b) -> a + b); break;
            case 0x02: modrm_arith(core, pc, false, true, (a, b) -> a + b); break;
            case 0x03: modrm_arith(core, pc, true, true, (a, b) -> a + b); break;

            case 0x08: modrm_arith(core, pc, false, false, (a, b) -> a | b); break;
            case 0x09: modrm_arith(core, pc, true, false, (a, b) -> a | b); break;
            case 0x0A: modrm_arith(core, pc, false, true, (a, b) -> a | b); break;
            case 0x0B: modrm_arith(core, pc, true, true, (a, b) -> a | b); break;

            case 0x20: modrm_arith(core, pc, false, false, (a, b) -> a & b); break;
            case 0x21: modrm_arith(core, pc, true, false, (a, b) -> a & b); break;
            case 0x22: modrm_arith(core, pc, false, true, (a, b) -> a & b); break;
            case 0x23: modrm_arith(core, pc, true, true, (a, b) -> a & b); break;

            case 0x28: modrm_arith(core, pc, false, false, (a, b) -> a - b); break;
            case 0x29: modrm_arith(core, pc, true, false, (a, b) -> a - b); break;
            case 0x2A: modrm_arith(core, pc, false, true, (a, b) -> a - b); break;
            case 0x2B: modrm_arith(core, pc, true, true, (a, b) -> a - b); break;

            case 0x30: modrm_arith(core, pc, false, false, (a, b) -> a ^ b); break;
            case 0x31: modrm_arith(core, pc, true, false, (a, b) -> a ^ b); break;
            case 0x32: modrm_arith(core, pc, false, true, (a, b) -> a ^ b); break;
            case 0x33: modrm_arith(core, pc, true, true, (a, b) -> a ^ b); break;

            case 0x38: modrm_cmp(core, pc, false, false); break;
            case 0x39: modrm_cmp(core, pc, true, false); break;
            case 0x3A: modrm_cmp(core, pc, false, true); break;
            case 0x3B: modrm_cmp(core, pc, true, true); break;

            case 0x04: { int v = core.readMem8(pc + 1) & 0xFF; int r = core.getReg(REG_EAX) + v; core.setReg(REG_EAX, r); } break;
            case 0x05: { int v = core.readMem16(pc + 1); int r = core.getReg(REG_EAX) + v; core.setReg(REG_EAX, r); } break;
            case 0x0C: { int v = core.readMem8(pc + 1) & 0xFF; int r = core.getReg(REG_EAX) | v; core.setReg(REG_EAX, r); } break;
            case 0x0D: { int v = core.readMem16(pc + 1); int r = core.getReg(REG_EAX) | v; core.setReg(REG_EAX, r); } break;
            case 0x24: { int v = core.readMem8(pc + 1) & 0xFF; int r = core.getReg(REG_EAX) & v; core.setReg(REG_EAX, r); } break;
            case 0x25: { int v = core.readMem16(pc + 1); int r = core.getReg(REG_EAX) & v; core.setReg(REG_EAX, r); } break;
            case 0x2C: { int v = core.readMem8(pc + 1) & 0xFF; int r = core.getReg(REG_EAX) - v; core.setReg(REG_EAX, r); } break;
            case 0x2D: { int v = core.readMem16(pc + 1); int r = core.getReg(REG_EAX) - v; core.setReg(REG_EAX, r); } break;
            case 0x34: { int v = core.readMem8(pc + 1) & 0xFF; int r = core.getReg(REG_EAX) ^ v; core.setReg(REG_EAX, r); } break;
            case 0x35: { int v = core.readMem16(pc + 1); int r = core.getReg(REG_EAX) ^ v; core.setReg(REG_EAX, r); } break;
            case 0x3C: { int v = core.readMem8(pc + 1) & 0xFF; int r = core.getReg(REG_EAX) - v; } break;
            case 0x3D: { int v = core.readMem16(pc + 1); int r = core.getReg(REG_EAX) - v; } break;

            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
                { int r = opcode & 0x07; core.setReg(r, core.getReg(r) + 1); } break;
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
                { int r = opcode & 0x07; core.setReg(r, core.getReg(r) - 1); } break;
            case 0x50: case 0x51: case 0x52: case 0x53:
            case 0x54: case 0x55: case 0x56: case 0x57:
                core.push32(core.getReg(opcode & 0x07)); break;
            case 0x58: case 0x59: case 0x5A: case 0x5B:
            case 0x5C: case 0x5D: case 0x5E: case 0x5F:
                core.setReg(opcode & 0x07, core.pop32()); break;

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
            case 0x8C: modrm_ignore(core, pc, true); break;
            case 0x8E: modrm_ignore(core, pc, true); break;

            case 0x90: break;

            case 0xA0: { int addr = core.readMem16(pc + 1); core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (core.readMem8(addr) & 0xFF)); } break;
            case 0xA1: { int addr = core.readMem16(pc + 1); core.setReg(REG_EAX, core.readMem16(addr)); } break;
            case 0xA2: { int addr = core.readMem16(pc + 1); core.writeMem8(addr, core.getReg(REG_EAX) & 0xFF); } break;
            case 0xA3: { int addr = core.readMem16(pc + 1); core.writeMem16(addr, core.getReg(REG_EAX)); } break;

            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
                { int r = opcode & 0x07; int v = core.readMem8(pc + 1) & 0xFF; core.setReg(r, (core.getReg(r) & ~0xFF) | v); } break;
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                { int r = opcode & 0x07; core.setReg(r, core.readMem16(pc + 1)); } break;

            case 0xC2: { int popCount = core.readMem16(pc + 1); core.setReg(REG_ESP, core.getReg(REG_ESP) + 2 + popCount); } break;
            case 0xC3: ret_near(core, pc); break;
            case 0xC6: modrm_mov_imm(core, pc, false); break;
            case 0xC7: modrm_mov_imm(core, pc, true); break;
            case 0xCB: ret_near(core, pc); break;
            case 0xCD: break;

            case 0xE8: call_rel16(core, pc); break;
            case 0xE9: jmp_rel16(core, pc); break;
            case 0xEB: jmp_rel8(core, pc); break;

            case 0xEC: { int dx = core.getReg(REG_EDX) & 0xFFFF; core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (core.readPort(dx) & 0xFF)); } break;
            case 0xED: { int dx = core.getReg(REG_EDX) & 0xFFFF; int al = core.readPort(dx) & 0xFF; int ah = core.readPort(dx + 1) & 0xFF; core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (ah << 8) | al); } break;
            case 0xEE: { int dx = core.getReg(REG_EDX) & 0xFFFF; core.writePort(dx, (byte)(core.getReg(REG_EAX) & 0xFF)); } break;
            case 0xEF: { int dx = core.getReg(REG_EDX) & 0xFFFF; core.writePort(dx, (byte)(core.getReg(REG_EAX) & 0xFF)); core.writePort(dx + 1, (byte)((core.getReg(REG_EAX) >> 8) & 0xFF)); } break;

            case 0xF4: break;

            case 0xFE: modrm_grp4(core, pc); break;
            case 0xFF: modrm_grp5(core, pc); break;

            case 0x80: modrm_imm_arith(core, pc, false, false); break;
            case 0x81: modrm_imm_arith(core, pc, true, false); break;
            case 0x83: modrm_imm_arith(core, pc, true, true); break;
        }
    }

    private void modrm_arith(X86Core core, int pc, boolean word, boolean dirToReg, BinOp op) {
        ModRm m = decodeModRm(core, pc);
        int destVal = dirToReg ? m.regVal : m.rmVal;
        int srcVal = dirToReg ? m.rmVal : m.regVal;
        int r = op.op(destVal, srcVal);
        writeResult(core, m, dirToReg, r, word);
    }

    private void modrm_cmp(X86Core core, int pc, boolean word, boolean dirToReg) {
        ModRm m = decodeModRm(core, pc);
        int destVal = dirToReg ? m.regVal : m.rmVal;
        int srcVal = dirToReg ? m.rmVal : m.regVal;
        int r = destVal - srcVal;
    }

    private void modrm_mov_to_rm(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc);
        writeVal(core, m, false, m.regVal, word);
    }

    private void modrm_mov_to_reg(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc);
        int val = m.rmVal;
        if (!word) val &= 0xFF;
        core.setReg(REG_EAX + m.regField, val);
    }

    private void modrm_mov_imm(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc);
        int imm = word ? core.readMem16(pc + m.len) : (core.readMem8(pc + m.len) & 0xFF);
        writeVal(core, m, false, imm, word);
    }

    private void modrm_xchg(X86Core core, int pc, boolean word) {
        ModRm m = decodeModRm(core, pc);
        int tmp = m.regVal;
        core.setReg(REG_EAX + m.regField, m.rmVal);
        writeVal(core, m, false, tmp, word);
    }

    private void modrm_ignore(X86Core core, int pc, boolean word) {
        decodeModRm(core, pc);
    }

    private void modrm_imm_arith(X86Core core, int pc, boolean word, boolean signedImm) {
        ModRm m = decodeModRm(core, pc);
        int regField = m.regField;
        int imm;
        if (signedImm) {
            imm = (byte)core.readMem8(pc + m.len);
        } else {
            imm = word ? core.readMem16(pc + m.len) : (core.readMem8(pc + m.len) & 0xFF);
        }
        int result = applyGrp1(m.rmVal, imm, regField);
        if (regField != 7) {
            writeVal(core, m, false, result, word);
        }
    }

    private void modrm_grp4(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc);
        int r = m.regField;
        if (r == 0) {
            writeVal(core, m, false, (m.rmVal + 1) & 0xFF, false);
        } else if (r == 1) {
            writeVal(core, m, false, (m.rmVal - 1) & 0xFF, false);
        }
    }

    private void modrm_grp5(X86Core core, int pc) {
        ModRm m = decodeModRm(core, pc);
        int r = m.regField;
        switch (r) {
            case 0:
                writeVal(core, m, false, m.rmVal + 1, true);
                break;
            case 1:
                writeVal(core, m, false, m.rmVal - 1, true);
                break;
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

    private int applyGrp1(int src, int imm, int regField) {
        switch (regField) {
            case 0: return src + imm;
            case 1: return src | imm;
            case 2: return src + imm; // ADC - simplified
            case 3: return src + imm; // SBB - simplified
            case 4: return src & imm;
            case 5: return src - imm;
            case 6: return src ^ imm;
            case 7: return src - imm; // CMP
            default: return src;
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
        int rel = core.readMem16(pc + 1);
        core.push32(pc + 3);
        core.setPC(pc + 3 + rel);
    }

    private void jmp_rel16(X86Core core, int pc) {
        int rel = core.readMem16(pc + 1);
        core.setPC(pc + 3 + rel);
    }

    private void jmp_rel8(X86Core core, int pc) {
        int rel = (byte)core.readMem8(pc + 1);
        core.setPC(pc + 2 + rel);
    }

    private ModRm decodeModRm(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 0x03;
        int regField = (modrm >> 3) & 0x07;
        int rmField = modrm & 0x07;
        int dispSize = 0;
        int disp = 0;
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
        int addr = modrm_addr(core, rmField, disp, mod);
        int rmVal;
        if (mod == 3) {
            rmVal = core.getReg(REG_EAX + rmField);
        } else {
            rmVal = core.readMem16(addr);
        }
        int regVal = core.getReg(REG_EAX + regField);
        ModRm m = new ModRm();
        m.mod = mod; m.regField = regField; m.rmField = rmField;
        m.dispSize = dispSize; m.disp = disp; m.effAddr = addr;
        m.rmVal = rmVal; m.regVal = regVal;
        m.len = 2 + dispSize;
        return m;
    }

    private void writeVal(X86Core core, ModRm m, boolean noReg, int val, boolean word) {
        if (m.mod == 3) {
            core.setReg(REG_EAX + m.rmField, val);
        } else {
            if (word) {
                core.writeMem16(m.effAddr, val);
            } else {
                core.writeMem8(m.effAddr, val & 0xFF);
            }
        }
    }

    private void writeResult(X86Core core, ModRm m, boolean dirToReg, int result, boolean word) {
        if (dirToReg) {
            core.setReg(REG_EAX + m.regField, result);
        } else {
            writeVal(core, m, false, result, word);
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

    private static class ModRm {
        int mod, regField, rmField, dispSize, disp, effAddr;
        int rmVal, regVal, len;
    }

    private interface BinOp {
        int op(int a, int b);
    }
}
