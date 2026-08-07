package jx.devices.net;

import jx.devices.AbstractDevice;
import jx.devices.DeviceConfiguration;

/**
 * Base class for network interface controllers.
 *
 * <p>Extends {@link AbstractDevice} and implements the legacy
 * {@code NetworkDevice} interface, so NIC implementations only need to
 * supply the network behavior methods and their device configuration.
 */
// implements deprecated NetworkDevice for backward compatibility
@SuppressWarnings("deprecation")
public abstract class AbstractNetworkDevice extends AbstractDevice implements NetworkDevice {

    public static final int RECEIVE_MODE_INDIVIDUAL = 1;
    public static final int RECEIVE_MODE_PROMISCOUS = 2;
    public static final int RECEIVE_MODE_MULTICAST  = 3;

    /**
     * Creates a network device with the given device id.
     */
    protected AbstractNetworkDevice(int deviceId) {
        super(deviceId);
    }

    /**
     * Creates a network device from a config without an explicit device id.
     */
    protected AbstractNetworkDevice(DeviceConfiguration config) {
        super(config);
    }
}
