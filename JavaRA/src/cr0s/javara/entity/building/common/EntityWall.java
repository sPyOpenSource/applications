package cr0s.javara.entity.building.common;

import cr0s.javara.entity.IDefense;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import cr0s.javara.util.SpriteSheet;

import javafx.scene.image.ImageView;

public abstract class EntityWall extends EntityBuilding implements IHaveCost, IDefense {

    protected boolean dirty;
    protected int adjacent = 0;

    protected SpriteSheet sheet;
    public String textureName = "brik.shp";

    protected int damageModifier = 0; // 0...4, damage level

    private final int UPDATE_INTERVAL_TICKS = 20;
    private final int updateNeighboursTicks = 0;
    
    public int lineBuildMaxLength = 8;
    
    public EntityWall(Pos aTile,
	    float aSizeWidth, float aSizeHeight, String aFootprint) {
	super(aTile, aSizeWidth, aSizeHeight, "x");
	// TODO Auto-generated constructor stub
    }

    protected void loadTextures() {
	ShpTexture tex = ResourceManager.getInstance().getConquerTexture(this.textureName);

	if (tex != null) {
	    this.sheet = new SpriteSheet(tex.getAsCombinedImage(null), 24, 24);
	} else {
	    System.err.println("Wall texture not found: " + this.textureName);
	}
    }

    @Override
    public void updateEntity(long delta) {
	if (!this.dirty) {
	    return;
	}

	this.adjacent = 0;

	// Search in directions: Down, Right, Up, Left
	int[] dx = { 0, 1, 0, -1 };
	int[] dy = { -1, 0, 1, 0 };

	for (int i = 0; i < 4; i++) {
	    int checkX = 0;//(int) (this.boundingBox.getCenterX() / 24) + dx[i];
	    int checkY = 0;//(int) (this.boundingBox.getCenterY() / 24) + dy[i];

	    // TODO: use actor/blocking map for this to get O(1) instead O(n) complexity
	    EntityBuilding b = world.getBuildingInCell(new Pos(checkX, checkY));

	    if (b != null && b instanceof EntityWall) {
		if (((EntityWall) b).textureName.equals(this.textureName)) {
		    this.adjacent |= 1 << i;
		}
	    }
	}

	//updateNeighbours();
	this.dirty = false;
    }

    private void updateNeighbours() {	
	int[] dx = { 0, 1, 0, -1 };
	int[] dy = { -1, 0, 1, 0 };

	int updated = 0;
	
	for (int i = 0; i < 4; i++) {
	    int checkX = 0;//(int) (this.boundingBox.getCenterX() / 24) + dx[i];
	    int checkY = 0;//(int) (this.boundingBox.getCenterY() / 24) + dy[i];

	    // TODO: use actor/blocking map for this to get O(1) instead O(n) complexity
	    EntityBuilding b = world.getBuildingInCell(new Pos(checkX, checkY));

	    if (b != null && b instanceof EntityWall) {
		((EntityWall) b).setDirty();
		updated++;
	    }
	}	
    }

    public void setDirty() {
	this.dirty = true;
    }

    @Override
    public ImageView renderEntity() {
	return sheet.getSubImage(0, 16 * this.damageModifier + this.adjacent);//.draw(this.posX, this.posY);
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == -2;
    }

    @Override
    public float getHeightInTiles() {
	return 1;
    }

    @Override
    public float getWidthInTiles() {
	return 1;
    }

    @Override
    public ImageView getTexture() {
	return sheet.getSubImage(0, 16 * this.damageModifier + this.adjacent);
    }

    @Override
    public void onBuildFinished() {
	this.dirty = true;
	this.updateNeighbours();
    }
    
    @Override
    public void setRepairing(boolean b) {
    }
}
