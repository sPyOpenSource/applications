package jCPU.x86;

/**
 * Minimal PC BIOS firmware for floppy disk boot.
 *
 * <p>The BIOS is the first code that runs when the x86 simulator boots from
 * floppy.  It performs the classic INT 0x19 boot sequence:</p>
 *
 * <ul>
 *   <li>Reads the boot sector (sector 0 / LBA 0) from the floppy drive</li>
 *   <li>Verifies the {@code 0x55AA} boot signature at offset 510-511</li>
 *   <li>Loads the 512-byte boot sector into memory at physical {@code 0x7C00}</li>
 *   <li>Sets {@code DL = 0x00} (boot drive = first floppy)</li>
 *   <li>Transfers control to {@code 0x0000:0x7C00}</li>
 * </ul>
 *
 * <p>The firmware also provides a subset of the standard BIOS interrupt
 * services used by boot-sector code:</p>
 *
 * <ul>
 *   <li>INT 0x10 - video: set mode, cursor, scroll, write char/attribute,
 *       teletype output, write string</li>
 *   <li>INT 0x11 / 0x12 - equipment list and conventional memory size</li>
 *   <li>INT 0x13 - disk: reset, read sectors, drive parameters, drive type</li>
 *   <li>INT 0x15 - AH=0x88 extended memory size</li>
 *   <li>INT 0x16 - keyboard: read/peek key, shift flags</li>
 *   <li>INT 0x19 - reboot from floppy</li>
 *   <li>INT 0x1A - AH=0x00 BIOS tick count</li>
 * </ul>
 *
 * <p>The emulator uses a flat memory model (segment registers are not
 * tracked), so segment:offset pairs such as INT 0x13 {@code ES:BX} or
 * INT 0x10 AH=0x13 {@code ES:BP} are treated as the linear offset.</p>
 */
public class X86BiosFirmware
{
    /** Physical load address of the boot sector (0x7C00). */
    public static final int BOOT_OFFSET = 0x7C00;

    /** Floppy drive number used for boot (first floppy). */
    public static final int DRIVE_FLOPPY_0 = 0x00;

    /** Standard 1.44 MB floppy geometry. */
    private static final int DEFAULT_SECTORS_PER_TRACK = 18;
    private static final int DEFAULT_HEADS = 2;

    /** VGA text mode geometry and memory base. */
    private static final int VGA_BASE = 0xB8000;
    private static final int WIDTH = 80;
    private static final int HEIGHT = 25;
    private static final int DEFAULT_ATTR = 0x07; // light grey on black

    private final X86Core core;
    private final X86FloppyDrive floppy;
    private final X86VgaPeripheral vga;
    private final X86UartPeripheral uart;

    private int vgaCursorX;
    private int vgaCursorY;

    /** Keyboard scan/ASCII codes queued for INT 0x16 (see {@link #pushKey(int)}). */
    private final java.util.ArrayDeque<Integer> keyQueue = new java.util.ArrayDeque<>();

    /** Reference time for INT 0x1A (BIOS tick count, 18.2 ticks/second). */
    private final long bootNanos = System.nanoTime();

    private final int sectorsPerTrack;
    private final int heads;

    public X86BiosFirmware(X86Core core, X86FloppyDrive floppy,
                           X86VgaPeripheral vga, X86UartPeripheral uart)
    {
        this.core = core;
        this.floppy = floppy;
        this.vga = vga;
        this.uart = uart;
        if (floppy != null && floppy.getSectorCount() > 0)
        {
            this.sectorsPerTrack = DEFAULT_SECTORS_PER_TRACK;
            this.heads = DEFAULT_HEADS;
        }
        else
        {
            this.sectorsPerTrack = 0;
            this.heads = 0;
        }
    }

