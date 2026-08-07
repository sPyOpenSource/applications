package jx.devices;

/**
 * Base class for all devices.
 *
 * <p>Subclasses implement {@link #init(DeviceConfiguration)} for their real
 * setup and override {@link #validateConfig(DeviceConfiguration)} for
 * config validation. Lifecycle is driven by the inherited {@code open()}
 * and {@code close()} methods.
 *
 * <p>Implements the legacy {@code Device} interface so that instances still
 * satisfy {@code instanceof Device} for consumers of the old API.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractDevice implements Device, AutoCloseable {
    protected final int deviceId;
    protected DeviceConfiguration config;

    protected AbstractDevice(int deviceId) {
        this.deviceId = deviceId;
    }

    protected AbstractDevice(DeviceConfiguration config) {
        this.deviceId = 0;
        this.config = config;
    }

    public int getId() {
        return deviceId;
    }

    public void open(DeviceConfiguration conf) {
        validateConfig(conf);
        this.config = conf;
        init(conf);
    }

    public void close() {
        this.config = null;
    }

    protected void validateConfig(DeviceConfiguration conf) {
    }

    protected abstract void init(DeviceConfiguration conf);

    public abstract DeviceConfigurationTemplate[] getSupportedConfigurations();
}
