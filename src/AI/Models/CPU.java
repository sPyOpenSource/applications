/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI.Models;

import AI.Models.vm.VmCP;
import AI.Models.vm.VmStackFrame;

/**
 *
 * @author X. Wang
 */
public class CPU {
    /* Type of Functional Units */
 int FU_ALU = 0x0;
 int FU_MUL =0x1;
 int FU_DIV =0x2;
 int FU_FPU_ALU =0x3;
 int FU_FPU_FMA =0x4;
 int NUM_MAX_FU =0x5;

/* Extension C Quadrants */
 int C_QUADRANT0 = 0;
 int C_QUADRANT1 =1;
 int C_QUADRANT2 =2;

/* Used for updating performance counters */

 int NUM_MAX_INS_TYPES =17;
 int INS_TYPE_LOAD =0x0;
 int INS_TYPE_STORE =0x1;
 int INS_TYPE_ATOMIC =0x2;
 int INS_TYPE_SYSTEM =0x3;
 int INS_TYPE_ARITMETIC= 0x4;
 int INS_TYPE_COND_BRANCH= 0x5;
 int INS_TYPE_JAL =0x6;
 int INS_TYPE_JALR =0x7;
 int INS_TYPE_INT_MUL =0x8;
 int INS_TYPE_INT_DIV =0x9;
 int INS_TYPE_FP_LOAD =0xa;
 int INS_TYPE_FP_STORE =0xb;
 int INS_TYPE_FP_ADD =0xc;
 int INS_TYPE_FP_MUL =0xd;
 int INS_TYPE_FP_FMA =0xe;
 int INS_TYPE_FP_DIV_SQRT =0xf;
 int INS_TYPE_FP_MISC =0x10;

