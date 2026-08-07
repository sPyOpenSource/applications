package jx.devices;

import jx.zero.Portal;

/**
 * Keyboard device interface.
 *
 * <p>Composition note: do not implement this interface directly. Expose a {@code Keyboard}
 * via {@link InputCapable#getKeyboard()} on an {@link AbstractDevice} subclass.</p>
 */
public interface Keyboard extends Portal {
    public void addKeyListener(KeyListener listener);
    public int getc();
    public int getcode();
}
