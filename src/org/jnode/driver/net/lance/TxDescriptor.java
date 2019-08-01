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

/**
 * @author Chris Cole
 */
public class TxDescriptor extends Descriptor {
    public TxDescriptor(Memory mem, int offset, int dataBufferOffset) {
        super(mem, offset, dataBufferOffset);

        setOwnerSelf(true);
    }

    public void transmit(Memory skbuf) {
        // fill the data buffer
        mem.copyFromMemory(skbuf, 0, dataBufferOffset, skbuf.size());
        setStatus((short) (STATUS_OWN | STATUS_STP | STATUS_ENP));
        setByteCount(skbuf.size());
        //mem.set32((offset + 0x04) >> 2, 0x8300F000 | (-skbuf.size() & 0xFFF));
        /*Debug.out.println(mem.get32(offset >> 2));
        Debug.out.println(mem.get32((offset+0x04) >> 2));
        Debug.out.println(mem.get32((offset+0x08) >> 2));
        Debug.out.println(mem.get32((offset+0x0c) >> 2));*/
        /*for(int i = 0; i < 40; i++){
            Debug.out.println(mem.get8(i + offset));
        }*/
    }

    private void setByteCount(int bcnt) {
        mem.set16((offset + 0x04) >> 1, (short) (-bcnt));
    }
}
