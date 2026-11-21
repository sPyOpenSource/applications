package jCPU.RiscV;

import static org.junit.Assert.*;
import org.junit.Test;

public class TimerPeripheralTest {
    @Test public void testCounterAdvancesPerTick() {
        TimerPeripheral timer = new TimerPeripheral();
        assertEquals(0, timer.read(0) & 0xFF);
        timer.tick();
        assertEquals(1, timer.read(0) & 0xFF);
        timer.tick();
        assertEquals(2, timer.read(0) & 0xFF);
    }

    @Test public void testCounterHighByteReadable() {
        TimerPeripheral timer = new TimerPeripheral();
        timer.tick();
        assertEquals(0, timer.read(4) & 0xFF);
    }

    @Test public void testCompareMatchAssertsInterrupt() {
        TimerPeripheral timer = new TimerPeripheral();
        timer.write(8, (byte) 3); // compare low = 3
        assertFalse(timer.interruptPending());
        timer.tick(); timer.tick(); timer.tick(); // counter reaches 3
        assertTrue(timer.interruptPending());
    }

    @Test public void testInterruptCanBeCleared() {
        TimerPeripheral timer = new TimerPeripheral();
        timer.write(8, (byte) 1);
        timer.tick(); // counter reaches 1 == compare
        assertTrue(timer.interruptPending());
        timer.clearInterrupt();
        assertFalse(timer.interruptPending());
    }

    @Test public void testInterruptReArmsAfterClear() {
        TimerPeripheral timer = new TimerPeripheral();
        timer.write(8, (byte) 1);
        timer.tick(); // counter = 1 == compare
        assertTrue(timer.interruptPending());
        timer.clearInterrupt();
        assertFalse(timer.interruptPending());
        timer.write(8, (byte) 2); // raise compare above counter
        timer.tick(); // counter = 2 == compare
        assertTrue(timer.interruptPending());
    }

    @Test public void testUnknownOffsetReadsZero() {
        TimerPeripheral timer = new TimerPeripheral();
        assertEquals(0, timer.read(12) & 0xFF);
    }

    @Test public void testCompareUpperBytesPreserved() {
        TimerPeripheral timer = new TimerPeripheral();
        timer.writeCompareLow(5);
        timer.tick(); timer.tick(); timer.tick(); timer.tick(); timer.tick(); // counter = 5 == compare
        assertTrue(timer.interruptPending());
    }
}
