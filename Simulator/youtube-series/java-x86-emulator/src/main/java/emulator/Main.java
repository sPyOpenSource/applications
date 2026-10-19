package emulator;

public class Main {
    public static void main(String[] args) {
        Cpu cpu = new Cpu();
        cpu.reset();
        cpu.timerReload = 5;
        cpu.timerCounter = 5;
        cpu.writeDword(0, 0x0200);
        int p = 0x0200;
        cpu.mem[p++] = (byte)0xB0; cpu.mem[p++] = (byte)'.';
        cpu.mem[p++] = (byte)0xE6; cpu.mem[p++] = 1;
        cpu.mem[p++] = (byte)0xCF;
        p = 0x0100;
        cpu.mem[p++] = (byte)0xB0; cpu.mem[p++] = 'H';
        cpu.mem[p++] = (byte)0xE6; cpu.mem[p++] = 1;
        cpu.mem[p++] = (byte)0xB0; cpu.mem[p++] = 'i';
        cpu.mem[p++] = (byte)0xE6; cpu.mem[p++] = 1;
        cpu.mem[p++] = (byte)0xF4;
        cpu.eip = 0x0100;
        cpu.run();
        System.out.println("EAX = " + cpu.reg(Cpu.Reg.EAX));
    }
}
