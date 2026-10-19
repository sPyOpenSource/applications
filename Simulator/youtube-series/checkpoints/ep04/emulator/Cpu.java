package emulator;

public class Cpu {
    public enum Reg { EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP }
    public final int[] gpr = new int[8];
    public int eip, eflags;
    public final byte[] mem = new byte[65536];

    public int reg(Reg r){ return gpr[r.ordinal()]; }
    public void setReg(Reg r, int v){ gpr[r.ordinal()] = v; }
    public int readByte(int addr){ return mem[addr] & 0xFF; }
    public void writeByte(int addr, int value){ mem[addr]=(byte)(value & 0xFF); }
    public int readDword(int addr){ return readByte(addr)|(readByte(addr+1)<<8)|(readByte(addr+2)<<16)|(readByte(addr+3)<<24); }

    public void reset(){ eip=0; eflags=0; for(int i=0;i<gpr.length;i++) gpr[i]=0; setReg(Reg.ESP,0x10000); }

    private int fetch(){ int b=readByte(eip); eip++; return b; }

    private void updateFlags(int result){
        if (result == 0) eflags |= 0x40;
        else eflags &= ~0x40;
    }

    public void run(){
        boolean halted=false; int count=0;
        while(!halted){
            int op=fetch();
            switch(op){
                case 0xB8: case 0xB9: case 0xBA: case 0xBB:
                case 0xBC: case 0xBD: case 0xBE: case 0xBF: {
                    int regNum = op - 0xB8;
                    int imm = readDword(eip); eip += 4;
                    gpr[regNum] = imm; break;
                }
                case 0x05: {
                    int imm = readDword(eip); eip += 4;
                    gpr[0] += imm; updateFlags(gpr[0]); break;
                }
                case 0x01: {
                    int modrm = fetch();
                    int mod=(modrm>>6)&3, src=(modrm>>3)&7, dst=modrm&7;
                    if (mod==3){ gpr[dst]+=gpr[src]; updateFlags(gpr[dst]); }
                    else { System.out.println("mem operands unsupported"); halted=true; }
                    break;
                }
                case 0xF4: halted=true; break;
                default:
                    System.out.println("Unknown opcode: 0x"+Integer.toHexString(op));
                    halted=true;
            }
            count++;
        }
        System.out.println("Executed "+count+" instruction(s).");
    }
}
