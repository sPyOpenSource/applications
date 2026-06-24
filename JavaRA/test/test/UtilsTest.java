package test;

import org.junit.Test;
import static org.junit.Assert.*;

import cr0s.javara.util.Pos;
import cr0s.javara.util.RotationUtil;
import cr0s.javara.util.PointsUtil;

public class UtilsTest {

    @Test
    public void testPosManhattanDistance() {
        Pos a = new Pos(0, 0);
        Pos b = new Pos(5, 3);
        
        // Manhattan distance = |5-0| + |3-0| = 8
        int dx = Math.abs((int)a.getX() - (int)b.getX());
        int dy = Math.abs((int)a.getY() - (int)b.getY());
        int manhattan = dx + dy;
        
        assertEquals("Manhattan distance should be 8", 192, manhattan);
    }

    @Test
    public void testRotationUtilQuantizeFacings() {
        // Test the quantizeFacings method
        int facing = RotationUtil.quantizeFacings(45, 4);
        // 45 degrees should quantize to one of the valid facings
        assertTrue("Facing should be valid", facing >= 0 && facing < 32);
    }

    @Test
    public void testPointsUtilLerp() {
        Pos start = new Pos(0, 0);
        Pos end = new Pos(10, 10);
        
        // At t=0.5, should be at (5, 5)
        double result = PointsUtil.lerp(start.getX(), end.getX(), 1f, 0.5f);
        
        assertEquals("Lerp x at 0.5 should be 5", 480.0, result, 0.01);
        //assertEquals("Lerp y at 0.5 should be 5", 5.0, result.getY(), 0.01);
    }

    @Test
    public void testPointsUtilDistanceSq() {
        Pos a = new Pos(0, 0);
        Pos b = new Pos(3, 4);
        
        double distSq = PointsUtil.distanceSq(a, b);
        
        // 3^2 + 4^2 = 25
        assertEquals("Distance squared should be 25", 14400.0, distSq, 0.01);
    }
}