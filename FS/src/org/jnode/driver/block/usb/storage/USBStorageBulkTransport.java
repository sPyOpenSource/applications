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
 
package org.jnode.driver.block.usb.storage;

import jx.zero.Memory;
import org.jnode.driver.bus.scsi.CDB;
import org.jnode.driver.bus.usb.SetupPacket;
import org.jnode.driver.bus.usb.USBControlPipe;
import org.jnode.driver.bus.usb.USBDataPipe;
import org.jnode.driver.bus.usb.USBDevice;
import org.jnode.driver.bus.usb.USBException;
import org.jnode.driver.bus.usb.USBPacket;
import org.jnode.driver.bus.usb.USBRequest;
import org.jnode.util.NumberUtils;

final class USBStorageBulkTransport implements ITransport, USBStorageConstants {

    /**
     * My logger
     */
    //private static final Logger log = Logger.getLogger(USBStorageBulkTransport.class);
    /** */
    private final USBStorageDeviceData storageDeviceData;
    /** */
    private int nextTag = 1;
    /** */
    private static final int MAX_BOT_TRANSFER = 65536; // 64KB

    /**
     * @param storageDeviceData
     */
    public USBStorageBulkTransport(USBStorageDeviceData storageDeviceData) {
        this.storageDeviceData = storageDeviceData;
    }

    /*
     * (non-Javadoc)
     * @see org.jnode.driver.block.usb.storage.ITransport#transport(org.jnode.driver.bus.scsi.CDB, int)
     */
    public void transport(CDB cdb, Memory data, int dataOffset, long timeout) throws USBException {
        Memory cdbMem = cdb.toByteArray();
        int dataLen = cdb.getDataTransfertCount();
        if (dataLen > 0 && data == null) {
            throw new USBException("CDB requires " + dataLen + " bytes of data, but no buffer was provided");
        }

        // 1. Build and send CBW (OUT)
        CBW cbw = new CBW();
        cbw.setSignature(US_BULK_CB_SIGN);
        int tag = nextTag++;
        cbw.setTag(tag);
        cbw.setDataTransferLength(dataLen);
        boolean hasData = data != null && dataLen > 0;
        byte flags = (byte) (hasData && isDataIn(cdb) ? US_BULK_FLAG_IN : US_BULK_FLAG_OUT);
        cbw.setFlags(flags);
        cbw.setLun((byte) 0);
        cbw.setLength((byte) cdbMem.size());
        cbw.setCdb(cdbMem);

        USBDataPipe outPipe = (USBDataPipe) storageDeviceData.getBulkOutEndPoint().getPipe();
        USBRequest req = outPipe.createRequest(cbw);
        outPipe.syncSubmit(req, timeout);
        if (req.getStatus() != USBREQ_ST_COMPLETED) {
            throw new USBException("CBW submit failed, status: 0x" + NumberUtils.hex(req.getStatus(), 4));
        }

        // 2. Data phase (chunked at MAX_BOT_TRANSFER)
        if (hasData) {
            boolean dataIn = (flags & US_BULK_FLAG_IN) != 0;
            USBDataPipe dataPipe = dataIn
                ? (USBDataPipe) storageDeviceData.getBulkInEndPoint().getPipe()
                : (USBDataPipe) storageDeviceData.getBulkOutEndPoint().getPipe();
            for (int offset = 0; offset < dataLen; offset += MAX_BOT_TRANSFER) {
                int chunk = Math.min(MAX_BOT_TRANSFER, dataLen - offset);
                USBPacket packet = new USBPacket(data.getSubRange(dataOffset + offset, chunk));
                USBRequest dataReq = dataPipe.createRequest(packet);
                dataPipe.syncSubmit(dataReq, timeout);
                if (dataReq.getStatus() != USBREQ_ST_COMPLETED) {
                    throw new USBException("Data phase failed, status: 0x" + NumberUtils.hex(dataReq.getStatus(), 4));
                }
            }
        }

        // 3. Receive CSW (IN)
        CSW csw = new CSW();
        USBDataPipe inPipe = (USBDataPipe) storageDeviceData.getBulkInEndPoint().getPipe();
        USBRequest cswReq = inPipe.createRequest(csw);
        inPipe.syncSubmit(cswReq, timeout);
        if (cswReq.getStatus() != USBREQ_ST_COMPLETED) {
            throw new USBException("CSW submit failed, status: 0x" + NumberUtils.hex(cswReq.getStatus(), 4));
        }

        // 4. Validate CSW
        if (csw.getSignature() != US_BULK_CS_SIGN) {
            throw new USBException("Invalid CSW signature: 0x" + NumberUtils.hex(csw.getSignature(), 8));
        }
        if (csw.getTag() != tag) {
            throw new USBException("CSW tag mismatch: expected 0x" + NumberUtils.hex(tag, 8)
                + ", got 0x" + NumberUtils.hex(csw.getTag(), 8));
        }
        int status = csw.getStatus() & 0xFF;
        if (status == US_BULK_CS_CMD_FAILED || status == US_BULK_CS_CMD_WRONG_SEQUENCE) {
            throw new SCSICommandFailedException(status, csw.getResidue());
        }
        int residue = csw.getResidue();
        if (residue != 0) {
            System.err.println("WARNING: CSW residue = " + residue + " on opcode 0x"
                + NumberUtils.hex(cdb.getOpcode(), 2));
        }
    }

    /**
     * Determine whether the given CDB transfers data from the device (IN).
     *
     * @param cdb
     */
    private boolean isDataIn(CDB cdb) {
        int opcode = cdb.getOpcode();
        return opcode == 0x12 || opcode == 0x03 || opcode == 0x1A ||
               opcode == 0x25 || opcode == 0x28 || opcode == 0xA8;
    }

    /**
     * Bulk-Only mass storage reset.
     */
    @Override
    public void reset() throws USBException {
        final USBControlPipe pipe = storageDeviceData.getDevice().getDefaultControlPipe();
        final USBRequest req = pipe.createRequest(new SetupPacket(USB_DIR_OUT
            | USB_TYPE_CLASS | USB_RECIP_INTERFACE, 0xFF, 0, 0, 0), null);
        pipe.syncSubmit(req, GET_TIMEOUT);
    }

    /**
     * Get max logical unit allowed by device. Device not support multiple LUN <i>may</i> stall.
     *
     * @param usbDev
     * @throws USBException
     */
    public void getMaxLun(USBDevice usbDev) throws USBException {
        //log.info("*** Get max lun ***");
        final USBControlPipe pipe = usbDev.getDefaultControlPipe();
        final USBPacket packet = new USBPacket(1);
        final USBRequest req = pipe.createRequest(new SetupPacket(USB_DIR_IN
            | USB_TYPE_CLASS | USB_RECIP_INTERFACE, 0xFE, 0, 0, 1), packet);
        pipe.syncSubmit(req, GET_TIMEOUT);
        //log.debug("*** Request data     : " + req.toString());
        //log.debug("*** Request status   : 0x" + NumberUtils.hex(req.getStatus(), 4));
        if (req.getStatus() == USBREQ_ST_COMPLETED) {
            storageDeviceData.setMaxLun(packet.getData().get8(0));
        } else if (req.getStatus() == USBREQ_ST_STALLED) {
            storageDeviceData.setMaxLun((byte) 0);
        } else {
            throw new USBException("Request status   : 0x" + NumberUtils.hex(req.getStatus(), 4));
        }
    }
}
