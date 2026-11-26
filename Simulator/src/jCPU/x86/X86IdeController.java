package jCPU.x86;

import jCPU.Peripheral;
import java.io.IOException;

/**
 * Primary IDE/ATA controller.
 *
 * <p>Registers itself on the standard primary-channel I/O ports:
 * 0x1F0-0x1F7 (data, error/features, sector count, sector/LBA-low,
 * cylinder-low/LBA-mid, cylinder-high/LBA-high, drive/head,
 * status/command) and 0x3F6 (alternate status/device control).
 *
 * <p>Supports two devices (master=drive 0, slave=drive 1), each of which
 * may be an {@link X86IdeHardDrive} or an {@link X86IdeCdrom}.  Implements
 * a minimal PIO command set: IDENTIFY DEVICE (0xEC), IDENTIFY PACKET
 * DEVICE (0xA0), READ SECTORS (0x20), WRITE SECTORS (0x30) and the ATAPI
 * PACKET command.</p>
 */
public class X86IdeController implements Peripheral
{
    /* ---------------- I/O port addresses ---------------- */
    private static final int PORT_DATA       = 0x1F0;
    private static final int PORT_ERROR      = 0x1F1;
    private static final int PORT_SECTOR_CNT = 0x1F2;
    private static final int PORT_SECTOR_NUM = 0x1F3;
    private static final int PORT_CYL_LOW    = 0x1F4;
    private static final int PORT_CYL_HIGH   = 0x1F5;
    private static final int PORT_DRIVE_HEAD = 0x1F6;
    private static final int PORT_STATUS_CMD = 0x1F7;
    private static final int PORT_ALT_STATUS = 0x3F6;

    /* ---------------- Status register bits ---------------- */
    private static final int STS_BUSY  = 0x80;
    private static final int STS_READY = 0x40;
    private static final int STS_DRQ   = 0x08;
    private static final int STS_ERR   = 0x01;

    /* ---------------- IDE commands ---------------- */
    private static final int CMD_IDENTIFY_DEVICE      = 0xEC;
    private static final int CMD_IDENTIFY_PACKET      = 0xA0;
    private static final int CMD_READ_SECTORS         = 0x20;
    private static final int CMD_WRITE_SECTORS        = 0x30;
    private static final int CMD_ATAPI_PACKET         = 0xA0;

    /* ---------------- Devices ---------------- */
    private final X86IdeDevice[] drives = new X86IdeDevice[2];
    private int selectedDrive;   // 0 = master, 1 = slave (from drive/head register bit 4)

    /* ---------------- Task-file registers ---------------- */
    private int sectorCount;     // sectors remaining in current command
    private int sectorNum;       // sector number / LBA bits 0-7
    private int cylLow;          // cylinder low / LBA bits 8-15
    private int cylHigh;         // cylinder high / LBA bits 16-23
    private int driveHead;       // drive/head register

    /* ---------------- Command state ---------------- */
    private int status = STS_READY;   // not busy, ready
    private int error = 0;
    private int command = 0;

    /* PIO data-transfer state */
    private final byte[] dataBuf = new byte[X86IdeDevice.SECTOR_SIZE];
    private int dataOffset;   // offset within dataBuf for byte-wise host access
    private int sectorsLeft;  // sectors remaining to transfer
    private int curLba;       // current LBA for multi-sector transfers
    private boolean writing;  // true = host writes data to device (WRITE SECTORS)

    public X86IdeController()
    {
        drives[0] = null;
        drives[1] = null;
        selectedDrive = 0;
    }

    /* ---------------- Device attachment ---------------- */

    public void setMaster(X86IdeDevice dev) { drives[0] = dev; }
    public void setSlave(X86IdeDevice dev)  { drives[1] = dev; }

    public X86IdeDevice getMaster() { return drives[0]; }
    public X86IdeDevice getSlave()  { return drives[1]; }

    private X86IdeDevice currentDrive()
    {
        return drives[selectedDrive];
    }

    /* ---------------- Identify data ---------------- */

