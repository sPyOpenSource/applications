package cr0s.javara.entity.building;

import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.building.common.EntityWall;
import cr0s.javara.main.GUI;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.resources.SoundManager;
import cr0s.javara.util.Pos;

import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.StackPane;

public class EntityBuildingProgress extends EntityBuilding implements IShroudRevealer {

    private final EntityBuilding targetBuilding;
    private ShpTexture makeTexture;

    private int ticksRemaining;
    private int currentFrame;

    public EntityBuildingProgress(EntityBuilding aTargetBuilding) {
	super(new Pos(aTargetBuilding.boundingBox.getX(), aTargetBuilding.boundingBox.getY()), (int)aTargetBuilding.getWidth(), (int)aTargetBuilding.getHeight(), aTargetBuilding.getFootprint());

	this.targetBuilding = aTargetBuilding;

	if (!targetBuilding.makeTextureName.isEmpty()) {
	    makeTexture = ResourceManager.getInstance().getConquerTexture(targetBuilding.makeTextureName);
	    this.ticksRemaining = makeTexture.numImages - 1;
	} else {
	    this.ticksRemaining = 1;
	}
	
	setBibType(this.targetBuilding.getBibType());

	this.setMaxHp(10);
	this.setHp(10);

	// Set building progress is invulnerable to avoid glitches
	this.setInvuln(true);
	
	// Play "building" sound
	if (this.owner == GUI.getInstance().getPlayer()) {
	    //SoundManager.getInstance().playSfxGlobal("placbldg", 0.7f);
	    
	    if (!(this.targetBuilding instanceof EntityWall)) {
		//SoundManager.getInstance().playSfxGlobal("build5", 0.7f);
	    }
	}
	
	//this.owner.getBase().addToCurrentlyBuilding(this);
    }

    @Override
    public void updateEntity(long delta) {
	this.currentFrame++;
	
	if (--this.ticksRemaining <= 0) {
	    setDead();

	    //this.owner.getBase().addBuilding(this.targetBuilding);
	    //this.owner.getBase().getCurrentlyBuilding().remove(this);
	    
	    this.targetBuilding.isVisible = true;
            owner.entities.add(this.targetBuilding);
	    owner.world.spawnEntityInWorld(this.targetBuilding);
            owner.getBase().addBuilding(targetBuilding);
            targetBuilding.owner = owner;
	    this.targetBuilding.onBuildFinished();
	}
    }

    @Override
    public StackPane renderEntity() {
	if (this.makeTexture == null) {
	    return null;
	}
	
	ImageView view = new ImageView(SwingFXUtils.toFXImage(this.makeTexture.getAsImage(this.currentFrame, null), null));//.draw(this.posX, this.posY);
        view.setX(boundingBox.getX());
        view.setY(boundingBox.getY());
        StackPane combined = new StackPane();
        combined.getChildren().add(view);
        return combined;
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return this.targetBuilding.shouldRenderedInPass(passNum);
    }

    @Override
    public float getHeightInTiles() {
	return this.tileHeight;
    }

    @Override
    public float getWidthInTiles() {
	return this.tileWidth;
    }

    @Override
    public int getRevealingRange() {
	if (this.targetBuilding instanceof IShroudRevealer iShroudRevealer) {
	    return iShroudRevealer.getRevealingRange() / 2;
	} else {
	    return 0;
	}
    }

    @Override
    public ImageView getImageView() {
	return this.targetBuilding.getImageView();
    }

    public EntityBuilding getTargetBuilding() {
	return this.targetBuilding;
    }  
    
}
