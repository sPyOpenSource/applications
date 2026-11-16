package jCPU.arm;

import jCPU.Peripheral;

public class EmmcController implements Peripheral {
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

    private final SdCard card;
    private final byte[] regs = new byte[0x30];
    private int status;

    public EmmcController(SdCard card) {
        this.card = card;
    }

    @Override
    public byte read(int offset) {
        if (offset < 0 || offset >= regs.length) {
            return 0;
        }
        return regs[offset];
    }

    @Override
    public void write(int offset, byte value) {
        if (offset < 0 || offset >= regs.length) {
            System.err.println("Warning: unimplemented EMMC register offset 0x" + Integer.toHexString(offset));
            return;
        }
        regs[offset] = value;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