    /**
     * Build a 512-byte IDENTIFY DEVICE data block for an ATA hard disk.
     * Only the fields a simple OS would care about are filled in.
     */
    private static byte[] identifyAta(X86IdeDevice dev)
    {
        byte[] id = new byte[X86IdeDevice.SECTOR_SIZE];

        // Word 0: general configuration (0x0040 = bit 6 set = not removable, bit 5 = not MFM)
        id[0]  = 0x40; id[1] = 0x00;

        // Words 1-9: obsolete / reserved
        // Words 10-19: serial number (20 bytes)
        String serial = "SIMULATORDISK          ";
        System.arraycopy(serial.getBytes(), 0, id, 20, serial.length());

        // Words 20-22: obsolete
        // Word 23: obsolete
        // Words 27-46: firmware revision (8 bytes) + model number (40 bytes)
        String fw = "1.0     ";
        System.arraycopy(fw.getBytes(), 0, id, 54, fw.length());

        String model = dev.getModel();
        // model is stored as 40 bytes, bytes swapped within each word
        byte[] modelBytes = new byte[40];
        java.util.Arrays.fill(modelBytes, (byte) ' ');
        byte[] m = model.getBytes();
        System.arraycopy(m, 0, modelBytes, 0, Math.min(m.length, 40));
        // swap bytes within each word (IDE identify data is big-endian per word)
        for (int i = 0; i < 40; i += 2)
        {
            id[58 + i]     = modelBytes[i + 1];
            id[58 + i + 1] = modelBytes[i];
        }

        // Words 47-48: obsolete
        // Word 49: capabilities (0x0200 = LBA supported)
        id[98] = 0x00; id[99] = 0x02;

        // Words 50-51: obsolete
        // Words 54-55: obsolete
        // Words 57-58: obsolete
        // Words 59-60: obsolete
        // Words 61-62: obsolete

        // Words 60-61: total sectors (28-bit LBA), little-endian
        int sectors = dev.getSectorCount();
        id[120] = (byte) (sectors & 0xFF);
        id[121] = (byte) ((sectors >> 8) & 0xFF);
        id[122] = (byte) ((sectors >> 16) & 0xFF);
        id[123] = (byte) ((sectors >> 24) & 0xFF);

        // Word 83: multiple sector count (0x0100 = 1 sector per interrupt)
        id[166] = 0x00; id[167] = 0x01;

        // Word 84: 0x4000 (supports LBA)
        id[168] = 0x00; id[169] = 0x40;

        // Word 88: ultra dma mode (0x0079 = mode 5)
        id[176] = 0x79; id[177] = 0x00;

        return id;
    }

    /**
     * Build a 512-byte IDENTIFY PACKET DEVICE data block for an ATAPI CDROM.
     */
    private static byte[] identifyPacket(X86IdeDevice dev)
    {
        byte[] id = new byte[X86IdeDevice.SECTOR_SIZE];

        // Word 0: general configuration
        // Bit 15 = 1 (ATAPI device), bit 7 = removable media, bit 5 = not MFM
        id[0] = (byte) 0x80; id[1] = 0x00;
        // byte 0: 0x80 = ATAPI, byte 1: 0x00 (device type: byte 8-9 will refine)
        // Actually byte 0: bit 7=ATAPI, bits 5-0=device type. 0x05 = CDROM.
        id[0] = (byte) (0x80 | 0x05); // device type 5 = CDROM
        id[1] = 0x00;

        // Words 10-19: serial number
        String serial = "SIMULATORCDROM         ";
        System.arraycopy(serial.getBytes(), 0, id, 20, serial.length());

        // Words 27-46: firmware + model
        String fw = "1.0     ";
        System.arraycopy(fw.getBytes(), 0, id, 54, fw.length());

        String model = dev.getModel();
        byte[] modelBytes = new byte[40];
        java.util.Arrays.fill(modelBytes, (byte) ' ');
        byte[] m = model.getBytes();
        System.arraycopy(m, 0, modelBytes, 0, Math.min(m.length, 40));
        for (int i = 0; i < 40; i += 2)
        {
            id[58 + i]     = modelBytes[i + 1];
            id[58 + i + 1] = modelBytes[i];
        }

        // Word 49: capabilities (0x0200 = LBA supported, 0x0001 = DMA?)
        id[98] = 0x00; id[99] = 0x02;

        // Words 60-61: total sectors (for CDROM this is often 0)
        id[120] = 0; id[121] = 0; id[122] = 0; id[123] = 0;

        // Word 83: multiple sector count
        id[166] = 0x00; id[167] = 0x01;

        // Word 94-95: ATAPI features (byte 94: 0x04 = packet size 12, byte 95: 0x00)
        id[188] = 0x04; id[189] = 0x00;

        // Word 96-99: release version / ATAPI version
        id[192] = 0x00; id[193] = 0x02;

        // Word 128-159: ATAPI identify (bytes 128-159)
        id[128] = 0x00; id[129] = 0x00; // command set supported
        id[130] = 0x00; id[131] = 0x00;

        // Byte 132: 0x01 = PACKET command supported, 0x02 = RELEASE/EDDY? 
        // Actually byte 132 bit 0 = 1 means PACKET command supported
        id[132] = 0x01;

        return id;
    }

