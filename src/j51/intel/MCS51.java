/**
 * $Id: MCS51.java 75 2010-07-07 06:04:05Z mviara $
 */
package j51.intel;

import j51.util.*;
import j51.swing.*;
import jCPU.AbstractOpcode;
import jCPU.CPU;
import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;
import jCPU.Opcode;
import java.util.logging.Level;

/**
 * 
 * Main class to emulate a MCS51 family microprocessor. All the
 * instruction are emulated using the class MCS51Opcode. When the
 * constructor is called one array is filled with all the instruction
 * and the emulator use a sample array access when running.
 *
 * No peripheral are emulated from this class.
 *
 * For performance reason no method are syncronized that means that no
 * more then one thread can use this class.
 *
 * @author Mario Viara
 * @version 1.03
 *
 * 1.03	Most used method now are final for better performance.
 * 
 * 1.02 Added support for multiple SFR page shared.
 *	Added final to all method used during emulation.
 *	
 * 1.01 Added support for new model of memory management.
 *	Array now are created using FastArray.
 *	Improved performance of interrupt management.
 *	
 */
public class MCS51 implements MCS51Constants, jCPU.CPU
{
	private static Logger log = Logger.getLogger(MCS51.class);
	
	// Interrupt statistics
	private FastArray<InterruptStatistic>	interruptStatistics = new FastArray<>();
	private FastArray<InterruptSource>	interruptList = new FastArray<>();
	
	// Sfr register pages
	private SfrPage	sfrPages[];
	private SfrPage sfrCurrent;
	private int	sfrPage = -1;
	
	// SFR used as HI byte in MOVX A,Rx and MOVX @Rx,A
	private int sfrXdataHi = P0;
	
	// Cpu clock counter
	private long clock = 0;
	
	// Program counter
	private int pc = 0;
	
	// CPU clock oscillator
	private int oscillator = 12000000;
	
	// Machine cycle in number of cpu clock
	protected int machineCycle = 12;


	// Code name
	private String codeNames[] = new String[64 * 1024];
	

	// Bit name
	private String bitNames[] = new String[256];

	// Bit sfr register
	private int sfrBitmap[] = new int[256];
	

	// Break point area
	private boolean breakPoint[] = new boolean[65536];

	// Profiling
	private long executionCounter[] = new long[65536];
	
	// Call trap
	private CallListener callListeners[] = new CallListener[65536];
	
	// Xdata
	private byte xdata[];

	// Idata
	private byte idata[];

	// Idata name
	private String idataNames[];

	static private Opcode opcodes[] = new Opcode[256];
	private long opcodesCounter[] = new long[256];
	

	private FastArray<MCS51Peripheral>	peripherals		= new FastArray<>();
	private FastArray<Runnable>		runQueue		= new FastArray<>();
	private FastArray<InterruptSource>	interruptRequest	= new FastArray<>();

	/**
	 * Emulation listener
	 *
	 * @since 1.04
	 */
	private FastArray<EmulationListener> emulationListeners = new FastArray<>();
	
	// Reset listener
	private FastArray<ResetListener> resetListeners = new FastArray<>();
	
	// Polling list
	private FastArray<MachineCyclesListener> machineListeners	= new FastArray<>();
	
	// Vector with performance client
	private FastArray performance = new FastArray();

	// Current serving interrupt
	private InterruptSource currentInterrupt = null;

	// Vector with the asyncronous timer
	private FastArray<AsyncTimer> asyncTimers = new FastArray<>();

	
	private Code code;

	private int currentDptr;
	private int dptrs[] = new int[16];

	// Current register bank
	private int regPtr;

	private FastArray<UpdatableComponent> updatableComponents = new FastArray<>();
	private javax.swing.Timer updateTimer = null;

	// Current IE_EA bit
	private boolean ie = false;
	
	/**
	 * Hold the last instance created.
	 * 
	 * @since 1.04
	 */
	static private MCS51 current = null;
	
	static
	{
		initOpcodes();
	}
	
	/**
	 * Static function can be called from any class to set a break
	 * point at the current PC. Can be used for debug pourpose.
	 *
	 * @version 1.00
	 * @since 1.04
	 */
	static public void breakNext()
	{
		current.setBreakAtPC();
	}
	
	public MCS51()
	{
		this(12000000, 1);
	}

	public MCS51(int osc)
	{
		this(osc, 1);
	}
	
	public MCS51(int osc, int numSfrPage)
	{
		log.log(Level.INFO, "Created processor with {0} SFR pages, Clock {1}", new Object[]{numSfrPage, osc});

		setOscillator(osc);
		
		current = this;
		
		/**
		 * Create SFR page system
		 */
		sfrPage = -1;
		sfrPages = new SfrPage[numSfrPage];
		sfrPages[0] = new SfrPage(0);
		
		for (int i = 1 ; i < numSfrPage ; i++){
			sfrPages[i] = new SfrPage(i, sfrPages[0]);
                }
		setSfrPage(0);
		
		setCode(new VolatileCode());
		setCodeSize(64 * 1024);
		setXdataSize(64 * 1024);
		setIdataSize(256);

		// Internal memory
		for (int i = 0 ; i < 128 ; i++){
			setSfrBitmap(i, 0x20 + i / 8);
                }

		// Sfr register
		for (int i = 128 ; i < 256 ; i++){
			setSfrBitmap(i, i & 0xf8);
                }

		// Break point
		for (int i = 0 ; i < breakPoint.length ; i++){
			breakPoint[i] = false;
                }

		// Call listeners
		for (int i = 0 ; i < callListeners.length ; i++){
			callListeners[i] = null;
                }
		

		for (int i = 0 ; i < 64 * 1024 ; i++){
			setCodeName(i, "#" + Hex.bin2word(i));
                }

		/**
		 * SFR register name
		 */
		setSfrName(ACC,	"ACC");
		setSfrName(B,	"B");
		setSfrName(PSW,	"PSW");
		setSfrName(SP,	"SP");
		setSfrName(DPL,	"DPL");
		setSfrName(DPH,	"DPH");
		setSfrName(P0,	"P0");
		setSfrName(P0M1,"P0M1");
		setSfrName(P0M2,"P0M2");
		setSfrName(P1,	"P1");
		setSfrName(P1M1,"P1M1");
		setSfrName(P1M2,"P1M2");
		setSfrName(P2,	"P2");
		setSfrName(P2M1,"P2M1");
		setSfrName(P2M2,"P2M2");
		setSfrName(P3,	"P3");
		setSfrName(P3M1,"P3M1");
		setSfrName(P3M2,"P3M2");
		setSfrName(SCON,"SCON");
		setSfrName(SBUF,"SBUF");
		setSfrName(TCON,"TCON");
		setSfrName(TMOD,"TMOD");
		setSfrName(TH0,"TH0");
		setSfrName(TL0,"TL0");
		setSfrName(TH1,"TH1");
		setSfrName(TL1,"TL1");
		setSfrName(IE, "IE");

		setBitName(SCON+0,"RI");
		setBitName(SCON+1,"TI");
		setBitName(SCON+2,"RB8");
		setBitName(SCON+3,"TB8");
		setBitName(SCON+4,"REN");
		setBitName(SCON+5,"SM2");
		setBitName(SCON+6,"SM1");
		setBitName(SCON+7,"SM0");

		setBitName(TCON+7,"TF1");
		setBitName(TCON+6,"TR1");
		setBitName(TCON+5,"TF0");
		setBitName(TCON+4,"TR0");

		setBitName(IE+7,"EA");
		setBitName(IE+6,"EC");
		setBitName(IE+5,"ET2");
		setBitName(IE+4,"ES");
		setBitName(IE+3,"ET1");
		setBitName(IE+2,"EX1");
		setBitName(IE+1,"ET0");
		setBitName(IE+0,"EX0");
		

		setBitName(PSW+7,"CY");
		setBitName(PSW+6,"AC");
		setBitName(PSW+5,"F0");
		setBitName(PSW+4,"RS1");
		setBitName(PSW+3,"RS0");
		setBitName(PSW+2,"OV");
		setBitName(PSW+1,"F1");
		setBitName(PSW+0,"P");
		
		// Set default idata name
		for (int reg = 0 ; reg < 8 ; reg++){
			setIdataName(reg, "R" + reg);
                }

		/**
		 * Track PSW for current register set
		 */
		addSfrWriteListener(PSW, (int r, int v) -> {
                    regPtr = ((v >> 3) & 3) * 8;
                });

		/**
		 * Track IE for fast interrupt processing
		 */
		addSfrWriteListener(IE, (int r, int v) -> {
                    ie = (v & IE_EA) != 0;
                });
				
				
	}

