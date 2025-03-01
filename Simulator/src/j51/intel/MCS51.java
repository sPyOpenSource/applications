/**
 * $Id: MCS51.java 75 2010-07-07 06:04:05Z mviara $
 */
package j51.intel;

import j51.intel.graph.*;
import j51.intel.graph.inst.*;
import j51.util.*;
import j51.swing.*;

import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;
import jCPU.Opcode;
import jCPU.iCPU;
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
public class MCS51 implements MCS51Constants, jCPU.iCPU
{
	private static Logger log = Logger.getLogger(MCS51.class);
	
	// Interrupt statistics
	private FastArray<InterruptStatistic> interruptStatistics = new FastArray<>();
	private FastArray<InterruptSource> interruptList = new FastArray<>();
	
	// Sfr register pages
	private SfrPage	sfrPages[];
	private SfrPage sfrCurrent;
	private int	sfrPage = -1;
	
	// SFR used as HI byte in MOVX A,Rx and MOVX @Rx,A
	private int sfrXdataHi = P0;
	
	// Cpu clock counter
	private long clock = 0;
	
	// Program counter
	public int pc = 0;
	
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
	

	private FastArray<MCS51Peripheral> peripherals	    = new FastArray<>();
	private FastArray<Runnable>	   runQueue	    = new FastArray<>();
	private FastArray<InterruptSource> interruptRequest = new FastArray<>();

	/**
	 * Emulation listener
	 *
	 * @since 1.04
	 */
	private FastArray<EmulationListener> emulationListeners = new FastArray<>();
	
	// Reset listener
	private FastArray<ResetListener> resetListeners = new FastArray<>();
	
	// Polling list
	private FastArray<MachineCyclesListener> machineListeners = new FastArray<>();
	
	// Vector with performance client
	private FastArray<MCS51Performance> performance = new FastArray<>();

	// Current serving interrupt
	private InterruptSource currentInterrupt = null;

	// Vector with the asyncronous timer
	private FastArray<AsyncTimer> asyncTimers = new FastArray<>();

	protected Code code;

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
		setSfrName(ACC,	 "ACC");
		setSfrName(B,	 "B"  );
		setSfrName(PSW,	 "PSW");
		setSfrName(SP,	 "SP" );
		setSfrName(DPL,	 "DPL");
		setSfrName(DPH,	 "DPH");
		setSfrName(P0,	 "P0" );
		setSfrName(P0M1, "P0M1");
		setSfrName(P0M2, "P0M2");
		setSfrName(P1,	 "P1"  );
		setSfrName(P1M1, "P1M1");
		setSfrName(P1M2, "P1M2");
		setSfrName(P2,	 "P2"  );
		setSfrName(P2M1, "P2M1");
		setSfrName(P2M2, "P2M2");
		setSfrName(P3,	 "P3"  );
		setSfrName(P3M1, "P3M1");
		setSfrName(P3M2, "P3M2");
		setSfrName(SCON, "SCON");
		setSfrName(SBUF, "SBUF");
		setSfrName(TCON, "TCON");
		setSfrName(TMOD, "TMOD");
		setSfrName(TH0,  "TH0");
		setSfrName(TL0,  "TL0");
		setSfrName(TH1,  "TH1");
		setSfrName(TL1,  "TL1");
		setSfrName(IE,   "IE" );

		setBitName(SCON + 0, "RI");
		setBitName(SCON + 1, "TI");
		setBitName(SCON + 2, "RB8");
		setBitName(SCON + 3, "TB8");
		setBitName(SCON + 4, "REN");
		setBitName(SCON + 5, "SM2");
		setBitName(SCON + 6, "SM1");
		setBitName(SCON + 7, "SM0");

		setBitName(TCON + 7, "TF1");
		setBitName(TCON + 6, "TR1");
		setBitName(TCON + 5, "TF0");
		setBitName(TCON + 4, "TR0");

		setBitName(IE + 7, "EA" );
		setBitName(IE + 6, "EC" );
		setBitName(IE + 5, "ET2");
		setBitName(IE + 4, "ES" );
		setBitName(IE + 3, "ET1");
		setBitName(IE + 2, "EX1");
		setBitName(IE + 1, "ET0");
		setBitName(IE + 0, "EX0");
		

		setBitName(PSW + 7, "CY");
		setBitName(PSW + 6, "AC");
		setBitName(PSW + 5, "F0");
		setBitName(PSW + 4, "RS1");
		setBitName(PSW + 3, "RS0");
		setBitName(PSW + 2, "OV");
		setBitName(PSW + 1, "F1");
		setBitName(PSW + 0, "P" );
		
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
		arithmetic(new ArithmeticADD(), 0x20, "ADD");
		arithmetic(new ArithmeticADDC(),0x30, "ADDC");
		arithmetic(new ArithmeticANL(), 0x50, "ANL");
		arithmetic(new ArithmeticORL(), 0x40, "ORL");
		arithmetic(new ArithmeticSUBB(),0x90, "SUBB");
		arithmetic(new ArithmeticXRL(), 0x60, "XRL");

		
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

