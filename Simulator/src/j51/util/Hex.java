/**
 * $Id: Hex.java 71 2010-07-02 06:55:21Z mviara $
 */
package j51.util;

/**
 *
 * Binary to ascii and ascii to binary conversion.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class Hex
{
	
	static private String d[] = null;

	static
	{
		d = new String[256];
		for (int i = 0 ; i < 256 ; i++)
		{
			String s = Integer.toHexString(i);
			if (s.length() < 2)
				s = "0"+s;
			d[i] = s.toUpperCase();
		}

		
	}
	
	static public int getDigit(String line,int pos) throws Exception
	{
		int c = line.charAt(pos);

		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		if (c >= '0' && c <= '9')
			return c - '0';

		throw new Exception("Invalid digit '"+line.charAt(pos)+"'");
	}
	
	static public int getByte(String line,int pos) throws Exception
	{
		return (getDigit(line,pos+0) << 4) | getDigit(line,pos+1);
	}

	static public int getWord(String line,int pos) throws Exception
	{
		return (getByte(line,pos+0) << 8) | getByte(line,pos+2);
	}

	static public String bin2word(int value)
	{
		return  bin2byte(value >> 8) + bin2byte(value);

	}
	
	static public String bin2byte(int value)
	{
		return d[value & 0xff];
	}

	static public String bin2dword(long value)
	{
		return bin2word((int)(value >> 16))+bin2word((int)value);

	}
}