    /**
     * Boot from floppy (BIOS INT 0x19).
     *
     * @return true if a bootable sector was found, loaded to {@code 0x7C00},
     *         and control transferred to {@code 0x0000:0x7C00}.
     */
    public boolean floppyBoot()
    {
        if (floppy == null)
        {
            System.out.println("BIOS: no floppy drive attached.");
            return false;
        }

        System.out.println("BIOS: floppy boot (drive 0x00)");

        byte[] boot = new byte[X86IdeDevice.SECTOR_SIZE];
        floppy.readSector(0, boot);

        // Check boot signature 0x55AA at offset 510-511
        if ((boot[510] & 0xFF) != 0x55 || (boot[511] & 0xFF) != 0xAA)
        {
            System.out.println("BIOS: no 0x55AA boot signature on floppy sector 0.");
            return false;
        }

        // Load boot sector to physical 0x7C00
        for (int i = 0; i < boot.length; i++)
        {
            core.writeMem8(BOOT_OFFSET + i, boot[i] & 0xFF);
        }

        // BIOS sets DL = boot drive (0x00 = first floppy) before jumping.
        core.setReg(X86Core.REG_EDX, DRIVE_FLOPPY_0);

        // A real reset leaves the segment registers zeroed and the CPU in
        // real mode; do the same so INT 0x19 reboots deterministically.
        for (int seg = 0; seg < X86Core.SEG_COUNT; seg++) {
            core.setSegSelector(seg, 0);
            core.setSegBase(seg, 0);
        }
        core.setProtectedMode(false);
        core.setCodeSize32(false);

        // Jump to 0x0000:0x7C00.
        core.setPC(BOOT_OFFSET);

        System.out.println("BIOS: boot sector loaded at 0x7C00, jumping to 0x0000:0x7C00.");
        return true;
    }

    /* ---------------- INT 0x13 disk services ---------------- */

    /**
     * Handle a BIOS interrupt call.  Parameters and results are passed through
     * CPU registers.
     *
     * <ul>
     *   <li>INT 0x10 - video services</li>
     *   <li>INT 0x11 - get equipment list</li>
     *   <li>INT 0x12 - get conventional memory size</li>
     *   <li>INT 0x13 - disk services</li>
     *   <li>INT 0x15 - system services (AH=0x88 extended memory size)</li>
     *   <li>INT 0x16 - keyboard services</li>
     *   <li>INT 0x19 - reboot from floppy</li>
     *   <li>INT 0x1A - time services (AH=0x00 tick count)</li>
     * </ul>
     *
     * @param vector the interrupt vector
     */
    public void handleInt(int vector)
    {
        switch (vector)
        {
            case 0x10:
                handleInt10();
                break;
            case 0x11:
                handleInt11();
                break;
            case 0x12:
                // AX = size of conventional memory in KB.
                core.setReg(X86Core.REG_EAX, 640);
                break;
            case 0x13:
                handleInt13();
                break;
            case 0x15:
                handleInt15();
                break;
            case 0x16:
                handleInt16();
                break;
            case 0x19:
                floppyBoot();
                break;
            case 0x1A:
                handleInt1A();
                break;
            default:
                System.out.println("BIOS: unsupported interrupt 0x" + Integer.toHexString(vector));
                break;
        }
    }

    /** Queue an ASCII/scan code so INT 0x16 can return it. */
    public void pushKey(int ascii)
    {
        synchronized (keyQueue)
        {
            if (keyQueue.size() < 256)
            {
                keyQueue.add(ascii & 0xFFFF);
            }
        }
    }

    /* ---------------- INT 0x11 / 0x12 / 0x15 ---------------- */

    /** INT 0x11: AX = equipment word (floppy installed, 80x25 colour). */
    private void handleInt11()
    {
        core.setReg(X86Core.REG_EAX, 0x0021);
        core.setFlag(X86Core.FLAG_CF, false);
    }

    /** INT 0x15 AH=0x88: AX = extended memory size in KB. */
    private void handleInt15()
    {
        int ah = (core.getReg(X86Core.REG_EAX) >> 8) & 0xFF;
        if (ah == 0x88)
        {
            core.setReg(X86Core.REG_EAX, 15 * 1024);
            core.setFlag(X86Core.FLAG_CF, false);
            return;
        }
        setAh(0x86); // unsupported function
        core.setFlag(X86Core.FLAG_CF, true);
    }

    /* ---------------- INT 0x16 keyboard services ---------------- */

