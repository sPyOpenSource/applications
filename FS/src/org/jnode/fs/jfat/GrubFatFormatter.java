/*
 * $Id$
 *
 * Copyright (C) 2003-2015 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package org.jnode.fs.jfat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import jx.devices.bio.BlockIO;
import jx.zero.Memory;
import jx.zero.MemoryManager;

import org.jnode.driver.block.Geometry;
import org.jnode.util.FileUtils;
//import org.jnode.util.LittleEndian;

/**
 * @author epr
 */
public class GrubFatFormatter {
    private Memory stage1;
    private Memory stage2;
    private final int bootSectorOffset;
    private String configFile;
    private int installPartition = 0xFFFFFFFF;
    private FatFormatter formatter;
MemoryManager MemManager;

    /**
     * @param bps
     * @param spc
     * @param geom
     * @param fatSize
     */
    public GrubFatFormatter(int bps, int spc, Geometry geom, FatType fatSize, int bootSectorOffset,
            String stage1ResourceName, String stage2ResourceName) {

        GrubBootSector bs =
                (GrubBootSector) createBootSector(stage1ResourceName, stage2ResourceName);
        bs.setOemName("JavaOS1.0");
        formatter =
                FatFormatter.HDFormatter(bps, (int) geom.getTotalSectors(), geom.getSectors(), geom
                        .getHeads(), fatSize, 0, calculateReservedSectors(512), bs);
        this.bootSectorOffset = bootSectorOffset;
    }

    private int calculateReservedSectors(int bps) {
        return stage2.size() / bps + 1 + 1;
    }

    /**
     * Constructor for GrubFatFormatter.
     * 
     * @param bootSectorOffset
     * @param stage1ResourceName
     * @param stage2ResourceName
     */
    public GrubFatFormatter(int bootSectorOffset, String stage1ResourceName,
            String stage2ResourceName) {
        GrubBootSector bs =
                (GrubBootSector) createBootSector(stage1ResourceName, stage2ResourceName);
        bs.setOemName("JavaOS1.0");
        formatter = FatFormatter.fat144FloppyFormatter(calculateReservedSectors(512), bs);
        this.bootSectorOffset = bootSectorOffset;
    }

    /**
     * Create the actual bootsector.
     */
    private BootSector createBootSector(String stage1Name, String stage2Name) {
        if (stage1Name == null) {
            stage1Name = "stage1";
        }
        if (stage2Name == null) {
            stage2Name = "stage2";
        }
        try {
            getStage1(stage1Name);
            getStage2(stage2Name);
            return new GrubBootSector(stage1);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Memory getStage1(String stage1ResourceName) throws IOException {
        if (stage1 == null) {
            InputStream is = getClass().getClassLoader().getResourceAsStream(stage1ResourceName);
            Memory buf = MemManager.alloc(512);
            FileUtils.copy(is, buf);
            is.close();
            stage1 = buf;
        }
        return stage1;
    }

    public Memory getStage2(String stage2ResourceName) throws IOException {
        if (stage2 == null) {
            URL stage2URL = getClass().getClassLoader().getResource(stage2ResourceName);
            URLConnection conn = stage2URL.openConnection();
            Memory buf = MemManager.alloc(conn.getContentLength());
            InputStream is = conn.getInputStream();
            FileUtils.copy(is, buf);
            is.close();
            stage2 = buf;
        }
        return stage2;
    }

    /**
     * @see org.jnode.fs.fat.FatFormatter#format(BlockDeviceAPI)
     */
    public void format(BlockIO api) throws IOException {
        formatter.format(api);
        GrubBootSector bs = (GrubBootSector) formatter.getBootSector();
        /* Fixup the blocklist end the end of the first sector of stage2 */
        stage2.setLittleEndian32((512 - 8) >> 2, bootSectorOffset + 2);

        /* Fixup the install partition */
        stage2.setLittleEndian32((512 + 0x08) >> 2, installPartition);

        /* Fixup the config file */
        if (configFile != null) {
            int ofs = 512 + 0x12;
            while (stage2.get8(ofs) != 0) {
                ofs++;
            }
            ofs++; /* Skip '\0' */
            for (int i = 0; i < configFile.length(); i++) {
                stage2.set8(ofs++, (byte) configFile.charAt(i));
            }
            stage2.set8(ofs, (byte)0);
        }

        /* Write stage2 */
        api.writeSectors(bs.getBytesPerSector(), stage2.size(), stage2, true);
    }

    /**
     * @return String
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * Sets the configFile.
     *
     * @param configFile
     *           The configFile to set
     */
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    /**
     * @return int
     */
    public int getInstallPartition() {
        return installPartition;
    }

    public BootSector getBootSector() {
        return formatter.getBootSector();
    }

    /**
     * Sets the installPartition.
     *
     * @param installPartition
     *           The installPartition to set
     */
    public void setInstallPartition(int installPartition) {
        this.installPartition = installPartition;
    }
}
