
package jx.devices.pci;

/**
 *
 * @author xuyi
 */
public interface PCIAddress {
   public int getBus();
   public int getDevice();
   public int getFunction();
}
