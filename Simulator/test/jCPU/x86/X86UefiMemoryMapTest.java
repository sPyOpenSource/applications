package jCPU.x86;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class X86UefiMemoryMapTest
{
    private static final long MB = 1024 * 1024;

    @Test
    public void testSetSplitsAndMergesRanges()
    {
        X86UefiMemoryMap m = new X86UefiMemoryMap(16 * MB);
        m.set(MB, 2 * MB, X86UefiMemoryMap.LOADER_CODE);
        assertEquals(3, m.getRanges().size());
        assertEquals(X86UefiMemoryMap.LOADER_CODE, m.typeAt(MB + 5));
        assertEquals(X86UefiMemoryMap.CONVENTIONAL, m.typeAt(2 * MB));

        m.set(MB, 2 * MB, X86UefiMemoryMap.CONVENTIONAL);
        assertEquals(1, m.getRanges().size());
    }

    @Test
    public void testAllocateTakesHighestFreePagesBelowLimit()
    {
        X86UefiMemoryMap m = new X86UefiMemoryMap(16 * MB);
        m.set(15 * MB, 16 * MB, X86UefiMemoryMap.RESERVED);
        long p = m.allocate(2, X86UefiMemoryMap.LOADER_DATA, 4096, 16 * MB - 1);
        assertEquals(15 * MB - 2 * 4096, p);

        long q = m.allocate(1, X86UefiMemoryMap.LOADER_DATA, 4096, 4 * MB - 1);
        assertEquals(4 * MB - 4096, q);
        assertFalse(m.isFree(q, 1));
        assertTrue(m.isFree(q - 4096, 1));
    }

    @Test
    public void testAllocateFailsWhenNothingFits()
    {
        X86UefiMemoryMap m = new X86UefiMemoryMap(16 * MB);
        assertEquals(-1, m.allocate(4097, X86UefiMemoryMap.LOADER_DATA, 4096, 16 * MB - 1));
    }

    @Test
    public void testKeyChangesWithEveryChange()
    {
        X86UefiMemoryMap m = new X86UefiMemoryMap(16 * MB);
        int k = m.getKey();
        m.allocate(1, X86UefiMemoryMap.LOADER_DATA, 4096, 16 * MB - 1);
        assertNotEquals(k, m.getKey());
    }

    @Test
    public void testRangesCoverAllMemoryInOrder()
    {
        X86UefiMemoryMap m = new X86UefiMemoryMap(16 * MB);
        m.set(0, 4096, X86UefiMemoryMap.RESERVED);
        m.set(3 * MB, 5 * MB, X86UefiMemoryMap.BOOT_SERVICES_DATA);
        m.allocate(3, X86UefiMemoryMap.LOADER_DATA, 4096, 16 * MB - 1);
        List<X86UefiMemoryMap.Range> r = m.getRanges();
        assertEquals(0, r.get(0).start);
        for (int i = 1; i < r.size(); i++) {
            assertEquals(r.get(i - 1).end, r.get(i).start);
            assertNotEquals(r.get(i - 1).type, r.get(i).type);
        }
        assertEquals(16 * MB, r.get(r.size() - 1).end);
    }
}
