
package j51.intel.graph.inst;

/**
 *
 * @author xuyi
 */
public class ArithmeticSUBB extends ArithmeticADDC
{
	
        @Override
	protected boolean op(int acc, int value, int c, int mask)
	{
		result = (acc & mask) - (value & mask) - c;
		return (result & (mask + 1)) != 0;
	}

}
