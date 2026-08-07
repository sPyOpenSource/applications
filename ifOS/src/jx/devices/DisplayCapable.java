package jx.devices;

/**
 * Capability interface: this device provides a display.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code Screen} directly.
 */
public interface DisplayCapable {
    Screen getScreen();
}