    /**
     * Handle an INT 0x16 keyboard call.
     * <ul>
     *   <li>AH=0x00/0x10: read key (AX = ASCII + scan code)</li>
     *   <li>AH=0x01/0x11: peek key (ZF=0 and AX valid if a key is pending)</li>
     *   <li>AH=0x02/0x12: get shift flags (AL)</li>
     * </ul>
     * The simulator has no real keyboard controller, so a key is only
     * available after {@link #pushKey(int)} was called.  Reads never block:
     * when the queue is empty AX is zero and ZF is set.
     */
    private void handleInt16()
    {
        int ah = (core.getReg(X86Core.REG_EAX) >> 8) & 0xFF;

        switch (ah)
        {
            case 0x00:
            case 0x10:
            {
                int key = takeKey();
                if (key < 0)
                {
                    core.setReg(X86Core.REG_EAX, 0);
                    core.setFlag(X86Core.FLAG_ZF, true);
                }
                else
                {
                    core.setReg(X86Core.REG_EAX, key & 0xFFFF);
                    core.setFlag(X86Core.FLAG_ZF, false);
                }
                break;
            }

            case 0x01:
            case 0x11:
            {
                int key = peekKey();
                if (key < 0)
                {
                    core.setReg(X86Core.REG_EAX, 0);
                    core.setFlag(X86Core.FLAG_ZF, true);
                }
                else
                {
                    core.setReg(X86Core.REG_EAX, key & 0xFFFF);
                    core.setFlag(X86Core.FLAG_ZF, false);
                }
                break;
            }

            case 0x02:
            case 0x12:
                core.setReg(X86Core.REG_EAX, 0); // no shift keys held
                break;

            default:
                setAh(0x80);
                core.setFlag(X86Core.FLAG_CF, true);
                break;
        }
    }

    /** Return the next pending key without removing it, or -1 if none. */
    private int peekKey()
    {
        synchronized (keyQueue)
        {
            if (!keyQueue.isEmpty())
            {
                return keyQueue.peek();
            }
        }
        return uart == null ? -1 : uart.peekInput();
    }

    /** Remove and return the next pending key, or -1 if none. */
    private int takeKey()
    {
        synchronized (keyQueue)
        {
            if (!keyQueue.isEmpty())
            {
                return keyQueue.poll();
            }
        }
        return uart == null ? -1 : uart.pollInput();
    }

    /* ---------------- INT 0x1A time services ---------------- */

    /**
     * INT 0x1A AH=0x00: CX:DX = BIOS tick count since midnight
     * (18.2 ticks per second), AL = midnight flag.
     */
    private void handleInt1A()
    {
        int ah = (core.getReg(X86Core.REG_EAX) >> 8) & 0xFF;
        if (ah != 0x00)
        {
            setAh(0x80);
            core.setFlag(X86Core.FLAG_CF, true);
            return;
        }
        long millis = (System.nanoTime() - bootNanos) / 1_000_000L;
        long ticks = (millis * 182L) / 10_000L;
        core.setReg(X86Core.REG_ECX, (int) ((ticks >> 16) & 0xFFFF));
        core.setReg(X86Core.REG_EDX, (int) (ticks & 0xFFFF));
        core.setReg(X86Core.REG_EAX, 0);
        core.setFlag(X86Core.FLAG_CF, false);
    }

    /* ---------------- INT 0x10 video services ---------------- */

