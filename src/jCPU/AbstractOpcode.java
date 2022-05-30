/**
 * $Id: AbstractMCS51Opcode.java 70 2010-07-01 09:57:00Z mviara $
 */
package jCPU;

import j51.util.FastArray;
import j51.util.Hex;

/**
 *
 * Abstract class to implement one MCS51 instruction. This class manage
 * all the operation relative to description get/set counter and the
 * other method of MCS51Opcode not relative to the execution of the
 * instruction using this superclass the only method that a child must
 * implement is the exec and toString used for decode the instruction.
 *
 * @author Mario Viara
 * @version 1.01
 * 
 * 1.01	Added support for FastArray.
 */
public abstract class AbstractOpcode implements Opcode
{
	// Decoded istructions for opcode without 'decoders'
	private String decoded = null;
	
	// Opcode length
	protected int length;
	
	// Opcode description
	protected String description;
	
	// First byte of the opcode
	protected int opcode;

	// Number of machine cyle for execution time
	private final int cycle;
	
	static private FastArray<DecodeString> decoders = null;

	
	public AbstractOpcode(int opcode, int length, int cycle, String description)
	{
		this.opcode = opcode;
		this.length = length;
		this.cycle  = cycle;
		this.description = description;

		if (decoders == null){
			decoders = new FastArray<>();
			decoders.add(new DecodeDATA12());
			decoders.add(new DecodeDATA16());
			decoders.add(new DecodeCODE16());
			decoders.add(new DecodeDATA8());
			decoders.add(new DecodeOFFSET());
			decoders.add(new DecodeDIRECT());
			decoders.add(new DecodeDIRECP());
			decoders.add(new DecodeDIRECM());
			decoders.add(new DecodeBIT());
		}
	}


	public final int getOpcode()
	{
		return opcode;
	}

	public String toString()
	{
		return description;
	}

	public final int getLength()
	{
		return length;
	}

	public final int getCycle()
	{
		return cycle;
	}

	
	public final String decode(CPU cpu, int pc)
	{
		if (decoded != null)
			return decoded;
		
		StringBuffer sb  = new StringBuffer(Hex.bin2word(pc)+" ");
		for (int i = 0 ; i < 3 ; i++)
		{
			if (i < getLength()){
				sb.append(Hex.bin2byte(cpu.code(pc + i))).append(" ");
                        } else {
				sb.append("   ");
                        }
		}

		sb.append(getDescription());

		int opcode = cpu.code(pc);
		int end	= pc + getLength();
		pc++;
		
		boolean haveParameter = false;
		while (true)
		{
			int index = -1;
			int pos   = sb.length();
			DecodeString d;
			
			for (int i = decoders.size() ; --i >= 0;)
			{
				d = decoders.get(i);
				int p = d.search(sb);
				if (p != -1)
				{
					if (p < pos)
					{
						pos = p;
						index = i;
					}
				}
			}

			if (index < 0){
				break;
                        }

			d = (DecodeString)decoders.get(index);
			pc += d.decode(cpu, opcode, end, pc, sb, pos);
			haveParameter = true;
		}

		if (haveParameter == false){
			decoded = sb.toString();
                }
		return sb.toString();
	}

	/**
	 * Return the description of the instruction this implementation
	 * is based over ther convention that the method toString() return
	 * the instruction description with the first word separated by tab
	 * from the operand and expand the first word plus the tab in one
	 * fixed lenght string. Example if the toString() return "CPL\tC"
	 * will be expanded in "CPL   C", to have all the instrction
	 * description with the same length.
	 */
	public final String getDescription()
	{
		String s = toString();
		int n = s.indexOf('\t');

		if (n < 0){
			return s;
                }

		String s1 = s.substring(0, n);
		while (s1.length() < 6){
			s1 = s1 + " ";
                }
		s1 += s.substring(n + 1);

		return s1;
	}

}


interface DecodeString
{
	public int decode(CPU cpu, int opcode, int end, int pc, StringBuffer value, int pos);
	public int search(StringBuffer s);
}

abstract class AbstractDecodeString implements DecodeString
{
	public String name;

