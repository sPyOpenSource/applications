package jx.devices;

import jx.devices.pci.PCIAccess;

/**
 * Capability interface: this device provides access to the PCI bus.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code jx.devices.pci.PCIDevice}.
 */
public interface PciCapable {
    PCIAccess getPciAccess();
}
