/**
 * $Id: AbstractInterruptSource.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.intel;

/*
 * Abstract implementation of interrupt source.
 *
 * @author Mario Viara
 * @version 1.00
 */
public abstract class AbstractInterruptSource implements InterruptSource
{
	protected int vector;

	public AbstractInterruptSource(int vector)
	{
		this.vector = vector;
	}

	public void interruptStart()
	{
	}

	public void interruptStop()
	{
	}


	public int getInterruptVector()
	{
		return vector;
	}

	
}
