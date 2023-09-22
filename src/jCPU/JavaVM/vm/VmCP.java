
package jCPU.JavaVM.vm;

import jx.classfile.constantpool.ClassCPEntry;
import jx.classfile.constantpool.ConstantPool;
import jx.classfile.constantpool.FieldRefCPEntry;
import jx.classfile.constantpool.InterfaceMethodRefCPEntry;
import jx.classfile.constantpool.MethodRefCPEntry;
import jx.classfile.constantpool.StringCPEntry;

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
 
/**
 * A VmCP is the runtime representation of a constant pool
 *
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */

public final class VmCP extends ConstantPool {
    public static final int TYPE_UTF8 = 1;
    public static final int TYPE_INT = 3;
    public static final int TYPE_FLOAT = 4;
    public static final int TYPE_LONG = 5;
    public static final int TYPE_DOUBLE = 6;
    public static final int TYPE_CLASSREF = 7;
    public static final int TYPE_STRING = 8;
    public static final int TYPE_FIELDREF = 9;
    public static final int TYPE_METHODREF = 10;
    public static final int TYPE_IMETHODREF = 11;
    public static final int TYPE_NAMEANDTYPE = 12;

    private final Object[] cp;

    private final int used;

    /**
     * Construct a new VmCP with a given number of entries
     *
     * @param count
     */
    protected VmCP(int count) {
        this.cp = new Object[count];
        this.used = count;
    }

    /**
     * Gets the number of enntries in this CP
     *
     * @return int
     */
    public int getLength() {
        return cp.length;
    }

    /**
     * Read an int out of this CP
     *
     * @param index The index where to read
     * @return int
     */
    /*public int getInt(int index) {
        if (index == 0)
            return 0;
        else
            return ((VmConstInt) get(index)).intValue();
    }*/

    /**
     * Write an int into this CP
     *
     * @param index The index where to read
     * @param data  The int to write
     */
    /*protected void setInt(int index, int data) {
        set(index, new VmConstInt(data));
    }*/

    /**
     * Read a long out of this CP
     *
     * @param index The index where to read
     * @return long
     */
    /*public long getLong(int index) {
        return ((VmConstLong) get(index)).longValue();
    }*/

    /**
     * Write a long into this CP
     *
     * @param index The index where to read
     * @param data  The long to write
     */
    /*protected void setLong(int index, long data) {
        set(index, new VmConstLong(data));
    }*/

    /**
     * Read a float out of this CP
     *
     * @param index The index where to read
     * @return float
     */
    /*public float getFloat(int index) {
        return ((VmConstFloat) get(index)).floatValue();
    }*/

    /**
     * Write a float into this CP
     *
     * @param index The index where to read
     * @param data  The float to write
     */
    /*protected void setFloat(int index, float data) {
        set(index, new VmConstFloat(data));
    }*/

    /**
     * Read a double out of this CP
     *
     * @param index The index where to read
     * @return double
     */
    public double getDouble(int index) {
        return (Double) get(index);
    }

    /**
     * Write a double into this CP
     *
     * @param index The index where to read
     * @param data  The double to write
     */
    /*protected void setDouble(int index, double data) {
        set(index, new VmConstDouble(data));
    }*/

    protected String getUTF8(int index) {
        return (String) get(index);
    }

    /*protected void setUTF8(int index, String value) {
        set(index, InternString.internString(value));
    }*/

    public StringCPEntry getString(int index) {
        return (StringCPEntry) get(index);
    }

    protected void setString(int index, StringCPEntry value) {
        set(index, value);
    }

    public ClassCPEntry getConstClass(int index) {
        return (ClassCPEntry) get(index);
    }

    protected void setConstClass(int index, ClassCPEntry value) {
        set(index, value);
    }

    public FieldRefCPEntry getConstFieldRef(int index) {
        return (FieldRefCPEntry) get(index);
    }

    protected void setConstFieldRef(int index, FieldRefCPEntry value) {
        set(index, value);
    }

    public MethodRefCPEntry getConstMethodRef(int index) {
        return (MethodRefCPEntry) get(index);
    }

    protected void setConstMethodRef(int index, MethodRefCPEntry value) {
        set(index, value);
    }

    public InterfaceMethodRefCPEntry getConstIMethodRef(int index) {
        return (InterfaceMethodRefCPEntry) get(index);
    }

    protected void setConstIMethodRef(int index, InterfaceMethodRefCPEntry value) {
        set(index, value);
    }

    public final Object getAny(int index) {
        return get(index);
    }

    /**
     * Gets the index of a constant in this CP, or -1 if not found.
     *
     * @param object
     * @return int
     */
    public final int indexOf(Object object) {
        for (int i = 0; i < used; i++) {
            final Object o = cp[i];
            if ((o != null) && (o.equals(object))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Read an Object out of this CP
     *
     * @param index The index where to read
     * @return Object
     */
    private Object get(int index) {
        return cp[index];
    }

    /**
     * Write an Object into this CP
     *
     * @param index The index where to read
     * @param data  The Object to write
     */
    private void set(int index, Object data) {
        if (data == null) {
            throw new NullPointerException(
                "Cannot set a null data");
        }
        cp[index] = data;
    }

    final void reset(int index) {
        cp[index] = null;
    }
}
