package jx.disass; 

import java.util.ArrayList; 
import java.io.PrintStream;
import java.sql.Ref;

import jx.compiler.backend.NCExceptionHandler;
import jx.compiler.backend.Opr;
import jx.compiler.backend.Reg;
import jx.compiler.symbols.*;

/** 
 *  Parallel to this class there is a class 
 *  nativeCode.Binarycode. 
 *  In this version of the compiler, the second class 
 *  is used as a mere container, while this class 
 *  is used to assemble the binary code. 
 */ 
public interface Visitor {
    final boolean doAlignJumpTargets = false;

    int ip = 0;

    // mapping from instruction addresses to bytecode
    ArrayList instructionTable = new ArrayList();
    
    int bcIndex = 0, startIP = 0;

    // native code array reallocation 
    static final int INITSIZE  = 100; 
    static final int CHUNKSIZE = 200;

    /** 
	After compiling a method, symbolTable contains _all_ 
	unresolved constants of the code. 
	These include
	- jump offsets of jumps inside the code
	- invocations of functions 
	- constant pool entries that should be stored to 
        allow the storing of compiled code between JVM invocations 
	- actually all subclasses of nativecode.SymbolTableEntryBase
    */ 
    ArrayList symbolTable = new ArrayList(); 
  
    /** 
	contains the native exception handlers
    */ 
    final ArrayList exceptionHandlers = new ArrayList();

    /** 
	The methods in the frontend expect the compiled code
	stored inside of a object of class nativecode.BinaryCode. 
	Convert a object of preproc.BinaryCodePreproc into a object of 
	nativecode.BinaryCode.
	Note: Exceptionhandlers are not copied. 
    */ 
    /*
    public jx.jit.nativecode.BinaryCode getOldBinaryCode() {

	Enumeration enum = symbolTable.elements(); 
	Vector unresolvedEntries = new Vector(); 
	while(enum.hasMoreElements()) {
	    SymbolTableEntryBase entry = (SymbolTableEntryBase)enum.nextElement();
	    if (entry instanceof IntValueSTEntry) {
		((IntValueSTEntry)entry).applyValue(code);
		//entry.apply(code, codeBase);
	    } else {
		unresolvedEntries.addElement(entry); 
	    }
	}
	symbolTable = unresolvedEntries; 

      return new jx.jit.nativecode.BinaryCode(code, ip, symbolTable); 
    }
    */

    public int getCurrentIP();

    public void realloc();

    /** 
	Realloc memory in the byte code array. 
	After calling this method, there are at least 
	'requiredSpace' free bytes in the array. 
     * @param requiredSpace
     */
    public void realloc(int requiredSpace);
    
    // ***** Code Generation ***** 
    
    /** 
	Insert a single byte
    */ 
    void insertByte(int value);

    void insertByte(SymbolTableEntryBase entry);

    /**
     * Insert ModRM and SIB byte 
     */
    void insertModRM(int reg, Opr rm);
	
    void insertModRM(Reg reg, Opr rm);

    /**
     * Insert call near indirect (reg/mem) (2 clks)
     * @param opr
     */
    public void call(Opr opr);

    /**
     * Insert call near (Symbol) (1 clks)
     * @param entry
     */
    public void call(SymbolTableEntryBase entry);

    /**
     * Convert byte to word (3 clks) + (.. clks)
     */
    public void cbw();

    /**
     * Convert double to quad word (2 clks)
     * fill edx with sign bit of eax
     */
    public void cdq();

    /**
     * Convert word to double word (3 clks)
     * fill dx with sign bit of ax
     */
    public void cwde();

    /**
     * Convert word to double (2 clks)
     * fill dx with sign bit of ax
     */
    public void cwd();

    /**
     * Insert return
     */
    public void ret();

    /**
     * clear interrupt flag (7 clks)
     */
    public void cli();

    /**
     * decrement byte value by 1 (1/3 clks)
     * @param opr
     */
    public void decb(Opr opr);
    
    /**
     * decrement long value by 1 (1/3 clks)
     * @param ref
     */
    public void decl(Ref ref);

    /** 
     * decrement register by 1 (1 clks)
     * @param reg
     */
    public void decl(Reg reg);

    /**
     * Insert a pushl(reg)
     * @param reg
     */
    public void pushl(Reg reg);

    public void pushl(Ref ref);

    public void pushl(int immd);

