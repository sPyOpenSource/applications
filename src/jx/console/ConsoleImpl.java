package jx.console;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jx.devices.Screen;
import jx.devices.Keyboard;
import jx.zero.debug.*;
import jx.zero.*;

public class ConsoleImpl implements Console {
    private final Screen screen;
    private final Keyboard keyboard;

    DebugPrintStream out;
    MemoryManager memMgr;
    DeviceMemory video;
    VirtualConsole current;
    VirtualConsole cons[] = new VirtualConsoleImpl[10];

    public ConsoleImpl(Naming naming, Screen screen, Keyboard keyboard) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
        out = new DebugPrintStream(new DebugOutputStream(d));
	memMgr = (MemoryManager) naming.lookup("MemoryManager");

	this.screen = screen;
	this.keyboard = keyboard;
	screen.clear();

	current = createVirtualConsole();
	current.activate();
        try {
            for(int i = 0; i < 20; i++){
                int c = current.getInputStream().read();
                //screen.putAt(0, i, (char)c);
            }
        } catch (IOException ex) {
            //Logger.getLogger(ConsoleImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void init(Naming naming) {
	final jx.screen.ScreenImpl screen = new jx.screen.ScreenImpl(naming);
	new ConsoleImpl(naming, screen, null);
    }

    @Override
    public VirtualConsole createVirtualConsole() {
	VirtualConsole v = new VirtualConsoleImpl(memMgr, this, screen, keyboard);
	return v;
    }

    @Override
    public void switchTo(VirtualConsole cons) {
	if (current != null) current.deactivate();
	current = cons;
	current.activate();
    }

    void moveCursorTo(int x, int y) {    
	screen.moveCursorTo(x, y);
    }
    /*
    public InputStream getInputStream() {
	return in;
    }
    public OutputStream getOutputStream() {
	return out;
    }
    public OutputStream getErrorStream() {
	return err;
    }

    public void write(int c) {
	dout.println("CONSOLE::write " + c);
	screen.putc((char)c);
    }
    */
}
