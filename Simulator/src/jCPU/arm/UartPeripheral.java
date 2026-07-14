package jCPU.arm;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

public class UartPeripheral implements Peripheral {
    private final ConcurrentLinkedQueue<Byte> inputBuffer = new ConcurrentLinkedQueue<>();

    public UartPeripheral() {
        this(System.in);
    }

    public UartPeripheral(InputStream in) {
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
