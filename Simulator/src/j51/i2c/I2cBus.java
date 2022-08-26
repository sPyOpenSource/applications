/**
 * $Id: I2cBus.java 57 2010-06-25 07:18:48Z mviara $
 */
package j51.i2c;

import java.util.Vector;

import j51.util.Logger;
import j51.util.Hex;
import j51.intel.OpenCollectorMemoryBit;
import j51.intel.MCS51;
import java.util.logging.Level;

/**
 * I2C Bus manager. This peripheral manage all the operation between
 * one master (the CPU) and one or more slave connected using the I2C
 * protocol. To implement the interface the CPU must have two
 * OpenCollectorMemoryBit mapped to phisical register.
 * 
 * <p>The following bus condition are supported:
 * <ul>
 *   <li> Start
 *   <li> Repeated start
 *   <li> Send byte
 *   <li> Recv byte
 *   <li> Stop
 * </ul>
 * 
 * @author Mario Viara
 * @version $Version$
 *
 * @since 1.04
 *
 * @see I2cSlave
 */
public class I2cBus implements jCPU.MemoryWriteListener,j51.intel.MCS51Peripheral,j51.intel.ResetListener
{
	private static final Logger log = Logger.getLogger(I2cBus.class);

	/** Memory bit connected to SCL wire */
	protected OpenCollectorMemoryBit	SCL;
	
	/** Memory bit connected to SDA Wire */
	protected OpenCollectorMemoryBit	SDA;
	
	private	boolean oldScl,oldSda;
	private	int state;
	private	int bitCount;
	private	int value;
	private	I2cSlave ph;
	private	boolean ack;
	private	boolean read;
	private	int address;
	private	boolean ignore;
	private	int byteCount;
	private	boolean lastByte;
	private Vector<I2cSlave> slaves = new Vector<>();
	
	/**
	 * Default constructor
	 */
	public I2cBus(OpenCollectorMemoryBit _scl,OpenCollectorMemoryBit _sda)
	{
		setScl(_scl);
		setSda(_sda);
	}

	public void registerCpu(MCS51 cpu)
	{
		cpu.addResetListener(this);
	}
	
	
	/**
	 * Set the memory bit used for SCL
	 *
	 * @param scl - Memory bit.
	 */
	public void setScl(OpenCollectorMemoryBit scl)
	{
		this.SCL = scl;
		SCL.addMemoryWriteListener(this);
	}

	/**
	 * Set the memory bit used for SDA
	 *
	 * @param sda - Memory bit
	 */
	public void setSda(OpenCollectorMemoryBit sda)
	{
		this.SDA = sda;
		SDA.addMemoryWriteListener(this);
	}


	public void reset(MCS51 cpu)
	{
		SDA.set(true);
		SCL.set(true);
		oldSda = SDA.get();
		oldScl = SCL.get();

		i2cIdle();
	}

	/**
	 * Set the interface to idle. Forget any bus condition.
	 */
	private void i2cIdle()
	{
		bitCount = 0;
		ph = null;
		ack = false;
		read = false;
		ignore = false;
		lastByte = false;
		byteCount = 0;
	}

	/**
	 * Called when a start or repeted start condition is detected
	 */
	protected void i2cStart()
	{
		bitCount = 0;
		if (ph != null)
		{
			I2cSlave tmp = ph;
			log.fine("Repeated start condition");
			i2cIdle();
			ph = tmp;
		}

		else
		{
			log.fine("Start condition");
			i2cIdle();
		}
	}

	/**
	 * Called when the stop condition is detected
	 */
	protected void i2cStop()
	{
		log.fine("Stop condition");
		i2cIdle();
	}

	public void addSlave(I2cSlave s)
	{
		slaves.add(s);
	}
	
	/**
	 * Search a slave with the specified address. Return true if
	 * one device is found or false if no device is found.
	 */
	private boolean searchSlave(int a)
	{
		// Use only the address
		a &= 0xFE;

		for (int i = 0 ; i < slaves.size(); i++)
		{
			ph = slaves.elementAt(i);
			
			if (ph.i2cAddress(a))
				return true;
		}

		return false;
	}


