package jCPU.x86;

import java.io.File;

/**
 * Headless UEFI boot: does what the GUI does for "attach disk image" +
 * "UEFI Boot" + "Go", without Swing and without {@link CPU} (which needs the
 * jx.compiler classes from another repository).
 *
 * <pre>
 *   java jCPU.x86.UefiBootRunner disk.img [maxSteps]
 * </pre>
 *
 * Firmware and EFI console output go to stdout through the UART.  The run
 * ends on an exception (e.g. an unknown opcode, with its instruction
 * history) or after maxSteps instructions; the exit code is 0 only when the
 * EFI application returned to the address the runner placed on the stack.
 */
public class UefiBootRunner
{
    /** Return address pushed before the entry point; reaching it means efi_main returned. */
    static final long RETURN_ADDR = 0xDEAD0000L;

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1) {
            System.err.println("usage: UefiBootRunner disk.img [maxSteps]");
            System.exit(2);
        }
        String image = args[0];
        long maxSteps = args.length > 1 ? Long.parseLong(args[1]) : 50_000_000L;

        // Same devices and ports as CPU's constructor.
        X86Core core = new X86Core(64 * 1024 * 1024);
        core.setActiveExecutor(new i8086());
        X86VgaPeripheral vga = new X86VgaPeripheral();
        for (int port = 0x3C0; port <= 0x3DA; port++) {
            core.registerPort(port, vga);
        }
        core.setVgaMemWrite(addr -> vga.writeMemory(addr, core.readMem8(addr)));
        X86UartPeripheral uart = new X86UartPeripheral();
        for (int port = 0x3F8; port <= 0x3FF; port++) {
            core.registerPort(port, uart);
        }
        X86IdeController ide = new X86IdeController();
        for (int port = 0x1F0; port <= 0x1F7; port++) {
            core.registerPort(port, ide);
        }
        core.registerPort(0x3F6, ide);

        // Same as CPU.attachDiskImage.
        long size = new File(image).length();
        ide.setMaster(new X86IdeHardDrive("Disk Image", (int) (size / X86IdeDevice.SECTOR_SIZE), image));

        // Same as CPU.uefiBoot + the GUI's UEFI Boot action.
        X86UefiFirmware uefi = new X86UefiFirmware(core, ide, uart, vga);
        long entry = uefi.boot();
        if (entry == 0) {
            System.out.println("[runner] no bootable EFI application found");
            System.exit(1);
        }
        if (!uefi.isPe32Plus()) {
            System.out.println("[runner] only x64 (PE32+) images are run here");
            System.exit(1);
        }
        core.setLongMode(true);
        core.setActiveExecutor(new X86Amd64());
        core.setPC64(entry);
        // The firmware does not set up a stack or return address yet; give
        // the application one so a clean return can be recognised.
        if (core.getReg64(X86Core.REG_RSP) == 0) {
            core.setReg64(X86Core.REG_RSP, 0x3F00000L);
        }
        core.push64(RETURN_ADDR);

        long steps = 0;
        int status = 1;
        try {
            while (steps < maxSteps) {
                if (core.getPC64() == RETURN_ADDR) {
                    System.out.println();
                    System.out.printf("[runner] EFI application returned, RAX=0x%x%n",
                            core.getReg64(X86Core.REG_RAX));
                    status = 0;
                    break;
                }
                core.step();
                steps++;
            }
            if (steps >= maxSteps) {
                System.out.println();
                System.out.printf("[runner] stopped after %d steps at RIP=0x%x%n", steps, core.getPC64());
            }
        } catch (Exception e) {
            System.out.println();
            System.out.printf("[runner] %s after %d steps:%n%s%n",
                    e.getClass().getSimpleName(), steps, e.getMessage());
        }
        System.exit(status);
    }
}