    public void pushl(SymbolTableEntryBase entry);
	
    public void pushfl();

    /**
     * push all general registers
     * (eax,ecx,edx,ebx,esp,ebp,esi,edi) 
     * (5 clks)
     */
    public void pushal();

    /** 
     * Insert a popl(reg)
     * @param reg
     */
    public void popl(Reg reg);

    /**
     * pop stack into eflags register (4 clks)
     */
    public void popfl();

    /**
     * pop all general register
     */
    public void popal();

    /** 
     * lock prefix
     */
    public void lock();

    /**
     * rep prefix
     */
    public void repz();


    /** 
     * spinlocks
     * @param lock
     */
    public void spin_lock(Ref lock);

    public void spin_unlock(Ref lock);

    /**
     * Integer Subtraction
     * @param src
     * @param des
     */
    public void subl(Opr src, Reg des);

    public void subl(Reg src, Ref des);

    public void subl(int immd, Opr des);

    public void subl(SymbolTableEntryBase entry, Opr des);

    /**
     * Integer Subtraction with Borrow
     * @param src
     * @param des
     */
    public void sbbl(Opr src, Reg des);

    public void sbbl(Reg src, Ref des);
    
    /**
     * Integer Unsigned Multiplication of eax  (10 clk)
     * @param src
     */
    public void mull(Opr src);

    /**
     * Integer Signed Multiplication (10 clk)
     * @param src
     * @param des
     */
    public void imull(Opr src, Reg des);

    /* imull(Reg src, Ref des) no x86-code */

    public void imull(int immd, Reg des);

    public void imull(int immd, Opr src, Reg des);

    public void imull(SymbolTableEntryBase entry, Reg des);

    /**
     * increment by 1 (1/3 clks)
     * @param opr
     */
    public void incb(Opr opr);

    /** 
     * increment by 1 (1/3 clks)
     * @param ref
     */
    public void incl(Ref ref);

    /**
     * increment register by 1 (1 clks)
     * @param reg
     */
    public void incl(Reg reg);


    /** 
	lea Load Effective Address (1 clk)
     *  m = index * [0,1,2,4,8] + base + disp
     *  base.disp(disp,index,[0,1,2,4,8])
     * @param opr        
     * @param reg        
     */
    public void lea(Opr opr, Reg reg);

    /**
     * SHL/SAL Shift left (1/3 clks)
     * @param immd
     * @param des
     */
    public void shll(int immd, Opr des);

    /**
     * SHL/SAL Shift left by %cl (4 clks)
     * @param des
     */
    public void shll(Opr des);

    /**
     * SHLD Double Precision Shift left (4 clks)
     * @param immd
     * @param low
     * @param des
     */
    public void shld(int immd, Reg low, Opr des);

    /**
     * SHLD Double Precision Shift left by %cl (4/5 clks)
     * @param low
     * @param des
     */
    public void shld(Reg low, Opr des);

    /**
     * SHR Shift right (1/3 clks)
     * @param immd
     * @param des
     */

    public void shrl(int immd, Opr des);

    public void shrl(SymbolTableEntryBase entry, Opr des);

    /**
     * SHL/SAL Shift left by %cl (4 clks)
     * @param des
     */
    public void shrl(Opr des);

    /**
     * SAR Shift right (signed) (1/3 clks)
     * @param immd
     * @param des
     */
    public void sarl(int immd, Opr des);

    /**
     * SAR Shift right by %cl (signed) (4 clks)
     * @param des
     */
    public void sarl(Opr des);

    /**
     * DIV Signed Divide
     * @param src
     */
    public void idivl(Opr src);

    /**
     * DIV Unsigned Divide
     * @param src
     */
    public void divl(Opr src);

    /**
     * Add
     * @param src
     * @param des
     */
    public void addl(Opr src, Reg des);

    public void addl(Reg src, Ref des);
    
    public void addl(int immd, Opr des);

    public void addl(SymbolTableEntryBase entry, Opr des);
 
    /**
     * And (1/3 clks)
     */
    public void andl(Opr src, Reg des);

    public void andl(Reg src, Ref des);
    
    public void andl(int immd, Opr des);

    public void andl(SymbolTableEntryBase entry, Opr des);

    /**
     * Or (1/3 clks)
     */
    public void orl(Opr src, Reg des);

    public void orl(Reg src, Ref des);
    
