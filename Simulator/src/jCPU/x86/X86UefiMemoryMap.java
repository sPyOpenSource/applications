package jCPU.x86;

import java.util.ArrayList;
import java.util.List;

/**
 * The UEFI memory map: every 4 KB page of memory has one EFI_MEMORY_TYPE.
 * Page allocations take free ({@link #CONVENTIONAL}) pages and give them a
 * type; the map is what {@code GetMemoryMap} reports.
 */
public class X86UefiMemoryMap
{
    public static final int RESERVED = 0;
    public static final int LOADER_CODE = 1;
    public static final int LOADER_DATA = 2;
    public static final int BOOT_SERVICES_CODE = 3;
    public static final int BOOT_SERVICES_DATA = 4;
    public static final int RUNTIME_SERVICES_CODE = 5;
    public static final int RUNTIME_SERVICES_DATA = 6;
    public static final int CONVENTIONAL = 7;

    public static final long PAGE = 4096;

    /** A run of pages with one type; start and end are page aligned, end exclusive. */
    public static final class Range
    {
        public final long start;
        public final long end;
        public final int type;

        Range(long start, long end, int type)
        {
            this.start = start;
            this.end = end;
            this.type = type;
        }
    }

    /** Sorted, adjacent, covering [0, size); neighbours never have the same type. */
    private final List<Range> ranges = new ArrayList<>();
    private int changes;

    public X86UefiMemoryMap(long size)
    {
        ranges.add(new Range(0, size & -PAGE, CONVENTIONAL));
    }

    public List<Range> getRanges()
    {
        return ranges;
    }

    /** Changes with every allocation and free; the MapKey of GetMemoryMap. */
    public int getKey()
    {
        return changes;
    }

    /** Give [start, end) the type; both are rounded out to whole pages. */
    public void set(long start, long end, int type)
    {
        start &= -PAGE;
        end = (end + PAGE - 1) & -PAGE;
        List<Range> out = new ArrayList<>();
        for (Range r : ranges) {
            if (r.end <= start || r.start >= end) {
                out.add(r);
                continue;
            }
            if (r.start < start) {
                out.add(new Range(r.start, start, r.type));
            }
            if (r.end > end) {
                out.add(new Range(end, r.end, r.type));
            }
        }
        out.add(new Range(start, end, type));
        out.sort((a, b) -> Long.compare(a.start, b.start));
        ranges.clear();
        for (Range r : out) {
            Range last = ranges.isEmpty() ? null : ranges.get(ranges.size() - 1);
            if (last != null && last.type == r.type && last.end == r.start) {
                ranges.set(ranges.size() - 1, new Range(last.start, r.end, r.type));
            } else {
                ranges.add(r);
            }
        }
        changes++;
    }

    /** True when every page of [start, start + pages) is free. */
    public boolean isFree(long start, long pages)
    {
        long end = start + pages * PAGE;
        for (Range r : ranges) {
            if (r.end > start && r.start < end && r.type != CONVENTIONAL) {
                return false;
            }
            if (r.start <= start && r.end >= end) {
                return r.type == CONVENTIONAL;
            }
        }
        return false;
    }

    /**
     * Allocate the highest free run of {@code pages} pages that ends at or
     * below {@code maxAddress} + 1 and starts at or above {@code minAddress}.
     *
     * @return its address, or -1 when there is none
     */
    public long allocate(long pages, int type, long minAddress, long maxAddress)
    {
        long size = pages * PAGE;
        for (int i = ranges.size() - 1; i >= 0; i--) {
            Range r = ranges.get(i);
            if (r.type != CONVENTIONAL) {
                continue;
            }
            long end = Math.min(r.end, (maxAddress + 1) & -PAGE);
            long start = end - size;
            if (pages > 0 && start >= r.start && start >= minAddress) {
                set(start, end, type);
                return start;
            }
        }
        return -1;
    }

    /** The type of the page holding {@code address}, or -1 outside memory. */
    public int typeAt(long address)
    {
        for (Range r : ranges) {
            if (address >= r.start && address < r.end) {
                return r.type;
            }
        }
        return -1;
    }
}
