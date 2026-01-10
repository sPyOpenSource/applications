package cr0s.javara.entity.building.common;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.IPowerProducer;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;

import java.awt.image.BufferedImage;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.StackPane;

public class EntityAdvPowerPlant extends EntityBuilding implements ISelectable, IPowerProducer, IShroudRevealer, IHaveCost {
    private BufferedImage normal, corrupted;
    private final String TEXTURE_NAME = "apwr.shp";
    private final String MAKE_TEXTURE_NAME = "apwrmake.shp";

    public static final int WIDTH_TILES = 3;
    public static final int HEIGHT_TILES = 4;

    private static final int POWER_PRODUCTION_LEVEL = 200;
    private static final int SHROUD_REVEALING_RANGE = 7;
    private static final int BUILDING_COST = 500;

    public EntityAdvPowerPlant(Pos tile) {
	super(tile, WIDTH_TILES * 24, HEIGHT_TILES * 24, "xxx xxx xxx ~~~");

	setBibType(BibType.MIDDLE);
	setProgressValue(-1);

	setMaxHp(700);
	setHp(getMaxHp());

	this.armorType = ArmorType.WOOD;
	this.makeTextureName = MAKE_TEXTURE_NAME;
	initTextures();
	
	this.unitProductionAlingment = Alignment.NEUTRAL;
	
	this.requiredToBuild.add(EntityRadarDome.class);
	
	this.setName("apwr");
    }

    private void initTextures() {
	ShpTexture tex = ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME);
	normal = tex.getAsImage(0, null);	
	corrupted = tex.getAsImage(1, null);
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
        StackPane combined = new StackPane();
combined.getChildren().add(view);
        view.setX(boundingBox.getX());
        view.setY(boundingBox.getY());
        return combined;
    }

    @Override
    public boolean shouldRenderedInPass(int passnum) {
	return passnum == 0;
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);
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
    public int getPowerProductionLevel() {
	return POWER_PRODUCTION_LEVEL;
    }

    @Override
    public int getRevealingRange() {
	return EntityAdvPowerPlant.SHROUD_REVEALING_RANGE;
    }
    
    @Override
    public ImageView getImageView() {
	return new ImageView(SwingFXUtils.toFXImage(normal, null));
    }

    @Override
    public int getBuildingCost() {
	return BUILDING_COST;
    }        
}
