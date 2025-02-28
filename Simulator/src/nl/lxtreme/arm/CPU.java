/*
 * Java ARM9-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm;

import java.io.File;
import java.io.IOException;
import java.util.*;

import nl.lxtreme.arm.memory.*;
import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.ProgramHeader;

/**
 * Provides a simplistic version of an ARM-core.
 */
public class CPU extends j51.intel.MCS51
{
  // VARIABLES
  private final int[] gpr;
  private final Cpsr cpsr;
  private int spsr;

  private boolean finished;
  private boolean running = false;
  private int entryPoint; // initial PC value

  private final List<Integer> breakpoints;

  // CONSTRUCTORS

  /**
   * Creates a new Arm instance.
   * 
   * @param memory
   *          the memory to use in the processor.
   */
  public CPU(Memory memory)
  {
    this.gpr = new int[16];
    this.cpsr = new Cpsr();

    this.code = memory;
    this.breakpoints = new ArrayList<>(32);
    this.entryPoint = 0;
  }
  
  public CPU(){
    this.gpr = new int[16];
    this.cpsr = new Cpsr();
    /*Memory memory = new Memory();
    //this.code = new Memory();
    File elfFile = new File("/Users/xuyi/Source/OS/armOS/lib/jcore/test/Simulator/test/resources/helloWorld_static");
    Elf elf;
    try {
        elf = new Elf(elfFile);
        for (ProgramHeader ph : elf.getProgramHeaders()){
            int size = (int) ph.getMemorySize();
            if (size <= 0){
                continue;
            }

            Chunk chunk = memory.create(ph.getVirtualAddress(), size);
            elf.readSegment(ph, chunk);
        }
    } catch (IOException ex) {
        //Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, ex);
    }
this.code = memory;*/
    this.breakpoints = new ArrayList<>(32);
    this.entryPoint = 0;
  }

  // METHODS

  /**
   * Arithmetic Shift Right (ASR) moves each bit of a bitstring (x) right by a
   * specified number of bits (y). Copies of the leftmost bit are shifted in at
   * the left end of the bitstring. Bits that are shifted off the right end of
   * the bitstring are discarded, except that the last such bit can be produced
   * as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int ASR(int x, int y)
  {
    return (x >> y);
  }

  /**
   * Logical Shift Left (LSL) moves each bit of a bitstring (x) left by a
   * specified number of bits (y). Zeros are shifted in at the right end of the
   * bitstring. Bits that are shifted off the left end of the bitstring are
   * discarded, except that the last such bit can be produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int LSL(int x, int y)
  {
    return (x << y);
  }

  /**
   * Logical Shift Right (LSR) moves each bit of a bitstring (x) right by a
   * specified number of bits (y). Zeros are shifted in at the left end of the
   * bitstring. Bits that are shifted off the right end of the bitstring are
   * discarded, except that the last such bit can be produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int LSR(int x, int y)
  {
    return (x >>> y);
  }

  /**
   * Rotate Right (ROR) moves each bit of a bitstring (x) right by a specified
   * number of bits (y). Each bit that is shifted off the right end of the
   * bitstring is re-introduced at the left end. The last bit shifted off the
   * right end of the bitstring can be produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param y
   *          the number of bits to shift.
   * @return the bitshifted bitstring.
   */
  static int ROR(int x, int y)
  {
    return ((x >>> y) | (x << (32 - y)));
  }

  /**
   * Rotate Right with Extend (RRX) moves each bit of a bitstring (x) right by
   * one bit. The carry input (c) is shifted in at the left end of the
   * bitstring. The bit shifted off the right end of the bitstring can be
   * produced as a carry output.
   * 
   * @param x
   *          the bitstring to shift;
   * @param c
   *          the carry bit.
   * @return the bitshifted bitstring.
   */
  static int RRX(int x, int c)
  {
    return ((x >>> 1) | ((c & 0x01) << 31));
  }
  
  @Override
    public void setBreakPoint(int pc, boolean mode)
    {
	if (mode){
            breakAdd(pc);
        } else {
            breakDel(pc);
        }
    }

  /**
   * Add breakpoint
   * 
   * @param address
   */
  public void breakAdd(int address)
  {
    if (!breakFind(address))
    {
      this.breakpoints.add(address);
    }
  }

  /**
   * @param address
   */
  public void breakDel(int address)
  {
    Iterator<Integer> iter = this.breakpoints.iterator();
    while (iter.hasNext())
    {
      Integer addr = iter.next();
      if ((addr != null) && (addr == address))
      {
        iter.remove();
        break;
      }
    }
  }

  /**
   * @param address
   * @return
   */
  public boolean breakFind(int address)
  {
      for (Integer addr : this.breakpoints) {
          if ((addr != null) && (addr == address))
          {
              return true;
          }
      }
    return false;
  }

  /**
   * 
   */
  public void dumpRegs()
  {
    System.out.println("REGISTERS DUMP:");
    System.out.println("===============");

    /* Print GPRs */
    for (int i = 0; i < 16; i += 2)
    {
      System.out.printf("r%-2d: 0x%08X\t\tr%-2d: 0x%08X\n", i, this.gpr[i], i + 1, this.gpr[i + 1]);
    }

    System.out.println();

    /* Print CPSR */
    System.out.printf("cpsr: 0x%x\n", this.cpsr.getValue());
    System.out.println(" (z: " + this.cpsr.z + ", n: " + this.cpsr.n + ", c: " + this.cpsr.c + ", v: " + this.cpsr.v
         + ", I: " + this.cpsr.I + ", F: " + this.cpsr.F + ", t: " + this.cpsr.t + ", mode: " + this.cpsr.mode + ")");

    /* Print SPSR */
    System.out.printf("spsr: 0x%x\n", this.spsr);
  }

  /**
   * @param count
   */
  public void dumpStack(int count)
  {
    System.out.println("STACK DUMP:");
    System.out.println("===========");

    /* Print stack */
    for (int i = 0; i < count; i++)
    {
      int addr = this.gpr[13] + (i << 2);
      int value;

      /* Read stack */
      value = this.code.read32(addr);

      /* Print value */
      System.out.printf("[%02d] 0x%08X\n", i, value);
    }
  }

  /**
   * @param idx
   * @return
   */
  public int peekReg(int idx)
  {
    return this.gpr[idx];
  }

  /**
   * @param idx
   * @param val
   */
  public void pokeReg(int idx, int val)
  {
    this.gpr[idx] = val;
  }

  /**
   * Resets this CPU.
   */
  @Override
  public void reset()
  {
    Arrays.fill(this.gpr, 0);
    this.gpr[15] = this.entryPoint;
    this.cpsr.setValue(this.spsr = 0);
    this.finished = false;
  }

  /**
   * @param val
   */
  public void setPC(int val)
  {
    this.gpr[15] = this.entryPoint = val;
  }
  
