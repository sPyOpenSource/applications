package jCPU.arm;

import jCPU.Peripheral;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

/**
 * Minimal ARM PL011 UART model. Only the data register has side effects:
 * writes go to stdout, reads return queued input. All other registers are
 * write-ignored/read-as-zero, except UARTFR which reports the transmit FIFO
 * as always empty so kernel polled output never stalls.
 */
public class UartPeripheral implements Peripheral {
    private static final int UARTDR = 0x000;
    private static final int UARTFR = 0x018;

    private static final int UARTFR_RXFE = 1 << 4;
    private static final int UARTFR_TXFE = 1 << 7;

    private final int uartAddress;
    private final ConcurrentLinkedQueue<Byte> inputBuffer = new ConcurrentLinkedQueue<>();
    private int traceWrites = 0;

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
        switch(offset & 0xFFF) {
        case UARTFR:
            /* Always ready: not busy, TX/RX FIFOs empty. */
            return (byte)(UARTFR_TXFE | UARTFR_RXFE);
        case UARTDR:
        default:
            Byte b = inputBuffer.poll();
            return b == null ? 0 : b;
        }
    }

    @Override
    public void write(int offset, byte value) {
        if((offset & 0xFFF) == UARTDR) {
            int ch = value & 0xFF;
            System.out.print((char) ch);
            System.out.flush(); // print() does not auto-flush; make console output immediate
            if(traceWrites++ < 128) {
                System.err.printf("[JCPU-UART] addr=0x%08X ch=0x%02X '%c'%n",
                    uartAddress + (offset & 0xFFF), ch,
                    (ch >= 0x20 && ch < 0x7F) ? (char) ch : '?');
            }
        }
        /* Other registers are configuration only; ignore them. */
    }
}
