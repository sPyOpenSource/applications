package cr0s.javara.entity.building.common;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.entity.IDeployable;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.IPowerConsumer;
import cr0s.javara.entity.building.soviet.EntityBarracks;
import cr0s.javara.entity.vehicle.EntityVehicle;

import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.MoveTo;

public class EntityWarFactory extends EntityBuilding implements ISelectable, IShroudRevealer, IDeployable, IPowerConsumer, IHaveCost {

    private SpriteSheet sheetTop;
    private BufferedImage weapDownNormal, weapDownCorrupted;
    
    private final String TEXTURE_NAME_DOWN = "weap.shp";
    private final String TEXTURE_NAME_TOP = "weap2.shp";
    private final String MAKE_TEXTURE_NAME = "weapmake.shp";
    private final int CORRUPTION_INDEX = 4;

    public static final int WIDTH_TILES = 3;
    public static final int HEIGHT_TILES = 4;
    
    private static final int SHROUD_REVEALING_RANGE = 10;
    private static final int ANIMATION_FRAME_DELAY = 1; // in ticks
    private static final int ANIMATION_LENGTH = 3;
    private static final int DEPLOY_TRY_INTERVAL = 5;
    
    private static final String FOOTPRINT = "xxx xxx ~~~ ~~~";
    private final Alignment weapAlignment = Alignment.SOVIET;
    private boolean isCorrupted = false;
    
    private int currentPass;
    private int animationFrame;
    private int animationFrameTicks;
    private int deployTryTicks;
    
    private boolean isDoorsOpenAnimation;
    private boolean isDoorsCloseAnimation;
    private boolean isDoorsOpen;
    
    private EntityVehicle targetEntity;
    private final Rectangle exitBoundingBox;

    // 6 directions to exit from war factory
    private final int[] exitDirectionsX = { 0, -1, -1, 1, 1, 0 };
    private final int[] exitDirectionsY = { 1,  0,  1, 1, 0, 0 };
    
    private static final int CONSUME_POWER_VALUE = 30;
    private static final int BUILDING_COST = 2000;
    
    private int ticksBeforeClose = 0;
    private final int TICKS_BEFORE_CLOSE = 30;
    
    public EntityWarFactory(Pos tile) {
	super(tile, WIDTH_TILES * 24, HEIGHT_TILES * 24, FOOTPRINT);

	//this.weapAlignment = player.getAlignment();

	setBibType(BibType.MIDDLE);
	setProgressValue(-1);

	setMaxHp(1500);
	setHp(getMaxHp());

	this.armorType = ArmorType.WOOD;
	
	this.isDoorsOpen = false;
	
	this.buildingSpeed = 20;
	this.makeTextureName = MAKE_TEXTURE_NAME;
	initTextures();
	
	this.exitBoundingBox = new Rectangle(boundingBox.getX(), boundingBox.getY(), sizeWidth, sizeHeight - 24);
	this.unitProductionAlingment = Alignment.NEUTRAL;
	
	this.requiredToBuild.add(EntityPowerPlant.class);
	this.requiredToBuild.add(EntityProc.class);
        this.requiredToBuild.add(EntityBarracks.class);
	
	this.setName("weap");
    }

    private void initTextures() {
	ShpTexture texTop = ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME_TOP);
	this.sheetTop = new SpriteSheet(texTop.getAsCombinedImage(null, false, 0, 0), 72, 48);
	
