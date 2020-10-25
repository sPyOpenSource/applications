/*
 * $Id: InterruptStatistic.java 46 2010-06-22 21:04:52Z mviara $
 */

package j51.intel;


/**
 *
 * Interrupt statistics.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class InterruptStatistic
{
	long	counter = 0;
	String	desc;
	InterruptSource	source;

	InterruptStatistic(InterruptSource is,String desc)
	{
		this.source = is;
		this.desc = desc;
		counter = 0;
	}

	public long getCounter()
	{
		return counter;
	}

	void incCounter()
	{
		counter++;
	}

	public String toString()
	{
		return desc;
	}
}

