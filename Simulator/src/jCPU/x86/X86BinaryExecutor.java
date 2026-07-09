package jCPU.x86;

import static jCPU.x86.X86Core.*;

public class X86BinaryExecutor implements X86Core.iExecutor {
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
            case 0x10: case 0x12: case 0x18: case 0x1A:
            case 0x20: case 0x22: case 0x28: case 0x2A:
            case 0x30: case 0x32: case 0x38: case 0x3A:
            case 0x86: case 0x88: case 0x8A: case 0x8C: case 0x8E:
            case 0x84: case 0x85:
                return modrmLen(core, pc);
            case 0x01: case 0x03: case 0x09: case 0x0B:
            case 0x11: case 0x13: case 0x19: case 0x1B:
            case 0x21: case 0x23: case 0x29: case 0x2B:
            case 0x31: case 0x33: case 0x39: case 0x3B:
            case 0x87: case 0x89: case 0x8B: case 0x8D:
            case 0x0F:
                return modrmLen(core, pc);
            case 0x04: case 0x0C: case 0x14: case 0x1C:
            case 0x24: case 0x2C: case 0x34: case 0x3C: return 2;
            case 0x05: case 0x0D: case 0x15: case 0x1D:
            case 0x25: case 0x2D: case 0x35: case 0x3D:
            case 0x68: case 0x69:
                return 3;
            case 0x6A: case 0x6B: return 2;
            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
            case 0x50: case 0x51: case 0x52: case 0x53:
            case 0x54: case 0x55: case 0x56: case 0x57:
            case 0x58: case 0x59: case 0x5A: case 0x5B:
            case 0x5C: case 0x5D: case 0x5E: case 0x5F:
            case 0x90: case 0x91: case 0x92: case 0x93:
            case 0x94: case 0x95: case 0x96: case 0x97:
            case 0x98: case 0x99: case 0x9C: case 0x9D:
            case 0xF4: case 0xF5: case 0xF8: case 0xF9:
            case 0xFA: case 0xFB: case 0xFC: case 0xFD:
            case 0xCC:
                return 1;
            case 0x6C: case 0x6D: case 0x6E: case 0x6F:
                return 1;
            case 0x70: case 0x71: case 0x72: case 0x73:
            case 0x74: case 0x75: case 0x76: case 0x77:
            case 0x78: case 0x79: case 0x7A: case 0x7B:
            case 0x7C: case 0x7D: case 0x7E: case 0x7F:
            case 0xE3:
            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
            case 0xCD: case 0xD4: case 0xD5:
                return 2;
            case 0xEB:
                return 2;
            case 0xA8: case 0xA9:
                return modrmLen(core, pc);
            case 0xA0: case 0xA1: case 0xA2: case 0xA3:
                return 3;
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                return 5;
            case 0xE8: case 0xE9:
                return 5;
            case 0xEA:
                return 5;
            case 0xC2: case 0xCA:
                return 3;
            case 0xC3: case 0xCB:
                return 1;
            case 0xC6: case 0xC7:
                return modrmLen(core, pc) + 1 + ((opcode == 0xC7) ? 4 : 1);
            case 0x80: return modrmLen(core, pc) + 1;
            case 0x81: return modrmLen(core, pc) + 4;
            case 0x82: case 0x83: return modrmLen(core, pc) + 1;
            case 0xD0: case 0xD1: case 0xD2: case 0xD3:
                return modrmLen(core, pc);
            case 0xF6: case 0xF7:
                return modrmLen(core, pc) + ((core.readMem8(pc + 1) >> 3 & 7) == 0 ? 1 + ((opcode == 0xF7) ? 4 : 1) : 0);
            case 0xFE: case 0xFF: return modrmLen(core, pc);
            case 0x8F: return modrmLen(core, pc);
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
            // ADD r/m8, r8
            case 0x00: modrm_arith8(core, pc, false, false, (a, b) -> {
                int r = a + b;
                setArithFlags(core, r, a, b, 8);
                return r;
            }); break;
            // ADD r/m32, r32
            case 0x01: modrm_arith32(core, pc, false, false, (a, b) -> {
                int r = a + b;
                setArithFlags(core, r, a, b, 32);
                return r;
            }); break;
            // ADD r8, r/m8
            case 0x02: modrm_arith8(core, pc, false, true, (a, b) -> {
                int r = a + b;
                setArithFlags(core, r, a, b, 8);
                return r;
            }); break;
            // ADD r32, r/m32
            case 0x03: modrm_arith32(core, pc, false, true, (a, b) -> {
                int r = a + b;
                setArithFlags(core, r, a, b, 32);
                return r;
            }); break;

            // OR r/m8, r8
            case 0x08: modrm_arith8(core, pc, false, false, (a, b) -> {
                int r = a | b;
                setLogicFlags(core, r, 8);
                return r;
            }); break;
            // OR r/m32, r32
            case 0x09: modrm_arith32(core, pc, false, false, (a, b) -> {
                int r = a | b;
                setLogicFlags(core, r, 32);
                return r;
            }); break;
            // OR r8, r/m8
            case 0x0A: modrm_arith8(core, pc, false, true, (a, b) -> {
                int r = a | b;
                setLogicFlags(core, r, 8);
                return r;
            }); break;
            // OR r32, r/m32
            case 0x0B: modrm_arith32(core, pc, false, true, (a, b) -> {
                int r = a | b;
                setLogicFlags(core, r, 32);
                return r;
            }); break;

