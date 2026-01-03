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
import cr0s.javara.entity.building.common.EntityPowerPlant;

import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;

public class EntityBarracks extends EntityBuilding implements ISelectable, IPowerConsumer, IShroudRevealer, IHaveCost {

    private int animIndex = 0;
    private int animDelayTicks = 0;
    private static final int ANIM_DELAY_TICKS = 2;

    private final String TEXTURE_NAME = "barr.shp";
    private final String MAKE_TEXTURE_NAME = "barrmake.shp";

    public static final int WIDTH_TILES = 2;
    public static final int HEIGHT_TILES = 3;

    private static final int TEXTURE_WIDTH = 48;
    private static final int TEXTURE_HEIGHT = 48;

    private static final int POWER_CONSUMPTION_LEVEL = 10;
    private static final int SHROUD_REVEALING_RANGE = 10;

    private static final int BUILDING_COST = 400;
    private SpriteSheet sheet;

    private Pos rallyPos;
    private Pos exitPos;
    
    public EntityBarracks(Pos tile) {
	super(tile, WIDTH_TILES * 24, HEIGHT_TILES * 24, "xx xx ~~");

	setBibType(BibType.SMALL);
	setProgressValue(-1);

	setMaxHp(800);
	setHp(getMaxHp());

	this.armorType = ArmorType.WOOD;
	this.makeTextureName = MAKE_TEXTURE_NAME;

	initTextures();
	this.unitProductionAlingment = Alignment.SOVIET;
	
	this.requiredToBuild.add(EntityPowerPlant.class);
	
	this.setName("barr");
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
	    corruptionShift = 10;
	}

	//sheet.startUse();
	ImageView view = sheet.getSubImage(0, corruptionShift + animIndex);//.drawEmbedded(tileX, tileY, this.getTextureWidth(), this.getTextureHeight());
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
        setImageView(view);
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

	    this.animIndex = (this.animIndex + 1) % 10;
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
	return EntityBarracks.POWER_CONSUMPTION_LEVEL;
    }

    @Override
    public int getRevealingRange() {
	return EntityBarracks.SHROUD_REVEALING_RANGE;
    }

    @Override
    public ImageView getTexture() {
	return sheet.getSubImage(0, 0);
    }

    @Override
    public int getBuildingCost() {
	return BUILDING_COST;
    }

    public void deployEntity(EntityActor newInstance) {
	if (newInstance instanceof MobileEntity me) {	    
	    me.isVisible = true;	    
	    newInstance.setWorld(this.world);
	  
	    world.spawnEntityInWorld(newInstance);
	    me.setPos(new Pos(boundingBox.getX(), boundingBox.getY()));
            
	    Path p = new Path();
	    p.getElements().add(new MoveTo(exitPos.getX(), exitPos.getY()));
	    p.getElements().add(new MoveTo(rallyPos.getX(), rallyPos.getY()));
	    
	    SubCell freeSubCell = world.blockingEntityMap.getFreeSubCell(rallyPos, SubCell.CENTER);
	    if (freeSubCell != null) {
		me.currentSubcell = freeSubCell;
		me.desiredSubcell = freeSubCell;
		me.setCellPos(exitPos);
		
		me.startMovingByPath(p, this);
	    } else {
		SubCell sc = SubCell.CENTER;

		MobileEntity blocker = world.getMobileEntityInCell(exitPos);
		if (blocker != null) {
		    blocker.nudge(me, true);
		}
		
		blocker = world.getMobileEntityInCell(rallyPos);
		if (blocker != null) {
		    blocker.nudge(me, true);
		}
		
		me.setCellPos(exitPos);
		me.currentSubcell = sc;
		me.desiredSubcell = sc;
		me.startMovingByPath(p, this);
	    }
	}
    }
    
    @Override
    public void onBuildFinished() {
	this.exitPos = new Pos(boundingBox.getX(), boundingBox.getY() + 1 * 24);
	this.rallyPos = new Pos(boundingBox.getX(), boundingBox.getY() + 2 * 24);	
    }
}
