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

import jx.zero.Memory;
import metaxa.os.devices.net.EthernetAdress;

/**
 * @author Chris Cole
 */
public class InitializationBlock32Bit {
    public static final int INIT_BLOCK_SIZE = 0x1C;

    private final Memory mem;

    public InitializationBlock32Bit(Memory mem, short mode,
            EthernetAdress physicalAddr, RxDescriptorRing rxRing, 
            TxDescriptorRing txRing, long logicalAddr) {

        this.mem = mem;
        // Populate the initial data structure
        mem.set16(0x00, (short)0);
        mem.set8(0x02, getEncodedRingLength(rxRing.getLength()));
        mem.set8(0x03, getEncodedRingLength(txRing.getLength()));
        mem.set8(0x04, physicalAddr.get(0));
        mem.set8(0x05, physicalAddr.get(1));
        mem.set8(0x06, physicalAddr.get(2));
        mem.set8(0x07, physicalAddr.get(3));
        mem.set8(0x08, physicalAddr.get(4));
        mem.set8(0x09, physicalAddr.get(5));
        mem.set32(0x0C >> 2, 0);//(logicalAddr & 0xFFFFFFFF));
        mem.set32(0x10 >> 2, 0);//((logicalAddr >> 32) & 0xFFFFFFFF));
        mem.set32(0x14 >> 2, rxRing.getAddressAs32());
        mem.set32(0x18 >> 2, txRing.getAddressAs32());
        /*Debug.out.println(rxRing.getAddressAs32());
        Debug.out.println(txRing.getAddressAs32());
        for(int i = 0; i < INIT_BLOCK_SIZE; i++){
            Debug.out.println(mem.get8(i));
        }*/
    }

    private byte getEncodedRingLength(int ringLength) {
        byte encoded = 0;
        while (ringLength != 1) {
            ringLength >>= 1;
            encoded += 1;
        }
        return (byte) (encoded << 4);
    }

    /*public void dumpData(Logger out) {
        out.debug("Intialization Block - 32 bit mode");
        for (int i = 0; i <= INIT_BLOCK_SIZE - 1; i += 4) {
            out.debug(
                    "0x" + NumberUtils.hex(mem.getAddress().toInt() + offset + i) + 
                    " : 0x" + NumberUtils.hex((byte) i) + 
                    " : 0x" + NumberUtils.hex(mem.getInt(offset + i)));
        }
    }*/
}
