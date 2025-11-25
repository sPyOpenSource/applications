package cr0s.javara.ui;

import cr0s.javara.main.GUI;
import cr0s.javara.util.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

public class PowerBarRenderer {
    private int width, height;
    private Pos barPos;

    public Color powerNormal = Color.GREEN;
    public Color powerDown = Color.YELLOW;
    public Color powerDownCritical = Color.RED;

    private Ewma providedLerp = new Ewma(0.03f);
    private Ewma usedLerp = new Ewma(0.03f);

    public PowerBarRenderer(Pos aBarPos, int aWidth, int aHeight) {
	this.barPos = aBarPos;

	this.width = aWidth;
	this.height = aHeight;
    }

    public void render(Scene g, Color filterColor) {
	float scaleBy = 100.0f;
	int provided = GUI.getInstance().getPlayer().getBase().getPowerLevel();
	int used = GUI.getInstance().getPlayer().getBase().getConsumptionLevel();

	int max = Math.max(provided, used);
	while (max >= scaleBy) {
	    scaleBy *= 2;
	}

	double providedFrac = providedLerp.update(provided / scaleBy);
	double usedFrac = usedLerp.update(used / scaleBy);

	Color color = (used > provided) ? (((float) provided / (float) used <= 0.6f) ? this.powerDownCritical : this.powerDown) : this.powerNormal;
	double barY = lerp(barPos.getY(), barPos.getY() - this.height, providedFrac);
	
	//g.setColor(color.multiply(filterColor));
	//g.fillRect(barPos.getX(), barY, (float) this.width, providedFrac * this.height);

	//g.setColor(Color.WHITE.multiply(filterColor));
	double usedHeight = lerp(barPos.getY(), barPos.getY() - this.height, usedFrac);
	
	//g.fillRect(barPos.getX() + (this.width / 2), barPos.getY() - this.height * usedFrac, this.width / 3, this.height * usedFrac);
    }	

    private class Ewma {
	private float animRate;
	private double value = 0;

	public Ewma (float aAnimRate) {
	    this.animRate = aAnimRate;
	}

	public double update(float newValue) {
	    this.value = lerp(this.value, newValue, this.animRate);
	    return value;
	}
    }

    public static double lerp(double a, double b, double t) { return a + t * (b - a); }
}
