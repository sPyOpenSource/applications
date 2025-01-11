
package jCPU.RiscV;

import jCPU.Opcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RVInstruction implements Opcode {
        public int binary, exception, exception_cause, current_fs;
        public int bytes_to_rw, set_fs, rm, imm, rd;
        public int f32_mask, f64_mask;
        public int rs1, rs2, rs3, quad, funct3, funct4, funct5, funct7;
        public int major_opcode, branch_type, type, fu_type, data_class;
        
        public boolean create_str, is_load, is_atomic_operate, is_atomic_load, is_atomic_store, is_atomic;
        public boolean has_fp_src1, has_fp_src2, has_fp_src3, is_store, has_fp_dest, has_dest, has_src1, has_src2;
        public boolean is_branch, is_func_call, is_func_ret, is_system, is_unsigned;

        public RVInstruction(int opcode) {
            binary = opcode;
        }

    @Override
    public void exec(iCPU cpu, int pc) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getLength() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String decode(iCPU cpu, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getCycle() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getOpcode() {
        return binary;
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    }
