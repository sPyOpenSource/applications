package cr0s.javara.entity.building.soviet;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.IPowerConsumer;
import cr0s.javara.entity.building.common.EntityRadarDome;
import cr0s.javara.entity.vehicle.soviet.EntityMammothTank;

import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.render.World;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class EntityAirField extends EntityBuilding implements ISelectable, IPowerConsumer, IShroudRevealer, IHaveCost {

    private SpriteSheet sheet;

    private int animIndex = 0;
    private int animDelayTicks = 0;
    private static final int ANIM_DELAY_TICKS = 2;

    private final String TEXTURE_NAME = "afld.shp";
    private final String MAKE_TEXTURE_NAME = "afldmake.shp";

    public static final int WIDTH_TILES = 3;
    public static final int HEIGHT_TILES = 2;

    private static final int TEXTURE_WIDTH = 72;
    private static final int TEXTURE_HEIGHT = 48;

    private static final int POWER_CONSUMPTION_LEVEL = 20;
    private static final int SHROUD_REVEALING_RANGE = 7;

    private static final int BUILDING_COST = 500;
    
    private final static Pos spawnOffset = new Pos(0, 12);
    private final static int initialFacing = 12;
    
    private final boolean isCharging = false;
    
    public EntityAirField(Pos tile) {
	super(tile, WIDTH_TILES * 24, HEIGHT_TILES * 24, "xxx xxx");

	setBibType(BibType.NONE);
	setProgressValue(-1);

	setMaxHp(1000);
	setHp(this.getMaxHp());

	this.armorType = ArmorType.WOOD;
	this.makeTextureName = MAKE_TEXTURE_NAME;

	initTextures();
	this.unitProductionAlingment = Alignment.SOVIET;
	
	this.requiredToBuild.add(EntityRadarDome.class);
	
	this.setName("afld");
    }

    private void initTextures() {
	ShpTexture tex = ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME);
	sheet = new SpriteSheet(tex.getAsCombinedImage(null), tex.getAsImage(0, null).getWidth(), tex.getAsImage(0, null).getHeight());
    }

    @Override
    public StackPane renderEntity() {
	int corruptionShift = 0;

	if (this.getHp() > this.getMaxHp() / 2) {
	    corruptionShift = 0;
	} else {
	    corruptionShift = 8;
	}

	//sheet.startUse();
	ImageView view = sheet.getSubImage(0, corruptionShift + ((this.isCharging) ? animIndex : 4 + animIndex));//.drawEmbedded(posX, posY, this.getTextureWidth(), this.getTextureHeight());
	//sheet.endUse();

	// Draw bounding box if debug mode is on
	if (GUI.DEBUG_MODE) {
	    /*g.setLineWidth(2);
	    g.setColor(owner.playerColor);
	    g.draw(boundingBox);
	    g.setLineWidth(1);*/
	}
	
	// Render repairing wrench
	if (this.repairIconBlink) {
	    //repairImage.draw(this.boundingBox.getX() + this.boundingBox.getWidth() / 2 - repairImage.getWidth() / 2, this.boundingBox.getY() + this.boundingBox.getHeight() / 2 - repairImage.getHeight() / 2);
	}
        view.setX(boundingBox.getX());
        view.setY(boundingBox.getY());
        StackPane combined = new StackPane();
        combined.getChildren().add(view);
        return combined;
    }

    @Override
    public boolean shouldRenderedInPass(int passnum) {
	return passnum == 0;
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);
	
	if (animDelayTicks++ > ANIM_DELAY_TICKS) {
	    animDelayTicks = 0;

	    if (this.isCharging) {
		this.animIndex = (this.animIndex + 1) % 4;
	    } else {
		this.animIndex = World.getRandomInt(0, 4); // Do random wind flag fluctiations
	    }
	}
    }

    @Override
    public void select() {
	this.isSelected = true;
    }

    @Override
    public void cancelSelect() {
	this.isSelected = false;
    }

    @Override
    public boolean isSelected() {
	return this.isSelected;
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
    public int getConsumptionLevel() {
	return EntityAirField.POWER_CONSUMPTION_LEVEL;
    }

    @Override
    public int getRevealingRange() {
	return EntityAirField.SHROUD_REVEALING_RANGE;
    }

    @Override
    public ImageView getImageView() {
        if(super.getImageView() == null){
            Image view = sheet.getSubImage(0, 0).getImage();
            super.getImageView().setX(boundingBox.getX());
            super.getImageView().setY(boundingBox.getY());
            getImageViews().add(view);
        }
        return super.getImageView();
    }

    @Override
    public int getBuildingCost() {
	return BUILDING_COST;
    }

    public void deployEntity(EntityActor newInstance) {
	if (newInstance instanceof MobileEntity me) {	    
	    me.isVisible = true;	    
	  
	    owner.world.spawnEntityInWorld(newInstance);
	    
	    me.setPos(this.getPosition().add(EntityAirField.spawnOffset));
	    me.currentFacing = EntityAirField.initialFacing;
	}
    }
    
    @Override
    public void onBuildFinished() {
	this.deployEntity(new EntityMammothTank(new Pos(0d, 0d)));
    }
}