    /**
     * Handle an INT 0x10 BIOS video call.
     * <ul>
     *   <li>AH=0x00: set video mode (AL = mode) - no-op, returns success</li>
     *   <li>AH=0x01: set cursor shape (CX) - accepted, not rendered</li>
     *   <li>AH=0x02: set cursor position (DH = row, DL = col, BH = page)</li>
     *   <li>AH=0x03: get cursor position (DH = row, DL = col, CX = shape)</li>
     *   <li>AH=0x05: set active display page (accepted, single page)</li>
     *   <li>AH=0x06: scroll window up (AL = lines, 0 = clear, BH = attribute)</li>
     *   <li>AH=0x07: scroll window down (AL = lines, 0 = clear, BH = attribute)</li>
     *   <li>AH=0x08: read character+attribute at cursor</li>
     *   <li>AH=0x09: write character+attribute at cursor (AL=char, BL=attr, CX=count)</li>
     *   <li>AH=0x0A: write character only at cursor (AL=char, CX=count)</li>
     *   <li>AH=0x0E: teletype output (AL = char, BH = page, BL = colour)</li>
     *   <li>AH=0x0F: get video mode (AL = mode, AH = columns, BH = page)</li>
     *   <li>AH=0x13: write string (ES:BP = string, CX = length, DH/DL = row/col)</li>
     * </ul>
     */
    private void handleInt10()
    {
        int ah = (core.getReg(X86Core.REG_EAX) >> 8) & 0xFF;
        int al = core.getReg(X86Core.REG_EAX) & 0xFF;
        int bh = (core.getReg(X86Core.REG_EBX) >> 8) & 0xFF;
        int dh = (core.getReg(X86Core.REG_EDX) >> 8) & 0xFF;
        int dl = core.getReg(X86Core.REG_EDX) & 0xFF;
        int ch = (core.getReg(X86Core.REG_ECX) >> 8) & 0xFF;
        int cl = core.getReg(X86Core.REG_ECX) & 0xFF;

        switch (ah)
        {
            case 0x00: // set video mode
                System.out.println("BIOS INT10: set video mode 0x" + Integer.toHexString(al));
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x01: // set cursor shape - nothing to render in this emulator
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x02: // set cursor position
                vgaSetCursor(dh, dl);
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x03: // get cursor position
                core.setReg(X86Core.REG_EDX,
                        (core.getReg(X86Core.REG_EDX) & 0xFFFF0000) | (vgaCursorY << 8) | vgaCursorX);
                core.setReg(X86Core.REG_ECX, (0x0607 << 16) | (core.getReg(X86Core.REG_ECX) & 0xFFFF));
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x05: // set active page - single page only
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x06: // scroll window up
                vgaScroll(true, al, bh, ch, cl, dh, dl);
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x07: // scroll window down
                vgaScroll(false, al, bh, ch, cl, dh, dl);
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x08: // read character and attribute at cursor
            {
                int base = (vgaCursorY * WIDTH + vgaCursorX) * 2;
                int glyph = videoRead(VGA_BASE + base);
                int attr = videoRead(VGA_BASE + base + 1);
                core.setReg(X86Core.REG_EAX, (attr << 8) | glyph);
                core.setFlag(X86Core.FLAG_CF, false);
                break;
            }

            case 0x0E: // teletype output
                vgaWriteChar((char) al);
                if (uart != null)
                {
                    uart.write(0x3F8, (byte) al);
                }
                if (vga != null)
                {
                    vga.dumpText();
                }
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x09: // write character and attribute at cursor (CX times)
            {
                int bl = core.getReg(X86Core.REG_EBX) & 0xFF;   // attribute
                int cx = core.getReg(X86Core.REG_ECX) & 0xFFFF; // repeat count
                writeCharAtCursor(al, bl, cx);
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;
            }

            case 0x0A: // write character only at cursor (CX times)
            {
                int cx = core.getReg(X86Core.REG_ECX) & 0xFFFF; // repeat count
                writeCharAtCursor(al, attrAtCursor(), cx);
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;
            }

            case 0x0F: // get video mode
                core.setReg(X86Core.REG_EAX, (WIDTH << 8) | (core.getReg(X86Core.REG_EAX) & 0xFF));
                core.setReg(X86Core.REG_EBX, (core.getReg(X86Core.REG_EBX) & 0xFF00FFFF) | (bh << 24));
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x13: // write string (BIOS extension)
                writeString(al, (core.getSegBase(X86Core.SEG_ES)
                                + (core.getReg(X86Core.REG_EBP) & 0xFFFF)) & 0xFFFFFF,
                        core.getReg(X86Core.REG_ECX) & 0xFFFF,
                        dh, dl, core.getReg(X86Core.REG_EBX) & 0xFF);
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            default:
                System.out.println("BIOS INT10: unsupported function AH=0x" + Integer.toHexString(ah));
                setAh(0x01);
                core.setFlag(X86Core.FLAG_CF, true);
                break;
        }
    }

