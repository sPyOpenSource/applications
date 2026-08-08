package test.fs;

import jx.fs.FS;
import jx.fs.InodeImpl;
import jx.fs.Node;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.LookupHelper;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.Naming;
import jx.zero.debug.DebugChannel;
import jx.zero.debug.DebugOutputStream;
import jx.zero.debug.DebugPrintStream;
import org.jnode.driver.block.usb.storage.scsi.USBStorageMount;

/**
 * Test domain that verifies a USB mass storage device mounted by
 * {@link USBStorageMount} end-to-end: create a file, write to it, read it
 * back, verify the content and delete it again. Results are printed on the
 * debug console (serial in QEMU).
 */
public class USBMassStorageTest {

    private static final String TEST_FILE = "usbtest.txt";
    private static final byte[] PAYLOAD = "Hello USB!".getBytes();

    public static void init(Naming naming, String[] args) {
        DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
        Debug.out = new DebugPrintStream(new DebugOutputStream(d));
        main(args);
    }

    public static void main(String[] args) {
        Naming naming = InitialNaming.getInitialNaming();
        try {
            final FS fs = (FS) LookupHelper.waitUntilPortalAvailable(naming, USBStorageMount.FS_PORTAL_NAME);
            Debug.out.println("USBMassStorageTest: FS portal '" + USBStorageMount.FS_PORTAL_NAME + "' available");
            run(fs);
        } catch (Exception t) {
            Debug.out.println("USBMassStorageTest: FAILED: " + t);
        }
    }

    private static void run(FS fs) throws Exception {
        final Node root = fs.getCwdNode();
        Debug.out.println("USBMassStorageTest: root contains " + count(root) + " entries");

        final MemoryManager rm = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
        final Memory mem = rm.alloc(PAYLOAD.length);
        mem.copyFromByteArray(PAYLOAD, 0, 0, PAYLOAD.length);

        final Node file = root.create(TEST_FILE, InodeImpl.S_IWUSR | InodeImpl.S_IRUGO);
        final int written = file.write(mem, 0, PAYLOAD.length);
        final Memory rbuf = rm.alloc(PAYLOAD.length);
        final int read = file.read(rbuf, 0, PAYLOAD.length);
        root.unlink(TEST_FILE);

        boolean contentOk = written == PAYLOAD.length && read == PAYLOAD.length;
        for (int i = 0; i < PAYLOAD.length && contentOk; i++) {
            contentOk = rbuf.get8(i) == PAYLOAD[i];
        }
        Debug.out.println("USBMassStorageTest: wrote=" + written + " read=" + read
            + " contentOk=" + contentOk);
        Debug.out.println("USBMassStorageTest: " + (contentOk ? "PASS" : "FAIL"));
    }

    private static int count(Node dir) {
        try {
            return dir.readdirNames().length;
        } catch (Exception e) {
            return -1;
        }
    }
}
