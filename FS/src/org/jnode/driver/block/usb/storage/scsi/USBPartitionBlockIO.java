package org.jnode.driver.block.usb.storage.scsi;

import jx.devices.bio.BlockIO;
import jx.zero.Memory;

/**
 * Exposes one primary partition of a whole-disk {@link BlockIO}.
 *
 * USB mass storage block devices address the whole disk, but a FAT filesystem
 * lives in a partition.  This wrapper translates partition-relative sector
 * numbers into whole-disk sector numbers.
 */
public final class USBPartitionBlockIO implements BlockIO {

    private final BlockIO wholeDisk;
    private final int partitionStart;
    private final int partitionSectors;

    public USBPartitionBlockIO(BlockIO wholeDisk, int partitionStart, int partitionSectors) {
        this.wholeDisk = wholeDisk;
        this.partitionStart = partitionStart;
        this.partitionSectors = partitionSectors;
    }

    @Override
    public int getCapacity() {
        return partitionSectors;
    }

    @Override
    public int getSectorSize() {
        return wholeDisk.getSectorSize();
    }

    @Override
    public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
        wholeDisk.readSectors(partitionStart + startSector, numberOfSectors, buf, synchronous);
    }

    @Override
    public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
        wholeDisk.writeSectors(partitionStart + startSector, numberOfSectors, buf, synchronous);
    }
}