    /* ---------------- Peripheral interface ---------------- */

    @Override
    public byte read(int port)
    {
        switch (port)
        {
            case PORT_DATA:
                return readDataByte();

            case PORT_ERROR:
                // During PIO data transfer (DRQ set) port 0x1F1 is the
                // data register high byte; otherwise it is the Error register.
                if ((status & STS_DRQ) != 0 && sectorsLeft > 0)
                {
                    return readDataByte();
                }
                return (byte) error;

            case PORT_SECTOR_CNT:
                return (byte) sectorCount;

            case PORT_SECTOR_NUM:
                return (byte) sectorNum;

            case PORT_CYL_LOW:
                return (byte) cylLow;

            case PORT_CYL_HIGH:
                return (byte) cylHigh;

            case PORT_DRIVE_HEAD:
                return (byte) driveHead;

            case PORT_STATUS_CMD:
                return (byte) status;

            case PORT_ALT_STATUS:
                // Alternate status: same as status but does not clear interrupt
                return (byte) status;

            default:
                return 0;
        }
    }

    @Override
    public void write(int port, byte value)
    {
        int v = value & 0xFF;

        switch (port)
        {
            case PORT_DATA:
                writeDataByte((byte) v);
                break;

            case PORT_ERROR:
                // Features register on write outside PIO; data register
                // high byte during PIO write transfer.
                if ((status & STS_DRQ) != 0 && sectorsLeft > 0 && writing)
                {
                    writeDataByte((byte) v);
                }
                break;

            case PORT_SECTOR_CNT:
                sectorCount = v;
                break;

            case PORT_SECTOR_NUM:
                sectorNum = v;
                break;

            case PORT_CYL_LOW:
                cylLow = v;
                break;

            case PORT_CYL_HIGH:
                cylHigh = v;
                break;

            case PORT_DRIVE_HEAD:
                driveHead = v;
                selectedDrive = (v & 0x10) != 0 ? 1 : 0; // bit 4 = DEV (1 = slave)
                break;

            case PORT_STATUS_CMD:
                executeCommand(v);
                break;

            case PORT_ALT_STATUS:
                // Device control: bit 2 = SRST (software reset), bit 3 = nIEN (disable interrupts)
                if ((v & 0x04) != 0)
                {
                    reset();
                }
                break;

            default:
                break;
        }
    }

    /* ---------------- Data register (16-bit PIO) ---------------- */

    private byte readDataByte()
    {
        X86IdeDevice dev = currentDrive();
        if (dev == null || (status & STS_BUSY) != 0)
        {
            return 0;
        }

        // If we are in a data-transfer phase (DRQ set), feed from buffer
        if ((status & STS_DRQ) != 0 && sectorsLeft > 0)
        {
            if (dataOffset >= dataBuf.length)
            {
                // Buffer exhausted: read next sector from device
                loadNextSector(dev);
                dataOffset = 0;
            }
            byte b = dataBuf[dataOffset++];
            if (dataOffset >= dataBuf.length)
            {
                // End of this sector in buffer
                sectorsLeft--;
                curLba++;
                if (sectorsLeft == 0)
                {
                    status &= ~STS_DRQ; // clear DRQ when done
                    status |= STS_READY;
                }
            }
            return b;
        }

        return 0;
    }

