package test;

import cr0s.javara.util.PointsUtil;
import cr0s.javara.util.Pos;
import static org.junit.Assert.*;
import org.junit.Test;

public class PointsUtilTest {

    private static final double DELTA = 1e-9;

    @Test
    public void testLerpZero() {
        double result = PointsUtil.lerp(10.0, 20.0, 0, 4);
        assertEquals(10.0, result, DELTA);
    }

    @Test
    public void testLerpHalf() {
        double result = PointsUtil.lerp(10.0, 20.0, 2, 4);
        assertEquals(15.0, result, DELTA);
    }

    @Test
    public void testLerpFull() {
        double result = PointsUtil.lerp(10.0, 20.0, 4, 4);
        assertEquals(20.0, result, DELTA);
    }

    @Test
    public void testInterpolatePosZero() {
        Pos from = new Pos(0.0, 0.0, 0.0);
        Pos to = new Pos(100.0, 200.0, 300.0);
        Pos result = PointsUtil.interpolatePos(from, to, 0, 4);
        assertEquals(0.0, result.getX(), DELTA);
        assertEquals(0.0, result.getY(), DELTA);
        assertEquals(0.0, result.getZ(), DELTA);
    }

    @Test
    public void testInterpolatePosMid() {
        Pos from = new Pos(0.0, 0.0, 0.0);
        Pos to = new Pos(100.0, 200.0, 300.0);
        Pos result = PointsUtil.interpolatePos(from, to, 2, 4);
        assertEquals(50.0, result.getX(), DELTA);
        assertEquals(100.0, result.getY(), DELTA);
        assertEquals(150.0, result.getZ(), DELTA);
    }

    @Test
    public void testInterpolatePosFull() {
        Pos from = new Pos(0.0, 0.0, 0.0);
        Pos to = new Pos(100.0, 200.0, 300.0);
        Pos result = PointsUtil.interpolatePos(from, to, 4, 4);
        assertEquals(100.0, result.getX(), DELTA);
        assertEquals(200.0, result.getY(), DELTA);
        assertEquals(300.0, result.getZ(), DELTA);
    }

    @Test
    public void testDistanceSq() {
        int d = PointsUtil.distanceSq(new Pos(0.0, 0.0), new Pos(3.0, 4.0));
        assertEquals(25, d);
    }

    @Test
    public void testDistanceSqSamePoint() {
        int d = PointsUtil.distanceSq(new Pos(10.0, 20.0), new Pos(10.0, 20.0));
        assertEquals(0, d);
    }

    @Test
    public void testLerpQuadraticZeroPitch() {
        Pos from = new Pos(0.0, 0.0, 0.0);
        Pos to = new Pos(100.0, 0.0, 0.0);
        Pos result = PointsUtil.lerpQuadratic(from, to, 0.0f, 2, 4);
        assertEquals(50.0, result.getX(), DELTA);
        assertEquals(0.0, result.getY(), DELTA);
        assertEquals(0.0, result.getZ(), DELTA);
    }

    @Test
    public void testRangeFromPdf() {
        java.util.Random r = new java.util.Random(42);
        int result = PointsUtil.rangeFromPdf(r, 10);
        assertTrue(Math.abs(result) <= 24);
    }
}