    @Override
    public void go(int limit) throws Exception{
        while(true){
            step();
        }
    }
    
  /**
   * Steps through the instructions.
   * 
   * @return <code>true</code> if a new instruction is available,
   *         <code>false</code> otherwise.
   */
  @Override
  public int step()
  {
    boolean ret;

    /* Check finish flag */
    if (this.finished){
      System.out.printf("FINISHED! (return: %d)", this.gpr[0]);
      return 0;
    }

    /* Remove thumb bit */
    int pc = this.gpr[15] & ~1;

    /* Check breakpoint */
    ret = breakFind(pc);
    if (ret){
      System.out.printf("BREAKPOINT! (0x%x)\n", pc);
      return 0;
    }

    /* Parse instruction */
    if (this.cpsr.t){
      parseThumb(pc);
    } else {
      System.out.println(getDecodeAt(pc));
    }

    return 1;
  }

  @Override
  public int getLengthAt(int pc){
      if(this.cpsr.t) return 2;
      else return 4;
  }
  
  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected int addition(int a, int b)
  {
    /* Add values */
    int result = a + b;

    /* Set flags */
    this.cpsr.c = carryFrom(a, b);
    this.cpsr.v = overflowFrom(a, b);
    this.cpsr.z = result == 0;
    this.cpsr.n = ((result >> 31) != 0);

    return result;
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected boolean borrowFrom(int a, int b)
  {
    return (a < b); // TODO suspect!
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected boolean carryFrom(int a, int b)
  {
    return ((a + b) < a); // TODO suspect!
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   * @return
   */
  protected boolean condCheck(int opcode)
  {
    int condCheck = (opcode >> 28) & 0x0f;
    try{
    return conditionCheck(ConditionCode.values()[condCheck]);
    } catch (Exception e) {
        return false;
    }
  }

  /**
   * 16-bit opcode.
   * 
   * @param opcode
   * @return
   */
  protected boolean condCheck(short opcode)
  {
    int condCheck = (opcode >> 8) & 0x0f;
    return conditionCheck(ConditionCode.values()[condCheck]);
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   */
  protected void condPrint(int opcode)
  {
    int condCheck = (opcode >> 28) & 0x0f;
    //System.out.print(ConditionCode.values()[condCheck]);
  }

  /**
   * 16-bit opcode.
   * 
   * @param opcode
   */
  protected void condPrint(short opcode)
  {
    int condCheck = (opcode >> 8) & 0x0f;
    System.out.print(ConditionCode.values()[condCheck]);
  }

  /**
   * 
   */
  protected void forceThumbMode()
  {
    this.cpsr.t = true;
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected boolean overflowFrom(int a, int b)
  {
    int s = a + b;

    return ((a & (1 << 31)) == (b & (1 << 31))) &&
            ((s & (1 << 31)) != (a & (1 << 31)));
  }

    /**
     * Parses the next ARM (32-bit) instruction.
     * @param pc
     * @return 
     */
  @Override
  public String getDecodeAt(int pc)
  {
      if(this.cpsr.t)
        return parseThumb(pc);
    System.out.printf("%08X [A] ", pc);

    /* Read opcode */
    int opcode = this.code.read32(pc);
String ins = String.format("     %08X ", opcode);
    //System.out.printf("(%08x) ", opcode);

    /* Update PC */
    this.gpr[15] += 4; // 32-bit

    /* Registers */
    int Rm  = ((opcode)       & 0xF);
    int Rs  = ((opcode >>  8) & 0xF);
    int Rd  = ((opcode >> 12) & 0xF);
    int Rn  = ((opcode >> 16) & 0xF);
    int Imm = ((opcode)       & 0xFF);
    int amt = Rs << 1;

    /* Flags */
    boolean I = ((opcode >> 25) & 1) != 0;
    boolean P = ((opcode >> 24) & 1) != 0;
    boolean U = ((opcode >> 23) & 1) != 0;
    boolean B = ((opcode >> 22) & 1) != 0;
    boolean W = ((opcode >> 21) & 1) != 0;
    boolean S = ((opcode >> 20) & 1) != 0;
    boolean L = ((opcode >> 20) & 1) != 0;

    if (((opcode >> 8) & 0x0FFFFF) == 0x012FFF)
    {
      boolean link = ((opcode >> 5) & 1) != 0;
ins += String.format("b%sx", (link) ? "l" : "");
      //System.out.printf("b%sx", (link) ? "l" : "");
      condPrint(opcode);
ins += String.format(" r%d", Rm);
//      System.out.printf(" r%d\n", Rm);

      if (!condCheck(opcode))
      {
        return ins;
      }

      if (link)
      {
        this.gpr[14] = this.gpr[15];
      }

      this.cpsr.t = (this.gpr[Rm] & 1) == 1;

      this.gpr[15] = this.gpr[Rm] & ~1;

      return ins;
    }

    if ((opcode >>> 24) == 0xEF)
    {
      int ImmA = (opcode & 0xFFFFFF);
ins += String.format("swi 0x%X", ImmA);
//      System.out.printf("swi 0x%X\n", ImmA);
      parseSvc(ImmA & 0xFF);

      return ins;
    }

    if ((((opcode >> 22) & 0x3F) == 0) &&
        (((opcode >>  4) & 0x0F) == 9))
    {
ins += String.format("%s", W ? "mla" : "mul");
//      System.out.printf("%s", W ? "mla" : "mul");
      condPrint(opcode);
      suffPrint(opcode);
ins += String.format(" r%d, r%d, r%d", Rn, Rm, Rs);
//      System.out.printf(" r%d, r%d, r%d", Rn, Rm, Rs);
      if (W)
      {
ins += String.format(", r%d", Rd);
//        System.out.printf(", r%d", Rd);
      }
//      System.out.printf("\n");

      if (!condCheck(opcode))
      {
        return ins;
      }

      if (W)
      {
        this.gpr[Rn] = (this.gpr[Rm] * this.gpr[Rs] + this.gpr[Rd]) & 0xFFFFFFFF;
      } else {
        this.gpr[Rn] = (this.gpr[Rm] * this.gpr[Rs]) & 0xFFFFFFFF;
      }

      if (S)
      {
        this.cpsr.z = this.gpr[Rn] == 0;
        this.cpsr.n = (this.gpr[Rn] >> 31) != 0;
      }

      return ins;
    }

    switch ((opcode >> 26) & 0x3)
    {
      case 0:
      {
        switch ((opcode >> 21) & 0xF)
        {
          case 0:
          { // AND
ins += "and";
            //System.out.printf("and");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
              //System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
              //System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] & ROR(Imm, amt);
            } else {
              this.gpr[Rd] = this.gpr[Rn] & shift(opcode, this.gpr[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 1:
          { // EOR
ins += "eor";
            //System.out.printf("eor");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
              //System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
              //System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] ^ ROR(Imm, amt);
            } else {
              this.gpr[Rd] = this.gpr[Rn] ^ shift(opcode, this.gpr[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 2:
          { // SUB
ins += "sub";
            //System.out.printf("sub");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] - ROR(Imm, amt);
            } else {
              this.gpr[Rd] = this.gpr[Rn] - shift(opcode, this.gpr[Rm]);
            }

            if (S)
            {
              this.cpsr.c = (I) ? (this.gpr[Rn] >= ROR(Imm, amt)) : (this.gpr[Rn] < this.gpr[Rd]);
              this.cpsr.v = (I) ? ((this.gpr[Rn] >> 31) & ~(this.gpr[Rd] >> 31)) != 0
                  : ((this.gpr[Rn] >> 31) & ~(this.gpr[Rd] >> 31)) != 0;
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 3:
          { // RSB
              ins += "rsb";
//            System.out.printf("rsb");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = ROR(Imm, amt) - this.gpr[Rn];
            } else {
              this.gpr[Rd] = shift(opcode, this.gpr[Rm]) - this.gpr[Rn];
            }

            if (S)
            {
              this.cpsr.c = (I) ? (this.gpr[Rn] > Imm) : (this.gpr[Rn] > this.gpr[Rm]);
              this.cpsr.v = (I) ? ((Imm >> 31) & ~((Imm - this.gpr[Rn]) >> 31)) != 0
                  : ((Imm >> 31) & ~((this.gpr[Rm] - this.gpr[Rn]) >> 31)) != 0;
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 4:
          { // ADD
              ins += "add";
//            System.out.printf("add");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] + ROR(Imm, amt);
            } else {
              this.gpr[Rd] = this.gpr[Rn] + shift(opcode, this.gpr[Rm]);
            }

            if (Rn == 15)
            {
              this.gpr[Rd] += 4;
            }

            if (S)
            {
              this.cpsr.c = this.gpr[Rd] < this.gpr[Rn];
              this.cpsr.v = ((this.gpr[Rn] >> 31) & ~(this.gpr[Rd] >> 31)) != 0;
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 5:
          { // ADC
              ins += "adc";
//            System.out.printf("adc");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] + ROR(Imm, amt) + (this.cpsr.c ? 1 : 0);
            } else {
              this.gpr[Rd] = this.gpr[Rn] + shift(opcode, this.gpr[Rm]) + (this.cpsr.c ? 1 : 0);
            }

            if (S)
            {
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 6:
          { // SBC
              ins += "sbc";
//            System.out.printf("sbc");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] - ROR(Imm, amt) - (this.cpsr.c ? 0 : 1);
            }  else {
              this.gpr[Rd] = this.gpr[Rn] - shift(opcode, this.gpr[Rm]) - (this.cpsr.c ? 0 : 1);
            }

            if (S)
            {
              this.cpsr.c = this.gpr[Rd] > this.gpr[Rn];
              this.cpsr.v = ((this.gpr[Rn] >> 31) & ~(this.gpr[Rd] >> 31)) != 0;
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 7:
          { // RSC
              ins += "rsc";
//            System.out.printf("rsc");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = ROR(Imm, amt) - this.gpr[Rn] - (this.cpsr.c ? 0 : 1);
            } else {
              this.gpr[Rd] = shift(opcode, this.gpr[Rm]) - this.gpr[Rn] - (this.cpsr.c ? 0 : 1);
            }

            if (S)
            {
              this.cpsr.c = (I) ? (this.gpr[Rd] > Imm) : (this.gpr[Rd] > this.gpr[Rm]);
              this.cpsr.v = (I) ? ((this.gpr[Rm] >> 31) & ~(this.gpr[Rd] >> 31)) != 0
                  : ((this.gpr[Rn] >> 31) & ~(this.gpr[Rd] >> 31)) != 0;
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 8:
          { // TST/MRS
            if (S)
            {
              int result;
ins += "tst";
//              System.out.printf("tst");
              condPrint(opcode);

              if (!I)
              {
                  ins += String.format(" r%d, r%d", Rn, Rm);
//                System.out.printf(" r%d, r%d\n", Rn, Rm);
                shiftPrint(opcode);

                result = this.gpr[Rn] & shift(opcode, this.gpr[Rm]);
              } else {
                  ins += String.format(" r%d, #0x%X", Rn, ROR(Imm, amt));
//                System.out.printf(" r%d, #0x%X\n", Rn, ROR(Imm, amt));
                result = this.gpr[Rn] & ROR(Imm, amt);
              }

              this.cpsr.z = result == 0;
              this.cpsr.n = (result >> 31) != 0;
            } else {
                ins += String.format("mrs r%d, cpsr", Rd);
//              System.out.printf("mrs r%d, cpsr\n", Rd);
              this.gpr[Rd] = this.cpsr.getValue();
            }

            return ins;
          }

          case 9:
          { // TEQ/MSR
            if (S)
            {
              int result;
ins += "teq";
//              System.out.printf("teq");
              condPrint(opcode);

              if (!I)
              {
                  ins += String.format(" r%d, r%d", Rn, Rm);
//                System.out.printf(" r%d, r%d\n", Rn, Rm);
                shiftPrint(opcode);

                result = this.gpr[Rn] ^ shift(opcode, this.gpr[Rm]);
              } else {
                  ins += String.format(" r%d, #0x%X", Rn, ROR(Imm, amt));
//                System.out.printf(" r%d, #0x%X\n", Rn, ROR(Imm, amt));
                result = this.gpr[Rn] ^ ROR(Imm, amt);
              }

              this.cpsr.z = result == 0;
              this.cpsr.n = (result >> 31) != 0;
            } else {
              if (I)
              {
                  ins += String.format("msr cpsr, r%d", Rm);
//                System.out.printf("msr cpsr, r%d\n", Rm);
                this.cpsr.setValue(this.gpr[Rm]);
              } else {
                  ins += String.format("msr cpsr, 0x%08X", Imm);
//                System.out.printf("msr cpsr, 0x%08X\n", Imm);
                this.cpsr.setValue(Imm);
              }
            }

            return ins;
          }

          case 10:
          { // CMP/MRS2
            if (S)
            {
              int value;
ins += "cmp";
//              System.out.printf("cmp");
              condPrint(opcode);

              if (I)
              {
                value = ROR(Imm, amt);
                ins += String.format(" r%d, 0x%08X", Rn, value);
//                System.out.printf(" r%d, 0x%08X\n", Rn, value);
              } else {
                value = this.gpr[Rm];
                ins += String.format(" r%d, r%d", Rn, Rm);
//                System.out.printf(" r%d, r%d\n", Rn, Rm);
              }

              if (condCheck(opcode))
              {
                subtract(this.gpr[Rn], value);
              }
            } else {
                ins += "mrs2";
//              System.out.printf("mrs2\n");
            }

            return ins;
          }

          case 11:
          { // CMN/MSR2
            if (S)
            {
              int value;
ins += "cmn";
//              System.out.printf("cmn");
              condPrint(opcode);

              if (I)
              {
                value = ROR(Imm, amt);
                ins += String.format(" r%d, 0x%08X", Rn, value);
//                System.out.printf(" r%d, 0x%08X\n", Rn, value);
              } else {
                value = this.gpr[Rm];
                ins += String.format(" r%d, r%d", Rn, Rm);
//                System.out.printf(" r%d, r%d\n", Rn, Rm);
              }

              if (condCheck(opcode))
              {
                addition(this.gpr[Rn], value);
              }
            } else {
                ins += "msr2";
//              System.out.printf("msr2\n");
            }

            return ins;
          }

          case 12:
          { // ORR
              ins += "orr";
//            System.out.printf("orr");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] | ROR(Imm, amt);
            } else {
              this.gpr[Rd] = this.gpr[Rn] | shift(opcode, this.gpr[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 13:
          { // MOV
              ins += "mov";
//            System.out.printf("mov");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d", Rn, Rm);
//              System.out.printf(" r%d, r%d", Rd, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, #0x%X", Rd, ROR(Imm, amt));
              //System.out.printf(" r%d, #0x%X", Rd, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = ROR(Imm, amt);
            } else {
              this.gpr[Rd] = (Rm == 15) ? (this.gpr[15] + 4 /* 32-bit */) : shift(opcode, this.gpr[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 14:
          { // BIC
              ins += "bic";
//            System.out.printf("bic");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d, r%d", Rd, Rn, Rm);
//              System.out.printf(" r%d, r%d, r%d", Rd, Rn, Rm);
              shiftPrint(opcode);
            }
            else
            {
                ins += String.format(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
//              System.out.printf(" r%d, r%d, #0x%X", Rd, Rn, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = this.gpr[Rn] & ~(ROR(Imm, amt));
            } else {
              this.gpr[Rd] = this.gpr[Rd] & ~shift(opcode, this.gpr[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }

          case 15:
          { // MVN
              ins += "mvn";
//            System.out.printf("mvn");
            condPrint(opcode);
            suffPrint(opcode);

            if (!I)
            {
                ins += String.format(" r%d, r%d", Rn, Rm);
//              System.out.printf(" r%d, r%d", Rd, Rm);
              shiftPrint(opcode);
            } else {
                ins += String.format(" r%d, #0x%X", Rd, ROR(Imm, amt));
//              System.out.printf(" r%d, #0x%X", Rd, ROR(Imm, amt));
            }

            //System.out.printf("\n");

            if (!condCheck(opcode))
            {
              return ins;
            }

            if (I)
            {
              this.gpr[Rd] = ~ROR(Imm, amt);
            } else {
              this.gpr[Rd] = ~shift(opcode, this.gpr[Rm]);
            }

            if (S)
            {
              this.cpsr.z = this.gpr[Rd] == 0;
              this.cpsr.n = (this.gpr[Rd] >> 31) != 0;
            }

            return ins;
          }
        }
      }

      case 1:
      { // LDR/STR
        int addr, value, wb;
ins += String.format("%s%s", (L) ? "ldr" : "str", (B) ? "b" : "");
//        System.out.printf("%s%s", (L) ? "ldr" : "str", (B) ? "b" : "");
        condPrint(opcode);
        ins += String.format(" r%d", Rd);
//        System.out.printf(" r%d,", Rd);

        Imm = opcode & 0xFFF;

        if (L && (Rn == 15))
        {
          addr = this.gpr[15] + Imm + 4;
          value = this.code.read32(addr);

          if (condCheck(opcode))
          {
            this.gpr[Rd] = value;
          }
ins += String.format(" =0x%X", value);
//          System.out.printf(" =0x%X\n", value);
          return ins;
        }
ins += String.format(" [r%d", Rn);
//        System.out.printf(" [r%d", Rn);

        if (I)
        {
          value = shift(opcode, this.gpr[Rm]);
ins += String.format(", %sr%d", (U) ? "" : "-", Rm);
//          System.out.printf(", %sr%d", (U) ? "" : "-", Rm);
          shiftPrint(opcode);
        } else {
          value = Imm;
          ins += String.format(", #%s0x%X", (U) ? "" : "-", value);
//          System.out.printf(", #%s0x%X", (U) ? "" : "-", value);
        }
        ins += String.format("]%s", (W) ? "!" : "");
//        System.out.printf("]%s\n", (W) ? "!" : "");

        if (!condCheck(opcode))
        {
          return ins;
        }

        if (U)
        {
          wb = this.gpr[Rn] + value;
        } else {
          wb = this.gpr[Rn] - value;
        }

        addr = (P) ? wb : this.gpr[Rn];

        if (L)
        {
          if (B)
          {
              if(running)
                this.gpr[Rd] = code(addr);
          } else {
              if(running)
                this.gpr[Rd] = this.code.read32(addr);
          }
        } else {
          value = this.gpr[Rd];
          if (Rd == 15)
          {
            value += 8;
          }

          if (B)
          {
              if(running)
                code(addr, (byte) (value & 0xFF));
          } else {
              if(running)
                this.code.write32(addr, value);
          }
        }

        if (W || !P)
        {
          this.gpr[Rn] = wb;
        }
        return ins;
      }

      default:
        break;
    }

    switch ((opcode >> 25) & 7)
    {
      case 4:
      { // LDM/STM
        int start = this.gpr[Rn];
        boolean pf = false;

        if (L)
        {
            ins += "ldm";
//          System.out.printf("ldm");
          if (Rn == 13)
          {
              ins += String.format("%s%s", (P) ? "e" : "f", (U) ? "d" : "a");
//            System.out.printf("%c%c", (P) ? 'e' : 'f', (U) ? 'd' : 'a');
          } else {
              ins += String.format("%s%s", (U) ? "i" : "d", (P) ? "b" : "a");
//            System.out.printf("%c%c", (U) ? 'i' : 'd', (P) ? 'b' : 'a');
          }
        } else {
            ins += "stm";
//          System.out.printf("stm");
          if (Rn == 13)
          {
              ins += String.format("%s%s", (P) ? "f" : "e", (U) ? "a" : "d");
//            System.out.printf("%c%c", (P) ? 'f' : 'e', (U) ? 'a' : 'd');
          } else {
              ins += String.format("%s%s", (U) ? "i" : "d", (P) ? "b" : "a");
//            System.out.printf("%c%c", (U) ? 'i' : 'd', (P) ? 'b' : 'a');
          }
        }

        if (Rn == 13)
        {
            ins += " sp";
          //System.out.printf(" sp");
        } else {
            ins += String.format(" r%d", Rn);
//          System.out.printf(" r%d", Rn);
        }

        if (W)
        {
            ins += "!";
//          System.out.printf("!");
        }
        ins += ", {";
//        System.out.printf(", {");

        for (int i = 0; i < 16; i++)
        {
          if (((opcode >> i) & 1) != 0)
          {
            if (pf)
            {
                ins += ", ";
//              System.out.printf(", ");
            }
            ins += String.format("r%d", i);
//            System.out.printf("r%d", i);

            pf = true;
          }
        }
ins += "}";
//        System.out.printf("}");
        if (B)
        {
            ins += "^";
//          System.out.printf("^");
          if ((opcode & (1 << 15)) != 0)
          {
            this.cpsr.setValue(this.spsr);
          }
        }
        //System.out.printf("\n");

        if (L)
        {
          for (int i = 0; i < 16; i++)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (P)
              {
                start += U ? 4 : -4; // 32-bit
              }
              if (running)
                this.gpr[i] = this.code.read32(start);
              if (!P)
              {
                start += U ? 4 : -4; // 32-bit
              }
            }
          }
        } else {
          for (int i = 15; i >= 0; i--)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (P)
              {
                start += U ? 4 : -4; // 32-bit
               }
              if(running)
                this.code.write32(start, this.gpr[i]);
              if (!P)
              {
                start += U ? 4 : -4; // 32-bit
              }
            }
          }
        }

        if (W)
        {
          this.gpr[Rn] = start;
        }
        return ins;
      }

      case 5:
      { // B/BL
        boolean link = (opcode & (1 << 24)) != 0;
ins += String.format("b%s", (link) ? "l" : "");
//        System.out.printf("b%s", (link) ? "l" : "");
        condPrint(opcode);

        Imm = (opcode & 0xFFFFFF) << 2;
        if ((Imm & (1 << 25)) != 0)
        {
          Imm = ~(~Imm & 0xFFFFFF);
        }
        Imm += 4; // 32-bit
ins += String.format(" 0x%08X", this.gpr[15] + Imm);
//        System.out.printf(" 0x%08X\n", this.gpr[15] + Imm);

        if (!condCheck(opcode))
        {
          return ins;
        }

        if (link)
        {
          this.gpr[14] = this.gpr[15];
        }
        this.gpr[15] += Imm;

        return ins;
      }

      case 7:
      { // MRC
ins += "mrc ...";
        //System.out.printf("mrc ...\n");
        return ins;
      }
    }
ins += String.format("Unknown opcode! (0x%08X", opcode);
    //System.out.printf("Unknown opcode! (0x%08X)\n", opcode);
return ins;
  }

  /**
   * 8-bit value
   * 
   * @param num
   */
  protected void parseSvc(int num)
  {
    /* Parse syscall */
    switch (num)
    {
      case 0:
      { // exit
        /* Set finish flag */
        this.finished = true;
        break;
      }

      case 4:
      { // write
        int fd = this.gpr[0];
        int addr = this.gpr[1];
        int len = this.gpr[2];

        /* No output descriptor */
        if ((fd < 1) || (fd > 2))
        {
          break;
        }

        /* Print string */
        for (int i = 0; i < len; i++)
        {
          System.out.printf("0x%x", code(addr + i));
        }

        /* Return value */
        this.gpr[0] = len;

        break;
      }

      default:
        System.out.printf("         [S] Unhandled syscall! (%02X)\n", num);
    }
  }

  /**
   * Parses a THUMB (16-bit) instruction.
   */
  protected String parseThumb(int pc)
  {
    //System.out.printf("%08X [T] ", this.gpr[15]);

    /* Read opcode */
    int opcode = this.code.read16(pc) & 0xFFFF;
    
String ins = String.format("     %04x ", opcode);
    System.out.printf("(%04x) ", opcode);

    /* Update PC */
    this.gpr[15] += 2; // 16-bit

    if ((opcode >> 13) == 0)
    {
      int Imm = (opcode >> 6) & 0x1F;
      int Rn = (opcode >> 6) & 7;
      int Rm = (opcode >> 3) & 7;
      int Rd = (opcode) & 7;

      switch ((opcode >> 11) & 3)
      {
        case 0:
        { // LSL
          if ((Imm > 0) && (Imm <= 32))
          {
            this.cpsr.c = (this.gpr[Rd] & (1 << (32 - Imm))) != 0;
            this.gpr[Rd] = LSL(this.gpr[Rd], Imm);
          }

          if (Imm > 32)
          {
            this.cpsr.c = false;
            this.gpr[Rd] = 0;
          }

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("lsl r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
          return ins;
        }

        case 1:
        { // LSR
          if ((Imm > 0) && (Imm <= 32))
          {
            this.cpsr.c = (this.gpr[Rd] & (1 << (Imm - 1))) != 0;
            this.gpr[Rd] = LSR(this.gpr[Rd], Imm);
          }

          if (Imm > 32)
          {
            this.cpsr.c = false;
            this.gpr[Rd] = 0;
          }

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("lsr r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
          return ins;
        }

        case 2:
        { // ASR
          if ((Imm > 0) && (Imm <= 32))
          {
            this.cpsr.c = (this.gpr[Rd] & (1 << (Imm - 1))) != 0;
            this.gpr[Rd] = ASR(this.gpr[Rd], Imm);
          }

          if (Imm > 32)
          {
            this.cpsr.c = false;
            this.gpr[Rd] = 0;
          }

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("asr r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
          return ins;
        }

        case 3:
        { // ADD, SUB
          if ((opcode & 0x400) != 0)
          {
            Imm &= 7;

            if ((opcode & 0x200) != 0)
            {
              this.gpr[Rd] = subtract(this.gpr[Rm], Imm);

              ins += String.format("sub r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
              return ins;
            } else {
              this.gpr[Rd] = addition(this.gpr[Rm], Imm);

              ins += String.format("add r%d, r%d, #0x%02X\n", Rd, Rm, Imm);
            }
          } else {
            if ((opcode & 0x200) != 0)
            {
              this.gpr[Rd] = subtract(this.gpr[Rm], this.gpr[Rn]);

              ins += String.format("sub r%d, r%d, r%d\n", Rd, Rm, Rn);
              return ins;
            } else {
              this.gpr[Rd] = addition(this.gpr[Rm], this.gpr[Rn]);

              ins += String.format("add r%d, r%d, r%d\n", Rd, Rm, Rn);
              return ins;
            }
          }

          return ins;
        }
      }
    }

    if ((opcode >> 13) == 1)
    {
      int Imm = (opcode & 0xFF);
      int Rn = (opcode >> 8) & 7;

      switch ((opcode >> 11) & 3)
      {
        case 0:
        { // MOV
          this.gpr[Rn] = Imm;

          this.cpsr.z = this.gpr[Rn] == 0;
          this.cpsr.n = (this.gpr[Rn] >> 31) != 0;

          ins += String.format("mov r%d, #0x%02X\n", Rn, Imm);
          return ins;
        }

        case 1:
        { // CMP
          subtract(this.gpr[Rn], Imm);

          ins += String.format("cmp r%d, #0x%02X\n", Rn, Imm);
          return ins;
        }

        case 2:
        { // ADD
          this.gpr[Rn] = addition(this.gpr[Rn], Imm);

          ins += String.format("add r%d, #0x%02X\n", Rn, Imm);
          return ins;
        }

        case 3:
        { // SUB
          this.gpr[Rn] = subtract(this.gpr[Rn], Imm);

          ins += String.format("sub r%d, #0x%02X\n", Rn, Imm);
          return ins;
        }
      }
    }

    if ((opcode >> 10) == 0x10)
    {
      int Rd = opcode & 7;
      int Rm = (opcode >> 3) & 7;

      switch ((opcode >> 6) & 0xF)
      {
        case 0:
        { // AND
          this.gpr[Rd] &= this.gpr[Rm];

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("and r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 1:
        { // EOR
          this.gpr[Rd] ^= this.gpr[Rm];

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("eor r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 2:
        { // LSL
          int shift = this.gpr[Rm] & 0xFF;

          if ((shift > 0) && (shift <= 32))
          {
            this.cpsr.c = (this.gpr[Rd] & (1 << (32 - shift))) != 0;
            this.gpr[Rd] = LSL(this.gpr[Rd], shift);
          }

          if (shift > 32)
          {
            this.cpsr.c = false;
            this.gpr[Rd] = 0;
          }

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("lsl r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 3:
        { // LSR
          int shift = this.gpr[Rm] & 0xFF;

          if ((shift > 0) && (shift <= 32))
          {
            this.cpsr.c = (this.gpr[Rd] & (1 << (shift - 1))) != 0;
            this.gpr[Rd] = LSR(this.gpr[Rd], shift);
          }

          if (shift > 32)
          {
            this.cpsr.c = false;
            this.gpr[Rd] = 0;
          }

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("lsr r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 4:
        { // ASR
          int shift = this.gpr[Rm] & 0xFF;

          if ((shift > 0) && (shift < 32))
          {
            this.cpsr.c = (this.gpr[Rd] & (1 << (shift - 1))) != 0;
            this.gpr[Rd] = ASR(this.gpr[Rd], shift);
          }

          if (shift == 32)
          {
            this.cpsr.c = (this.gpr[Rd] >> 31) != 0;
            this.gpr[Rd] = 0;
          }

          if (shift > 32)
          {
            this.cpsr.c = false;
            this.gpr[Rd] = 0;
          }

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("asr r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 5:
        { // ADC
          this.gpr[Rd] = addition(this.gpr[Rd], this.gpr[Rm]);
          this.gpr[Rd] = addition(this.gpr[Rd], this.cpsr.c ? 1 : 0);

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("adc r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 6:
        { // SBC
          this.gpr[Rd] = subtract(this.gpr[Rd], this.gpr[Rm]);
          this.gpr[Rd] = subtract(this.gpr[Rd], this.cpsr.c ? 0 : 1);

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("sbc r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 7:
        { // ROR
          int shift = this.gpr[Rm] & 0xFF;

          while (shift >= 32)
          {
            shift -= 32;
          }

          if (shift != 0)
          {
            this.cpsr.c = (this.gpr[Rd] & (1 << (shift - 1))) != 0;
            this.gpr[Rd] = ROR(this.gpr[Rd], shift);
          }

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("ror r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 8:
        { // TST
          int result = this.gpr[Rd] & this.gpr[Rm];

          this.cpsr.z = result == 0;
          this.cpsr.n = (result >> 31) != 0;

          ins += String.format("tst r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 9:
        { // NEG
          this.gpr[Rd] = -this.gpr[Rm];

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("neg r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 10:
        { // CMP
          subtract(this.gpr[Rd], this.gpr[Rm]);

          ins += String.format("cmp r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 11:
        { // CMN/MVN
          if ((opcode & 0x100) != 0)
          {
            this.gpr[Rd] = ~this.gpr[Rm];

            this.cpsr.z = this.gpr[Rd] == 0;
            this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

            ins += String.format("mvn r%d, r%d\n", Rd, Rm);
          }
          else
          {
            addition(this.gpr[Rd], this.gpr[Rm]);

            ins += String.format("cmn r%d, r%d\n", Rd, Rm);
          }

          return ins;
        }

        case 12:
        { // ORR
          this.gpr[Rd] |= this.gpr[Rm];

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("orr r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 13:
        { // MUL
          this.gpr[Rd] *= this.gpr[Rm];

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("mul r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 14:
        { // BIC
          this.gpr[Rd] &= ~this.gpr[Rm];

          this.cpsr.z = this.gpr[Rd] == 0;
          this.cpsr.n = (this.gpr[Rd] >> 31) != 0;

          ins += String.format("bic r%d, r%d\n", Rd, Rm);
          return ins;
        }
      }
    }

    if ((opcode >> 7) == 0x8F)
    {
      int Rm = (opcode >> 3) & 0xF;

      this.gpr[14] = this.gpr[15] | 1;

      this.cpsr.t = (this.gpr[Rm] & 1) != 0;

      this.gpr[15] = this.gpr[Rm] & ~1;

      ins += String.format("blx r%d\n", Rm);
      return ins;
    }

    if ((opcode >> 10) == 0x11)
    {
      int Rd = ((opcode >> 4) & 8) | (opcode & 7);
      int Rm = ((opcode >> 3) & 0xF);

      switch ((opcode >> 8) & 3)
      {
        case 0:
        { // ADD
          this.gpr[Rd] = addition(this.gpr[Rd], this.gpr[Rm]);

          ins += String.format("add r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 1:
        { // CMP
          subtract(this.gpr[Rd], this.gpr[Rm]);

          ins += String.format("cmp r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 2:
        { // MOV (NOP)
          if ((Rd == 8) && (Rm == 8))
          {
            ins += String.format("nop\n");
            return ins;
          }

          this.gpr[Rd] = this.gpr[Rm];

          ins += String.format("mov r%d, r%d\n", Rd, Rm);
          return ins;
        }

        case 3:
        { // BX
          this.cpsr.t = (this.gpr[Rm] & 1) != 0;

          if (Rm == 15)
          {
            this.gpr[15] += 2; // 16-bit
          }
          else
          {
            this.gpr[15] = this.gpr[Rm] & ~1;
          }

          ins += String.format("bx r%d\n", Rm);
          return ins;
        }
      }
    }

    if ((opcode >> 11) == 9)
    {
      int Rd = (opcode >> 8) & 7;
      int Imm = (opcode & 0xFF);
      int addr = this.gpr[15] + (Imm << 2) + 2; // 16-bit
if(running)
      this.gpr[Rd] = this.code.read32(addr);

      ins += String.format("ldr r%d, =0x%08X\n", Rd, this.gpr[Rd]);
      return ins;
    }

    if ((opcode >> 12) == 5)
    {
      int Rd = (opcode) & 7;
      int Rn = (opcode >> 3) & 7;
      int Rm = (opcode >> 6) & 7;

      switch ((opcode >> 9) & 7)
      {
        case 0:
        { // STR
          int addr = this.gpr[Rn] + this.gpr[Rm];
          int value = this.gpr[Rd];
if(running)
          this.code.write32(addr, value);

          ins += String.format("str r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return ins;
        }

        case 2:
        { // STRB
          int addr = this.gpr[Rn] + this.gpr[Rm];
          byte value = (byte) (this.gpr[Rd] & 0xFF);
if(running)
          code(addr, value);

          ins += String.format("strb r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return ins;
        }

        case 4:
        { // LDR
          int addr = this.gpr[Rn] + this.gpr[Rm];

          this.gpr[Rd] = this.code.read32(addr);

          ins += String.format("ldr r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return ins;
        }

        case 6:
        { // LDRB
          int addr = this.gpr[Rn] + this.gpr[Rm];
if(running)
          this.gpr[Rd] = code(addr);

          ins += String.format("ldrb r%d, [r%d, r%d]\n", Rd, Rn, Rm);
          return ins;
        }
      }
    }

    if ((opcode >> 13) == 3)
    {
      int Rd = (opcode) & 7;
      int Rn = (opcode >> 3) & 7;
      int Imm = (opcode >> 6) & 7;

      if ((opcode & 0x1000) != 0)
      {
        if ((opcode & 0x800) != 0)
        {
          int addr = this.gpr[Rn] + (Imm << 2);
if(running)
          this.gpr[Rd] = code(addr);

          ins += String.format("ldrb r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm);
        } else {
          int addr = this.gpr[Rn] + (Imm << 2);
          byte value = (byte) (this.gpr[Rd] & 0xFF);
if(running)
          code(addr, value);

          ins += String.format("strb r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm);
        }
      } else {
        if ((opcode & 0x800) != 0)
        {
          int addr = this.gpr[Rn] + (Imm << 2);
if(running)
          this.gpr[Rd] = this.code.read32(addr);

          ins += String.format("ldr r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 2);
        } else {
          int addr = this.gpr[Rn] + (Imm << 2);
          int value = this.gpr[Rd];
if(running)
          this.code.write32(addr, value);

          ins += String.format("str r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 2);
        }
      }

      return ins;
    }

    if ((opcode >> 12) == 8)
    {
      int Rd = (opcode) & 7;
      int Rn = (opcode >> 3) & 7;
      int Imm = (opcode >> 6) & 7;

      if ((opcode & 0x800) != 0)
      {
        int addr = this.gpr[Rn] + (Imm << 1);
if(running)
        this.gpr[Rd] = this.code.read16(addr);

        ins += String.format("ldrh r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 1);
      } else {
        int addr = this.gpr[Rn] + (Imm << 1);
        short value = (short) (this.gpr[Rd] & 0xFFFF);
if(running)
        this.code.write16(addr, value);

        ins += String.format("strh r%d, [r%d, 0x%02X]\n", Rd, Rn, Imm << 1);
      }

      return ins;
    }

    if ((opcode >> 12) == 9){
      int Rd = (opcode >> 8) & 7;
      int Imm = (opcode & 0xFF);

      if ((opcode & 0x800) != 0){
        int addr = this.gpr[13] + (Imm << 2);

        this.gpr[Rd] = this.code.read32(addr);

        ins += String.format("ldr r%d, [sp, 0x%02X]\n", Rd, Imm << 2);
      } else {
        int addr = this.gpr[13] + (Imm << 2);
        int value = this.gpr[Rd];

        this.code.write32(addr, value);

        ins += String.format("str r%d, [sp, 0x%02X]\n", Rd, Imm << 2);
      }

      return ins;
    }

    if ((opcode >> 12) == 10)
    {
      int Rd = (opcode >> 8) & 7;
      int Imm = (opcode & 0xFF);

      if ((opcode & 0x800) != 0)
      {
        this.gpr[Rd] = this.gpr[13] + (Imm << 2);

        ins += String.format("add r%d, sp, #0x%02X\n", Rd, Imm << 2);
      } else {
        this.gpr[Rd] = (this.gpr[15] & ~2) + (Imm << 2);

        ins += String.format("add r%d, pc, #0x%02X\n", Rd, Imm << 2);
      }

      return ins;
    }

    if ((opcode >> 12) == 11)
    {
      switch ((opcode >> 9) & 7)
      {
        case 0:
        { // ADD/SUB
          int Imm = (opcode & 0x7F);

          if ((opcode & 0x80) != 0)
          {
            this.gpr[13] -= Imm << 2;
            ins += String.format("sub sp, #0x%02X\n", Imm << 2);
          } else {
            this.gpr[13] += Imm << 2;
            ins += String.format("add sp, #0x%02X\n", Imm << 2);
          }

          return ins;
        }

        case 2:
        { // PUSH
          boolean lrf = (opcode & 0x100) != 0;
          boolean pf = false;

          if (lrf)
          {
            push(this.gpr[14]);
          }

          for (int i = 7; i >= 0; i--)
          {
            if (((opcode >> i) & 1) != 0)
            {
              push(this.gpr[i]);
            }
          }

          ins += String.format("push {");

          for (int i = 0; i < 8; i++)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (pf)
              {
                ins += String.format(",");
              }
              ins += String.format("r%d", i);

              pf = true;
            }
          }

          if (lrf)
          {
            if (pf)
            {
              ins += String.format(",");
            }
            ins += String.format("lr");
          }

          ins += String.format("}\n");
          return ins;
        }

        case 6:
        { // POP
          boolean pcf = (opcode & 0x100) != 0;
          boolean pf = false;

          ins += String.format("pop {");

          for (int i = 0; i < 8; i++)
          {
            if (((opcode >> i) & 1) != 0)
            {
              if (pf)
              {
                ins += String.format(",");
              }
              ins += String.format("r%d", i);

              this.gpr[i] = pop();
              pf = true;
            }
          }

          if (pcf)
          {
            if (pf)
            {
              ins += String.format(",");
            }
            ins += String.format("pc");

            this.gpr[15] = pop();
            this.cpsr.t = (this.gpr[15] & 1) != 0;
          }

          ins += String.format("}\n");
          return ins;
        }
      }
    }

    if ((opcode >> 12) == 12)
    {
      int Rn = (opcode >> 8) & 7;

      if ((opcode & 0x800) != 0)
      {
        ins += String.format("ldmia r%d!, {", Rn);

        for (int i = 0; i < 8; i++)
        {
          if (((opcode >> i) & 1) != 0)
          {
            this.gpr[i] = this.code.read32(this.gpr[Rn]);
            this.gpr[Rn] += 4;

            ins += String.format("r%d,", i);
          }
        }

        ins += String.format("}\n");
        return ins;
      } else {
        ins += String.format("stmia r%d!, {", Rn);

        for (int i = 0; i < 8; i++)
        {
          if (((opcode >> i) & 1) != 0)
          {
            this.code.write32(this.gpr[Rn], this.gpr[i]);
            this.gpr[Rn] += 4;

            ins += String.format("r%d,", i);
          }
        }

        ins += String.format("}\n");
        return ins;
      }
    }

    if ((opcode >> 12) == 13)
    {
      int Imm = (opcode & 0xFF) << 1;

      if ((Imm & 0x100) != 0)
      {
        Imm = ~((~Imm) & 0xFF);
      }

      Imm += 2;

      ins += String.format("b");
      condPrint(opcode);
      ins += String.format(" 0x%08X\n", (this.gpr[15] + Imm));

      if (condCheck(opcode))
      {
        this.gpr[15] += Imm;
      }

      return ins;
    }

    if ((opcode >> 11) == 28)
    {
      int Imm = (opcode & 0x7FF) << 1;

      if ((Imm & (1 << 11)) != 0)
      {
        Imm = (~Imm) & 0xFFE;
        this.gpr[15] -= Imm;
      } else {
        this.gpr[15] += Imm + 2; // 16-bit
      }

      ins += String.format("b 0x%08X, 0x%X\n", this.gpr[15], Imm);
      return ins;
    }

    if ((opcode >> 11) == 0x1E)
    {
      boolean blx = false;
      if ((opcode & (1 << 11)) == 0)
      {
        // H = 0
        int Imm = ((opcode & 0x7FF) << 12);

        this.gpr[14] = this.gpr[15] + Imm;
      } else {
        // H = 1
        int temp = this.gpr[15];

        int Imm = ((opcode & 0x7FF) << 1);

        this.gpr[15] = this.gpr[14] + Imm;
        this.gpr[14] = temp | 1;

        if ((Imm & (1 << 22)) != 0)
        {
          Imm = (~Imm) & 0x7FFFFE;
          this.gpr[15] -= Imm;
        } else {
          this.gpr[15] += Imm + 2;
        }
      }

      if (blx)
      {
        this.cpsr.t = false;
        ins += String.format("blx 0x%08X\n", this.gpr[15]);
      } else {
        ins += String.format("bl 0x%08X\n", this.gpr[15]);
      }

      return ins;
    }

    ins += String.format("Unknown opcode! (0x%04X)\n", opcode);
    return ins;
  }

  /**
   * @return 32-bit value.
   */
  @Override
  public int pop()
  {
    int addr = this.gpr[13];

    this.gpr[13] += 4; // 32-bit

    /* Read value */
    return this.code.read32(addr);
  }

  /**
   * 32-bit value.
   * 
   * @param value
   */
  @Override
  public void push(int value)
  {
    /* Update SP */
    this.gpr[13] -= 4; // 32-bit

    /* Write value */
    this.code.write32(this.gpr[13], value);
  }

  /**
   * 32-bit value/opcode.
   * 
   * @param opcode
   * @param value
   * @return
   */
  protected int shift(int opcode, int value)
  {
    boolean signed = ((opcode >> 20) & 1) == 1;

    int amt = (opcode >> 7) & 0x1F;
    int result;

    if (amt == 0)
    {
      return value;
    }

    switch ((opcode >> 5) & 3)
    {
      case 0:
        if (signed)
        {
          this.cpsr.c = (value & (1 << (32 - amt))) == 1;
        }

        result = LSL(value, amt);
        break;
      case 1:
        if (signed)
        {
          this.cpsr.c = (value & (1 << (amt - 1))) == 1;
        }

        result = LSR(value, amt);
        break;
      case 2:
        if (signed)
        {
          this.cpsr.c = (value & (1 << (amt - 1))) == 1;
        }

        result = ASR(value, amt);
        break;
      case 3:
        result = ROR(value, amt);
        break;

      default:
        result = value;
    }

    return result;
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   */
  protected void shiftPrint(int opcode)
  {
    int amt = (opcode >> 7) & 0x1F;
    if (amt == 0)
    {
      return;
    }

    switch ((opcode >> 5) & 3)
    {
      case 0:
        System.out.printf(",LSL#%d", amt);
        break;
      case 1:
        System.out.printf(",LSR#%d", amt);
        break;
      case 2:
        System.out.printf(",ASR#%d", amt);
        break;
      case 3:
        System.out.printf(",ROR#%d", amt);
        break;
    }
  }

  /**
   * 32-bit values.
   * 
   * @param a
   * @param b
   * @return
   */
  protected int subtract(int a, int b)
  {
    /* Subtract values */
    int result = a - b;

    /* Set flags */
    this.cpsr.c = !borrowFrom(a, b);
    this.cpsr.v = overflowFrom(a, -b);
    this.cpsr.z = result == 0;
    this.cpsr.n = ((result >> 31) == 1);

    return result;
  }

  /**
   * 32-bit opcode.
   * 
   * @param opcode
   */
  protected void suffPrint(int opcode)
  {
    if (((opcode >> 20) & 1) != 0)
    {
      System.out.print("s");
    }
  }

  /**
   * @param aOpcode
   * @return
   */
  private boolean conditionCheck(ConditionCode aCode)
  {
    /* Check condition */
    switch (aCode)
    {
      case EQ:
        return this.cpsr.z;
      case NE:
        return !this.cpsr.z;
      case CS:
        return this.cpsr.c;
      case CC:
        return !this.cpsr.c;
      case MI:
        return this.cpsr.n;
      case PL:
        return !this.cpsr.n;
      case VS:
        return this.cpsr.v;
      case VC:
        return !this.cpsr.v;
      case HI:
        return (this.cpsr.c && !this.cpsr.z);
      case LS:
        return (!this.cpsr.c || this.cpsr.z);
      case GE:
        return (this.cpsr.n == this.cpsr.v);
      case LT:
        return (this.cpsr.n != this.cpsr.v);
      case GT:
        return ((this.cpsr.n == this.cpsr.v) && !this.cpsr.z);
      case LE:
        return ((this.cpsr.n != this.cpsr.v) || this.cpsr.z);
      case AL:
        return true;
    }

    return false;
  }
}
