import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.ZipOutputStream;

public class ZipFiles {
    static short get(){return (short)0xaa55;}
	public static void main(String[] args) {
            short z = (short)0xaa55;
            if(z==get())
                System.out.println(0xffff&z);
ZipFiles zip = new ZipFiles();
Object [] x = new Object[]{null, null};
Object[]y = x.clone();
		/*try {
                    try (FileOutputStream fos = new FileOutputStream("uncompressed-test.zip"); ZipOutputStream zos = new ZipOutputStream(fos)) {
                        
                        // This sets the compression level to STORED, ie, uncompressed
                        zos.setLevel(ZipOutputStream.STORED);
                        
                        String[] fileNames = {
                            "/home/spy/OS/jcore/isodir/awt_003.jll",
                            "/home/spy/OS/jcore/isodir/awt_peer.jll",
                            "/home/spy/OS/jcore/isodir/bootrc.jll",
                            "/home/spy/OS/jcore/isodir/collections.jll",
                            "/home/spy/OS/jcore/isodir/devices.jll",
                            "/home/spy/OS/jcore/isodir/fb.jll",
                            "/home/spy/OS/jcore/isodir/fb_devices.jll",
                            "/home/spy/OS/jcore/isodir/fb_emul.jll",
                            "/home/spy/OS/jcore/isodir/framebuffer.jll",
                            "/home/spy/OS/jcore/isodir/init2.jll",
                            "/home/spy/OS/jcore/isodir/jdk0.jll",
                            "/home/spy/OS/jcore/isodir/jdk1.jll",
                            "/home/spy/OS/jcore/isodir/jdk_charstream.jll",
                            "/home/spy/OS/jcore/isodir/keyboard.jll",
                            "/home/spy/OS/jcore/isodir/keyboard_pc.jll",
                            "/home/spy/OS/jcore/isodir/list.jll",
                            "/home/spy/OS/jcore/isodir/mga_if.jll",
                            "/home/spy/OS/jcore/isodir/mouse.jll",
                            "/home/spy/OS/jcore/isodir/pci.jll",
                            "/home/spy/OS/jcore/isodir/pci_pc.jll",
                            "/home/spy/OS/jcore/isodir/sleep.jll",
                            "/home/spy/OS/jcore/isodir/streams.jll",
                            "/home/spy/OS/jcore/isodir/test_jx_awt.jll",
                            "/home/spy/OS/jcore/isodir/window_starter.jll",
                            "/home/spy/OS/jcore/isodir/wintv_util.jll",
                            "/home/spy/OS/jcore/isodir/wm.jll",
                            "/home/spy/OS/jcore/isodir/wm_impl.jll",
                            "/home/spy/OS/jcore/isodir/zero.jll",
                            "/home/spy/OS/jcore/isodir/zero_env.jll",
                            "/home/spy/OS/jcore/isodir/zero_misc.jll",
                            "/home/spy/OS/jcore/isodir/boot.rc",
                            "/home/spy/OS/jcore/isodir/boot.rc.rdp.embed",
                            "/home/spy/OS/jcore/isodir/default.fon",
                            "/home/spy/OS/jcore/isodir/jxcolors.ini",
                            "/home/spy/OS/jcore/isodir/realmode",
                            "/home/spy/OS/jcore/isodir/std.keymap",
                            "/home/spy/OS/jcore/isodir/vision01.ppm"
                        };
                        for(String file:fileNames)
                            addToZipFile(file, zos);
                    }
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}*/

	}

	public static void addToZipFile(String fileName, ZipOutputStream zos) throws FileNotFoundException, IOException {

            System.out.println("Writing '" + fileName + "' to zip file");

            byte[] data;
            try (RandomAccessFile f = new RandomAccessFile(fileName, "r")) {
                data = new byte[(int)f.length()];
                f.readFully(data);
            }
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(fileName.replace("/home/spy/OS/jcore/isodir/", ""));
            entry.setSize(data.length);
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(data);
            entry.setCrc(crc.getValue());
            zos.putNextEntry(entry);
            zos.write(data, 0, data.length);
            zos.closeEntry();
	}
}
