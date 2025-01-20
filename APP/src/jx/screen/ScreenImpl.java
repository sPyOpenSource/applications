package jx.screen;

import jx.devices.Screen;
import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.Ports;

public class ScreenImpl implements Screen, Service {
    
    final static int CGA_SCREEN  = 0xb8000;
    final static int MONO_SCREEN = 0xb0000;
    final static int EFI         = 0xc0000000;
    final static int SCREEN = EFI;

    final static short videoPortReg = 0x3d4;
    final static short videoPortVal = 0x3d5;

    DeviceMemory video;
    int x, y;
    DebugPrintStream out;
    Ports ports;
    Naming naming;
    
    public static void init(Naming naming) {
	new ScreenImpl(naming);
    }
    public ScreenImpl(Naming naming) {
	this.naming = naming;
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	out = new DebugPrintStream(new DebugOutputStream(d));
	MemoryManager memMgr = (MemoryManager) naming.lookup("MemoryManager");
	ports = (Ports) naming.lookup("Ports");
	video = memMgr.allocDeviceMemory(SCREEN, 80 * 25 * 2);
        enableCursor(0, 15);
    }

    @Override
    public int getWidth() { return 80; }
    @Override
    public int getHeight() { return 25; }

    @Override
    public void putAt(int x, int y, char c) {
	//out.println("putAt");
	if (x >= 80 || y >= 25 || x < 0 || y < 0) return;
	video.set8((80 * y + x) * 2,  (byte)c);
	video.set8((80 * y + x) * 2 + 1,  (byte)0x0f);
    }

    @Override
    public void moveCursorTo (int x, int y) {
	int offset = 80 * y + x;

	// high byte
	ports.outb(videoPortReg, (byte)0xe);
	ports.outb(videoPortVal, (byte)((offset >> 8) & 0xff)); //offset / 256

	// low byte
	ports.outb(videoPortReg, (byte)0xf);
	ports.outb(videoPortVal, (byte)(offset & 0xff)); // offset % 256
    }
    
    void enableCursor(int cursor_start, int cursor_end)
    {
        ports.outb(videoPortReg, (byte)0x0A);
        ports.outb(videoPortVal, (byte)((ports.inb(videoPortVal) & 0xC0) | cursor_start));

        ports.outb(videoPortReg, (byte)0x0B);
        ports.outb(videoPortVal, (byte)((ports.inb(videoPortVal) & 0xE0) | cursor_end));
    }

    @Override
    public void clear() {
	video.fill16((short)0x0f00, 0, 80 * 24);
	x = y = 0;
    }

    @Override
    public DeviceMemory getVideoMemory() {
	return video;
    }

}