	/**
	 * Set the emulation is used to notify the peripheral about the
	 * emulation state.
	 *
	 * @author Mario Viara
	 * @version 1.00
	 *
	 * @since 1.04
	 *
	 * @param mode - True emulation is enabled.
	 * 
	 */
	public final void setEmulation(boolean mode)
	{
		for (int i = emulationListeners.size() ; --i >= 0 ;){
			emulationListeners.get(i).setEmulation(mode);
                }
	}

	/**
	 * Add a new emulation listeners
	 *
	 * @author Mario Viara
	 * @version 1.00
	 *
	 * @since 1.04
	 *
	 * @param l - Emulation listener.
	 */
	public void addEmulationListener(EmulationListener l)
	{
		emulationListeners.add(l);
	}
	
	/**
	 * Create a open collector bit of memory mapped to SFR
	 *
	 * @author Mario Viara
	 * @version 1.00
	 * @since 1.04
	 * 
	 * @param sfr - Sfr register
	 * @param b - Bit number 0 -7
	 * @return Memory mapped bit
	 * 
	 */
	public OpenCollectorMemoryBit createSfrBitOc(int sfr,int b)
	{
		return new OpenCollectorMemoryBit(sfrCurrent,sfr,b);
	}
	
	public void addUpdatableComponent(UpdatableComponent c)
	{
		updatableComponents.add(c);
		
		if (updateTimer == null){
			updateTimer = new javax.swing.Timer(100, (java.awt.event.ActionEvent e) -> {
                            for (int i = updatableComponents.size() ; --i >= 0;){
                                updatableComponents.get(i).update();
                            }
                        });
			updateTimer.start();
		}
		
	}
	
	public Object getPeripheralByClass(Class c)
	{
		for (int i = 0 ; i < getPeripheralsCount() ; i++)
		{
			Object o = getPeripheralAt(i);
			if (c.isInstance(o)){
				return o;
                        }
		}
		return null;
	}
	
	public Object getPeripheralByClass(String name) throws Exception
	{
		Class c = Class.forName(name);
		return getPeripheralByClass(c);
	}
	
	public void setCallListener(int pc,CallListener l) throws Exception
	{
		if (callListeners[pc] != null){
			throw new Exception("Duplicate call trap at " + Hex.bin2word(pc));
                }
		callListeners[pc] = l;
	}
	
        @Override
	public CallListener getCallListener(int pc)
	{
		return callListeners[pc];
	}
	
	public Opcode getOpcode(int c)
	{
		return opcodes[c];
	}

	public String getOpcodeDescription(int c)
	{
		return opcodes[c].getDescription();
	}
	
	public long getOpcodeCounter(int c)
	{
		return opcodesCounter[c];
	}


	public void setCode(Code code)
	{
		this.code = code;
	}
	
	static private void initOpcodes()
	{
		
		// Reset opcode
		for (int i = 0 ; i < 256 ; i ++)
		{
			setOpcode(i, null);
		}
		
		// ACALL / AJMP
		for (int i = 0 ; i < 8 ; i++){
			setOpcode(new ACALL((i << 5)|0x11));
			setOpcode(new AJMP((i << 5)|0x01));
		}

		// Arithmetic instruction
		arithmetic(new ArithmeticADD(),0x20,"ADD");
		arithmetic(new ArithmeticADDC(),0x30,"ADDC");
		arithmetic(new ArithmeticANL(),0x50,"ANL");
		arithmetic(new ArithmeticORL(),0x40,"ORL");
		arithmetic(new ArithmeticSUBB(),0x90,"SUBB");
		arithmetic(new ArithmeticXRL(),0x60,"XRL");

		
		setOpcode(new ANL_DIRECT_A());
		setOpcode(new ANL_DIRECT_DATA());
		setOpcode(new ANL_C_DIRECT());
		setOpcode(new ANL_C_NOT_DIRECT());

		setOpcode(new CJNE_A_DIRECT());
		setOpcode(new CJNE_A_DATA());

		for (int r = 0 ; r < 8 ; r++)
		{
			setOpcode(new CJNE_R_DATA(r));
			setOpcode(new DEC_R(r));
			setOpcode(new DJNZ_R(r));
			setOpcode(new INC_R(r));
			setOpcode(new MOV_A_R(r));
			setOpcode(new MOV_R_A(r));
			setOpcode(new MOV_R_DIRECT(r));
			setOpcode(new MOV_R_DATA(r));
			setOpcode(new MOV_DIRECT_R(r));
			setOpcode(new XCH_A_R(r));
		}

		for (int r = 0 ; r < 2 ; r++)
		{
			setOpcode(new CJNE_RI_DATA(r));
			setOpcode(new DEC_RI(r));
			setOpcode(new INC_RI(r));
			setOpcode(new MOV_A_RI(r));
			setOpcode(new MOV_RI_A(r));
			setOpcode(new MOV_DIRECT_RI(r));
			setOpcode(new MOV_RI_DIRECT(r));
			setOpcode(new MOV_RI_DATA(r));
			setOpcode(new MOVX_A_RI(r));
			setOpcode(new MOVX_RI_A(r));
			setOpcode(new XCH_A_RI(r));
			setOpcode(new XCHD_A_RI(r));
		}

		setOpcode(new CLR_A());
		setOpcode(new CLR_C());
		setOpcode(new CLR_BIT());

		setOpcode(new CPL_A());
		setOpcode(new CPL_C());
		setOpcode(new CPL_BIT());

		setOpcode(new DA_A());

		setOpcode(new DEC_A());
		setOpcode(new DEC_DIRECT());

		setOpcode(new DIV_AB());
		
		setOpcode(new DJNZ_DIRECT());

		setOpcode(new INC_A());
		setOpcode(new INC_DIRECT());
		setOpcode(new INC_DPTR());

		setOpcode(new JB());
		setOpcode(new JBC());
		setOpcode(new JC());

		setOpcode(new JMP_A_DPTR());

		setOpcode(new JNB());
		setOpcode(new JNC());
		setOpcode(new JNZ());
		setOpcode(new JZ());

		setOpcode(new LCALL());
		setOpcode(new LJMP());

		setOpcode(new MOV_A_DIRECT());
		setOpcode(new MOV_A_DATA());
		setOpcode(new MOV_DIRECT_A());
		setOpcode(new MOV_DIRECT_DIRECT());
		setOpcode(new MOV_DIRECT_DATA());
		setOpcode(new MOV_C_BIT());
		setOpcode(new MOV_BIT_C());
		setOpcode(new MOV_DPTR_DATA16());
		setOpcode(new MOVC_A_DPTR_A());
		setOpcode(new MOVC_A_PC_A());
		setOpcode(new MOVX_A_DPTR());
		setOpcode(new MOVX_DPTR_A());
		setOpcode(new MUL_AB());
		setOpcode(new NOP());
		setOpcode(new ORL_C_BIT());
		setOpcode(new ORL_C_NBIT());
		
		setOpcode(new POP_DIRECT());
		setOpcode(new PUSH_DIRECT());
		setOpcode(new RET());
		setOpcode(new RETI());
		
		setOpcode(new RL_A());
		setOpcode(new RLC_A());
		setOpcode(new RR_A());
		setOpcode(new RRC_A());

		setOpcode(new SETB_C());
		setOpcode(new SETB_BIT());

		setOpcode(new SJMP());
		setOpcode(new SWAP_A());

		setOpcode(new XCH_A_DIRECT());

		setOpcode(new XRL_DIRECT_A());
		setOpcode(new XRL_DIRECT_DATA());
		setOpcode(new ORL_DIRECT_A());
		setOpcode(new ORL_DIRECT_DATA());

		setOpcode(new RESERVED());
		
	}

        @Override
	public int getSfrXdataHi()
	{
		return sfrXdataHi;
	}
	
	protected void setSfrXdataHi(int sfr)
	{
		sfrXdataHi = sfr;
	}
	
	public void addResetListener(ResetListener l)
	{
		resetListeners.add(l);
	}

	/**
	 * Add one new asyncronous timer listener.
	 *
	 * @param timeout - Time in machine cycle
	 * @param l    - Listener to be  called when the timer expire.
	 */
	public void addAsyncTimerListener(int timeout, AsyncTimerListener l)
	{
		for (int i = 0 ; i < asyncTimers.size() ; i++){
			AsyncTimer t = (AsyncTimer)asyncTimers.get(i);

			// Same timeout ?
			if (timeout == t.timeout){
				t.add(l);
				return;
			}

			// Less timeout ?
			if (timeout < t.timeout){
				AsyncTimer t1 = new AsyncTimer();
				t1.timeout = timeout;
				t1.add(l);
				t.timeout  = t.timeout - timeout;
				asyncTimers.add(i,t1);
				return;
			}
			timeout -= t.timeout;
		}

		AsyncTimer t1 = new AsyncTimer();
		t1.timeout = timeout;
		t1.add(l);
		asyncTimers.add(t1);
	}

