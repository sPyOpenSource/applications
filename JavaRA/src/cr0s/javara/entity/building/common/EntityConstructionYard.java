package cr0s.javara.entity.building.common;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import java.awt.image.BufferedImage;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.StackPane;

public class EntityConstructionYard extends EntityBuilding implements ISelectable, IShroudRevealer {

    private SpriteSheet sheet;

    private BufferedImage normal, corrupted;
    private final String TEXTURE_NAME = "fact.shp";
    private final String MAKE_TEXTURE_NAME = "factmake.shp";

    public static final int WIDTH_TILES = 3;
    public static final int HEIGHT_TILES = 4;
    private static final int SHROUD_REVEALING_RANGE = 10;

    private static final String FOOTPRINT = "xxx xxx xxx ~~~";

    private final Alignment yardAlignment = Alignment.SOVIET;

    public EntityConstructionYard(Pos tile) {
	super(tile, WIDTH_TILES * 24, HEIGHT_TILES * 24, FOOTPRINT);

	//this.yardAlignment = player.getAlignment();

	setBibType(BibType.MIDDLE);
	setProgressValue(-1);

	setMaxHp(1500);
	setHp(getMaxHp());

	this.armorType = ArmorType.WOOD;
	
	this.buildingSpeed = 100;
	this.makeTextureName = MAKE_TEXTURE_NAME;
	initTextures();

	this.unitProductionAlingment = Alignment.NEUTRAL;

	this.setName("fact");
    }

    private void initTextures() {
	ShpTexture tex = ResourceManager.getInstance().getConquerTexture(TEXTURE_NAME);
	corrupted = tex.getAsImage(51, null);
	normal = tex.getAsImage(0, null);	
    }

    @Override
    public StackPane renderEntity() {
        ImageView view;
	if (this.getHp() > this.getMaxHp() / 2) {
	    view = new ImageView(SwingFXUtils.toFXImage(normal, null));//.draw(nx, ny);
	} else {
	    view = new ImageView(SwingFXUtils.toFXImage(corrupted, null));//.draw(nx, ny);
	}
        view.setX(boundingBox.getX());
        view.setY(boundingBox.getY());
        setImageView(view);
        StackPane combined = new StackPane();
combined.getChildren().add(view);
        return combined;
	// Draw bounding box if debug mode is on
	/*if (GUI.DEBUG_MODE) {
	    g.setLineWidth(2);
	    g.setColor(owner.playerColor);
	    g.draw(boundingBox);
	    g.setLineWidth(1);
	}

	if (this.isSelected) {
	    EntityBuilding currentlyBuilding = (EntityBuilding) this.owner.getBase().getProductionQueue().getCurrentProducingBuilding();
	    if (currentlyBuilding != null) {
		Production p = this.owner.getBase().getProductionQueue().getProductionForBuilding(currentlyBuilding);

		if (p.isBuilding()) {
		    //p.drawProductionButton(g, this.boundingBox.getX(), this.boundingBox.getY(), Color.rgb(255, 255, 255, 1), true);
		}
	    }
	}
	
	// Render repairing wrench
	if (this.repairIconBlink) {
	    //repairImage.draw(this.boundingBox.getX() + this.boundingBox.getWidth() / 2 - repairImage.getWidth() / 2, this.boundingBox.getY() + this.boundingBox.getHeight() / 2 - repairImage.getHeight() / 2);
	}
        return null;*/
    }

    @Override
    public boolean shouldRenderedInPass(int passnum) {
	return passnum == 0;
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);

	/*EntityBuilding currentlyBuilding = (EntityBuilding) this.owner.getBase().getProductionQueue().getCurrentProducingBuilding();
	if (currentlyBuilding != null) {
	    Production p = this.owner.getBase().getProductionQueue().getProductionForBuilding(currentlyBuilding);

	    if (p.isBuilding()) {
		this.setProgressValue((int) (p.getCurrentBuildingProgress() * 100));
		this.setMaxProgress(100);
	    }
	} else {
	    this.setProgressValue(-1);
	}*/
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
	return this.yardAlignment;
    }

    @Override
    public int getRevealingRange() {
	return EntityConstructionYard.SHROUD_REVEALING_RANGE;
    }

    @Override
    public ImageView getTexture() {
	/*if (sheet == null) {
	    return null;
	}*/
        if(getImageViews().isEmpty()){
            //ImageView view = sheet.getSubImage(0, 0);
            ImageView view = new ImageView(SwingFXUtils.toFXImage(normal, null));
            view.setX(boundingBox.getX());
            view.setY(boundingBox.getY());
            getImageViews().add(view);
        }
        return getImageViews().get(0);
    }    
    
}
