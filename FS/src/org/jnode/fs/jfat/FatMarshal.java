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

import jx.zero.Memory;
import org.jnode.util.NumberUtils;

/**
 * @author gvt
 */
public class FatMarshal {
    private final byte[] array;
    private boolean dirty = false;

    public FatMarshal(Memory array) {
        if (array == null)
            throw new NullPointerException("array cannot be null");
        this.array = new byte[array.size()];
        array.copyToByteArray(this.array, 0, 0, this.array.length);
    }
    
    public FatMarshal(int length) {
        array = new byte[length];
    }

    public int length() {
        return array.length;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        dirty = true;
    }

    public void flush() {
        dirty = false;
    }

    private void checkOffset(int offset, int length) {
        if (offset < 0)
            throw new IndexOutOfBoundsException("offset[" + offset + "] cannot be negative");

        if (length <= 0)
            throw new IndexOutOfBoundsException("length[" + length + "] has to be positive");

        if (offset > (array.length - length))
            throw new IndexOutOfBoundsException("length[" + length + "] + offset[" + offset +
                "] >" + "array.length[" + array.length + "]");
    }

    public byte get(int offset) {
        checkOffset(offset, 1);
        return array[offset];
    }

    public void put(int offset, byte value) {
        checkOffset(offset, 1);
        array[offset] = value;
        setDirty();
    }

    public int getUInt8(int offset) {
        checkOffset(offset, 1);
        return array[offset];
    }

    public void setUInt8(int offset, int value) {
        checkOffset(offset, 1);
        array[offset] = (byte) value;
        setDirty();
    }

    public int getUInt16(int offset) {
        checkOffset(offset, 2);
        return (short) ((array[offset] & 0xFF) | ((array[offset + 1] & 0xFF) << 8));
    }

    public void setUInt16(int offset, int value) {
        checkOffset(offset, 2);
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >>> 8);
        setDirty();
    }

    public int getUInt32(int offset) {
        checkOffset(offset, 4);
        return (array[offset] & 0xFF)
            | ((array[offset + 1] & 0xFF) << 8)
            | ((array[offset + 2] & 0xFF) << 16)
            | ((array[offset + 3] & 0xFF) << 24);
    }

    public void setUInt32(int offset, int value) {
        checkOffset(offset, 4);
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >>> 8);
        array[offset + 2] = (byte) (value >>> 16);
        array[offset + 3] = (byte) (value >>> 24);
        setDirty();
    }

    public String getString(int offset, int length) {
        checkOffset(offset, length);

        StringBuilder b = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            b.append((char) array[offset + i]);
        }
        return b.toString();
    }

    public void setString(int offset, int length, String value) {
        checkOffset(offset, length);

        for (int i = 0; i < length; i++) {
            char ch;
            if (i < value.length())
                ch = value.charAt(i);
            else
                ch = (char) 0;
            array[offset + i] = (byte) ch;
        }
        setDirty();
    }

    public char[] getChars(int offset, int length) {
        checkOffset(offset, length);

        char[] value = new char[length];
        for (int i = 0; i < length; i++) {
            value[i] = (char) array[offset + i];
        }
        return value;
    }

    public void setChars(int offset, int length, char[] value) {
        checkOffset(offset, length);

        for (int i = 0; i < length; i++){
            array[offset + i] = (byte) value[i];
        }
        setDirty();
    }

    public Memory getByteBuffer() {
        return null;
    }
    
    public byte[] getBytes(int offset, int length) {
        byte[] value = new byte[length];
        getBytes(offset, length, 0, value);
        return value;
    }

    public void setBytes(int offset, int length, byte[] value) {
        setBytes(offset, length, 0, value);
    }

    public void getBytes(int offset, int length, int start, byte[] dst) {
        checkOffset(offset, length);
        System.arraycopy(array, offset, dst, start, length);
    }

    public void setBytes(int offset, int length, int start, byte[] src) {
        checkOffset(offset, length);
        System.arraycopy(src, start, array, offset, length);
        setDirty();
    }

    @Override
    public String toString() {
        return String.format("FatMarshal %s", NumberUtils.hex(array));
    }
}
