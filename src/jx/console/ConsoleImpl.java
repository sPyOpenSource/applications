package jx.console;

import java.io.IOException;
import jx.devices.Screen;
import jx.devices.Keyboard;
import jx.shell.Shell;
import jx.zero.debug.*;
import jx.zero.*;

public class ConsoleImpl implements Console {
    private final Screen screen;
    private final Keyboard keyboard;
    private Shell shell;

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
        
        shell = new Shell(current.getOutputStream(), current.getInputStream());
        
        /*shell.register("ifconfig", null);
        shell.register("netstat", null);
        
        shell.register("cp", null);
        shell.register("file", null);
        shell.register("lp", null);
        shell.register("ls", null);
        shell.register("more", null);
        shell.register("mv", null);
        shell.register("pr", null);
        shell.register("rm", null);
        
        shell.register("man", null);
        
        shell.register("cd", null);
        shell.register("pwd", null);
        
        shell.register("wc", null);
        shell.register("grep", null);*/
        
        current.activate();
        
        try {
            shell.mainloop();
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
}