 int INS_CLASS_INT= 0x11;
 int INS_CLASS_FP =0x12;

/* For exception handling during simulation */
 int SIM_ILLEGAL_OPCODE= 0x1;
 int SIM_COMPLEX_OPCODE =0x2;
 int SIM_TIMEOUT_EXCEPTION =0x3;
 int SIM_MMU_EXCEPTION =0x4;

/* Type of Branch instructions */
 int BRANCH_UNCOND =0x0;
 int BRANCH_COND =0x1;
 int BRANCH_FUNC_CALL =0x2;
 int BRANCH_FUNC_RET =0x3;

/**
 * RISC-V Instruction Decoding Library
 *
 * Copyright (c) 2016-2017 Fabrice Bellard
 *
 * MARSS-RISCV : Micro-Architectural System Simulator for RISC-V
 *
 * Copyright (c) 2017-2019 Gaurav Kothari {gkothar1@binghamton.edu}
 * State University of New York at Binghamton
 *
 * Copyright (c) 2018-2019 Parikshit Sarnaik {psarnai1@binghamton.edu}
 * State University of New York at Binghamton
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
    
 int cget_field1(int i, int j, int k, int n){
    return 0;
}

public  void
decode_compressed_q0(RVInstruction ins)
{
    int insn, rd, rs1, rs2, funct3, quad;
    int imm;

    insn = ins.binary;
    funct3 = (insn >> 13) & 7;
    rd = ((insn >> 2) & 7) | 8;
    rs2 = rd;
    quad = C_QUADRANT0;
    switch (funct3)
    {
        case 0: /* c.addi4spn */
            ins.has_dest = 1;
            ins.has_src1 = 1;
            rs1 = 2;
            imm = cget_field1(insn, 11, 4, 5) | cget_field1(insn, 7, 6, 9)
                  | cget_field1(insn, 6, 2, 2) | cget_field1(insn, 5, 3, 3);
            if (imm == 0){
                //goto illegal_insn;
            }
            break;
        case 1: /* c.fld */
        {
            if (ins.current_fs == 0){
                //goto illegal_insn;
            }
            ins.is_load = 1;
            ins.has_fp_dest = 1;
            ins.has_src1 = 1;
            ins.bytes_to_rw = 8;
            ins.f64_mask = 1;
            ins.set_fs = 1;
            ins.type = INS_TYPE_FP_LOAD;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 5, 6, 7);
            rs1 = ((insn >> 7) & 7) | 8;
        }
        break;
        case 2: /* c.lw */
        {
            ins.is_load = 1;
            ins.has_dest = 1;
            ins.has_src1 = 1;
            ins.bytes_to_rw = 4;
            ins.type = INS_TYPE_LOAD;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 6, 2, 2)
                  | cget_field1(insn, 5, 6, 6);
            rs1 = ((insn >> 7) & 7) | 8;
        }
        break;
        case 3: /* c.ld */
        {
            ins.is_load = 1;
            ins.has_dest = 1;
            ins.has_src1 = 1;
            ins.bytes_to_rw = 8;
            ins.type = INS_TYPE_LOAD;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 5, 6, 7);
            rs1 = ((insn >> 7) & 7) | 8;
        }
        break;
        /*case 3: // c.flw 
        {
            ins.is_load = 1;
            ins.has_fp_dest = 1;
            ins.has_src1 = 1;
            ins.bytes_to_rw = 4;
            ins.f32_mask = 1;
            ins.set_fs = 1;
            ins.type = INS_TYPE_FP_LOAD;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 6, 2, 2)
                  | cget_field1(insn, 5, 6, 6);
            rs1 = ((insn >> 7) & 7) | 8;
        }
        break;*/
        case 5: /* c.fsd */
            ins.is_store = 1;
            ins.has_src1 = 1;
            ins.has_fp_src2 = 1;
            ins.bytes_to_rw = 8;
            ins.type = INS_TYPE_FP_STORE;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 5, 6, 7);
            rs1 = ((insn >> 7) & 7) | 8;
            break;
        case 6: /* c.sw */
            ins.is_store = 1;
            ins.has_src1 = 1;
            ins.has_src2 = 1;
            ins.bytes_to_rw = 4;
            ins.type = INS_TYPE_STORE;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 6, 2, 2)
                  | cget_field1(insn, 5, 6, 6);
            rs1 = ((insn >> 7) & 7) | 8;
            break;
        case 7: /* c.sd */
            ins.is_store = 1;
            ins.has_src1 = 1;
            ins.has_src2 = 1;
            ins.bytes_to_rw = 8;
            ins.type = INS_TYPE_STORE;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 5, 6, 7);
            rs1 = ((insn >> 7) & 7) | 8;
            break;
        /*case 7: // c.fsw 
            ins.is_store = 1;
            ins.has_src1 = 1;
            ins.has_fp_src2 = 1;
            ins.bytes_to_rw = 4;
            ins.type = INS_TYPE_FP_STORE;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 6, 2, 2)
                  | cget_field1(insn, 5, 6, 6);
            rs1 = ((insn >> 7) & 7) | 8;
            break;*/
        default:
            //goto illegal_insn;
            return;
    }
    ins.rd = rd;
    ins.rs1 = rs1;
    ins.rs2 = rs2;
    ins.funct3 = funct3;
    ins.imm = imm;
    ins.quad = quad;
/*illegal_insn:
    ins.exception = 1;
    ins.exception_cause = SIM_ILLEGAL_OPCODE;*/
}

 int sextc(int i, int j){
    return 0;
}

