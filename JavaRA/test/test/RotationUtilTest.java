package test;

import cr0s.javara.util.Pos;
import cr0s.javara.util.RotationUtil;
import static org.junit.Assert.*;
import org.junit.Test;

public class RotationUtilTest {

    private static final double DELTA = 1e-6;

    @Test
    public void testGetRotationNorth() {
        int rot = RotationUtil.getRotationFromXY(0, 0, 0, -10);
        assertEquals(0, rot);
    }

    @Test
    public void testGetRotationWest() {
        int rot = RotationUtil.getRotationFromXY(0, 0, -10, 0);
        assertEquals(8, rot);
    }

    @Test
    public void testGetRotationSouth() {
        int rot = RotationUtil.getRotationFromXY(0, 0, 0, 10);
        assertEquals(16, rot);
    }

    @Test
    public void testGetRotationEast() {
        int rot = RotationUtil.getRotationFromXY(0, 0, 10, 0);
        assertEquals(24, rot);
    }

    @Test
    public void testGetRotationNorthEast() {
        int rot = RotationUtil.getRotationFromXY(0, 0, 10, -10);
        assertTrue(rot == 28 || rot == 29);
    }

    @Test
    public void testQuantizeFacingsMax8() {
        assertEquals(0, RotationUtil.quantizeFacings(0, 8));
        assertEquals(1, RotationUtil.quantizeFacings(3, 8));
        assertEquals(1, RotationUtil.quantizeFacings(4, 8));
        assertEquals(2, RotationUtil.quantizeFacings(7, 8));
        assertEquals(2, RotationUtil.quantizeFacings(8, 8));
        assertEquals(8, RotationUtil.quantizeFacings(31, 8));
    }

    @Test
    public void testQuantizeFacingsMax4() {
        assertEquals(0, RotationUtil.quantizeFacings(0, 4));
        assertEquals(1, RotationUtil.quantizeFacings(7, 4));
        assertEquals(1, RotationUtil.quantizeFacings(8, 4));
        assertEquals(4, RotationUtil.quantizeFacings(31, 4));
    }

    @Test
    public void testFacingToRecoilVectorNorth() {
        Pos v = RotationUtil.facingToRecoilVector(0);
        assertEquals(0.0, v.getX(), DELTA);
        assertEquals(24.0, v.getY(), DELTA);
    }

    @Test
    public void testFacingToRecoilVectorWest() {
        Pos v = RotationUtil.facingToRecoilVector(8);
        assertEquals(24.0, v.getX(), DELTA);
        assertEquals(0.0, v.getY(), DELTA);
    }

    @Test
    public void testFacingToRecoilVectorSouth() {
        Pos v = RotationUtil.facingToRecoilVector(16);
        assertEquals(0.0, v.getX(), DELTA);
        assertEquals(-24.0, v.getY(), DELTA);
    }

    @Test
    public void testFacingToRecoilVectorEast() {
        Pos v = RotationUtil.facingToRecoilVector(24);
        assertEquals(-24.0, v.getX(), DELTA);
        assertEquals(0.0, v.getY(), DELTA);
    }

    @Test
    public void testFacingToRecoilVectorDiagonal() {
        Pos v = RotationUtil.facingToRecoilVector(4);
        assertTrue((int) v.getX() != 0 || (int) v.getY() != 0);
    }

    @Test
    public void testFacingToAngleMax32() {
        float angle = RotationUtil.facingToAngle(0, 32);
        assertEquals(0.0, angle, DELTA);
    }

    @Test
    public void testFacingToAngle90Degrees() {
        float angle = RotationUtil.facingToAngle(8, 32);
        assertEquals(Math.toRadians(90), angle, DELTA);
    }

    @Test
    public void testFacingToAngle180Degrees() {
        float angle = RotationUtil.facingToAngle(16, 32);
        assertEquals(Math.toRadians(180), angle, DELTA);
    }

    @Test
    public void testAngleToFacing() {
        int facing = RotationUtil.angleToFacing(0);
        assertEquals(0, facing);
    }

    @Test
    public void testAngleToFacing180() {
        int facing = RotationUtil.angleToFacing((float) Math.PI);
        assertEquals(16, facing);
    }

    @Test
    public void testCycleWithinBounds() {
        float result = RotationUtil.cycle(15, 32);
        assertEquals(15.0f, result, DELTA);
    }

    @Test
    public void testCycleAboveBounds() {
        float result = RotationUtil.cycle(35, 32);
        assertEquals(3.0f, result, DELTA);
    }

    @Test
    public void testCycleBelowZero() {
        float result = RotationUtil.cycle(-5, 32);
        assertEquals(27.0f, result, DELTA);
    }

    @Test
    public void testTickFacingAlreadyAtTarget() {
        int result = RotationUtil.tickFacing(10, 10, 3);
        assertEquals(10, result);
    }

    @Test
    public void testTickFacingSmallTurn() {
        int result = RotationUtil.tickFacing(10, 11, 3);
        assertEquals(11, result);
    }

    @Test
    public void testTickFacingRotateRight() {
        int result = RotationUtil.tickFacing(4, 10, 2);
        assertEquals(10, result);
    }

    @Test
    public void testTickFacingRotateLeft() {
        int result = RotationUtil.tickFacing(10, 4, 2);
        assertEquals(4, result);
    }

    @Test
    public void testTickFacingWrapAround() {
        int result = RotationUtil.tickFacing(30, 2, 3);
        assertEquals(2, result);
    }

    @Test
    public void testFACING_TO_DEGREE() {
        assertEquals(11.25f, RotationUtil.FACING_TO_DEGREE, DELTA);
    }
}
