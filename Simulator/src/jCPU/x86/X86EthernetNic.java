package jCPU.x86;

import jCPU.Peripheral;

/**
 * Minimal-functionality Marvell Yukon 88E8055 NIC emulation.
 *
 * Registers at I/O ports 0x300-0x31F via the existing Peripheral interface.
 * PCAP host connectivity is added in Task 2; this task only establishes the
 * register map and empty read/write stubs.
 */
public class X86EthernetNic implements Peripheral
{
    /* ---------------- register ports ---------------- */
    public static final int PORT_VENDOR_LO   = 0x300;
    public static final int PORT_VENDOR_HI   = 0x301;
    public static final int PORT_DEVICE_LO   = 0x302;
    public static final int PORT_DEVICE_HI   = 0x303;
    public static final int PORT_MAC_BASE    = 0x304;   // 6 bytes: 0x304-0x309
    public static final int PORT_TX_DATA     = 0x310;
    public static final int PORT_TX_LEN_LO   = 0x311;
    public static final int PORT_TX_LEN_HI   = 0x312;
    public static final int PORT_TX_CMD      = 0x313;
    public static final int PORT_TX_STATUS   = 0x314;
    public static final int PORT_RX_DATA     = 0x320;
    public static final int PORT_RX_LEN_LO   = 0x321;
    public static final int PORT_RX_LEN_HI   = 0x322;
    public static final int PORT_RX_CMD      = 0x323;
    public static final int PORT_RX_STATUS   = 0x324;
    public static final int PORT_INT_STATUS  = 0x330;

    /* ---------------- register values ---------------- */
    public static final short VENDOR_ID_MARVELL = 0x11AB;
    public static final short DEVICE_ID_YUKON_88E8055 = 0x4353;

    /* TX status bits */
    public static final byte TX_STATUS_READY   = 0x01;
    public static final byte TX_STATUS_SENDING = 0x02;
    public static final byte TX_STATUS_ERROR   = 0x04;

    /* RX status bits */
    public static final byte RX_STATUS_PACKET  = 0x01;

    /* Interrupt status bits */
    public static final byte INT_RX_PACKET  = 0x01;
    public static final byte INT_TX_COMPLETE = 0x02;

    /* TX commands */
    public static final byte TX_CMD_SEND = 0x01;

    /* RX commands */
    public static final byte RX_CMD_ACK = 0x01;

    private final String ifaceName;
    private final byte[] mac = new byte[6];

    public X86EthernetNic(String ifaceName) {
        this.ifaceName = ifaceName;
    }

    @Override
    public byte read(int port) {
        return 0;
    }

    @Override
    public void write(int port, byte value) {
    }
}