public  void
decode_compressed_q1(RVInstruction ins)
{
    int insn, rd, rs1, rs2 = 0, funct3, funct4 = 0, funct5 = 0, quad;
    int imm = 0;

    insn = ins.binary;
    funct3 = (insn >> 13) & 7;
    rd = (insn >> 7) & 0x1f;
    rs1 = rd;
    quad = C_QUADRANT1;
    switch (funct3)
    {
        case 0: /* c.addi/c.nop */
            ins.has_dest = 1;
            ins.has_src1 = 1;
            imm = sextc(
                cget_field1(insn, 12, 5, 5) | cget_field1(insn, 2, 0, 4), 6);
            break;
        case 1: /* c.addiw */
            ins.has_dest = 1;
            ins.has_src1 = 1;
            imm = sextc(
                cget_field1(insn, 12, 5, 5) | cget_field1(insn, 2, 0, 4), 6);
            break;
        case 2: /* c.li */
            ins.has_dest = 1;
            imm = sextc(
                cget_field1(insn, 12, 5, 5) | cget_field1(insn, 2, 0, 4), 6);
            break;
        case 3:
            if (rd == 2)
            {
                /* c.addi16sp */
                ins.has_dest = 1;
                ins.has_src1 = 1;
                rd = 2;
                rs1 = rd;
                imm = sextc(cget_field1(insn, 12, 9, 9)
                                | cget_field1(insn, 6, 4, 4)
                                | cget_field1(insn, 5, 6, 6)
                                | cget_field1(insn, 3, 7, 8)
                                | cget_field1(insn, 2, 5, 5),
                            10);
                if (imm == 0){
                    //goto illegal_insn;
                }
            }
            else if (rd != 0)
            {
                /* c.lui */
                ins.has_dest = 1;
                imm = sextc(cget_field1(insn, 12, 17, 17)
                                | cget_field1(insn, 2, 12, 16),
                            18);
            }
            break;
        case 4:
            ins.has_dest = 1;
            ins.has_src1 = 1;
            funct4 = (insn >> 10) & 3;
            rd = ((insn >> 7) & 7) | 8;
            rs1 = rd;
            switch (funct4)
            {
                case 0: /* c.srli */
                case 1: /* c.srai */
                    imm = cget_field1(insn, 12, 5, 5)
                          | cget_field1(insn, 2, 0, 4);
                    break;
                case 2: /* c.andi */
                    imm = sextc(cget_field1(insn, 12, 5, 5)
                                    | cget_field1(insn, 2, 0, 4),
                                6);
                    break;
                case 3:
                    rs2 = ((insn >> 2) & 7) | 8;
                    ins.has_src2 = 1;
                    funct5 = ((insn >> 5) & 3) | ((insn >> (12 - 2)) & 4);
                    switch (funct5)
                    {
                        case 0: /* c.sub */
                        case 1: /* c.xor */
                        case 2: /* c.or */
                        case 3: /* c.and */
                        case 4: /* c.subw */
                        case 5: /* c.addw */
                            break;
                        default:
                            //goto illegal_insn;
                    }
                    break;
            }
            break;
        case 5: /* c.j */
            ins.is_branch = 1;
            ins.branch_type = BRANCH_UNCOND;
            ins.type = INS_TYPE_JAL;
            imm = sextc(
                cget_field1(insn, 12, 11, 11) | cget_field1(insn, 11, 4, 4)
                    | cget_field1(insn, 9, 8, 9) | cget_field1(insn, 8, 10, 10)
                    | cget_field1(insn, 7, 6, 6) | cget_field1(insn, 6, 7, 7)
                    | cget_field1(insn, 3, 1, 3) | cget_field1(insn, 2, 5, 5),
                12);
            break;
        case 6: /* c.beqz */
            ins.is_branch = 1;
            ins.branch_type = BRANCH_COND;
            ins.type = INS_TYPE_COND_BRANCH;
            ins.has_src1 = 1;
            rs1 = ((insn >> 7) & 7) | 8;
            rs2 = 0;
            imm = sextc(
                cget_field1(insn, 12, 8, 8) | cget_field1(insn, 10, 3, 4)
                    | cget_field1(insn, 5, 6, 7) | cget_field1(insn, 3, 1, 2)
                    | cget_field1(insn, 2, 5, 5),
                9);
            break;
        case 7: /* c.bnez */
            ins.is_branch = 1;
            ins.branch_type = BRANCH_COND;
            ins.type = INS_TYPE_COND_BRANCH;
            ins.has_src1 = 1;
            rs1 = ((insn >> 7) & 7) | 8;
            rs2 = 0;
            imm = sextc(
                cget_field1(insn, 12, 8, 8) | cget_field1(insn, 10, 3, 4)
                    | cget_field1(insn, 5, 6, 7) | cget_field1(insn, 3, 1, 2)
                    | cget_field1(insn, 2, 5, 5),
                9);
            break;
        default:
            //goto illegal_insn;
    }
    ins.rd = rd;
    ins.rs1 = rs1;
    ins.rs2 = rs2;
    ins.imm = imm;
    ins.quad = quad;
    ins.funct3 = funct3;
    ins.funct4 = funct4;
    ins.funct5 = funct5;
}

public void illegal_insn(RVInstruction ins){
    ins.exception = 1;
    ins.exception_cause = SIM_ILLEGAL_OPCODE;
}

