package jCPU.x86;

/**
 * Base class for a USB device.
 *
 * <p>Holds the standard USB descriptors (device, configuration, string) and
 * the device address assigned by the host during enumeration.  Provides the
 * default-control pipe (endpoint 0) handling that every USB device must
 * support.</p>
 */
public abstract class X86UsbDevice
{
    /** USB device descriptor fields */
    private final int bcdUSB;          // USB specification release number
    private final int bDeviceClass;
    private final int bDeviceSubClass;
    private final int bDeviceProtocol;
    final int bMaxPacketSize0; // max packet size for endpoint 0
    private final int idVendor;
    private final int idProduct;
    private final int bcdDevice;
    private final int iManufacturer;   // string descriptor index
    private final int iProduct;
    private final int iSerialNumber;
    private final int bNumConfigurations;

    /** USB address assigned by the host (0 = default/unconfigured) */
    private int address;

    /** Current configuration value */
    private int configuration;

    public X86UsbDevice(int bcdUSB, int bDeviceClass, int bDeviceSubClass,
                        int bDeviceProtocol, int bMaxPacketSize0,
                        int idVendor, int idProduct, int bcdDevice,
                        int iManufacturer, int iProduct, int iSerialNumber,
                        int bNumConfigurations)
    {
        this.bcdUSB = bcdUSB;
        this.bDeviceClass = bDeviceClass;
        this.bDeviceSubClass = bDeviceSubClass;
        this.bDeviceProtocol = bDeviceProtocol;
        this.bMaxPacketSize0 = bMaxPacketSize0;
        this.idVendor = idVendor;
        this.idProduct = idProduct;
        this.bcdDevice = bcdDevice;
        this.iManufacturer = iManufacturer;
        this.iProduct = iProduct;
        this.iSerialNumber = iSerialNumber;
        this.bNumConfigurations = bNumConfigurations;
        this.address = 0;
        this.configuration = 0;
    }

    public int getBcdUSB() { return bcdUSB; }
    public int getDeviceClass() { return bDeviceClass; }
    public int getDeviceSubClass() { return bDeviceSubClass; }
    public int getDeviceProtocol() { return bDeviceProtocol; }
    public int getMaxPacketSize0() { return bMaxPacketSize0; }
    public int getIdVendor() { return idVendor; }
    public int getIdProduct() { return idProduct; }
    public int getBcdDevice() { return bcdDevice; }
    public int getNumConfigurations() { return bNumConfigurations; }

    public int getAddress() { return address; }
    public void setAddress(int address) { this.address = address; }

    public int getConfiguration() { return configuration; }
    public void setConfiguration(int configuration) { this.configuration = configuration; }

    /** Build the 18-byte USB device descriptor. */
    public byte[] getDeviceDescriptor()
    {
        byte[] d = new byte[18];
        d[0] = 18;                      // bLength
        d[1] = 0x01;                    // bDescriptorType = DEVICE
        d[2] = (byte) (bcdUSB & 0xFF);
        d[3] = (byte) (bcdUSB >> 8);    // bcdUSB
        d[4] = (byte) bDeviceClass;
        d[5] = (byte) bDeviceSubClass;
        d[6] = (byte) bDeviceProtocol;
        d[7] = (byte) bMaxPacketSize0;
        d[8] = (byte) (idVendor & 0xFF);
        d[9] = (byte) (idVendor >> 8);
        d[10] = (byte) (idProduct & 0xFF);
        d[11] = (byte) (idProduct >> 8);
        d[12] = (byte) (bcdDevice & 0xFF);
        d[13] = (byte) (bcdDevice >> 8);
        d[14] = (byte) iManufacturer;
        d[15] = (byte) iProduct;
        d[16] = (byte) iSerialNumber;
        d[17] = (byte) bNumConfigurations;
        return d;
    }

    /** Build the configuration descriptor (subclasses override for real configs). */
    public byte[] getConfigurationDescriptor()
    {
        // Configuration descriptor (9 bytes) + one Interface descriptor (9 bytes)
        int totalLength = 9 + 9;
        byte[] d = new byte[totalLength];
        int i = 0;
        // Configuration descriptor
        d[i++] = (byte) 9;              // bLength
        d[i++] = 0x02;                  // bDescriptorType = CONFIGURATION
        d[i++] = (byte) (totalLength & 0xFF);
        d[i++] = (byte) (totalLength >> 8); // wTotalLength
        d[i++] = 1;                     // bNumInterfaces
        d[i++] = 1;                     // bConfigurationValue
        d[i++] = 0;                     // iConfiguration
        d[i++] = (byte)0x80;                  // bmAttributes (bus powered)
        d[i++] = (byte) (bMaxPacketSize0 / 2); // bMaxPower (2 mA units)
        // Interface descriptor
        d[i++] = (byte) 9;              // bLength
        d[i++] = 0x04;                  // bDescriptorType = INTERFACE
        d[i++] = 0;                     // bInterfaceNumber
        d[i++] = 0;                     // bAlternateSetting
        d[i++] = 0;                     // bNumEndpoints (besides endpoint 0)
        d[i++] = (byte) bDeviceClass;
        d[i++] = (byte) bDeviceSubClass;
        d[i++] = (byte) bDeviceProtocol;
        d[i++] = (byte) iProduct;       // iInterface
        return d;
    }

    /** Handle a standard device request on endpoint 0.  Returns the response
     *  data (or null for status-stage-only requests). */
    public abstract byte[] handleControlRequest(byte bmRequestType, byte bRequest,
                                                int wValue, int wIndex, int wLength);

    /** Optional: device has been attached to the bus. */
    public void attached() {}

    /** Optional: device has been detached from the bus. */
    public void detached() {}
}