    private void vgaSetCursor(int row, int col)
    {
        vgaCursorX = col;
        vgaCursorY = row;
        if (vgaCursorY >= HEIGHT) vgaCursorY = HEIGHT - 1;
        if (vgaCursorX >= WIDTH) vgaCursorX = WIDTH - 1;
    }

    /**
     * Write a character with attribute at the current VGA cursor position,
     * repeated {@code count} times, without advancing the cursor.
     */
    private void writeCharAtCursor(int ch, int attr, int count)
    {
        int base = (vgaCursorY * WIDTH + vgaCursorX) * 2;
        for (int i = 0; i < count; i++)
        {
            int addr = VGA_BASE + base + i * 2;
            videoWrite(addr, ch & 0xFF);
            videoWrite(addr + 1, attr & 0xFF);
        }
        if (vga != null) vga.dumpText();
    }

    /**
     * INT 0x10 AH=0x13: write a string.
     *
     * @param mode  AL - bit 0: update cursor, bit 1: string holds char/attribute pairs
     * @param src   ES:BP (flat: BP) string address
     * @param len   CX - string length
     * @param row   DH - starting row
     * @param col   DL - starting column
     * @param attr  BL - attribute used when the string has no attribute pairs
     */
    private void writeString(int mode, int src, int len, int row, int col, int attr)
    {
        boolean usePairs = (mode & 0x02) != 0;
        boolean updateCursor = (mode & 0x01) != 0;

        vgaSetCursor(row, col);
        int srcPos = src;
        for (int i = 0; i < len; i++)
        {
            int ch = core.readMem8(srcPos++) & 0xFF;
            int a = usePairs ? (core.readMem8(srcPos++) & 0xFF) : attr;
            if (ch == '\r') { vgaCursorX = 0; continue; }
            if (ch == '\n') { newline(); continue; }
            int addr = VGA_BASE + (vgaCursorY * WIDTH + vgaCursorX) * 2;
            videoWrite(addr, ch);
            videoWrite(addr + 1, a & 0xFF);
            if (++vgaCursorX >= WIDTH) newline();
        }
        if (!updateCursor)
        {
            vgaSetCursor(row, col);
        }
        if (vga != null) vga.dumpText();
    }

    /** Scroll the rectangle (CH,CL)-(DH,DL) up or down by AL lines (0 = clear). */
    private void vgaScroll(boolean up, int lines, int attr, int top, int left, int bottom, int right)
    {
        if (top > bottom) { int t = top; top = bottom; bottom = t; }
        if (left > right) { int t = left; left = right; right = t; }
        if (top >= HEIGHT) top = HEIGHT - 1;
        if (bottom >= HEIGHT) bottom = HEIGHT - 1;
        if (left >= WIDTH) left = WIDTH - 1;
        if (right >= WIDTH) right = WIDTH - 1;

        int height = bottom - top + 1;
        int width = right - left + 1;
        int n = Math.min(lines, height);

        for (int k = 0; k < height; k++)
        {
            // Scroll up copies from the bottom upwards; scroll down copies from
            // the top downwards, so a line is never overwritten before it is read.
            int y = up ? k : height - 1 - k;
            int srcRow = up ? top + y + n : top + y - n;
            for (int x = 0; x < width; x++)
            {
                int dst = VGA_BASE + ((top + y) * WIDTH + left + x) * 2;
                if (srcRow >= top && srcRow <= bottom)
                {
                    int srcAddr = VGA_BASE + (srcRow * WIDTH + left + x) * 2;
                    videoWrite(dst, videoRead(srcAddr));
                    videoWrite(dst + 1, videoRead(srcAddr + 1));
                }
                else
                {
                    videoWrite(dst, ' ');
                    videoWrite(dst + 1, attr & 0xFF);
                }
            }
        }
        if (vga != null) vga.dumpText();
    }

    /** Returns the full attribute byte at the current cursor position. */
    private int attrAtCursor()
    {
        int base = (vgaCursorY * WIDTH + vgaCursorX) * 2;
        return videoRead(VGA_BASE + base + 1);
    }

