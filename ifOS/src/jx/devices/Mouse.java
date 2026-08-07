package jx.devices;

/**
 * Mouse device interface.
 *
 * <p>Composition note: do not implement this interface directly. Expose a {@code Mouse}
 * via {@link InputCapable#getMouse()} on an {@link AbstractDevice} subclass.</p>
 */
public interface Mouse extends jx.zero.Portal {
}
