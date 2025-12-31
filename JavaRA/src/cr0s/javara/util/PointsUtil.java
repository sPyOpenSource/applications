package cr0s.javara.util;

import java.util.Random;

public class PointsUtil {
    public static Pos interpolatePos(Pos from, Pos to, int mul, int div) {
	double fx = from.getX();
	double fy = from.getY();
	double fz = from.getZ();

	double tx = to.getX();
	double ty = to.getY();
	double tz = to.getZ();

	double px = lerp(fx, tx, mul, div);
	double py = lerp(fy, ty, mul, div);
	double pz = lerp(fz, tz, mul, div);

	return new Pos(px, py, pz);
    }

    public static double lerp(double a, double b, double mul, double div )
    {
	return a + (b - a) * mul / div;
    }    

    public static Pos lerpQuadratic(Pos a, Pos b, float pitch, int mul, int div) {
	Pos ret = interpolatePos(a, b, mul, div);
	
	if (pitch == 0) {
	    return ret;
	}
	
	float offset = (float) (((((a.distanceTo(b) * mul) / div) * (div - mul)) / div) * Math.tan(pitch));
	
	ret.setZ(ret.getZ() + offset);
	
	return ret;
    }
    
    public static int distanceSq(Pos p1, Pos p2) {
	double dx = p1.getX() - p2.getX();
	double dy = p1.getY() - p2.getY();

	return (int) Math.ceil(dx * dx + dy * dy);
    }

    public static int rangeFromPdf(Random r, int samples)
    {
	final int CELL_SIZE = 24;
	
	int result = 0;
	for (int i = 0; i < samples; i++) {
	    // Get random number from -CELL_SIZE to CELL_SIZE
	    result += -CELL_SIZE + r.nextInt(2 * CELL_SIZE);
	}
	
	return result / samples;
    }    
}