public  void
decode_compressed_q2(RVInstruction ins)
{
    int insn, rd, rs1, rs2, funct3, quad;
    int imm = 0;

    insn = ins.binary;
    quad = C_QUADRANT2;
    funct3 = (insn >> 13) & 7;
    rd = (insn >> 7) & 0x1f;
    rs1 = rd;
    rs2 = (insn >> 2) & 0x1f;
    switch (funct3)
    {
        case 0: /* c.slli */
            ins.has_dest = 1;
            ins.has_src1 = 1;
            rs1 = rd;
            imm = cget_field1(insn, 12, 5, 5) | rs2;
            break;
        case 1: /* c.fldsp */
        {
            ins.is_load = 1;
            ins.has_fp_dest = 1;
            ins.has_src1 = 1;
            ins.bytes_to_rw = 8;
            ins.f64_mask = 1;
            ins.set_fs = 1;
            ins.type = INS_TYPE_FP_LOAD;
            rs1 = 2;
            imm = cget_field1(insn, 12, 5, 5) | (rs2 & (3 << 3))
                  | cget_field1(insn, 2, 6, 8);
        }
        break;
        case 2: /* c.lwsp */
        {
            ins.is_load = 1;
            ins.bytes_to_rw = 4;
            ins.has_dest = 1;
            ins.has_src1 = 1;
            ins.type = INS_TYPE_LOAD;
            rs1 = 2;
            imm = cget_field1(insn, 12, 5, 5) | (rs2 & (7 << 2))
                  | cget_field1(insn, 2, 6, 7);
        }
        break;
        case 3: /* c.ldsp */
        {
            ins.is_load = 1;
            ins.bytes_to_rw = 8;
            ins.has_dest = 1;
            ins.has_src1 = 1;
            ins.type = INS_TYPE_LOAD;
            rs1 = 2;
            imm = cget_field1(insn, 12, 5, 5) | (rs2 & (3 << 3))
                  | cget_field1(insn, 2, 6, 8);
        }
        break;
        /*case 3: // c.flwsp 
        {
            ins.is_load = 1;
            ins.has_fp_dest = 1;
            ins.has_src1 = 1;
            ins.bytes_to_rw = 4;
            ins.f32_mask = 1;
            ins.set_fs = 1;
            ins.type = INS_TYPE_FP_LOAD;
            rs1 = 2;
            imm = cget_field1(insn, 12, 5, 5) | (rs2 & (7 << 2))
                  | cget_field1(insn, 2, 6, 7);
        }
        break;*/
        case 4:
            if (((insn >> 12) & 1) == 0)
            {
                if (rs2 == 0)
                {
                    /* c.jr */
                    ins.is_branch = 1;
                    ins.branch_type = BRANCH_UNCOND;
                    ins.type = INS_TYPE_JALR;
                    ins.has_src1 = 1;
                    if (rd == 0)
                        //goto illegal_insn;
                    if (rs1 == 1)
                    {
                        ins.is_func_ret = 1;
                    }
                }
                else
                {
                    /* c.mv */
                    ins.has_dest = 1;
                    ins.has_src2 = 1;
                }
            }
            else
            {
                if (rs2 == 0)
                {
                    if (rd == 0)
                    {
                        /* c.ebreak */
                        ins.exception = 1;
                        ins.exception_cause = SIM_COMPLEX_OPCODE;
                        ins.type = INS_TYPE_SYSTEM;
                    }
                    else
                    {
                        /* c.jalr */
                        ins.is_branch = 1;
                        ins.branch_type = BRANCH_UNCOND;
                        ins.type = INS_TYPE_JALR;
                        ins.has_dest = 1;
                        ins.has_src1 = 1;
                        rd = 1;
                        ins.is_func_call = 1;
                    }
                }
                else
                {
                    /* c.add */
                    ins.has_dest = 1;
                    ins.has_src1 = 1;
                    ins.has_src2 = 1;
                }
            }
            break;
        case 5: /* c.fsdsp */
            ins.is_store = 1;
            ins.has_src1 = 1;
            ins.has_fp_src2 = 1;
            ins.bytes_to_rw = 8;
            ins.type = INS_TYPE_FP_STORE;
            rs1 = 2;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 7, 6, 8);
            break;
        case 6: /* c.swsp */
            ins.is_store = 1;
            ins.bytes_to_rw = 4;
            ins.has_src1 = 1;
            ins.has_src2 = 1;
            ins.type = INS_TYPE_STORE;
            rs1 = 2;
            imm = cget_field1(insn, 9, 2, 5) | cget_field1(insn, 7, 6, 7);
            break;
        case 7: /* c.sdsp */
            ins.is_store = 1;
            ins.bytes_to_rw = 8;
            ins.has_src1 = 1;
            ins.has_src2 = 1;
            ins.type = INS_TYPE_STORE;
            rs1 = 2;
            imm = cget_field1(insn, 10, 3, 5) | cget_field1(insn, 7, 6, 8);
            break;
        /*case 7: // c.fswsp 
            ins.is_store = 1;
            ins.has_src1 = 1;
            ins.has_fp_src2 = 1;
            ins.bytes_to_rw = 4;
            ins.type = INS_TYPE_FP_STORE;
            rs1 = 2;
            imm = cget_field1(insn, 9, 2, 5) | cget_field1(insn, 7, 6, 7);
            break;*/
        default:
            //goto illegal_insn;
    }
    ins.rd = rd;
    ins.rs1 = rs1;
    ins.rs2 = rs2;
    ins.imm = imm;
    ins.quad = quad;
    ins.funct3 = funct3;
}

