package jCPU.arm;

import jCPU.Peripheral;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

public class UartPeripheral implements Peripheral {
    private final int uartAddress;
    private final ConcurrentLinkedQueue<Byte> inputBuffer = new ConcurrentLinkedQueue<>();

    public UartPeripheral() {
        this(0x10000000);
    }

    public UartPeripheral(int uartAddress) {
        this(System.in, uartAddress);
    }

    public UartPeripheral(InputStream in) {
        this(in, 0x10000000);
    }

    public UartPeripheral(InputStream in, int uartAddress) {
        this.uartAddress = uartAddress;
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
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

    public int getUartAddress() {
        return uartAddress;
    }

    @Override
    public byte read(int offset) {
        // offset is ignored in this simple UART implementation
        Byte b = inputBuffer.poll();
        return b == null ? 0 : b;
    }

    @Override
    public void write(int offset, byte value) {
        // offset is ignored in this simple UART implementation
        System.out.print((char) value);
    }
}
