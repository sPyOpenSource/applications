package jCPU.RiscV;

/**
 * Thrown when an ESP32-C3 board memory access targets an unmapped address.
 * Carries the offending address and the access type for diagnostics.
 */
public class BusErrorException extends Exception {
    private final int address;
    private final String access;

    public BusErrorException(int address, String access) {
        super("Bus error at 0x" + Integer.toHexString(address) + " (" + access + ")");
        this.address = address;
        this.access = access;
    }

    public int getAddress() { return address; }
    public String getAccess() { return access; }
}