	/**
	 * Process one received byte
	 */
	protected void i2cRecv(int value) 
	{
		log.log(Level.FINE, "I2CRECV {0}", Hex.bin2byte(value));

		if (ph == null)
		{
			address = value;
			if (searchSlave(value))
			{
				log.log(Level.FINE, "Found {0}", ph);
				ack = ph.i2cWrite(byteCount++,value);
			}
			else
				log.log(Level.INFO, "Not found Peripheral at {0}", Hex.bin2byte(value));
		}
		else
			ack = ph.i2cWrite(byteCount++,value);


	}

	/**
	 * Called when is necessary to send one byte.
	 */
	protected void i2cSend(boolean sda) 
	{
		if (lastByte)
			return;

		switch (bitCount)
		{
			case	0:
				value = ph.i2cRead(byteCount++) & 0xff;
				log.log(Level.FINE, "I2CSEND {0} count {1}", new Object[]{Hex.bin2byte(value), byteCount});
			default:
				if ((value & 0x80) != 0)
				{
					SDA.set(true);
				}
				else
				{
					SDA.set(false);
				}
				value <<= 1;
				bitCount++;
				break;
			case	8:
				lastByte = sda;
				log.log(Level.FINER, "Last byte {0}", lastByte);
				bitCount++;
				break;
		}

		log.log(Level.FINER, "Send Clock {0} Bit {1} value {2}", new Object[]{bitCount, SDA.get(), Hex.bin2byte(value)});

	}

	/**
	 * Called when a new bit is received
	 */
	protected void i2cRecv(boolean sda) 
	{
		switch (bitCount)
		{
			case	0:
				value = 0;
			default:
				value <<= 1;
				if (sda)
					value |= 1;
				if (++bitCount == 8)
					i2cRecv(value);

				break;
			case	8:
				bitCount++;
				break;
		}
	}

	/**
	 * Called on the rising edge of the clock
	 */
	protected void i2cRise(boolean bit) 
	{
		log.finer("I2CRise "+bit);

		if (read)
			i2cSend(bit);
		else
			i2cRecv(bit);
	}

	/**
	 * Called on the failing edge od the clock.
	 */
	protected void i2cFail(boolean bit) 
	{
		log.log(Level.FINER, "I2CFail {0}", bit);
		if (bitCount == 8)
		{
			if (read)
			{
				log.finer("Release SDA");
				SDA.set(true);
			}
			else
			{
				if (ack)
				{
					log.finer("ACK SDA");
					SDA.set(false);
				}
				else
				{
					SDA.set(true);
					log.finer("NACK SDA");
				}
			}

		}
		else if (bitCount == 9)
		{
			ack = false;
			if (!read)
				SDA.set(true);
			bitCount = 0;

			if (byteCount == 1)
			{
				read = (value & 1) != 0;
				if (read)
					log.finer("Read operation");
				else
					log.finer("Write operation");

			}

		}
	}

	/**
	 * Bus manager.
	 * 
	 * This function is the main implementation of the I2C bus and
	 * process the bit change.
	 *
	 * @param scl - Current value of clock.
	 * @param sda - Current value of data.
	 */
	protected void bus(boolean scl,boolean sda) 
	{
		if (ignore)
			return;


		if (oldScl == scl && sda == oldSda)
			return;

		ignore = true;

		log.log(Level.FINEST, "ENTER SCL={0},SDA={1} OSCL={2},OSDA={3} LSCL={4},LSDA={5}", new Object[]{scl, sda, oldScl, oldSda, SCL.getLocal(), SDA.getLocal()});



		if (scl && oldScl && oldSda && !sda)
			i2cStart();

		if (scl && oldScl && !oldSda && sda)
			i2cStop();

		if (!oldScl && scl)
			i2cRise(sda);

		if (oldScl && !scl)
			i2cFail(sda);

		oldScl = SCL.get();
		oldSda = SDA.get();
		ignore = false;

		log.log(Level.FINEST, "LEAVE SCL={0},SDA={1} OSCL={2},OSDA={3} LSCL={4},LSDA={5}", new Object[]{scl, sda, oldScl, oldSda, SCL.getLocal(), SDA.getLocal()});

	}

	/**
	 * Implementation of listener to receive write on the bit
	 * mapped to the wire of the I2cBUS.
	 */
	public void writeMemory(int address,int newValue,int oldValue)
	{
		bus(SCL.get(),SDA.get());
	}
}

