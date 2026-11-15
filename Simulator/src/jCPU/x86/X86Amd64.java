package jCPU.x86;

import static jCPU.x86.X86Core.*;

public class X86Amd64 implements X86Core.iExecutor
{
    private String lastDecode = "";

    /* REX prefix bits: 0100WRXB */
    private static final int REX_PREFIX = 0x40;
    private static final int REX_W = 0x08; // 64-bit operand size
    private static final int REX_R = 0x04; // extends ModR/M reg field
    private static final int REX_X = 0x02; // extends SIB index
    private static final int REX_B = 0x01; // extends ModR/M rm or SIB base

    private int rexW = 0;
    private int rexR = 0;
    private int rexX = 0;
    private int rexB = 0;

    /* Address of the instruction being executed, including any prefix. */
    private long insnStart = 0;

    @Override
    public int step(X86Core core) throws Exception {
        long rip = core.getPC64();
        int opcode = core.readMem8((int) rip);
        lastDecode = "";
        long oldRip = rip;
        insnStart = rip;
        rexW = rexR = rexX = rexB = 0;

        // Handle REX prefix
        if ((opcode & 0xF0) == REX_PREFIX) {
            rexW = (opcode & REX_W) != 0 ? 1 : 0;
            rexR = (opcode & REX_R) != 0 ? 1 : 0;
            rexX = (opcode & REX_X) != 0 ? 1 : 0;
            rexB = (opcode & REX_B) != 0 ? 1 : 0;
            rip++;
            opcode = core.readMem8((int) rip);
        }

        // Throws X86UnknownOpcodeException for anything that is not implemented,
        // leaving RIP on the offending instruction.
        executeOpcode(core, opcode, (int) rip);
        if (core.getPC64() == oldRip) {
            // The instruction did not branch: advance past it.
            int len = instructionLen(core, opcode, (int) rip, rexW);
            core.setPC64(rip + len);
        }
        return 1;
    }

    /**
     * Stop execution on an instruction that is not implemented.  Skipping it
     * would mean guessing its length and silently continuing with a wrong
     * machine state, so the caller gets an exception instead.
     */
    private X86UnknownOpcodeException unknownOpcode(X86Core core, String what) {
        return new X86UnknownOpcodeException(core, insnStart, what);
    }

    @Override
    public String getDecodeAt(X86Core core, int pc) {
        return String.format("%02X", core.readMem8(pc));
    }

    /* Map a 3-bit register field to a 64-bit register index, applying REX.B/R */
    private int regIndex64(int base, int ext) {
        return base + (ext != 0 ? 8 : 0);
    }

    /* Map rm field to 64-bit register index with REX.B */
    private int rmIndex64(int rmField) {
        return rmField + (rexB != 0 ? 8 : 0);
    }

    /* Map reg field to 64-bit register index with REX.R */
    private int regFieldIndex64(int regField) {
        return regField + (rexR != 0 ? 8 : 0);
    }

    /* ---------------- instruction length decoder ---------------- */

    private int instructionLen(X86Core core, int opcode, int pc, int rexW) {
        // Two-byte escape (0x0F prefix)
        if (opcode == 0x0F) {
            int op2 = core.readMem8(pc + 1);
            return 2 + twoByteLen(core, op2, pc + 2, rexW);
        }

        // MOV r64, imm64: REX.W + B8..BF
        if ((opcode & 0xF8) == 0xB8 && rexW != 0) {
            return 10; // REX(1) + opcode(1) + imm64(8)
        }

        // PUSH r64: REX.W + 50..57
        // POP r64: REX.W + 58..5F
        if ((opcode & 0xF8) == 0x50 && rexW != 0) {
            return 2;
        }

        // INC/DEC r64: REX.W + 40..4F / 48..4F (REX.W + 40..47 INC, 48..4F DEC)
        if ((opcode & 0xF8) == 0x40 && rexW != 0) {
            return 2;
        }

        // CMP r64, imm32: REX.W + 81 /7 (only if modrm reg==7) - handled by modrm
        // PUSH imm32: 0x68
        // IMUL: 0x69
        // JMP near rel32: 0xE9
        // CALL near rel32: 0xE8
        if (opcode == 0x05 || opcode == 0x0D || opcode == 0x25 ||
            opcode == 0x2D || opcode == 0x35 || opcode == 0x3D ||
            opcode == 0x68 || opcode == 0x69 ||
            opcode == 0xE8 || opcode == 0xE9) {
            return 5;
        }

        // CMP/TEST/AND/OR/XOR/ADD/SUB r64, imm8: REX.W + 83 /r (modrm reg field)
        // MOV r/m64, imm32: REX.W + C7 /0
        // 2-byte with imm8: 0x04, 0x0C, 0x24, 0x2C, 0x34, 0x3C, 0x6A, 0xEB, 0xCD
        if (opcode == 0x04 || opcode == 0x0C || opcode == 0x24 ||
            opcode == 0x2C || opcode == 0x34 || opcode == 0x3C ||
            opcode == 0x6A || opcode == 0xEB || opcode == 0xCD) {
            return 2;
        }

        // NOP: 0x90
        // HLT: 0xF4
        // CPUID: 0x0F 0xA2 (two-byte)
        // PUSHAD: 0x60, POPAD: 0x61
        if (opcode == 0x60 || opcode == 0x61) {
            return 1;
        }

        // CLC/STC/CLD/STD: 0xF8/0xF9/0xFC/0xFD
        if (opcode == 0xF8 || opcode == 0xF9 || opcode == 0xFC || opcode == 0xFD) {
            return 1;
        }

        // CMC: 0xF5
        if (opcode == 0xF5) {
            return 1;
        }

        // POP ES: 0x07, PUSH DS: 0x1E
        if (opcode == 0x07 || opcode == 0x1E) {
            return 1;
        }

        // JO rel8: 0x70
        if (opcode == 0x70) {
            return 2;
        }

        // MOV AL, moffs8: 0xA0
        if (opcode == 0xA0) {
            return 3;
        }

        // OUT imm8: 0xE6/0xE7
        if (opcode == 0xE6 || opcode == 0xE7) {
            return 2;
        }

        // MOV r32, imm32: B8..BF (no REX.W)
        if ((opcode & 0xF8) == 0xB8 && rexW == 0) {
            return 5;
        }

        // GROUP 3 byte: 0xF6 /r
        if (opcode == 0xF6 && rexW == 0) {
            return 2 + modrmDispLen(core, pc);
        }

        // ADC/SUB/SBB/XOR/CMP r/m8, r8: 0x10/0x18/0x28/0x30/0x38 /r
        if (opcode == 0x10 || opcode == 0x18 || opcode == 0x28 || opcode == 0x30 || opcode == 0x38) {
            return 2 + modrmDispLen(core, pc);
        }

        // Shift by 1: 0xD0-D3 /r
        if (opcode == 0xD0 || opcode == 0xD1 || opcode == 0xD2 || opcode == 0xD3) {
            return 2 + modrmDispLen(core, pc);
        }

        // OR/XCHG r/m8, r8: 0x08/0x86 /r
        if (opcode == 0x08 || opcode == 0x86) {
            return 2 + modrmDispLen(core, pc);
        }

        // IMUL r32, r/m32, imm32: 0x69 /r imm32
        if (opcode == 0x69) {
            return 6 + modrmDispLen(core, pc);
        }

        // INS: 0x6D
        if (opcode == 0x6D) {
            return 1;
        }

        // ADD AL, imm8: 0x04
        if (opcode == 0x04) {
            return 2;
        }

        // POP DS: 0x17
        if (opcode == 0x17) {
            return 1;
        }

        // Single byte
        return 1;
    }

    private int twoByteLen(X86Core core, int op2, int pc, int rexW) {
        // MOVSXD r64, r/m32: REX.W + 0x0F 0x63
        if ((op2 & 0xF8) == 0x60 && rexW != 0) {
            // ModR/M at pc
            return 3 + modrmDispLen(core, pc);
        }
        // Jcc near: 0x0F 0x80..0x8F
        if ((op2 & 0xF0) == 0x80) {
            return 6; // 0F(1) + op2(1) + rel32(4)
        }
        // CPUID: 0x0F 0xA2
        if (op2 == 0xA2) {
            return 2;
        }
        // MOVZX/MOVSX: 0x0F B6/B7/BE/BF
        if ((op2 & 0xFC) == 0xB6 || (op2 & 0xFC) == 0xBE) {
            return 3 + modrmDispLen(core, pc);
        }
        // LEA: 0x0F 0x8D (actually 0x8D is one-byte, not two-byte)
        // CALL r/m64: 0x0F 0xFF /2
        return 2 + modrmDispLen(core, pc);
    }

    private int modrmDispLen(X86Core core, int pc) {
        int modrm = core.readMem8(pc);
        int mod = (modrm >> 6) & 0x03;
        int rm = modrm & 0x07;
        int len = 1;
        if (mod == 1) len += 1;
        else if (mod == 2) len += 4;
        else if (mod == 0 && rm == 6) len += 4;
        if (mod != 3 && rm == 4) {
            // SIB byte
            len += 1;
            int sib = core.readMem8(pc + len - 1);
            int sibMod = (sib >> 6) & 0x03;
            if (sibMod == 0 && (sib & 0x07) == 5) len += 4;
            else if (sibMod == 1) len += 1;
            else if (sibMod == 2) len += 4;
        }
        return len;
    }