	/**
	 * Add a new task to the run queue, to ensure that all change to
	 * the cpu will run in only one thread this method must be called
	 * for every event that modify SFR from another thread. For example
	 * one emulated swing component.
	 *
	 * @author Mario Viara
	 * @version 1.00
	 *
	 * @param r - Runnable task.
	 */
	public void addRunQueue(Runnable r)
	{
		synchronized (runQueue){
			runQueue.add(r);
		}
	}
	
	public void addAsyncTimerListenerMillis(int ms,AsyncTimerListener l)
	{
		addAsyncTimerListener(((oscillator / machineCycle) / 1000) * ms, l);
	}

	public void addInterruptSource(int sfr, InterruptSource source)
	{
		addInterruptSource(sfr, source, " INT");
	}

	public int getInterruptCount()
	{
		return interruptList.size();
	}

	public InterruptStatistic getInterruptAt(int i)
	{
		return interruptStatistics.get(i);
	}
	
	public void addInterruptSource(int sfr, InterruptSource source, String desc )
	{
		if (interruptList.indexOf(source) == -1){
			while (desc.length() < 16){
				desc += " ";
                        }
			interruptStatistics.add(new InterruptStatistic(source, desc + " AT 0x" + Hex.bin2word(source.getInterruptVector())));
			interruptList.add(source);
		}
		
		SfrRegister r = getSfr(sfr);
		
		r.interruptSources.add(source);
	}
	
	public void addMachineCycleListener(MachineCyclesListener p)
	{
		machineListeners.add(p);
	}
	
	public void addPerformanceListener(MCS51Performance p)
	{
		performance.remove(p);
		performance.add(p);
	}

	
	public void addSfrMemoryReadListener(int sfr, MemoryReadListener listener)
	{
		sfrCurrent.addMemoryReadListener(sfr, listener);
	}
	
	public void addSfrReadListener(int sfr, SfrReadListener listener)
	{
		addSfrMemoryReadListener(sfr, new MemoryReadListenerSfr(listener));
	}

	public void addSfrMemoryWriteListener(int sfr, MemoryWriteListener listener)
	{
		sfrCurrent.addMemoryWriteListener(sfr, listener);
	}
	
	public void addSfrWriteListener(int sfr, SfrWriteListener listener)
	{
		addSfrMemoryWriteListener(sfr, new MemoryWriteListenerSfr(listener));
	}
	
	public int getPeripheralsCount()
	{
		return peripherals.size();
	}

	protected void addPeripheral(MCS51Peripheral c)
	{
		c.registerCpu(this);
		peripherals.add(c);
	}

	public MCS51Peripheral getPeripheralAt(int i)
	{
		return peripherals.get(i);
	}

	static private  void arithmetic(ArithmeticOperation op,int basecode,String name)
	{
		// XXX A,Ri
		for (int i = 0 ; i < 8 ; i++){
			setOpcode(new Arithmetic(basecode|0x08|i, 1, op, name)
			{
                                @Override
				public int getValue(CPU cpu, int pc)
				{
					return cpu.r((int)(opcode & 7));
				}

                                @Override
				public String toString()
				{
					return description + "\tA,R" + (opcode & 7);
				}
			});
                }

		// XXX A,direct
		setOpcode(new Arithmetic(basecode|5, 2, op, name)
		{
                        @Override
			public int getValue(CPU cpu, int pc)
			{
				return cpu.getDirectCODE(pc + 1);
			}

                        @Override
			public String toString()
			{
				return description+"\tA,DIRECT";
			}
		});

		// XXX A,@Rx0
		for (int i = 0 ; i < 2 ; i++)
		{
			setOpcode(new Arithmetic(basecode|6|i, 1, op, name)
			{
				public int getValue(CPU cpu, int pc)
				{
					return cpu.idata(cpu.r((int)(opcode & 1)));
				}

				public String toString()
				{
					return description + "\tA,R" + (opcode & 1);
				}
			});

		}

		// XXX A,#data
		setOpcode(new Arithmetic(basecode|4, 2, op, name)
		{
			public int getValue(CPU cpu, int pc)
			{
				return cpu.code(pc + 1);
			}

			public String toString()
			{
				return description + "\tA,#DATA8";
			}
		});

	}
	
	static private void setOpcode(Opcode o)
	{
		setOpcode(o.getOpcode(), o);
	}
	
	static private void setOpcode(int i, Opcode o)
	{
		if (opcodes[i] != null)
		{
			System.out.println("Error " + Integer.toHexString(i) + " " + opcodes[i] + " e " + o);
			System.exit(1);
		}
		opcodes[i] = o;
	}

	public final int getDirectCODE(int pc)
	{
		return getDirect(code(pc));
	}

	public String getDirectName(int add)
	{
		if (add >= 128){
			return getSfrName(add);
                } else {
			return getIdataName(add);
                }

	}

	public final int getDirect(int add)
	{
		if (add >= 128){
			return sfr(add);
                } else {
			return idata(add);
                }
	}

	/**
	 * Read a value  using direct address. If the address is in the
	 * SFR space (128-255) the data are read using readDirect.
	 * 	 
	 * @author Mario viara
	 * @version 1.01
	 *
	 * @since 1.04
	 *
	 * @param add - Address 0 - 255
	 *
	 * @see Memory
	 */
	public final int getDirectDirect(int add)
	{
		if (add >= 128)
			return sfrCurrent.readDirect(add);
		else
			return idata(add);
	}
	
	public final void setDirect(int add,int value)
	{
		if (add >= 128)
			sfr(add,value);
		else
			idata(add,value);
	}

	public final void setBitName(int add,String name)
	{
		bitNames[add] = name;
	}
	
	public final String getBitName(int add)
	{
		int bit;

		if (bitNames[add] != null)
		{
			return bitNames[add];
		}
		
		bit = (add & 7);

		if (add < 128)
		{
			add = 0x20 + add / 8 ;
		} else {
			add = add & 0xf8;
		}

		return getDirectName(add)+"^"+bit;
	}
	
	public final boolean getBitCODE(int add)
	{
		return getBit(code(add));
	}

	/**
	 * Set the internal address of one bit
	 */
	public final void setSfrBitmap(int i, int add)
	{
		sfrBitmap[i] = add;
	}
	
	public boolean getBit(int add)
	{
		int value;
	
		value = getDirect(sfrBitmap[add]);
		
		return (value & ((1 << (add & 0x07)))) != 0;
	}

	
	public final void setBit(int add,boolean value)
	{
		int bit;
		int v;

		bit = 1 << (add & 7);
		add = sfrBitmap[add];
		v = getDirectDirect(add);
		if (value)
			v |= bit;
		else
			v &= ~bit;
		
		setDirect(add,(int)v);
		
	}

	
	public int sp()
	{
		return sfr(SP);
	}

	public void sp(int value)
	{
		sfr(SP,value);
	}
	
	public int pc()
	{
		return pc;
	}
	
	public void pc(int value)
	{
		this.pc = value & 0xffff;
	}

	public final int dpl()
	{
		return sfr(DPL);
	}

	public final void dpl(int value)
	{
		sfr(DPL,value);
	}
	
	public final int dph()
	{
		return sfr(DPH);
	}

	public final void dph(int value)
	{
		sfr(DPH,value);
	}

	public int getDptr(int n)
	{
		if (n == currentDptr)
			return dptr();
		return dptrs[n];
	}
	
	public final int dptr()
	{
		return sfr(DPH) * 256 | sfr(DPL);
	}

	public final void dptr(int value)
	{
		sfr(DPH,value>>8);
		sfr(DPL,value);
	}


	/**
	 * Set one regiter.
	 *
	 * @param r - Register to set (0-7)
	 * @param value - Value to assign to the register.
	 */
	public void r(int r,int value)
	{
		idata(regPtr+r,value);
	}

	
	public int r(int r)
	{
		return idata(regPtr+r);
	}

	public final int b()
	{
		return sfr(B);
	}

	public final void b(int value)
	{
		sfr(B,value);
	}


	public final int acc()
	{
		return sfr(ACC);
	}

	/**
	 * Return the accumulator
	 */
	public final void acc(int value)
	{
		sfr(ACC,value);
	}

	/**
	 * Return the current Program Status Word
	 *
	 * @version 1.00
	 */
	public final int psw()
	{
		return sfr(PSW);
	}

	public final void psw(int value)
	{
		sfr(PSW,value);
		
	}
	
	public final void cy(boolean value)
	{
		if (value)
			pswSet(PSW_CY);
		else
			pswReset(PSW_CY);
					
	}

