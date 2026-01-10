package cr0s.javara.render;

import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.RotationUtil;

import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class Sequence {
    private final ShpTexture tex;
    private final int start;
    private int facings;
    private final int length;
    private final int ticks;
    
    private int currentTicks;
    private int currentFrame;
    
    private boolean isFinished;
    private int currentFacing;
    
    private final Color remapColor;
    protected boolean isLoop;
    
    public Sequence(ShpTexture t, int startIndex, int facingsCount, int len, int delayTicks, Color remap) {
	this.tex = t;
	this.start = startIndex;
	this.facings = facingsCount;
	
	if (this.facings == 0) {
	    this.facings = 1;
	}
	
	if (len > 0) {
	    this.length = len;
	} else {
	    this.length = 1;
	}
	
	this.ticks = delayTicks;
	
	this.remapColor = remap;
    }
    
    public void update(int facing) {
	this.currentFacing = facing;
	
	if (!isFinished && (ticks == 0 || --currentTicks <= 0)) {
	    this.currentTicks = this.ticks;
	    
	    if (this.length > 0) {
		if (this.currentFrame >= length) {
		    this.currentFrame = 0;
		    this.isFinished = !isLoop;
		    return;
		}
		
		this.currentFrame++;
	    } else {
		this.currentFrame = 0;
	    }
	}
    }
    
    public Image render() {
	int f;
	if (this.facings < 8) { 
	    f = RotationUtil.quantizeFacings(this.currentFacing, this.facings);
	} else {
	    f = this.currentFacing;
	}
	
	int i = this.start + ((f % this.facings) * this.length);
	if (this.isLoop) { 
	    i += this.currentFrame % this.length;
	} else {
	    i += Math.min(this.currentFrame, this.length - 1);
	}
	
	if (i >= this.tex.numImages) {
	    i = this.tex.numImages - 1;
	}
	
	return SwingFXUtils.toFXImage(this.tex.getAsImage(i, this.remapColor), null);
    }

    public boolean isFinished() {
	return this.isFinished;
    }
    
    public void reset() {
	this.currentFrame = 0;
	this.currentTicks = 0;
	
	this.isFinished = false;
    }
    
    public void setIsLoop(boolean loop) {
	this.isLoop = loop;
    }
    
    public ShpTexture getTexture() {
	return this.tex;
    }
}
