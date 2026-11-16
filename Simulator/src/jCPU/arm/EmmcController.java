package jCPU.arm;

import jCPU.Peripheral;

public class EmmcController implements Peripheral, Comparable<EmmcController> {
    /**
     * CPU.setIRQ/clearIRQ keep their sources in a TreeSet, so the source token must be
     * Comparable. Each controller gets a unique id; the controller itself is the token so
     * that setIRQ(this) and clearIRQ(this) refer to the same entry.
     */
    private static final java.util.concurrent.atomic.AtomicLong IRQ_SEQUENCE = new java.util.concurrent.atomic.AtomicLong();
    private final long irqId = IRQ_SEQUENCE.incrementAndGet();

    public static final int ARG1 = 0x00;
    public static final int ARG2 = 0x04;
    public static final int CMDCMD = 0x08;
    public static final int CMDTMO = 0x0C;
    public static final int CMDRESP0 = 0x10;
    public static final int CMDRESP1 = 0x14;
    public static final int CMDRESP2 = 0x18;
    public static final int CMDRESP3 = 0x1C;
    public static final int DATA = 0x20;
    public static final int STATUS = 0x24;
    public static final int INTERRUPT = 0x28;
    public static final int IRQ_MASK = 0x2C;

    public static final int STATUS_BUSY = 1 << 0;
    public static final int STATUS_ERROR = 1 << 1;

    public static final int CMD_GO_IDLE_STATE = 0;
    public static final int CMD_SEND_OP_COND = 41; // ACMD41 = index 41 with bit 6 (0x40) set
    public static final int CMD_SET_BLOCKLEN = 16;
    public static final int CMD_READ_SINGLE = 17;
    public static final int CMD_WRITE_SINGLE = 24;

    public static final int IRQ_COMPLETE = 1 << 0;

    private final SdCard card;
    private final CPU cpu;
    private final byte[] regs = new byte[0x30];
    private int status;

    public EmmcController(SdCard card) {
        this(card, null);
    }

    public EmmcController(SdCard card, CPU cpu) {
        this.card = card;
        this.cpu = cpu;
    }

    @Override
    public byte read(int offset) {
        if (offset < 0 || offset >= regs.length) {
            return 0;
        }
        if (offset >= STATUS && offset <= STATUS + 3) {
            return (byte) ((status >>> (8 * (offset - STATUS))) & 0xFF);
        }
        return regs[offset];
    }

    @Override
    public void write(int offset, byte value) {
        if (offset < 0 || offset >= regs.length) {
            System.err.println("Warning: unimplemented EMMC register offset 0x" + Integer.toHexString(offset));
            return;
        }
        if (offset == INTERRUPT) {
            // write-1-to-clear: the written value is a clear mask, not a stored value
            if ((value & IRQ_COMPLETE) != 0) {
                regs[INTERRUPT] &= ~IRQ_COMPLETE;
                if (cpu != null) {
                    cpu.clearIRQ(this);
                }
            }
            return;
        }
        regs[offset] = value;
        if (offset == CMDCMD + 3) {
            // The command is executed once the high byte of CMDCMD is written.
            int cmdWord = (regs[CMDCMD] & 0xFF)
                    | ((regs[CMDCMD + 1] & 0xFF) << 8)
                    | ((regs[CMDCMD + 2] & 0xFF) << 16)
                    | ((regs[CMDCMD + 3] & 0xFF) << 24);
            processCommand(cmdWord);
        }
    }