	public boolean cy()
	{
		return ((psw() & PSW_CY) != 0);
	}

	public final void ac(boolean value)
	{
		if (value)
			pswSet(PSW_AC);
		else
			pswReset(PSW_AC);

	}

	public final boolean ac()
	{
		return ((psw() & PSW_AC) != 0);
	}

	public final void ov(boolean value)
	{
		if (value)
			pswSet(PSW_OV);
		else
			pswReset(PSW_OV);

	}

	public final boolean ov()
	{
		return ((psw() & PSW_OV) != 0);
	}

	private final void pswSet(int value)
	{
		sfr(PSW,sfr(PSW) | value);
	}

	private final void pswReset(int value)
	{
		sfr(PSW,sfr(PSW) & ~value);
	}
	
	private void setCodeSize(int size)
	{
		code.setCodeSize(size);
	}

	public int getXdataSize()
	{
		return xdata.length;
	}
	
	protected void setXdataSize(int size)
	{
		xdata = new byte[size];
		
		for (int i = 0 ; i < size ; i++)
		{
			xdata[i] = 0;
		}
	}

	private void setIdataSize(int size)
	{
		idata = new byte[size];
		idataNames = new String[size];
		for (int i = 0 ; i < size ; i++)
		{
			idata[i] = 0;
			setIdataName(i,Hex.bin2byte(i));
		}
	}

	private void setIdataName(int reg,String name)
	{
		idataNames[reg] = name;
	}

	public String getIdataName(int reg)
	{
		return idataNames[reg];
	}

	/**
	 * Set Code name
	 */
	public void setCodeName(int i, String name)
	{
		codeNames[i] = name;
	}

	public String getCodeName(int i)
	{
		return codeNames[i];
	}
	
	/**
	 * Set a SFR name
	 *
	 * @version 1.01
	 */
	public void setSfrName(int reg, String name)
	{
		sfrCurrent.setName(reg,name);
	}

	/**
	 * Return the name of SFR register.
	 *
	 * @version 1.01
	 */
	public String getSfrName(int reg)
	{
		return sfrCurrent.getName(reg);
	}
	
	public void code(int addr, int value)
	{
		code.setCode(addr, value);
	}

	public final int getCodeSize()
	{
		return code.getCodeSize();
	}

	public final int code(int addr, boolean move)
	{
		return code.getCode(addr,move);
	}

	public final int code(int addr)
	{
		return code(addr,false);
	}

	public final int code16(int addr,boolean move)
	{
		return code.getCode16(addr,move);
	}
				
	public final int code16(int addr)
	{
		return code16(addr,false);
	}
	
	public int xdata(int add)
	{
		int value = xdata[add & 0xffff] & 0xff;

		return value;
	}
	
	public void xdata(int add,int value)
	{
		xdata[add] = (byte)value;
	}

	public final int idata(int add)
	{
		return idata[add] & 0xff;
	}
	
	public final void idata(int add,int value)
	{
		idata[add] = (byte)value;
	}

	private int setSfrPage(int page)
	{
		if (page != sfrPage){
			int current = sfrPage;
			sfrCurrent = sfrPages[page];
			sfrPage = page;

			return current;
		} else {
			return page;
                }
	}
	
	public final SfrRegister getSfr(int sfr)
	{
		return sfrCurrent.getReg(sfr);
	}

        @Override
	public int sfr(int add)
	{
		return sfrCurrent.read(add);
	}


	public final boolean sfrIsSet(int add,int mask)
	{
		return ((sfr(add) & mask) == mask);
	}
	
	public final void sfrSet(int add,int mask)
	{
		sfr(add,sfr(add)|mask);
	}

	
	public final void sfrReset(int add,int mask)
	{
		sfr(add,sfr(add) & ~mask);
	}

	public final int sfr16(int hi,int low)
	{
		return sfr(hi) * 256 + sfr(low);
	}

	public final void sfr16(int value,int hi,int low)
	{
		sfr(hi,value >> 8);
		sfr(low,value);
	}
	
	public  final void sfr(int add,int value)
	{
		sfrCurrent.write(add,value);
		SfrRegister r = getSfr(add);

		if (sfrCurrent.getWriteListener()){
			for (int i = 0 ;i < r.interruptSources.size() ; i++){
				InterruptSource is = r.interruptSources.get(i);
				if (is.interruptCondition()){
					if (!interruptRequest.contains(is)){
						int ii = interruptList.indexOf(is);
						
						if (ii != -1){
							interruptStatistics.get(ii).incCounter();
							
						}
						
						interruptRequest.add(is);
					}
				}
			}
		}
	}

	/**
	 * Reset the CPU.
	 *
	 * @author Mario Viara
	 * @version 1.00
	 */
	public void reset()
	{
		log.info("Begin Reset");
		setSfrPage(0);
		
		for (int i = 0 ;i < executionCounter.length ; i++){
			executionCounter[i] = 0;
                }
		
		// Reset opcode counter
		for (int i = 0 ; i < 256 ; i++){
			opcodesCounter[i] = 0;
                }
		
		// Clear Xdata area
		for (int i = 0 ; i < xdata.length ; i++){
			xdata(i,0);
                }

		// Clear idata area
		for (int i = 0 ; i < idata.length ; i++){
			idata(i,0);
                }

		// Set default SFR
		sfrCurrent.setWriteListener(false);
				
		for (int i = 0 ; i < 256 ; i++){
			sfr(i,0);
                }
		sfrCurrent.setWriteListener(true);


		// Set stack pointer
		sfr(SP,7);
		
		// Set program counter
		pc = 0;

		// Reset clock counter
		clock = 0;

		currentDptr = 0;

		// Clear all DPTR
		for (int i = 0 ; i < dptrs.length ; i ++){
			dptrs[i] = 0;
                }

		// Call all listener
		for (int i = 0 ; i < resetListeners.size(); i++){
			resetListeners.get(i).reset(this);
                }

		log.info("End Reset");

	}

	/**
	 * Swap dptr
	 */
	public void swapDptr(int n)
	{
		if (n >= dptrs.length){
			n = dptrs.length - 1;
                }
		
		if (n != currentDptr){
			dptrs[currentDptr] = dptr();
			dptr(dptrs[n]);
			currentDptr = n;
			
		}
	}

	/**
	 * Pop a word (16 bit from the stack
	 */
	public int popw() throws Exception
	{
		int sp = sfr(SP);
		int value = idata(sp) << 8;
		
		if (--sp < 0){
			throw new Exception("Stack underflow at " + Hex.bin2word(pc));
                }
		value |= idata(sp);
		
		if (--sp < 0){
			throw new Exception("Stack underflow at " + Hex.bin2word(pc));
                }
		sfr(SP, sp);

		return value;
		
	}

	/**
	 * Pop a int from the stack. First read the internal memory at the
	 * address SP then decrement the SP and return the value.
	 *
	 * @return the value 'popped'
	 */
	public int pop() throws Exception
	{
		int sp = sfr(SP);
		int value = idata(sp);
		if (--sp < 0){
			throw new Exception("Stack underflow at " + Hex.bin2word(pc));
                }
		sfr(SP, sp);
		
		return value;
	}

        @Override
	public void pushw(int value) throws Exception
	{
		int sp = sfr(SP);
		if (++sp > 255){
			throw new Exception("Stack overflow at "+Hex.bin2word(pc));
                }
		idata(sp,value);
		if (++sp > 255){
			throw new Exception("Stack overflow at "+Hex.bin2word(pc));
                }
		idata(sp,value >> 8);
		sfr(SP,sp);
	}
	
	public void push(int value) throws Exception
	{
		int sp = sfr(SP);
		if (++sp > 255){
			throw new Exception("Stack overflow at "+Hex.bin2word(pc));
                }
		idata(sp,value);
		sfr(SP,sp);
	}

	public final void eoi()
	{
		if (currentInterrupt != null){
			if (currentInterrupt.interruptCondition()){
				interruptRequest.add(currentInterrupt);
			}
			currentInterrupt = null;
		}
	}

	public void go() throws Exception
	{
		go(-1);
	}
	
