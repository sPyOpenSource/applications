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
 
package org.jnode.fs;

import java.nio.file.FileSystemException;
import jx.devices.bio.BlockIO;
import jx.fs.FileSystem;

/**
 * 
 * @author Fabien DUMINY (fduminy at jnode.org)
 * 
 * @param <T> a file system implementation.
 */
public abstract class Formatter<T extends FileSystem> implements Cloneable {
    private final FileSystemType<T> type;

    protected Formatter(FileSystemType<T> type) {
        this.type = type;
    }

    /**
     * Format the given device
     * 
     * @param device The device we want to format
     * 
     * @return the newly created FileSystem
     * 
     * @throws FileSystemException if error occurs during formating of the device
     */
    public abstract T format(BlockIO device) throws FileSystemException;

    /**
     * Gets type of the formated file system.
     * 
     * @return type of the file system.
     */
    public final FileSystemType<T> getFileSystemType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Formatter<T> clone() throws CloneNotSupportedException {
        return (Formatter<T>) super.clone();
    }
}
