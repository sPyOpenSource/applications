package jCPU.arm;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UartPeripheral implements Peripheral {
    private final ConcurrentLinkedQueue<Byte> inputBuffer = new ConcurrentLinkedQueue<>();

    public UartPeripheral() {
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    int c = reader.read();
                    if (c != -1) {
                        inputBuffer.add((byte) c);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    @Override
    public byte read(int offset) {
        Byte b = inputBuffer.poll();
        return b == null ? 0 : b;
    }

    @Override
    public void write(int offset, byte value) {
        System.out.print((char) value);
    }
}
