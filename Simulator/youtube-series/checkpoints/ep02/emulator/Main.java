package emulator;

public class Main {
    public static void main(String[] args) {
        Cpu cpu = new Cpu();
        cpu.reset();
        cpu.run();
        System.out.println("CPU halted.");
    }
}
