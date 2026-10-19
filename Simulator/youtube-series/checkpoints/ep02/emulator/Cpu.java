package emulator;

public class Cpu {
    public enum Reg { EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP }
    public final int[] gpr = new int[8];
    public int eip;
    public int eflags;
    public final byte[] mem = new byte[65536];

    public int reg(Reg r){ return gpr[r.ordinal()]; }
    public void setReg(Reg r, int v){ gpr[r.ordinal()] = v; }

    public int readByte(int addr){ return mem[addr] & 0xFF; }
    public void writeByte(int addr, int value){ mem[addr]=(byte)(value & 0xFF); }
    public int readWord(int addr){ return readByte(addr)|(readByte(addr+1)<<8); }
    public void writeWord(int addr,int value){ writeByte(addr,value); writeByte(addr+1,value>>8); }

    public void reset(){
        eip=0; eflags=0;
        for(int i=0;i<gpr.length;i++) gpr[i]=0;
        setReg(Reg.ESP,0x10000);
    }
    public void run(){ /* episode 3 */ }
}
