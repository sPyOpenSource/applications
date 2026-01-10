package cr0s.javara.entity.effect;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.main.GUI;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.paint.Color;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class MoveFlash extends Entity implements IEffect {

    private final SpriteSheet tex;
    private final int TICKS_PER_FRAME = 2;
    private final int MAX_FRAMES = 5;
    private int ticks = TICKS_PER_FRAME;
    private int frameIndex = 0;

    public MoveFlash(Pos pos,
	    float aSizeWidth, float aSizeHeight) {
	super(pos, aSizeWidth, aSizeHeight);
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
    public StackPane renderEntity() {
	ImageView view = tex.getSubImage(0, frameIndex);//.draw(this.posX - 12, this.posY - 12);
        StackPane combined = new StackPane();
        combined.getChildren().add(view);
        return combined;
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

}
