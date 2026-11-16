package jCPU.RiscV;
import jCPU.Peripheral;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;
public class UartPeripheral implements Peripheral {
    private final PrintStream out;
    private final ConcurrentLinkedQueue<Byte> input = new ConcurrentLinkedQueue<>();
    private Thread reader;
    public UartPeripheral() { this(System.in, System.out); }
    public UartPeripheral(InputStream in) { this(in, System.out); }
    public UartPeripheral(InputStream in, PrintStream out) {
        this.out = out;
        this.reader = new Thread(() -> { try { int b; while ((b = in.read()) != -1) input.offer((byte) b); } catch (Exception ignored) {} }, "uart-input-reader");
        this.reader.setDaemon(true); this.reader.start();
    }
    @Override public byte read(int offset) { Byte b = input.poll(); return b == null ? 0 : b; }
    @Override public void write(int offset, byte value) { out.print((char) value); }
}