	AbstractDecodeString(String name)
	{
		this.name = name;
	}

        @Override
	public int search(StringBuffer s)
	{
		return s.toString().indexOf(name);
	}


}

class DecodeDATA12 extends AbstractDecodeString
{
	DecodeDATA12()
	{
		super("#DATA12");
	}

        @Override
	public int decode(CPU cpu, int opcode, int end, int pc, StringBuffer value, int pos)
	{
		value.replace(pos, pos + name.length(), "#" + Hex.bin2word(getAddress(cpu, pc)));

		return 2;
	}

	protected int getAddress(CPU cpu, int pc)
	{
		pc -= 1;
		int add = cpu.code(pc + 1) | ((cpu.code(pc + 0) << 3) & 0x700);
		add |= (pc + 2 ) & 0xF800;
		return add;
	}

}

class DecodeCODE16 extends AbstractDecodeString
{
	DecodeCODE16()
	{
		super("#CODE16");
	}

	public int decode(CPU cpu, int opcode, int end, int pc, StringBuffer value, int pos)
	{
		int hi = cpu.code(pc);
		int lo = cpu.code(pc + 1);
		String s = cpu.getCodeName(hi * 256 + lo);
		
		value.replace(pos, pos + name.length(), s);

		return 2;
	}
	
}

class DecodeDATA16 extends AbstractDecodeString
{
	DecodeDATA16()
	{
		super("#DATA16");
	}

	public int decode(CPU cpu, int opcode, int end, int pc, StringBuffer value, int pos)
	{
		int hi = cpu.code(pc);
		int lo = cpu.code(pc + 1);
		value.replace(pos, pos + name.length(), "#" + Hex.bin2byte(hi) + Hex.bin2byte(lo));

		return 2;
	}
}

class DecodeDATA8 extends AbstractDecodeString
{
	DecodeDATA8()
	{
		super("#DATA8");
	}

	public int decode(CPU cpu,int opcode,int end,int pc,StringBuffer value,int pos)
	{
		value.replace(pos,pos+name.length(),"#"+Hex.bin2byte(cpu.code(pc)));

		return 1;
	}
}

class DecodeBIT extends AbstractDecodeString
{
	DecodeBIT()
	{
		super("#BIT");
	}

	public int decode(CPU cpu,int opcode,int end,int pc,StringBuffer value,int pos)
	{
		value.replace(pos,pos+name.length(),cpu.getBitName(cpu.code(pc)));

		return 1;
	}
}

class DecodeOFFSET extends AbstractDecodeString
{
	DecodeOFFSET()
	{
		super("#OFFSET");
	}

	public int decode(CPU cpu,int opcode,int end,int pc,StringBuffer value,int pos)
	{
		int v = end;
		int offset = cpu.code(pc);

		if (offset < 128){
			v += offset;
                } else {
			v -= 0x100 - offset;
                }

		value.replace(pos,pos+name.length(),"#"+Hex.bin2word(v));

		return 1;
	}
}

class DecodeDIRECT extends AbstractDecodeString
{
	DecodeDIRECT()
	{
		super("DIRECT");
	}

	public int decode(CPU cpu,int opcode,int end,int pc,StringBuffer value,int pos)
	{
		int r = cpu.code(pc);

		value.replace(pos,pos+name.length(),cpu.getDirectName(r));

		return 1;
	}
}


class DecodeDIRECP extends AbstractDecodeString
{
	DecodeDIRECP()
	{
		super("DIRECP");
	}

	public int decode(CPU cpu,int opcode,int end,int pc,StringBuffer value,int pos)
	{
		int r = cpu.code(pc+1);

		value.replace(pos,pos+name.length(),cpu.getDirectName(r));

		return 1;
	}
}


class DecodeDIRECM extends AbstractDecodeString
{
	DecodeDIRECM()
	{
		super("DIRECM");
	}

        @Override
	public int decode(CPU cpu,int opcode,int end,int pc,StringBuffer value,int pos)
	{
		int r = cpu.code(pc-1);

		value.replace(pos,pos+name.length(),cpu.getDirectName(r));

		return 1;
	}
}