    /* ---------------- ModR/M decode for 64-bit mode ---------------- */

    private static class ModRm64 {
        int mod, regField, rmField, dispSize, disp, effAddr;
        long rmVal64;
        int rmVal;
        int regVal;
        int len;
        boolean regIs64;
        boolean rmIs64;
    }

    private ModRm64 decodeModRm64(X86Core core, int pc, boolean operandSize64) {
        int modrm = core.readMem8(pc + 1);
        int mod = (modrm >> 6) & 0x03;
        int regField = (modrm >> 3) & 0x07;
        int rmField = modrm & 0x07;
        int dispSize = 0;
        long disp = 0;
        int len = 2;

        if (mod == 1) {
            dispSize = 1;
            disp = (byte) core.readMem8(pc + 2);
        } else if (mod == 2) {
            dispSize = 4;
            disp = core.readMem32(pc + 2);
        } else if (mod == 0 && rmField == 6) {
            // RIP-relative: disp32
            dispSize = 4;
            disp = core.readMem32(pc + 2);
        }

        int addrBase = modrmAddr64(core, rmField, (int) disp, mod, rexR, rexB);
        boolean useSib = (mod != 3 && rmField == 4);
        int sibLen = 0;
        int sibIndex = 0, sibBase = 0, sibScale = 0;

        if (useSib) {
            int sib = core.readMem8(pc + 2);
            sibScale = (sib >> 6) & 0x03;
            sibIndex = (sib >> 3) & 0x07;
            sibBase = sib & 0x07;
            sibLen = 1;
            len++;
            if (mod == 0 && sibBase == 5) {
                dispSize = 4;
                disp = core.readMem32(pc + 2 + sibLen);
                len += 4;
            } else if (mod == 1) {
                dispSize = 1;
                disp = (byte) core.readMem8(pc + 2 + sibLen);
                len += 1;
            } else if (mod == 2) {
                dispSize = 4;
                disp = core.readMem32(pc + 2 + sibLen);
                len += 4;
            }
        }

        ModRm64 m = new ModRm64();
        m.mod = mod;
        m.regField = regField;
        m.rmField = rmField;
        m.dispSize = dispSize;
        m.disp = (int) disp;
        m.len = len;

        m.regIs64 = operandSize64;
        m.rmIs64 = operandSize64;

        if (mod == 3) {
            // Register direct
            m.rmVal = core.getReg(rmIndex64(rmField));
            m.regVal = core.getReg(regFieldIndex64(regField));
            m.rmVal64 = core.getReg64(rmIndex64(rmField));
        } else {
            m.effAddr = addrBase + (int) disp;
            m.rmVal = core.readMem32(m.effAddr);
            m.regVal = core.getReg(regFieldIndex64(regField));
            m.rmVal64 = core.readMem64(m.effAddr);
        }
        return m;
    }

    private int modrmAddr64(X86Core core, int rm, int disp, int mod, int rexR, int rexB) {
        // Simplified: ignore REX.X for now, just handle base registers
        if (mod == 0 && rm == 6) return disp;
        switch (rm) {
            case 0: return (int) (core.getReg64(REG_RBX) + core.getReg64(REG_RSI) + disp);
            case 1: return (int) (core.getReg64(REG_RBX) + core.getReg64(REG_RDI) + disp);
            case 2: return (int) (core.getReg64(REG_RBP) + core.getReg64(REG_RSI) + disp);
            case 3: return (int) (core.getReg64(REG_RBP) + core.getReg64(REG_RDI) + disp);
            case 4: return (int) (core.getReg64(REG_RSI) + disp);
            case 5: return (int) (core.getReg64(REG_RDI) + disp);
            case 6: return (mod == 0) ? disp : (int) (core.getReg64(REG_RBP) + disp);
            case 7: return (int) (core.getReg64(REG_RBX) + disp);
        }
        return disp;
    }

    /* ---------------- opcode execution ---------------- */

    private void executeOpcode(X86Core core, int opcode, int pc) throws Exception {
        // MOV r64, imm64: REX.W + B8..BF
        if ((opcode & 0xF8) == 0xB8 && rexW != 0) {
            int reg = rmIndex64(opcode & 0x07);
            long imm = core.readMem64(pc + 2);
            core.setReg64(reg, imm);
            return;
        }

        // PUSH r64: REX.W + 50..57
        if ((opcode & 0xF8) == 0x50 && rexW != 0) {
            int reg = rmIndex64(opcode & 0x07);
            core.push64(core.getReg64(reg));
            return;
        }

        // POP r64: REX.W + 58..5F
        if ((opcode & 0xF8) == 0x58 && rexW != 0) {
            int reg = rmIndex64(opcode & 0x07);
            core.setReg64(reg, core.pop64());
            return;
        }

        // INC r64: REX.W + 40..47
        if ((opcode & 0xF8) == 0x40 && rexW != 0) {
            int reg = rmIndex64(opcode & 0x07);
            long val = core.getReg64(reg) + 1;
            core.setReg64(reg, val);
            setFlagsArith64(core, val, core.getReg64(reg) - 1, 1, false);
            return;
        }

        // DEC r64: REX.W + 48..4F
        if ((opcode & 0xF8) == 0x48 && rexW != 0) {
            int reg = rmIndex64(opcode & 0x07);
            long val = core.getReg64(reg) - 1;
            core.setReg64(reg, val);
            setFlagsArith64(core, val, core.getReg64(reg) + 1, 1, true);
            return;
        }

        // MOV r64, r/m64: REX.W + 0x89 (reg to rm) / 0x8B (rm to reg)
        if (rexW != 0) {
            if (opcode == 0x89) {
                ModRm64 m = decodeModRm64(core, pc, true);
                core.writeMem64(m.effAddr, core.getReg64(regFieldIndex64(m.regField)));
                return;
            }
            if (opcode == 0x8B) {
                ModRm64 m = decodeModRm64(core, pc, true);
                core.setReg64(regFieldIndex64(m.regField), m.rmVal64);
                return;
            }
            if (opcode == 0x88) {
                ModRm64 m = decodeModRm64(core, pc, false);
                core.writeMem8(m.effAddr, core.getReg(regFieldIndex64(m.regField)) & 0xFF);
                return;
            }
            if (opcode == 0x8A) {
                ModRm64 m = decodeModRm64(core, pc, false);
                core.setReg(regFieldIndex64(m.regField), core.readMem8(m.effAddr) & 0xFF);
                return;
            }
            if (opcode == 0x81 && mGrp1Reg(core, pc) == 7) {
                // CMP r/m64, imm32: REX.W + 81 /7
                ModRm64 m = decodeModRm64(core, pc, true);
                long imm = core.readMem32(pc + m.len) & 0xFFFFFFFFL;
                long src = m.rmVal64;
                long res = src - imm;
                setFlagsSub64(core, src, imm, res);
                return;
            }
            if (opcode == 0x83 && mGrp1Reg(core, pc) == 7) {
                // CMP r/m64, imm8: REX.W + 83 /7
                ModRm64 m = decodeModRm64(core, pc, true);
                long imm = (byte) core.readMem8(pc + m.len);
                long src = m.rmVal64;
                long res = src - imm;
                setFlagsSub64(core, src, imm, res);
                return;
            }
            if (opcode == 0xC7 && mGrp1Reg(core, pc) == 0) {
                // MOV r/m64, imm32: REX.W + C7 /0
                ModRm64 m = decodeModRm64(core, pc, true);
                long imm = core.readMem32(pc + m.len) & 0xFFFFFFFFL;
                core.writeMem64(m.effAddr, imm);
                return;
            }
        }

        // CMP r32, imm32: 0x81 /7 (no REX.W)
        if (opcode == 0x81 && rexW == 0 && mGrp1Reg(core, pc) == 7) {
            ModRm64 m = decodeModRm64(core, pc, false);
            long imm = core.readMem32(pc + m.len) & 0xFFFFFFFFL;
            long src = m.rmVal & 0xFFFFFFFFL;
            long res = src - imm;
            setFlagsSub32(core, src, imm, res);
            return;
        }

        // PUSH imm32: 0x68
        if (opcode == 0x68) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            core.push64(imm);
            return;
        }

        // PUSH imm8: 0x6A
        if (opcode == 0x6A) {
            long imm = (byte) core.readMem8(pc + 1);
            core.push64(imm);
            return;
        }

        // PUSHAD: 0x60
        if (opcode == 0x60) {
            core.push64(core.getReg64(REG_RAX));
            core.push64(core.getReg64(REG_RCX));
            core.push64(core.getReg64(REG_RDX));
            core.push64(core.getReg64(REG_RBX));
            core.push64(core.getReg64(REG_RSP) + 32); // original ESP
            core.push64(core.getReg64(REG_RBP));
            core.push64(core.getReg64(REG_RSI));
            core.push64(core.getReg64(REG_RDI));
            return;
        }

