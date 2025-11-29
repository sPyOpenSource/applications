package cr0s.javara.entity.effect;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;
import javafx.scene.image.ImageView;

public class Explosion extends Entity implements IEffect {

    private SpriteSheet tex;
    private int frameIndex = 0;
    private final int TICKS_PER_FRAME = 1;
    private int maxFrames;
    
    private int ticks = TICKS_PER_FRAME;
    
    private int width, height;
    
    public Explosion(Pos pos, String texture) {
	this(pos.getX(), pos.getY(), 24, 24);
	
	if (texture != null && !texture.isEmpty()) {
	    ShpTexture t = ResourceManager.getInstance().getConquerTexture(texture);
	    this.width = t.width;
	    this.height = t.height;
	    this.maxFrames = t.numImages;

	    this.tex = new SpriteSheet(t.getAsCombinedImage(null), t.width, t.height);
	} else {
	    this.setDead();
	}
    }
    
    public Explosion(double posX, double posY,
	    float aSizeWidth, float aSizeHeight) {
	super(posX, posY, aSizeWidth, aSizeHeight);
    }

    @Override
    public void updateEntity(long delta) {
	if (--ticks <= 0) {
	    this.ticks = TICKS_PER_FRAME;
	    
	    this.frameIndex++;
	    
	    if (this.frameIndex >= this.maxFrames) {
		this.frameIndex = 0;
		this.setDead();
	    }
	}
    }

    @Override
    public ImageView renderEntity() {
	return tex.getSubImage(0, frameIndex);//.draw(this.posX - this.width / 2, this.posY - this.height / 2);
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

}
