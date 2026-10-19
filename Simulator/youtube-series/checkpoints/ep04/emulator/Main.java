package emulator;

public class Main {
    public static void main(String[] args) {
        Cpu cpu = new Cpu();
        cpu.reset();
        byte[] prog = {
            (byte)0xB8, 2,0,0,0,
            (byte)0x05, 3,0,0,0,
            (byte)0xF4
        };
        for (int i=0;i<prog.length;i++) cpu.mem[i]=prog[i];
        cpu.run();
        System.out.println("EAX = " + cpu.reg(Cpu.Reg.EAX));  // 5
    }
}
