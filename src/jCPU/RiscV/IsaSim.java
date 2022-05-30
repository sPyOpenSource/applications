package jCPU.RiscV;

/*
 * RISC-V Instruction Set Simulator
 * 
 * A simple single-cycle simulator of the RV32IM instruction set architecture.
 * 
 * @author Hans Jakob Damsgaard (hansjakobdamsgaard@gmail.com)
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IsaSim extends CPU{
    // Insert path to binary file containing RISC-V instructions
    public final static String FILEPATH = "tests/task3/loop.bin";

    // Initial value of the program counter (default is zero)
    public final static Integer INITIAL_PC = 0;

    // Initial value of the stack pointer (default is 2^31 - 1)
    public final static Integer INITIAL_SP = Integer.MAX_VALUE;

    // Activate/deactivate debugging prints (default is true)
    public final static Boolean DEBUGGING = true;

    // Static variables used throughout the simulator
    static int pc = INITIAL_PC; // Program counter (counting in bytes)
    static int reg[] = new int[32]; // 32 registers

    // A single memory for instructions and data allowing "byte addressing"
    // using different integer key values (pc and sp counting in bytes)
    public static Memory ram = new Memory();
    boolean offsetPC = false, breakProgram = false; // For determining next pc value
    int cc = 0; // Clock cycle counter

    public IsaSim() {
            System.out.println("Hello RISC-V World!");
        try {
            ram.readBinary(FILEPATH, INITIAL_PC); // Read instructions into memory
        } catch (IOException ex) {
            Logger.getLogger(IsaSim.class.getName()).log(Level.SEVERE, null, ex);
        }
            reg[2] = INITIAL_SP; // Reset stack pointer

    }
        
    @Override
    public void go(int limit) throws Exception{
        while(true){
             RVInstruction ins = new RVInstruction(ram.readWord(pc));
             decode_riscv_binary(ins);
            step();
        }
    }
    
                @Override
		public int step () {
			// Combine four bytes to produce a single instruction
			int instr = ram.readWord(pc);

			// Retrieve the least significant seven bits of the instruction
			// indicating the type of instruction
			int opcode = instr & 0x7f;

			// Execute the instruction based on its type
			switch (opcode) {
			case LUI_MASK:
				if (DEBUGGING) {
					System.out.println(cc + " LUI instruction");
				}
				loadUpperImmediate(instr);
				break;

			case AUIPC_MASK:
				if (DEBUGGING) {
					System.out.println(cc + " AUIPC instruction");
				}
				addUpperImmediatePC(instr);
				break;

			case JAL_MASK:
				if (DEBUGGING) {
					System.out.println(cc + " JAL instruction");
				}
				jumpAndLink(instr);
				offsetPC = true;
				break;

			case JALR_MASK:
				if (DEBUGGING) {
					System.out.println(cc + " JALR instruction");
				}
				jumpAndLinkRegister(instr);
				offsetPC = true;
				break;

			case BRANCH_MASK:
				if (DEBUGGING) {
					System.out.println(cc + " Branch instruction");
				}
				offsetPC = branchInstruction(instr);
				break;

			case LOAD_MASK:
				if (DEBUGGING) {
					System.out.println(cc + " Load instruction");
				}
				loadInstruction(instr);
				break;

			case STORE_MASK:
				if (DEBUGGING) {
					System.out.println(cc + " Store instruction");
				}
				storeInstruction(instr);
				break;

			case OP_IMM_MASK: // Immediate instructions
				if (DEBUGGING) {
					System.out.println(cc + " Immediate instruction");
				}
				immediateInstruction(instr);
				break;

			case OP_MASK: // Arithmetic
				if (DEBUGGING) {
					System.out.println(cc + " Arithmetic instruction");
				}
				arithmeticInstruction(instr);
				break;

			case FENCE_MASK: // Fence (NOT IMPLEMENTED)
				break;

			case CSR_MASK: // Ecalls and CSR (SOME IMPLEMENTED, SOME LEFT OUT)
				breakProgram = ecallInstruction();
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
			if (breakProgram || !ram.containsKey(pc)) {
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
                        return 1;
		}
		//System.out.println("Program exit");

	public static void loadUpperImmediate(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = instr & 0xFFFFF000;
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", imm = " + imm);
		}
		reg[rd] = imm; // LUI stores the immediate in the top 20 bits of register rd
	}

	public static void addUpperImmediatePC(int instr) {
		// General information
		int rd = (instr >> 7) & 0x1F;
		int imm = instr & 0xFFFFF000;
		if (DEBUGGING) {
			System.out.println("rd = " + rd + ", imm = " + imm);
		}
		reg[rd] = pc + imm;
	}

	public static void jumpAndLink(int instr) {
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
	}

	public static void jumpAndLinkRegister(int instr) {
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
		pc = (reg[rs1] + imm) & 0xFFFFFFFE; // Jump target address sets LSB to 0
		if (pc % 4 != 0) {
			System.out.println("Instruction fetch exception; pc not multiple of 4 bytes");
		}
	}

	public static boolean branchInstruction(int instr) {
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

	public static void loadInstruction(int instr) throws NullPointerException {
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
			memValue = ram.readByte(memAddr);
			if ((memValue >> 7) == 1) {
				memValue |= 0xFFFFFF00; // Sign-extension if necessary
			}
			break;
		case 0x1: // LH
			memValue = ram.readHalfWord(memAddr);
			if ((memValue >> 15) == 1) {
				memValue |= 0xFFFF0000; // Sign-extension if necessary
			}
			break;
		case 0x2: // LW
			memValue = ram.readWord(memAddr);
			break;
		case 0x4: // LBU
			memValue = ram.readByte(memAddr);
			break;
		case 0x5: // LHU
			memValue = ram.readHalfWord(memAddr);
		}
		reg[rd] = memValue;
	}
	
	public static void storeInstruction(int instr) {
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
			ram.storeByte(memAddr, memValue);
			break;
		case 0x1: // SH
			memValue &= 0x0000FFFF;
			ram.storeHalfWord(memAddr, memValue);
			break;
		case 0x2: // SW
			ram.storeWord(memAddr, memValue);
			break;
		}
	}

	public static void immediateInstruction(int instr) {
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
	}

	public static void arithmeticInstruction(int instr) {
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
	}

	public static boolean ecallInstruction() {
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
			 Logger.getLogger(IsaSim.class.getName()).log(Level.SEVERE, null, e);
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
			 Logger.getLogger(IsaSim.class.getName()).log(Level.SEVERE, null, e);
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