        // POPAD: 0x61
        if (opcode == 0x61) {
            core.setReg64(REG_RDI, core.pop64());
            core.setReg64(REG_RSI, core.pop64());
            core.setReg64(REG_RBP, core.pop64());
            core.pop64(); // skip original ESP
            core.setReg64(REG_RBX, core.pop64());
            core.setReg64(REG_RDX, core.pop64());
            core.setReg64(REG_RCX, core.pop64());
            core.setReg64(REG_RAX, core.pop64());
            return;
        }

        // NOP: 0x90
        if (opcode == 0x90) {
            return;
        }

        // HLT: 0xF4
        if (opcode == 0xF4) {
            return;
        }

        // JMP near rel8: 0xEB
        if (opcode == 0xEB) {
            int rel = (byte) core.readMem8(pc + 1);
            core.setPC64(pc + 2 + rel);
            return;
        }

        // JMP near rel32: 0xE9
        if (opcode == 0xE9) {
            int rel = core.readMem32(pc + 1);
            core.setPC64(pc + 5 + rel);
            return;
        }

        // CALL near rel32: 0xE8
        if (opcode == 0xE8) {
            int rel = core.readMem32(pc + 1);
            core.push64(pc + 5);
            core.setPC64(pc + 5 + rel);
            return;
        }

        // RET near: 0xC3
        if (opcode == 0xC3) {
            core.setPC64(core.pop64());
            return;
        }

        // ADD r/m64, r64: REX.W + 0x01
        // SUB r/m64, r64: REX.W + 0x29
        // AND r/m64, r64: REX.W + 0x21
        // OR  r/m64, r64: REX.W + 0x09
        // XOR r/m64, r64: REX.W + 0x31
        if (rexW != 0) {
            switch (opcode) {
                case 0x01: // ADD
                    modrm_arith64(core, pc, true, false, (a, b) -> a + b, true);
                    return;
                case 0x03: // ADD r64, r/m64
                    modrm_arith64(core, pc, true, true, (a, b) -> a + b, true);
                    return;
                case 0x29: // SUB r/m64, r64
                    modrm_arith64(core, pc, true, false, (a, b) -> a - b, true);
                    return;
                case 0x2B: // SUB r64, r/m64
                    modrm_arith64(core, pc, true, true, (a, b) -> a - b, true);
                    return;
                case 0x21: // AND r/m64, r64
                    modrm_arith64(core, pc, true, false, (a, b) -> a & b, false);
                    return;
                case 0x23: // AND r64, r/m64
                    modrm_arith64(core, pc, true, true, (a, b) -> a & b, false);
                    return;
                case 0x09: // OR r/m64, r64
                    modrm_arith64(core, pc, true, false, (a, b) -> a | b, false);
                    return;
                case 0x0B: // OR r64, r/m64
                    modrm_arith64(core, pc, true, true, (a, b) -> a | b, false);
                    return;
                case 0x31: // XOR r/m64, r64
                    modrm_arith64(core, pc, true, false, (a, b) -> a ^ b, false);
                    return;
                case 0x33: // XOR r64, r/m64
                    modrm_arith64(core, pc, true, true, (a, b) -> a ^ b, false);
                    return;
                case 0x39: // CMP r/m64, r64
                    modrm_cmp64(core, pc, true, false);
                    return;
                case 0x3B: // CMP r64, r/m64
                    modrm_cmp64(core, pc, true, true);
                    return;
                case 0x85: // TEST r/m64, r64
                    modrm_test64(core, pc, true, false);
                    return;
                case 0x87: // XCHG r/m64, r64
                    modrm_xchg64(core, pc, true);
                    return;
            }
        }

        // LEA r64, m: 0x8D (no REX.W needed, but works with it)
        if (opcode == 0x8D) {
            ModRm64 m = decodeModRm64(core, pc, false);
            core.setReg64(regFieldIndex64(m.regField), m.effAddr & 0xFFFFFFFFL);
            return;
        }

        /* ---------------- Accumulator immediate (no ModR/M) ---------------- */

