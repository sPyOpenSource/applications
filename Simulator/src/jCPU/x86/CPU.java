/**
 * $Id: Test.java,v 1.1 2007/02/16 07:50:23 mviara Exp $
 * $Name:  $
 *
 *
 * Bytechnology S400
 * 
 * $Log: Test.java,v $
 * Revision 1.1  2007/02/16 07:50:23  mviara
 * *** empty log message ***
 *
 * Revision 1.2  2005/11/29 03:47:03  mviara
 * Added same bit name.
 *
 * Revision 1.1  2005/11/27 23:59:39  mviara
 * Preliminary version of S400.
 *
 * 
 */
package jCPU.x86;

import jCPU.MCS51.MCS51Peripheral;
import jCPU.MCS51.SfrReadListener;
import jCPU.MCS51.SfrWriteListener;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.JFrame;


class G128x64 extends j51.device.lcd.GLcd implements MCS51Peripheral,
					      SfrWriteListener,
					      SfrReadListener
{
	private final int ADDL = 0xfd;
	private final int ADDH = 0xfe;
	private final int DATA = 0xff;
	
	jCPU.MCS51.CPU cpu;
	private int address = 0;
	
	public G128x64()
	{
		super(128, 64, 2);
	}

        @Override
	public void registerCpu(jCPU.MCS51.CPU cpu)
	{
		this.cpu = cpu;
		cpu.addSfrWriteListener(ADDL,this);
		cpu.addSfrWriteListener(ADDH,this);
		cpu.addSfrWriteListener(DATA,this);

		cpu.addSfrReadListener(DATA,this);
	}

        @Override
	public void sfrWrite(int r,int v)
	{
		switch (r)
		{
			case	ADDL:
				address &= 0xff00;
				address |= v & 0xff;
				break;
			case	ADDH:
				address &= 0x00ff;
				address |= v << 8;
				break;
			case	DATA:
				setMemory(address,(byte)(v & 0xff));
				break;
		}
	}

        @Override
	public int sfrRead(int r)
	{
		return getMemory(address) & 0xff;
	}
}

public class CPU extends jx.disass.x86
{
	private final X86Core core;
	private JFrame vgaFrame;
	private X86VgaPeripheral vga;
	private X86UartPeripheral uart;
	private X86IdeController ide;
	private X86UefiFirmware uefi;
	private X86UhciController uhci;
	private X86FloppyDrive floppy;
	private X86BiosFirmware bios;
	private boolean traceExecution;

	public CPU() throws Exception
	{
		core = new X86Core(256 * 1024 * 1024);
		core.setActiveExecutor(new i8086());

		// VGA text/graphics memory at 0xB8000, VGA I/O ports 0x3C0-0x3DA
		vga = new X86VgaPeripheral();
		for (int port = 0x3C0; port <= 0x3DA; port++) {
			core.registerPort(port, vga);
		}
		// Forward VGA text memory writes (0xB8000-0xBFFFF) to the VGA peripheral
		// so the display updates when boot-sector code writes to video memory.
		core.setVgaMemWrite(addr -> vga.writeMemory(addr, core.readMem8(addr)));
		// UART COM1 at I/O ports 0x3F8-0x3FF
		uart = new X86UartPeripheral();
		for (int port = 0x3F8; port <= 0x3FF; port++) {
			core.registerPort(port, uart);
		}
		// 8254 timer (0x40-0x43) and port B (0x61), which gates and reads PIT channel 2
		X86PitPeripheral pit = new X86PitPeripheral(core);
		for (int port = 0x40; port <= 0x43; port++) {
			core.registerPort(port, pit);
		}
		core.registerPort(0x61, pit);
		// IDE primary channel: data/error/features/sector count/sector/cylinder/drive-head/status-command
		// (0x1F0-0x1F7) and alternate status/device control (0x3F6)
		ide = new X86IdeController();
		try {
			ide.setMaster(new X86IdeHardDrive("QEMU HARDDISK", 65536, null));
			ide.setSlave(new X86IdeCdrom("QEMU CDROM", 0, null));
		} catch (IOException ignore) { }
		for (int port = 0x1F0; port <= 0x1F7; port++) {
			core.registerPort(port, ide);
		}
		core.registerPort(0x3F6, ide);

		// UHCI USB host controller at I/O ports 0xE000-0xE011 (2 ports)
		uhci = new X86UhciController(0xE000, 2);
		for (int port = 0xE000; port <= 0xE011; port++) {
			core.registerPort(port, uhci);
		}

		// UEFI firmware
		uefi = new X86UefiFirmware(core, ide, uart, vga);

		// Floppy disk drive (drive 0x00, blank until an image is attached).
		// The BIOS reads the boot sector from here when booting from floppy.
		floppy = new X86FloppyDrive("FLOPPY", 2880, null);
		bios = new X86BiosFirmware(core, floppy, vga, uart);
		// INT 0x10 / 0x13 BIOS services (used by boot-sector code).
		core.setIntHandler((c, vector) -> bios.handleInt(vector));

		//enableVga(vga);
		setOscillator(11184000);
	}

