package jCPU.arm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Loads a flat Raspberry Pi 2 {@code kernel.img} into simulated RAM at
 * {@link Pi2MemoryMap#RAM_BASE} (0x00000000) and returns a CPU ready to run it.
 * <p>
 * The {@link CPU} constructor already maps 1 MB of RAM at {@code RAM_BASE};
 * {@link #loadImage(byte[])} copies the image bytes into that region.
 */
public final class Pi2Loader {

	private Pi2Loader() {}

	/**
	 * Maps {@code image} bytes into RAM at {@link Pi2MemoryMap#RAM_BASE}.
	 *
	 * @return a CPU with the image loaded at address 0 (not yet reset)
	 */
	public static CPU loadImage(byte[] image) {
		CPU cpu = new CPU();
		ByteArrayRegion ram = findRam(cpu.getMemorySpace());
		byte[] backing = ram.getBackingArray();
		System.arraycopy(image, 0, backing, 0, Math.min(image.length, backing.length));
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

	private static ByteArrayRegion findRam(PhysicalMemorySpace mem) {
		for (PhysicalMemorySpace.MappedRegion mr : mem.getMappedRegions()) {
			if (mr.getBase() == Pi2MemoryMap.RAM_BASE) {
				return (ByteArrayRegion) mr.getRegion();
			}
		}
		throw new IllegalStateException("no RAM region mapped at " + Integer.toHexString(Pi2MemoryMap.RAM_BASE));
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: Pi2Loader <kernel.img> [sd.img]");
			System.exit(1);
		}
		byte[] image = readImage(args[0]);
		CPU cpu = args.length >= 2 ? loadImageWithSd(image, args[1]) : loadImage(image);
		cpu.reset();
		cpu.go(0);
	}

	private static byte[] readImage(String path) {
		try {
			return Files.readAllBytes(Paths.get(path));
		} catch (IOException e) {
			System.err.println("Failed to read kernel image: " + e.getMessage());
			System.exit(1);
			return null; /* unreachable; satisfies definite assignment */
		}
	}
}