    public void orl(int immd, Opr des);

    public void orl(SymbolTableEntryBase entry, Opr des);
    
    /**
     * Or (1/3 clks)
     */
    public void xorl(Opr src, Reg des);

    public void xorl(Reg src, Ref des);
    
    public void xorl(int immd, Opr des);

    public void xorl(SymbolTableEntryBase entry, Opr des);

    /**
     * Not (1/3 clks)
     */
    public void notl(Opr opr);

    /**
     * Neg (1/3 clks)
     */
    public void negl(Opr opr);

    /**
     * Add with Carry
     */
    public void adcl(Opr src, Reg des);

    public void adcl(Reg src, Ref des);

    /**
     * Compare Two Operands
     */
    public void cmpb(int immd, Opr des);

    public void cmpl(Opr src, Reg des);

    public void cmpl(Reg src, Ref des);

    public void cmpl(int immd, Opr des);
    
    public void cmpl(SymbolTableEntryBase entry, Opr des);

    /**
     * @param des
     */
    public void sete(Opr des);

    public void setne(Opr des);

    public void intr(int nr);


    /**
     * Jump short/near if equal
     */
    public void je(int rel);

    public void je(SymbolTableEntryBase entry);

    /**
     * Jump short/near if not equal
     */
    public void jne(int rel);

    public void jne(SymbolTableEntryBase entry);

    public void jnae(SymbolTableEntryBase entry);

    /**
     * Jump short/near if less
     */
    public void jl(SymbolTableEntryBase entry);
    
    /**
     * Jump short/near if greater or equal
     */
    public void jge(SymbolTableEntryBase entry);
    
    /**
     * Jump short/near if greater
     */
    public void jg(SymbolTableEntryBase entry);

    /**
     * Jump short/near if less or equal
     */
    public void jle(SymbolTableEntryBase entry);

    /**
     * Jump short/near if unsigned greater
     */
    public void ja(SymbolTableEntryBase entry);

    /**
     * Jump short/near if unsigned greater or equal
     */
    public void jae(SymbolTableEntryBase entry);

    /**
     * Jump short/near if sign
     */
    public void js(int rel);
  
    /**
     * Jump short/near 
     */
    public void jmp(int rel);

    public void jmp(Opr des);

    public void jmp(SymbolTableEntryBase entry);

    public void jmp(Reg index,SymbolTableEntryBase[] tables);

    /**
     * Move 8 Bit Data
     */
    public void movb(Opr src, Reg des);

    public void movb(Reg src, Ref des);

    public void movb(int immd, Opr des);
	    
    /** 
	Move 16 Bit Data
     */
    public void movw(Opr src, Reg des);

    public void movw(Reg src, Ref des);

    /**
     * Move 32 Bit Data
     */
    public void movl(Opr src, Reg des);

    public void movl(Reg src, Ref des);
    public void movl(int immd, Opr des);

    public void movl(SymbolTableEntryBase entry, Opr des);

    /**
     * Move with Zero-Extend (short) (3 clks)
     */
    public void movzwl(Opr src, Reg des);
    
    /**
     * move with Zero-Extend (byte) (3 clks)
     */
    public void movzbl(Opr src, Reg des);

    /**
       Move with Sign-Extend (short to register) (3 clks)
     */
    public void movswl(Opr src, Reg des);

    /**
       Move with Sign-Extend (byte to register) (3 clks)
     */
    public void movsbl(Opr src, Reg des);

    /**
       No Operation (1 clks)
     */
    public void nop();

    /**
       write to model specific register (30-45 clks)

       ecx  | register
       =============================
       0x00 | machine check address
       0x01 | machine check type
       =============================
       0x10 | time stamp counter
       0x11 | control and event select
       0x12 | counter 0
       0x13 | counter 1

     */
    
    public void wrmsr();

    /**
       read from model specific register (20-24 clks)

       see wrmsr() for register selection
     */

    public void rdmsr();
	

    /**
       Read from Time Stamp Counter 
       return EDX:EAX
     */

    public void rdtsc();

    /** 
       read performance monitor counter
       (only P6)
       
       ecx = 0 : return EDX:EAX counter0
       ecx = 1 : return EDX:EAX counter1
     */

    public void rdpmc();

    /**
       test - logical compare (1/2 clks)
     */

    public void test(Opr src, Reg des);

    public void test(int immd, Opr des);

