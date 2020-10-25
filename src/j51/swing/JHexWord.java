/**
 * $Id: JHexWord.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.swing;

/**
 * Editor for hex Word (16 bit)
 *
 * @author Mario Viara
 * @version 1.00
 */
public class JHexWord extends JHexField
{
	public JHexWord(boolean bold)
	{
		super(4,bold);
	}
	public JHexWord()
	{
		super(4,false);

	}

	public JHexWord(int value,boolean bold)
	{
		super(4,bold);
		setValue(value);
		setValue(value);
	}
	
	public JHexWord(int value)
	{
		this(value,false);
	}
}
