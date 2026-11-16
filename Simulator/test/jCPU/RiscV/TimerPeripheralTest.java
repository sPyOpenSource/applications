package jCPU.RiscV;
import static org.junit.Assert.*;
import org.junit.Test;
public class TimerPeripheralTest {
    @Test public void testCounterAdvancesPerTick() {
        TimerPeripheral timer = new TimerPeripheral();
        assertEquals(0, timer.readCounterLow());
        timer.tick();
        assertEquals(1, timer.readCounterLow());
        timer.tick();
        assertEquals(2, timer.readCounterLow());
    }
    @Test public void testCompareMatchAssertsInterrupt() {
        TimerPeripheral timer = new TimerPeripheral();
        timer.writeCompareLow(3);
        assertFalse(timer.interruptPending());
        timer.tick(); timer.tick(); timer.tick();
        assertTrue(timer.interruptPending());
    }
}
