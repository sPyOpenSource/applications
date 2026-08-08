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
 * Test domain for large file / multi-sector transfers.
 * Verifies:
 * - Chunked transfers > MAX_BOT_TRANSFER (64KB)
 * - Exact 64KB boundary
 * - Partial sector zero-padding
 * - Multi-sector read/write
 */
public class LargeFileTest {

    // Sizes to test
    private static final int[] TEST_SIZES = {
        512,          // 1 sector
        4096,         // 8 sectors
        32768,        // 64 sectors (32KB)
        65536,        // 128 sectors (64KB = MAX_BOT_TRANSFER boundary)
        65537,        // 64KB + 1 byte (crosses chunk boundary)
        131072,       // 128KB (2 chunks)
        204800,       // 200KB (3+ chunks)
        524288        // 512KB
    };

    private static final String TEST_PREFIX = "largetest_";

    public static void init(Naming naming, String[] args) {
        DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
        Debug.out = new DebugPrintStream(new DebugOutputStream(d));
        main(args);
    }

    public static void main(String[] args) {
        Naming naming = InitialNaming.getInitialNaming();
        try {
            final FS fs = (FS) LookupHelper.waitUntilPortalAvailable(naming, USBStorageMount.FS_PORTAL_NAME);
            Debug.out.println("LargeFileTest: FS portal available");
            run(naming, fs);
        } catch (Throwable t) {
            Debug.out.println("LargeFileTest: FAILED: " + t);
        }
    }

    private static void run(Naming naming, FS fs) throws Exception {
        final Node root = fs.getCwdNode();
        final MemoryManager rm = (MemoryManager) naming.lookup("MemoryManager");

        boolean allPassed = true;
        for (int size : TEST_SIZES) {
            String name = TEST_PREFIX + size + ".dat";
            boolean passed = testSize(naming, root, rm, name, size);
            allPassed &= passed;
            // Clean up between tests
            try { root.unlink(name); } catch (Exception ignore) {}
        }

        Debug.out.println("LargeFileTest: " + (allPassed ? "ALL PASSED" : "SOME FAILED"));
    }

    private static boolean testSize(Naming naming, Node root, MemoryManager rm, String name, int size) {
        Debug.out.println("LargeFileTest: testing size=" + size + " bytes (" + (size/1024) + "KB)");

        // Generate deterministic payload
        byte[] payload = new byte[size];
        for (int i = 0; i < size; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        // Write
        Memory mem = rm.alloc(size);
        mem.copyFromByteArray(payload, 0, 0, size);

        try {
            Node file = root.create(name, InodeImpl.S_IWUSR | InodeImpl.S_IRUGO);
            int written = file.write(mem, 0, size);
            if (written != size) {
                Debug.out.println("LargeFileTest: FAIL size=" + size + " wrote=" + written + " expected=" + size);
                return false;
            }
            Debug.out.println("LargeFileTest: wrote " + written + " bytes");

            // Read back
            Memory rbuf = rm.alloc(size);
            int read = file.read(rbuf, 0, size);
            if (read != size) {
                Debug.out.println("LargeFileTest: FAIL size=" + size + " read=" + read + " expected=" + size);
                return false;
            }

            // Verify content
            boolean ok = true;
            for (int i = 0; i < size; i++) {
                if (rbuf.get8(i) != payload[i]) {
                    Debug.out.println("LargeFileTest: FAIL size=" + size + " mismatch at byte " + i 
                        + ": got 0x" + Integer.toHexString(rbuf.get8(i) & 0xFF) 
                        + " expected 0x" + Integer.toHexString(payload[i] & 0xFF));
                    ok = false;
                    break;
                }
            }

            Debug.out.println("LargeFileTest: size=" + size + " " + (ok ? "PASS" : "FAIL"));
            return ok;

        } catch (Exception e) {
            Debug.out.println("LargeFileTest: FAIL size=" + size + " exception: " + e);
            return false;
        }
    }
}