	ShpTexture texDown = ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME_DOWN);
	weapDownNormal = texDown.getAsImage(0, null);
	weapDownCorrupted = texDown.getAsImage(1, null);	
    }

    @Override
    public ImageView renderEntity() {
	//double nx = posX;
	//double ny = posY;
ImageView view = null;
	// Draw downTexture
	if (this.currentPass == 0) {
	    if (!this.isCorrupted) {
		view = new ImageView(SwingFXUtils.toFXImage(weapDownNormal,null));//.draw(nx, ny);
	    } else {
		//weapDownCorrupted.draw(nx, ny);
	    }
	} else {
	    if (isDoorsOpenAnimation || isDoorsCloseAnimation) { 
		view = sheetTop.getSubImage(0, ((this.isCorrupted) ? this.CORRUPTION_INDEX : 0) + this.animationFrame);//.draw(nx, ny);;
	    } else {
		view = sheetTop.getSubImage(0, ((this.isCorrupted) ? this.CORRUPTION_INDEX : 0) + ((isDoorsOpen) ? 3 : 0));//.draw(nx, ny);;
	    }
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
        return view;
    }

    @Override
    public boolean shouldRenderedInPass(int passnum) {
	this.currentPass = passnum;
	
	return (passnum == 0) || (passnum == 2);
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);
	
	this.isCorrupted = this.getHp() <= this.getMaxHp() / 2;
	
	if (this.isDoorsCloseAnimation && ++this.ticksBeforeClose <= this.TICKS_BEFORE_CLOSE) {
	    return;
	}
	
	if (this.animationFrameTicks++ > EntityWarFactory.ANIMATION_FRAME_DELAY) {
	    this.animationFrameTicks = 0;
	    
	    if (this.isDoorsOpenAnimation && !this.isDoorsCloseAnimation) {
		this.animationFrame++;
		
		if (this.animationFrame == EntityWarFactory.ANIMATION_LENGTH) {
		    this.isDoorsOpenAnimation = false;
		    this.animationFrame = 0;
		    this.isDoorsOpen = true;
		}
	    } else if (!this.isDoorsOpenAnimation && this.isDoorsCloseAnimation) {
		this.animationFrame--;
		
		if (this.animationFrame == 0) {
		    this.isDoorsCloseAnimation = false;
		    this.isDoorsOpen = false;
		}
	    }
	}
	
	if (isDoorsOpen && this.targetEntity != null) {
	    if (this.deployTryTicks++ > DEPLOY_TRY_INTERVAL) {
		this.deployTryTicks = 0;
		
		tryToDeployEntity();
	    }
	}
    }

    private void tryToDeployEntity() {
	if (tryToMoveOutEntityToUnlockedCells(this.targetEntity)) {
	    targetEntity.isVisible = true;
	    
	    if (targetEntity == null || !targetEntity.boundingBox.intersects(this.exitBoundingBox.getBoundsInLocal())) { // if entity is still inside factory, don't close the doors
		animateCloseDoors();
		this.targetEntity = null;
	    }	    
	}
    }
    
    private boolean tryToMoveOutEntityToUnlockedCells(EntityVehicle v) {
	boolean isSuccess = false;

	int exitX = (int)boundingBox.getX() / 24 + 1;
	int exitY = (int)boundingBox.getY() / 24 + HEIGHT_TILES - 2;
	
	boolean isExitBlocked = isCellBlocked(exitX, exitY);
	
	if (isExitBlocked) {
	    // Try to nudge blocker
	    MobileEntity blocker = world.getMobileEntityInCell(new Pos(exitX, exitY));
	    if (blocker != null) {
		blocker.nudge(null, true);
	    }
	    
	    return false;
	}
	
	Path p = new Path();
	p.getElements().add(new MoveTo((int) targetEntity.getCenterPosX(), (int) targetEntity.getCenterPosY()));
	p.getElements().add(new MoveTo(exitX, exitY));	
	
	for (int i = 0; i < 6; i++) {
	    int cellX = exitX + this.exitDirectionsX[i];
	    int cellY = exitY + this.exitDirectionsY[i];

	    if (!isCellBlocked(cellX, cellY)) {
		isSuccess = true;

		p.getElements().add(new MoveTo(cellX, cellY));

		v.startMovingByPath(p, this);		

		return true;
	    }	    
	}

	return false;
    }
    
    public boolean isCellBlocked(int cellX, int cellY) {
	return !world.isCellPassable(cellX, cellY) || (world.getEntityNonBuildingInPoint((cellX * 24) + 12, (cellY * 24) + 12) != null);
    }
    
    public boolean deployEntity(EntityVehicle target) {
	if (this.targetEntity != null) {
	    return false;
	}
	
	world.spawnEntityInWorld(target);
	target.setWorld(world);
	this.targetEntity = target;
	targetEntity.setPositionByCenter(boundingBox.getX() + 24 + 12, boundingBox.getY() + 24 * 1);
	targetEntity.currentFacing = 16;
	targetEntity.isVisible = false;
	
	animateOpenDoors();
	
	return false;
    }
    
    private void animateOpenDoors() {
	this.isDoorsOpenAnimation = true;
	this.animationFrameTicks = 0;
	this.animationFrame = 0;
	
	this.isDoorsCloseAnimation = false;
    }

    private void animateCloseDoors() {
	this.isDoorsCloseAnimation = true;
	this.animationFrameTicks = 0;
	this.animationFrame = EntityWarFactory.ANIMATION_LENGTH;
	
	this.isDoorsOpenAnimation = false;
	
	this.ticksBeforeClose = 0;
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

    public Alignment getAlignment() {
	return this.weapAlignment;
    }

    @Override
    public int getRevealingRange() {
	return EntityWarFactory.SHROUD_REVEALING_RANGE;
    }
    
    @Override
    public ImageView getTexture() {
	if (this.sheetTop == null) {
	    return null;
	}
	
	return sheetTop.getSubImage(0, 0);
    }
    
    public BufferedImage getBottomTexture() {
	return this.weapDownNormal;
    }

    @Override
    public boolean canDeploy() {
	return true;
    }

    @Override
    public void deploy() {
	executeDeployment();
    }

    @Override
    public void executeDeployment() {
	this.owner.getBase().setPrimaryWarFactory(this);
    }

    @Override
    public int getBuildingCost() {
	return EntityWarFactory.BUILDING_COST;
    }

    @Override
    public int getConsumptionLevel() {
	return EntityWarFactory.CONSUME_POWER_VALUE;
    }    
}