	public void go(int limit) throws Exception
	{
		int emulatedTime = 0;
		long startTime = System.currentTimeMillis();
		long realTime;
		long statTime = startTime;
		long now;
		int elapsed;
		int sleepCounter = 0;
		final int running = 10;
		final int statistics = 5000;

		if (limit != -1){
			breakPoint[limit] = true;
		}
		
		// Cycle for ms
		int cyclems = (oscillator * running) / 1000;

		while (true){
			// Run running ms
			int cycle = cyclems;
			int count;

			do{
				count = execute();
				clock += count;
				cycle -= count;

				if (breakPoint[pc]){
					if (pc == limit){
						breakPoint[limit] = false;
                                        }
					throw new InterruptedException("Break point at " + Hex.bin2word(pc));
				}
			} while (cycle > 0);

			checkRunQueue();

			now = System.currentTimeMillis();
			realTime  = now - startTime;
			emulatedTime += running;
			elapsed = (int)(now - statTime);
			
			if (elapsed >= statistics){
				int perc = ((elapsed - sleepCounter)*100)/elapsed;
				statTime = now;
				sleepCounter = 0;
				
				for (int i = 0 ; i < performance.size() ; i++){
					((MCS51Performance)performance.get(i)).cpuPerformance(perc,elapsed);
				}

				/**
				 * Release control to other thread like
				 * swing ...
				 */
				if (perc >= 99){
					Thread.sleep(10);
					sleepCounter = 10;
				}

			}
			
			int delay = (int)(emulatedTime - realTime);
			if (delay > running){
				startTime = now+delay;
				sleepCounter += delay;
				emulatedTime = 0;
				Thread.sleep(delay);
			}
		}
	}

	public void pass() throws Exception
	{
		Opcode o = opcodes[code(pc)];
		int newPc = pc + o.getLength();
		go(newPc);

	}

	public long getExecutionCounter(int addr)
	{
		return executionCounter[addr];
	}

	/**
	 * Check the run queue
	 */
	private final void checkRunQueue()
	{
		if (runQueue.size() > 0){
			synchronized (runQueue){
				for (int i = runQueue.size() - 1 ; --i >= 0 ; ){
					runQueue.get(i).run();
                                }
				runQueue.clear();
			}
                }
	}

	public int step() throws Exception
	{
		checkRunQueue();
		int count = execute();
		clock += count;
		return count;
	}
	
	/**
	 * Exec one single instructions.
	 *
	 * @return The number of cycle machine elapsed.
	 */
	public int execute() throws Exception
	{
		int i;

		/**
		 * Check interrupt request
		 */
		if (currentInterrupt == null && ie && interruptRequest.size() > 0){
			currentInterrupt = interruptRequest.get(0);
			interruptRequest.remove(0);
			
			pushw(pc);
			pc(currentInterrupt.getInterruptVector());
			currentInterrupt.interruptStart();
			return 2;
		}

		// Exec one instruction
		executionCounter[pc] ++;
		
		int oldPc = pc;
		int c = code.getCode(oldPc, false);
		opcodesCounter[c] ++;
		Opcode o = opcodes[c];
		int cycle = o.getCycle();
		
		pc = pc + o.getLength();
		//log.fine("EXEC "+getDecodeAt(oldPc));
			   
		o.exec(this, oldPc);
		
		// Check machine cycle pollers
		for ( i = machineListeners.size()  ; --i >= 0 ;){
			machineListeners.get(i).cycles(cycle);
                }

		// Check async timers
		while (asyncTimers.size() > 0){
			AsyncTimer a = asyncTimers.get(0);

			a.timeout -= cycle;
			if (a.timeout <= 0){
				// Remove the list
				asyncTimers.remove(0);

				// Call all the listeners
				for (i = a.size()  ; --i >= 0;){
					AsyncTimerListener l = (AsyncTimerListener)a.get(i);
					l.expired(this);
				}
			} else {
				break;
                        }
		}
		
		return cycle * machineCycle;
	}
	
	public boolean getBreakPoint(int pc)
	{
		return breakPoint[pc];
	}

	public void setBreakAtPC()
	{
		setBreakPoint(pc, true);
	}
	
	public void setBreakPoint(int pc, boolean mode)
	{
		breakPoint[pc] = mode;
	}

	public String getDecodeAt(int pc)
	{
		Opcode o = opcodes[code(pc)];
		return o.decode(this, pc);
	}
	
	public String getDescriptionAt(int pc)
	{
		Opcode o = opcodes[code(pc)];
		return o.getDescription();
	}

	public int getLengthAt(int pc)
	{
		Opcode o = opcodes[code(pc)];
		return o.getLength();
	}
	

	public void setOscillator(int oscillator)
	{
		if (oscillator != this.oscillator){
			log.log(Level.INFO, "Set oscillator {0}", oscillator);
			this.oscillator = oscillator;
		}
	}
	
	public int getOscillator()
	{
		return oscillator;
	}

	public long clock()
	{
		return clock;
	}


	public void machineCycle(int n)
	{
		machineCycle = n;
	}
	
	public int machineCycle()
	{
		return machineCycle;
	}
	
        @Override
	public String toString()
	{
		return "Intel MCS51 $Id: MCS51.java 75 2010-07-07 06:04:05Z mviara $";
	}

	static public void main(String args[])
	{
		MCS51 cpu = new MCS51();
		cpu.reset();
	}

}


class RESERVED extends AbstractOpcode
{
	RESERVED()
	{
		super(0xa5,1,1,"RESERVED");
	}

        @Override
	public void exec(CPU cpu, int pc) throws Exception
	{
		throw new Exception("Invalid opcode : A5");
	}

}

class ANL_DIRECT_A extends AbstractOpcode
{
	ANL_DIRECT_A()
	{
		super(0x52,2,1,"ANL");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.getDirect(add) & cpu.acc());
	}

	public String toString()
	{
		return description + "\tA,DIRECT";
	}
}

class ANL_DIRECT_DATA extends AbstractOpcode
{
	ANL_DIRECT_DATA()
	{
		super(0x53, 3, 2, "ANL");
	}

	public void exec(CPU cpu, int pc)
	{
		int add = cpu.code(pc + 1);
		cpu.setDirect(add, (int)(cpu.getDirect(add) & cpu.code(pc + 2)));
	}

	public String toString()
	{
		return description + "\tDIRECT,#DATA8";
	}

}

class XRL_DIRECT_A extends AbstractOpcode
{
	XRL_DIRECT_A()
	{
		super(0x62,2,1,"XRL");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) ^ cpu.acc()));
	}
}

class XRL_DIRECT_DATA extends AbstractOpcode
{
	XRL_DIRECT_DATA()
	{
		super(0x63,3,2,"XRL\tDIRECT,#DATA8");
	}

	
	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) ^ cpu.code(pc+2)));
	}
}

class ORL_DIRECT_A extends AbstractOpcode
{
	ORL_DIRECT_A()
	{
		super(0x42,2,1,"ORL\tDIRECT,A");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) | cpu.acc()));
	}
}

class ORL_DIRECT_DATA extends AbstractOpcode
{
	ORL_DIRECT_DATA()
	{
		super(0x43,3,2,"ORL\tDIRECT,#DATA8");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) | cpu.code(pc+2)));
	}
}

class ANL_C_DIRECT extends AbstractOpcode
{
	ANL_C_DIRECT()
	{
		super(0x82,2,2,"ANL");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.cy(cpu.getBit(cpu.code(pc+1)) & cpu.cy());
		
	}

	public String toString()
	{
		return description+"\tC,#BIT";
	}
}


class ANL_C_NOT_DIRECT extends AbstractOpcode
{
	ANL_C_NOT_DIRECT()
	{
		super(0xb0,2,2,"ANL");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.cy(!(cpu.getBit(cpu.code(pc+1)) & cpu.cy()));
	}

	public String toString()
	{
		return description+"\tC,NOT #BIT";
	}

}

abstract class JR extends AbstractOpcode
{
	
	
	JR(int opcode,int len,int cycle,String desc)
	{
		super(opcode,len,cycle,desc);
	}

	protected final void jr(CPU cpu,int pc,int offset)
	{
		pc = pc + length;

		if (offset < 128)
			pc += offset;
		else
			pc -= 0x100 - offset;
		
		cpu.pc(pc);
	}
}

class SJMP extends JR
{
	SJMP()
	{
		super(0x80,2,2,"SJMP\t#OFFSET");
	}

	public final void exec(CPU cpu,int pc)
	{
		jr(cpu,pc,cpu.code(pc+1));
	}
}

abstract class CJNE extends JR
{
	CJNE(int opcode,int len,int cycle,String desc)
	{
		super(opcode,len,cycle,desc);
	}

	protected final void cjne(CPU cpu,int pc,int op1,int op2,int offset)
	{
		
		if (op1 < op2)
		{
			cpu.cy(true);
		} else {
			cpu.cy(false);
		}

		if (op1 != op2){
			jr(cpu,pc,offset);
                }
	}


}

class CJNE_A_DIRECT extends CJNE
{
	CJNE_A_DIRECT()
	{
		super(0xb5,3,2,"CJNE\tA,DIRECT,#OFFSET");
	}

	public void exec(CPU cpu, int pc)
	{
		cjne(cpu, pc, cpu.acc(), cpu.getDirectCODE(pc + 1), cpu.code(pc + 2));
	}
}

