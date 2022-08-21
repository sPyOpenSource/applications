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

/**
 * @author Chris Cole
 */
public class RxDescriptor extends Descriptor {
    public static final int STATUS_FRAM = 0x2000;
    public static final int STATUS_OFLO = 0x1000;
    public static final int STATUS_CRC  = 0x0800;
    public static final int STATUS_BUFF = 0x0400;

    public RxDescriptor(Memory mem, int offset, int dataBufferOffset) {
        super(mem, offset, dataBufferOffset);

        setOwnerSelf(false);
    }

    public void clearStatus() {
        mem.set16((offset + STATUS) >> 1, (short) STATUS_OWN);
    }

    public short getMessageByteCount() {
        Debug.out.println("b0: " + mem.get8(offset + BCNT));
        Debug.out.println("b1: " + mem.get8(1 + offset + BCNT));
        return mem.get16((offset + BCNT) >> 1);
    }

    public Memory getDataBuffer(MemoryManager memMgr) {
        //byte[] buf = new byte[getMessageByteCount()];
        //Debug.out.println("b: "+buf.length);
        //mem.getBytes(dataBufferOffset, buf, 0, buf.length);
        Memory skbuf = memMgr.alloc(getMessageByteCount());
        skbuf.copyFromMemory(mem, dataBufferOffset, 0, skbuf.size());
        return skbuf;
    }
}
