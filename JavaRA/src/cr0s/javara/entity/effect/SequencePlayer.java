package cr0s.javara.entity.effect;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.render.Sequence;
import cr0s.javara.util.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class SequencePlayer extends Entity implements IEffect {

    private Sequence seq;
    private int frameIndex = 0;
    private final int TICKS_PER_FRAME = 1;
    private int maxFrames;
    
    private int ticks = TICKS_PER_FRAME;
    
    private int width, height;
    
    public SequencePlayer(Pos pos, Sequence seq) {
	this(pos, seq.getTexture().width, seq.getTexture().height);

	this.seq = seq;
	
	if (seq.isFinished()) {
	    this.setDead();
	}
    }
    
    public SequencePlayer(Pos pos,
	    float aSizeWidth, float aSizeHeight) {
	super(pos, aSizeWidth, aSizeHeight);
    }

    @Override
    public void updateEntity(long delta) {
	if (this.seq.isFinished()) {
	    this.setDead();
	}
	
	this.seq.update(this.currentFacing);
    }

    @Override
    public StackPane renderEntity() {
	if (!this.seq.isFinished()) {
	    //this.seq.render(this.getTranslateX(), this.getTranslateY());
	}
        return null;
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

}
