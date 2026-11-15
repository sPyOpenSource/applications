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
	private X86IdeController ide;
	private X86UefiFirmware uefi;
	private X86UhciController uhci;

	public CPU() throws Exception
	{
		core = new X86Core(64 * 1024 * 1024);
		core.setActiveExecutor(new i8086());

		// VGA text/graphics memory at 0xB8000, VGA I/O ports 0x3C0-0x3DA
		X86VgaPeripheral vga = new X86VgaPeripheral();
		for (int port = 0x3C0; port <= 0x3DA; port++) {
			core.registerPort(port, vga);
		}
		// UART COM1 at I/O ports 0x3F8-0x3FF
		X86UartPeripheral uart = new X86UartPeripheral();
		for (int port = 0x3F8; port <= 0x3FF; port++) {
			core.registerPort(port, uart);
		}
		// IDE primary channel: data/error/features/sector count/sector/cylinder/drive-head/status-command
		// (0x1F0-0x1F7) and alternate status/device control (0x3F6)
		ide = new X86IdeController();
		try {
			ide.setMaster(new X86IdeHardDrive("QEMU HARDDISK", 65536, null));
			ide.setSlave(new X86IdeCdrom("QEMU CDROM", 0, null));
		} catch (Exception ignore) { }
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

		enableVga(vga);
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
	public int uefiBoot()
	{
		return uefi.boot();
	}

	public boolean uefiInitialized()
	{
		return uefi.isInitialized();
	}

	/**
	 * Load an OVMF.fd UEFI firmware image into simulated memory starting at
	 * address 0.  Returns the number of bytes loaded, or -1 if the file
	 * could not be read.
	 *
	 * <p>After loading, call {@link #setPC(int)} to the desired entry point
	 * (for IA32 OVMF the reset vector is typically at 0xFFFFFFF0, or the
	 * SEC phase entry near the start of the image) and then {@link #go(int)}
	 * to begin execution.</p>
	 */
	public int loadOvmf(String path)
	{
		try
		{
			byte[] data = Files.readAllBytes(Paths.get(path));
			int base = 0; // load at the beginning of the 4GB flat address space
			for (int i = 0; i < data.length; i++)
			{
				core.writeMem8(base + i, data[i]);
			}
			return data.length;
		}
		catch (IOException e)
		{
			return -1;
		}
	}

	public void setPC(int pc)
	{
		core.setPC(pc);
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
