package cr0s.javara.entity.effect;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.gameplay.Player;
import cr0s.javara.gameplay.Team;
import cr0s.javara.render.Sequence;
import cr0s.javara.util.Pos;
import javafx.scene.Scene;

public class SequencePlayer extends Entity implements IEffect {

    private Sequence seq;
    private int frameIndex = 0;
    private final int TICKS_PER_FRAME = 1;
    private int maxFrames;
    
    private int ticks = TICKS_PER_FRAME;
    
    private int width, height;
    
    public SequencePlayer(Pos pos, Sequence seq) {
	this(pos.getX(), pos.getY(), seq.getTexture().width, seq.getTexture().height);

	this.seq = seq;
	
	if (seq.isFinished()) {
	    this.setDead();
	}
    }
    
    public SequencePlayer(double posX, double posY,
	    float aSizeWidth, float aSizeHeight) {
	super(posX, posY, aSizeWidth, aSizeHeight);
    }

    @Override
    public void updateEntity(int delta) {
	if (this.seq.isFinished()) {
	    this.setDead();
	}
	
	this.seq.update(this.currentFacing);
    }

    @Override
    public void renderEntity(Scene g) {
	if (!this.seq.isFinished()) {
	    //this.seq.render(this.getTranslateX(), this.getTranslateY());
	}
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

}
