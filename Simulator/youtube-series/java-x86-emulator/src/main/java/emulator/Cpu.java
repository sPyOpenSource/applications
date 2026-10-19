package emulator;

public class Cpu {
    public enum Reg { EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP }
    public final int[] gpr = new int[8];
    public int eip, eflags;
    public final byte[] mem = new byte[65536];

    public int irqPending = -1;
    public int timerCounter = 0;
    public int timerReload = 5;

    public int reg(Reg r){ return gpr[r.ordinal()]; }
    public void setReg(Reg r, int v){ gpr[r.ordinal()] = v; }

    public int readByte(int addr){ return mem[addr] & 0xFF; }
    public void writeByte(int addr, int value){ mem[addr]=(byte)(value & 0xFF); }
    public int readWord(int addr){ return readByte(addr)|(readByte(addr+1)<<8); }
    public void writeWord(int addr,int value){ writeByte(addr,value); writeByte(addr+1,value>>8); }
    public int readDword(int addr){ return readByte(addr)|(readByte(addr+1)<<8)|(readByte(addr+2)<<16)|(readByte(addr+3)<<24); }
    public void writeDword(int addr,int value){ writeByte(addr,value); writeByte(addr+1,value>>8); writeByte(addr+2,value>>16); writeByte(addr+3,value>>24); }

    public void reset(){
        eip=0; eflags=0;
        for(int i=0;i<gpr.length;i++) gpr[i]=0;
        setReg(Reg.ESP,0x10000);
        irqPending=-1; timerCounter=timerReload;
    }

    private int fetch(){ int b=readByte(eip); eip++; return b; }

    private void updateFlags(int result){
        if (result == 0) eflags |= 0x40;
        else eflags &= ~0x40;
    }

    private void push32(int v){ setReg(Reg.ESP, reg(Reg.ESP)-4); writeDword(reg(Reg.ESP), v); }
    private int pop32(){ int v=readDword(reg(Reg.ESP)); setReg(Reg.ESP, reg(Reg.ESP)+4); return v; }

    private void dispatchIrq(){
        int vec = irqPending;
        irqPending = -1;
        push32(eflags);
        push32(eip);
        eip = readDword(vec * 4);
    }

    public void run(){
        boolean halted=false; int count=0;
        while(true){
            if (halted) {
                if (timerReload == 0) break;
                while (irqPending < 0) {
                    timerCounter--;
                    if (timerCounter <= 0){ irqPending = 0; timerCounter = timerReload; }
                }
                dispatchIrq();
                halted = false;
                continue;
            }
            timerCounter--;
            if (timerCounter <= 0){ irqPending = 0; timerCounter = timerReload; }
            if (irqPending >= 0){ dispatchIrq(); continue; }
            int op=fetch();
            switch(op){
                case 0xB0: case 0xB1: case 0xB2: case 0xB3:
                case 0xB4: case 0xB5: case 0xB6: case 0xB7: {
                    int regNum = op - 0xB0;
                    int imm = fetch();
                    gpr[regNum] = (gpr[regNum] & ~0xFF) | (imm & 0xFF);
                    break;
                }
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
                case 0xE6: {
                    int port = fetch();
                    int value = gpr[0] & 0xFF;
                    if (port == 1) System.out.print((char) value);
                    else System.out.println("OUT to unknown port " + port);
                    break;
                }
                case 0xCF: {
                    eip = pop32();
                    eflags = pop32();
                    break;
                }
                case 0xF4: halted=true; timerReload=0; break;
                default:
                    System.out.println("Unknown opcode: 0x"+Integer.toHexString(op));
                    halted=true;
            }
            count++;
            if (irqPending >= 0) dispatchIrq();
        }
        System.out.println("\nExecuted "+count+" instruction(s).");
    }
}
