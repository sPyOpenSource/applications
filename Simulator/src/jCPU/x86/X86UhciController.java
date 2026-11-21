package jCPU.x86;

import jCPU.Peripheral;

/**
 * UHCI (Universal Host Controller Interface) USB host controller.
 *
 * <p>Intel's USB 1.1 host controller interface.  Registers are accessed
 * through I/O ports starting at a configurable base port.  The controller
 * manages a root hub, enumerates attached devices, and processes control
 * transfers on endpoint 0.</p>
 *
 * <pre>
 *  Port offset  Register
 *  0x00-0x01    USBCMD  - USB Command
 *  0x02-0x03    USBSTS  - USB Status
 *  0x04-0x05    USBINTR - USB Interrupt Enable
 *  0x06-0x07    FRNUM   - Frame Number
 *  0x08-0x0B    FLBASEADD - Frame List Base Address (physical memory)
 *  0x0C-0x0D    SOFMOD  - Start of Frame Modify
 *  0x0E-0x0F    PORTSC1 - Port 1 Status/Control
 *  0x10-0x11    PORTSC2 - Port 2 Status/Control  (implementation-defined)
 * </pre>
 */
public class X86UhciController implements Peripheral
{
    /* Port offsets from base */
    private static final int REG_USBCMD     = 0x00;
    private static final int REG_USBSTS     = 0x02;
    private static final int REG_USBINTR    = 0x04;
    private static final int REG_FRNUM      = 0x06;
    private static final int REG_FLBASEADD  = 0x08;
    private static final int REG_SOFMOD     = 0x0C;
    private static final int REG_PORTSC1    = 0x0E;
    private static final int REG_PORTSC2    = 0x10;

    /* USBCMD bits */
    private static final int CMD_RS      = 0x0001; // Run/Stop
    private static final int CMD_HCRESET = 0x0002; // Host Controller Reset
    private static final int CMD_GRE     = 0x0004; // Global Reset Enable
    private static final int CMD_FGR     = 0x0010; // Force Global Resume

    /* USBSTS bits */
    private static final int STS_USBI   = 0x0001; // USB Interrupt
    private static final int STS_ERROR  = 0x0002; // USB Error Interrupt
    private static final int STS_RD     = 0x0004; // Resume Detect
    private static final int STS_HCPE   = 0x0008; // Host Controller Error
    private static final int STS_HCH    = 0x0010; // Host Controller Halted
    private static final int STS_STC    = 0x0020; // Schedule Tick

    /* PortSC bits */
    private static final int PORT_CSC   = 0x0001; // Connect Status Change
    private static final int PORT_PEDC  = 0x0002; // Port Enable/Disable Change
    private static final int PORT_PED   = 0x0004; // Port Enable/Disable
    private static final int PORT_LSDA  = 0x0008; // Low Speed Device Attached
    private static final int PORT_RD    = 0x0010; // Resume Detect
    private static final int PORT_PRS   = 0x0020; // Port Reset
    private static final int PORT_POCI  = 0x0040; // Port Over Current Change
    private static final int PORT_PS    = 0x0080; // Port Suspend
    private static final int PORT_PR    = 0x0100; // Port Reset (write)
    private static final int PORT_SUSPEND = 0x0200;
    private static final int PORT_LS    = 0x0400; // Line Status

    private final int basePort;
    private final int portCount;

    private final X86UsbHub rootHub;

    /* UHCI registers */
    private int usbCmd;
    private int usbSts;
    private int usbIntr;
    private int frNum;
    private int flBaseAddr;     // memory address of frame list
    private int sofMod;

    private int frameCounter;

    private boolean running;

    public X86UhciController(int basePort, int portCount)
    {
        this.basePort = basePort;
        this.portCount = portCount;
        this.rootHub = new X86UsbHub(portCount);

        this.usbCmd = 0;
        this.usbSts = STS_HCH; // halted initially
        this.usbIntr = 0;
        this.frNum = 0;
        this.flBaseAddr = 0;
        this.sofMod = 0x40;
        this.frameCounter = 0;
        this.running = false;
    }

    public X86UsbHub getRootHub() { return rootHub; }

    /** Get the I/O port range length. */
    public int getPortCount() { return portCount; }

    /** Called by the simulator each frame to advance the controller. */
    public void tick()
    {
        if (!running) return;

        frameCounter++;
        frNum = (frNum + 1) & 0x7FF;

        // Check root hub ports for connect status changes
        for (int p = 0; p < portCount; p++)
        {
            if (rootHub.isPortCsc(p))
            {
                usbSts |= STS_USBI;
                if ((usbIntr & 0x0001) != 0)
                {
                    assertInterrupt();
                }
                // Set CSC bit in port status
                // (handled when port register read returns)
            }
        }

        // Process control transfers on endpoint 0 for attached devices
        for (int p = 0; p < portCount; p++)
        {
            if (rootHub.isPortConnected(p) && rootHub.isPortEnabled(p))
            {
                // Process any pending control request (simplified: just tick)
            }
        }
    }

    /** Assert an interrupt to the CPU (hook for interrupt controller). */
    protected void assertInterrupt()
    {
        // USB IRQ typically routed to PIC IRQ 10/11 or via IOAPIC.
        // Subclass or simulator integration can override to deliver the IRQ.
    }

    /* ---------------- Peripheral interface ---------------- */

