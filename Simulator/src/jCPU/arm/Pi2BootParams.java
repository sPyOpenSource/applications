package jCPU.arm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Supplies the boot parameters a Pi 2 Linux kernel expects on entry.
 *
 * <p>The ARM Linux boot protocol passes r0=0, r1=machine type and r2=physical
 * address of a tagged list or DTB.  Without a DTB the kernel cannot discover
 * memory or a console, so it dies before it ever touches a UART register —
 * which matches "instructions execute but stdout stays empty".  This builds a
 * minimal, valid flat device tree with a PL011 serial node and a console
 * command line, places it in RAM and loads the boot registers.
 */
public final class Pi2BootParams {
	/** Physical address of the DTB (32 MiB).
	 *
	 * <p>Must sit above the zImage's self-relocation target of 0x01000000:
	 * the decompressor copies itself there (PC 0x17C: {@code stmdb sb!, ...})
	 * and would otherwise overwrite the DTB before Linux validates it. */
	public static final int DTB_ADDRESS = 0x02000000;

	private Pi2BootParams() {}

	/** Install the DTB in RAM and set r0-r2 for Linux's boot protocol. */
	public static void install(CPU cpu) {
		byte[] dtb = buildDtb();
		VirtualMemorySpace vm = cpu.getVirtualMemorySpace();
		for(int i = 0; i < dtb.length; i++) {
			vm.write(DTB_ADDRESS + i, dtb[i] & 0xFF);
		}
		cpu.writeRegister(0, 0);
		cpu.writeRegister(1, 0); // machine type ignored when r2 is a valid DTB
		cpu.writeRegister(2, DTB_ADDRESS);
	}

	/* ---- minimal FDT builder ---- */

	private static final int FDT_BEGIN_NODE = 1;
	private static final int FDT_END_NODE = 2;
	private static final int FDT_PROP = 3;
	private static final int FDT_END = 9;
	private static final int FDT_MAGIC = 0xD00DFEED;

	private static final class Fdt {
		final ByteArrayOutputStream stringsBuf = new ByteArrayOutputStream();
		final ByteArrayOutputStream structBuf = new ByteArrayOutputStream();
		final DataOutputStream strings = new DataOutputStream(stringsBuf);
		final DataOutputStream struct = new DataOutputStream(structBuf);
		final Map<String, Integer> nameOffsets = new HashMap<>();

		int nameOffset(String name) throws IOException {
			Integer off = nameOffsets.get(name);
			if(off != null) return off;
			int result = stringsBuf.size();
			strings.write(name.getBytes(StandardCharsets.US_ASCII));
			strings.write(0);
			nameOffsets.put(name, result);
			return result;
		}

		private static void align4(ByteArrayOutputStream buf) {
			while((buf.size() & 3) != 0) buf.write(0);
		}

		void beginNode(String name) throws IOException {
			struct.writeInt(FDT_BEGIN_NODE);
			struct.write(name.getBytes(StandardCharsets.US_ASCII));
			struct.write(0);
			align4(structBuf);
		}

		void endNode() throws IOException { struct.writeInt(FDT_END_NODE); }

		void prop(String name, byte[] data) throws IOException {
			struct.writeInt(FDT_PROP);
			struct.writeInt(data.length);
			struct.writeInt(nameOffset(name));
			struct.write(data);
			align4(structBuf);
		}

		void propString(String name, String value) throws IOException {
			propStrings(name, value);
		}

		void propStrings(String name, String... values) throws IOException {
			ByteArrayOutputStream dataBuf = new ByteArrayOutputStream();
			for(String value : values) {
				dataBuf.write(value.getBytes(StandardCharsets.US_ASCII));
				dataBuf.write(0);
			}
			prop(name, dataBuf.toByteArray());
		}

		void propU32(String name, int value) throws IOException {
			prop(name, new byte[] {
				(byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value
			});
		}

		void propCells(String name, int... values) throws IOException {
			byte[] data = new byte[values.length * 4];
			for(int i = 0; i < values.length; i++) {
				int v = values[i];
				data[i * 4 + 0] = (byte)(v >>> 24);
				data[i * 4 + 1] = (byte)(v >>> 16);
				data[i * 4 + 2] = (byte)(v >>> 8);
				data[i * 4 + 3] = (byte)v;
			}
			prop(name, data);
		}

		void end() throws IOException {
			struct.writeInt(FDT_END);
			struct.flush();
			strings.flush();
		}

		byte[] build() throws IOException {
			byte[] structBytes = structBuf.toByteArray();
			byte[] stringBytes = stringsBuf.toByteArray();
			int offRsvmap = 40;
			int offStruct = offRsvmap + 16; // 16-byte terminating reservation entry
			int offStrings = offStruct + structBytes.length;
			int total = offStrings + stringBytes.length;

			ByteArrayOutputStream out = new ByteArrayOutputStream(total);
			DataOutputStream d = new DataOutputStream(out);
			d.writeInt(FDT_MAGIC);
			d.writeInt(total);
			d.writeInt(offStruct);
			d.writeInt(offStrings);
			d.writeInt(offRsvmap);
			d.writeInt(17); // version
			d.writeInt(16); // last compatible version
			d.writeInt(0);  // boot_cpuid_phys
			d.writeInt(stringBytes.length);
			d.writeInt(structBytes.length);
			d.write(new byte[16]); // empty reservation block terminator
			d.write(structBytes);
			d.write(stringBytes);
			d.flush();
			return out.toByteArray();
		}
	}

	static byte[] buildDtb() {
		try {
			Fdt f = new Fdt();
			f.beginNode("");
			f.propString("compatible", "brcm,bcm2836");
			f.propString("model", "Raspberry Pi 2 Model B");
			f.propU32("#address-cells", 1);
			f.propU32("#size-cells", 1);

			f.beginNode("chosen");
			f.propString("bootargs",
				"console=ttyAMA0,115200 earlycon=pl011,mmio32,0x3f201000,115200n8 "
				+ "loglevel=8 ignore_loglevel");
			f.propString("stdout-path", "serial0:115200n8");
			f.endNode();

			f.beginNode("aliases");
			/* Alias values are full, NUL-terminated path strings — NOT phandles.
			 * of_find_node_by_path("serial0") reads this as a C string; a u32
			 * phandle would start with 0x00 and resolve to an empty path, so
			 * earlycon/stdout-path would silently fail. */
			f.propString("serial0", "/soc/serial@3f201000");
			f.endNode();

			f.beginNode("memory@0");
			f.propString("device_type", "memory");
			f.propCells("reg", 0x00000000, 0x3E000000);
			f.endNode();

			f.beginNode("soc");
			f.propString("compatible", "simple-bus");
			f.propU32("#address-cells", 1);
			f.propU32("#size-cells", 1);
			f.prop("ranges", new byte[0]); // identity mapping

			f.beginNode("serial@3f201000");
			f.propStrings("compatible", "arm,pl011", "arm,primecell");
			f.propString("model", "ARM PL011");
			f.propCells("reg", 0x3F201000, 0x1000);
			f.propU32("phandle", 1);
			f.propU32("clock-frequency", 3000000);
			f.endNode();

			f.endNode(); // soc
			f.endNode(); // root
			f.end();
			return f.build();
		} catch(IOException e) {
			throw new IllegalStateException("failed to build DTB", e);
		}
	}
}
