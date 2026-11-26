/*
 * $Id$
 *
 * Copyright (C) 2003-2015 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 */
package org.jnode.fs.jfat;

import java.io.IOException;
import jx.devices.bio.BlockIO;

/**
 * A FAT implementation for FAT-16.
 */
public class Fat16 extends Fat {

    protected Fat16(BootSector bs, BlockIO api) {
        super(bs, api);
    }

    @Override
    protected int offset(int index) {
        return 2 * index;
    }

    @Override
    public int get(int index) throws IOException {
        return getUInt16(index) & 0xFFFF;
    }

    @Override
    public int set(int index, int element) throws IOException {
        final int old = getUInt16(index);
        setInt16(index, element & 0xFFFF);
        return old & 0xFFFF;
    }

    @Override
    public int getClusterPosition(int index) {
        final BootSector bootSector = getBootSector();
        final int rootDirectoryOffset = bootSector.getFirstDataSector() * bootSector.getBytesPerSector();
        if (index == 0) {
            return rootDirectoryOffset;
        }
        // FAT-12/16 reserve a fixed root directory area before the data clusters.
        final int filesOffset = rootDirectoryOffset + bootSector.getNrRootDirEntries() * 32;
        return filesOffset + ((index - firstCluster()) * getClusterSize());
    }

    @Override
    public boolean isEofChain(int entry) {
        return entry >= 0xFFF0;
    }

    @Override
    public int eofChain() {
        return 0xFFFF;
    }
}
