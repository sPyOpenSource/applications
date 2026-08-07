package jx.devices;

/**
 * Capability interface: this device provides a keyboard and/or mouse.
 * A device that does not provide one of the two returns {@code null}.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code Keyboard} / {@code Mouse} directly.
 */
public interface InputCapable {
    Keyboard getKeyboard();
    Mouse getMouse();
}