        // ADD EAX, imm32: 0x05
        if (opcode == 0x05) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            long res = val + imm;
            core.setReg64(REG_RAX, res);
            setFlagsArith64(core, res, val, imm, false);
            return;
        }

        // OR EAX, imm32: 0x0D
        if (opcode == 0x0D) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            long res = val | imm;
            core.setReg64(REG_RAX, res);
            core.setFlag(FLAG_CF, false);
            core.setFlag(FLAG_OF, false);
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        // ADC EAX, imm32: 0x15 (simplified as ADD)
        if (opcode == 0x15) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            long res = val + imm;
            core.setReg64(REG_RAX, res);
            setFlagsArith64(core, res, val, imm, false);
            return;
        }

        // SBB EAX, imm32: 0x1D (simplified as SUB)
        if (opcode == 0x1D) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            long res = val - imm;
            core.setReg64(REG_RAX, res);
            setFlagsSub64(core, val, imm, res);
            return;
        }

        // AND EAX, imm32: 0x25
        if (opcode == 0x25) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            long res = val & imm;
            core.setReg64(REG_RAX, res);
            core.setFlag(FLAG_CF, false);
            core.setFlag(FLAG_OF, false);
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        // SUB EAX, imm32: 0x2D
        if (opcode == 0x2D) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            long res = val - imm;
            core.setReg64(REG_RAX, res);
            setFlagsSub64(core, val, imm, res);
            return;
        }

        // XOR EAX, imm32: 0x35
        if (opcode == 0x35) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            long res = val ^ imm;
            core.setReg64(REG_RAX, res);
            core.setFlag(FLAG_CF, false);
            core.setFlag(FLAG_OF, false);
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        // CMP EAX, imm32: 0x3D
        if (opcode == 0x3D) {
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            setFlagsSub64(core, val, imm, val - imm);
            return;
        }

        // ADC/SUB/SBB/XOR/CMP r/m8, r8: 0x10/0x18/0x28/0x30/0x38 /r
        if (opcode == 0x10 || opcode == 0x18 || opcode == 0x28 || opcode == 0x30 || opcode == 0x38) {
            ModRm64 m = decodeModRm64(core, pc, false);
            long src = m.regVal & 0xFF;
            long dest = m.rmVal & 0xFF;
            long res;
            switch (opcode) {
                case 0x10: res = dest + src + (core.getFlag(FLAG_CF) ? 1 : 0); break;
                case 0x18: res = dest - src; break;
                case 0x28: res = dest - src; break;
                case 0x30: res = dest ^ src; break;
                case 0x38: res = dest - src; break;
                default: res = 0;
            }
            if (m.mod == 3) {
                core.setReg(regFieldIndex64(m.regField), (int) res);
            } else if (opcode != 0x38) {
                core.writeMem8(m.effAddr, (int) res);
            }
            core.setFlag(FLAG_ZF, (res & 0xFF) == 0);
            core.setFlag(FLAG_SF, (res & 0x80) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_AF, false);
            if (opcode == 0x10) {
                core.setFlag(FLAG_CF, res > 0xFF);
                core.setFlag(FLAG_OF, (((dest ^ res) & (src ^ res)) & 0x80) != 0);
            } else if (opcode == 0x18) {
                core.setFlag(FLAG_CF, dest < src);
                core.setFlag(FLAG_OF, (((dest ^ src) & (dest ^ res)) & 0x80) != 0);
            } else if (opcode == 0x38) {
                core.setFlag(FLAG_CF, dest < src);
                core.setFlag(FLAG_OF, (((dest ^ src) & (dest ^ res)) & 0x80) != 0);
            } else {
                core.setFlag(FLAG_CF, res > 0xFF);
                core.setFlag(FLAG_OF, (((dest ^ res) & (src ^ res)) & 0x80) != 0);
            }
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        // MOV AL, moffs8: 0xA0
        if (opcode == 0xA0) {
            int addr = core.readMem16(pc + 1);
            core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (core.readMem8(addr) & 0xFF));
            return;
        }

        /* ---------------- IMUL r32, r/m32, imm32: 0x69 /r ---------------- */

        if (opcode == 0x69) {
            ModRm64 m = decodeModRm64(core, pc, false);
            long imm = core.readMem32(pc + m.len) & 0xFFFFFFFFL;
            long src = m.rmVal & 0xFFFFFFFFL;
            long dest = core.getReg(regFieldIndex64(m.regField)) & 0xFFFFFFFFL;
            long res = dest * src;
            core.setReg64(regFieldIndex64(m.regField), res);
            core.setFlag(FLAG_CF, (res >>> 32) != 0);
            core.setFlag(FLAG_OF, (res >>> 32) != 0);
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        /* ---------------- ADD AL, imm8: 0x04 ---------------- */

        if (opcode == 0x04) {
            long imm = core.readMem8(pc + 1) & 0xFF;
            long val = core.getReg64(REG_RAX) & 0xFF;
            long res = val + imm;
            core.setReg64(REG_RAX, (core.getReg64(REG_RAX) & ~0xFFL) | (res & 0xFFL));
            core.setFlag(FLAG_ZF, (res & 0xFF) == 0);
            core.setFlag(FLAG_SF, (res & 0x80) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_CF, res > 0xFF);
            core.setFlag(FLAG_OF, (((val ^ res) & (imm ^ res)) & 0x80) != 0);
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        /* ---------------- POP DS: 0x17 ---------------- */

        if (opcode == 0x17) {
            core.pop64();
            return;
        }

        /* ---------------- INS: 0x6D (input string) - simplified ---------------- */

        if (opcode == 0x6D) {
            int dx = core.getReg(REG_EDX) & 0xFFFF;
            int edi = core.getReg(REG_EDI);
            core.writeMem8(edi, core.readPort(dx));
            if (core.getFlag(FLAG_DF)) {
                core.setReg(REG_EDI, edi - 1);
            } else {
                core.setReg(REG_EDI, edi + 1);
            }
            return;
        }

        // MOV r32, imm32: B8..BF (no REX.W)
        if ((opcode & 0xF8) == 0xB8 && rexW == 0) {
            int reg = opcode & 0x07;
            long imm = core.readMem32(pc + 1) & 0xFFFFFFFFL;
            core.setReg64(reg, imm);
            return;
        }

        /* ---------------- ADD r/m8, r8: 0x00 /r ---------------- */

        if (opcode == 0x00) {
            ModRm64 m = decodeModRm64(core, pc, false);
            long src = m.regVal & 0xFF;
            long dest = m.rmVal & 0xFF;
            long res = dest + src;
            if (m.mod == 3) {
                core.setReg(regFieldIndex64(m.regField), (int) res);
            } else {
                core.writeMem8(m.effAddr, (int) res);
            }
            core.setFlag(FLAG_ZF, (res & 0xFF) == 0);
            core.setFlag(FLAG_SF, (res & 0x80) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_CF, res > 0xFF);
            core.setFlag(FLAG_OF, (((dest ^ res) & (src ^ res)) & 0x80) != 0);
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        /* ---------------- SBB r/m32, r32: 0x18 /r ---------------- */

        if (opcode == 0x18) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int dest = core.getReg(regFieldIndex64(m.regField));
            int src = m.rmVal;
            int res = dest - src;
            core.setReg(regFieldIndex64(m.regField), res);
            setFlagsSub32(core, dest, src, res);
            return;
        }

        /* ---------------- 32-bit operand operations (no REX.W) ---------------- */

        // MOV r32, r/m32: 0x8B /r (no REX.W)
        // MOV r/m32, r32: 0x89 /r (no REX.W)
        // MOV r/m8, r8: 0x88 /r (no REX.W)
        // MOV r8, r/m8: 0x8A /r (no REX.W)
        if (rexW == 0) {
            switch (opcode) {
                case 0x89: // MOV r/m32, r32
                    modrm_arith32(core, pc, false, false, (a, b) -> b, true);
                    return;
                case 0x8B: // MOV r32, r/m32
                    modrm_arith32(core, pc, false, true, (a, b) -> b, true);
                    return;
                case 0x88: // MOV r/m8, r8
                    modrm_arith32(core, pc, false, false, (a, b) -> b & 0xFF, false);
                    return;
                case 0x8A: // MOV r8, r/m8
                    modrm_arith32(core, pc, false, true, (a, b) -> b & 0xFF, false);
                    return;
            }
        }

        // ADD/SUB/AND/OR/XOR/CMP r/m32, r32 and r32, r/m32: no REX.W + 0x01/0x03/0x29/0x2B/0x21/0x23/0x09/0x0B/0x31/0x33/0x39/0x3B
        if (rexW == 0) {
            switch (opcode) {
                case 0x01: // ADD r/m32, r32
                    modrm_arith32(core, pc, true, false, (a, b) -> a + b, true);
                    return;
                case 0x03: // ADD r32, r/m32
                    modrm_arith32(core, pc, true, true, (a, b) -> a + b, true);
                    return;
                case 0x29: // SUB r/m32, r32
                    modrm_arith32(core, pc, true, false, (a, b) -> a - b, true);
                    return;
                case 0x2B: // SUB r32, r/m32
                    modrm_arith32(core, pc, true, true, (a, b) -> a - b, true);
                    return;
                case 0x21: // AND r/m32, r32
                    modrm_arith32(core, pc, true, false, (a, b) -> a & b, false);
                    return;
                case 0x23: // AND r32, r/m32
                    modrm_arith32(core, pc, true, true, (a, b) -> a & b, false);
                    return;
                case 0x09: // OR r/m32, r32
                    modrm_arith32(core, pc, true, false, (a, b) -> a | b, false);
                    return;
                case 0x0B: // OR r32, r/m32
                    modrm_arith32(core, pc, true, true, (a, b) -> a | b, false);
                    return;
                case 0x31: // XOR r/m32, r32
                    modrm_arith32(core, pc, true, false, (a, b) -> a ^ b, false);
                    return;
                case 0x33: // XOR r32, r/m32
                    modrm_arith32(core, pc, true, true, (a, b) -> a ^ b, false);
                    return;
                case 0x39: // CMP r/m32, r32
                    modrm_cmp32(core, pc, true, false);
                    return;
                case 0x3B: // CMP r32, r/m32
                    modrm_cmp32(core, pc, true, true);
                    return;
                case 0x85: // TEST r/m32, r32
                    modrm_test32(core, pc, true, false);
                    return;
                case 0x87: // XCHG r/m32, r32
                    modrm_xchg32(core, pc);
                    return;
            }
        }

        // PUSH r32: 0x50..0x57 (no REX.W)
        if (rexW == 0 && (opcode & 0xF8) == 0x50) {
            core.push64(core.getReg(opcode & 0x07));
            return;
        }

        // POP r32: 0x58..0x5F (no REX.W)
        if (rexW == 0 && (opcode & 0xF8) == 0x58) {
            core.setReg(opcode & 0x07, (int) core.pop64());
            return;
        }

        // INC/DEC r32: 0x40..0x47 / 0x48..0x4F (no REX.W)
        if (rexW == 0 && (opcode & 0xF8) == 0x40) {
            int reg = opcode & 0x07;
            int val = core.getReg(reg) + 1;
            core.setReg(reg, val);
            setFlagsArith32(core, val, core.getReg(reg) - 1, 1, false);
            return;
        }
        if (rexW == 0 && (opcode & 0xF8) == 0x48) {
            int reg = opcode & 0x07;
            int val = core.getReg(reg) - 1;
            core.setReg(reg, val);
            setFlagsArith32(core, val, core.getReg(reg) + 1, 1, true);
            return;
        }

        /* ---------------- GROUP 1: 0x80 /r, 0x81 /r, 0x83 /r ---------------- */

        // GROUP 1 with imm8: 0x83 /r
        if (opcode == 0x83) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int reg = m.regField;
            long imm = (byte) core.readMem8(pc + m.len);
            long src = m.rmVal & 0xFFFFFFFFL;
            switch (reg) {
                case 0: // ADD
                    modrm_imm32(core, m, src + imm, true);
                    return;
                case 1: // OR
                    modrm_imm32(core, m, src | imm, false);
                    return;
                case 2: // ADC (simplified as ADD)
                    modrm_imm32(core, m, src + imm, true);
                    return;
                case 3: // SBB (simplified as SUB)
                    modrm_imm32(core, m, src - imm, true);
                    return;
                case 4: // AND
                    modrm_imm32(core, m, src & imm, false);
                    return;
                case 5: // SUB
                    modrm_imm32(core, m, src - imm, true);
                    return;
                case 6: // XOR
                    modrm_imm32(core, m, src ^ imm, false);
                    return;
                case 7: // CMP
                    setFlagsSub32(core, src, imm, src - imm);
                    return;
            }
        }

        // GROUP 1 with imm32: 0x81 /r
        if (opcode == 0x81) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int reg = m.regField;
            long imm = core.readMem32(pc + m.len) & 0xFFFFFFFFL;
            long src = m.rmVal & 0xFFFFFFFFL;
            switch (reg) {
                case 0: // ADD
                    modrm_imm32(core, m, src + imm, true);
                    return;
                case 1: // OR
                    modrm_imm32(core, m, src | imm, false);
                    return;
                case 2: // ADC
                    modrm_imm32(core, m, src + imm, true);
                    return;
                case 3: // SBB
                    modrm_imm32(core, m, src - imm, true);
                    return;
                case 4: // AND
                    modrm_imm32(core, m, src & imm, false);
                    return;
                case 5: // SUB
                    modrm_imm32(core, m, src - imm, true);
                    return;
                case 6: // XOR
                    modrm_imm32(core, m, src ^ imm, false);
                    return;
                case 7: // CMP
                    setFlagsSub32(core, src, imm, src - imm);
                    return;
            }
        }

        /* ---------------- GROUP 2: 0xC0 /r, 0xC1 /r shifts ---------------- */

        // SHIFT r/m32, imm8: 0xC0 /r (no REX.W)
        // SHIFT r/m32, imm8: 0xC1 /r (REX.W)
        if ((opcode == 0xC0 || opcode == 0xC1) && (rexW != 0 || opcode == 0xC0)) {
            boolean operand64 = (rexW != 0);
            ModRm64 m = decodeModRm64(core, pc, operand64);
            int shift = m.regField;
            if (shift == 2 || shift == 3 || shift == 6) {
                throw unknownOpcode(core, String.format("opcode 0x%02X /%d", opcode, shift));
            }
            int count = core.readMem8(pc + m.len) & (operand64 ? 0x3F : 0x1F);
            long val = operand64 ? m.rmVal64 : (m.rmVal & 0xFFFFFFFFL);
            long res = val;
            if (count != 0) {
                switch (shift) {
                    case 0: res = operand64 ? Long.rotateLeft(val, count) : rotateLeft32((int) val, count); break;
                    case 1: res = operand64 ? Long.rotateRight(val, count) : rotateRight32((int) val, count); break;
                    case 4: res = val << count; break;
                    case 5: res = val >>> count; break;
                    case 7: res = val >> count; break;
                }
            }
            if (operand64) {
                core.writeMem64(m.effAddr, res);
            } else {
                core.writeMem32(m.effAddr, (int) res);
            }
            if (count == 1) {
                core.setFlag(FLAG_CF, (val & (operand64 ? 0x8000000000000000L : 0x80000000L)) != 0);
            } else {
                core.setFlag(FLAG_CF, false);
            }
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & (operand64 ? 0x8000000000000000L : 0x80000000L)) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        /* ---------------- GROUP 4: 0xFE /r ---------------- */

        // INC/DEC r/m8: 0xFE /r
        if (opcode == 0xFE) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int reg = m.regField;
            if (reg > 1) {
                throw unknownOpcode(core, String.format("opcode 0xFE /%d", reg));
            }
            long val = m.rmVal & 0xFF;
            if (reg == 0) {
                val = (val + 1) & 0xFF;
                core.writeMem8(m.effAddr, (int) val);
                core.setFlag(FLAG_ZF, val == 0);
                core.setFlag(FLAG_SF, (val & 0x80) != 0);
                core.setFlag(FLAG_PF, parity((int) val));
                core.setFlag(FLAG_AF, false);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            } else if (reg == 1) {
                val = (val - 1) & 0xFF;
                core.writeMem8(m.effAddr, (int) val);
                core.setFlag(FLAG_ZF, val == 0);
                core.setFlag(FLAG_SF, (val & 0x80) != 0);
                core.setFlag(FLAG_PF, parity((int) val));
                core.setFlag(FLAG_AF, false);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            }
            return;
        }

        /* ---------------- GROUP 5: 0xFF /r ---------------- */

        // INC/DEC/CALL/JMP/PUSH r/m32: 0xFF /r
        if (opcode == 0xFF) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int reg = m.regField;
            long val = m.rmVal & 0xFFFFFFFFL;
            switch (reg) {
                case 0: // INC r/m32
                    val = val + 1;
                    core.writeMem32(m.effAddr, (int) val);
                    setFlagsArith32(core, (int) val, (int) (val - 1), 1, false);
                    return;
                case 1: // DEC r/m32
                    val = val - 1;
                    core.writeMem32(m.effAddr, (int) val);
                    setFlagsArith32(core, (int) val, (int) (val + 1), 1, true);
                    return;
                case 2: // CALL r/m32
                    core.push64(pc + m.len);
                    core.setPC64(val);
                    return;
                case 3: // CALL r/m32 (far) - simplified
                    core.push64(pc + m.len);
                    core.setPC64(val);
                    return;
                case 4: // JMP r/m32
                    core.setPC64(val);
                    return;
                case 5: // JMP far - simplified
                    core.setPC64(val);
                    return;
                case 6: // PUSH r/m32
                    core.push64(val);
                    return;
            }
        }

        // STOSD: store EAX to [RDI], adjust RDI
        if (opcode == 0xAB) {
            int edi = core.getReg(REG_EDI);
            core.writeMem32(edi, core.getReg(REG_EAX));
            if (core.getFlag(FLAG_DF)) {
                core.setReg(REG_EDI, edi - 4);
            } else {
                core.setReg(REG_EDI, edi + 4);
            }
            return;
        }

        /* ---------------- POP DS: 0x17 ---------------- */

        if (opcode == 0x17) {
            // POP DS - simplified, just discard top of stack
            core.pop64();
            return;
        }

        /* ---------------- INS: 0x6D (input string) - simplified ---------------- */

        if (opcode == 0x6D) {
            // INS: input from DX port to [RDI], adjust RDI
            int dx = core.getReg(REG_EDX) & 0xFFFF;
            int edi = core.getReg(REG_EDI);
            core.writeMem8(edi, core.readPort(dx));
            if (core.getFlag(FLAG_DF)) {
                core.setReg(REG_EDI, edi - 1);
            } else {
                core.setReg(REG_EDI, edi + 1);
            }
            return;
        }

        /* ---------------- IN instructions ---------------- */

        // IN AL, imm8: 0xE4
        if (opcode == 0xE4) {
            int port = core.readMem8(pc + 1) & 0xFF;
            core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (core.readPort(port) & 0xFF));
            return;
        }

        // IN AX/EAX, imm8: 0xE5
        if (opcode == 0xE5) {
            int port = core.readMem8(pc + 1) & 0xFF;
            int al = core.readPort(port) & 0xFF;
            int ah = core.readPort(port + 1) & 0xFF;
            core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (ah << 8) | al);
            return;
        }

        // IN AL, DX: 0xEC
        if (opcode == 0xEC) {
            int dx = core.getReg(REG_EDX) & 0xFFFF;
            core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFF) | (core.readPort(dx) & 0xFF));
            return;
        }

        // IN AX/EAX, DX: 0xED
        if (opcode == 0xED) {
            int dx = core.getReg(REG_EDX) & 0xFFFF;
            int al = core.readPort(dx) & 0xFF;
            int ah = core.readPort(dx + 1) & 0xFF;
            core.setReg(REG_EAX, (core.getReg(REG_EAX) & ~0xFFFF) | (ah << 8) | al);
            return;
        }

        // OUT imm8, AL: 0xE6
        if (opcode == 0xE6) {
            int port = core.readMem8(pc + 1) & 0xFF;
            core.writePort(port, (byte)(core.getReg(REG_EAX) & 0xFF));
            return;
        }

        // OUT imm8, AX/EAX: 0xE7
        if (opcode == 0xE7) {
            int port = core.readMem8(pc + 1) & 0xFF;
            core.writePort(port, (byte)(core.getReg(REG_EAX) & 0xFF));
            core.writePort(port + 1, (byte)((core.getReg(REG_EAX) >> 8) & 0xFF));
            return;
        }

        // OUT DX, AL: 0xEE
        if (opcode == 0xEE) {
            int dx = core.getReg(REG_EDX) & 0xFFFF;
            core.writePort(dx, (byte)(core.getReg(REG_EAX) & 0xFF));
            return;
        }

        // OUT DX, AX/EAX: 0xEF
        if (opcode == 0xEF) {
            int dx = core.getReg(REG_EDX) & 0xFFFF;
            core.writePort(dx, (byte)(core.getReg(REG_EAX) & 0xFF));
            core.writePort(dx + 1, (byte)((core.getReg(REG_EAX) >> 8) & 0xFF));
            return;
        }

        /* ---------------- LOCK prefix: 0xF0 ---------------- */

        // LOCK: atomic prefix, simplified as no-op
        if (opcode == 0xF0) {
            return;
        }

        /* ---------------- Flag instructions ---------------- */

        // CLC: 0xF8
        if (opcode == 0xF8) {
            core.setFlag(FLAG_CF, false);
            return;
        }

        // STC: 0xF9
        if (opcode == 0xF9) {
            core.setFlag(FLAG_CF, true);
            return;
        }

        // CLD: 0xFC
        if (opcode == 0xFC) {
            core.setFlag(FLAG_DF, false);
            return;
        }

        // STD: 0xFD
        if (opcode == 0xFD) {
            core.setFlag(FLAG_DF, true);
            return;
        }

        // CMC: 0xF5 (complement carry flag)
        if (opcode == 0xF5) {
            core.setFlag(FLAG_CF, !core.getFlag(FLAG_CF));
            return;
        }

        // PUSH DS: 0x1E
        if (opcode == 0x1E) {
            core.push64(0); // DS segment value (simplified as 0)
            return;
        }

        // POP ES: 0x07
        if (opcode == 0x07) {
            // POP ES - simplified, just discard
            return;
        }

        // JO rel8: 0x70
        if (opcode == 0x70) {
            int rel = (byte) core.readMem8(pc + 1);
            if (core.getFlag(FLAG_OF)) {
                core.setPC64(pc + 2 + rel);
            }
            return;
        }

        /* ---------------- Shift by 1: 0xD0-D3 ---------------- */

        // ROL/ROR/SHL/SHR/SAR r/m32/m8, 1: 0xD0-D1
        // ROL/ROR/SHL/SHR/SAR r/m32/m8, CL: 0xD2-D3
        if (opcode == 0xD0 || opcode == 0xD1 || opcode == 0xD2 || opcode == 0xD3) {
            boolean useCl = (opcode & 0x02) != 0;
            boolean operand16 = (opcode & 0x01) != 0;
            ModRm64 m = decodeModRm64(core, pc, !operand16);
            int shift = m.regField;
            if (shift == 2 || shift == 3 || shift == 6) {
                throw unknownOpcode(core, String.format("opcode 0x%02X /%d", opcode, shift));
            }
            int count = useCl ? core.getReg(REG_ECX) & 0x1F : 1;
            long val = operand16 ? m.rmVal64 : (m.rmVal & 0xFF);
            long res = val;
            if (count != 0) {
                switch (shift) {
                    case 0: res = Long.rotateLeft(val, count); break;
                    case 1: res = Long.rotateRight(val, count); break;
                    case 4: res = val << count; break;
                    case 5: res = val >>> count; break;
                    case 7: res = val >> count; break;
                }
            }
            if (operand16) {
                core.writeMem32(m.effAddr, (int) res);
            } else {
                core.writeMem8(m.effAddr, (int) res);
            }
            if (count == 1) {
                core.setFlag(FLAG_CF, (val & (operand16 ? 0x80000000L : 0x80L)) != 0);
            } else {
                core.setFlag(FLAG_CF, false);
            }
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & (operand16 ? 0x80000000L : 0x80L)) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        /* ---------------- OR/XCHG r/m8, r8: 0x08/0x86 /r ---------------- */

        if (opcode == 0x08 || opcode == 0x86) {
            ModRm64 m = decodeModRm64(core, pc, false);
            long src = m.regVal & 0xFF;
            long dest = m.rmVal & 0xFF;
            long res;
            if (opcode == 0x08) {
                res = dest | src;
            } else {
                res = src;
                core.setReg(regFieldIndex64(m.regField), (int) dest);
            }
            if (m.mod == 3) {
                core.setReg(regFieldIndex64(m.regField), (int) res);
            } else {
                core.writeMem8(m.effAddr, (int) res);
            }
            if (opcode == 0x08) {
                core.setFlag(FLAG_CF, false);
                core.setFlag(FLAG_OF, false);
                core.setFlag(FLAG_ZF, res == 0);
                core.setFlag(FLAG_SF, (res & 0x80) != 0);
                core.setFlag(FLAG_PF, parity((int) res));
                core.setFlag(FLAG_AF, false);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            }
            return;
        }

        // Two-byte escape
        if (opcode == 0x0F) {
            int op2 = core.readMem8(pc + 1);
            executeTwoByte(core, op2, pc + 2);
            return;
        }

        // TEST r/m64, imm32: 0xF7 /0
        if (opcode == 0xF7 && rexW != 0 && mGrp1Reg(core, pc) == 0) {
            ModRm64 m = decodeModRm64(core, pc, true);
            long imm = core.readMem32(pc + m.len) & 0xFFFFFFFFL;
            long res = m.rmVal64 & imm;
            core.setFlag(FLAG_CF, false);
            core.setFlag(FLAG_OF, false);
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        // SHL/SHR/ROL/ROR/SAR r/m64, imm8: REX.W + 0xC1 /4,/5,/0,/1,/7
        if (opcode == 0xC1 && rexW != 0) {
            ModRm64 m = decodeModRm64(core, pc, true);
            int shift = mGrp1Reg(core, pc);
            if (shift == 2 || shift == 3 || shift == 6) {
                throw unknownOpcode(core, String.format("opcode 0x%02X /%d", opcode, shift));
            }
            int count = core.readMem8(pc + m.len) & 0x3F;
            long val = m.rmVal64;
            long res = val;
            if (count != 0) {
                switch (shift) {
                    case 0: res = Long.rotateLeft(val, count); break;   // ROL
                    case 1: res = Long.rotateRight(val, count); break;  // ROR
                    case 4: res = val << count; break;                  // SHL
                    case 5: res = val >>> count; break;                 // SHR
                    case 7: res = val >> count; break;                  // SAR
                }
            }
            core.writeMem64(m.effAddr, res);
            if (count == 1) {
                core.setFlag(FLAG_CF, (val & 0x8000000000000000L) != 0);
                core.setFlag(FLAG_OF, ((val ^ res) & 0x8000000000000000L) != 0);
            } else {
                core.setFlag(FLAG_OF, false);
            }
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        // NOT r/m64: 0xF7 /2
        if (opcode == 0xF7 && rexW != 0 && mGrp1Reg(core, pc) == 2) {
            ModRm64 m = decodeModRm64(core, pc, true);
            core.writeMem64(m.effAddr, ~m.rmVal64);
            return;
        }

        // NEG r/m64: 0xF7 /3
        if (opcode == 0xF7 && rexW != 0 && mGrp1Reg(core, pc) == 3) {
            ModRm64 m = decodeModRm64(core, pc, true);
            long val = m.rmVal64;
            long res = -val;
            core.writeMem64(m.effAddr, res);
            core.setFlag(FLAG_ZF, res == 0);
            core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
            core.setFlag(FLAG_PF, parity((int) res));
            core.setFlag(FLAG_CF, val != 0);
            core.setFlag(FLAG_OF, val == 0x8000000000000000L);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        // GROUP 3 byte: 0xF6 /r (no REX.W)
        // TEST r/m8, imm8 (/0), NOT r/m8 (/2), NEG r/m8 (/3)
        if (opcode == 0xF6 && rexW == 0) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int reg = m.regField;
            if (reg != 0 && reg != 2 && reg != 3) {
                throw unknownOpcode(core, String.format("opcode 0xF6 /%d", reg));
            }
            if (reg == 0) { // TEST r/m8, imm8
                long imm = core.readMem8(pc + m.len) & 0xFF;
                long res = m.rmVal & imm;
                core.setFlag(FLAG_CF, false);
                core.setFlag(FLAG_OF, false);
                core.setFlag(FLAG_ZF, res == 0);
                core.setFlag(FLAG_SF, (res & 0x80) != 0);
                core.setFlag(FLAG_PF, parity((int) res));
                core.setFlag(FLAG_AF, false);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            } else if (reg == 2) { // NOT r/m8
                long val = m.rmVal & 0xFF;
                core.writeMem8(m.effAddr, (int) (~val & 0xFF));
            } else if (reg == 3) { // NEG r/m8
                long val = m.rmVal & 0xFF;
                long res = -val;
                core.writeMem8(m.effAddr, (int) (res & 0xFF));
                core.setFlag(FLAG_ZF, res == 0);
                core.setFlag(FLAG_SF, (res & 0x80) != 0);
                core.setFlag(FLAG_PF, parity((int) res));
                core.setFlag(FLAG_CF, val != 0);
                core.setFlag(FLAG_OF, val == 0x80);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            }
            return;
        }

        // MOV r/m64, imm8 sign-extended: REX.W + C6 /0
        if (rexW != 0 && opcode == 0xC6) {
            ModRm64 m = decodeModRm64(core, pc, false);
            long imm = (byte) core.readMem8(pc + m.len);
            core.writeMem64(m.effAddr, imm);
            return;
        }

        // MOV r/m8, imm8: C6 /0 (no REX.W)
        if (rexW == 0 && opcode == 0xC6) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int imm = core.readMem8(pc + m.len) & 0xFF;
            core.writeMem8(m.effAddr, imm);
            return;
        }

        // MOV r/m32, imm32: C7 /0 (no REX.W)
        if (rexW == 0 && opcode == 0xC7 && mGrp1Reg(core, pc) == 0) {
            ModRm64 m = decodeModRm64(core, pc, false);
            int imm = core.readMem32(pc + m.len);
            core.writeMem32(m.effAddr, imm);
            return;
        }

        // CDQE: 0x98 (sign-extend EAX to RAX)
        if (opcode == 0x98) {
            long val = core.getReg64(REG_RAX) & 0xFFFFFFFFL;
            val = (val << 32 >> 32);
            core.setReg64(REG_RAX, val);
            return;
        }

        // CQO: 0x99 (sign-extend RAX to RDX:RAX)
        if (opcode == 0x99) {
            long val = core.getReg64(REG_RAX);
            long sign = val >> 63;
            core.setReg64(REG_RDX, sign == 0 ? 0 : -1L);
            return;
        }

        // CLI: 0xFA (clear interrupt flag)
        if (opcode == 0xFA) {
            core.setFlag(FLAG_IF, false);
            return;
        }

        // STI: 0xFB (set interrupt flag)
        if (opcode == 0xFB) {
            core.setFlag(FLAG_IF, true);
            return;
        }

        // PUSHFD: 0x9C (push EFLAGS/RFLAGS)
        if (opcode == 0x9C) {
            core.push64(core.getRflags64());
            return;
        }

        // POPFD: 0x9D (pop EFLAGS/RFLAGS)
        if (opcode == 0x9D) {
            core.setRflags64(core.pop64());
            return;
        }

        throw unknownOpcode(core, String.format("opcode 0x%02X", opcode));
    }

    /* Return the ModR/M reg field (3 bits) for GROUP 1 opcode determination */
    private int mGrp1Reg(X86Core core, int pc) {
        int modrm = core.readMem8(pc + 1);
        return (modrm >> 3) & 0x07;
    }

    private void modrm_arith64(X86Core core, int pc, boolean word, boolean dirToReg,
                               BinOp op, boolean updateFlags) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        long destVal = dirToReg ? m.regVal & 0xFFFFFFFFL : m.rmVal64;
        long srcVal = dirToReg ? m.rmVal64 : m.regVal & 0xFFFFFFFFL;
        int r = (int) op.op(destVal, srcVal);
        if (dirToReg) {
            core.setReg64(regFieldIndex64(m.regField), r);
            if (updateFlags) setFlagsArith64(core, r, destVal, srcVal, false);
        } else {
            core.writeMem64(m.effAddr, r);
            if (updateFlags) setFlagsArith64(core, r, destVal, srcVal, false);
        }
    }

    private void modrm_cmp64(X86Core core, int pc, boolean word, boolean dirToReg) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        long destVal = dirToReg ? m.regVal & 0xFFFFFFFFL : m.rmVal64;
        long srcVal = dirToReg ? m.rmVal64 : m.regVal & 0xFFFFFFFFL;
        long res = destVal - srcVal;
        setFlagsSub64(core, destVal, srcVal, res);
    }

    private void modrm_test64(X86Core core, int pc, boolean word, boolean dirToReg) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        long destVal = dirToReg ? m.regVal & 0xFFFFFFFFL : m.rmVal64;
        long srcVal = dirToReg ? m.rmVal64 : m.regVal & 0xFFFFFFFFL;
        long res = destVal & srcVal;
        core.setFlag(FLAG_CF, false);
        core.setFlag(FLAG_OF, false);
        core.setFlag(FLAG_ZF, res == 0);
        core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
        core.setFlag(FLAG_PF, parity((int) res));
        core.setFlag(FLAG_AF, false);
        core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
    }

    private void modrm_xchg64(X86Core core, int pc, boolean word) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        long tmp = m.rmVal64;
        core.writeMem64(m.effAddr, core.getReg64(regFieldIndex64(m.regField)));
        core.setReg64(regFieldIndex64(m.regField), tmp);
    }

    /* ---------------- 32-bit helper methods ---------------- */

    private void modrm_arith32(X86Core core, int pc, boolean updateFlags, boolean dirToReg,
                               BinOp op, boolean writeResult) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, false);
        int destVal = dirToReg ? core.getReg(regFieldIndex64(m.regField)) : m.rmVal;
        int srcVal = dirToReg ? m.rmVal : core.getReg(regFieldIndex64(m.regField));
        int r = (int) op.op(destVal, srcVal);
        if (dirToReg) {
            core.setReg(regFieldIndex64(m.regField), r);
            if (updateFlags) setFlagsArith32(core, r, destVal, srcVal, false);
        } else if (writeResult) {
            core.writeMem32(m.effAddr, r);
            if (updateFlags) setFlagsArith32(core, r, destVal, srcVal, false);
        }
    }

    private void modrm_cmp32(X86Core core, int pc, boolean word, boolean dirToReg) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        int destVal = dirToReg ? core.getReg(regFieldIndex64(m.regField)) : m.rmVal;
        int srcVal = dirToReg ? m.rmVal : core.getReg(regFieldIndex64(m.regField));
        int res = destVal - srcVal;
        setFlagsSub32(core, destVal, srcVal, res);
    }

    private void modrm_test32(X86Core core, int pc, boolean word, boolean dirToReg) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        int destVal = dirToReg ? core.getReg(regFieldIndex64(m.regField)) : m.rmVal;
        int srcVal = dirToReg ? m.rmVal : core.getReg(regFieldIndex64(m.regField));
        int res = destVal & srcVal;
        core.setFlag(FLAG_CF, false);
        core.setFlag(FLAG_OF, false);
        core.setFlag(FLAG_ZF, res == 0);
        core.setFlag(FLAG_SF, (res & 0x80000000) != 0);
        core.setFlag(FLAG_PF, parity(res));
        core.setFlag(FLAG_AF, false);
        core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
    }

    private void modrm_xchg32(X86Core core, int pc) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, false);
        int tmp = m.rmVal;
        core.writeMem32(m.effAddr, core.getReg(regFieldIndex64(m.regField)));
        core.setReg(regFieldIndex64(m.regField), tmp);
    }

    private void modrm_imm32(X86Core core, ModRm64 m, long result, boolean updateFlags) throws Exception {
        core.writeMem32(m.effAddr, (int) result);
        if (updateFlags) {
            setFlagsArith32(core, (int) result, m.rmVal, 0, false);
        }
    }

    private void setFlagsArith32(X86Core core, int result, int src1, int src2, boolean isSub) {
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & 0x80000000) != 0);
        core.setFlag(FLAG_PF, parity(result));
        core.setFlag(FLAG_AF, false);
        if (isSub) {
            setFlagsSub32(core, src1, src2, result);
        } else {
            boolean cf = (result < src1 || result < src2);
            core.setFlag(FLAG_CF, cf);
            boolean of = (((src1 ^ result) & (src2 ^ result)) & 0x80000000) != 0;
            core.setFlag(FLAG_OF, of);
        }
        core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
    }

    private static int rotateLeft32(int val, int count) {
        count &= 0x1F;
        return (val << count) | (val >>> (32 - count));
    }

    private static int rotateRight32(int val, int count) {
        count &= 0x1F;
        return (val >>> count) | (val << (32 - count));
    }

    private void modrm_mov_imm8(X86Core core, int pc, boolean word) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        int imm = core.readMem8(pc + m.len) & 0xFF;
        if (word) {
            core.writeMem64(m.effAddr, imm);
        } else {
            core.writeMem8(m.effAddr, imm);
        }
    }

    private void modrm_mov_imm32(X86Core core, int pc, boolean word) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        long imm = word ? (core.readMem32(pc + m.len) & 0xFFFFFFFFL) : (core.readMem8(pc + m.len) & 0xFF);
        core.writeMem64(m.effAddr, imm);
    }

    private void modrm_grp3(X86Core core, int pc, boolean word) throws Exception {
        ModRm64 m = decodeModRm64(core, pc, word);
        int reg = m.regField;
        long val = m.rmVal64;
        long rax = core.getReg64(REG_RAX);
        switch (reg) {
            case 0: // TEST r/m64, imm
                long imm = word ? (core.readMem32(pc + m.len) & 0xFFFFFFFFL) : (core.readMem8(pc + m.len) & 0xFF);
                long res = val & imm;
                core.setFlag(FLAG_CF, false);
                core.setFlag(FLAG_OF, false);
                core.setFlag(FLAG_ZF, res == 0);
                core.setFlag(FLAG_SF, (res & 0x8000000000000000L) != 0);
                core.setFlag(FLAG_PF, parity((int) res));
                core.setFlag(FLAG_AF, false);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
                break;
            case 2: // NOT r/m64
                core.writeMem64(m.effAddr, ~val);
                break;
            case 3: // NEG r/m64
                long neg = -val;
                core.writeMem64(m.effAddr, neg);
                core.setFlag(FLAG_ZF, neg == 0);
                core.setFlag(FLAG_SF, (neg & 0x8000000000000000L) != 0);
                core.setFlag(FLAG_PF, parity((int) neg));
                core.setFlag(FLAG_CF, val != 0);
                core.setFlag(FLAG_OF, val == 0x8000000000000000L);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
                break;
            case 4: // MUL r/m64
                // Simplified: RDX:RAX = RAX * val
                long mul = rax * val;
                core.setReg64(REG_RAX, mul);
                core.setReg64(REG_RDX, (int)(mul >>> 32));
                core.setFlag(FLAG_CF, (mul >>> 32) != 0);
                core.setFlag(FLAG_OF, (mul >>> 32) != 0);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
                break;
            case 5: // IMUL r/m64
                // Simplified: RDX:RAX = RAX * val (signed)
                long imul = rax * val;
                core.setReg64(REG_RAX, imul);
                core.setReg64(REG_RDX, (int)(imul >>> 32));
                core.setFlag(FLAG_CF, (imul >>> 32) != 0 && (imul >>> 32) != -1);
                core.setFlag(FLAG_OF, (imul >>> 32) != 0 && (imul >>> 32) != -1);
                core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
                break;
            case 6: // DIV r/m64
                // Simplified
                break;
            case 7: // IDIV r/m64
                // Simplified
                break;
        }
    }

    /* ---------------- flag helpers ---------------- */

    private void setFlagsArith64(X86Core core, long result, long src1, long src2, boolean isSub) {
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & 0x8000000000000000L) != 0);
        core.setFlag(FLAG_PF, parity((int) result));
        core.setFlag(FLAG_AF, false);
        if (isSub) {
            setFlagsSub64(core, src1, src2, result);
        } else {
            // ADD: CF = carry out, OF = signed overflow
            boolean cf = (result < src1 || result < src2);
            core.setFlag(FLAG_CF, cf);
            boolean of = (((src1 ^ result) & (src2 ^ result)) & 0x8000000000000000L) != 0;
            core.setFlag(FLAG_OF, of);
        }
        core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
    }

    private void setFlagsSub64(X86Core core, long src1, long src2, long result) {
        core.setFlag(FLAG_ZF, result == 0);
        core.setFlag(FLAG_SF, (result & 0x8000000000000000L) != 0);
        core.setFlag(FLAG_PF, parity((int) result));
        core.setFlag(FLAG_AF, false);
        boolean cf = (result > src1); // borrow
        core.setFlag(FLAG_CF, cf);
        boolean of = (((src1 ^ src2) & (src1 ^ result)) & 0x8000000000000000L) != 0;
        core.setFlag(FLAG_OF, of);
        core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
    }

    private void setFlagsSub32(X86Core core, long src1, long src2, long result) {
        int r = (int) result;
        core.setFlag(FLAG_ZF, r == 0);
        core.setFlag(FLAG_SF, (r & 0x80000000) != 0);
        core.setFlag(FLAG_PF, parity(r));
        core.setFlag(FLAG_AF, false);
        boolean cf = ((int) result > (int) src1);
        core.setFlag(FLAG_CF, cf);
        boolean of = (((int) src1 ^ (int) src2) & ((int) src1 ^ r) & 0x80000000) != 0;
        core.setFlag(FLAG_OF, of);
        core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
    }

    private static boolean parity(int v) {
        v ^= v >>> 16;
        v ^= v >>> 8;
        v ^= v >>> 4;
        v &= 0xF;
        return (0x6996 >>> v & 1) == 1;
    }

    /* Conditional move/set condition codes (same as Jcc) */
    private boolean cmovCondition(X86Core core, int op2) {
        switch (op2) {
            case 0x40: case 0x90: return core.getFlag(FLAG_OF);       // O / O
            case 0x41: case 0x91: return !core.getFlag(FLAG_OF);      // NO / NO
            case 0x42: case 0x92: return core.getFlag(FLAG_CF);       // B / B
            case 0x43: case 0x93: return !core.getFlag(FLAG_CF);     // NB / NB
            case 0x44: case 0x94: return core.getFlag(FLAG_ZF);       // Z / Z
            case 0x45: case 0x95: return !core.getFlag(FLAG_ZF);     // NZ / NZ
            case 0x46: case 0x96: return core.getFlag(FLAG_CF) || core.getFlag(FLAG_ZF); // BE / BE
            case 0x47: case 0x97: return !core.getFlag(FLAG_CF) && !core.getFlag(FLAG_ZF); // NBE / NBE
            case 0x48: case 0x98: return core.getFlag(FLAG_SF);       // S / S
            case 0x49: case 0x99: return !core.getFlag(FLAG_SF);     // NS / NS
            case 0x4A: case 0x9A: return core.getFlag(FLAG_PF);       // P / P
            case 0x4B: case 0x9B: return !core.getFlag(FLAG_PF);     // NP / NP
            case 0x4C: case 0x9C: return core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF); // L / L
            case 0x4D: case 0x9D: return core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF); // GE / GE
            case 0x4E: case 0x9E: return core.getFlag(FLAG_ZF) || (core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF)); // LE / LE
            case 0x4F: case 0x9F: return !core.getFlag(FLAG_ZF) && (core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF)); // G / G
            default: return false;
        }
    }

    private void executeTwoByte(X86Core core, int op2, int pc) throws Exception {
        // MOVSXD r64, r/m32: REX.W + 0x0F 0x63 /r
        if ((op2 & 0xF8) == 0x60 && rexW != 0) {
            ModRm64 m = decodeModRm64(core, pc, true);
            int val32 = core.readMem32(m.effAddr);
            core.setReg64(regFieldIndex64(m.regField), val32 & 0xFFFFFFFFL);
            return;
        }

        // Jcc near: 0x0F 0x80..0x8F
        if ((op2 & 0xF0) == 0x80) {
            int rel = core.readMem32(pc + 1);
            boolean take = false;
            switch (op2) {
                case 0x80: take = core.getFlag(FLAG_OF); break;
                case 0x81: take = !core.getFlag(FLAG_OF); break;
                case 0x82: take = core.getFlag(FLAG_CF); break;
                case 0x83: take = !core.getFlag(FLAG_CF); break;
                case 0x84: take = core.getFlag(FLAG_ZF); break;
                case 0x85: take = !core.getFlag(FLAG_ZF); break;
                case 0x86: take = core.getFlag(FLAG_CF) || core.getFlag(FLAG_ZF); break;
                case 0x87: take = !core.getFlag(FLAG_CF) && !core.getFlag(FLAG_ZF); break;
                case 0x88: take = core.getFlag(FLAG_SF); break;
                case 0x89: take = !core.getFlag(FLAG_SF); break;
                case 0x8A: take = core.getFlag(FLAG_PF); break;
                case 0x8B: take = !core.getFlag(FLAG_PF); break;
                case 0x8C: take = core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF); break;
                case 0x8D: take = core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF); break;
                case 0x8E: take = core.getFlag(FLAG_ZF) || (core.getFlag(FLAG_SF) != core.getFlag(FLAG_OF)); break;
                case 0x8F: take = !core.getFlag(FLAG_ZF) && (core.getFlag(FLAG_SF) == core.getFlag(FLAG_OF)); break;
            }
            if (take) core.setPC64(pc + 1 + rel);
            return;
        }

        // SYSCALL: 0x0F 0x05
        if (op2 == 0x05) {
            return;
        }
        // SYSRET: 0x0F 0x07
        if (op2 == 0x07) {
            return;
        }
        // CPUID: 0x0F 0xA2
        if (op2 == 0xA2) {
            return;
        }
        // BSWAP r64: 0x0F C8..CF
        if ((op2 & 0xF8) == 0xC8) {
            int reg = rmIndex64(op2 & 0x07);
            long val = core.getReg64(reg);
            long swapped = Long.reverseBytes(val);
            core.setReg64(reg, swapped);
            return;
        }
        // MOVZX r64, r/m8: 0x0F B6 /r
        // MOVZX r64, r/m16: 0x0F B7 /r
        // MOVSX r64, r/m8: 0x0F BE /r
        // MOVSX r64, r/m16: 0x0F BF /r
        if ((op2 & 0xFC) == 0xB6 || (op2 & 0xFC) == 0xBE) {
            boolean sign = (op2 & 0x02) != 0; // BE/BF = sign-extend
            boolean isWord = (op2 & 0x01) != 0; // B7/BF = 16-bit source
            ModRm64 m = decodeModRm64(core, pc, false);
            long val;
            if (isWord) {
                int v = core.readMem16(m.effAddr);
                val = sign ? (v << 16 >> 16) : (v & 0xFFFFL);
            } else {
                int v = core.readMem8(m.effAddr);
                val = sign ? (v << 24 >> 24) : (v & 0xFFL);
            }
            core.setReg64(regFieldIndex64(m.regField), val);
            return;
        }
        // CALL r/m64: 0x0F 0xFF /2
        if (op2 == 0xFF) {
            ModRm64 m = decodeModRm64(core, pc, true);
            int reg = m.regField;
            if (reg == 2) {
                core.push64(pc + m.len);
                core.setPC64(m.rmVal64);
                return;
            }
        }

        // CMOVcc: 0x0F 0x40..0x4F (conditional move r64, r/m64)
        if ((op2 & 0xF8) == 0x40 && rexW != 0) {
            ModRm64 m = decodeModRm64(core, pc, true);
            boolean take = cmovCondition(core, op2);
            if (take) {
                core.setReg64(regFieldIndex64(m.regField), m.rmVal64);
            }
            return;
        }

        // SETcc: 0x0F 0x90..0x9F (conditional set byte r/m8)
        if ((op2 & 0xF8) == 0x90) {
            ModRm64 m = decodeModRm64(core, pc, false);
            boolean take = cmovCondition(core, op2);
            core.writeMem8(m.effAddr, take ? 1 : 0);
            return;
        }

        // XADD r/m64, r64: REX.W + 0x0F 0xC1 /r
        if (op2 == 0xC1 && rexW != 0) {
            ModRm64 m = decodeModRm64(core, pc, true);
            long dest = core.getReg64(regFieldIndex64(m.regField));
            long sum = dest + m.rmVal64;
            core.writeMem64(m.effAddr, dest);
            core.setReg64(regFieldIndex64(m.regField), sum);
            setFlagsArith64(core, sum, dest, m.rmVal64, false);
            return;
        }

        // CMPXCHG r/m64, r64: REX.W + 0x0F 0xB1 /r
        if (op2 == 0xB1 && rexW != 0) {
            ModRm64 m = decodeModRm64(core, pc, true);
            long rax = core.getReg64(REG_RAX);
            long dest = m.rmVal64;
            if (rax == dest) {
                core.writeMem64(m.effAddr, core.getReg64(regFieldIndex64(m.regField)));
                core.setFlag(FLAG_ZF, true);
            } else {
                core.setReg64(REG_RAX, dest);
                core.setFlag(FLAG_ZF, false);
            }
            core.setFlag(FLAG_CF, false);
            core.setFlag(FLAG_OF, false);
            core.setFlag(FLAG_SF, false);
            core.setFlag(FLAG_PF, false);
            core.setFlag(FLAG_AF, false);
            core.setRflags64(core.getFlags() & 0xFFFFFFFFL);
            return;
        }

        throw unknownOpcode(core, String.format("opcode 0x0F 0x%02X", op2));
    }

    private interface BinOp {
        long op(long a, long b);
    }
}
