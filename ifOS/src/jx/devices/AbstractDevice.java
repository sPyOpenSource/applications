package jx.devices;

/**
 * Base class for all devices.
 *
 * <p>Lifecycle is driven by {@code open()} (validate → init) and
 * {@code close()} (deinit). Subclasses override {@link #init(DeviceConfiguration)}
 * and {@link #deinit()} for real setup/teardown and
 * {@link #validateConfig(DeviceConfiguration)} for config validation.
 *
 * <p>Implements the legacy {@code Device} interface so that instances still
 * satisfy {@code instanceof Device} for consumers of the old API.
 */
// implements deprecated Device so instances satisfy instanceof Device for legacy consumers
@SuppressWarnings("deprecation")
public abstract class AbstractDevice implements Device, AutoCloseable {
    protected final int deviceId;
    protected DeviceConfiguration config;

    /**
     * Creates a device with the given device id.
     *
     * <p>The {@link #AbstractDevice(DeviceConfiguration)} constructor leaves
     * {@code deviceId} at {@code 0}.
     */
    protected AbstractDevice(int deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Creates a device from a config without an explicit device id.
     *
     * <p>This constructor leaves {@code deviceId} at {@code 0}, so
     * {@link #getId()} returns {@code 0} until a real id is available.
     */
    protected AbstractDevice(DeviceConfiguration config) {
        this.deviceId = 0;
        this.config = config;
    }

    /**
     * Returns the device id ({@code 0} when created from config only).
     */
    public int getId() {
        return deviceId;
    }

    /**
     * Opens the device with the given configuration.
     *
     * <p>Must be called exactly once; the base does not guard against double
     * invocation. Overriding it without calling {@code super.open(...)}
     * bypasses validation, for advanced subclasses.
     */
    public void open(DeviceConfiguration conf) {
        validateConfig(conf);
        this.config = conf;
        init(conf);
    }

    /**
     * Closes the device, releasing its configuration.
     */
    public void close() {
        deinit();
        this.config = null;
    }

    /**
     * Validates a configuration before the device is opened.
     *
     * <p>Runs BEFORE config is stored, so it cannot inspect the previously
     * held config.
     */
    protected void validateConfig(DeviceConfiguration conf) {
    }

    /**
     * Performs the real device setup for the given configuration.
     */
    protected abstract void init(DeviceConfiguration conf);

    /**
     * Performs device teardown; called by {@link #close()} before the config
     * is released. Intended as a no-op hook for subclasses to override.
     */
    protected void deinit() {
    }

    /**
     * Returns the configurations this device supports.
     */
    public abstract DeviceConfigurationTemplate[] getSupportedConfigurations();
}
