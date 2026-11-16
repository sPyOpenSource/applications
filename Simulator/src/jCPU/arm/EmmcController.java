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

    public static final int CMD_GO_IDLE_STATE = 0;
    public static final int CMD_SEND_OP_COND = 41; // ACMD41 = index 41 with bit 6 (0x40) set
    public static final int CMD_SET_BLOCKLEN = 16;
    public static final int CMD_READ_SINGLE = 17;
    public static final int CMD_WRITE_SINGLE = 24;

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
                case CMD_WRITE_SINGLE:
                    // handled in Tasks 5/6; for now mark busy then clear
                    break;
                default:
                    setStatus(STATUS_ERROR);
                    break;
            }
        }
        setStatus(getStatus() & ~STATUS_BUSY);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
