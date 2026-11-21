package jCPU.x86;

/**
 * USB root hub device.
 *
 * <p>A root hub is the built-in hub of a USB host controller.  It responds to
 * standard hub class requests (GetStatus, ClearFeature, SetFeature for port
 * reset/power/connect-status-change) and reports its device descriptor as a
 * hub class device.</p>
 */
public class X86UsbHub extends X86UsbDevice
{
    private final int portCount;
    private final boolean[] portConnected;  // device attached to port
    private final boolean[] portEnabled;    // port enabled
    private final boolean[] portPowered;    // port powered on
    private final boolean[] portCsc;        // connect status change
    private final boolean[] portReset;      // port reset active

    public X86UsbHub(int portCount)
    {
        super(0x0200, 0x09, 0x00, 0x01, 8,   // hub class, full speed, 8-byte EP0
              0x0000, 0x0000, 0x0000,        // vendor/product/device
              0, 0, 0, 1);                   // no strings, 1 configuration
        this.portCount = portCount;
        this.portConnected = new boolean[portCount];
        this.portEnabled = new boolean[portCount];
        this.portPowered = new boolean[portCount];
        this.portCsc = new boolean[portCount];
        this.portReset = new boolean[portCount];
    }

    public int getPortCount() { return portCount; }

    public boolean isPortConnected(int port)
    {
        return port >= 0 && port < portCount && portConnected[port];
    }

    public void setPortConnected(int port, boolean connected)
    {
        if (port < 0 || port >= portCount) return;
        portConnected[port] = connected;
        portCsc[port] = true; // signal connect status change
    }

    public boolean isPortEnabled(int port)
    {
        return port >= 0 && port < portCount && portEnabled[port];
    }

    public void setPortEnabled(int port, boolean enabled)
    {
        if (port < 0 || port >= portCount) return;
        portEnabled[port] = enabled;
    }

    public boolean isPortPowered(int port)
    {
        return port >= 0 && port < portCount && portPowered[port];
    }

    public void setPortPowered(int port, boolean powered)
    {
        if (port < 0 || port >= portCount) return;
        portPowered[port] = powered;
    }

    public boolean isPortCsc(int port)
    {
        return port >= 0 && port < portCount && portCsc[port];
    }

    public void clearPortCsc(int port)
    {
        if (port >= 0 && port < portCount) portCsc[port] = false;
    }

    public boolean isPortReset(int port)
    {
        return port >= 0 && port < portCount && portReset[port];
    }

    public void setPortReset(int port, boolean reset)
    {
        if (port < 0 || port >= portCount) return;
        portReset[port] = reset;
        if (!reset && portConnected[port])
        {
            portEnabled[port] = true;
            portCsc[port] = true; // reset complete change
        }
    }

    @Override
    public byte[] getConfigurationDescriptor()
    {
        // Hub class configuration: 1 interface, endpoint for status change notifications
        int totalLength = 9 + 9 + 7; // config + interface + endpoint
        byte[] d = new byte[totalLength];
        int i = 0;
        // Configuration descriptor
        d[i++] = (byte) 9;
        d[i++] = 0x02;
        d[i++] = (byte) (totalLength & 0xFF);
        d[i++] = (byte) (totalLength >> 8);
        d[i++] = 1; // bNumInterfaces
        d[i++] = 1; // bConfigurationValue
        d[i++] = 0; // iConfiguration
        d[i++] = (byte)0xE0; // self powered
        d[i++] = 0; // bMaxPower
        // Interface descriptor (hub class)
        d[i++] = (byte) 9;
        d[i++] = 0x04;
        d[i++] = 0; // bInterfaceNumber
        d[i++] = 0; // bAlternateSetting
        d[i++] = 1; // bNumEndpoints
        d[i++] = 0x09; // HUB class
        d[i++] = 0x00; // no subclass
        d[i++] = 0x00; // no protocol
        d[i++] = 0; // iInterface
        // Endpoint descriptor (interrupt IN for status changes)
        d[i++] = (byte) 7;
        d[i++] = 0x05;
        d[i++] = (byte) 0x81; // EP1 IN
        d[i++] = 0x03; // Interrupt
        d[i++] = 0x08; // max packet 8
        d[i++] = 0x00;
        d[i++] = (byte)0xFF; // polling every 255ms
        return d;
    }

