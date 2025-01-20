package test.debug;

import jx.zero.*;
import jx.zero.debug.*;

public class Monitor {
    public static void init(Naming naming, String[] args) {
	//Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	final DebugSupport debugSupport = (DebugSupport) naming.lookup("DebugSupport");
        if (debugSupport == null){
            return;
        }
	debugSupport.registerMonitorCommand("test", new MonitorCommand() {
            @Override
            public void execCommand(String[] args) {
                Debug.out.println("***********************************************");
                Debug.out.println("*             SUCCESS                         *");
                Debug.out.println("***********************************************");
            }
            @Override
            public String getHelp() {
                return "";
            }
        });
    }
}
