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
 * Test domain for media change detection (eject/insert).
 * Waits for USBFS portal, then:
 * 1. Reads root dir (triggers media check)
 * 2. Creates a file to verify media present
 * 3. Prints capacity changes if any
 * 
 * In QEMU, use monitor command to change media:
 *   (qemu) change usb-hd0 /tmp/usbtest2.img
 * 
 * Expected: UNIT_ATTENTION/0x28 triggers processChanged() re-read capacity
 */
public class MediaChangeTest {

    private static final String TEST_FILE = "media_change_test.txt";
    private static final byte[] PAYLOAD = "Media change test".getBytes();

    public static void init(Naming naming, String[] args) {
        DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
        Debug.out = new DebugPrintStream(new DebugOutputStream(d));
        main(args);
    }

    public static void main(String[] args) {
        Naming naming = InitialNaming.getInitialNaming();
        try {
            final FS fs = (FS) LookupHelper.waitUntilPortalAvailable(naming, USBStorageMount.FS_PORTAL_NAME);
            Debug.out.println("MediaChangeTest: FS portal available");
            run(naming, fs);
        } catch (Throwable t) {
            Debug.out.println("MediaChangeTest: FAILED: " + t);
        }
    }

    private static void run(Naming naming, FS fs) throws Exception {
        // Initial capacity check via BlockIO portal
        printCapacity(naming);
        
        // Test 1: Read root directory (triggers processChanged if media changed)
        final Node root = fs.getCwdNode();
        Debug.out.println("MediaChangeTest: root entries = " + count(root));

        // Test 2: Write a file (verifies media writable)
        final MemoryManager rm = (MemoryManager) naming.lookup("MemoryManager");
        final Memory mem = rm.alloc(PAYLOAD.length);
        mem.copyFromByteArray(PAYLOAD, 0, 0, PAYLOAD.length);

        final Node file = root.create(TEST_FILE, InodeImpl.S_IWUSR | InodeImpl.S_IRUGO);
        final int written = file.write(mem, 0, PAYLOAD.length);
        Debug.out.println("MediaChangeTest: wrote " + written + " bytes");

        // Test 3: Read back
        final Memory rbuf = rm.alloc(PAYLOAD.length);
        final int read = file.read(rbuf, 0, PAYLOAD.length);
        boolean ok = read == PAYLOAD.length;
        for (int i = 0; i < PAYLOAD.length && ok; i++) {
            ok = rbuf.get8(i) == PAYLOAD[i];
        }
        Debug.out.println("MediaChangeTest: read " + read + " bytes, content " + (ok ? "OK" : "MISMATCH"));

        // Test 4: Capacity again (should be stable)
        printCapacity(naming);

        // Cleanup
        root.unlink(TEST_FILE);
        Debug.out.println("MediaChangeTest: " + (ok ? "PASS" : "FAIL"));
    }

    private static void printCapacity(Naming naming) {
        try {
            // The USBBlockIO portal should be available alongside USBFS
            jx.devices.bio.BlockIO bio = (jx.devices.bio.BlockIO) 
                LookupHelper.waitUntilPortalAvailable(naming, USBStorageMount.BIO_PORTAL_NAME);
            Debug.out.println("MediaChangeTest: capacity = " + bio.getCapacity() 
                + " sectors x " + bio.getSectorSize() + " bytes");
        } catch (Exception e) {
            Debug.out.println("MediaChangeTest: capacity check failed: " + e);
        }
    }

    private static int count(Node dir) {
        try {
            return dir.readdirNames().length;
        } catch (Exception e) {
            return -1;
        }
    }
}