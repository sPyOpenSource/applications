package jCPU.x86.boot;

import jCPU.x86.X86Amd64;
import jCPU.x86.X86Core;
import jCPU.x86.X86IdeController;
import jCPU.x86.X86IdeDevice;
import jCPU.x86.X86IdeHardDrive;
import jCPU.x86.X86PitPeripheral;
import jCPU.x86.X86UartPeripheral;
import jCPU.x86.X86UefiFirmware;
import jCPU.x86.X86VgaPeripheral;
import jCPU.x86.i8086;
import java.io.File;

/**
 * Headless UEFI boot: does what the GUI does for "attach disk image" +
 * "UEFI Boot" + "Go", without Swing and without {@link CPU} (which needs the
 * jx.compiler classes from another repository).
 *
 * <pre>
 *   java jCPU.x86.boot.UefiBootRunner disk.img [maxSteps] [--after TEXT --type KEYS]...
 * </pre>
 *
 * Without {@code --after}/{@code --type}, standard input is the keyboard.
 * With them, each KEYS is typed once TEXT has appeared in the output after
 * the previous match, so an interactive session runs the same way every
 * time.  KEYS understands {@code \n} (Enter) and {@code \e} (Escape).
 *
 * Firmware and EFI console output go to stdout through the UART.  The run
 * ends on an exception (e.g. an unknown opcode, with its instruction
 * history) or after maxSteps instructions; the exit code is 0 only when the
 * EFI application exited with EFI_SUCCESS.
 */
public class UefiBootRunner
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 1) {
            System.err.println("usage: UefiBootRunner disk.img [maxSteps] [--after TEXT --type KEYS]...");
            System.exit(2);
        }
        String image = args[0];
        int argi = 1;
        long maxSteps = 50_000_000L;
        if (args.length > 1 && !args[1].startsWith("--")) {
            maxSteps = Long.parseLong(args[1]);
            argi = 2;
        }
        java.util.List<String[]> script = new java.util.ArrayList<>();
        for (; argi + 3 < args.length && args[argi].equals("--after") && args[argi + 2].equals("--type"); argi += 4) {
            script.add(new String[] { args[argi + 1], unescape(args[argi + 3]) });
        }
        if (argi < args.length) {
            System.err.println("unexpected argument: " + args[argi]);
            System.exit(2);
        }

        // With a script, the keyboard is a pipe the runner types into, and
        // the output is kept so the runner can see when to type.
        StringBuilder output = new StringBuilder();
        java.io.PipedOutputStream keys = null;
        if (!script.isEmpty()) {
            keys = new java.io.PipedOutputStream();
            System.setIn(new java.io.PipedInputStream(keys));
            java.io.PrintStream original = System.out;
            System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
                @Override
                public void write(int b) {
                    original.write(b);
                    synchronized (output) {
                        output.append((char) (b & 0xFF));
                    }
                }

                @Override
                public void flush() {
                    original.flush();
                }
            }, true));
        }
        int scriptIndex = 0;
        int searchFrom = 0;

        // Same devices and ports as CPU's constructor.
        X86Core core = new X86Core(X86Core.DEFAULT_MEMORY_SIZE);
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
        X86PitPeripheral pit = new X86PitPeripheral(core);
        for (int port = 0x40; port <= 0x43; port++) {
            core.registerPort(port, pit);
        }
        core.registerPort(0x61, pit);
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

        long steps = 0;
        int status = 1;
        try {
            while (steps < maxSteps && !uefi.hasExited()) {
                core.step();
                steps++;
                if (scriptIndex < script.size() && steps % 10_000 == 0) {
                    int at;
                    synchronized (output) {
                        at = output.indexOf(script.get(scriptIndex)[0], searchFrom);
                    }
                    if (at >= 0) {
                        searchFrom = at + script.get(scriptIndex)[0].length();
                        keys.write(script.get(scriptIndex)[1].getBytes("ISO-8859-1"));
                        keys.flush();
                        scriptIndex++;
                    }
                }
            }
            if (uefi.hasExited()) {
                System.out.printf("[runner] done after %d steps%n", steps);
                status = uefi.getExitStatus() == 0 ? 0 : 1;
            } else {
                System.out.println();
                System.out.printf("[runner] stopped after %d steps at RIP=0x%x; where it runs now:%n",
                        steps, core.getPC64());
                // Show the loop it is in: the next instructions, each address once.
                java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
                for (int i = 0; i < 2000 && seen.size() < 40 && !uefi.hasExited(); i++) {
                    long rip = core.getPC64();
                    if (seen.add(rip)) {
                        StringBuilder bytes = new StringBuilder();
                        for (int k = 0; k < 8; k++) {
                            bytes.append(String.format(" %02X", core.readMem8(rip + k)));
                        }
                        System.out.printf("  0x%x%s%n", rip, bytes);
                    }
                    core.step();
                }
            }
        } catch (Exception e) {
            System.out.println();
            System.out.printf("[runner] %s after %d steps:%n%s%n",
                    e.getClass().getSimpleName(), steps, e.getMessage());
        }
        System.exit(status);
    }

    private static String unescape(String s)
    {
        return s.replace("\\n", "\n").replace("\\e", "\u001b");
    }
}