public  void
decode_compressed_type(RVInstruction ins)
{
    int quad = ins.binary & 3;
    switch (quad)
    {
        case 0:
        {
            decode_compressed_q0(ins);
            break;
        }
        case 1:
        {
            decode_compressed_q1(ins);
            break;
        }
        case 2:
        {
            decode_compressed_q2(ins);
            break;
        }
        default:
        {
            System.exit(1);
        }
    }
}

public  void
set_op_fu(RVInstruction i)
{
    int funct3;
    int insn = i.binary;
    int imm = insn >> 25;
    if (imm == 1)
    {
        funct3 = (insn >> 12) & 7;
        switch (funct3)
        {
            case 0: /* mul */
            case 1: /* mulh */
            case 2: /* mulhsu */
            case 3: /* mulhu */
                i.fu_type = FU_MUL;
                i.type = INS_TYPE_INT_MUL;
                break;
            case 4: /* div */
            case 5: /* divu */
            case 6: /* rem */
            case 7: /* remu */
                i.fu_type = FU_DIV;
                i.type = INS_TYPE_INT_DIV;
                break;
        }
    }
}

public  int
chk_op_imm_exceptions(RVInstruction i, int bit_size)
{
    int funct3 = (i.binary >> 12) & 7;

    switch (funct3)
    {
        case 1: /* slli */
            if ((i.imm & ~(bit_size - 1)) != 0)
            {
                return -1;
            }
            break;
        case 5: /* srli/srai */
            if ((i.imm & ~((bit_size - 1) | 0x400)) != 0)
            {
                return -1;
            }
    }
    return 0;
}

public  int
chk_op_exceptions(RVInstruction i)
{
    int imm = i.binary >> 25;

    if (imm != 1)
    {
        if ((imm & ~0x20) != 0)
        {
            return -1;
        }
    }
    return 0;
}

    CPU(char[][] pc, VmStackFrame stackFrame, VmCP p) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
public char[][] opCode;
public VmStackFrame stack;
public VmCP p;
        
    public CPU(){
        
    }
    
/**
 * @param  Encoded 32-bit instruction binary
 * @return Decoded RVInstruction
 */
