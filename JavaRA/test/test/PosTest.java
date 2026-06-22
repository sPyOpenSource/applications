package test;

import cr0s.javara.util.Pos;
import static org.junit.Assert.*;
import org.junit.Test;

public class PosTest {

    private static final double DELTA = 1e-9;

    @Test
    public void testIntConstructorMultipliesBy24() {
        Pos p = new Pos(3, 5);
        assertEquals(72.0, p.getX(), DELTA);
        assertEquals(120.0, p.getY(), DELTA);
        assertEquals(0.0, p.getZ(), DELTA);
    }

    @Test
    public void testDoubleConstructor() {
        Pos p = new Pos(10.5, 20.5);
        assertEquals(10.5, p.getX(), DELTA);
        assertEquals(20.5, p.getY(), DELTA);
        assertEquals(0.0, p.getZ(), DELTA);
    }

    @Test
    public void testThreeArgConstructor() {
        Pos p = new Pos(1.0, 2.0, 3.0);
        assertEquals(1.0, p.getX(), DELTA);
        assertEquals(2.0, p.getY(), DELTA);
        assertEquals(3.0, p.getZ(), DELTA);
    }

    @Test
    public void testClone() {
        Pos p = new Pos(10.0, 20.0, 30.0);
        Pos clone = p.Clone();
        assertEquals(p.getX(), clone.getX(), DELTA);
        assertEquals(p.getY(), clone.getY(), DELTA);
        assertEquals(p.getZ(), clone.getZ(), DELTA);
        assertNotSame(p, clone);
    }

    @Test
    public void testGetSetZ() {
        Pos p = new Pos(1.0, 2.0);
        p.setZ(5.0);
        assertEquals(5.0, p.getZ(), DELTA);
    }

    @Test
    public void testDistanceToSq() {
        Pos a = new Pos(0.0, 0.0);
        Pos b = new Pos(3.0, 4.0);
        assertEquals(25.0, a.distanceToSq(b), DELTA);
    }

    @Test
    public void testDistanceToSqWithZ() {
        Pos a = new Pos(0.0, 0.0, 0.0);
        Pos b = new Pos(1.0, 2.0, 3.0);
        assertEquals(14.0, a.distanceToSq(b), DELTA);
    }

    @Test
    public void testDistanceTo() {
        Pos a = new Pos(0.0, 0.0);
        Pos b = new Pos(3.0, 4.0);
        assertEquals(5.0, a.distanceTo(b), DELTA);
    }

    @Test
    public void testEqualsSameValues() {
        Pos a = new Pos(10.0, 20.0, 30.0);
        Pos b = new Pos(10.0, 20.0, 30.0);
        assertEquals(a, b);
    }

    @Test
    public void testEqualsDifferentX() {
        Pos a = new Pos(10.0, 20.0);
        Pos b = new Pos(11.0, 20.0);
        assertNotEquals(a, b);
    }

    @Test
    public void testEqualsSameObject() {
        Pos a = new Pos(1.0, 2.0);
        assertEquals(a, a);
    }

    @Test
    public void testEqualsNull() {
        Pos a = new Pos(1.0, 2.0);
        assertNotNull(a);
    }

    @Test
    public void testHashCodeConsistentWithEquals() {
        Pos a = new Pos(10.0, 20.0, 30.0);
        Pos b = new Pos(10.0, 20.0, 30.0);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testAdd() {
        Pos a = new Pos(1.0, 2.0, 3.0);
        Pos b = new Pos(10.0, 20.0, 30.0);
        Pos result = a.add(b);
        assertEquals(11.0, result.getX(), DELTA);
        assertEquals(22.0, result.getY(), DELTA);
        assertEquals(33.0, result.getZ(), DELTA);
    }

    @Test
    public void testSub() {
        Pos a = new Pos(10.0, 20.0, 30.0);
        Pos b = new Pos(1.0, 2.0, 3.0);
        Pos result = a.sub(b);
        assertEquals(9.0, result.getX(), DELTA);
        assertEquals(18.0, result.getY(), DELTA);
        assertEquals(27.0, result.getZ(), DELTA);
    }

    @Test
    public void testMulFloat() {
        Pos p = new Pos(2.0, 3.0, 4.0);
        Pos result = p.mul(2.0f);
        assertEquals(4.0, result.getX(), DELTA);
        assertEquals(6.0, result.getY(), DELTA);
        assertEquals(8.0, result.getZ(), DELTA);
    }

    @Test
    public void testMulPos() {
        Pos a = new Pos(2.0, 3.0, 4.0);
        Pos b = new Pos(5.0, 6.0, 7.0);
        Pos result = a.mul(b);
        assertEquals(10.0, result.getX(), DELTA);
        assertEquals(18.0, result.getY(), DELTA);
        assertEquals(28.0, result.getZ(), DELTA);
    }

    @Test
    public void testAddFloat() {
        Pos p = new Pos(1.0, 2.0);
        Pos result = p.add(5.0f);
        assertEquals(6.0, result.getX(), DELTA);
        assertEquals(7.0, result.getY(), DELTA);
    }

    @Test
    public void testGetCellX() {
        Pos p = new Pos(5, 3);
        assertEquals(5, p.getCellX());
    }

    @Test
    public void testGetCellY() {
        Pos p = new Pos(5, 3);
        assertEquals(3, p.getCellY());
    }

    @Test
    public void testLengthSquared() {
        Pos p = new Pos(3.0, 4.0);
        assertEquals(25.0, p.lengthSquared(), DELTA);
    }

    @Test
    public void testLength() {
        Pos p = new Pos(3.0, 4.0);
        assertEquals(5.0, p.length(), DELTA);
    }

    @Test
    public void testGetHorizontalLength() {
        Pos p = new Pos(3.0, 4.0, 99.0);
        assertEquals(5.0, p.getHorizontalLength(), DELTA);
    }

    @Test
    public void testToString() {
        Pos p = new Pos(10.0, 20.0, 30.0);
        String str = p.toString();
        assertTrue(str.contains("10.0"));
        assertTrue(str.contains("20.0"));
        assertTrue(str.contains("30.0"));
    }

    @Test
    public void testSetLocationThrows() {
        Pos p = new Pos(0.0, 0.0);
        try {
            p.setLocation(1.0, 2.0);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testDot() {
        Pos a = new Pos(1.0, 0.0, 0.0);
        Pos b = new Pos(1.0, 0.0, 0.0);
        double result = a.dot(b);
        assertTrue(Double.isFinite(result));
    }

    @Test
    public void testSetX() {
        Pos p = new Pos(0.0, 0.0);
        p.setX(42.0);
        assertEquals(42.0, p.getX(), DELTA);
    }

    @Test
    public void testSetY() {
        Pos p = new Pos(0.0, 0.0);
        p.setY(42.0);
        assertEquals(42.0, p.getY(), DELTA);
    }
}
