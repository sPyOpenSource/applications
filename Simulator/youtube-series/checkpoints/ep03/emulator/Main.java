package emulator;

public class Main {
    public static void main(String[] args) {
        Cpu cpu = new Cpu();
        cpu.reset();
        cpu.mem[0] = (byte)0xF4;   // HLT at address 0
        cpu.run();
        System.out.println("CPU halted. EIP=" + cpu.eip);
    }
}