    /** Read a byte of VGA text memory from emulated RAM. */
    private int videoRead(int addr)
    {
        return core.readMem8(addr) & 0xFF;
    }

    /**
     * Write a byte of VGA text memory.  The write goes through {@link X86Core}
     * so emulated RAM stays in sync; the VGA peripheral is updated explicitly
     * as well so the display refreshes even when no memory hook is installed.
     */
    private void videoWrite(int addr, int value)
    {
        core.writeMem8(addr, value & 0xFF);
        if (vga != null)
        {
            vga.writeMemory(addr, value & 0xFF);
        }
    }

    private void newline()
    {
        vgaCursorX = 0;
        if (++vgaCursorY >= HEIGHT)
        {
            vgaCursorY = HEIGHT - 1;
            vgaScroll(true, 1, 0x07, 0, 0, HEIGHT - 1, WIDTH - 1);
        }
    }

    /* ---------------- INT 0x13 disk services ---------------- */

    /**
     * Handle an INT 0x13 BIOS disk call.  Parameters and results are passed
     * through CPU registers:
     * <ul>
     *   <li>AH = function, AL = sector count (read)</li>
     *   <li>CH = cylinder low, CL = sector number | (cylinder high &lt;&lt; 6)</li>
     *   <li>DH = head, DL = drive</li>
     *   <li>ES:BX = buffer address (segment ignored, linear address = BX)</li>
     * </ul>
     * On return CF is set on error and AH holds the status code.
     */
    public void handleInt13()
    {
        int ah = (core.getReg(X86Core.REG_EAX) >> 8) & 0xFF;
        int al = core.getReg(X86Core.REG_EAX) & 0xFF;
        int ch = (core.getReg(X86Core.REG_ECX) >> 8) & 0xFF;
        int cl = core.getReg(X86Core.REG_ECX) & 0xFF;
        int dh = (core.getReg(X86Core.REG_EDX) >> 8) & 0xFF;
        int dl = core.getReg(X86Core.REG_EDX) & 0xFF;
        int bx = core.getReg(X86Core.REG_EBX) & 0xFFFF;

        switch (ah)
        {
            case 0x00: // reset disk system
                System.out.println("BIOS INT13: reset drive 0x" + Integer.toHexString(dl));
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x02: // read sectors (CHS)
                handleReadSectors(al, ch, cl, dh, dl, bx);
                break;

            case 0x41: // check extensions present
                // BX should be 0xAA55 on input; return BX=0xAA55 on success
                core.setReg(X86Core.REG_EBX, 0xAA55);
                setAh(0x00);
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            case 0x42: // extended read sectors (LBA)
                handleExtendedReadSectors(dl);
                break;

            case 0x48: // extended drive parameters
                handleExtendedDriveParams(dl);
                break;

            case 0x08: // get drive parameters
                handleDriveParams(dl);
                break;

            case 0x15: // get drive type
                System.out.println("BIOS INT13: drive type for 0x" + Integer.toHexString(dl));
                setAh(0x02); // floppy
                core.setFlag(X86Core.FLAG_CF, false);
                break;

            default:
                System.out.println("BIOS INT13: unsupported function AH=0x" + Integer.toHexString(ah));
                setAh(0x01); // invalid command
                core.setFlag(X86Core.FLAG_CF, true);
                break;
        }
    }

