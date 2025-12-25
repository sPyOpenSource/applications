package cr0s.javara.entity.building.soviet;

import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.common.EntityWall;
import cr0s.javara.gameplay.Team.Alignment;

public class EntityBarredWireWall extends EntityWall {

    private static final int BUILDING_COST = 30;

    public EntityBarredWireWall(Double aTileX, Double aTileY,
	    float aSizeWidth, float aSizeHeight,
	    String aFootprint) {
	super(aTileX, aTileY, aSizeWidth, aSizeHeight, "x");
	
	this.textureName = "fenc.shp";
	loadTextures();
	
	setBibType(BibType.NONE);
	setProgressValue(-1);

	setMaxHp(5);
	setHp(getMaxHp());

	this.makeTextureName = "";
	this.unitProductionAlingment = Alignment.NEUTRAL;
	
	this.setName("fenc");
    }

    public EntityBarredWireWall(Double aTileX, Double aTileY) {
	this(aTileX, aTileY, 24, 24, "x");
    }

    @Override
    public int getBuildingCost() {
	return EntityBarredWireWall.BUILDING_COST;
    }
}
