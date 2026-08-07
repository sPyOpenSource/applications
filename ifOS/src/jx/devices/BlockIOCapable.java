package jx.devices;

import jx.devices.bio.BlockIO;

/**
 * Capability interface: this device provides block I/O.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code jx.devices.bio.BlockIO} directly.
 */
public interface BlockIOCapable {
    BlockIO getBlockIO();
}
