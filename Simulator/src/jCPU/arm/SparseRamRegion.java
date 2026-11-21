package jCPU.arm;

import java.util.HashMap;

/**
 * RAM region that covers a large address range without allocating it all up
 * front. 4 KB pages are created on the first write; reads from untouched pages
 * return zero, just like real RAM after reset.
 */
public final class SparseRamRegion extends ByteBackedRegion {
	private static final int PAGE_SHIFT = 12;
	private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
	private static final int PAGE_MASK = PAGE_SIZE - 1;

	private final long size;
	private final HashMap<Integer, byte[]> pages = new HashMap<>();
	private int cachedPage = -1;
	private byte[] cachedPageBytes;

	public SparseRamRegion(long size) {
		this.size = size;
	}

	@Override
	public long getRegionSize() {
		return size;
	}

	private byte[] getPage(int address, boolean allocate) {
		int index = address >>> PAGE_SHIFT;
		if(index == cachedPage) return cachedPageBytes;
		byte[] bytes = pages.get(index);
		if(bytes == null && allocate) {
			bytes = new byte[PAGE_SIZE];
			pages.put(index, bytes);
		}
		cachedPage = index;
		cachedPageBytes = bytes;
		return bytes;
	}

	@Override
	public byte backingReadByte(int address) {
		byte[] bytes = getPage(address, false);
		return bytes == null ? 0 : bytes[address & PAGE_MASK];
	}

	@Override
	public void backingWriteByte(int address, byte value) {
		getPage(address, true)[address & PAGE_MASK] = value;
	}

	/** Copies {@code image} to the start of this region. */
	public void load(byte[] image) {
		int offset = 0;
		while(offset < image.length) {
			int inPage = offset & PAGE_MASK;
			int count = Math.min(PAGE_SIZE - inPage, image.length - offset);
			System.arraycopy(image, offset, getPage(offset, true), inPage, count);
			offset += count;
		}
		cachedPage = -1;
		cachedPageBytes = null;
	}
}
