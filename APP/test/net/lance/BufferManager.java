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
 
package org.jnode.driver.net.lance;

import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import metaxa.os.devices.net.EthernetAdress;

/**
 * @author Chris Cole
 */
public class BufferManager {
    public static final int DATA_BUFFER_SIZE = 2048;

    /**
     * MemoryResource to hold initialization block, descriptor rings, and data buffers
     */
    private final Memory mem;
    private final MemoryManager rm;

    private final InitializationBlock32Bit initBlock;
    private final RxDescriptorRing rxRing;
    private final TxDescriptorRing txRing;

    private final int size;

    public BufferManager(MemoryManager rm, int rxRingLength, int txRingLength, int mode,
                         EthernetAdress physicalAddr, long logicalAddr) {
        // Compute the required size for the memory resource
        size = InitializationBlock32Bit.INIT_BLOCK_SIZE;
        //+ (rxRingLength + txRingLength) * (Descriptor.MESSAGE_DESCRIPTOR_SIZE + DATA_BUFFER_SIZE);

        // Get the memory
        //try {
        this.rm = rm;
        //rm.claimMemoryResource(owner, null, size, ResourceManager.MEMMODE_NORMAL);
        //} catch (ResourceNotFreeException e) {
          //  System.out.println("buffer memory resouce not free exception");
        //}
        // define the offsets into the memory resource for the entities
        final int rxRingOffset = 0;//InitializationBlock32Bit.INIT_BLOCK_SIZE;
        final int txRingOffset = rxRingOffset + (rxRingLength * Descriptor.MESSAGE_DESCRIPTOR_SIZE);
        final int rxDataBufferOffset = txRingOffset + (txRingLength * Descriptor.MESSAGE_DESCRIPTOR_SIZE);
        final int txDataBufferOffset = rxDataBufferOffset + (rxRingLength * DATA_BUFFER_SIZE);
        // Create and initialize the receive ring
        Memory ring = rm.allocAligned((rxRingLength + rxRingLength) * (Descriptor.MESSAGE_DESCRIPTOR_SIZE + DATA_BUFFER_SIZE), 2);
        rxRing = new RxDescriptorRing(ring, rxRingOffset, rxRingLength, rxDataBufferOffset);
        // Create and initialize the transmit ring
        txRing = new TxDescriptorRing(ring, txRingOffset, txRingLength, txDataBufferOffset);
        // Create and initialize the initialization block
        mem = rm.alloc(size);
        initBlock = new InitializationBlock32Bit(
                mem, (short) mode, physicalAddr, rxRing, txRing, logicalAddr);
    }

    /**
     * Gets the address of the initdata structure as a 32-bit int
     * @return 
     */
    public final int getInitDataAddressAs32Bit() {
        return mem.getStartAddress();
    }

    public void transmit(Memory buf) {
        final int len = buf.size();
        if (len > DATA_BUFFER_SIZE) {
            Debug.out.println("Length must be <= " + DATA_BUFFER_SIZE);
        }
        txRing.transmit(buf);
    }

    public Memory getPacket() {
        return rxRing.getPacket(rm);
    }

    /*public void dumpData(Logger out) {
        initBlock.dumpData(out);
        rxRing.dumpData(out);
        txRing.dumpData(out);
    }*/
}
