
package jCPU;

import j51.intel.CallListener;

/**
 *
 * @author X. Wang
 */
public interface CPU {
    int code(int i);

    public String getCodeName(int i);

    public String getBitName(int code);

    public String getDirectName(int r);

    public int acc();

    public int r(int i);

    public int code16(int i);

    public CallListener getCallListener(int address);

    public void pushw(int i)  throws Exception;

    public void pc(int address);

    public void acc(int i);

    public void idata(int add, int acc);

    public int getDirectCODE(int i);

    public int getDirect(int add);

    public void setDirect(int add, int i);

    public boolean getBit(int code);

    public boolean cy();

    public void cy(boolean b);

    public int idata(int r);

    public void setBit(int code, boolean b);

    public boolean ac();

    public void r(int r, int tmp);

    public int b();

    public void b(int i);

    public void ov(boolean b);

    public int dptr();

    public void dptr(int i);

    public boolean getBitCODE(int i);

    public int popw() throws Exception;

    public void eoi();

    public void xdata(int offset, int acc);

    public int pop() throws Exception;

    public int xdata(int dptr);

    public void push(int directCODE) throws Exception;

    public void ac(boolean op);
    
    public int sfr(int add);
    
    public int getSfrXdataHi();
}
