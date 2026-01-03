package cr0s.javara.entity.building.common;

import java.util.LinkedList;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.IPips;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.actor.activity.activities.harvester.FindResources;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.IOreCapacitor;
import cr0s.javara.entity.building.IPowerConsumer;
import cr0s.javara.entity.vehicle.common.EntityHarvester;

import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.order.ITargetLines;
import cr0s.javara.order.TargetLine;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import java.awt.image.BufferedImage;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.StackPane;

public class EntityProc extends EntityBuilding implements ISelectable, IPowerConsumer, IShroudRevealer, IPips, IOreCapacitor, ITargetLines, IHaveCost {

    private SpriteSheet sheet;
    private BufferedImage normal, corrupted;
    private final String TEXTURE_NAME = "proc.shp";
    private final String MAKE_TEXTURE_NAME = "procmake.shp";

    public static final int WIDTH_TILES = 3;
    public static final int HEIGHT_TILES = 4;
    public static final int HARV_FACING = 8;

    private static final int POWER_CONSUMPTION_LEVEL = 30;
    private static final int SHROUD_REVEALING_RANGE = 9;

    // Harverster offset position
    private static final int HARV_OFFSET_X = 1;
    private static final int HARV_OFFSET_Y = 2;

    // Ore capacity
    public static final int MAX_CAPACITY = 2000;
    public static final int PIPS_COUNT = 17;

    private final LinkedList<TargetLine> targetLines = new LinkedList<>();
    private static final int BUILDING_COST = 1400;

    public EntityProc(Pos tile) {
	super(tile, WIDTH_TILES * 24, HEIGHT_TILES * 24, "_x_ xxx x~~ ~~~");

	setBibType(BibType.MIDDLE);
	setProgressValue(-1);

	setMaxHp(900);
	setHp(getMaxHp());

	this.armorType = ArmorType.WOOD;

	this.makeTextureName = MAKE_TEXTURE_NAME;
	initTextures();

	this.unitProductionAlingment = Alignment.NEUTRAL;
	this.requiredToBuild.add(EntityPowerPlant.class);

	this.setName("proc");
    }

    @Override
    public void onBuildFinished() {
	super.onBuildFinished();
	spawnHarvester();
    }

    private void spawnHarvester() {
	if (world == null) {
	    return;
	}

	Pos harvCell = getHarvesterCell();
	EntityHarvester harv = new EntityHarvester(harvCell);

	harv.currentFacing = HARV_FACING;
	harv.isVisible = true;
	harv.setWorld(world);

	harv.linkedProc = this;
	harv.queueActivity(new FindResources());
        harv.owner = owner;
	world.spawnEntityInWorld(harv);
        owner.entities.add(harv);
	this.setName("proc");
    }

    public Pos getHarvesterCell() {
	return new Pos(this.boundingBox.getX() + HARV_OFFSET_X * 24, this.boundingBox.getY() + HARV_OFFSET_Y * 24);	
    }

    private void initTextures() {
	ShpTexture tex = ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME);
	corrupted = tex.getAsImage(1, null);
	normal = tex.getAsImage(0, null);	
    }

    @Override
    public StackPane renderEntity() {
        ImageView view = null;
	if (this.getHp() > this.getMaxHp() / 2) {
	    view = new ImageView(SwingFXUtils.toFXImage(normal, null));//.draw(nx, ny);
	} else {
	    //corrupted.draw(nx, ny);
	}

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
	return EntityProc.POWER_CONSUMPTION_LEVEL;
    }

    @Override
    public int getRevealingRange() {
	return EntityProc.SHROUD_REVEALING_RANGE;
    }

    @Override
    public ImageView getTexture() {
	return new ImageView(SwingFXUtils.toFXImage(normal,null));
    }

    public void acceptResources(int aCapacity) {
	owner.getBase().giveOre(aCapacity);
    }

    @Override
    public int getPipCount() {
	return EntityProc.PIPS_COUNT;
    }

    @Override
    public Color getPipColorAt(int i) {
	return (owner.getBase().ore * EntityProc.PIPS_COUNT > i * owner.getBase().oreCapacity) ? Color.YELLOW : null;
    }

    @Override
    public int getOreCapacityValue() {
	return EntityProc.MAX_CAPACITY;
    }

    @Override
    public LinkedList<TargetLine> getTargetLines() {
	return linkedHarvestersLines();
    }

    private LinkedList<TargetLine> linkedHarvestersLines() {
	this.targetLines.clear();

	for (Entity e : world.getEntitiesList()) {
	    if (e instanceof EntityHarvester) {
		if (((EntityHarvester)e).linkedProc == this) {
		    //this.targetLines.add(new TargetLine(new Pos(this.boundingBox.getCenterX(), this.boundingBox.getCenterY()), ((EntityHarvester) e).getCenterPos(), Color.yellow));
		}
	    }
	}

	return this.targetLines;
    }

    @Override
    public int getBuildingCost() {
	return BUILDING_COST;
    }        
}
