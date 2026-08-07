package jx.devices;

import jx.zero.DeviceMemory;
import jx.zero.Portal;

/**
 * Screen (display) device interface.
 *
 * <p>Composition note: do not implement this interface directly. Expose a {@code Screen}
 * via {@link DisplayCapable#getScreen()} on an {@link AbstractDevice} subclass.</p>
 */
public interface Screen extends Portal {
    public int getWidth();
    public int getHeight();
    public void moveCursorTo(int x, int y);
    public void putAt(int x, int y, char c);
    public void clear();
    public DeviceMemory getVideoMemory();
}