            // ADC r/m8, r8
            case 0x10: modrm_arith8(core, pc, false, false, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0;
                int r = a + b + cf;
                setArithFlags(core, r, a, b + cf, 8);
                return r;
            }); break;
            // ADC r/m32, r32
            case 0x11: modrm_arith32(core, pc, false, false, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0;
                int r = a + b + cf;
                setArithFlags(core, r, a, b + cf, 32);
                return r;
            }); break;
            // ADC r8, r/m8
            case 0x12: modrm_arith8(core, pc, false, true, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0;
                int r = a + b + cf;
                setArithFlags(core, r, a, b + cf, 8);
                return r;
            }); break;
            // ADC r32, r/m32
            case 0x13: modrm_arith32(core, pc, false, true, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0;
                int r = a + b + cf;
                setArithFlags(core, r, a, b + cf, 32);
                return r;
            }); break;

            // SBB r/m8, r8
            case 0x18: modrm_arith8(core, pc, false, false, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0;
                int r = a - (b + cf);
                setSubFlags(core, r, a, b + cf, 8);
                return r;
            }); break;
            // SBB r/m32, r32
            case 0x19: modrm_arith32(core, pc, false, false, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0;
                int r = a - (b + cf);
                setSubFlags(core, r, a, b + cf, 32);
                return r;
            }); break;

            // AND r/m8, r8
            case 0x20: modrm_arith8(core, pc, false, false, (a, b) -> {
                int r = a & b;
                setLogicFlags(core, r, 8);
                return r;
            }); break;
            // AND r/m32, r32
            case 0x21: modrm_arith32(core, pc, false, false, (a, b) -> {
                int r = a & b;
                setLogicFlags(core, r, 32);
                return r;
            }); break;
            // AND r8, r/m8
            case 0x22: modrm_arith8(core, pc, false, true, (a, b) -> {
                int r = a & b;
                setLogicFlags(core, r, 8);
                return r;
            }); break;
            // AND r32, r/m32
            case 0x23: modrm_arith32(core, pc, false, true, (a, b) -> {
                int r = a & b;
                setLogicFlags(core, r, 32);
                return r;
            }); break;

            // SUB r/m8, r8
            case 0x28: modrm_arith8(core, pc, false, false, (a, b) -> {
                int r = a - b;
                setSubFlags(core, r, a, b, 8);
                return r;
            }); break;
            // SUB r/m32, r32
            case 0x29: modrm_arith32(core, pc, false, false, (a, b) -> {
                int r = a - b;
                setSubFlags(core, r, a, b, 32);
                return r;
            }); break;
            // SUB r8, r/m8
            case 0x2A: modrm_arith8(core, pc, false, true, (a, b) -> {
                int r = a - b;
                setSubFlags(core, r, a, b, 8);
                return r;
            }); break;
            // SUB r32, r/m32
            case 0x2B: modrm_arith32(core, pc, false, true, (a, b) -> {
                int r = a - b;
                setSubFlags(core, r, a, b, 32);
                return r;
            }); break;

            // XOR r/m8, r8
            case 0x30: modrm_arith8(core, pc, false, false, (a, b) -> {
                int r = a ^ b;
                setLogicFlags(core, r, 8);
                return r;
            }); break;
            // XOR r/m32, r32
            case 0x31: modrm_arith32(core, pc, false, false, (a, b) -> {
                int r = a ^ b;
                setLogicFlags(core, r, 32);
                return r;
            }); break;
            // XOR r8, r/m8
            case 0x32: modrm_arith8(core, pc, false, true, (a, b) -> {
                int r = a ^ b;
                setLogicFlags(core, r, 8);
                return r;
            }); break;
            // XOR r32, r/m32
            case 0x33: modrm_arith32(core, pc, false, true, (a, b) -> {
                int r = a ^ b;
                setLogicFlags(core, r, 32);
                return r;
            }); break;

            // CMP r/m8, r8
            case 0x38: modrm_cmp8(core, pc, false, false); break;
            // CMP r/m32, r32
            case 0x39: modrm_cmp32(core, pc, false, false); break;
            // CMP r8, r/m8
            case 0x3A: modrm_cmp8(core, pc, false, true); break;
            // CMP r32, r/m32
            case 0x3B: modrm_cmp32(core, pc, false, true); break;

            // MOV r/m8, r8
            case 0x88: modrm_mov8(core, pc, false, false); break;
            // MOV r/m32, r32
            case 0x89: modrm_mov32(core, pc, false, false); break;
            // MOV r8, r/m8
            case 0x8A: modrm_mov8(core, pc, false, true); break;
            // MOV r32, r/m32
            case 0x8B: modrm_mov32(core, pc, false, true); break;
            // LEA r32, m
            case 0x8D: lea(core, pc); break;

            // MOV r/m16, sreg
            case 0x8C: modrm(core, pc, true, false, (rmVal) -> core.readMem8(pc + 1) & 7, (val, rmVal) -> {}); break;

            // MOV sreg, r/m16
            case 0x8E: break;

            // XCHG r32, r32 (r/m32, r32)
            case 0x87: xchg32(core, pc); break;
            // XCHG eAX, r32
            case 0x91: case 0x92: case 0x93: case 0x94:
            case 0x95: case 0x96: case 0x97:
                xchg_acc(core, opcode); break;

            // INC r32
            case 0x40: case 0x41: case 0x42: case 0x43:
            case 0x44: case 0x45: case 0x46: case 0x47:
                inc32(core, opcode - 0x40); break;
            // DEC r32
            case 0x48: case 0x49: case 0x4A: case 0x4B:
            case 0x4C: case 0x4D: case 0x4E: case 0x4F:
                dec32(core, opcode - 0x48); break;

            // PUSH r32
            case 0x50: case 0x51: case 0x52: case 0x53:
            case 0x54: case 0x55: case 0x56: case 0x57:
                core.push32(core.getReg(opcode - 0x50)); break;
            // POP r32
            case 0x58: case 0x59: case 0x5A: case 0x5B:
            case 0x5C: case 0x5D: case 0x5E: case 0x5F:
                core.setReg(opcode - 0x58, core.pop32()); break;

            // MOV r8, imm8
            case 0xB0: case 0xB1: case 0xB2: case 0xB3:
            case 0xB4: case 0xB5: case 0xB6: case 0xB7:
                mov_imm8(core, pc, opcode - 0xB0); break;
            // MOV r32, imm32
            case 0xB8: case 0xB9: case 0xBA: case 0xBB:
            case 0xBC: case 0xBD: case 0xBE: case 0xBF:
                mov_imm32(core, pc, opcode - 0xB8); break;

            // MOV AL, moffs8
            case 0xA0: mov_moffs(core, pc, true, false); break;
            // MOV eAX, moffs32
            case 0xA1: mov_moffs(core, pc, true, true); break;
            // MOV moffs8, AL
            case 0xA2: mov_moffs(core, pc, false, false); break;
            // MOV moffs32, eAX
            case 0xA3: mov_moffs(core, pc, false, true); break;

            // ADD AL, imm8
            case 0x04: alu_acc_imm8(core, pc, (a, b) -> {
                int r = a + b; setArithFlags(core, r, a, b, 8); return r;
            }); break;
            // OR AL, imm8
            case 0x0C: alu_acc_imm8(core, pc, (a, b) -> {
                int r = a | b; setLogicFlags(core, r, 8); return r;
            }); break;
            // ADC AL, imm8
            case 0x14: alu_acc_imm8(core, pc, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0; int r = a + b + cf; setArithFlags(core, r, a, b + cf, 8); return r;
            }); break;
            // SBB AL, imm8
            case 0x1C: alu_acc_imm8(core, pc, (a, b) -> {
                int cf = core.getFlag(FLAG_CF) ? 1 : 0; int r = a - (b + cf); setSubFlags(core, r, a, b + cf, 8); return r;
            }); break;
            // AND AL, imm8
            case 0x24: alu_acc_imm8(core, pc, (a, b) -> {
                int r = a & b; setLogicFlags(core, r, 8); return r;
            }); break;
            // SUB AL, imm8
            case 0x2C: alu_acc_imm8(core, pc, (a, b) -> {
                int r = a - b; setSubFlags(core, r, a, b, 8); return r;
            }); break;
            // XOR AL, imm8
            case 0x34: alu_acc_imm8(core, pc, (a, b) -> {
                int r = a ^ b; setLogicFlags(core, r, 8); return r;
            }); break;
            // CMP AL, imm8
            case 0x3C: alu_acc_imm8(core, pc, (a, b) -> {
                int r = a - b; setSubFlags(core, r, a, b, 8); return r;
            }); break;

            // ADD eAX, imm32
            case 0x05: alu_acc_imm32(core, pc, (a, b) -> {
                int r = a + b; setArithFlags(core, r, a, b, 32); return r;
            }); break;
            // OR eAX, imm32
            case 0x0D: alu_acc_imm32(core, pc, (a, b) -> {
                int r = a | b; setLogicFlags(core, r, 32); return r;
            }); break;
            // AND eAX, imm32
            case 0x25: alu_acc_imm32(core, pc, (a, b) -> {
                int r = a & b; setLogicFlags(core, r, 32); return r;
            }); break;
            // SUB eAX, imm32
            case 0x2D: alu_acc_imm32(core, pc, (a, b) -> {
                int r = a - b; setSubFlags(core, r, a, b, 32); return r;
            }); break;
            // XOR eAX, imm32
            case 0x35: alu_acc_imm32(core, pc, (a, b) -> {
                int r = a ^ b; setLogicFlags(core, r, 32); return r;
            }); break;
            // CMP eAX, imm32
            case 0x3D: alu_acc_imm32(core, pc, (a, b) -> {
                int r = a - b; setSubFlags(core, r, a, b, 32); return r;
            }); break;

            // NOP
            case 0x90: break;

            // PUSHF/POPF
            case 0x9C: core.push32(core.getFlags()); break;
            case 0x9D: core.setFlags(core.pop32()); break;

            // CLC/STC/CMC
            case 0xF8: core.setFlag(FLAG_CF, false); break;
            case 0xF9: core.setFlag(FLAG_CF, true); break;
            case 0xF5: core.setFlag(FLAG_CF, !core.getFlag(FLAG_CF)); break;

            // CLD/STD
            case 0xFC: core.setFlag(FLAG_DF, false); break;
            case 0xFD: core.setFlag(FLAG_DF, true); break;

            // CLI/STI (IF flag accepted but no behavior)
            case 0xFA: break;
            case 0xFB: break;

            // CBW/CWDE
            case 0x98: {
                int al = core.getReg(REG_EAX) & 0xFF;
                core.setReg(REG_EAX, (al & 0x80) != 0 ? al | 0xFFFFFF00 : al);
                break;
            }
            // CWD/CDQ
            case 0x99: {
                int eax = core.getReg(REG_EAX);
                core.setReg(REG_EDX, (eax & 0x80000000) != 0 ? 0xFFFFFFFF : 0);
                break;
            }

            // HLT
            case 0xF4: break;

            // INT
            case 0xCD: break;

            // JMP rel8
            case 0xEB: {
                int disp = (byte)core.readMem8(pc + 1);
                core.setPC(pc + 2 + disp);
                return;
            }

            // JMP rel32
            case 0xE9: {
                int disp = core.readMem8(pc + 1) | (core.readMem8(pc + 2) << 8) | (core.readMem8(pc + 3) << 16) | (core.readMem8(pc + 4) << 24);
                core.setPC(pc + 5 + disp);
                return;
            }

            // CALL rel32
            case 0xE8: {
                int disp = core.readMem8(pc + 1) | (core.readMem8(pc + 2) << 8) | (core.readMem8(pc + 3) << 16) | (core.readMem8(pc + 4) << 24);
                core.push32(pc + 5);
                core.setPC(pc + 5 + disp);
                return;
            }

            // RET near
            case 0xC3: {
                core.setPC(core.pop32());
                return;
            }

            // RET near with imm16
            case 0xC2: {
                int popCnt = core.readMem8(pc + 1) | (core.readMem8(pc + 2) << 8);
                core.setPC(core.pop32());
                core.setReg(REG_ESP, core.getReg(REG_ESP) + popCnt);
                return;
            }

            // Conditional jumps
            case 0x70: jcc(core, pc, JccCondition.JO); break;
            case 0x71: jcc(core, pc, JccCondition.JNO); break;
            case 0x72: jcc(core, pc, JccCondition.JB); break;
            case 0x73: jcc(core, pc, JccCondition.JAE); break;
            case 0x74: jcc(core, pc, JccCondition.JE); break;
            case 0x75: jcc(core, pc, JccCondition.JNE); break;
            case 0x76: jcc(core, pc, JccCondition.JBE); break;
            case 0x77: jcc(core, pc, JccCondition.JA); break;
            case 0x78: jcc(core, pc, JccCondition.JS); break;
            case 0x79: jcc(core, pc, JccCondition.JNS); break;
            case 0x7A: jcc(core, pc, JccCondition.JP); break;
            case 0x7B: jcc(core, pc, JccCondition.JNP); break;
            case 0x7C: jcc(core, pc, JccCondition.JL); break;
            case 0x7D: jcc(core, pc, JccCondition.JGE); break;
            case 0x7E: jcc(core, pc, JccCondition.JLE); break;
            case 0x7F: jcc(core, pc, JccCondition.JG); break;

            // Group 1: 0x80/0x82/0x83 (imm8), 0x81 (imm32)
            case 0x80: group1_imm8(core, pc); break;
            case 0x82: group1_imm8(core, pc); break;
            case 0x83: group1_imm8s(core, pc); break;
            case 0x81: group1_imm32(core, pc); break;

            // Group 3: 0xF6 (imm8), 0xF7 (imm32)
            case 0xF6: group3_8(core, pc); break;
            case 0xF7: group3_32(core, pc); break;

            // INC/DEC r/m
            case 0xFE: group4(core, pc); break;
            case 0xFF: group5(core, pc); break;

            // MOV r/m8, imm8
            case 0xC6: mov_rm_imm8(core, pc); break;
            // MOV r/m32, imm32
            case 0xC7: mov_rm_imm32(core, pc); break;

            // PUSH r/m32
            case 0x8F: pop_rm(core, pc); break;

            // TEST r/m8, r8
            case 0x84: modrm_test8(core, pc, false, false); break;
            // TEST r/m32, r32
            case 0x85: modrm_test32(core, pc, false, false); break;

            // NOT/NEG r/m (Group 3 sub-opcodes 2 and 3)
            // (handled via group3 below)

            // MOVZX r32, r/m8
            // MOVSX r32, r/m8
            // (0x0F prefix opcodes handled separately)

            default: break;
        }
    }

    private enum JccCondition {
        JO, JNO, JB, JAE, JE, JNE, JBE, JA, JS, JNS, JP, JNP, JL, JGE, JLE, JG
    }

    private void jcc(X86Core core, int pc, JccCondition cond) {
        int disp = (byte)core.readMem8(pc + 1);
        boolean taken = false;
        switch (cond) {
            case JO:  taken = core.getFlag(FLAG_OF); break;
            case JNO: taken = !core.getFlag(FLAG_OF); break;
            case JB:  taken = core.getFlag(FLAG_CF); break;
            case JAE: taken = !core.getFlag(FLAG_CF); break;
            case JE:  taken = core.getFlag(FLAG_ZF); break;
            case JNE: taken = !core.getFlag(FLAG_ZF); break;
            case JBE: taken = core.getFlag(FLAG_CF) || core.getFlag(FLAG_ZF); break;
            case JA:  taken = !core.getFlag(FLAG_CF) && !core.getFlag(FLAG_ZF); break;
            case JS:  taken = core.getFlag(FLAG_SF); break;
            case JNS: taken = !core.getFlag(FLAG_SF); break;
            case JP:  taken = core.getFlag(FLAG_PF); break;
            case JNP: taken = !core.getFlag(FLAG_PF); break;
            case JL:  taken = core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF); break;
            case JGE: taken = core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF); break;
            case JLE: taken = (core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF)) || core.getFlag(FLAG_ZF); break;
            case JG:  taken = (core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF)) && !core.getFlag(FLAG_ZF); break;
        }
        if (taken) {
            core.setPC(pc + 2 + disp);
        } else {
            core.setPC(pc + 2);
        }
    }

    private void setArithFlags(X86Core core, int result, int a, int b, int bits) {
        int mask = bits == 8 ? 0xFF : 0xFFFFFFFF;
        int signMask = bits == 8 ? 0x80 : 0x80000000;
        boolean cf;
        if (bits == 8) {
            cf = (result & ~0xFF) != 0;
        } else {
            long la = a & 0xFFFFFFFFL;
            long lb = b & 0xFFFFFFFFL;
            cf = (la + lb) > 0xFFFFFFFFL;
        }
        core.setFlag(FLAG_CF, cf);
        core.setFlag(FLAG_ZF, (result & mask) == 0);
        core.setFlag(FLAG_SF, (result & signMask) != 0);
        core.setFlag(FLAG_OF, ((a ^ result) & (b ^ result) & signMask) != 0);
        int p = result & 0xFF;
        p ^= p >> 4;
        p ^= p >> 2;
        p ^= p >> 1;
        core.setFlag(FLAG_PF, (p & 1) == 0);
    }

    private void setSubFlags(X86Core core, int result, int a, int b, int bits) {
        int mask = bits == 8 ? 0xFF : 0xFFFFFFFF;
        int signMask = bits == 8 ? 0x80 : 0x80000000;
        boolean cf;
        if (bits == 8) {
            cf = (result & ~0xFF) != 0;
        } else {
            long la = a & 0xFFFFFFFFL;
            long lb = b & 0xFFFFFFFFL;
            cf = la < lb;
        }
        core.setFlag(FLAG_CF, cf);
        core.setFlag(FLAG_ZF, (result & mask) == 0);
        core.setFlag(FLAG_SF, (result & signMask) != 0);
        core.setFlag(FLAG_OF, ((a ^ b) & (a ^ result) & signMask) != 0);
        int p = result & 0xFF;
        p ^= p >> 4;
        p ^= p >> 2;
        p ^= p >> 1;
        core.setFlag(FLAG_PF, (p & 1) == 0);
    }

    private void setLogicFlags(X86Core core, int result, int bits) {
        int mask = bits == 8 ? 0xFF : 0xFFFFFFFF;
        int signMask = bits == 8 ? 0x80 : 0x80000000;
        core.setFlag(FLAG_CF, false);
        core.setFlag(FLAG_OF, false);
        core.setFlag(FLAG_ZF, (result & mask) == 0);
        core.setFlag(FLAG_SF, (result & signMask) != 0);
        int p = result & 0xFF;
        p ^= p >> 4;
        p ^= p >> 2;
        p ^= p >> 1;
        core.setFlag(FLAG_PF, (p & 1) == 0);
    }

    private interface BinOp8 { int apply(int a, int b); }
    private interface BinOp32 { int apply(int a, int b); }

    private void modrm_arith8(X86Core core, int pc, boolean is32, boolean srcIsRm, BinOp8 op) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        int a, b;
        if (srcIsRm) {
            a = readReg8(core, reg);
            b = readRm8(core, mod, rm, pc);
        } else {
            b = readReg8(core, reg);
            a = readRm8(core, mod, rm, pc);
        }
        int r = op.apply(a & 0xFF, b & 0xFF) & 0xFF;
        writeRm8(core, mod, rm, pc, r);
    }

    private void modrm_arith32(X86Core core, int pc, boolean is32, boolean srcIsRm, BinOp32 op) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        int a, b;
        if (srcIsRm) {
            a = readReg32(core, reg);
            b = readRm32(core, mod, rm, pc);
        } else {
            b = readReg32(core, reg);
            a = readRm32(core, mod, rm, pc);
        }
        int r = op.apply(a, b);
        writeRm32(core, mod, rm, pc, r);
    }

    private void modrm_mov8(X86Core core, int pc, boolean is32, boolean srcIsRm) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        if (srcIsRm) {
            int v = readRm8(core, mod, rm, pc);
            writeReg8(core, reg, v);
        } else {
            int v = readReg8(core, reg);
            writeRm8(core, mod, rm, pc, v);
        }
    }

    private void modrm_mov32(X86Core core, int pc, boolean is32, boolean srcIsRm) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        if (srcIsRm) {
            int v = readRm32(core, mod, rm, pc);
            core.setReg(reg, v);
        } else {
            int v = core.getReg(reg);
            writeRm32(core, mod, rm, pc, v);
        }
    }

    private void modrm_cmp8(X86Core core, int pc, boolean is32, boolean srcIsRm) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        int a = readReg8(core, reg);
        int b = readRm8(core, mod, rm, pc);
        int r = a - b;
        setSubFlags(core, r, a, b, 8);
    }

    private void modrm_cmp32(X86Core core, int pc, boolean is32, boolean srcIsRm) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        int a = core.getReg(reg);
        int b = readRm32(core, mod, rm, pc);
        int r = a - b;
        setSubFlags(core, r, a, b, 32);
    }

    private void modrm_test8(X86Core core, int pc, boolean is32, boolean srcIsRm) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        int a = readReg8(core, reg);
        int b = readRm8(core, mod, rm, pc);
        int r = a & b;
        setLogicFlags(core, r, 8);
    }

    private void modrm_test32(X86Core core, int pc, boolean is32, boolean srcIsRm) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        int a = core.getReg(reg);
        int b = readRm32(core, mod, rm, pc);
        int r = a & b;
        setLogicFlags(core, r, 32);
    }

    private void modrm(X86Core core, int pc, boolean is32, boolean srcIsRm, 
                       java.util.function.IntUnaryOperator readOp,
                       java.util.function.ObjIntConsumer<Integer> writeOp) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int reg = (modrm >> 3) & 7;
        int rm = modrm & 7;
        int rmVal = (mod == 3) ? core.getReg(rm) : resolveAddr(core, mod, rm, pc);
        int srcVal = reg;
        int val = readOp.applyAsInt(rmVal);
        writeOp.accept(rmVal, val);
    }

    private void xchg32(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int regIdx = (modrm >> 3) & 7;
        int rm = modrm & 7;
        if (mod == 3) {
            int tmp = core.getReg(regIdx);
            core.setReg(regIdx, core.getReg(rm));
            core.setReg(rm, tmp);
        }
    }

    private void xchg_acc(X86Core core, int opcode) {
        int regIdx = opcode - 0x91;
        int tmp = core.getReg(REG_EAX);
        core.setReg(REG_EAX, core.getReg(regIdx));
        core.setReg(regIdx, tmp);
    }

    private void inc32(X86Core core, int reg) {
        int v = core.getReg(reg) + 1;
        setArithFlags(core, v, core.getReg(reg), 1, 32);
        core.setReg(reg, v);
    }

    private void dec32(X86Core core, int reg) {
        int v = core.getReg(reg) - 1;
        setSubFlags(core, v, core.getReg(reg), 1, 32);
        core.setReg(reg, v);
    }

    private void lea(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int reg = (modrm >> 3) & 7;
        core.setReg(reg, resolveAddr(core, (modrm >> 6) & 3, modrm & 7, pc));
    }

    private int resolveAddr(X86Core core, int mod, int rm, int pc) {
        if (mod == 0 && rm == 6) {
            return read32(core, pc + 2);
        }
        if (mod == 0) {
            return getBase(core, rm);
        }
        if (mod == 1) {
            return getBase(core, rm) + (byte)core.readMem8(pc + 2);
        }
        return getBase(core, rm) + read32(core, pc + 2);
    }

    private int getBase(X86Core core, int rm) {
        return core.getReg(rm);
    }

    private int read8(X86Core core, int addr) {
        return core.readMem8(addr) & 0xFF;
    }

    private int read32(X86Core core, int addr) {
        return core.readMem8(addr) | (core.readMem8(addr + 1) << 8) | (core.readMem8(addr + 2) << 16) | (core.readMem8(addr + 3) << 24);
    }

    private int readReg8(X86Core core, int reg) {
        return core.getReg(reg) & 0xFF;
    }

    private int readReg32(X86Core core, int reg) {
        return core.getReg(reg);
    }

    private void writeReg8(X86Core core, int reg, int val) {
        core.setReg(reg, (core.getReg(reg) & 0xFFFFFF00) | (val & 0xFF));
    }

    private int readRm8(X86Core core, int mod, int rm, int pc) {
        if (mod == 3) return readReg8(core, rm);
        int addr = resolveAddr(core, mod, rm, pc);
        return read8(core, addr);
    }

    private int readRm32(X86Core core, int mod, int rm, int pc) {
        if (mod == 3) return core.getReg(rm);
        int addr = resolveAddr(core, mod, rm, pc);
        return read32(core, addr);
    }

    private void writeRm8(X86Core core, int mod, int rm, int pc, int val) {
        if (mod == 3) {
            writeReg8(core, rm, val);
        } else {
            int addr = resolveAddr(core, mod, rm, pc);
            core.writeMem8(addr, val & 0xFF);
        }
    }

    private void writeRm32(X86Core core, int mod, int rm, int pc, int val) {
        if (mod == 3) {
            core.setReg(rm, val);
        } else {
            int addr = resolveAddr(core, mod, rm, pc);
            core.writeMem8(addr, val & 0xFF);
            core.writeMem8(addr + 1, (val >> 8) & 0xFF);
            core.writeMem8(addr + 2, (val >> 16) & 0xFF);
            core.writeMem8(addr + 3, (val >> 24) & 0xFF);
        }
    }

    private void alu_acc_imm8(X86Core core, int pc, BinOp8 op) {
        int a = core.getReg(REG_EAX) & 0xFF;
        int b = core.readMem8(pc + 1) & 0xFF;
        int r = op.apply(a, b) & 0xFF;
        core.setReg(REG_EAX, (core.getReg(REG_EAX) & 0xFFFFFF00) | r);
    }

    private void alu_acc_imm32(X86Core core, int pc, BinOp32 op) {
        int a = core.getReg(REG_EAX);
        int b = read32(core, pc + 1);
        int r = op.apply(a, b);
        core.setReg(REG_EAX, r);
    }

    private void mov_imm8(X86Core core, int pc, int reg) {
        int v = core.readMem8(pc + 1) & 0xFF;
        core.setReg(reg, (core.getReg(reg) & 0xFFFFFF00) | v);
    }

    private void mov_imm32(X86Core core, int pc, int reg) {
        int v = read32(core, pc + 1);
        core.setReg(reg, v);
    }

    private void mov_moffs(X86Core core, int pc, boolean toAcc, boolean is32) {
        int addr = read32(core, pc + 1);
        if (toAcc) {
            if (is32) {
                core.setReg(REG_EAX, read32(core, addr));
            } else {
                core.setReg(REG_EAX, (core.getReg(REG_EAX) & 0xFFFFFF00) | (core.readMem8(addr) & 0xFF));
            }
        } else {
            if (is32) {
                int val = core.getReg(REG_EAX);
                core.writeMem8(addr, val & 0xFF);
                core.writeMem8(addr + 1, (val >> 8) & 0xFF);
                core.writeMem8(addr + 2, (val >> 16) & 0xFF);
                core.writeMem8(addr + 3, (val >> 24) & 0xFF);
            } else {
                core.writeMem8(addr, core.getReg(REG_EAX) & 0xFF);
            }
        }
    }

    private void mov_rm_imm8(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int rm = modrm & 7;
        int val = core.readMem8(pc + 2) & 0xFF;
        writeRm8(core, mod, rm, pc, val);
    }

    private void mov_rm_imm32(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int rm = modrm & 7;
        int val = read32(core, pc + 2);
        writeRm32(core, mod, rm, pc, val);
    }

    private void pop_rm(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 3;
        int rm = modrm & 7;
        writeRm32(core, mod, rm, pc, core.pop32());
    }

    private void group1_imm8(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int subOp = (modrm >> 3) & 7;
        int imm = core.readMem8(pc + 2) & 0xFF;
        int a = readRm8(core, (modrm >> 6) & 3, modrm & 7, pc);
        int r = 0;
        switch (subOp) {
            case 0: r = a + imm; setArithFlags(core, r, a, imm, 8); break;
            case 1: r = a | imm; setLogicFlags(core, r, 8); break;
            case 2: r = a + imm + (core.getFlag(FLAG_CF) ? 1 : 0); setArithFlags(core, r, a, imm + (core.getFlag(FLAG_CF) ? 1 : 0), 8); break;
            case 3: r = a - (imm + (core.getFlag(FLAG_CF) ? 1 : 0)); setSubFlags(core, r, a, imm + (core.getFlag(FLAG_CF) ? 1 : 0), 8); break;
            case 4: r = a & imm; setLogicFlags(core, r, 8); break;
            case 5: r = a - imm; setSubFlags(core, r, a, imm, 8); break;
            case 6: r = a ^ imm; setLogicFlags(core, r, 8); break;
            case 7: r = a - imm; setSubFlags(core, r, a, imm, 8); break;
        }
        writeRm8(core, (modrm >> 6) & 3, modrm & 7, pc, r & 0xFF);
    }

    private void group1_imm8s(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int subOp = (modrm >> 3) & 7;
        int imm = (byte)core.readMem8(pc + 2);
        int a = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
        int r = 0;
        switch (subOp) {
            case 0: r = a + imm; setArithFlags(core, r, a, imm, 32); break;
            case 1: r = a | imm; setLogicFlags(core, r, 32); break;
            case 2: r = a + imm + (core.getFlag(FLAG_CF) ? 1 : 0); setArithFlags(core, r, a, imm + (core.getFlag(FLAG_CF) ? 1 : 0), 32); break;
            case 3: r = a - (imm + (core.getFlag(FLAG_CF) ? 1 : 0)); setSubFlags(core, r, a, imm + (core.getFlag(FLAG_CF) ? 1 : 0), 32); break;
            case 4: r = a & imm; setLogicFlags(core, r, 32); break;
            case 5: r = a - imm; setSubFlags(core, r, a, imm, 32); break;
            case 6: r = a ^ imm; setLogicFlags(core, r, 32); break;
            case 7: r = a - imm; setSubFlags(core, r, a, imm, 32); break;
        }
        writeRm32(core, (modrm >> 6) & 3, modrm & 7, pc, r);
    }

    private void group1_imm32(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int subOp = (modrm >> 3) & 7;
        int imm = read32(core, pc + 2);
        int a = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
        int r = 0;
        switch (subOp) {
            case 0: r = a + imm; setArithFlags(core, r, a, imm, 32); break;
            case 1: r = a | imm; setLogicFlags(core, r, 32); break;
            case 2: r = a + imm + (core.getFlag(FLAG_CF) ? 1 : 0); setArithFlags(core, r, a, imm + (core.getFlag(FLAG_CF) ? 1 : 0), 32); break;
            case 3: r = a - (imm + (core.getFlag(FLAG_CF) ? 1 : 0)); setSubFlags(core, r, a, imm + (core.getFlag(FLAG_CF) ? 1 : 0), 32); break;
            case 4: r = a & imm; setLogicFlags(core, r, 32); break;
            case 5: r = a - imm; setSubFlags(core, r, a, imm, 32); break;
            case 6: r = a ^ imm; setLogicFlags(core, r, 32); break;
            case 7: r = a - imm; setSubFlags(core, r, a, imm, 32); break;
        }
        writeRm32(core, (modrm >> 6) & 3, modrm & 7, pc, r);
    }

    private void group3_8(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int subOp = (modrm >> 3) & 7;
        int a = readRm8(core, (modrm >> 6) & 3, modrm & 7, pc);
        switch (subOp) {
            case 2: {  // NOT r/m8
                writeRm8(core, (modrm >> 6) & 3, modrm & 7, pc, (~a) & 0xFF);
                break;
            }
            case 3: {  // NEG r/m8
                int r = (-a) & 0xFF;
                setSubFlags(core, r, 0, a, 8);
                writeRm8(core, (modrm >> 6) & 3, modrm & 7, pc, r);
                break;
            }
            case 0: {  // TEST r/m8, imm8
                int imm = core.readMem8(pc + 2) & 0xFF;
                int r = a & imm;
                setLogicFlags(core, r, 8);
                break;
            }
            case 4: case 5: case 6: case 7:  // MUL/IMUL/DIV/IDIV r/m8
                break;
        }
    }

    private void group3_32(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int subOp = (modrm >> 3) & 7;
        int a = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
        switch (subOp) {
            case 2: {  // NOT r/m32
                writeRm32(core, (modrm >> 6) & 3, modrm & 7, pc, ~a);
                break;
            }
            case 3: {  // NEG r/m32
                int r = -a;
                setSubFlags(core, r, 0, a, 32);
                writeRm32(core, (modrm >> 6) & 3, modrm & 7, pc, r);
                break;
            }
            case 0: {  // TEST r/m32, imm32
                int imm = read32(core, pc + 2);
                int r = a & imm;
                setLogicFlags(core, r, 32);
                break;
            }
            case 4: case 5: case 6: case 7:  // MUL/IMUL/DIV/IDIV r/m32
                break;
        }
    }

    private void group4(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        int subOp = (modrm >> 3) & 7;
        int a = readRm8(core, (modrm >> 6) & 3, modrm & 7, pc);
        if (subOp == 0) {
            int r = a + 1;
            setArithFlags(core, r, a, 1, 8);
            writeRm8(core, (modrm >> 6) & 3, modrm & 7, pc, r & 0xFF);
        } else if (subOp == 1) {
            int r = a - 1;
            setSubFlags(core, r, a, 1, 8);
            writeRm8(core, (modrm >> 6) & 3, modrm & 7, pc, r & 0xFF);
        }
    }

    private void group5(X86Core core, int pc) throws Exception {
        int modrm = core.readMem8(pc + 1);
        int subOp = (modrm >> 3) & 7;
        switch (subOp) {
            case 0: {  // INC r/m32
                int a = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
                int r = a + 1;
                setArithFlags(core, r, a, 1, 32);
                writeRm32(core, (modrm >> 6) & 3, modrm & 7, pc, r);
                break;
            }
            case 1: {  // DEC r/m32
                int a = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
                int r = a - 1;
                setSubFlags(core, r, a, 1, 32);
                writeRm32(core, (modrm >> 6) & 3, modrm & 7, pc, r);
                break;
            }
            case 2: {  // CALL r/m32 (near indirect)
                int target = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
                core.push32(pc + modrmLen(core, pc));
                core.setPC(target);
                break;
            }
            case 3: {  // CALL r/m32 (far indirect) — treated as near
                int target = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
                core.push32(pc + modrmLen(core, pc));
                core.setPC(target);
                break;
            }
            case 4: {  // JMP r/m32 (near indirect)
                int target = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
                core.setPC(target);
                break;
            }
            case 5: {  // JMP r/m32 (far indirect) — treated as near
                int target = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
                core.setPC(target);
                break;
            }
            case 6: {  // PUSH r/m32
                int v = readRm32(core, (modrm >> 6) & 3, modrm & 7, pc);
                core.push32(v);
                break;
            }
        }
    }
}
