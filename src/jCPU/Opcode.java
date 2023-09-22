/**
 * $Id: MCS51Opcode.java 45 2010-06-22 20:53:26Z mviara $
 */
package jCPU;

/**
 *
 * Interface for one instruction of the MCS51 processor.
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface Opcode
{

	/**
	 * Exec one instruction. It is important remember that the PC will
	 * be automatically incremented from the MCS51 emulator BEFORE 
	 * CALLING THIS METHOD. That means that the pc parameter is the old
	 * program counter and the function MCS51.pc() if used return the
	 * new one after the execution of the instruction.
	 *
	 * @param cpu - The MCS51 cpu
	 * @param pc - The location of the first byte of the opcode.
	 */
	public int exec(CPU cpu, int pc) throws Exception;

	/**
	 * @return the lenght in byte of the instruction.
	 */
	public int getLength();

	/**
	 * Decode one instruction
	 */
	public String decode(CPU cpu, int pc);

	/**
	 * @return the number of cycle machine necessary foo execute the
	 * instruction.
	 */
	public int getCycle();

	/**
	 * Get the opcode of the instruction
	 */
	public int getOpcode();

	/**
	 * @return the description of the instruction
	 */
	public String getDescription();

}
