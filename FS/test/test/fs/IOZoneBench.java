package test.fs;

import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class IOZoneBench {
    public static void init(Naming naming, String[]  args) throws Exception {
    DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
    Debug.out = new DebugPrintStream(new DebugOutputStream(d));
    main(args);
    }

    public static void main(String[]  args) throws Exception {
    if (args.length != 5) throw new Error("wrong args");

    Main.IOZONE_MIN_FILESIZE = Integer.parseInt(args[1]);
    Main.IOZONE_MAX_FILESIZE = Integer.parseInt(args[2]);

    Main.IOZONE_MIN_RECSIZE = Integer.parseInt(args[3]);
    Main.IOZONE_MAX_RECSIZE = Integer.parseInt(args[4]);

    new Main(InitialNaming.getInitialNaming(), args[0]);
    }
}
