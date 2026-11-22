package jCPU.x86;

import jCPU.Peripheral;

/**
 * Minimal-functionality Marvell Yukon 88E8055 NIC emulation.
 *
 * Registers at I/O ports 0x300-0x31F via the existing Peripheral interface.
 * RX/TX register read/write logic is implemented, including the RX data port
 * with an auto-increment read cursor and RX enqueue input validation.
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

    /* TX/RX buffer state */
    private final byte[] txBuffer = new byte[2048];
    private int txLength = 0;
    private byte txStatus = TX_STATUS_READY;

    private final byte[] rxBuffer = new byte[2048];
    private int rxLength = 0;
    private byte rxStatus = 0;
    private int rxReadIndex = 0;

    private byte interruptStatus = 0;

    /** Whether PCAP host I/O is available. pcap4j is not on the
     *  classpath on this system, so host connectivity is deferred and
     *  this is fixed to false. Real host I/O requires pcap4j. */
    private final boolean pcapAvailable;

    public X86EthernetNic(String ifaceName) {
        this.ifaceName = ifaceName;
        // pcap4j is not available on this system, so host I/O via PCAP is
        // deferred. pcapAvailable stays false; add pcap4j to the classpath
        // for real host connectivity.
        this.pcapAvailable = false;
    }

    @Override
    public byte read(int port) {
        int offset = port - PORT_MAC_BASE;
        if (offset >= 0 && offset < 6) {
            return mac[offset];
        }
        switch (port) {
            case PORT_VENDOR_LO: return (byte) (VENDOR_ID_MARVELL & 0xFF);
            case PORT_VENDOR_HI: return (byte) ((VENDOR_ID_MARVELL >> 8) & 0xFF);
            case PORT_DEVICE_LO: return (byte) (DEVICE_ID_YUKON_88E8055 & 0xFF);
            case PORT_DEVICE_HI: return (byte) ((DEVICE_ID_YUKON_88E8055 >> 8) & 0xFF);
            case PORT_TX_LEN_LO: return (byte) (txLength & 0xFF);
            case PORT_TX_LEN_HI: return (byte) ((txLength >> 8) & 0xFF);
            case PORT_TX_STATUS: return txStatus;
            case PORT_RX_LEN_LO: return (byte) (rxLength & 0xFF);
            case PORT_RX_LEN_HI: return (byte) ((rxLength >> 8) & 0xFF);
            case PORT_RX_STATUS: return rxStatus;
            case PORT_RX_DATA:
                if (rxReadIndex < rxLength) {
                    return rxBuffer[rxReadIndex++];
                }
                return 0;
            case PORT_INT_STATUS: return interruptStatus;
            default: return 0;
        }
    }

    @Override
    public void write(int port, byte value) {
        int offset = port - PORT_MAC_BASE;
        if (offset >= 0 && offset < 6) {
            mac[offset] = value;
            return;
        }
        switch (port) {
            case PORT_TX_DATA:
                if (txLength < txBuffer.length) {
                    txBuffer[txLength++] = value;
                }
                break;
            case PORT_TX_LEN_LO:
                txLength = (txLength & 0xFF00) | (value & 0xFF);
                break;
            case PORT_TX_LEN_HI:
                txLength = (txLength & 0x00FF) | ((value & 0xFF) << 8);
                break;
            case PORT_TX_CMD:
                if (value == TX_CMD_SEND) {
                    if (!pcapAvailable) {
                        // Graceful degradation: pcap4j is not on the
                        // classpath, so we cannot inject the frame to the
                        // host. Report a transmit error rather than
                        // pretending the frame was sent.
                        txStatus = TX_STATUS_ERROR;
                    } else {
                        // Future: inject via pcap and report SENDING.
                        txStatus = TX_STATUS_SENDING;
                    }
                    interruptStatus |= INT_TX_COMPLETE;
                }
                break;
            case PORT_RX_CMD:
                if (value == RX_CMD_ACK) {
                    rxLength = 0;
                    rxStatus = 0;
                    rxReadIndex = 0;
                    interruptStatus &= ~INT_RX_PACKET;
                }
                break;
            case PORT_INT_STATUS:
                interruptStatus &= (byte) ~value;
                break;
            default:
                break;
        }
    }

    /** Enqueue received bytes from the PCAP thread (Task 4). */
    public void enqueueRx(byte[] packet, int len) {
        if (packet == null) return;
        if (len < 0) len = 0;
        if (len > rxBuffer.length) len = rxBuffer.length;
        if (len > packet.length) len = packet.length;
        System.arraycopy(packet, 0, rxBuffer, 0, len);
        rxLength = len;
        rxStatus = RX_STATUS_PACKET;
        interruptStatus |= INT_RX_PACKET;
    }

    /** Read one byte from the current RX packet (guest reads PORT_RX_DATA). */
    public byte readRxData(int index) {
        if (index < 0 || index >= rxLength) return 0;
        return rxBuffer[index];
    }
}