    private void handleReadSectors(int sectorCount, int ch, int cl, int dh, int dl, int bx)
    {
        if (floppy == null || sectorsPerTrack == 0)
        {
            setAh(0x01);
            core.setFlag(X86Core.FLAG_CF, true);
            return;
        }
        if (sectorCount == 0)
        {
            // A count of 0 means 256 sectors on real hardware; treat as 0 here.
            setAh(0x00);
            core.setFlag(X86Core.FLAG_CF, false);
            return;
        }

        int sector = cl & 0x3F;
        int cylinder = ch | ((cl & 0xC0) << 2);
        int head = dh;

        if (sector == 0 || sector > sectorsPerTrack)
        {
            setAh(0x09); // attempt to DMA across 64K boundary / bad sector
            core.setFlag(X86Core.FLAG_CF, true);
            return;
        }

        int lba = ((cylinder * heads + head) * sectorsPerTrack) + (sector - 1);
        int dest = (core.getSegBase(X86Core.SEG_ES) + (bx & 0xFFFF)) & 0xFFFFFF;

        for (int i = 0; i < sectorCount; i++)
        {
            int targetLba = lba + i;
            if (targetLba >= floppy.getSectorCount())
            {
                setAh(0x09);
                core.setFlag(X86Core.FLAG_CF, true);
                return;
            }
            byte[] buf = new byte[X86IdeDevice.SECTOR_SIZE];
            floppy.readSector(targetLba, buf);
            int base = dest + i * X86IdeDevice.SECTOR_SIZE;
            for (int j = 0; j < buf.length; j++)
            {
                core.writeMem8(base + j, buf[j]);
            }
        }

        setAh(0x00);
        core.setFlag(X86Core.FLAG_CF, false);
        System.out.println("BIOS INT13: read " + sectorCount + " sector(s) from LBA "
                + lba + " to 0x" + Integer.toHexString(dest));
    }

    /**
     * INT 0x13 AH=0x42: Extended Read Sectors (LBA).
     * <p>DS:SI points to a Disk Address Packet (DAP):
     * <ul>
     *   <li>[0]   Size of packet (0x10 = 16)
     *   <li>[1]   Reserved (0x00)
     *   <li>[2-3] Number of sectors to transfer (16-bit)
     *   <li>[4-5] Buffer offset (16-bit)
     *   <li>[6-7] Buffer segment (16-bit)
     *   <li>[8-15] LBA start address (64-bit, little-endian)
     * </ul>
     */
    private void handleExtendedReadSectors(int dl)
    {
        if (floppy == null || sectorsPerTrack == 0)
        {
            setAh(0x01);
            core.setFlag(X86Core.FLAG_CF, true);
            return;
        }

        int si = core.getReg(X86Core.REG_ESI) & 0xFFFF;
        int dsBase = core.getSegBase(X86Core.SEG_DS);
        int dapAddr = (dsBase + si) & 0xFFFFFF;

        int dapSize = core.readMem8(dapAddr) & 0xFF;
        if (dapSize < 0x10)
        {
            setAh(0x01);
            core.setFlag(X86Core.FLAG_CF, true);
            return;
        }

        int sectorCount = core.readMem16(dapAddr + 2) & 0xFFFF;
        int bufOff = core.readMem16(dapAddr + 4) & 0xFFFF;
        int bufSeg = core.readMem16(dapAddr + 6) & 0xFFFF;
        long lba = core.readMem32(dapAddr + 8) & 0xFFFFFFFFL;
        // Read high 32 bits of LBA (usually 0 for floppy)
        int lbaHi = core.readMem32(dapAddr + 12);

        if (sectorCount == 0)
        {
            setAh(0x00);
            core.setFlag(X86Core.FLAG_CF, false);
            return;
        }

        int bufOffBase = bufOff;
        // Extended read uses the buffer segment from the DAP, not ES.
        // In real mode, linear address = segment * 16 + offset.
        int dest = (bufSeg * 16 + bufOffBase) & 0xFFFFFF;

        for (int i = 0; i < sectorCount; i++)
        {
            long targetLba = lba + i;
            if (targetLba >= floppy.getSectorCount())
            {
                setAh(0x09);
                core.setFlag(X86Core.FLAG_CF, true);
                return;
            }
            byte[] buf = new byte[X86IdeDevice.SECTOR_SIZE];
            floppy.readSector((int) targetLba, buf);
            int base = dest + i * X86IdeDevice.SECTOR_SIZE;
            for (int j = 0; j < buf.length; j++)
            {
                core.writeMem8(base + j, buf[j]);
            }
        }

        setAh(0x00);
        core.setFlag(X86Core.FLAG_CF, false);
        System.out.println("BIOS INT13: extended read " + sectorCount + " sector(s) from LBA "
                + lba + " to 0x" + Integer.toHexString(dest));
    }

