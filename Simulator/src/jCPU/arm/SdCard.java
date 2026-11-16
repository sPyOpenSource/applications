package jCPU.arm;

import java.io.IOException;
import java.io.RandomAccessFile;

public class SdCard implements AutoCloseable {
    public static final int SECTOR_SIZE = 512;
    private final RandomAccessFile file;
    private final int sectorCount;

    public SdCard(String imagePath) throws IOException {
        file = new RandomAccessFile(imagePath, "rw");
        sectorCount = (int) (file.length() / SECTOR_SIZE);
    }

    public int getSectorCount() {
        return sectorCount;
    }

    public byte[] readSector(int sector) {
        if (sector < 0 || sector >= sectorCount) {
            throw new IllegalArgumentException("sector out of range: " + sector);
        }
        byte[] buf = new byte[SECTOR_SIZE];
        try {
            file.seek((long) sector * SECTOR_SIZE);
            int read = file.read(buf);
            if (read != SECTOR_SIZE) {
                throw new IOException("short read: " + read);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buf;
    }

    public void writeSector(int sector, byte[] data) {
        if (sector < 0 || sector >= sectorCount) {
            throw new IllegalArgumentException("sector out of range: " + sector);
        }
        if (data == null || data.length != SECTOR_SIZE) {
            throw new IllegalArgumentException("data must be 512 bytes");
        }
        try {
            file.seek((long) sector * SECTOR_SIZE);
            file.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
