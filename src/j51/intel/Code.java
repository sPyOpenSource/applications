/**
 * $Id: Code.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

/**
 *
 * Interface to code space
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface Code extends Memory
{
	public void setCodeSize(int size);
	public int  getCodeSize();
	public void setCode(int addr,int value);
	public int  getCode(int addr,boolean move);
	public int  getCode16(int addr,boolean move);
	
}