    private void processCommand(int cmdWord) {
        setStatus(STATUS_BUSY);
        int cmdIndex = cmdWord & 0x3F;
        boolean isAcmd = (cmdWord & 0x40) != 0;
        // clear response
        regs[CMDRESP0] = 0;
        regs[CMDRESP1] = 0;
        regs[CMDRESP2] = 0;
        regs[CMDRESP3] = 0;
        if (isAcmd) {
            if (cmdIndex == CMD_SEND_OP_COND) {
                regs[CMDRESP0] = 0x00; // ready
            } else {
                setStatus(STATUS_ERROR);
            }
        } else {
            switch (cmdIndex) {
                case CMD_GO_IDLE_STATE:
                    regs[CMDRESP0] = 0x01; // idle
                    break;
                case CMD_SEND_OP_COND:
                    // CMD41 sent without the APP_CMD bit also means SEND_OP_COND
                    regs[CMDRESP0] = 0x00; // ready
                    break;
                case CMD_SET_BLOCKLEN:
                    break; // acknowledged
                case CMD_READ_SINGLE:
                    if (!readSectorToRam()) {
                        setStatus(STATUS_ERROR);
                    }
                    break;
                case CMD_WRITE_SINGLE:
                    if (!writeSectorFromRam()) {
                        setStatus(STATUS_ERROR);
                    }
                    break;
                default:
                    setStatus(STATUS_ERROR);
                    break;
            }
        }
        setStatus(getStatus() & ~STATUS_BUSY);
        // Completion is signalled on both success and error so software never waits forever.
        regs[INTERRUPT] |= IRQ_COMPLETE;
        if (cpu != null) {
            int mask = regs[IRQ_MASK] & 0xFF;
            if (mask == 0 || (mask & IRQ_COMPLETE) != 0) {
                cpu.setIRQ(this);
            }
        }
    }

    /**
     * CMD17: read the sector in ARG1 from the card and DMA it to the RAM address in DATA.
     *
     * @return true on success, false if the card/CPU is unavailable, the sector is out of
     *         range, or the DMA transfer fails.
     */
    private boolean readSectorToRam() {
        if (card == null || cpu == null) {
            return false;
        }
        int sector = readWord(ARG1);
        int addr = readWord(DATA);
        byte[] buffer;
        try {
            buffer = card.readSector(sector);
        } catch (IllegalArgumentException e) {
            return false;
        }
        try {
            for (int i = 0; i < buffer.length / 4; i++) {
                int word = (buffer[i * 4] & 0xFF)
                        | ((buffer[i * 4 + 1] & 0xFF) << 8)
                        | ((buffer[i * 4 + 2] & 0xFF) << 16)
                        | ((buffer[i * 4 + 3] & 0xFF) << 24);
                cpu.instructionWriteWord(addr + i * 4, word, false);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * CMD24: DMA 512 bytes from the RAM address in DATA and write them to the sector in ARG1.
     *
     * @return true on success, false if the card/CPU is unavailable, the sector is out of
     *         range, or the DMA transfer fails.
     */
    private boolean writeSectorFromRam() {
        if (card == null || cpu == null) {
            return false;
        }
        int sector = readWord(ARG1);
        int addr = readWord(DATA);
        byte[] buffer = new byte[SdCard.SECTOR_SIZE];
        try {
            for (int i = 0; i < buffer.length / 4; i++) {
                int word = cpu.instructionReadWord(addr + i * 4, false);
                buffer[i * 4] = (byte) (word & 0xFF);
                buffer[i * 4 + 1] = (byte) ((word >> 8) & 0xFF);
                buffer[i * 4 + 2] = (byte) ((word >> 16) & 0xFF);
                buffer[i * 4 + 3] = (byte) ((word >> 24) & 0xFF);
            }
        } catch (Exception e) {
            return false;
        }
        try {
            card.writeSector(sector, buffer);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /** Assemble a little-endian 32-bit word from the register file. */
    private int readWord(int offset) {
        return (regs[offset] & 0xFF)
                | ((regs[offset + 1] & 0xFF) << 8)
                | ((regs[offset + 2] & 0xFF) << 16)
                | ((regs[offset + 3] & 0xFF) << 24);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int compareTo(EmmcController other) {
        return Long.compare(irqId, other.irqId);
    }
}
