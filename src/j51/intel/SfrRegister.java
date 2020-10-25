/*
 * $Id: SfrRegister.java 70 2010-07-01 09:57:00Z mviara $
 */
package j51.intel;

import j51.util.Hex;
import j51.util.FastArray;

/**
 *
 * Special Function Register.
 *
 * @author Mario Viara
 * @version 1.02
 *
 * 1.02 New model based on MemoryByte
 * 1.01	Now value are not in the register for new memory model.
 */
public class SfrRegister extends MemoryByte
{
	FastArray<InterruptSource>	interruptSources = new FastArray<InterruptSource>();

	SfrRegister(int i)
	{
		setName(Hex.bin2byte(i));
	}

}

