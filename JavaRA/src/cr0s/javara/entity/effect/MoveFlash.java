package cr0s.javara.entity.effect;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.main.GUI;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.paint.Color;
import javafx.scene.image.ImageView;

public class MoveFlash extends Entity implements IEffect {

    private final SpriteSheet tex;
    private int frameIndex = 0;
    private final int TICKS_PER_FRAME = 2;
    private final int MAX_FRAMES = 5;
    private int ticks = TICKS_PER_FRAME;
    
    public MoveFlash(double posX, double posY,
	    float aSizeWidth, float aSizeHeight) {
	super(posX, posY, aSizeWidth, aSizeHeight);
	ShpTexture t = ResourceManager.getInstance().getTemplateShpTexture(GUI.getInstance().getWorld().getMap().getTileSet().getSetName(), "moveflsh.tem");
	t.forcedColor = Color.rgb(255, 255, 255, 128f/255);
	
	tex = new SpriteSheet(t.getAsCombinedImage(null), 23, 23);
    }

    @Override
    public void updateEntity(long delta) {
	if (--ticks <= 0) {
	    this.ticks = TICKS_PER_FRAME;
	    
	    this.frameIndex++;
	    
	    if (this.frameIndex >= MAX_FRAMES) {
		this.frameIndex = 0;
		this.setDead();
	    }
	}
    }

    @Override
    public ImageView renderEntity() {
	return tex.getSubImage(0, frameIndex);//.draw(this.posX - 12, this.posY - 12);
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

}
