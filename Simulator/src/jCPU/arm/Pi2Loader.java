package jCPU.arm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Loads a flat Raspberry Pi 2 {@code kernel.img} into simulated RAM at
 * {@link Pi2MemoryMap#RAM_BASE} (0x00000000) and returns a CPU ready to run it.
 * <p>
 * The {@link CPU} constructor maps only 1 MB of RAM at {@code RAM_BASE};
 * {@link #loadImage(byte[])} replaces it with a lazily allocated region
 * covering the board's whole RAM space (up to the peripheral window).
 */
public final class Pi2Loader {

	/** Linux ARM 2G/2G split: PAGE_OFFSET (virt = phys + this). */
	private static final int PAGE_OFFSET = 0x80000000;
	/** Alternate 3G/1G PAGE_OFFSET, mapped for kernels built with that split. */
	private static final int PAGE_OFFSET_3G = 0xC0000000;
	/** High-vector page used when SCTLR.V is set. */
	private static final int HIGH_VECTOR_BASE = 0xFFFF0000;
	private static final int HIGH_VECTOR_SIZE = 0x10000;

	private Pi2Loader() {}

	/**
	 * Maps {@code image} bytes into RAM at {@link Pi2MemoryMap#RAM_BASE}.
	 *
	 * @return a CPU with the image loaded at address 0 (not yet reset)
	 */
	public static CPU loadImage(byte[] image) {
		CPU cpu = new CPU();
		PhysicalMemorySpace mem = cpu.getMemorySpace();

		/* A real Pi 2 has RAM from 0x0 up to the peripheral window at
		 * 0x3F000000. Map all of it lazily so both large kernel images and
		 * runtime writes far beyond the image are handled. */
		SparseRamRegion ram = new SparseRamRegion(Pi2MemoryMap.PERIPHERAL_BASE & 0xFFFFFFFFL);
		mem.unmapRegion(Pi2MemoryMap.RAM_BASE, findRam(mem));
		mem.mapRegion(Pi2MemoryMap.RAM_BASE, ram);
		/* Linux is linked with a 2G/2G split (PAGE_OFFSET 0x80000000): its
		 * virtual addresses map 1:1 onto physical RAM (virt = phys + PAGE_OFFSET).
		 * jCPU has no MMU, so alias the same backing region there to let the
		 * kernel execute after it switches to virtual addresses. Also cover the
		 * 3G/1G PAGE_OFFSET for kernels built with that configuration. */
		mem.mapRegion(PAGE_OFFSET, ram);
		mem.mapRegion(PAGE_OFFSET_3G, ram);
		/* With SCTLR.V set, exceptions vector through 0xFFFF0000. That page
		 * must be backed or the abort handler cannot be reached. */
		mem.mapRegion(HIGH_VECTOR_BASE, new ByteArrayRegion(HIGH_VECTOR_SIZE));
		ram.load(image);

		/* A Pi 2 kernel needs r0/r1/r2 boot parameters — in particular a DTB
		 * in r2 — or it fails before console initialisation. */
		Pi2BootParams.install(cpu);

		wireTimer(cpu);
		return cpu;
	}

	/**
	 * Loads {@code image} and wires an {@link EmmcController} backed by
	 * {@code sdPath} over the EMMC register range at
	 * {@link Pi2MemoryMap#EMMC_BASE}. The system timer is also wired at
	 * {@link Pi2MemoryMap#SYSTEM_TIMER_BASE}.
	 *
	 * @return a CPU with the image loaded and the EMMC controller registered
	 *         (not yet reset)
	 */
	public static CPU loadImageWithSd(byte[] image, String sdPath) throws IOException {
		CPU cpu = loadImage(image);
		SdCard card = new SdCard(sdPath);
		EmmcController ctl = new EmmcController(card, cpu);
		cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);
		return cpu;
	}

	private static void wireTimer(CPU cpu) {
		SystemTimer timer = new SystemTimer(cpu);
		cpu.setSystemTimer(timer);
		cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.SYSTEM_TIMER_BASE, 0x1000, timer);
	}

	private static MemoryRegion findRam(PhysicalMemorySpace mem) {
		for (PhysicalMemorySpace.MappedRegion mr : mem.getMappedRegions()) {
			if (mr.getBase() == Pi2MemoryMap.RAM_BASE) {
				return mr.getRegion();
			}
		}
		throw new IllegalStateException("no RAM region mapped at " + Integer.toHexString(Pi2MemoryMap.RAM_BASE));
	}

	public static void main(String[] args) throws Exception {
		/*if (args.length < 1) {
			System.err.println("Usage: Pi2Loader <kernel.img> [sd.img]");
			System.exit(1);
		}
		byte[] image = readImage(args[0]);
		CPU cpu = args.length >= 2 ? loadImageWithSd(image, args[1]) : loadImage(image);*/
                CPU cpu = loadImage(readImage("/Users/xuyi/Downloads/OVMF-X64-r15214/qemu-rpi-kernel-master/native-emuation/5.4.51/kernel7.img"));
		cpu.reset();
		cpu.go(0);
	}

	private static byte[] readImage(String path) {
		try {
			byte[] image = Files.readAllBytes(Paths.get(path));
			/* Diagnose the kernel image header: first instruction, magic and
			 * declared load/end offsets. Helps confirm a valid ARM image. */
			StringBuilder sb = new StringBuilder();
			sb.append("[JCPU-IMAGE] path=").append(path).append(" size=0x")
				.append(Integer.toHexString(image.length)).append(" words:");
			for(int i = 0; i < Math.min(16, image.length / 4); i++) {
				int o = i * 4;
				int w = (image[o] & 0xFF) | ((image[o+1] & 0xFF) << 8)
					| ((image[o+2] & 0xFF) << 16) | ((image[o+3] & 0xFF) << 24);
				sb.append(String.format(" 0x%08X", w));
			}
			System.err.println(sb);
			return image;
		} catch (IOException e) {
			System.err.println("Failed to read kernel image: " + e.getMessage());
			System.exit(1);
			return null; /* unreachable; satisfies definite assignment */
		}
	}
}