	public X86Core getCore() {
		return core;
	}

	/**
	 * Run the UEFI boot sequence: locate the ESP via GPT, load
	 * \EFI\BOOT\BOOTIA32.EFI via FAT+PE, and return its entry point.
	 * Caller should set PC to the returned address before stepping.
	 */
	public long uefiBoot()
	{
		long entryPoint = uefi.boot();
		if (entryPoint != 0 && uefi.isPe32Plus()) {
			setLongMode(true);
		}
		return entryPoint;
	}

	public boolean uefiInitialized()
	{
		return uefi.isInitialized();
	}

	/**
	 * Load an OVMF.fd UEFI firmware image into simulated memory.
	 *
	 * <p>The image is loaded so that it ends at the top of the 4GB address
	 * space (0xFFFFFFFF), matching how QEMU places OVMF for IA32.  The IA32
	 * reset vector at 0xFFFFFFF0 therefore maps into the loaded image, and
	 * {@link #setPC(int)} should be called with 0xFFFFFFF0 before starting.</p>
	 *
         * @param path
	 * @return the number of bytes loaded, or -1 if the file could not be read.
	 */
	public int loadOvmf(String path)
	{
		try
		{
			byte[] data = Files.readAllBytes(Paths.get(path));
			// Load so the image ends at the top of available physical memory,
			// matching QEMU's OVMF placement relative to the RAM top.
			int base = core.getMemSize() - data.length;
			for (int i = 0; i < data.length; i++)
			{
				core.writeMem8(base + i, data[i]);
			}
			System.out.println("OVMF.fd loaded: " + data.length + " bytes at 0x" +
					Integer.toHexString(base) +
					", reset vector at 0x" + Integer.toHexString(base + data.length - 16) +
					" -> FD offset 0x" + Integer.toHexString(data.length - 16));
			return data.length;
		}
		catch (IOException e)
		{
			return -1;
		}
	}

	/**
	 * Return the reset vector address for an OVMF image loaded by {@link
	 * #loadOvmf}: 16 bytes below the top of physical memory.
	 */
	public int getOvmfResetVector()
	{
		return core.getMemSize() - 16;
	}