class CJNE_A_DATA extends CJNE
{
	CJNE_A_DATA()
	{
		super(0xb4,3,2,"CJNE\tA,#DATA8,#OFFSET");
	}

	public void exec(CPU cpu,int pc)
	{
		cjne(cpu,pc,cpu.acc(),cpu.code(pc+1),cpu.code(pc+2));
	}
}
   
class CJNE_R_DATA extends CJNE
{
	CJNE_R_DATA(int r)
	{
		super(0xb8+r,3,2,"CJNE");
	}

	public void exec(CPU cpu,int pc)
	{
		cjne(cpu,pc,cpu.r((int)(opcode & 7)),cpu.code(pc+1),cpu.code(pc+2));
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#DATA8,#OFFSET";
	}
}

class CJNE_RI_DATA extends CJNE
{
	CJNE_RI_DATA(int r)
	{
		super(0xb6+r,3,2,"CJNE");
	}

	public void exec(CPU cpu,int pc)
	{
		cjne(cpu, pc, cpu.idata(cpu.r((int)(opcode & 1))), cpu.code(pc + 1), cpu.code(pc + 2));
	}

	public String toString()
	{
		return description + "\t@R" + (opcode & 1) + ",#DATA8,#OFFSET";
	}

}

class CLR_A extends AbstractOpcode
{
	public CLR_A()
	{
		super(0xe4, 1, 1, "CLR\tA");
	}

	public void exec(CPU cpu, int pc)
	{
		cpu.acc((int)0);
	}
}

class CLR_C extends AbstractOpcode
{
	public CLR_C()
	{
		super(0xc3, 1, 1, "CLR\tC");
	}

	public void exec(CPU cpu, int pc)
	{
		cpu.cy(false);
	}
}

class CLR_BIT extends AbstractOpcode
{
	public CLR_BIT()
	{
		super(0xc2,2,1,"CLR\t#BIT");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.setBit(cpu.code(pc + 1), false);
	}
}

class CPL_A extends AbstractOpcode
{
	public CPL_A()
	{
		super(0xf4,1,1,"CPL\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.acc((int)~cpu.acc());
	}
}


class CPL_C extends AbstractOpcode
{
	public CPL_C()
	{
		super(0xb3,1,1,"CPL\tC");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.cy(!cpu.cy());
	}
}

class CPL_BIT extends AbstractOpcode
{
	public CPL_BIT()
	{
		super(0xb2,2,1,"CPL\t#BIT");
	}

	public void exec(CPU cpu,int pc)
	{
		int bit = cpu.code(pc+1);
		cpu.setBit(bit, !cpu.getBit(bit));
	}
}

class DA_A extends AbstractOpcode
{
	public DA_A()
	{
		super(0xd4,1,1,"DA\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		int a = cpu.acc();

		if ((a & 0x0f)	> 9 || cpu.ac())
		{
			a += 6;
			if ((a & 0xf0) != (cpu.acc() & 0xf0)){
				cpu.cy(true);
                        }
		}

		if ((a & 0xf0) > 0x90 || cpu.cy())
		{
			a += 0x60;
			if (a  > 255)
				cpu.cy(true);
		}

		cpu.acc((int)a);
	}
}

class XCH_A_R extends AbstractOpcode
{
	public XCH_A_R(int r)
	{
		super(0xc8|r,1,1,"XCH");
	}

	public void exec(CPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		int tmp = cpu.acc();
		cpu.acc(cpu.r(r));
		cpu.r(r,tmp);
	}

	public String toString()
	{
		return description + "\tA,R" + (opcode & 7);
	}

}

class XCH_A_RI extends AbstractOpcode
{
	public XCH_A_RI(int r)
	{
		super(0xc6|r,1,1,"XCH");
	}

	public void exec(CPU cpu, int pc)
	{
		int r = cpu.r((int)(opcode & 1));
		int tmp = cpu.acc();
		cpu.acc(cpu.idata(r));
		cpu.idata(r, tmp);
	}

	public String toString()
	{
		return description + "\tA,@R" + (opcode & 1);
	}

}

class XCHD_A_RI extends AbstractOpcode
{
	public XCHD_A_RI(int r)
	{
		super(0xd6|r,1,1,"XCHD");
	}

	public void exec(CPU cpu,int pc)
	{
		int r = cpu.r((int)(opcode & 1));
		int tmp = cpu.acc();
		cpu.acc((int)((cpu.acc() & 0xf0) | (cpu.idata(r) & 0x0f)));
		cpu.idata(r,(int)((cpu.idata(r) & 0xf0) | (tmp) & 0x0f));
	}

	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}


class XCH_A_DIRECT extends AbstractOpcode
{
	public XCH_A_DIRECT()
	{
		super(0xc5,2,1,"XCH\tA,DIRECT");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		int tmp = cpu.acc();
		cpu.acc(cpu.getDirect(add));
		cpu.setDirect(add,tmp);
	}

}

class SWAP_A extends AbstractOpcode
{
	public SWAP_A()
	{
		super(0xc4,1,1,"SWAP\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		int a = cpu.acc();
		cpu.acc((int)((a >> 4) & 0x0f | (a << 4)));
	}

}

class DEC_A extends AbstractOpcode
{
	public DEC_A()
	{
		super(0x14,1,1,"DEC\tA");
	}
	
	public void exec(CPU cpu,int pc)
	{
		cpu.acc((int)(cpu.acc() - 1));
	}
	
}

class DEC_R extends AbstractOpcode
{
	public DEC_R(int r)
	{
		super(0x18|r,1,1,"DEC");
	}

	public void exec(CPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		cpu.r(r,(int)(cpu.r(r) - 1));
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7);
	}
}

class DEC_RI extends AbstractOpcode
{
	public DEC_RI(int r)
	{
		super(0x16|r,1,1,"DEC");
	}

	public void exec(CPU cpu,int pc)
	{
		int address = cpu.r((int)(opcode & 1));
		cpu.idata(address,cpu.idata(address ) -1 );
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1);
	}

}

class DEC_DIRECT extends AbstractOpcode
{
	public DEC_DIRECT()
	{
		super(0x15,2,1,"DEC\tDIRECT");
	}

	public void exec(CPU cpu,int pc)
	{
		int direct = cpu.code(pc+1);
		cpu.setDirect(direct,(int)(cpu.getDirect(direct) - 1));
	}

}

class DIV_AB extends AbstractOpcode
{
	public DIV_AB()
	{
		super(0x84,1,4,"DIV\tAB");
	}

	public void exec(CPU cpu,int pc)
	{
		int a,b;

		a = cpu.acc();
		b = cpu.b();

		cpu.acc((int)(a/b));
		cpu.b((int)(a % b));
		cpu.cy(false);
		cpu.ov(false);
	}
}

class MUL_AB extends AbstractOpcode
{
	public MUL_AB()
	{
		super(0xa4,1,4,"MUL\tAB");
	}

	public void exec(CPU cpu,int pc)
	{
		int value = cpu.acc() * cpu.b();

		cpu.b((int)(value >> 8));
		cpu.acc((int)value);
		cpu.cy();
		cpu.ov((value > 255));
	}
}

abstract class DJNZ extends JR
{
	DJNZ(int opcode,int len,int cycle,String desc)
	{
		super(opcode,len,cycle,desc);
	}

	protected final void jnz(MCS51 cpu,int pc,int value)
	{
		value &= 0xff;
		if (value != 0)
			jr(cpu,pc,cpu.code(pc+getLength() - 1));
	}
}

class DJNZ_R extends DJNZ
{
	DJNZ_R(int r)
	{
		super(0xd8|r,2,2,"DJNZ");
	}

	public void exec(CPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		int value = (int)(cpu.r(r) - 1);
		cpu.r(r,value);
		jnz((MCS51) cpu,pc,value);
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#OFFSET";
	}

}

class DJNZ_DIRECT extends DJNZ
{
	DJNZ_DIRECT()
	{
		super(0xd5,3,2,"DJNZ\tDIRECT,#OFFSET");
	}

	public void exec(CPU cpu,int pc)
	{
		int address = cpu.code(pc+1);
		int value = cpu.getDirect(address) - 1;
		cpu.setDirect(address,value);
		jnz((MCS51) cpu, pc, value);
	}
}

class INC_A extends AbstractOpcode
{
	INC_A()
	{
		super(4,1,1,"INC\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.acc((int)(cpu.acc()+1));
	}
	
}

class INC_R extends AbstractOpcode
{
	INC_R(int r)
	{
		super(8|r,1,1,"INC");
	}

	public void exec(CPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		cpu.r(r,(int)(cpu.r(r)+1));
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7);
	}

}

class INC_RI extends AbstractOpcode
{
	INC_RI(int r)
	{
		super(6|r,1,1,"INC");
	}