    @Override
    public byte read(int port)
    {
        int offset = port - basePort;
        switch (offset)
        {
            case REG_USBCMD:
                return (byte) (usbCmd & 0xFF);
            case REG_USBCMD + 1:
                return (byte) (usbCmd >> 8);

            case REG_USBSTS:
                return (byte) (usbSts & 0xFF);
            case REG_USBSTS + 1:
                return (byte) (usbSts >> 8);

            case REG_USBINTR:
                return (byte) (usbIntr & 0xFF);
            case REG_USBINTR + 1:
                return (byte) (usbIntr >> 8);

            case REG_FRNUM:
                return (byte) (frNum & 0xFF);
            case REG_FRNUM + 1:
                return (byte) (frNum >> 8);

            case REG_FLBASEADD:
                return (byte) (flBaseAddr & 0xFF);
            case REG_FLBASEADD + 1:
                return (byte) ((flBaseAddr >> 8) & 0xFF);
            case REG_FLBASEADD + 2:
                return (byte) ((flBaseAddr >> 16) & 0xFF);
            case REG_FLBASEADD + 3:
                return (byte) ((flBaseAddr >> 24) & 0xFF);

            case REG_SOFMOD:
                return (byte) (sofMod & 0xFF);
            case REG_SOFMOD + 1:
                return (byte) (sofMod >> 8);

            case REG_PORTSC1:
                return portStatusByte(0, 0);
            case REG_PORTSC1 + 1:
                return portStatusByte(0, 1);

            case REG_PORTSC2:
                return portStatusByte(1, 0);
            case REG_PORTSC2 + 1:
                return portStatusByte(1, 1);

            default:
                return 0;
        }
    }

    @Override
    public void write(int port, byte value)
    {
        int v = value & 0xFF;
        int offset = port - basePort;

        switch (offset)
        {
            case REG_USBCMD:
                usbCmd = (usbCmd & 0xFF00) | v;
                handleUsbCmd();
                break;
            case REG_USBCMD + 1:
                usbCmd = (usbCmd & 0x00FF) | (v << 8);
                handleUsbCmd();
                break;

            case REG_USBSTS:
                // Writing 1 to status bits clears them (write-1-to-clear)
                usbSts &= ~(v & 0xFF);
                break;
            case REG_USBSTS + 1:
                usbSts &= ~(v << 8);
                break;

            case REG_USBINTR:
                usbIntr = (usbIntr & 0xFF00) | v;
                break;
            case REG_USBINTR + 1:
                usbIntr = (usbIntr & 0x00FF) | (v << 8);
                break;

            case REG_FRNUM:
                // FRNUM is read-only
                break;

            case REG_FLBASEADD:
                flBaseAddr = (flBaseAddr & 0xFFFFFF00) | v;
                break;
            case REG_FLBASEADD + 1:
                flBaseAddr = (flBaseAddr & 0xFFFF00FF) | (v << 8);
                break;
            case REG_FLBASEADD + 2:
                flBaseAddr = (flBaseAddr & 0xFF00FFFF) | (v << 16);
                break;
            case REG_FLBASEADD + 3:
                flBaseAddr = (flBaseAddr & 0x00FFFFFF) | (v << 24);
                break;

            case REG_SOFMOD:
                sofMod = (sofMod & 0xFF00) | v;
                break;
            case REG_SOFMOD + 1:
                sofMod = (sofMod & 0x00FF) | (v << 8);
                break;

            case REG_PORTSC1:
                handlePortWrite(0, (byte)v, false);
                break;
            case REG_PORTSC1 + 1:
                handlePortWrite(0, (byte)v, true);
                break;
            case REG_PORTSC2:
                handlePortWrite(1, (byte)v, false);
                break;
            case REG_PORTSC2 + 1:
                handlePortWrite(1, (byte)v, true);
                break;

            default:
                break;
        }
    }

    private void handleUsbCmd()
    {
        if ((usbCmd & CMD_HCRESET) != 0)
        {
            usbCmd &= ~CMD_HCRESET;
            usbSts |= STS_HCH;
            running = false;
        }
        boolean newRunning = (usbCmd & CMD_RS) != 0;
        if (newRunning && !running)
        {
            running = true;
            usbSts &= ~STS_HCH;
        }
        else if (!newRunning && running)
        {
            running = false;
            usbSts |= STS_HCH;
        }
    }

    private int portStatus(int portIdx, int highByte)
    {
        int status = 0;
        boolean connected = rootHub.isPortConnected(portIdx);
        boolean enabled = rootHub.isPortEnabled(portIdx);
        boolean powered = rootHub.isPortPowered(portIdx);
        boolean csc = rootHub.isPortCsc(portIdx);
        boolean pedc = false;
        boolean prs = rootHub.isPortReset(portIdx);

        if (connected) status |= 0x0100; // Current Connect Status (CCS)
        if (csc) status |= PORT_CSC;
        if (pedc) status |= PORT_PEDC;
        if (enabled) status |= PORT_PED;
        if (prs) status |= PORT_PRS;
        if (powered) status |= 0x0100; // simplified

        if (highByte != 0)
        {
            return (status >> 8) & 0xFF;
        }
        return status & 0xFF;
    }

    private byte portStatusByte(int portIdx, int highByte)
    {
        return (byte) portStatus(portIdx, highByte);
    }

    private void handlePortWrite(int portIdx, byte value, boolean highByte)
    {
        if (portIdx >= portCount) return;
        int v = value & 0xFF;
        if (highByte)
        {
            // High byte: only PORT_PR (bit 0x00 of high byte = bit 8 of word)
            if ((v & 0x01) != 0)
            {
                rootHub.setPortReset(portIdx, true);
            }
        }
        else
        {
            // Low byte: write-1-to-clear for CSC/PEDC
            if ((v & PORT_CSC) != 0) rootHub.clearPortCsc(portIdx);
            if ((v & PORT_PEDC) != 0) { /* clear PEDC */ }
            if ((v & PORT_PR) != 0) rootHub.setPortReset(portIdx, true);
            if ((v & PORT_PS) != 0) { /* suspend */ }
        }
    }
}
