package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86EthernetNicTest
{
    @Test
    public void testVendorIdReadsBackMarvell() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0xAB, nic.read(0x300));
        assertEquals((byte) 0x11, nic.read(0x301));
    }

    @Test
    public void testDeviceIdReadsBackYukon88E8055() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0x53, nic.read(0x302));
        assertEquals((byte) 0x43, nic.read(0x303));
    }

    @Test
    public void testMacAddressReadWrite() {
        X86EthernetNic nic = new X86EthernetNic(null);
        byte[] mac = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55};
        for (int i = 0; i < 6; i++) {
            nic.write(0x304 + i, mac[i]);
        }
        for (int i = 0; i < 6; i++) {
            assertEquals(mac[i], nic.read(0x304 + i));
        }
    }

    @Test
    public void testTransmitStatusStartsReady() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0x01, nic.read(0x314));
    }

    @Test
    public void testInterruptStatusStartsZero() {
        X86EthernetNic nic = new X86EthernetNic(null);
        assertEquals((byte) 0x00, nic.read(0x330));
    }

    @Test
    public void testTransmitCycleSetsInterrupt() {
        X86EthernetNic nic = new X86EthernetNic(null);
        nic.write(0x310, (byte) 0xDE);
        nic.write(0x310, (byte) 0xAD);
        nic.write(0x310, (byte) 0xBE);
        nic.write(0x310, (byte) 0xEF);
        nic.write(0x311, (byte) 0x04);
        nic.write(0x312, (byte) 0x00);
        nic.write(0x313, (byte) 0x01); // TX_CMD_SEND
        byte txStatus = nic.read(0x314);
        assertTrue("TX should be sending or error", (txStatus & 0x02) != 0 || (txStatus & 0x04) != 0);
        assertEquals((byte) 0x02, nic.read(0x330) & 0x02); // INT_TX_COMPLETE
    }

    @Test
    public void testReceiveCycle() {
        X86EthernetNic nic = new X86EthernetNic(null);
        byte[] packet = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF, (byte) 0x00};
        nic.enqueueRx(packet, packet.length);
        assertEquals((byte) 0x01, nic.read(0x324));           // RX_STATUS_PACKET
        assertEquals((byte) 0x07, nic.read(0x321));           // length low
        assertEquals((byte) 0x00, nic.read(0x322));           // length high
        assertEquals((byte) 0xAA, nic.readRxData(0));
        assertEquals((byte) 0xFF, nic.readRxData(5));
        nic.write(0x323, (byte) 0x01);                        // RX_CMD_ACK
        assertEquals((byte) 0x00, nic.read(0x324));           // status cleared
        assertEquals((byte) 0x00, nic.read(0x330) & 0x01);    // INT_RX_PACKET cleared
    }

    @Test
    public void testInterruptClear() {
        X86EthernetNic nic = new X86EthernetNic(null);
        nic.write(0x330, (byte) 0x00); // write mask to clear
        assertEquals((byte) 0x00, nic.read(0x330));
    }
}