	/**
	 * Replace the IDE master drive with a raw sector disk image (e.g. a floppy
	 * or hard disk {@code .img} file).  The image size must be a multiple of
	 * 512 bytes.  After attaching, use {@link #uefiBoot()} to attempt a UEFI
	 * boot (GPT-partitioned images only), or read files from a FAT-formatted
	 * image using the FAT filesystem reader.
	 *
	 * @param path path to the raw sector disk image file
	 * @throws IOException if the image cannot be read or its size is not sector-aligned
	 */
	public void attachDiskImage(String path) throws IOException
	{
		java.io.File file = new java.io.File(path);
		long size = file.length();
		if (size % X86IdeDevice.SECTOR_SIZE != 0) {
			throw new IOException("Disk image size (" + size + " bytes) is not a multiple of "
					+ X86IdeDevice.SECTOR_SIZE + " bytes: " + path);
		}
		int sectorCount = (int) (size / X86IdeDevice.SECTOR_SIZE);
		X86IdeHardDrive drive = new X86IdeHardDrive("Disk Image: " + file.getName(), sectorCount, path);
		ide.setMaster(drive);
		System.out.println("Disk image attached: " + path + " (" + sectorCount + " sectors, "
				+ size + " bytes)");
	}

	/**
	 * Attach a raw floppy disk image to the floppy drive.  The image size must
	 * be a multiple of 512 bytes (e.g. 2880 sectors = 1.44 MB).  After
	 * attaching, use {@link #floppyBoot()} to attempt a BIOS floppy boot.
	 *
	 * @param path path to the raw floppy disk image file
	 * @throws IOException if the image cannot be read or its size is not sector-aligned
	 */
	public void attachFloppyImage(String path) throws IOException
	{
		java.io.File file = new java.io.File(path);
		long size = file.length();
		if (size % X86IdeDevice.SECTOR_SIZE != 0) {
			throw new IOException("Floppy image size (" + size + " bytes) is not a multiple of "
					+ X86IdeDevice.SECTOR_SIZE + " bytes: " + path);
		}
		int sectorCount = (int) (size / X86IdeDevice.SECTOR_SIZE);
		floppy = new X86FloppyDrive("Floppy Image: " + file.getName(), sectorCount, path);
		bios = new X86BiosFirmware(core, floppy, vga, uart);
		core.setIntHandler((c, vector) -> bios.handleInt(vector));
		System.out.println("Floppy image attached: " + path + " (" + sectorCount + " sectors, "
				+ size + " bytes)");
	}

	/**
	 * BIOS boot from floppy disk (INT 0x19).  Reads the boot sector from the
	 * floppy drive, verifies the {@code 0x55AA} signature, loads it to
	 * physical {@code 0x7C00}, sets {@code DL = 0x00}, and jumps to
	 * {@code 0x0000:0x7C00}.
	 *
	 * @return true if a bootable floppy sector was found and control
	 *         transferred to {@code 0x7C00}.
	 */
	public boolean floppyBoot()
	{
		return bios.floppyBoot();
	}

	/**
	 * Returns the floppy drive so callers can inspect or reattach media.
	 */
	public X86FloppyDrive getFloppyDrive()
	{
		return floppy;
	}

	/**
	 * Returns the BIOS firmware so callers can queue keyboard input
	 * (see {@link X86BiosFirmware#pushKey(int)}).
	 */
	public X86BiosFirmware getBios()
	{
		return bios;
	}

	/**
	 * Returns the IDE controller so callers can inspect or reattach drives.
	 */
	public X86IdeController getIdeController()
	{
		return ide;
	}

	public void setPC(int pc)
	{
		core.setPC(pc);
	}

	/**
	 * Enable 64-bit long mode and switch the instruction executor to the
	 * x86-64 decoder.  Call this before starting execution of a PE32+
	 * (x64) UEFI application.
	 */
	public void setLongMode(boolean longMode)
	{
		core.setLongMode(longMode);
		if (longMode) {
			core.setActiveExecutor(new X86Amd64());
		} else {
			core.setActiveExecutor(new i8086());
		}
	}

	public boolean isLongMode()
	{
		return core.isLongMode();
	}

	public long getPC64()
	{
		return core.getPC64();
	}

	public void setPC64(long pc)
	{
		core.setPC64(pc);
	}

	public void setTraceExecution(boolean trace)
	{
		this.traceExecution = trace;
	}

