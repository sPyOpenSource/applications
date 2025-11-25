package cr0s.javara.perfomance;

import java.util.HashMap;
import javafx.scene.paint.Color;

public class Profiler {
    HashMap<String, ProfilingSection> profilers;
    
    private Color[] colors = { Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.YELLOW, Color.LIGHTGRAY, Color.WHITE };
    private int nextColor;
    
    private static Profiler instance;
    private Profiler() {
	this.profilers = new HashMap<>();
    }
    
    public static Profiler getInstance() {
	if (instance == null) {
	    instance = new Profiler();
	}
	
	return instance;
    }
    
    private ProfilingSection getSection(final String name) {
	if (this.profilers.containsKey(name)) {
	    return this.profilers.get(name);
	} else {
	    ProfilingSection newSection = new ProfilingSection(chooseColorForNewSection());
	    this.profilers.put(name, newSection);
	    
	    return newSection;
	}
    }
    
    private Color chooseColorForNewSection() {
	Color choosen = this.colors[nextColor];
	nextColor = (nextColor + 1) % this.colors.length; // Get next color in cycle of colors
	
	return choosen;
    }

    public void startForSection(final String name) {
	this.getSection(name).startTimer();
    }
    
    public void stopForSection(final String name) {
	this.getSection(name).stopTimer();
    }
    
    public int getMsPassedForSection(final String name) {
	return this.getSection(name).getMsPassed();
    }
}
