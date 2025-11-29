package cr0s.javara.entity.effect;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;
import javafx.scene.image.ImageView;

public class MuzzleFlash extends Entity implements IEffect {

    private SpriteSheet tex;
    private int frameIndex = 0;
    private final int TICKS_PER_FRAME = 1;
    private int maxFrames;
    private int numFacings;
    private int facing;
    
    private int ticks = TICKS_PER_FRAME;
    private int width, height;
    private Sequence seq;
    
    public MuzzleFlash(Pos pos, String texture, int fac, int numFacings, int length) {
	this(pos.getX(), pos.getY(), 24, 24);
	
	ShpTexture t = ResourceManager.getInstance().getConquerTexture(texture);
	this.width = t.width;
	this.height= t.height;
	
	seq = new Sequence(t, 0, numFacings, length, 1, null);
	this.facing = fac;
    }
    
    public MuzzleFlash(double posX, double posY,
	    float aSizeWidth, float aSizeHeight) {
	super(posX, posY, aSizeWidth, aSizeHeight);
    }

    @Override
    public void updateEntity(long delta) {
	if (!seq.isFinished()) {
	    seq.update(this.currentFacing);
	} else {
	    this.setDead();
	}
    }

    @Override
    public ImageView renderEntity() {
	return null;//this.seq.render(this.getTranslateX() - this.width / 2, this.getTranslateY() - this.height / 2);
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

}