public void
decode_riscv_binary(RVInstruction ins, int insn)
{
    ins.binary = insn;
    ins.fu_type = FU_ALU;
    ins.data_class = INS_CLASS_INT;
    ins.type = INS_TYPE_ARITMETIC;
    if ((ins.binary & 3) != 3)
    {
        /* Compressed Instruction */
        decode_compressed_type(ins);
    }
    else
    {
        /* 32-bit Instruction */
        ins.major_opcode = insn & 0x7f;
        ins.funct3 = (insn >> 12) & 7;
        ins.funct7 = (ins.binary & 0xfe000000) >> 25;
        ins.rd = (insn >> 7) & 0x1f;
        ins.rs1 = (insn >> 15) & 0x1f;
        ins.rs2 = (insn >> 20) & 0x1f;
        switch (ins.major_opcode)
        {
            case LOAD_MASK:
            {
                ins.is_load = 1;
                ins.has_src1 = 1;
                ins.has_dest = 1;
                ins.imm = (int)insn >> 20;
                ins.type = INS_TYPE_LOAD;
                switch (ins.funct3)
                {
                    case 0x0: /* lb */
                    {
                        ins.bytes_to_rw = 1;
                        break;
                    }
                    case 0x1: /* lh */
                    {
                        ins.bytes_to_rw = 2;
                        break;
                    }
                    case 0x2: /* lw */
                    {
                        ins.bytes_to_rw = 4;
                        break;
                    }
                    case 0x3: /* ld */
                    {
                        ins.bytes_to_rw = 8;
                        break;
                    }
                    case 0x4: /* lbu */
                    {
                        ins.bytes_to_rw = 1;
                        ins.is_unsigned = 1;
                        break;
                    }
                    case 0x5: /* lhu */
                    {
                        ins.bytes_to_rw = 2;
                        ins.is_unsigned = 1;
                        break;
                    }
                    case 0x6: /* lwu */
                    {
                        ins.bytes_to_rw = 4;
                        ins.is_unsigned = 1;
                        break;
                    }
                }
                break;
            }
            case OP_IMM_MASK:
            case OP_IMM_32_MASK:
            {
                if (ins.major_opcode == OP_IMM_MASK)
                {
                    if (chk_op_imm_exceptions(ins, 64/*BIT_SIZE*/)!=0)
                    {
                        //goto exception;
                    }
                }
                else
                {
                    if (chk_op_imm_exceptions(ins, 32) != 0)
                    {
                        //goto exception;
                    }
                }

                ins.has_src1 = 1;
                ins.has_dest = 1;
                ins.imm = (int)insn >> 20;
                break;
            }
            case OP_MASK:
            case OP_MASK_32:
            {
                if (chk_op_exceptions(ins)!=0)
                {
                    //goto exception;
                }

                ins.has_src1 = 1;
                ins.has_src2 = 1;
                ins.has_dest = 1;
                /* set the functional units for mul and div */
                set_op_fu(ins);
                break;
            }
            case LUI_MASK:
            case AUIPC_MASK:
            {
                ins.has_dest = 1;
                ins.imm = (int)(insn & 0xfffff000);
                break;
            }
            case STORE_MASK:
            {
                ins.is_store = 1;
                ins.has_src1 = 1;
                ins.has_src2 = 1;
                ins.imm = ins.rd | ((insn >> (25 - 5)) & 0xfe0);
                ins.imm = (ins.imm << 20) >> 20;
                ins.type = INS_TYPE_STORE;
                switch (ins.funct3)
                {
                    case 0x0: /* sb */
                    {
                        ins.bytes_to_rw = 1;
                        break;
                    }
                    case 0x1: /* sh */
                    {
                        ins.bytes_to_rw = 2;
                        break;
                    }
                    case 0x2: /* sw */
                    {
                        ins.bytes_to_rw = 4;
                        break;
                    }
                    case 0x3: /* sd */
                    {
                        ins.bytes_to_rw = 8;
                        break;
                    }
                }
                break;
            }
            case CSR_MASK:
            {
                ins.is_system = 1;
                ins.exception = 1;
                ins.type = INS_TYPE_SYSTEM;

                /* Complex Opcode */
                ins.exception_cause = SIM_COMPLEX_OPCODE;
                break;
            }
            case FENCE_MASK:
            {
                ins.is_system = 1;
                ins.exception = 1;
                ins.type = INS_TYPE_SYSTEM;

                /* Complex Opcode */
                ins.exception_cause = SIM_COMPLEX_OPCODE;
                break;
            }

            case JAL_MASK:
            {
                ins.is_branch = 1;
                ins.branch_type = BRANCH_UNCOND;
                ins.has_dest = 1;
                ins.imm = ((insn >> (31 - 20)) & (1 << 20))
                           | ((insn >> (21 - 1)) & 0x7fe)
                           | ((insn >> (20 - 11)) & (1 << 11))
                           | (insn & 0xff000);
                ins.imm = (ins.imm << 11) >> 11;
                ins.type = INS_TYPE_JAL;
                if (ins.rd == 1)
                {
                    ins.is_func_call = 1;
                }
                break;
            }
            case JALR_MASK:
            {
                ins.is_branch = 1;
                ins.branch_type = BRANCH_UNCOND;
                ins.has_src1 = 1;
                ins.has_dest = 1;
                ins.imm = (int)insn >> 20;
                ins.type = INS_TYPE_JALR;
                if (ins.rd == 1)
                {
                    ins.is_func_call = 1;
                }
                if (ins.rs1 == 1)
                {
                    ins.is_func_ret = 1;
                }
                break;
            }
            case BRANCH_MASK:
            {
                ins.is_branch = 1;
                ins.branch_type = BRANCH_COND;
                ins.has_src1 = 1;
                ins.has_src2 = 1;
                ins.imm = ((insn >> (31 - 12)) & (1 << 12))
                           | ((insn >> (25 - 5)) & 0x7e0)
                           | ((insn >> (8 - 1)) & 0x1e)
                           | ((insn << (11 - 7)) & (1 << 11));
                ins.imm = (ins.imm << 19) >> 19;
                ins.type = INS_TYPE_COND_BRANCH;
                break;
            }
            case ATOMIC_MASK:
            {
                int funct3;

                ins.is_atomic = 1;
                ins.has_dest = 1;
                ins.type = INS_TYPE_ATOMIC;
                funct3 = (insn >> 12) & 7;
                switch (funct3)
                {
                    case 2:
                    case 3:
                    {
                        funct3 = ins.binary >> 27;
                        switch (funct3)
                        {
                            case 2: /* lr.w */
                            {
                                if (ins.rs2 != 0){
                                    //goto exception;
                                }
                                ins.has_src1 = 1;
                                ins.is_atomic_load = 1;
                                ins.bytes_to_rw = 64;//sizeof(target_ulong);
                                break;
                            }
                            case 3: /* sc.w */
                            {
                                ins.has_src1 = 1;
                                ins.has_src2 = 1;
                                ins.is_atomic_store = 1;
                                ins.bytes_to_rw = 64;//sizeof(target_ulong);
                                break;
                            }
                            case 1:    /* amiswap.w */
                            case 0:    /* amoadd.w */
                            case 4:    /* amoxor.w */
                            case 0xc:  /* amoand.w */
                            case 0x8:  /* amoor.w */
                            case 0x10: /* amomin.w */
                            case 0x14: /* amomax.w */
                            case 0x18: /* amominu.w */
                            case 0x1c: /* amomaxu.w */
                            {
                                ins.has_src1 = 1;
                                ins.has_src2 = 1;
                                ins.is_atomic_operate = 1;
                                ins.is_atomic_load = 1;
                                ins.is_atomic_store = 1;
                                ins.bytes_to_rw = 64;//sizeof(target_ulong);
                                break;
                            }
                            default:
                                //goto exception;
                        }
                        break;
                    }
                    default:
                        //goto exception;
                }
                break;
            }
            case FLOAD_MASK:
            {
                if (ins.current_fs == 0)
                {
                    //goto exception;
                }
                ins.is_load = 1;
                ins.has_fp_dest = 1;
                ins.has_src1 = 1;
                ins.set_fs = 1;
                ins.imm = (int)insn >> 20;
                ins.type = INS_TYPE_FP_LOAD;
                switch (ins.funct3)
                {
                    case 2: /* flw */
                    {
                        ins.bytes_to_rw = 4;
                        ins.f32_mask = 1;
                    }
                    break;
                    case 3: /* fld */
                    {
                        ins.bytes_to_rw = 8;
                        ins.f64_mask = 1;
                    }
                    break;
                }
                break;
            }
            case FSTORE_MASK:
            {
                if (ins.current_fs == 0)
                {
                    //goto exception;
                }
                ins.is_store = 1;
                ins.has_src1 = 1;
                ins.has_fp_src2 = 1;
                ins.imm = ins.rd | ((insn >> (25 - 5)) & 0xfe0);
                ins.imm = (ins.imm << 20) >> 20;
                ins.type = INS_TYPE_FP_STORE;
                switch (ins.funct3)
                {
                    case 2: /* fsw */
                        ins.bytes_to_rw = 4;
                        break;
                    case 3: /* fsd */
                        ins.bytes_to_rw = 8;
                        break;
                }
                break;
            }
            case FMADD_MASK:
            {
                if ((ins.current_fs == 0) || (ins.rm < 0))
                {
                    //goto exception;
                }
                ins.data_class = INS_CLASS_FP;
                ins.fu_type = FU_FPU_FMA;
                ins.type = INS_TYPE_FP_FMA;
                ins.has_fp_dest = 1;
                ins.has_fp_src1 = 1;
                ins.has_fp_src2 = 1;
                ins.has_fp_src3 = 1;
                ins.set_fs = 1;
                ins.funct3 = (ins.binary >> 25) & 3;
                ins.rs3 = ins.binary >> 27;
                break;
            }
            case FMSUB_MASK:
            {
                if ((ins.current_fs == 0) || (ins.rm < 0))
                {
                    //goto exception;
                }
                ins.data_class = INS_CLASS_FP;
                ins.fu_type = FU_FPU_FMA;
                ins.type = INS_TYPE_FP_FMA;
                ins.has_fp_dest = 1;
                ins.has_fp_src1 = 1;
                ins.has_fp_src2 = 1;
                ins.has_fp_src3 = 1;
                ins.set_fs = 1;
                ins.funct3 = (ins.binary >> 25) & 3;
                ins.rs3 = ins.binary >> 27;
                break;
            }
            case FNMSUB_MASK:
            {
                if ((ins.current_fs == 0) || (ins.rm < 0))
                {
                    //goto exception;
                }
                ins.data_class = INS_CLASS_FP;
                ins.fu_type = FU_FPU_FMA;
                ins.type = INS_TYPE_FP_FMA;
                ins.has_fp_dest = 1;
                ins.has_fp_src1 = 1;
                ins.has_fp_src2 = 1;
                ins.has_fp_src3 = 1;
                ins.set_fs = 1;
                ins.funct3 = (ins.binary >> 25) & 3;
                ins.rs3 = ins.binary >> 27;
                break;
            }
            case FNMADD_MASK:
            {
                if ((ins.current_fs == 0) || (ins.rm < 0))
                {
                    //goto exception;
                }
                ins.data_class = INS_CLASS_FP;
                ins.fu_type = FU_FPU_FMA;
                ins.type = INS_TYPE_FP_FMA;
                ins.has_fp_dest = 1;
                ins.has_fp_src1 = 1;
                ins.has_fp_src2 = 1;
                ins.has_fp_src3 = 1;
                ins.set_fs = 1;
                ins.funct3 = (ins.binary >> 25) & 3;
                ins.rs3 = ins.binary >> 27;
                break;
            }
            case F_ARITHMETIC_MASK:
            {
                if (ins.current_fs == 0)
                {
                    //goto exception;
                }
                ins.data_class = INS_CLASS_FP;
                ins.fu_type = FU_FPU_ALU;
                ins.rm = ins.funct3;
                ins.type = INS_TYPE_FP_MISC;
                switch (ins.funct7)
                {
                    default:
                        //goto exception;
                }
                break;
            }
            default:
            {
                //goto exception;
            }
        }
    }

    /* Generate instruction string */
    if (ins.create_str)
    {
        get_riscv_ins_str(ins);
    }
}

