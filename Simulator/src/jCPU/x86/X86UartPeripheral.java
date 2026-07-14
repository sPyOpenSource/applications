package jCPU.x86;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

public class X86UartPeripheral implements X86Peripheral {
    private static final int COM1_BASE = 0x3F8;
    private final ConcurrentLinkedQueue<Byte> inputBuffer = new ConcurrentLinkedQueue<>();
    private final int basePort;

    public X86UartPeripheral() {
        this(COM1_BASE);
    }

    public X86UartPeripheral(int basePort) {
        this.basePort = basePort;
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    int c = reader.read();
                    if (c == -1) break;
                    inputBuffer.add((byte) c);
                }
            } catch (Exception e) {
                System.err.println("UART reader error: " + e.getMessage());
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    @Override
    public byte readPort(int port) {
        int offset = port - basePort;
        switch (offset) {
            case 0: // Receive buffer
                Byte b = inputBuffer.poll();
                return b == null ? 0 : b;
            case 5: // Line status register
                return (byte) ((inputBuffer.isEmpty() ? 0 : 1) | 0x60); // THRE + TEMT
            default:
                return 0;
        }
    }

    @Override
    public void writePort(int port, byte value) {
        int offset = port - basePort;
        if (offset == 0) { // Transmit buffer
            System.out.print((char) value);
        }
    }
}