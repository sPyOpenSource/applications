package jx.devices;

/**
 * The top-level device interface. All devices must implement this interface.
 * @author Michael Golm
 * @deprecated Extend {@link AbstractDevice} instead of implementing this interface.
 */
@Deprecated
public interface Device {
    /**
     * @return all configurations that are supported by this device.
     */
    DeviceConfigurationTemplate[] getSupportedConfigurations();

    /**
     * Initialize the device.
     * @param conf
     */
    public void open(DeviceConfiguration conf);

    /**
     * Release all resources associated with the physical device.
     */
    public void close();

    public int getId();
}
