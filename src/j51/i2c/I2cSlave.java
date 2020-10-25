/**
 * $Id: I2cSlave.java 57 2010-06-25 07:18:48Z mviara $
 */
package j51.i2c;

/**
 * Interface for I2c slave peripheral conncted to the I2CBus.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 *
 * @see I2cbus
 */
public interface  I2cSlave 
{
	/**
	 * Write one byte
	 */
	public boolean i2cWrite(int count,int value) ;
	
	/**
	 * Read one byte
	 */
	public int i2cRead(int count) ;

	/**
	 * Check if the address is managed by this peripheral
	 */
	public boolean i2cAddress(int address);
}