    private void writeDataByte(byte value)
    {
        X86IdeDevice dev = currentDrive();
        if (dev == null || (status & STS_BUSY) != 0)
        {
            return;
        }

        if ((status & STS_DRQ) != 0 && sectorsLeft > 0 && writing)
        {
            // Buffer full: save the current sector at its LBA, then advance.
            if (dataOffset >= dataBuf.length)
            {
                dev.writeSector(curLba, dataBuf);
                dataOffset = 0;
                sectorsLeft--;
                curLba++;
                if (sectorsLeft == 0)
                {
                    status &= ~STS_DRQ;
                    status |= STS_READY;
                    writing = false;
                    return; // byte belongs to a sector beyond the request
                }
            }
            dataBuf[dataOffset++] = value;
        }
    }

    private void loadNextSector(X86IdeDevice dev)
    {
        if (sectorsLeft <= 0) return;
        int lba = curLba;
        dev.readSector(lba, dataBuf);
    }

    /* ---------------- Command execution ---------------- */

    private void executeCommand(int cmd)
    {
        command = cmd;
        X86IdeDevice dev = currentDrive();

        if (dev == null)
        {
            error = 0x01; // no device
            status = STS_ERR | STS_READY;
            return;
        }

        status = STS_BUSY;

        try
        {
            switch (cmd)
            {
                case CMD_IDENTIFY_DEVICE:
                    handleIdentify(dev);
                    break;

                case CMD_IDENTIFY_PACKET:
                    handleIdentifyPacket(dev);
                    break;

                case CMD_READ_SECTORS:
                    handleReadSectors(dev);
                    break;

                case CMD_WRITE_SECTORS:
                    handleWriteSectors(dev);
                    break;

                default:
                    // Unknown command: abort with error
                    error = 0x04; // command aborted
                    status = STS_ERR | STS_READY;
                    break;
            }
        }
        catch (IOException e)
        {
            error = 0x04;
            status = STS_ERR | STS_READY;
        }
    }

    private void handleIdentify(X86IdeDevice dev) throws IOException
    {
        if (dev.isAtapi())
        {
            // ATAPI device does not respond to IDENTIFY DEVICE
            error = 0x04;
            status = STS_ERR | STS_READY;
            return;
        }

        byte[] id = identifyAta(dev);
        System.arraycopy(id, 0, dataBuf, 0, dataBuf.length);
        dataOffset = 0;
        sectorsLeft = 1; // one sector (256 words)
        curLba = 0;
        status = STS_DRQ | STS_READY;
    }

    private void handleIdentifyPacket(X86IdeDevice dev) throws IOException
    {
        if (!dev.isAtapi())
        {
            // ATA device does not respond to IDENTIFY PACKET DEVICE
            error = 0x04;
            status = STS_ERR | STS_READY;
            return;
        }

        byte[] id = identifyPacket(dev);
        System.arraycopy(id, 0, dataBuf, 0, dataBuf.length);
        dataOffset = 0;
        sectorsLeft = 1;
        curLba = 0;
        status = STS_DRQ | STS_READY;
    }

    private void handleReadSectors(X86IdeDevice dev)
    {
        int count = sectorCount & 0xFF;
        if (count == 0) count = 256;

        int lba = buildLba();
        curLba = lba;
        sectorsLeft = count;
        writing = false;
        dataOffset = dataBuf.length; // force loadNextSector on first read
        status = STS_DRQ | STS_READY;
    }

    private void handleWriteSectors(X86IdeDevice dev)
    {
        int count = sectorCount & 0xFF;
        if (count == 0) count = 256;

        int lba = buildLba();
        curLba = lba;
        sectorsLeft = count;
        writing = true;
        dataOffset = 0;
        status = STS_DRQ | STS_READY;
    }

    private int buildLba()
    {
        // 28-bit LBA from sector/cylinder/drive-head registers
        return (sectorNum & 0xFF) |
               ((cylLow & 0xFF) << 8) |
               ((cylHigh & 0xFF) << 16) |
               ((driveHead & 0x0F) << 24);
    }

    private void reset()
    {
        status = STS_READY;
        error = 0;
        command = 0;
        sectorCount = 0;
        sectorNum = 0;
        cylLow = 0;
        cylHigh = 0;
        driveHead = 0;
        selectedDrive = 0;
        sectorsLeft = 0;
        writing = false;
        dataOffset = 0;
    }

    public void close() throws IOException
    {
        for (X86IdeDevice d : drives)
        {
            if (d != null) d.close();
        }
    }
}