	public void exec(CPU cpu,int pc)
	{
		int i = cpu.r((int)(opcode & 1));
		cpu.setDirect(i,(int)(cpu.getDirect(i) + 1));
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1);
	}

}

class INC_DIRECT extends AbstractOpcode
{
	INC_DIRECT()
	{
		super(5,2,1,"INC\tDIRECT");
	}

	public void exec(CPU cpu,int pc)
	{
		int a = cpu.code(pc+1);
		cpu.setDirect(a,(int)(cpu.getDirect(a)+1));
	}

}

class INC_DPTR extends AbstractOpcode
{
	INC_DPTR()
	{
		super(0xa3,1,2,"INC\tDPTR");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.dptr(cpu.dptr()+1);
	}

}

class JB extends JR
{
	JB()
	{
		super(0x20,3,2,"JB\t#BIT,#OFFSET");
	}

	public final void exec(CPU cpu, int pc)
	{
		if (cpu.getBitCODE(pc + 1))
			jr(cpu, pc, cpu.code(pc + 2));
	}
}

class JBC extends JR
{
	JBC()
	{
		super(0x10,3,2,"JBC\t#BIT,#OFFSET");
	}

	public final void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		if (cpu.getBit(add))
		{
			cpu.setBit(add,false);
			jr(cpu,pc,cpu.code(pc+2));
		}
	}
}

class JNB extends JR
{
	JNB()
	{
		super(0x30,3,2,"JNB\t#BIT,#OFFSET");
	}

	public final void exec(CPU cpu,int pc)
	{
		boolean bit = cpu.getBitCODE(pc+1);

		if (!bit)
			jr(cpu,pc,cpu.code(pc+2));
	}
}

class JNC extends JR
{
	JNC()
	{
		super(0x50,2,2,"JNC\t#OFFSET");
	}

	public final void exec(CPU cpu,int pc)
	{
		if (!cpu.cy()){
			jr(cpu,pc,cpu.code(pc+1));
                }
	}
}


class JC extends JR
{
	JC()
	{
		super(0x40,2,2,"JC\t#OFFSET");
	}

	public final void exec(CPU cpu,int pc)
	{
		if (cpu.cy()){
			jr(cpu,pc,cpu.code(pc+1));
                }
	}
}

class JNZ extends JR
{
	JNZ()
	{
		super(0x70,2,2,"JNZ\t#OFFSET");
	}

	public final void exec(CPU cpu,int pc)
	{
		if (cpu.acc() != 0)
			jr(cpu,pc,cpu.code(pc+1));
	}
}


class JZ extends JR
{
	JZ()
	{
		super(0x60,2,2,"JZ\t#OFFSET");
	}

	public final void exec(CPU cpu,int pc)
	{
		if (cpu.acc() == 0)
			jr(cpu,pc,cpu.code(pc+1));
	}
}


class JMP_A_DPTR extends AbstractOpcode
{
	JMP_A_DPTR()
	{
		super(0x73,1,2,"JMP\t@A+DPTR");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.pc(cpu.dptr()+cpu.acc());
	}
}

   
interface ArithmeticOperation
{
	public void calc(CPU cpu,int value);
}


class LCALL extends AbstractOpcode
{
	
	LCALL()
	{
		super(0x12,3,2,"LCALL\t#CODE16");
	}

	public void exec(CPU cpu,int pc) throws Exception
	{
		int address = cpu.code16(pc+1);
		CallListener l = cpu.getCallListener(address);

		if (l != null)
		{
			l.call((MCS51) cpu,address);
		} else {
			cpu.pushw(pc+3);
			cpu.pc(address);
		}
	}
	
}

class LJMP extends AbstractOpcode
{
	LJMP()
	{
		super(0x2, 3, 2, "LJMP\t#CODE16");
	}

	public void exec(CPU cpu, int pc)
	{
		cpu.pc(cpu.code16(pc + 1));
		//cpu.pc((cpu.code(pc+1) << 8) | cpu.code(pc+2));
	}

}


class MOV_A_R extends AbstractOpcode
{
	public MOV_A_R(int r)
	{
		super(0xe8|r,1,1,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.acc(cpu.r((int)(opcode & 7)));
	}

	public String toString()
	{
		return description+"\tA,R"+(opcode & 7);
	}

}

class MOV_DPTR_DATA16 extends AbstractOpcode
{
	public MOV_DPTR_DATA16()
	{
		super(0x90,3,2,"MOV\tDPTR,#DATA16");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.dptr((cpu.code(pc+1) << 8) | cpu.code(pc+2));
	}
}

class POP_DIRECT extends AbstractOpcode
{
	public POP_DIRECT()
	{
		super(0xd0,2,2,"POP\tDIRECT");
	}

	public void exec(CPU cpu,int pc) throws Exception
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.pop());
	}
}

class PUSH_DIRECT extends AbstractOpcode
{
	public PUSH_DIRECT()
	{
		super(0xc0, 2, 2, "PUSH\tDIRECT");
	}

	public void exec(CPU cpu, int pc) throws Exception
	{
		cpu.push(cpu.getDirectCODE(pc + 1));
	}
}

class RET extends AbstractOpcode
{
	public RET()
	{
		super(0x22,1,2,"RET");
	}

	public void exec(CPU cpu,int pc) throws Exception
	{
		cpu.pc(cpu.popw());
	}
}

class RETI extends AbstractOpcode
{
	public RETI()
	{
		super(0x32,1,2,"RETI");
	}

	public void exec(CPU cpu,int pc) throws Exception
	{
		cpu.pc(cpu.popw());
		cpu.eoi();
	}
}

class RL_A extends AbstractOpcode
{
	public RL_A()
	{
		super(0x23,1,1,"RL\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a << 1;
		if ((cpu.acc() & 0x80) != 0){
			a |= 1;
                }
		cpu.acc((int)a);
	}
}

class RR_A extends AbstractOpcode
{
	public RR_A()
	{
		super(0x3,1,1,"RR\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a >> 1;
		if ((cpu.acc() & 0x01) != 0)
			a |= 0x80;
		cpu.acc((int)a);
	}
}


class SETB_BIT extends AbstractOpcode
{
	public SETB_BIT()
	{
		super(0xd2,2,1,"SETB\t#BIT");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.setBit(cpu.code(pc+1),true);
	}
}

class SETB_C extends AbstractOpcode
{
	public SETB_C()
	{
		super(0xd3,1,1,"SETB\tC");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.cy(true);
	}
}


class RRC_A extends AbstractOpcode
{
	public RRC_A()
	{
		super(0x13,1,1,"RRC\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a >> 1;
		if (cpu.cy()){
			a |= 0x80;
                }
		
		cpu.cy((cpu.acc() & 1) != 0);
			
		cpu.acc((int)a);
	}
}


class RLC_A extends AbstractOpcode
{
	public RLC_A()
	{
		super(0x33,1,1,"RLC\tA");
	}

	public void exec(CPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a << 1;
		if (cpu.cy())
			a |= 1;
		cpu.cy((a & 0x100) != 0) ;
		cpu.acc(a);
	}
}


class ORL_C_BIT extends AbstractOpcode
{
	public ORL_C_BIT()
	{
		super(0x72,2,2,"ORL\tC,#BIT");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.cy(cpu.getBitCODE(pc+1)|cpu.cy());
	}
}

class ORL_C_NBIT extends AbstractOpcode
{
	public ORL_C_NBIT()
	{
		super(0xA0,2,2,"ORL\tC,NOT #BIT");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.cy(cpu.cy() | !cpu.getBitCODE(pc+1));
	}
}

class MOV_C_BIT extends AbstractOpcode
{
	public MOV_C_BIT()
	{
		super(0xa2,2,1,"MOV\tC,#BIT");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.cy(cpu.getBitCODE(pc+1));
	}
}

class MOV_BIT_C extends AbstractOpcode
{
	public MOV_BIT_C()
	{
		super(0x92,2,2,"MOV\t#BIT,C");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.setBit(cpu.code(pc+1),cpu.cy());
	}
}

class MOV_RI_A extends AbstractOpcode
{
	public MOV_RI_A(int r)
	{
		super(0xf6|r,1,1,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.r((int)(opcode & 1));
		cpu.idata(add,cpu.acc());
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",A";
	}

}

class MOV_RI_DIRECT extends AbstractOpcode
{
	public MOV_RI_DIRECT(int r)
	{
		super(0xa6|r,2,2,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.r((int)(opcode & 1));
		cpu.idata(add,cpu.getDirectCODE(pc+1));
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",DIRECT";
	}

}

class MOV_RI_DATA extends AbstractOpcode
{
	public MOV_RI_DATA(int r)
	{
		super(0x76|r,2,1,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.r((int)(opcode & 1));
		cpu.idata(add,(int)(cpu.code(pc+1)));
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",#DATA8";
	}

}

class MOV_R_A extends AbstractOpcode
{
	public MOV_R_A(int r)
	{
		super(0xf8|r,1,1,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.r((int)(opcode & 7),cpu.acc());
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",A";
	}

}

class MOV_R_DIRECT extends AbstractOpcode
{
	public MOV_R_DIRECT(int r)
	{
		super(0xa8|r,2,2,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.r((int)(opcode & 7),cpu.getDirectCODE(pc+1));
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",DIRECT";
	}

}

class MOV_DIRECT_R extends AbstractOpcode
{
	public MOV_DIRECT_R(int r)
	{
		super(0x88|r,2,2,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.r((int)(opcode & 7)));
	}