    /** 
	Insert a single byte constant 
     */ 
    public void insertConst1(int value);
  
    /** 
	Insert a four byte constant 
     */ 
    public void insertConst4(int value);

    void insertConst4At(int ncIndex, int value);

    /** 
	Insert a four byte constant with an unknown value. 
	(must be resolved before the code is installed) 
     */ 
    public void insertConst4(SymbolTableEntryBase entry);
    
    // (immd>>8)==0
    public boolean is8BitValue(int value);

    /** 
	Insert a 0 byte constant with an unknown value. 
	(contains information about current code position, i.e., a stack map) 
     */ 
    public void insertConst0(SymbolTableEntryBase entry);


    /**
       Intel Architecture Optimization. Reference Manual (chapter 2,page 11)
       "Pentium II and III processors have a cache line size of 32 byte.
       Since the instruction prefetch buffers fetch 16-byte boundaries,
       code alignment has a direct impact on prefetch buffer efficiency"
       
       * Loop entry labels should be 16-byte-aligned when less then 
       eight byte away from a 16-byte boundary.
       
       * Labels that follow an unconditional branch of function call
       should be aligend as above.
       
       * Labels that follow a conditional branch need _not_ be aligned.
     */
    
    public void alignCode();
    
    /** 
	Initialized the target position of 'jumpObject'. 
	(Call insertConst4() for corresponding jump instruction) 
     */
    public void addJumpTarget(UnresolvedJump jumpObject);

    public void alignIP();

    public void alignIP_4_Byte();

    public void alignIP_16_Byte();

    public void alignIP_32_Byte();

    public void addExceptionTarget(UnresolvedJump handler);

    /** 
	Make a symbol table entry relative. 
	If you use insertConst4(), this class assumes that 
	the value to be inserted is absolute. But if the 
	inserted value is a jump offset it is relative to 
	the instruction pointer of the next instruction. 
	That is what you can tell the compiler with this 
	method. 
     */ 
    public void makeRelative(SymbolTableEntryBase entry);
    
    /**
       Called after each instruction. 
     */ 
    public void endInstr();

    // ***** Management stuff ***** 
    
    public void finishCode();
    
    /** 
	Apply all resolveable symbol table entries.
	(e.g. insert jump offsets ....)
	After calling this method, the vector 'symbolTable' 
	contains all symbol table entries that are not resolveable.
	If you want to install the compiled code after calling this 
	method, this vector should be empty. 
     * @param codeBase
     */ 
    public void resolve(int codeBase);

    // ***** Building of Debug messages ****** 
    
    static final String[] REGNAME = {
	"ax", "cx", "dx", "bx", "sp", "bp", "si", "di"
    };
    
    public String regToString(int reg);
    
    // ***** Exceptions *****
    
    public void addExceptionRangeStart(NCExceptionHandler handler);
    public void addExceptionRangeEnd(NCExceptionHandler handler);
    
    /**
	add a start of an exception handler.
     * @param handler
     */
    public void addExceptionHandler(NCExceptionHandler handler);
    
    /**
	return an array of all exception handlers of this 
	method. (these handlers contain the native code indices 
	of the range start, range end and of the handler start 
     * @return
     */
    public NCExceptionHandler[] getExceptionHandlers();

    // ***** Printing ***** 
    
    public String getBinaryCodeAsHex(int firstByte, int stopByte);
    
    // returns a hexdump of the compiled function 
    public String getBinaryCodeAsHex();
    
    String getBinaryCodeAsAssembler(int firstByte, int stopByte);
    
    // returns a hexdump of the compiled function 
    public String getBinaryCodeAsAssembler();
    
    public void printInstr(String instr, String arg1, SymbolTableEntryBase arg2);
    public void printInstr(String instr, SymbolTableEntryBase arg1, String arg2);
    public void printInstr(String instr, String arg1, SymbolTableEntryBase arg2, String arg3);
    public void printInstr(String instr);
    public void printInstr(String instr, String arg1, String arg2);
    public void printInstr(String instr, SymbolTableEntryBase arg1);
    public void printJumpTarget(UnresolvedJump entry);
    public void printHexByte(int value);  
    public void printHexInt(int value);
    public void printInstructions();
    public void printGASInstructions(PrintStream out);
    public void startBC(int bcPosition);
    public void endBC();
    public ArrayList getInstructionTable();
}