    @Override
    public byte[] handleControlRequest(byte bmRequestType, byte bRequest,
                                       int wValue, int wIndex, int wLength)
    {
        // Recipient = DEVICE (0x00)
        if ((bmRequestType & 0x1F) == 0x00)
        {
            switch (bRequest)
            {
                case 0x00: // GET_STATUS
                    return new byte[]{0x00, 0x00};
                case 0x05: // SET_ADDRESS
                    setAddress(wValue & 0xFF);
                    return null; // status stage only
                case 0x06: // GET_DESCRIPTOR
                    int descType = wValue >> 8;
                    int descIndex = wValue & 0xFF;
                    if (descType == 0x01) // DEVICE
                    {
                        return getDeviceDescriptor();
                    }
                    if (descType == 0x02) // CONFIGURATION
                    {
                        return getConfigurationDescriptor();
                    }
                    if (descType == 0x03 && descIndex == 0) // STRING (lang IDs)
                    {
                        return new byte[]{0x04, 0x03, 0x09, 0x04};
                    }
                    return null;
            }
        }

        // Recipient = OTHER (0x1E) could be port; Recipient = INTERFACE/ENDPOINT
        if ((bmRequestType & 0x1F) == 0x1E || (bmRequestType & 0x1F) == 0x00)
        {
            // Hub class requests use recipient = OTHER (0x1E) with class bit
            if ((bmRequestType & 0x60) == 0x20) // class request
            {
                int port = wIndex & 0xFF;
                switch (bRequest)
                {
                    case 0x00: // GET_STATUS (hub)
                    {
                        if ((bmRequestType & 0x1F) == 0x00) // hub status
                        {
                            return new byte[]{
                                (byte) ((portPowered[0] ? 0x01 : 0x00)),
                                (byte) ((portCsc[0] ? 0x01 : 0x00))};
                        }
                        break;
                    }
                    case 0x01: // CLEAR_FEATURE
                    {
                        if ((bmRequestType & 0x1F) == 0x00) // hub
                        {
                            if (wValue == 0x0001) // C_HUB_LOCAL_POWER
                            { }
                            else if (wValue == 0x0002) // C_HUB_OVER_CURRENT
                            { }
                        }
                        else if (port >= 1 && port <= portCount) // port
                        {
                            if (wValue == 0x0001) // PORT_CONNECTION
                            { }
                            else if (wValue == 0x0002) // PORT_ENABLE
                            {
                                setPortEnabled(port - 1, false);
                            }
                            else if (wValue == 0x0008) // PORT_RESET
                            { }
                            else if (wValue == 0x0010) // PORT_POWER
                            {
                                setPortPowered(port - 1, false);
                            }
                            else if (wValue == 0x0012) // C_PORT_RESET
                            { }
                            else if (wValue == 0x0014) // C_PORT_CONNECTION
                            {
                                clearPortCsc(port - 1);
                            }
                        }
                        return null; // status stage
                    }
                    case 0x03: // SET_FEATURE
                    {
                        if ((bmRequestType & 0x1F) == 0x00) // hub
                        {
                            // nothing to set at hub level
                        }
                        else if (port >= 1 && port <= portCount) // port
                        {
                            if (wValue == 0x0002) // PORT_ENABLE
                            {
                                setPortEnabled(port - 1, true);
                            }
                            else if (wValue == 0x0004) // PORT_SUSPEND
                            { }
                            else if (wValue == 0x0008) // PORT_RESET
                            {
                                setPortReset(port - 1, true);
                            }
                            else if (wValue == 0x0009) // PORT_POWER
                            {
                                setPortPowered(port - 1, true);
                            }
                        }
                        return null; // status stage
                    }
                    case 0x06: // GET_DESCRIPTOR (HUB descriptor)
                    {
                        int hubDescType = wValue >> 8;
                        if (hubDescType == 0x29) // HUB descriptor
                        {
                            return buildHubDescriptor();
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }

    private byte[] buildHubDescriptor()
    {
        // Hub descriptor (class-specific)
        byte[] d = new byte[7 + (portCount + 1) / 2];
        int i = 0;
        d[i++] = (byte) d.length; // bDescLength
        d[i++] = 0x29; // bDescriptorType = HUB
        d[i++] = (byte) portCount; // bNbrPorts
        d[i++] = 0x0A; // wHubCharacteristics (0x000A)
        d[i++] = 0x00;
        d[i++] = (byte) (bMaxPacketSize0 / 2); // bPwrOn2PwrGood (2ms)
        d[i++] = 0x00; // bHubContrCurrent
        // Device removable flags (one bit per port, byte aligned)
        int byteCount = (portCount + 1 + 7) / 8;
        for (int b = 0; b < byteCount; b++)
        {
            d[i++] = 0x00; // all ports not removable
        }
        // PortPWRCtrlMask (one byte per port)
        for (int p = 0; p < portCount; p++)
        {
            d[i++] = 0x00;
        }
        return d;
    }
}
