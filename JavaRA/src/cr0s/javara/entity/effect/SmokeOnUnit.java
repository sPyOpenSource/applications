package cr0s.javara.entity.effect;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;
import javafx.scene.image.ImageView;

public class SmokeOnUnit extends Entity implements IEffect {

    private static final int DEFAULT_LIFETIME_TICKS = 250;
    private SpriteSheet tex;
    private int frameIndex = 0;
    private final int TICKS_PER_FRAME = 1;
    private int maxFrames;
    
    private int ticks = TICKS_PER_FRAME;
    private int width, height;
    
    private int lifeTicks = DEFAULT_LIFETIME_TICKS;
    private EntityActor parentUnit;
    
    public SmokeOnUnit(EntityActor parentUnit, String texture) {
	this(parentUnit.getPosition(), 24, 24);
	
	this.parentUnit = parentUnit;
	
	ShpTexture t = ResourceManager.getInstance().getConquerTexture(texture);
	this.width = t.width;
	this.height = t.height;
	this.maxFrames = 42;
	
	this.frameIndex = 0;
	
	this.tex = new SpriteSheet(t.getAsCombinedImage(null), t.width, t.height);
    }
    
    public SmokeOnUnit(Pos pos,
	    float aSizeWidth, float aSizeHeight) {
	super(pos, aSizeWidth, aSizeHeight);
    }

    @Override
    public void updateEntity(long delta) {
	if (this.parentUnit.isDead()) {
	    this.setDead();
	}
	
	if (--this.ticks <= 0) {
	    this.ticks = this.TICKS_PER_FRAME;
	    
	    this.frameIndex = (this.frameIndex + 1) % this.maxFrames;
	}
	
	if (--this.lifeTicks <= 0) {
	    this.setDead();
	}
    }

    @Override
    public ImageView renderEntity() {
	return this.tex.getSubImage(0, 49 + this.frameIndex);//.draw(this.parentUnit.getPosition().getX() - this.width / 2, this.parentUnit.getPosition().getY() - this.height / 2);
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return this.parentUnit.shouldRenderedInPass(passNum);
    }

}