    /**
     * INT 0x13 AH=0x48: Extended Drive Parameters.
     * <p>DS:SI points to a buffer that is filled with drive information:
     * <ul>
     *   <li>[0-1]   Buffer size (including this field)
     *   <li>[2-3]   Total cylinders - 1
     *   <li>[4-5]   Total heads
     *   <li>[6-7]   Sectors per track
     *   <li>[8-11]  Total sectors (low 32 bits)
     *   <li>[12-15] Total sectors high 32 bits
     * </ul>
     */
    private void handleExtendedDriveParams(int dl)
    {
        if (floppy == null || sectorsPerTrack == 0)
        {
            setAh(0x01);
            core.setFlag(X86Core.FLAG_CF, true);
            return;
        }

        int si = core.getReg(X86Core.REG_ESI) & 0xFFFF;
        int dsBase = core.getSegBase(X86Core.SEG_DS);
        int bufAddr = (dsBase + si) & 0xFFFFFF;

        int totalSectors = floppy.getSectorCount();
        int maxCylinder = 80 - 1;
        int maxHead = heads - 1;

        // Buffer size
        core.writeMem16(bufAddr, 0x0010);
        // Total cylinders - 1
        core.writeMem16(bufAddr + 2, maxCylinder);
        // Total heads
        core.writeMem16(bufAddr + 4, heads);
        // Sectors per track
        core.writeMem16(bufAddr + 6, sectorsPerTrack);
        // Total sectors (low)
        core.writeMem32(bufAddr + 8, totalSectors);
        // Total sectors high
        core.writeMem32(bufAddr + 12, 0);

        setAh(0x00);
        core.setFlag(X86Core.FLAG_CF, false);
        System.out.println("BIOS INT13: extended drive params cylinders=" + maxCylinder
                + " heads=" + heads + " sectors/track=" + sectorsPerTrack
                + " total sectors=" + totalSectors);
    }

    private void handleDriveParams(int dl)
    {
        if (floppy == null)
        {
            setAh(0x01);
            core.setFlag(X86Core.FLAG_CF, true);
            return;
        }
        // Max CHS for a 1.44 MB floppy: 80 cylinders, 2 heads, 18 sectors/track.
        int maxCylinder = 80 - 1;
        int maxSector = sectorsPerTrack;
        int maxHead = heads - 1;
        // CH = max cylinder low
        // CL = max sector | (max cylinder high << 6)
        int cl = maxSector | (((maxCylinder >> 2) & 0x03) << 6);
        core.setReg(X86Core.REG_ECX, (core.getReg(X86Core.REG_ECX) & 0xFFFF00FF) | (maxCylinder << 8) | cl);
        core.setReg(X86Core.REG_EDX, (core.getReg(X86Core.REG_EDX) & 0x0000FFFF) | (dl << 16) | (maxHead << 8));
        setAh(0x00);
        core.setFlag(X86Core.FLAG_CF, false);
        System.out.println("BIOS INT13: drive params cylinders=" + maxCylinder
                + " heads=" + maxHead + " sectors/track=" + maxSector);
    }

    private void setAh(int value)
    {
        core.setReg(X86Core.REG_EAX,
                (core.getReg(X86Core.REG_EAX) & 0x00FF) | (value << 8));
    }

    private void vgaWriteChar(char c)
    {
        if (c == '\n')
        {
            newline();
            return;
        }
        if (c == '\r')
        {
            vgaCursorX = 0;
            return;
        }
        if (c == '\b')
        {
            if (vgaCursorX > 0) vgaCursorX--;
            return;
        }
        if (c == '\t')
        {
            int next = (vgaCursorX + 8) & ~7;
            while (vgaCursorX < next && vgaCursorX < WIDTH)
            {
                int addr = VGA_BASE + (vgaCursorY * WIDTH + vgaCursorX) * 2;
                videoWrite(addr, ' ');
                videoWrite(addr + 1, DEFAULT_ATTR);
                vgaCursorX++;
            }
            return;
        }

        int addr = VGA_BASE + (vgaCursorY * WIDTH + vgaCursorX) * 2;
        videoWrite(addr, c & 0xFF);
        videoWrite(addr + 1, DEFAULT_ATTR);
        if (++vgaCursorX >= WIDTH)
        {
            newline();
        }
    }
}