	static private void arithmetic(ArithmeticOperation op, int basecode, String name)
	{
		// XXX A,Ri
		for (int i = 0 ; i < 8 ; i++){
			setOpcode(new Arithmetic(basecode|0x08|i, 1, op, name)
			{
                                @Override
				public int getValue(iCPU cpu, int pc)
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
			public int getValue(iCPU cpu, int pc)
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
                                @Override
				public int getValue(iCPU cpu, int pc)
				{
					return cpu.idata(cpu.r((int)(opcode & 1)));
				}

                                @Override
				public String toString()
				{
					return description + "\tA,R" + (opcode & 1);
				}
			});
		}

		// XXX A,#data
		setOpcode(new Arithmetic(basecode|4, 2, op, name)
		{
                        @Override
			public int getValue(iCPU cpu, int pc)
			{
				return cpu.code(pc + 1);
			}

                        @Override
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

        @Override
	public final int getDirectCODE(int pc)
	{
		return getDirect(code(pc));
	}

        @Override
	public String getDirectName(int add)
	{
		if (add >= 128){
			return getSfrName(add);
                } else {
			return getIdataName(add);
                }

	}

        @Override
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
	
        @Override
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
	
        @Override
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

		return getDirectName(add) + "^" + bit;
	}
	
        @Override
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

	
        @Override
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
	
        @Override
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
        @Override
	public void r(int r,int value)
	{
		idata(regPtr+r,value);
	}

	
        @Override
	public int r(int r)
	{
		return idata(regPtr + r);
	}

        @Override
	public final int b()
	{
		return sfr(B);
	}

        @Override
	public final void b(int value)
	{
		sfr(B,value);
	}


        @Override
	public final int acc()
	{
		return sfr(ACC);
	}

	/**
	 * Return the accumulator
	 */
	public final void acc(int value)
	{
		sfr(ACC, value);
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
		sfr(PSW, value);
	}
	
        @Override
	public final void cy(boolean value)
	{
		if (value)
			pswSet(PSW_CY);
		else
			pswReset(PSW_CY);	
	}

        @Override
	public boolean cy()
	{
		return ((psw() & PSW_CY) != 0);
	}

        @Override
	public final void ac(boolean value)
	{
		if (value)
			pswSet(PSW_AC);
		else
			pswReset(PSW_AC);
	}

        @Override
	public final boolean ac()
	{
		return ((psw() & PSW_AC) != 0);
	}

        @Override
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

	private void pswSet(int value)
	{
		sfr(PSW, sfr(PSW) | value);
	}

	private void pswReset(int value)
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
			setIdataName(i, Hex.bin2byte(i));
		}
	}

	private void setIdataName(int reg, String name)
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
		return code.getCode(addr, move);
	}

        @Override
	public final int code(int addr)
	{
		return code(addr, false);
	}

	public final int code16(int addr,boolean move)
	{
		return code.getCode16(addr,move);
	}
				
        @Override
	public final int code16(int addr)
	{
		return code16(addr,false);
	}
	
        @Override
	public int xdata(int add)
	{
		int value = xdata[add & 0xffff] & 0xff;

		return value;
	}
	
        @Override
	public void xdata(int add,int value)
	{
		xdata[add] = (byte)value;
	}

        @Override
	public final int idata(int add)
	{
		return idata[add] & 0xff;
	}
	
        @Override
	public final void idata(int add, int value)
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


	public final boolean sfrIsSet(int add, int mask)
	{
		return ((sfr(add) & mask) == mask);
	}
	
	public final void sfrSet(int add, int mask)
	{
		sfr(add, sfr(add)|mask);
	}

	
	public final void sfrReset(int add, int mask)
	{
		sfr(add, sfr(add) & ~mask);
	}

	public final int sfr16(int hi, int low)
	{
		return sfr(hi) * 256 + sfr(low);
	}

	public final void sfr16(int value, int hi, int low)
	{
		sfr(hi, value >> 8);
		sfr(low, value);
	}
	
	public final void sfr(int add, int value)
	{
		sfrCurrent.write(add, value);
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
			xdata(i, 0);
                }

		// Clear idata area
		for (int i = 0 ; i < idata.length ; i++){
			idata(i, 0);
                }

		// Set default SFR
		sfrCurrent.setWriteListener(false);
				
		for (int i = 0 ; i < 256 ; i++){
			sfr(i, 0);
                }
		sfrCurrent.setWriteListener(true);


		// Set stack pointer
		sfr(SP, 7);
		
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
	
        @Override
	public void push(int value) throws Exception
	{
		int sp = sfr(SP);
		if (++sp > 255){
			throw new Exception("Stack overflow at "+Hex.bin2word(pc));
                }
		idata(sp,value);
		sfr(SP,sp);
	}

        @Override
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
					performance.get(i).cpuPerformance(perc,elapsed);
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
	private void checkRunQueue()
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
		for (i = machineListeners.size(); --i >= 0;){
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
