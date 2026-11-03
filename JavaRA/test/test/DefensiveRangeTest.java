package test;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cr0s.javara.entity.building.soviet.EntityFireTurret;
import cr0s.javara.entity.building.soviet.EntityTeslaCoil;
import cr0s.javara.util.Pos;

public class DefensiveRangeTest {
    @BeforeClass
    public static void startJavaFx() {
        FxTestSupport.ensureStarted();
    }

    @AfterClass
    public static void stopJavaFx() {
        FxTestSupport.shutdown();
    }

    @Test
    public void fireTurretRangeIsFiveCells() {
        assertEquals(5, new EntityFireTurret(new Pos(0, 0)).getRange());
    }

    @Test
    public void teslaCoilRangeIsEightCells() {
        assertEquals(8, new EntityTeslaCoil(new Pos(0, 0)).getRange());
    }
}