	public String toString()
	{
		return description+"\tDIRECT,R"+(opcode & 7);
	}

}

class MOV_DIRECT_DATA extends AbstractOpcode
{
	public MOV_DIRECT_DATA()
	{
		super(0x75,3,2,"MOV\tDIRECT,#DATA8");
	}

	public void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.code(pc+2));
	}
}

class MOV_R_DATA extends AbstractOpcode
{
	public MOV_R_DATA(int r)
	{
		super(0x78|r,2,1,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.r((int)(opcode & 7),cpu.code(pc+1));
	}
	
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#DATA8";
	}

}

class MOV_A_RI extends AbstractOpcode
{
	public MOV_A_RI(int r)
	{
		super(0xe6|r,1,1,"MOV");
	}

	public void exec(CPU cpu,int pc)
	{
		cpu.acc(cpu.idata(cpu.r((int)(opcode & 1))));
	}

	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}

class MOV_A_DIRECT extends AbstractOpcode
{
	public MOV_A_DIRECT()
	{
		super(0xe5,2,1,"MOV\tA,DIRECT");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.acc(cpu.getDirectCODE(pc+1));
	}
}

class MOVC_A_DPTR_A extends AbstractOpcode
{
	public MOVC_A_DPTR_A()
	{
		super(0x93,1,2,"MOVC\tA,@DPTR+A");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.acc(cpu.code(cpu.dptr()+cpu.acc()));
	}
}


class MOVX_A_DPTR extends AbstractOpcode
{
	public MOVX_A_DPTR()
	{
		super(0xe0,1,2,"MOVX\tA,@DPTR");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.acc(cpu.xdata(cpu.dptr()));
	}
}


class MOVX_DPTR_A extends AbstractOpcode
{
	public MOVX_DPTR_A()
	{
		super(0xf0,1,2,"MOVX\t@DPTR,A");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.xdata(cpu.dptr(),cpu.acc());

	}
}


class MOVC_A_PC_A extends AbstractOpcode
{
	public MOVC_A_PC_A()
	{
		super(0x83,1,2,"MOVC\tA,@PC+A");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.acc(cpu.code(pc+1+cpu.acc()));
	}
}

class MOVX_A_RI extends AbstractOpcode
{
	public MOVX_A_RI(int r)
	{
		super(0xe2|r,1,2,"MOVX");
	}

	public final void exec(CPU cpu,int pc)
	{
		int offset = cpu.sfr(cpu.getSfrXdataHi()) << 8;
		offset += cpu.idata(cpu.r((int)(opcode & 1)));
		cpu.acc(cpu.xdata(offset));
	}
        
	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}

class MOVX_RI_A extends AbstractOpcode
{
	public MOVX_RI_A(int r)
	{
		super(0xf2|r,1,2,"MOVX");
	}

	public final void exec(CPU cpu, int pc)
	{
		int offset = cpu.sfr(cpu.getSfrXdataHi()) << 8;
		offset += cpu.idata(cpu.r((int)(opcode & 1)));
		cpu.xdata(offset,cpu.acc());
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",A";
	}

}

class MOV_DIRECT_DIRECT extends AbstractOpcode
{
	public MOV_DIRECT_DIRECT()
	{
		super(0x85,3,2,"MOV\tDIRECP,DIRECM");
	}

	public final void exec(CPU cpu,int pc)
	{
		int source = cpu.code(pc+1);
		int dest = cpu.code(pc+2);
		cpu.setDirect(dest,cpu.getDirect(source));
	}
}

class MOV_A_DATA extends AbstractOpcode
{
	public MOV_A_DATA()
	{
		super(0x74,2,1,"MOV\tA,#DATA8");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.acc(cpu.code(pc+1));
	}
}

class MOV_DIRECT_A extends AbstractOpcode
{
	public MOV_DIRECT_A()
	{
		super(0xf5,2,1,"MOV\tDIRECT,A");
	}

	public final void exec(CPU cpu,int pc)
	{
		cpu.setDirect(cpu.code(pc+1),cpu.acc());
	}
}

class MOV_DIRECT_RI extends AbstractOpcode
{
	public MOV_DIRECT_RI(int r)
	{
		super(0x86|r,2,2,"MOV");
	}

        @Override
	public final void exec(CPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.idata(cpu.r((int)(opcode & 1))));
	}

        @Override
	public String toString()
	{
		return description+"\tDIRECT,@R"+(opcode & 1);
	}

}

class NOP extends AbstractOpcode
{
	NOP()
	{
		super(0,1,1,"NOP");
	}

        @Override
	public final void exec(CPU cpu,int pc)
	{
		
	}
	
}

class ArithmeticANL implements ArithmeticOperation
{
        @Override
	public final void calc(CPU cpu,int value)
	{
		cpu.acc((int)(cpu.acc() & value));
	}
}

class ArithmeticORL implements ArithmeticOperation
{
	public final void calc(CPU cpu,int value)
	{
		cpu.acc((int)(cpu.acc() | value));
	}
}

class ArithmeticXRL implements ArithmeticOperation
{
	public final void calc(CPU cpu,int value)
	{
		cpu.acc((int)(cpu.acc() ^ value));
	}
}


class ArithmeticADD implements ArithmeticOperation
{
	protected int result;
	
	protected boolean op(int acc,int value,int c,int mask)
	{
		result = (acc & mask) + (value & mask) + c;
		return (result & (mask + 1)) != 0;
	}
	
	protected final void add(CPU cpu,int value,int c)
	{
		int acc = cpu.acc();
		cpu.ac(op(acc, value, c, 0x0f));
		boolean cy7 = op(acc, value, c, 0x7F);
		cpu.cy(op(acc, value, c, 0xff));
		cpu.ov(cpu.cy() != cy7);
		cpu.acc(result);
	}
	
        @Override
	public void calc(CPU cpu,int value)
	{
		add(cpu, value, 0);
	}

}

class ArithmeticADDC extends ArithmeticADD
{

	
	public void calc(CPU cpu, int value)
	{
		int c = cpu.cy() ? 1 : 0;
		add(cpu, value, c);
	}

}

class ArithmeticSUBB extends ArithmeticADDC
{
	
        @Override
	protected boolean op(int acc, int value, int c, int mask)
	{
		result = (acc & mask) - (value & mask) - c;
		return (result & (mask + 1)) != 0;
	}


}

abstract class Arithmetic extends AbstractOpcode
{
	ArithmeticOperation op;
	
	public Arithmetic(int opcode, int length, ArithmeticOperation op, String name)
	{
		super(opcode, length, 1, name);
		this.op = op;
	}

        @Override
	public void exec(CPU cpu, int pc)
	{
		op.calc(cpu,getValue(cpu, pc));
	}

	abstract int getValue(CPU cpu, int pc);
}

class ACALL extends AbstractOpcode
{
	
	public ACALL(int opcode)
	{
		super(opcode,2,2,"ACALL\t#DATA12");
	}
	
	protected ACALL(int opcode,String name)
	{
		super(opcode,2,2,name);
	}
		
	protected final int getAddress(MCS51 cpu,int pc)
	{
		int add = cpu.code(pc+1) | ((opcode << 3) & 0x700);
		add |= (pc + 2 )	& 0xF800;
		return add;
	}
	
	public void exec(CPU cpu, int pc) throws Exception
	{
		int address = getAddress((MCS51) cpu, pc);
		CallListener l = cpu.getCallListener(address);
		if (l != null)
		{
			l.call((MCS51) cpu,address);
		} else {
			cpu.pushw(pc+2);
			cpu.pc(getAddress((MCS51) cpu, pc));
		}
	}

}

class AJMP extends ACALL
{
	public AJMP(int opcode)
	{
		super(opcode,"AJMP\t#DATA12");
	}


	public final void exec(CPU cpu, int pc)
	{

		cpu.pc(getAddress((MCS51) cpu, pc));
	}

		
}

class AsyncTimer extends FastArray
{
	public int timeout = 0;
}
