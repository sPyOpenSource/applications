
package jCPU;

import j51.swing.JInfo;
import jCPU.MCS51.MCS51Performance;

/**
 *
 * @author X. Wang
 */
public interface iCPU {
    default int code(int i) { return 0; }

    default String getCodeName(int i) { return ""; }

    default String getBitName(int code) { return ""; }

    default String getDirectName(int r) { return ""; }

    default int acc() { return 0; }

    default int r(int i) { return 0; }

    default int code16(int i) { return 0; }

    default CallListener getCallListener(int address) { return null; }

    default void pushw(int i) throws Exception {}

    default void pc(int address) {}

    default void acc(int i) {}

    default void idata(int add, int acc) {}

    default int getDirectCODE(int i) { return 0; }

    default int getDirect(int add) { return 0; }

    default void setDirect(int add, int i) {}

    default boolean getBit(int code) { return false; }

    default boolean cy() { return false; }

    default void cy(boolean b) {}

    default int idata(int r) { return 0; }

    default void setBit(int code, boolean b) {}

    default boolean ac() { return false; }

    default void r(int r, int tmp) {}

    default int b() { return 0; }

    default void b(int i) {}

    default void ov(boolean b) {}

    default int dptr() { return 0; }

    default void dptr(int i) {}

    default boolean getBitCODE(int i) { return false; }

    default int popw() throws Exception { return 0; }

    default void eoi() {}

    default void xdata(int offset, int acc) {}

    default int pop() throws Exception { return 0; }

    default int xdata(int dptr) { return 0; }

    default void push(int directCODE) throws Exception {}

    default void ac(boolean op) {}

    default int sfr(int add) { return 0; }

    default int getSfrXdataHi() { return 0; }

    default void addPerformanceListener(MCS51Performance p) {}

    default int getCodeSize() { return 0; }

    default void code(int i, int i0) {}

    default void reset() {}

    default void setEmulation(boolean mode) {}

    default void setCodeName(int address, String label) {}

    default int getInterruptCount() { return 0; }

    default InterruptStatistic getInterruptAt(int i) { return null; }

    default long getExecutionCounter(int i) { return 0; }

    public String getDecodeAt(int i);

    default long getOpcodeCounter(int i) { return 0; }

    default String getOpcodeDescription(int i) { return ""; }

    default void stopSimulation() {}

    default void pass() throws Exception {}

    public int step() throws Exception;

    public void go(int i) throws Exception;

    default int getLengthAt(int pc) { return 1; }

    public void setBreakPoint(int pc, boolean b);

    public int pc();

    public void setOscillator(int value);

    public void machineCycle(int i);

    public long clock();

    public int getOscillator();

    public int machineCycle();

    public void sp(int value);

    public void dpl(int value);

    public void dph(int value);

    public int sp();

    public int dpl();

    public int dph();

    public int psw();

    public void psw(int value);

    public int getXdataSize();

    default String getSfrName(int i) { return ""; }

    default void addResetListener(ResetListener aThis) {}

    default void sfr(int P0M1, int i) {}
}