/* Floating Point Instructions */
final  int FLOAD_MASK =0x07;
final  int FSTORE_MASK =0x27;
final  int FMADD_MASK =0x43;
final  int FMSUB_MASK =0x47;
final  int FNMSUB_MASK =0x4B;
final  int FNMADD_MASK =0x4F;
final int F_ARITHMETIC_MASK =0x53;

/* Major Opcodes */
final  int OP_IMM_MASK =0x13;
final  int OP_IMM_32_MASK =0x1b;
final  int OP_MASK =0x33;
final  int OP_MASK_32 =0x3b;
final  int LUI_MASK =0x37;
final  int AUIPC_MASK =0x17;
final  int JAL_MASK =0x6f;
final  int JALR_MASK =0x67;
final  int BRANCH_MASK =0x63;
final  int LOAD_MASK =0x3;
final  int STORE_MASK =0x23;
final  int FENCE_MASK =0xf;
final  int CSR_MASK =0x73;
final  int ATOMIC_MASK =0x2F;

    private void get_riscv_ins_str(RVInstruction ins) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private  class RVInstruction {
        public int binary, exception, exception_cause, has_dest, has_src1, current_fs, is_load, has_fp_dest;
        public int bytes_to_rw, is_store, has_fp_src1, has_fp_src2, has_fp_src3, set_fs, funct3, rs3, rm, imm, rd;
        public int f32_mask, f64_mask, has_src2, is_atomic_operate, is_atomic_load, is_atomic_store, rs2, is_atomic;
        public int is_branch, is_func_call, is_func_ret, rs1, is_system, is_unsigned, quad, funct4, funct5, funct7;
        public int major_opcode, branch_type, type, fu_type, data_class;
        public boolean create_str;

        public RVInstruction() {
        }
    }
}
