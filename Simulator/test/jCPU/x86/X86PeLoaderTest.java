package jCPU.x86;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Header fields and base relocations of a small PE32+ image built here.
 */
public class X86PeLoaderTest
{
    private static final long BASE = 0x140000000L;

    private static void put(byte[] b, int off, long v, int size)
    {
        for (int i = 0; i < size; i++) {
            b[off + i] = (byte) (v >>> (8 * i));
        }
    }

    private static long get(byte[] b, int off, int size)
    {
        long v = 0;
        for (int i = size - 1; i >= 0; i--) {
            v = (v << 8) | (b[off + i] & 0xFF);
        }
        return v;
    }

    /**
     * Headers in the first 0x200 bytes, .text (RVA 0x1000, file 0x200) holding
     * an absolute pointer to itself + 8, .reloc (RVA 0x2000, file 0x400) with
     * one DIR64 entry for it.
     */
    private static byte[] image()
    {
        byte[] f = new byte[0x600];
        f[0] = 'M';
        f[1] = 'Z';
        put(f, 0x3C, 0x40, 4);
        f[0x40] = 'P';
        f[0x41] = 'E';
        int coff = 0x44;
        put(f, coff, 0x8664, 2);            // machine
        put(f, coff + 2, 2, 2);             // sections
        put(f, coff + 16, 240, 2);          // optional header size
        int opt = coff + 20;
        put(f, opt, 0x20B, 2);              // PE32+
        put(f, opt + 16, 0x1000, 4);        // entry point
        put(f, opt + 24, BASE, 8);          // image base
        put(f, opt + 32, 0x1000, 4);        // section alignment
        put(f, opt + 36, 0x200, 4);         // file alignment
        put(f, opt + 56, 0x3000, 4);        // size of image
        put(f, opt + 60, 0x200, 4);         // size of headers
        put(f, opt + 108, 16, 4);           // data directories
        put(f, opt + 112 + 5 * 8, 0x2000, 4);
        put(f, opt + 112 + 5 * 8 + 4, 12, 4);
        int sec = opt + 240;
        System.arraycopy(".text".getBytes(), 0, f, sec, 5);
        put(f, sec + 8, 0x10, 4);
        put(f, sec + 12, 0x1000, 4);
        put(f, sec + 16, 0x200, 4);
        put(f, sec + 20, 0x200, 4);
        sec += 40;
        System.arraycopy(".reloc".getBytes(), 0, f, sec, 6);
        put(f, sec + 8, 12, 4);
        put(f, sec + 12, 0x2000, 4);
        put(f, sec + 16, 0x200, 4);
        put(f, sec + 20, 0x400, 4);

        put(f, 0x200, BASE + 0x1008, 8);    // the pointer
        put(f, 0x208, 0x1122334455667788L, 8);
        put(f, 0x400, 0x1000, 4);           // page RVA
        put(f, 0x404, 12, 4);               // block size
        put(f, 0x408, 0xA000, 2);           // DIR64 at offset 0
        put(f, 0x40A, 0x0000, 2);           // padding
        return f;
    }

    @Test
    public void testReadsPe32PlusHeaderFields()
    {
        X86PeLoader pe = new X86PeLoader(image());

        assertTrue(pe.pe32Plus);
        assertEquals(BASE, pe.imageBase);
        assertEquals(BASE + 0x1000, pe.getEntryPoint64());
        assertEquals(0x1000, pe.sectionAlignment);
        assertEquals(0x200, pe.fileAlignment);
        assertEquals(0x3000, pe.sizeOfImage);
        assertEquals(0x200, pe.sizeOfHeaders);
        assertEquals(0x2000, pe.relocRva);
        assertEquals(12, pe.relocSize);
        assertEquals(2, pe.getSections().length);
    }

    @Test
    public void testLayoutAtImageBaseKeepsPointers()
    {
        byte[] mem = new X86PeLoader(image()).layout(image(), BASE);

        assertEquals(0x3000, mem.length);
        assertEquals('M', mem[0]);
        assertEquals(BASE + 0x1008, get(mem, 0x1000, 8));
        assertEquals(0x1122334455667788L, get(mem, 0x1008, 8));
    }

    @Test
    public void testLayoutElsewhereAppliesRelocations()
    {
        byte[] mem = new X86PeLoader(image()).layout(image(), 0x1000000L);

        assertEquals(0x1001008L, get(mem, 0x1000, 8));
        assertEquals(0x1122334455667788L, get(mem, 0x1008, 8));
    }

    @Test
    public void testImageWithStrippedRelocationsCannotMove()
    {
        byte[] f = image();
        put(f, 0x44 + 18, 0x0023, 2);       // RELOCS_STRIPPED | EXECUTABLE | LARGE_ADDRESS_AWARE
        X86PeLoader pe = new X86PeLoader(f);

        assertTrue(pe.relocsStripped);
        assertNull(pe.layout(f, 0x1000000L));
        assertNotNull(pe.layout(f, BASE));
    }

    @Test
    public void testImageWithoutRelocationEntriesCanMove()
    {
        // Position-independent code has nothing to relocate and no .reloc data.
        byte[] f = image();
        put(f, 0x44 + 20 + 112 + 5 * 8 + 4, 0, 4);
        put(f, 0x200, 0, 8);
        byte[] mem = new X86PeLoader(f).layout(f, 0x1000000L);

        assertNotNull(mem);
        assertEquals(0x1122334455667788L, get(mem, 0x1008, 8));
    }
}
