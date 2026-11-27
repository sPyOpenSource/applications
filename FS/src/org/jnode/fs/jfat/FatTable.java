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
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; If not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 */
 
package org.jnode.fs.jfat;

import java.util.Iterator;
import java.util.Vector;

public class FatTable {
    // Use a linear Vector instead of HashMap: the JCore runtime's HashMap
    // is not thread-safe and synchronized modifiers are ignored by the
    // compiler, so concurrent put() calls from multiple threads corrupt
    // its internal state.  Directory entry counts are small, so linear
    // search is acceptable.
    private final Vector<FatEntry> entries = new Vector<>();

    public FatTable() {
    }

    public FatEntry get(String name) {
        for (int i = 0; i < entries.size(); i++) {
            FatEntry e = entries.elementAt(i);
            if (FatUtils.compareIgnoreCase(e.getName(), name))
                return e;
        }
        return null;
    }

    public FatEntry put(FatEntry entry) {
        if (get(entry.getName()) != null)
            throw new IllegalArgumentException("shouldn't happen");
        entries.addElement(entry);
        return entry;
    }

    public FatEntry remove(FatEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.elementAt(i) == entry) {
                entries.removeElementAt(i);
                return entry;
            }
        }
        throw new IllegalArgumentException("shouldn't happen");
    }

    public FatEntry put(String name, FatEntry entry) {
        if (get(name) != null)
            throw new IllegalArgumentException("shouldn't happen");
        entries.addElement(entry);
        return entry;
    }

    public FatEntry look(FatEntry entry) {
        FatEntry e = get(entry.getName());

        if (e != null)
            return e;
        else
            return put(entry);
    }

    public int size() {
        return entries.size();
    }
}
