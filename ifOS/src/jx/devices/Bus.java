package jx.devices;

/**
 * @deprecated Extend {@link AbstractDevice} and compose capability interfaces
 *             ({@link PciCapable}, {@link BlockIOCapable}) instead of implementing this interface.
 */
@Deprecated
public interface Bus extends Device {
    public abstract Device getChild(int index);
}