	public boolean isTraceExecution()
	{
		return traceExecution;
	}

        @Override
	public void stopSimulation()
	{
		core.stopSimulation();
	}

	public void dumpRegisters()
	{
		System.out.println("=== X86 Registers ===");
		if (core.isLongMode()) {
			System.out.println(String.format("  RAX=0x%016X  RBX=0x%016X  RCX=0x%016X  RDX=0x%016X",
					core.getReg64(X86Core.REG_RAX), core.getReg64(X86Core.REG_RBX),
					core.getReg64(X86Core.REG_RCX), core.getReg64(X86Core.REG_RDX)));
			System.out.println(String.format("  RSI=0x%016X  RDI=0x%016X  RBP=0x%016X  RSP=0x%016X",
					core.getReg64(X86Core.REG_RSI), core.getReg64(X86Core.REG_RDI),
					core.getReg64(X86Core.REG_RBP), core.getReg64(X86Core.REG_RSP)));
			System.out.println(String.format("  R8=0x%016X  R9=0x%016X  R10=0x%016X  R11=0x%016X",
					core.getReg64(X86Core.REG_R8), core.getReg64(X86Core.REG_R9),
					core.getReg64(X86Core.REG_R10), core.getReg64(X86Core.REG_R11)));
			System.out.println(String.format("  R12=0x%016X R13=0x%016X R14=0x%016X R15=0x%016X",
					core.getReg64(X86Core.REG_R12), core.getReg64(X86Core.REG_R13),
					core.getReg64(X86Core.REG_R14), core.getReg64(X86Core.REG_R15)));
			System.out.println(String.format("  RIP=0x%016X RFLAGS=0x%016X",
					core.getPC64(), core.getRflags64()));
		} else {
			System.out.println(String.format("  EAX=0x%08X  EBX=0x%08X  ECX=0x%08X  EDX=0x%08X",
					core.getReg(0), core.getReg(3), core.getReg(1), core.getReg(2)));
			System.out.println(String.format("  ESI=0x%08X  EDI=0x%08X  EBP=0x%08X  ESP=0x%08X",
					core.getReg(6), core.getReg(7), core.getReg(5), core.getReg(4)));
			System.out.println(String.format("  PC=0x%08X  FLAGS=0x%08X",
					core.getPC(), core.getFlags()));
		}
	}

	/**
	 * Show the VGA console window by default (skipped in headless environments).
	 */
	private void enableVga(X86VgaPeripheral vga) {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		vgaFrame = new JFrame("VGA - x86");
		vgaFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		vgaFrame.getContentPane().add(vga.getDisplay());
		vgaFrame.pack();
		vgaFrame.setLocationRelativeTo(null);
		vgaFrame.setVisible(true);
	}

        @Override
	public int step() throws Exception
	{
		uhci.tick();
		if (core.isLongMode()) {
			long rip = core.getPC64();
			int opcode = core.readMem8((int) rip);
			if (traceExecution)
			{
				System.out.println(String.format("RIP=0x%016X OP=0x%02X RAX=0x%016X RSP=0x%016X",
						rip, opcode, core.getReg64(X86Core.REG_RAX), core.getReg64(X86Core.REG_RSP)));
			}
			return core.step();
		}
		int pc = core.getPC();
		int opcode = core.readMem8(pc);
		if (traceExecution)
		{
			System.out.println(String.format("PC=0x%08X OP=0x%02X EAX=0x%08X ESP=0x%08X",
					pc, opcode, core.getReg(0), core.getReg(4)));
			if (pc < 0xfff00000)
			{
				System.out.println("  *** PC outside OVMF range! Dumping registers:");
				dumpRegisters();
			}
		}
		return core.step();
	}

        @Override
	public void go(int limit) throws Exception
	{
		core.go(limit);
	}

        @Override
	public String getDecodeAt(int pc)
	{
		return core.getDecodeAt(pc);
	}
}
